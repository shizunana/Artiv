package com.artiv.service;

import com.artiv.common.BusinessException;
import com.artiv.common.ProjectTimeRules;
import com.artiv.entity.*;
import com.artiv.mapper.*;
import com.artiv.guide.tool.GuidePublicReader.ReadResult;
import com.artiv.guide.tool.GuideReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

/** 向导专属只读投影。不调用订单详情的水印修复或邀请查询的过期写回，不提供写操作。 */
@Service
@Transactional(readOnly = true)
public class GuideBusinessReadService {
    private final ProjectService projects;
    private final OrderInfoMapper orders;
    private final OrderStageMapper stages;
    private final InvitationMapper invitations;
    private final ProjectMapper projectMapper;
    private final ObjectMapper json;
    public GuideBusinessReadService(ProjectService projects, OrderInfoMapper orders, OrderStageMapper stages,
                                    InvitationMapper invitations, ProjectMapper projectMapper, ObjectMapper json) {
        this.projects = projects; this.orders = orders; this.stages = stages;
        this.invitations = invitations; this.projectMapper = projectMapper; this.json = json;
    }
    public ReadResult read(long owner, String operation, Long id, int page) {
        if (owner <= 0 || page < 1 || page > 10 || id != null && id <= 0) throw unavailable();
        return switch (operation) {
            case "my_projects" -> {
                var result = projects.listMine(page, 5, owner, null);
                var records = result.getRecords().stream().filter(p -> ownedProject(p, owner)).limit(5)
                        .map(p -> projectCard(projects.getGuideSummary(p.getId(), owner), owner)).toList();
                yield result(operation, page, result.getTotal(), records);
            }
            case "my_project" -> {
                if (id == null) throw unavailable();
                var p = projects.getGuideSummary(id, owner);
                yield result(operation, page, 1, List.of(projectCard(p, owner)));
            }
            case "my_orders", "my_order" -> {
                var query = new LambdaQueryWrapper<OrderInfo>().select(OrderInfo::getId, OrderInfo::getEmployerId,
                        OrderInfo::getPainterId, OrderInfo::getStatus, OrderInfo::getCurrentStage,
                        OrderInfo::getPhaseSnapshot, OrderInfo::getDeleted)
                        .eq(OrderInfo::getDeleted, 0).and(q -> q.eq(OrderInfo::getEmployerId, owner).or().eq(OrderInfo::getPainterId, owner));
                if (operation.equals("my_order")) { if (id == null) throw unavailable(); query.eq(OrderInfo::getId, id); }
                query.orderByDesc(OrderInfo::getId);
                var result = orders.selectPage(new Page<>(page, 5), query);
                var records = result.getRecords().stream().filter(o -> ownedOrder(o, owner)).limit(5).map(o -> orderCard(o, owner)).toList();
                if (id != null && records.isEmpty()) throw unavailable();
                yield result(operation, page, result.getTotal(), records);
            }
            case "my_invitations", "my_invitation" -> {
                var query = new LambdaQueryWrapper<Invitation>().select(Invitation::getId, Invitation::getProjectId,
                        Invitation::getPainterId, Invitation::getStatus, Invitation::getInvitationType, Invitation::getPainterHidden, Invitation::getDeleted)
                        .eq(Invitation::getPainterId, owner).eq(Invitation::getDeleted, 0).eq(Invitation::getPainterHidden, 0);
                if (operation.equals("my_invitation")) { if (id == null) throw unavailable(); query.eq(Invitation::getId, id); }
                query.orderByDesc(Invitation::getId);
                var result = invitations.selectPage(new Page<>(page, 5), query);
                var records = result.getRecords().stream().filter(i -> owner == Objects.requireNonNullElse(i.getPainterId(), -1L)
                                && Integer.valueOf(0).equals(i.getDeleted()) && Integer.valueOf(0).equals(i.getPainterHidden()))
                        .limit(5).map(this::invitationCard).toList();
                if (id != null && records.isEmpty()) throw unavailable();
                yield result(operation, page, result.getTotal(), records);
            }
            default -> throw unavailable();
        };
    }
    private Map<String, Object> projectCard(Project p, long owner) {
        if (!ownedProject(p, owner)) throw unavailable();
        var data = new LinkedHashMap<String, Object>();
        data.put("id", String.valueOf(p.getId())); data.put("title", clip(p.getTitle(), 80));
        data.put("status", label(p.getStatus(), List.of("招募中", "已选定", "进行中", "已完成", "已下架")));
        data.put("expired", ProjectTimeRules.isExpired(p.getDeadline()));
        data.put("moderated", Integer.valueOf(1).equals(p.getModerationStatus()));
        data.put("contentEditable", Boolean.TRUE.equals(p.getContentEditable()));
        data.put("deletable", p.getDeletable()); data.put("deleteBlockReason", p.getDeleteBlockReason());
        data.put("nextStep", "仅解释查询时状态；需要操作时请人工进入企划面板，由原业务接口重新校验。可编辑/可删除不代表可立即执行其他操作。");
        return data;
    }
    private Map<String, Object> orderCard(OrderInfo o, long owner) {
        boolean painter = Objects.equals(owner, o.getPainterId());
        String phase = "未能确认阶段名称";
        try {
            var list = json.readTree(o.getPhaseSnapshot());
            int index = o.getCurrentStage() == null ? -1 : o.getCurrentStage();
            if (list.isArray() && index >= 0 && index < list.size()) phase = clip(list.get(index).path("name").asText(), 60);
        } catch (Exception ignored) { }
        var current = o.getCurrentStage() == null ? -1 : o.getCurrentStage();
        var latest = stages.selectOne(new LambdaQueryWrapper<OrderStage>().select(OrderStage::getStatus, OrderStage::getStageIndex)
                .eq(OrderStage::getOrderId, o.getId()).eq(OrderStage::getStageIndex, current).eq(OrderStage::getDeleted, 0)
                .orderByDesc(OrderStage::getSubmissionNo).last("LIMIT 1"));
        String next = switch (Objects.requireNonNullElse(o.getStatus(), -1)) {
            case 0 -> painter ? "前往原订单页决定是否接单；尚未开始创作" : "等待画师接单；可到原订单页查看待接单操作";
            case 1 -> painter ? latest != null && Integer.valueOf(2).equals(latest.getStatus()) ? "当前阶段被要求修改；在原订单页核对意见后重新提交" : "按当前阶段创作并在原订单页提交" : "等待画师提交当前阶段";
            case 2 -> painter ? "等待企划方验收当前阶段" : "在原订单页核对当前阶段稿，再人工决定通过或要求修改";
            case 3 -> "订单已完成，不再提交阶段稿；评价和原图下载资格以原订单页实时校验为准";
            case 4 -> "订单已取消，不再继续交稿或验收";
            case 5 -> "处于争议/售后处理中，等待原售后流程处理，不建议继续阶段交付";
            default -> "状态无法确认，请核对原订单页";
        };
        return Map.of("id", String.valueOf(o.getId()), "yourRole", painter ? "画师" : "企划方", "status", label(o.getStatus(), List.of("待接单", "进行中", "待验收", "已完成", "已取消", "争议中")),
                "currentStageIndex", current, "currentStageName", phase, "nextStep", next,
                "navigationNote", "管理员测试入口不提供普通工作台订单跳转；未扩大路由或订单权限");
    }
    private Map<String, Object> invitationCard(Invitation i) {
        // 只读企划状态投影；不取留言、拒绝理由、报价、私图或应征内容。
        var p = projectMapper.selectOne(new LambdaQueryWrapper<Project>().select(Project::getId, Project::getDeleted,
                Project::getMode, Project::getStatus, Project::getModerationStatus, Project::getDeadline).eq(Project::getId, i.getProjectId()));
        int type = Objects.requireNonNullElse(i.getInvitationType(), 0);
        boolean active = p != null && Integer.valueOf(0).equals(p.getDeleted()) && Integer.valueOf(0).equals(p.getStatus())
                && Integer.valueOf(0).equals(p.getModerationStatus()) && Integer.valueOf(type == 0 ? 1 : 0).equals(p.getMode())
                && !ProjectTimeRules.isExpired(p.getDeadline());
        boolean pending = Integer.valueOf(0).equals(i.getStatus());
        String effective = !pending ? "已处理，以存储状态为准" : active ? "企划当前有效；是否可接受仍须原接口校验账号和业务条件" : "当前企划条件不满足，不能据此接受邀请";
        return Map.of("id", String.valueOf(i.getId()), "projectId", String.valueOf(i.getProjectId()),
                "type", label(type, List.of("定向邀请", "公开邀请报价", "公开带价邀请")),
                "storedStatus", label(i.getStatus(), List.of("待处理", "已接受", "已拒绝", "已失效")),
                "currentValidity", effective, "pendingStoredButUnavailable", pending && !active,
                "nextStep", "只解释本人收到且未隐藏的邀请；不会写回过期状态。接受邀请不等于建立订单，删除提醒也不影响已建会话/订单。");
    }
    private ReadResult result(String kind, int page, long total, List<?> records) {
        String now = Instant.now().toString();
        String scope = "仅当前登录账号本人关系内的数据；第 " + page + " 页，每页最多5条，匹配总数 " + total + "，本页 " + records.size() + " 条；不是全平台或完整待办统计。";
        return new ReadResult(Map.of("scope", scope, "page", page, "pageSize", 5, "total", total,
                "hasMore", total > (long)page * 5, "checkedAt", now, "records", records),
                List.of(new GuideReference("private_query", kind + "-" + page, "本人业务状态查询", scope, now)));
    }
    private boolean ownedProject(Project p, long owner) { return p != null && Objects.equals(p.getEmployerId(), owner) && Integer.valueOf(0).equals(p.getDeleted()); }
    private boolean ownedOrder(OrderInfo o, long owner) { return o != null && Integer.valueOf(0).equals(o.getDeleted()) && (Objects.equals(o.getEmployerId(), owner) || Objects.equals(o.getPainterId(), owner)); }
    private String label(Integer value, List<String> labels) { return value != null && value >= 0 && value < labels.size() ? labels.get(value) : "未知状态"; }
    private String clip(String value, int max) { return value == null ? "" : value.substring(0, Math.min(max, value.length())); }
    private BusinessException unavailable() { return new BusinessException(404, "本人业务记录不存在或无权读取"); }
}
