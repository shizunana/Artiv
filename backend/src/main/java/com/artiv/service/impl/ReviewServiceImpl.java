package com.artiv.service.impl;

import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.OrderReviewContext;
import com.artiv.dto.ReviewAppealRequest;
import com.artiv.dto.ReviewListItem;
import com.artiv.dto.ReviewRequest;
import com.artiv.dto.ReviewStatistics;
import com.artiv.dto.admin.ReviewAppealDetail;
import com.artiv.entity.OrderInfo;
import com.artiv.entity.Review;
import com.artiv.entity.ReviewAppeal;
import com.artiv.entity.ReviewImage;
import com.artiv.entity.User;
import com.artiv.mapper.OrderInfoMapper;
import com.artiv.mapper.ReviewAppealMapper;
import com.artiv.mapper.ReviewImageMapper;
import com.artiv.mapper.ReviewMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.service.AdminAuditService;
import com.artiv.service.MessageService;
import com.artiv.service.ReviewService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.artiv.common.SystemMessageText.markTitle;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final int COMPLETED = 3;
    private static final int APPEAL_PENDING = 0;
    private static final int APPEAL_APPROVED = 1;
    private static final int APPEAL_REJECTED = 2;
    private static final int MAX_IMAGES = 6;

    private final ReviewMapper reviewMapper;
    private final ReviewImageMapper reviewImageMapper;
    private final ReviewAppealMapper reviewAppealMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;
    private final AdminAuditService adminAuditService;

    @Override
    public Page<ReviewListItem> listPublic(long page, long size, Long userId, String role, Long viewerId) {
        String normalizedRole = normalizeRole(role);
        User target = userMapper.selectById(userId);
        if (!isActive(target)) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        Page<Review> source = reviewMapper.selectPublicPage(
                new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 50)), userId, normalizedRole);
        Page<ReviewListItem> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(toListItems(source.getRecords(), viewerId));
        return result;
    }

    @Override
    @Transactional
    public ReviewListItem create(Long orderId, Long userId, ReviewRequest request) {
        OrderInfo order = orderInfoMapper.selectByIdForUpdate(orderId);
        if (order == null || !Integer.valueOf(0).equals(order.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (!userId.equals(order.getEmployerId()) && !userId.equals(order.getPainterId())) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
        if (!Integer.valueOf(COMPLETED).equals(order.getStatus()) || order.getCompletedAt() == null) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "仅已完成订单可以评价");
        }
        LocalDateTime deadline = order.getCompletedAt().plusDays(7);
        if (!LocalDateTime.now().isBefore(deadline)) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "该订单的7天评价期已结束");
        }
        if (reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, orderId).eq(Review::getFromId, userId)) > 0) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "你已评价过该订单");
        }

        List<String> images = normalizeImages(request.getImages());
        Review review = new Review();
        review.setOrderId(orderId);
        review.setFromId(userId);
        review.setToId(userId.equals(order.getEmployerId()) ? order.getPainterId() : order.getEmployerId());
        review.setScore(request.getScore());
        review.setContent(normalizeText(request.getContent(), 500));
        review.setDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        review.setCreatedAt(now);
        review.setUpdatedAt(now);
        try {
            reviewMapper.insert(review);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "你已评价过该订单");
        }
        for (int index = 0; index < images.size(); index++) {
            ReviewImage image = new ReviewImage();
            image.setReviewId(review.getId());
            image.setImageUrl(images.get(index));
            image.setSortOrder(index);
            image.setCreatedAt(now);
            reviewImageMapper.insert(image);
        }
        messageService.createNotification(review.getToId(), 0, "REVIEW_RECEIVED", null,
                "您收到一条关于订单" + markTitle(displayOrderTitle(order))
                        + "的交易评价，可在公开主页的合作评价中查看。");
        return toListItems(List.of(review), userId).get(0);
    }

    @Override
    public OrderReviewContext getOrderReviewContext(OrderInfo order, Long userId) {
        if (order == null || (!userId.equals(order.getEmployerId()) && !userId.equals(order.getPainterId()))) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
        OrderReviewContext context = new OrderReviewContext();
        if (!Integer.valueOf(COMPLETED).equals(order.getStatus()) || order.getCompletedAt() == null) {
            context.setStatus("NOT_AVAILABLE");
            return context;
        }
        context.setDeadline(order.getCompletedAt().plusDays(7));
        Review existing = reviewMapper.selectOne(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, order.getId()).eq(Review::getFromId, userId).last("LIMIT 1"));
        if (existing != null) {
            context.setStatus("SUBMITTED");
            if (Integer.valueOf(0).equals(existing.getDeleted())) {
                context.setReview(toListItems(List.of(existing), userId).get(0));
            }
        } else if (LocalDateTime.now().isBefore(context.getDeadline())) {
            context.setStatus("PENDING");
        } else {
            context.setStatus("EXPIRED");
        }
        return context;
    }

    @Override
    public ReviewStatistics getStatistics(Long userId, String role) {
        return getStatistics(List.of(userId), role).getOrDefault(userId, new ReviewStatistics(null, 0));
    }

    @Override
    public Map<Long, ReviewStatistics> getStatistics(List<Long> userIds, String role) {
        String normalizedRole = normalizeRole(role);
        List<Long> ids = userIds == null ? List.of() : userIds.stream().filter(Objects::nonNull).distinct().toList();
        Map<Long, ScoreAccumulator> accumulators = new LinkedHashMap<>();
        ids.forEach(id -> accumulators.put(id, new ScoreAccumulator()));
        if (ids.isEmpty()) return Map.of();
        List<Review> reviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .in(Review::getToId, ids).eq(Review::getDeleted, 0));
        Map<Long, OrderInfo> orders = loadOrders(reviews);
        for (Review review : reviews) {
            OrderInfo order = orders.get(review.getOrderId());
            boolean matches = order != null && ("painter".equals(normalizedRole)
                    ? review.getToId().equals(order.getPainterId())
                    : review.getToId().equals(order.getEmployerId()));
            if (matches) accumulators.get(review.getToId()).add(review.getScore());
        }
        Map<Long, ReviewStatistics> result = new LinkedHashMap<>();
        accumulators.forEach((id, values) -> result.put(id, values.toStatistics()));
        return result;
    }

    @Override
    @Transactional
    public ReviewAppeal submitAppeal(Long reviewId, Long userId, ReviewAppealRequest request) {
        Review review = reviewMapper.selectByIdForUpdate(reviewId);
        if (review == null || !Integer.valueOf(0).equals(review.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        OrderInfo order = orderInfoMapper.selectById(review.getOrderId());
        if (order == null || !userId.equals(review.getToId()) || !userId.equals(order.getPainterId())) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "仅收到该评价的画师可以申诉");
        }
        if (reviewAppealMapper.selectCount(new LambdaQueryWrapper<ReviewAppeal>()
                .eq(ReviewAppeal::getReviewId, reviewId)) > 0) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "该评价已经提交过申诉");
        }
        ReviewAppeal appeal = new ReviewAppeal();
        appeal.setReviewId(reviewId);
        appeal.setApplicantId(userId);
        appeal.setReason(normalizeRequired(request.getReason()));
        appeal.setStatus(APPEAL_PENDING);
        LocalDateTime now = LocalDateTime.now();
        appeal.setCreatedAt(now);
        appeal.setUpdatedAt(now);
        try {
            reviewAppealMapper.insert(appeal);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "该评价已经提交过申诉");
        }
        messageService.createNotification(userId, 0, "REVIEW_APPEAL_SUBMITTED", null,
                "订单" + markTitle(displayOrderTitle(order))
                        + "的评价删除申诉已提交，管理员处理后会发送系统通知。");
        return appeal;
    }

    @Override
    public ReviewAppeal getOwnAppeal(Long reviewId, Long userId) {
        Review review = reviewMapper.selectIncludingDeletedByIds(List.of(reviewId)).stream()
                .findFirst().orElseThrow(() -> new BusinessException(ResultCode.RESOURCE_NOT_FOUND));
        OrderInfo order = orderInfoMapper.selectById(review.getOrderId());
        if (order == null || !userId.equals(review.getToId()) || !userId.equals(order.getPainterId())) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
        ReviewAppeal appeal = reviewAppealMapper.selectOne(new LambdaQueryWrapper<ReviewAppeal>()
                .eq(ReviewAppeal::getReviewId, reviewId).last("LIMIT 1"));
        if (appeal == null) throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        return appeal;
    }

    @Override
    public Page<ReviewAppealDetail> listAppeals(long page, long size, Integer status, String keyword,
                                                LocalDate startDate, LocalDate endDate) {
        if (status != null && (status < APPEAL_PENDING || status > APPEAL_REJECTED)) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Page<ReviewAppeal> source = reviewAppealMapper.selectAdminPage(
                new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 100)), status, normalizedKeyword,
                startDate == null ? null : startDate.atStartOfDay(),
                endDate == null ? null : endDate.plusDays(1).atStartOfDay());
        Page<ReviewAppealDetail> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(toAppealDetails(source.getRecords()));
        return result;
    }

    @Override
    public ReviewAppealDetail getAppeal(Long appealId) {
        ReviewAppeal appeal = reviewAppealMapper.selectById(appealId);
        if (appeal == null) throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        return toAppealDetails(List.of(appeal)).get(0);
    }

    @Override
    @Transactional
    public ReviewAppealDetail resolveAppeal(Long appealId, Long adminId, boolean approve, String reason) {
        ReviewAppeal appeal = reviewAppealMapper.selectByIdForUpdate(appealId);
        if (appeal == null) throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        if (!Integer.valueOf(APPEAL_PENDING).equals(appeal.getStatus())) {
            throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "该申诉已经处理");
        }
        Review review = reviewMapper.selectByIdForUpdate(appeal.getReviewId());
        if (review == null) throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        OrderInfo order = orderInfoMapper.selectById(review.getOrderId());
        String orderTitle = markTitle(displayOrderTitle(order));
        String decisionReason = normalizeRequired(reason);
        Map<String, Object> before = Map.of("appealStatus", appeal.getStatus(), "reviewDeleted", review.getDeleted());
        if (approve) {
            if (reviewMapper.hideById(review.getId()) != 1) {
                throw new BusinessException(ResultCode.STATUS_FLOW_INVALID.getCode(), "该评价已被隐藏");
            }
            review.setDeleted(1);
            review.setUpdatedAt(LocalDateTime.now());
        }
        appeal.setStatus(approve ? APPEAL_APPROVED : APPEAL_REJECTED);
        appeal.setAdminId(adminId);
        appeal.setDecisionReason(decisionReason);
        appeal.setDecidedAt(LocalDateTime.now());
        appeal.setUpdatedAt(LocalDateTime.now());
        reviewAppealMapper.updateById(appeal);
        Map<String, Object> after = Map.of("appealStatus", appeal.getStatus(), "reviewDeleted", review.getDeleted());
        adminAuditService.record(adminId, approve ? "REVIEW_APPEAL_APPROVED" : "REVIEW_APPEAL_REJECTED",
                "REVIEW_APPEAL", appealId, before, after, decisionReason);
        messageService.createNotification(appeal.getApplicantId(), 0,
                approve ? "REVIEW_APPEAL_APPROVED" : "REVIEW_APPEAL_REJECTED", null,
                "订单" + orderTitle + (approve ? "的评价删除申诉已通过。" : "的评价删除申诉未通过。")
                        + "处理说明：" + decisionReason);
        if (approve) {
            messageService.createNotification(review.getFromId(), 0, "REVIEW_HIDDEN_AFTER_APPEAL", null,
                    "您对订单" + orderTitle + "提交的一条交易评价经申诉审核后已被隐藏。处理说明：" + decisionReason);
        }
        return toAppealDetails(List.of(appeal)).get(0);
    }

    @Override
    public long countPendingAppeals() {
        return reviewAppealMapper.selectCount(new LambdaQueryWrapper<ReviewAppeal>()
                .eq(ReviewAppeal::getStatus, APPEAL_PENDING));
    }

    private List<ReviewListItem> toListItems(List<Review> reviews, Long viewerId) {
        if (reviews == null || reviews.isEmpty()) return List.of();
        Map<Long, OrderInfo> orders = loadOrders(reviews);
        Map<Long, User> reviewers = userMapper.selectBatchIds(reviews.stream().map(Review::getFromId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<Long> reviewIds = reviews.stream().map(Review::getId).toList();
        Map<Long, List<String>> images = reviewImageMapper.selectList(new LambdaQueryWrapper<ReviewImage>()
                        .in(ReviewImage::getReviewId, reviewIds)
                        .orderByAsc(ReviewImage::getSortOrder).orderByAsc(ReviewImage::getId))
                .stream().collect(Collectors.groupingBy(ReviewImage::getReviewId, LinkedHashMap::new,
                        Collectors.mapping(ReviewImage::getImageUrl, Collectors.toList())));
        Map<Long, ReviewAppeal> appeals = viewerId == null ? Map.of() : reviewAppealMapper.selectList(
                        new LambdaQueryWrapper<ReviewAppeal>().in(ReviewAppeal::getReviewId, reviewIds))
                .stream().collect(Collectors.toMap(ReviewAppeal::getReviewId, Function.identity()));
        List<ReviewListItem> result = new ArrayList<>();
        for (Review review : reviews) {
            OrderInfo order = orders.get(review.getOrderId());
            User reviewer = reviewers.get(review.getFromId());
            ReviewAppeal appeal = appeals.get(review.getId());
            ReviewListItem item = new ReviewListItem();
            item.setReviewId(review.getId());
            item.setOrderId(review.getOrderId());
            item.setReviewerId(review.getFromId());
            item.setReviewerNickname(displayName(reviewer));
            item.setReviewerAvatar(reviewer == null ? null : reviewer.getAvatar());
            item.setReviewerRole(order != null && review.getFromId().equals(order.getPainterId()) ? "painter" : "employer");
            item.setScore(review.getScore());
            item.setContent(review.getContent());
            item.setImages(images.getOrDefault(review.getId(), List.of()));
            item.setSourceType(order != null && order.getProjectId() != null ? "project" : "stall");
            item.setSourceTitle(order == null ? "历史订单" : displayOrderTitle(order));
            item.setCreatedAt(review.getCreatedAt());
            boolean targetPainter = order != null && viewerId != null && viewerId.equals(review.getToId())
                    && viewerId.equals(order.getPainterId());
            item.setCanAppeal(targetPainter && appeal == null && Integer.valueOf(0).equals(review.getDeleted()));
            item.setAppealStatus(targetPainter && appeal != null ? appeal.getStatus() : null);
            result.add(item);
        }
        return result;
    }

    private List<ReviewAppealDetail> toAppealDetails(List<ReviewAppeal> appeals) {
        if (appeals == null || appeals.isEmpty()) return List.of();
        Map<Long, Review> reviews = reviewMapper.selectIncludingDeletedByIds(
                        appeals.stream().map(ReviewAppeal::getReviewId).distinct().toList())
                .stream().collect(Collectors.toMap(Review::getId, Function.identity()));
        Map<Long, ReviewListItem> reviewItems = toListItems(new ArrayList<>(reviews.values()), null).stream()
                .collect(Collectors.toMap(ReviewListItem::getReviewId, Function.identity()));
        Map<Long, User> applicants = userMapper.selectBatchIds(appeals.stream().map(ReviewAppeal::getApplicantId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return appeals.stream().map(appeal -> {
            User applicant = applicants.get(appeal.getApplicantId());
            ReviewAppealDetail detail = new ReviewAppealDetail();
            detail.setAppeal(appeal);
            detail.setReview(reviewItems.get(appeal.getReviewId()));
            detail.setApplicantId(appeal.getApplicantId());
            detail.setApplicantNickname(displayName(applicant));
            detail.setApplicantAvatar(applicant == null ? null : applicant.getAvatar());
            return detail;
        }).toList();
    }

    private Map<Long, OrderInfo> loadOrders(List<Review> reviews) {
        List<Long> orderIds = reviews.stream().map(Review::getOrderId).filter(Objects::nonNull).distinct().toList();
        if (orderIds.isEmpty()) return Map.of();
        return orderInfoMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(OrderInfo::getId, Function.identity()));
    }

    private List<String> normalizeImages(List<String> requestImages) {
        if (requestImages == null || requestImages.isEmpty()) return List.of();
        if (requestImages.size() > MAX_IMAGES) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "评价图片最多6张");
        }
        return requestImages.stream().map(value -> {
            String image = value == null ? "" : value.trim();
            if (!image.startsWith("/uploads/") || image.contains("..") || image.length() > 255) {
                throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "评价图片必须使用本站上传文件");
            }
            return image;
        }).toList();
    }

    private String normalizeText(String value, int max) {
        if (!StringUtils.hasText(value)) return null;
        String result = value.trim();
        if (result.length() > max) throw new BusinessException(ResultCode.PARAM_INVALID);
        return result;
    }

    private String normalizeRequired(String value) {
        String result = normalizeText(value, 500);
        if (!StringUtils.hasText(result)) throw new BusinessException(ResultCode.PARAM_INVALID);
        return result;
    }

    private String normalizeRole(String role) {
        String result = StringUtils.hasText(role) ? role.trim().toLowerCase() : "painter";
        if (!List.of("painter", "employer").contains(result)) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "评价身份仅支持 painter 或 employer");
        }
        return result;
    }

    private String displayName(User user) {
        if (user == null) return "已注销用户";
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    private String displayOrderTitle(OrderInfo order) {
        if (order == null) return "历史订单";
        if (order.getProjectId() != null) {
            return StringUtils.hasText(order.getProjectTitle()) ? order.getProjectTitle() : "企划订单 #" + order.getId();
        }
        return StringUtils.hasText(order.getStallTitle()) ? order.getStallTitle() : "橱窗订单 #" + order.getId();
    }

    private boolean isActive(User user) {
        return user != null && Integer.valueOf(0).equals(user.getDeleted()) && Integer.valueOf(0).equals(user.getStatus());
    }

    private static final class ScoreAccumulator {
        private long count;
        private long sum;

        void add(Integer score) {
            if (score != null) {
                count++;
                sum += score;
            }
        }

        ReviewStatistics toStatistics() {
            if (count == 0) return new ReviewStatistics(null, 0);
            double average = BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP).doubleValue();
            return new ReviewStatistics(average, count);
        }
    }
}
