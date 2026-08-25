package com.artiv.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.artiv.common.BusinessException;
import com.artiv.dto.OrderStageReviewRequest;
import com.artiv.dto.OrderStageSubmitRequest;
import com.artiv.entity.OrderInfo;
import com.artiv.entity.OrderStage;
import com.artiv.entity.Application;
import com.artiv.entity.Project;
import com.artiv.entity.Stall;
import com.artiv.entity.User;
import com.artiv.mapper.OrderEventMapper;
import com.artiv.mapper.OrderInfoMapper;
import com.artiv.mapper.OrderStageMapper;
import com.artiv.mapper.ApplicationMapper;
import com.artiv.mapper.ProjectMapper;
import com.artiv.mapper.StallMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderInfoMapper orderInfoMapper;
    @Mock private OrderStageMapper orderStageMapper;
    @Mock private OrderEventMapper orderEventMapper;
    @Mock private ApplicationMapper applicationMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private StallMapper stallMapper;
    @Mock private UserMapper userMapper;
    @Mock private MessageService messageService;
    @Mock private OrderPreviewService orderPreviewService;
    @Mock private AccountRestrictionService accountRestrictionService;
    @Mock private ReviewService reviewService;
    @Spy private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @InjectMocks private OrderServiceImpl orderService;

    @Test
    void adminBanCancelsUnfinishedOrderRefundsAndRestoresStock() {
        OrderInfo order = new OrderInfo();
        order.setId(41L);
        order.setEmployerId(15L);
        order.setPainterId(2L);
        order.setStallId(9L);
        order.setAmount(new BigDecimal("320.00"));
        order.setStatus(1);
        order.setDeleted(0);
        when(orderInfoMapper.selectList(any())).thenReturn(List.of(order));
        when(orderInfoMapper.selectByIdForUpdate(41L)).thenReturn(order);
        User employer = activeUser(15L, new BigDecimal("10.00"));
        when(userMapper.selectByIdForUpdate(15L)).thenReturn(employer);
        Stall stall = activeStall();
        stall.setStock(0);
        when(stallMapper.selectByIdForUpdate(9L)).thenReturn(stall);

        int cancelled = orderService.cancelUnfinishedForBannedUser(2L, 3L, "管理员封禁用户：违规交易");

        assertEquals(1, cancelled);
        assertEquals(new BigDecimal("330.00"), employer.getBalance());
        assertEquals(1, stall.getStock());
        assertEquals(4, order.getStatus());
        verify(orderInfoMapper).updateById(order);
    }

    @Test
    void createStallPurchaseLocksBalanceAndDecrementsStock() {
        Stall stall = activeStall();
        when(stallMapper.selectByIdForUpdate(9L)).thenReturn(stall);
        User employer = activeUser(15L, new BigDecimal("1000.00"));
        User painter = activeUser(2L, BigDecimal.ZERO);
        when(userMapper.selectByIdForUpdate(15L)).thenReturn(employer);
        when(userMapper.selectById(2L)).thenReturn(painter);
        org.mockito.Mockito.doAnswer(invocation -> {
            OrderInfo order = invocation.getArgument(0);
            order.setId(77L);
            return 1;
        }).when(orderInfoMapper).insert(any(OrderInfo.class));

        OrderInfo result = orderService.createStallPurchase(9L, 15L);

        assertEquals(new BigDecimal("712.00"), employer.getBalance());
        assertEquals(2, stall.getStock());
        assertEquals(77L, result.getId());
        assertEquals("角色立绘委托", result.getStallTitle());
        verify(userMapper).updateById(employer);
        verify(stallMapper).updateById(stall);
        verify(messageService).createStallPurchaseCommunication(eq(result), eq(stall));
    }

    @Test
    void createProjectOrderUsesApplicationQuoteAfterDeadlineAndDoesNotChangeProjectState() throws Exception {
        Application application = new Application();
        application.setId(31L);
        application.setProjectId(8L);
        application.setPainterId(2L);
        application.setQuote(new BigDecimal("350.00"));
        application.setIntro("擅长相关题材");
        application.setStatus(0);
        application.setActive(1);
        application.setDeleted(0);
        Project project = new Project();
        project.setId(8L);
        project.setEmployerId(15L);
        project.setTitle("企划测试");
        project.setDescription("测试需求");
        project.setBudget(new BigDecimal("500.00"));
        project.setImages("/uploads/project.png,/uploads/other.png");
        project.setStatus(0);
        project.setDeleted(0);
        project.setDeadline(LocalDate.of(2020, 8, 30));
        project.setPhases("[{\"name\":\"草图\"},{\"name\":\"终稿\"}]");
        User employer = activeUser(15L, new BigDecimal("1000.00"));
        User painter = activeUser(2L, BigDecimal.ZERO);
        painter.setCertStatus(2);
        when(applicationMapper.selectByIdForUpdate(31L)).thenReturn(application);
        when(projectMapper.selectById(8L)).thenReturn(project);
        when(orderInfoMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.selectByIdForUpdate(15L)).thenReturn(employer);
        when(userMapper.selectById(2L)).thenReturn(painter);
        org.mockito.Mockito.doAnswer(invocation -> {
            OrderInfo order = invocation.getArgument(0);
            order.setId(88L);
            return 1;
        }).when(orderInfoMapper).insert(any(OrderInfo.class));

        OrderInfo result = orderService.createProjectOrder(8L, 31L, 15L);

        assertEquals(new BigDecimal("650.00"), employer.getBalance());
        assertEquals(31L, result.getApplicationId());
        assertEquals("企划测试", result.getProjectTitle());
        assertEquals("/uploads/project.png", result.getProjectCover());
        assertEquals("[{\"name\":\"草图\"},{\"name\":\"终稿\"}]", result.getPhaseSnapshot());
        assertEquals("2020-08-30", objectMapper.readTree(result.getProjectSnapshot()).path("deadline").asText());
        assertEquals(0, project.getStatus());
        verify(projectMapper, never()).updateById(any(Project.class));
        verify(messageService).createProjectOrderCommunication(result, project);
    }

    @Test
    void createProjectOrderRejectsSecondActiveOrderForSameApplication() {
        Application application = new Application();
        application.setId(31L);
        application.setProjectId(8L);
        application.setPainterId(2L);
        application.setQuote(new BigDecimal("350.00"));
        application.setStatus(0);
        application.setActive(1);
        application.setDeleted(0);
        Project project = new Project();
        project.setId(8L);
        project.setEmployerId(15L);
        project.setStatus(0);
        project.setDeleted(0);
        when(applicationMapper.selectByIdForUpdate(31L)).thenReturn(application);
        when(projectMapper.selectById(8L)).thenReturn(project);
        when(orderInfoMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> orderService.createProjectOrder(8L, 31L, 15L));

        verify(userMapper, never()).selectByIdForUpdate(15L);
        verify(orderInfoMapper, never()).insert(any(OrderInfo.class));
    }

    @Test
    void getDetailRejectsUserOutsideParticipants() {
        OrderInfo order = new OrderInfo();
        order.setId(77L);
        order.setEmployerId(15L);
        order.setPainterId(2L);
        order.setDeleted(0);
        when(orderInfoMapper.selectById(77L)).thenReturn(order);

        assertThrows(com.artiv.common.BusinessException.class,
                () -> orderService.getDetail(77L, 99L));
    }

    @Test
    void getDetailBackfillsLegacyStallPreviewAndHidesOriginalFromEmployer() {
        OrderInfo order = activeOrder(1);
        order.setStallId(9L);
        OrderStage legacy = new OrderStage();
        legacy.setId(12L);
        legacy.setOrderId(77L);
        legacy.setStageIndex(0);
        legacy.setSubmissionNo(1);
        legacy.setAttachments("/uploads/legacy-preview.png");
        legacy.setOriginals("/uploads/original.png");
        legacy.setDeleted(0);
        when(orderInfoMapper.selectById(77L)).thenReturn(order);
        when(orderStageMapper.selectList(any())).thenReturn(List.of(legacy));
        when(orderPreviewService.isGeneratedPreview("/uploads/legacy-preview.png")).thenReturn(false);
        when(orderPreviewService.ensureWatermarkedPreview(
                "/uploads/legacy-preview.png", "/uploads/original.png"))
                .thenReturn("/uploads/order-previews/fixed.jpg");

        var detail = orderService.getDetail(77L, 15L);

        assertEquals("/uploads/order-previews/fixed.jpg", detail.getSubmissions().get(0).getAttachments());
        assertNull(detail.getSubmissions().get(0).getOriginals());
        verify(orderStageMapper).updateById(legacy);
    }

    @Test
    void submitStageSeparatesPreviewAndOriginalForStallAndProjectOrders() {
        OrderInfo stallOrder = activeOrder(1);
        stallOrder.setId(77L);
        stallOrder.setStallId(9L);
        stallOrder.setStallTitle("夏日角色立绘");
        stallOrder.setPhaseSnapshot("[{\"name\":\"初稿\"}]");
        OrderInfo projectOrder = activeOrder(1);
        projectOrder.setId(78L);
        projectOrder.setProjectId(8L);
        projectOrder.setProjectTitle("夏日角色立绘长期企划测试");
        projectOrder.setPhaseSnapshot("[{\"name\":\"初稿\"}]");
        when(orderInfoMapper.selectByIdForUpdate(77L)).thenReturn(stallOrder);
        when(orderInfoMapper.selectByIdForUpdate(78L)).thenReturn(projectOrder);
        when(orderStageMapper.selectCount(any())).thenReturn(0L);
        when(orderPreviewService.createWatermarkedPreview("/uploads/original.png"))
                .thenReturn("/uploads/order-previews/preview.jpg");
        User painter = activeUser(2L, BigDecimal.ZERO);
        painter.setNickname("Enana");
        when(userMapper.selectById(2L)).thenReturn(painter);
        OrderStageSubmitRequest request = new OrderStageSubmitRequest();
        request.setImage("/uploads/original.png");
        request.setContent("阶段交付");

        orderService.submitStage(77L, 2L, request);
        orderService.submitStage(78L, 2L, request);

        ArgumentCaptor<OrderStage> captor = ArgumentCaptor.forClass(OrderStage.class);
        verify(orderStageMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        captor.getAllValues().forEach(stage -> {
            assertEquals("/uploads/order-previews/preview.jpg", stage.getAttachments());
            assertEquals("/uploads/original.png", stage.getOriginals());
        });
        verify(messageService).createOrderEventMessage(eq(stallOrder), eq(2L), eq("ORDER_STAGE_SUBMITTED"),
                eq("画师「Enana」已提交订单「夏日角色立绘」的「初稿」，请验收。"));
        verify(messageService).createOrderEventMessage(eq(projectOrder), eq(2L), eq("ORDER_STAGE_SUBMITTED"),
                eq("画师「Enana」已提交订单「夏日角色立绘长期企划...」的「初稿」，请验收。"));
    }

    @Test
    void acceptRequiresAssignedCertifiedPainter() {
        OrderInfo order = new OrderInfo();
        order.setId(77L);
        order.setEmployerId(15L);
        order.setPainterId(2L);
        order.setProjectTitle("夏日角色立绘长期企划测试");
        order.setStatus(0);
        order.setDeleted(0);
        when(orderInfoMapper.selectByIdForUpdate(77L)).thenReturn(order);
        User painter = activeUser(2L, BigDecimal.ZERO);
        painter.setNickname("Enana");
        painter.setCertStatus(2);
        when(userMapper.selectById(2L)).thenReturn(painter);

        OrderInfo result = orderService.accept(77L, 2L);

        assertEquals(1, result.getStatus());
        verify(orderInfoMapper).updateById(result);
        verify(messageService).createOrderEventMessage(eq(result), eq(2L), eq("ORDER_ACCEPTED"),
                eq("画师「Enana」已确认接单，订单「夏日角色立绘长期企划...」已进入创作阶段。"));
    }

    @Test
    void painterCannotRequestAfterSale() {
        OrderInfo order = activeOrder(1);
        when(orderInfoMapper.selectByIdForUpdate(77L)).thenReturn(order);

        assertThrows(BusinessException.class,
                () -> orderService.requestAfterSale(77L, 2L, "画师申请售后"));
    }

    @Test
    void employerCancelsPendingProjectOrderWithClearProjectMessage() {
        OrderInfo order = activeOrder(0);
        order.setProjectId(8L);
        order.setProjectTitle("企划测试");
        order.setAmount(new BigDecimal("500.00"));
        User employer = activeUser(15L, BigDecimal.ZERO);
        when(orderInfoMapper.selectByIdForUpdate(77L)).thenReturn(order);
        when(userMapper.selectByIdForUpdate(15L)).thenReturn(employer);

        orderService.cancel(77L, 15L, "需求调整");

        verify(messageService).createOrderEventMessage(eq(order), eq(15L), eq("ORDER_CANCELLED"),
                eq("订单「企划测试」已取消，托管金额已退回：需求调整"));
    }

    @Test
    void employerCancelsPendingStallOrderWithClearStockRestoreMessage() {
        OrderInfo order = activeOrder(0);
        order.setStallId(9L);
        order.setStallTitle("头像橱窗");
        order.setAmount(new BigDecimal("288.00"));
        User employer = activeUser(15L, BigDecimal.ZERO);
        Stall stall = activeStall();
        when(orderInfoMapper.selectByIdForUpdate(77L)).thenReturn(order);
        when(userMapper.selectByIdForUpdate(15L)).thenReturn(employer);
        when(stallMapper.selectByIdForUpdate(9L)).thenReturn(stall);

        orderService.cancel(77L, 15L, "不再需要");

        assertEquals(4, stall.getStock());
        verify(messageService).createOrderEventMessage(eq(order), eq(15L), eq("ORDER_CANCELLED"),
                eq("订单「头像橱窗」已取消，托管金额已退回：不再需要"));
    }

    @Test
    void employerCanCompleteFromFirstSubmittedStageAndPainterIsPaidImmediately() {
        OrderInfo order = activeOrder(2);
        order.setAmount(new BigDecimal("288.00"));
        order.setStallTitle("夏日角色立绘");
        order.setCurrentStage(0);
        order.setPhaseSnapshot("[{\"name\":\"初稿\",\"percent\":40},{\"name\":\"终稿\",\"percent\":60}]");
        OrderStage stage = new OrderStage();
        stage.setId(8L);
        stage.setOrderId(77L);
        stage.setStageIndex(0);
        stage.setSubmissionNo(1);
        stage.setName("初稿");
        stage.setStatus(0);
        User painter = activeUser(2L, new BigDecimal("10.00"));
        when(orderInfoMapper.selectByIdForUpdate(77L)).thenReturn(order);
        when(orderStageMapper.selectList(any())).thenReturn(List.of(stage));
        when(userMapper.selectByIdForUpdate(2L)).thenReturn(painter);
        when(userMapper.incrementBalance(2L, new BigDecimal("288.00"))).thenReturn(1);
        OrderStageReviewRequest request = new OrderStageReviewRequest();
        request.setAction("complete");

        OrderInfo result = orderService.reviewStage(77L, 15L, request);

        assertEquals(3, result.getStatus());
        assertEquals(new BigDecimal("298.00"), painter.getBalance());
        assertEquals(1, stage.getStatus());
        verify(userMapper).incrementBalance(2L, new BigDecimal("288.00"));
        verify(messageService).createOrderEventMessage(eq(result), eq(15L), eq("ORDER_COMPLETED"),
                eq("企划方已确认订单「夏日角色立绘」当前交付并直接完成交易，后续创作阶段已跳过，款项已结算。"));
    }

    @Test
    void completingProjectOrderWithdrawsItsApplication() {
        OrderInfo order = activeOrder(2);
        order.setProjectId(8L);
        order.setApplicationId(31L);
        order.setAmount(new BigDecimal("288.00"));
        OrderStage stage = new OrderStage();
        stage.setId(8L);
        stage.setOrderId(77L);
        stage.setStageIndex(0);
        stage.setSubmissionNo(1);
        stage.setName("final");
        stage.setStatus(0);
        User painter = activeUser(2L, BigDecimal.ZERO);
        Application application = new Application();
        application.setId(31L);
        application.setStatus(0);
        application.setActive(1);
        application.setDeleted(0);
        when(orderInfoMapper.selectByIdForUpdate(77L)).thenReturn(order);
        when(orderStageMapper.selectList(any())).thenReturn(List.of(stage));
        when(userMapper.selectByIdForUpdate(2L)).thenReturn(painter);
        when(userMapper.incrementBalance(2L, new BigDecimal("288.00"))).thenReturn(1);
        when(applicationMapper.selectByIdForUpdate(31L)).thenReturn(application);
        OrderStageReviewRequest request = new OrderStageReviewRequest();
        request.setAction("complete");

        orderService.reviewStage(77L, 15L, request);

        assertEquals(3, application.getStatus());
        assertEquals(0, application.getActive());
        assertEquals("ORDER_COMPLETED", application.getEndedReason());
        assertEquals(true, application.getEndedAt() != null);
        verify(applicationMapper).updateById(application);
    }

    @Test
    void approvingFinalConfiguredStageCompletesOrderAndPaysPainter() {
        OrderInfo order = activeOrder(2);
        order.setAmount(new BigDecimal("300.00"));
        order.setProjectTitle("夏日角色立绘");
        order.setCurrentStage(2);
        order.setPhaseSnapshot("[{\"name\":\"初稿\"},{\"name\":\"修改\"},{\"name\":\"终稿\"}]");
        OrderStage stage = new OrderStage();
        stage.setId(9L);
        stage.setOrderId(77L);
        stage.setStageIndex(2);
        stage.setSubmissionNo(1);
        stage.setName("终稿");
        stage.setStatus(0);
        User painter = activeUser(2L, BigDecimal.ZERO);
        when(orderInfoMapper.selectByIdForUpdate(77L)).thenReturn(order);
        when(orderStageMapper.selectList(any())).thenReturn(List.of(stage));
        when(userMapper.selectByIdForUpdate(2L)).thenReturn(painter);
        when(userMapper.incrementBalance(2L, new BigDecimal("300.00"))).thenReturn(1);
        OrderStageReviewRequest request = new OrderStageReviewRequest();
        request.setAction("approve");

        OrderInfo result = orderService.reviewStage(77L, 15L, request);

        assertEquals(3, result.getStatus());
        assertEquals(new BigDecimal("300.00"), painter.getBalance());
        verify(messageService).createOrderEventMessage(eq(result), eq(15L), eq("ORDER_COMPLETED"),
                eq("企划方已确认订单「夏日角色立绘」的最终交付，订单已完成并结算。"));
    }

    @Test
    void requestingRevisionIncludesOrderTitleAndRealStageName() {
        OrderInfo order = activeOrder(2);
        order.setProjectTitle("角色立绘企划");
        order.setCurrentStage(0);
        OrderStage stage = new OrderStage();
        stage.setId(10L);
        stage.setOrderId(77L);
        stage.setStageIndex(0);
        stage.setSubmissionNo(1);
        stage.setName("初稿");
        stage.setStatus(0);
        when(orderInfoMapper.selectByIdForUpdate(77L)).thenReturn(order);
        when(orderStageMapper.selectList(any())).thenReturn(List.of(stage));
        OrderStageReviewRequest request = new OrderStageReviewRequest();
        request.setAction("revise");
        request.setRemark("请调整构图");

        orderService.reviewStage(77L, 15L, request);

        assertEquals(1, order.getStatus());
        assertEquals(2, stage.getStatus());
        verify(messageService).createOrderEventMessage(eq(order), eq(15L), eq("ORDER_REVISION_REQUESTED"),
                eq("企划方要求修改订单「角色立绘企划」的「初稿」：请调整构图"));
    }

    @Test
    void approvingNonFinalStageIncludesOrderTitleAndRealStageName() {
        OrderInfo order = activeOrder(2);
        order.setStallTitle("头像橱窗");
        order.setCurrentStage(0);
        order.setPhaseSnapshot("[{\"name\":\"初稿\"},{\"name\":\"终稿\"}]");
        OrderStage stage = new OrderStage();
        stage.setId(11L);
        stage.setOrderId(77L);
        stage.setStageIndex(0);
        stage.setSubmissionNo(1);
        stage.setName("初稿");
        stage.setStatus(0);
        when(orderInfoMapper.selectByIdForUpdate(77L)).thenReturn(order);
        when(orderStageMapper.selectList(any())).thenReturn(List.of(stage));
        OrderStageReviewRequest request = new OrderStageReviewRequest();
        request.setAction("approve");

        orderService.reviewStage(77L, 15L, request);

        assertEquals(1, order.getStatus());
        assertEquals(1, order.getCurrentStage());
        verify(messageService).createOrderEventMessage(eq(order), eq(15L), eq("ORDER_STAGE_APPROVED"),
                eq("企划方已通过订单「头像橱窗」的「初稿」，已进入下一阶段。"));
    }

    @Test
    void listMineCanSeparateBoughtOrdersFromAcceptedOrders() {
        when(orderInfoMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.listMine(1, 10, 2L, "stall", "all", "employer");
        orderService.listMine(1, 10, 2L, "stall", "all", "painter");

        verify(orderInfoMapper, org.mockito.Mockito.times(2)).selectPage(any(), any());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void listMineSortsActiveThenPendingThenTerminalAndNewestInsideEachGroup() {
        when(orderInfoMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.listMine(1, 10, 2L, "stall", "all", "employer");

        ArgumentCaptor<LambdaQueryWrapper> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(orderInfoMapper).selectPage(any(), wrapperCaptor.capture());
        String sql = sqlSegment(wrapperCaptor.getValue());
        assertTrue(sql.contains("CASE WHEN status IN (1, 2, 5) THEN 1"));
        assertTrue(sql.contains("WHEN status = 0 THEN 2"));
        assertTrue(sql.contains("created_at DESC"));
        assertTrue(sql.contains("id DESC"));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void listMineReviewFilterKeepsOnlyUnreviewedOrdersInsideSevenDays() {
        when(orderInfoMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.listMine(1, 10, 2L, "stall", "review", "painter");

        ArgumentCaptor<LambdaQueryWrapper> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(orderInfoMapper).selectPage(any(), wrapperCaptor.capture());
        LambdaQueryWrapper<OrderInfo> wrapper = wrapperCaptor.getValue();
        String sql = sqlSegment(wrapper);
        assertTrue(sql.contains("completed_at"));
        assertTrue(sql.contains("NOT EXISTS"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(3));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void listMineAfterSaleFilterOnlyKeepsActiveDisputes() {
        when(orderInfoMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.listMine(1, 10, 2L, "stall", "afterSale", "painter");

        ArgumentCaptor<LambdaQueryWrapper> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(orderInfoMapper).selectPage(any(), wrapperCaptor.capture());
        LambdaQueryWrapper<OrderInfo> wrapper = wrapperCaptor.getValue();
        sqlSegment(wrapper);
        assertTrue(wrapper.getParamNameValuePairs().containsValue(5));
        assertFalse(wrapper.getParamNameValuePairs().containsValue(4));
    }

    private String sqlSegment(LambdaQueryWrapper<OrderInfo> wrapper) {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), OrderInfo.class);
        return wrapper.getCustomSqlSegment();
    }

    private Stall activeStall() {
        Stall stall = new Stall();
        stall.setId(9L);
        stall.setPainterId(2L);
        stall.setTitle("角色立绘委托");
        stall.setCover("/uploads/cover.png");
        stall.setPrice(new BigDecimal("288.00"));
        stall.setStock(3);
        stall.setStatus(1);
        stall.setDeleted(0);
        stall.setPhases("[{\"name\":\"初稿\",\"percent\":50},{\"name\":\"交付\",\"percent\":50}]");
        return stall;
    }

    private User activeUser(Long id, BigDecimal balance) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        user.setBalance(balance);
        user.setStatus(0);
        user.setDeleted(0);
        return user;
    }

    private OrderInfo activeOrder(int status) {
        OrderInfo order = new OrderInfo();
        order.setId(77L);
        order.setEmployerId(15L);
        order.setPainterId(2L);
        order.setStatus(status);
        order.setCurrentStage(0);
        order.setDeleted(0);
        return order;
    }
}
