package com.huiyue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huiyue.common.BusinessException;
import com.huiyue.common.ResultCode;
import com.huiyue.dto.StallLimitedUpdateRequest;
import com.huiyue.entity.OrderInfo;
import com.huiyue.entity.Stall;
import com.huiyue.entity.User;
import com.huiyue.mapper.OrderInfoMapper;
import com.huiyue.mapper.UserMapper;
import com.huiyue.mapper.StallMapper;
import com.huiyue.service.StallService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StallServiceImpl implements StallService {

    private static final int ORDER_COMPLETED = 3;
    private static final int ORDER_CANCELLED = 4;

    private static final Set<String> CATEGORY_OPTIONS = Set.of(
            "头像", "Q版全身", "半身像", "立绘", "组合页",
            "服设", "壁纸", "表情包", "Live2d", "平面设计"
    );

    private final StallMapper stallMapper;
    private final UserMapper userMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final ObjectMapper objectMapper;

    private static final Set<String> SUBJECT_OPTIONS = Set.of(
            "LOGO", "Q版", "lolita", "安头", "拆分", "场景", "定制", "动图", "氛围", "搞笑",
            "古风", "黑白", "横插", "厚涂", "可爱", "立绘", "模板", "男性", "女性", "排版",
            "签名", "日常", "日系", "色块", "竖插", "双人", "水印", "像素", "小动物", "小元素",
            "有背景", "正比", "周边制品", "桌宠", "字体设计"
    );

    @Override
    public Page<Stall> listStalls(long page, long size, String category, String keyword, String sort,
                                  String subject, java.math.BigDecimal priceMin, java.math.BigDecimal priceMax,
                                  String deliverTime) {
        LambdaQueryWrapper<Stall> wrapper = new LambdaQueryWrapper<Stall>()
                .eq(Stall::getDeleted, 0)
                .eq(Stall::getStatus, 1)
                .gt(Stall::getStock, 0)
                .eq(StringUtils.hasText(category), Stall::getCategory, category)
                .like(StringUtils.hasText(keyword), Stall::getTitle, keyword)
                .like(StringUtils.hasText(subject), Stall::getParams, subject)
                .ge(priceMin != null, Stall::getPrice, priceMin)
                .le(priceMax != null, Stall::getPrice, priceMax)
                .like(StringUtils.hasText(deliverTime), Stall::getDeliverTime, deliverTime);
        if ("priceAsc".equals(sort)) {
            wrapper.orderByAsc(Stall::getPrice);
        } else if ("priceDesc".equals(sort)) {
            wrapper.orderByDesc(Stall::getPrice);
        } else {
            wrapper.orderByDesc(Stall::getCreatedAt);
        }
        Page<Stall> result = stallMapper.selectPage(new Page<>(page, size), wrapper);
        fillPainterNames(result.getRecords());
        return result;
    }

    @Override
    public Page<Stall> listMine(long page, long size, Long painterId) {
        LambdaQueryWrapper<Stall> wrapper = new LambdaQueryWrapper<Stall>()
                .eq(Stall::getDeleted, 0)
                .eq(Stall::getPainterId, painterId)
                .orderByDesc(Stall::getCreatedAt);
        Page<Stall> result = stallMapper.selectPage(new Page<>(page, size), wrapper);
        fillUnfinishedOrderFlags(result.getRecords());
        return result;
    }

    @Override
    public Stall getById(Long id, Long viewerId) {
        Stall stall = requireExisting(id);
        boolean publiclyAvailable = Integer.valueOf(1).equals(stall.getStatus())
                && stall.getStock() != null && stall.getStock() > 0;
        boolean owner = viewerId != null && viewerId.equals(stall.getPainterId());
        boolean historicalBuyer = viewerId != null && orderInfoMapper.selectCount(
                new LambdaQueryWrapper<OrderInfo>()
                        .eq(OrderInfo::getDeleted, 0)
                        .eq(OrderInfo::getStallId, id)
                        .eq(OrderInfo::getEmployerId, viewerId)) > 0;
        if (!publiclyAvailable && !owner && !historicalBuyer) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "请先上架后查看详情");
        }
        fillPainterNames(java.util.List.of(stall));
        if (owner) {
            stall.setHasUnfinishedOrders(hasUnfinishedOrders(id));
        }
        return stall;
    }

    /** 填充画师昵称 */
    private void fillPainterNames(java.util.List<Stall> stalls) {
        if (stalls == null || stalls.isEmpty()) {
            return;
        }
        for (Stall stall : stalls) {
            if (stall.getPainterId() != null) {
                User painter = userMapper.selectById(stall.getPainterId());
                if (painter != null) {
                    stall.setPainterName(painter.getNickname());
                }
            }
        }
    }

    private void fillUnfinishedOrderFlags(List<Stall> stalls) {
        if (stalls == null || stalls.isEmpty()) {
            return;
        }
        for (Stall stall : stalls) {
            stall.setHasUnfinishedOrders(hasUnfinishedOrders(stall.getId()));
        }
    }

    private boolean hasUnfinishedOrders(Long stallId) {
        return orderInfoMapper.selectCount(
                new LambdaQueryWrapper<OrderInfo>()
                        .eq(OrderInfo::getDeleted, 0)
                        .eq(OrderInfo::getStallId, stallId)
                        .notIn(OrderInfo::getStatus, ORDER_COMPLETED, ORDER_CANCELLED)) > 0;
    }

    @Override
    public Stall create(Stall stall, Long painterId) {
        if (!CATEGORY_OPTIONS.contains(stall.getCategory())) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "橱窗类型必须从统一选项中选择");
        }
        stall.setId(null);
        stall.setPainterId(painterId);
        stall.setStatus(1);
        stall.setDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        stall.setCreatedAt(now);
        stall.setUpdatedAt(now);
        stallMapper.insert(stall);
        return stall;
    }

    @Override
    public Stall updateLimited(Long id, StallLimitedUpdateRequest request, Long painterId) {
        Stall exist = requireExisting(id);
        if (!exist.getPainterId().equals(painterId)) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
        Map<String, Object> params = readParams(exist.getParams());
        LinkedHashSet<String> subjects = new LinkedHashSet<>(asSubjects(params.get("subject")));
        for (String subject : request.getSubjects()) {
            if (subject == null || !SUBJECT_OPTIONS.contains(subject.trim())) {
                throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "题材风格必须从统一选项中选择");
            }
            subjects.add(subject.trim());
        }
        params.put("subject", new ArrayList<>(subjects));
        exist.setStock(request.getStock());
        exist.setParams(writeParams(params));
        exist.setUpdatedAt(LocalDateTime.now());
        stallMapper.updateById(exist);
        return requireExisting(id);
    }

    private Map<String, Object> readParams(String value) {
        try {
            return objectMapper.readValue(StringUtils.hasText(value) ? value : "{}", new TypeReference<>() {});
        } catch (Exception exception) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "橱窗稿件参数格式错误");
        }
    }

    private List<String> asSubjects(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).filter(StringUtils::hasText).toList();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            return List.of(text.split(","));
        }
        return List.of();
    }

    private String writeParams(Map<String, Object> params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception exception) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "橱窗稿件参数保存失败");
        }
    }

    @Override
    public Stall changeStatus(Long id, Integer status, Long painterId) {
        Stall exist = requireExisting(id);
        if (!exist.getPainterId().equals(painterId)) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
        exist.setStatus(status);
        stallMapper.updateById(exist);
        return exist;
    }

    @Override
    @Transactional
    public void delete(Long id, Long painterId) {
        Stall exist = requireExisting(id);
        if (!exist.getPainterId().equals(painterId)) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
        if (hasUnfinishedOrders(id)) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(),
                    "该橱窗存在未完成订单，暂时无法删除");
        }
        exist.setStatus(0);
        exist.setUpdatedAt(LocalDateTime.now());
        stallMapper.updateById(exist);
        // 使用 MyBatis-Plus 标准逻辑删除，确保 deleted=1 且后续列表查询自动排除。
        stallMapper.deleteById(id);
    }

    private Stall requireExisting(Long id) {
        Stall stall = stallMapper.selectById(id);
        if (stall == null || stall.getDeleted() == 1) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        return stall;
    }
}
