package com.artiv.service;

import com.artiv.common.BusinessException;
import com.artiv.dto.ReviewAppealRequest;
import com.artiv.dto.ReviewRequest;
import com.artiv.entity.OrderInfo;
import com.artiv.entity.Review;
import com.artiv.entity.ReviewAppeal;
import com.artiv.entity.User;
import com.artiv.mapper.OrderInfoMapper;
import com.artiv.mapper.ReviewAppealMapper;
import com.artiv.mapper.ReviewImageMapper;
import com.artiv.mapper.ReviewMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock ReviewMapper reviewMapper;
    @Mock ReviewImageMapper reviewImageMapper;
    @Mock ReviewAppealMapper reviewAppealMapper;
    @Mock OrderInfoMapper orderInfoMapper;
    @Mock UserMapper userMapper;
    @Mock MessageService messageService;
    @Mock AdminAuditService adminAuditService;
    @InjectMocks ReviewServiceImpl reviewService;

    @Test
    void completedOrderParticipantCanReviewAndTargetIsDerivedByServer() {
        OrderInfo order = completedOrder();
        when(orderInfoMapper.selectByIdForUpdate(31L)).thenReturn(order);
        when(reviewMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId(81L);
            return 1;
        }).when(reviewMapper).insert(any(Review.class));
        when(orderInfoMapper.selectBatchIds(any())).thenReturn(List.of(order));
        User reviewer = new User(); reviewer.setId(15L); reviewer.setNickname("企划方");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(reviewer));
        ReviewRequest request = new ReviewRequest();
        request.setScore(5);
        request.setContent("合作顺利");
        request.setImages(List.of("/uploads/review.png"));

        var result = reviewService.create(31L, 15L, request);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewMapper).insert(captor.capture());
        assertEquals(2L, captor.getValue().getToId());
        assertEquals(5, result.getScore());
        verify(messageService).createNotification(eq(2L), eq(0), eq("REVIEW_RECEIVED"),
                org.mockito.ArgumentMatchers.isNull(),
                eq("您收到一条关于订单「头像委托」的交易评价，可在公开主页的合作评价中查看。"));
        verify(reviewImageMapper).insert(any(com.artiv.entity.ReviewImage.class));
    }

    @Test
    void outsiderAndExpiredOrderCannotBeReviewed() {
        OrderInfo order = completedOrder();
        when(orderInfoMapper.selectByIdForUpdate(31L)).thenReturn(order);
        assertThrows(BusinessException.class, () -> reviewService.create(31L, 99L, request()));

        order.setCompletedAt(LocalDateTime.now().minusDays(7));
        assertThrows(BusinessException.class, () -> reviewService.create(31L, 15L, request()));
        verify(reviewMapper, never()).insert(any(Review.class));
    }

    @Test
    void sameParticipantCanOnlyReviewOrderOnce() {
        when(orderInfoMapper.selectByIdForUpdate(31L)).thenReturn(completedOrder());
        when(reviewMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> reviewService.create(31L, 15L, request()));

        verify(reviewMapper, never()).insert(any(Review.class));
    }

    @Test
    void ratingStatisticsKeepPainterAndEmployerRolesSeparate() {
        Review receivedAsPainter = review(1L, 41L, 15L, 2L, 5);
        Review receivedAsEmployer = review(2L, 42L, 20L, 2L, 1);
        when(reviewMapper.selectList(any())).thenReturn(List.of(receivedAsPainter, receivedAsEmployer));
        OrderInfo painterOrder = completedOrder(); painterOrder.setId(41L);
        OrderInfo employerOrder = completedOrder(); employerOrder.setId(42L); employerOrder.setEmployerId(2L); employerOrder.setPainterId(20L);
        when(orderInfoMapper.selectBatchIds(any())).thenReturn(List.of(painterOrder, employerOrder));

        var painterStats = reviewService.getStatistics(2L, "painter");
        var employerStats = reviewService.getStatistics(2L, "employer");

        assertEquals(5.0, painterStats.getAverageScore());
        assertEquals(1, painterStats.getReviewCount());
        assertEquals(1.0, employerStats.getAverageScore());
        assertEquals(1, employerStats.getReviewCount());
    }

    @Test
    void onlyReviewedPainterCanAppealOnce() {
        Review review = review(81L, 31L, 15L, 2L, 2);
        when(reviewMapper.selectByIdForUpdate(81L)).thenReturn(review);
        when(orderInfoMapper.selectById(31L)).thenReturn(completedOrder());
        when(reviewAppealMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> { ((ReviewAppeal) invocation.getArgument(0)).setId(9L); return 1; })
                .when(reviewAppealMapper).insert(any(ReviewAppeal.class));
        ReviewAppealRequest request = new ReviewAppealRequest(); request.setReason("评价包含无关攻击内容");

        ReviewAppeal result = reviewService.submitAppeal(81L, 2L, request);

        assertEquals(9L, result.getId());
        assertEquals(0, result.getStatus());
        verify(messageService).createNotification(eq(2L), eq(0), eq("REVIEW_APPEAL_SUBMITTED"),
                org.mockito.ArgumentMatchers.isNull(),
                eq("订单「头像委托」的评价删除申诉已提交，管理员处理后会发送系统通知。"));
        assertThrows(BusinessException.class, () -> reviewService.submitAppeal(81L, 15L, request));
    }

    @Test
    void approvedAppealUsesDedicatedLogicalHideUpdate() {
        Review review = review(81L, 31L, 15L, 2L, 2);
        ReviewAppeal appeal = new ReviewAppeal();
        appeal.setId(9L);
        appeal.setReviewId(81L);
        appeal.setApplicantId(2L);
        appeal.setStatus(0);
        when(reviewAppealMapper.selectByIdForUpdate(9L)).thenReturn(appeal);
        when(reviewMapper.selectByIdForUpdate(81L)).thenReturn(review);
        when(orderInfoMapper.selectById(31L)).thenReturn(completedOrder());
        when(reviewMapper.hideById(81L)).thenReturn(1);
        when(reviewMapper.selectIncludingDeletedByIds(any())).thenReturn(List.of(review));
        when(orderInfoMapper.selectBatchIds(any())).thenReturn(List.of(completedOrder()));
        when(reviewImageMapper.selectList(any())).thenReturn(List.of());
        User employer = new User(); employer.setId(15L); employer.setNickname("企划方");
        User painter = new User(); painter.setId(2L); painter.setNickname("画师");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(employer, painter));

        var result = reviewService.resolveAppeal(9L, 3L, true, "申诉成立");

        assertEquals(1, result.getAppeal().getStatus());
        assertEquals(1, review.getDeleted());
        verify(reviewMapper).hideById(81L);
        verify(reviewMapper, never()).updateById(any(Review.class));
        verify(adminAuditService).record(eq(3L), eq("REVIEW_APPEAL_APPROVED"),
                eq("REVIEW_APPEAL"), eq(9L), any(), any(), eq("申诉成立"));
    }

    private ReviewRequest request() {
        ReviewRequest request = new ReviewRequest(); request.setScore(4); return request;
    }

    private OrderInfo completedOrder() {
        OrderInfo order = new OrderInfo();
        order.setId(31L); order.setEmployerId(15L); order.setPainterId(2L);
        order.setStatus(3); order.setDeleted(0); order.setCompletedAt(LocalDateTime.now().minusDays(1));
        order.setStallId(8L); order.setStallTitle("头像委托");
        return order;
    }

    private Review review(Long id, Long orderId, Long fromId, Long toId, int score) {
        Review review = new Review();
        review.setId(id); review.setOrderId(orderId); review.setFromId(fromId); review.setToId(toId);
        review.setScore(score); review.setDeleted(0); review.setCreatedAt(LocalDateTime.now());
        return review;
    }
}
