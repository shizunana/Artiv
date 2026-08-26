package com.artiv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.InvitationAcceptRequest;
import com.artiv.dto.InvitationProjectSummary;
import com.artiv.dto.InvitationRejectRequest;
import com.artiv.dto.PublicInvitationCreateRequest;
import com.artiv.entity.Application;
import com.artiv.entity.Invitation;
import com.artiv.entity.Project;
import com.artiv.entity.User;
import com.artiv.mapper.ApplicationMapper;
import com.artiv.mapper.InvitationMapper;
import com.artiv.mapper.ProjectMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.service.AccountRestrictionService;
import com.artiv.service.ApplicationService;
import com.artiv.service.InvitationService;
import com.artiv.service.MessageService;
import com.artiv.service.UserBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.artiv.common.ProjectTimeRules.isExpired;
import static com.artiv.common.SystemMessageText.mark;
import static com.artiv.common.SystemMessageText.markTitle;

@Service
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService {

    private static final int TYPE_DIRECTED = 0;
    private static final int TYPE_PUBLIC_QUOTE = 1;
    private static final int TYPE_PUBLIC_PRICED = 2;
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_ACCEPTED = 1;
    private static final int STATUS_REJECTED = 2;
    private static final int STATUS_INVALID = 3;
    private static final int QUOTE_SOURCE_PAINTER = 0;
    private static final int QUOTE_SOURCE_EMPLOYER = 1;

    private final InvitationMapper invitationMapper;
    private final ApplicationMapper applicationMapper;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final ApplicationService applicationService;
    private final MessageService messageService;
    private final AccountRestrictionService accountRestrictionService;
    private final UserBlockService userBlockService;

    @Override
    @Transactional
    public Invitation createPublic(PublicInvitationCreateRequest request, Long employerId) {
        accountRestrictionService.requireAvailable(employerId, "邀请画师");
        if (!Integer.valueOf(TYPE_PUBLIC_QUOTE).equals(request.getInvitationType())
                && !Integer.valueOf(TYPE_PUBLIC_PRICED).equals(request.getInvitationType())) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "公开企划邀请类型不正确");
        }
        Project project = projectMapper.selectById(request.getProjectId());
        if (!isActivePublicProject(project) || !employerId.equals(project.getEmployerId())) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "只能邀请画师参与自己正在招募的公开企划");
        }
        User painter = requireCertifiedPainter(request.getPainterId());
        if (employerId.equals(painter.getId())) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "不能邀请自己参与企划");
        }
        userBlockService.requireNotBlocked(employerId, painter.getId(), "发送邀请");
        Long activeApplication = applicationMapper.selectCount(new LambdaQueryWrapper<Application>()
                .eq(Application::getProjectId, project.getId())
                .eq(Application::getPainterId, painter.getId())
                .eq(Application::getActive, 1)
                .eq(Application::getDeleted, 0));
        if (activeApplication != null && activeApplication > 0) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "该画师已经应征当前企划，不能重复邀请");
        }
        if (Integer.valueOf(TYPE_PUBLIC_QUOTE).equals(request.getInvitationType())
                && request.getOfferedPrice() != null) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "邀请报价不需要填写邀请价格");
        }
        if (Integer.valueOf(TYPE_PUBLIC_PRICED).equals(request.getInvitationType())
                && (request.getOfferedPrice() == null
                || request.getOfferedPrice().compareTo(new BigDecimal("50.00")) < 0)) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "带价邀请价格不能低于 50 元");
        }

        LocalDateTime now = LocalDateTime.now();
        Invitation invitation = new Invitation();
        invitation.setProjectId(project.getId());
        invitation.setPainterId(painter.getId());
        invitation.setMessage(trimToNull(request.getMessage()));
        invitation.setInvitationType(request.getInvitationType());
        invitation.setOfferedPrice(Integer.valueOf(TYPE_PUBLIC_PRICED).equals(request.getInvitationType())
                ? request.getOfferedPrice() : null);
        invitation.setStatus(STATUS_PENDING);
        invitation.setPainterHidden(0);
        invitation.setDeleted(0);
        invitation.setCreatedAt(now);
        invitation.setUpdatedAt(now);
        invitationMapper.insert(invitation);

        User employer = userMapper.selectById(employerId);
        String content = "企划方" + mark(displayName(employer, employerId)) + "邀请您参与企划"
                + markTitle(project.getTitle());
        if (Integer.valueOf(TYPE_PUBLIC_PRICED).equals(request.getInvitationType())) {
            content += "，邀请价格" + mark(formatPrice(request.getOfferedPrice()));
        }
        content += "，请进入企划详情查看并选择接受或拒绝。";
        String subtype = Integer.valueOf(TYPE_PUBLIC_QUOTE).equals(request.getInvitationType())
                ? "PUBLIC_QUOTE_INVITATION_CREATED" : "PUBLIC_PRICED_INVITATION_CREATED";
        messageService.createNotification(painter.getId(), 0, subtype, project.getId(), content);
        return invitation;
    }

    @Override
    @Transactional
    public Page<InvitationProjectSummary> listMine(long page, long size, Long userId, Integer status) {
        if (status != null && (status < STATUS_PENDING || status > STATUS_INVALID)) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "邀请状态筛选参数不正确");
        }
        convergeInvalidPublicInvitations(userId);
        LambdaQueryWrapper<Invitation> query = new LambdaQueryWrapper<Invitation>()
                .eq(Invitation::getPainterId, userId)
                .eq(Invitation::getPainterHidden, 0)
                .eq(Invitation::getDeleted, 0);
        if (status == null) {
            query.orderByAsc(Invitation::getStatus).orderByDesc(Invitation::getCreatedAt);
        } else {
            query.eq(Invitation::getStatus, status).orderByDesc(Invitation::getCreatedAt);
        }
        Page<Invitation> invitations = invitationMapper.selectPage(new Page<>(page, size), query);
        List<Long> projectIds = invitations.getRecords().stream().map(Invitation::getProjectId).distinct().toList();
        Map<Long, Project> projects = projectIds.isEmpty() ? Map.of()
                : projectMapper.selectBatchIds(projectIds).stream()
                .collect(Collectors.toMap(Project::getId, Function.identity()));
        Page<InvitationProjectSummary> result = new Page<>(page, size, invitations.getTotal());
        result.setRecords(invitations.getRecords().stream()
                .map(invitation -> toSummary(invitation, projects.get(invitation.getProjectId())))
                .toList());
        return result;
    }

    @Override
    @Transactional
    public InvitationProjectSummary getMineByProject(Long projectId, Long userId) {
        convergeInvalidPublicInvitations(userId);
        Invitation invitation = invitationMapper.selectOne(new LambdaQueryWrapper<Invitation>()
                .eq(Invitation::getProjectId, projectId)
                .eq(Invitation::getPainterId, userId)
                .eq(Invitation::getDeleted, 0)
                .eq(Invitation::getStatus, STATUS_PENDING)
                .orderByDesc(Invitation::getCreatedAt)
                .last("LIMIT 1"));
        if (invitation == null) {
            invitation = invitationMapper.selectOne(new LambdaQueryWrapper<Invitation>()
                    .eq(Invitation::getProjectId, projectId)
                    .eq(Invitation::getPainterId, userId)
                    .eq(Invitation::getDeleted, 0)
                    .orderByDesc(Invitation::getCreatedAt)
                    .last("LIMIT 1"));
        }
        if (invitation == null) throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        return toSummary(invitation, projectMapper.selectById(projectId));
    }

    @Override
    @Transactional
    public Invitation accept(Long id, InvitationAcceptRequest request, Long painterId) {
        accountRestrictionService.requireAvailable(painterId, "接受邀请");
        Invitation invitation = requirePendingMine(id, painterId);
        User painter = requireCertifiedPainter(painterId);
        int invitationType = normalizedType(invitation);
        Project project;
        BigDecimal quote;
        String intro;
        int quoteSource;
        boolean directed = invitationType == TYPE_DIRECTED;
        if (directed) {
            project = requireActiveDirectedProject(invitation.getProjectId(), painterId, true);
            quote = requirePositiveQuote(request.getQuote());
            intro = "接受定向邀请";
            quoteSource = QUOTE_SOURCE_PAINTER;
        } else {
            project = requireActivePublicProject(invitation.getProjectId());
            if (invitationType == TYPE_PUBLIC_QUOTE) {
                quote = requirePositiveQuote(request.getQuote());
                intro = "接受邀请报价";
                quoteSource = QUOTE_SOURCE_PAINTER;
            } else if (invitationType == TYPE_PUBLIC_PRICED) {
                if (invitation.getOfferedPrice() == null
                        || invitation.getOfferedPrice().compareTo(new BigDecimal("50.00")) < 0) {
                    throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "邀请价格无效，无法接受");
                }
                quote = invitation.getOfferedPrice();
                intro = "接受带价邀请";
                quoteSource = QUOTE_SOURCE_EMPLOYER;
            } else {
                throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "邀请类型不正确");
            }
        }
        userBlockService.requireNotBlocked(project.getEmployerId(), painterId, "接受邀请");

        Application application = applicationService.createFromInvitation(
                project.getId(), painterId, intro, quote, quoteSource, directed);
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Invitation> update = new LambdaUpdateWrapper<Invitation>()
                .eq(Invitation::getProjectId, project.getId())
                .eq(Invitation::getPainterId, painterId)
                .eq(Invitation::getStatus, STATUS_PENDING)
                .eq(Invitation::getDeleted, 0)
                .set(Invitation::getStatus, STATUS_ACCEPTED)
                .set(Invitation::getApplicationId, application.getId())
                .set(Invitation::getRejectReason, null)
                .set(Invitation::getRespondedAt, now)
                .set(Invitation::getUpdatedAt, now);
        if (directed) {
            update.eq(Invitation::getId, invitation.getId()).eq(Invitation::getInvitationType, TYPE_DIRECTED);
        } else {
            update.in(Invitation::getInvitationType, TYPE_PUBLIC_QUOTE, TYPE_PUBLIC_PRICED);
        }
        int updated = invitationMapper.update(null, update);
        if (updated < 1) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "邀请已被处理，请刷新后重试");
        }

        String content = acceptedContent(painter, project);
        messageService.createInvitationAcceptedConversation(project.getEmployerId(), painterId, project.getId(), content);
        String subtype = switch (invitationType) {
            case TYPE_PUBLIC_QUOTE -> "PUBLIC_QUOTE_INVITATION_ACCEPTED";
            case TYPE_PUBLIC_PRICED -> "PUBLIC_PRICED_INVITATION_ACCEPTED";
            default -> "INVITATION_ACCEPTED";
        };
        messageService.createNotification(project.getEmployerId(), 0, subtype, project.getId(), content);
        return invitationMapper.selectById(invitation.getId());
    }

    @Override
    @Transactional
    public Invitation reject(Long id, InvitationRejectRequest request, Long painterId) {
        accountRestrictionService.requireAvailable(painterId, "拒绝邀请");
        Invitation invitation = requirePendingMine(id, painterId);
        User painter = requireCertifiedPainter(painterId);
        int invitationType = normalizedType(invitation);
        Project project = invitationType == TYPE_DIRECTED
                ? requireActiveDirectedProject(invitation.getProjectId(), painterId, false)
                : requireActivePublicProject(invitation.getProjectId());
        String reason = request.getReason().trim();
        LocalDateTime now = LocalDateTime.now();
        int updated = invitationMapper.update(null, new LambdaUpdateWrapper<Invitation>()
                .eq(Invitation::getId, invitation.getId())
                .eq(Invitation::getPainterId, painterId)
                .eq(Invitation::getStatus, STATUS_PENDING)
                .eq(Invitation::getDeleted, 0)
                .set(Invitation::getStatus, STATUS_REJECTED)
                .set(Invitation::getRejectReason, reason)
                .set(Invitation::getRespondedAt, now)
                .set(Invitation::getUpdatedAt, now));
        if (updated != 1) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "邀请已被处理，请刷新后重试");
        }
        if (invitationType == TYPE_DIRECTED) {
            project.setStatus(4);
            project.setUpdatedAt(now);
            projectMapper.updateById(project);
        }
        String content = "画师" + mark(displayName(painter, painterId)) + "婉拒了您的企划"
                + markTitle(project.getTitle()) + "的邀请，婉拒理由" + mark(reason);
        messageService.createNotification(project.getEmployerId(), 0, "INVITATION_REJECTED", project.getId(), content);
        return invitationMapper.selectById(invitation.getId());
    }

    @Override
    @Transactional
    public void hideReminder(Long id, Long painterId) {
        Invitation invitation = invitationMapper.selectById(id);
        if (invitation == null || !Integer.valueOf(0).equals(invitation.getDeleted())
                || !painterId.equals(invitation.getPainterId())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (Integer.valueOf(1).equals(invitation.getPainterHidden())) return;
        LocalDateTime now = LocalDateTime.now();
        int updated = invitationMapper.update(null, new LambdaUpdateWrapper<Invitation>()
                .eq(Invitation::getId, id)
                .eq(Invitation::getPainterId, painterId)
                .eq(Invitation::getDeleted, 0)
                .set(Invitation::getPainterHidden, 1)
                .set(Invitation::getPainterHiddenAt, now)
                .set(Invitation::getUpdatedAt, now));
        if (updated != 1) throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
    }

    private void convergeInvalidPublicInvitations(Long painterId) {
        List<Invitation> pending = invitationMapper.selectList(new LambdaQueryWrapper<Invitation>()
                .eq(Invitation::getPainterId, painterId)
                .in(Invitation::getInvitationType, TYPE_PUBLIC_QUOTE, TYPE_PUBLIC_PRICED)
                .eq(Invitation::getStatus, STATUS_PENDING)
                .eq(Invitation::getDeleted, 0));
        if (pending.isEmpty()) return;
        List<Long> projectIds = pending.stream().map(Invitation::getProjectId).distinct().toList();
        Map<Long, Project> projects = projectMapper.selectBatchIds(projectIds).stream()
                .collect(Collectors.toMap(Project::getId, Function.identity()));
        List<Long> invalidIds = pending.stream()
                .filter(invitation -> !isActivePublicProject(projects.get(invitation.getProjectId())))
                .map(Invitation::getId).toList();
        if (invalidIds.isEmpty()) return;
        LocalDateTime now = LocalDateTime.now();
        invitationMapper.update(null, new LambdaUpdateWrapper<Invitation>()
                .in(Invitation::getId, invalidIds)
                .eq(Invitation::getStatus, STATUS_PENDING)
                .set(Invitation::getStatus, STATUS_INVALID)
                .set(Invitation::getRespondedAt, now)
                .set(Invitation::getUpdatedAt, now));
    }

    private Invitation requirePendingMine(Long id, Long painterId) {
        Invitation invitation = invitationMapper.selectById(id);
        if (invitation == null || !Integer.valueOf(0).equals(invitation.getDeleted())
                || !painterId.equals(invitation.getPainterId())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (!Integer.valueOf(STATUS_PENDING).equals(invitation.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "邀请已被处理");
        }
        return invitation;
    }

    private Project requireActiveDirectedProject(Long projectId, Long painterId, boolean requireUnexpired) {
        Project project = projectMapper.selectById(projectId);
        if (project == null || !Integer.valueOf(0).equals(project.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(project.getMode()) || !Integer.valueOf(0).equals(project.getStatus())
                || Integer.valueOf(1).equals(project.getModerationStatus())
                || (requireUnexpired && isExpired(project.getDeadline()))) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "该定向企划当前不能处理邀请");
        }
        Long targetCount = invitationMapper.selectCount(new LambdaQueryWrapper<Invitation>()
                .eq(Invitation::getProjectId, projectId)
                .eq(Invitation::getPainterId, painterId)
                .eq(Invitation::getInvitationType, TYPE_DIRECTED)
                .eq(Invitation::getDeleted, 0));
        if (targetCount == null || targetCount == 0) throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        return project;
    }

    private Project requireActivePublicProject(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (!isActivePublicProject(project)) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "该公开企划已失效，不能处理邀请");
        }
        return project;
    }

    private boolean isActivePublicProject(Project project) {
        return project != null && Integer.valueOf(0).equals(project.getDeleted())
                && Integer.valueOf(0).equals(project.getMode())
                && Integer.valueOf(0).equals(project.getStatus())
                && Integer.valueOf(0).equals(project.getModerationStatus())
                && !isExpired(project.getDeadline());
    }

    private User requireCertifiedPainter(Long painterId) {
        User painter = userMapper.selectById(painterId);
        if (painter == null || !Integer.valueOf(0).equals(painter.getDeleted())
                || !Integer.valueOf(0).equals(painter.getStatus())
                || !Integer.valueOf(2).equals(painter.getCertStatus())) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "仅正常状态的已认证画师可以接收和处理邀请");
        }
        return painter;
    }

    private InvitationProjectSummary toSummary(Invitation invitation, Project project) {
        InvitationProjectSummary summary = new InvitationProjectSummary();
        summary.setInvitationId(invitation.getId());
        summary.setProjectId(invitation.getProjectId());
        summary.setInvitationType(normalizedType(invitation));
        summary.setOfferedPrice(invitation.getOfferedPrice());
        summary.setMessage(invitation.getMessage());
        summary.setStatus(invitation.getStatus());
        summary.setRejectReason(invitation.getRejectReason());
        summary.setApplicationId(invitation.getApplicationId());
        summary.setCreatedAt(invitation.getCreatedAt());
        summary.setRespondedAt(invitation.getRespondedAt());
        boolean available = project != null && Integer.valueOf(0).equals(project.getDeleted());
        summary.setProjectAvailable(available);
        if (project != null) {
            summary.setCover(firstImage(project.getImages()));
            summary.setProjectTitle(project.getTitle());
            summary.setCategory(project.getCategory());
            summary.setProjectDescription(project.getDescription());
            summary.setBudget(project.getBudget());
            summary.setBudgetMin(project.getBudgetMin());
            summary.setBudgetMax(project.getBudgetMax());
        } else {
            summary.setProjectTitle("企划已删除");
        }
        return summary;
    }

    private int normalizedType(Invitation invitation) {
        return invitation.getInvitationType() == null ? TYPE_DIRECTED : invitation.getInvitationType();
    }

    private BigDecimal requirePositiveQuote(BigDecimal quote) {
        if (quote == null || quote.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "请填写大于 0 的报价");
        }
        return quote;
    }

    private String acceptedContent(User painter, Project project) {
        return "画师" + mark(displayName(painter, painter.getId())) + "接受了您的企划"
                + markTitle(project.getTitle()) + "的邀请，双方已建立会话，您可以开始与画师沟通后续合作内容。";
    }

    private String formatPrice(BigDecimal value) {
        return "¥" + value.stripTrailingZeros().toPlainString();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String firstImage(String images) {
        if (!StringUtils.hasText(images)) return null;
        for (String image : images.split(",")) {
            if (!image.isBlank()) return image.trim();
        }
        return null;
    }

    private String displayName(User user, Long fallbackId) {
        if (user != null && StringUtils.hasText(user.getNickname())) return user.getNickname();
        if (user != null && StringUtils.hasText(user.getUsername())) return user.getUsername();
        return "#" + fallbackId;
    }
}
