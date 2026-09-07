package com.artiv.service;

import com.artiv.entity.User;
import com.artiv.entity.Application;
import com.artiv.entity.OrderInfo;
import com.artiv.entity.Project;
import com.artiv.common.BusinessException;
import com.artiv.mapper.AdminActionLogMapper;
import com.artiv.mapper.ApplicationMapper;
import com.artiv.mapper.ArtistCertificationMapper;
import com.artiv.mapper.OrderDisputeMapper;
import com.artiv.mapper.OrderInfoMapper;
import com.artiv.mapper.ProjectMapper;
import com.artiv.mapper.StallMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.mapper.WorkMapper;
import com.artiv.service.impl.AdminStatsServiceImpl;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStatsServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private ApplicationMapper applicationMapper;
    @Mock private OrderInfoMapper orderInfoMapper;
    @Mock private StallMapper stallMapper;
    @Mock private WorkMapper workMapper;
    @Mock private ArtistCertificationMapper certificationMapper;
    @Mock private OrderDisputeMapper disputeMapper;
    @Mock private AdminActionLogMapper auditMapper;
    @Mock private ReviewService reviewService;
    @Mock private ReportService reportService;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private AdminStatsServiceImpl adminStatsService;

    @Test
    void supplyStatisticsUseCertificationInsteadOfRegistrationRole() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), User.class);
        when(projectMapper.selectList(any())).thenReturn(List.of());
        when(stallMapper.selectList(any())).thenReturn(List.of());
        when(userMapper.selectList(any())).thenReturn(List.of());
        when(orderInfoMapper.selectList(any())).thenReturn(List.of());
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(orderInfoMapper.selectCount(any())).thenReturn(0L);

        adminStatsService.demand(LocalDate.now(), LocalDate.now());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<User>> queryCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(userMapper).selectList(queryCaptor.capture());
        String sql = queryCaptor.getValue().getCustomSqlSegment();
        assertTrue(sql.contains("cert_status"));
        assertFalse(sql.contains("role"));
    }

    @Test
    void newSnapshotKeyWinsAndUnknownTypesAreReportedWithoutRewriting() {
        OrderInfo first = new OrderInfo(); first.setId(1L); first.setProjectId(1L); first.setAmount(new BigDecimal("150"));
        first.setProjectSnapshot("{\"projectCategory\":\"角色设计\",\"category\":\"头像\"}");
        OrderInfo second = new OrderInfo(); second.setId(2L); second.setProjectId(2L); second.setAmount(new BigDecimal("200"));
        second.setProjectSnapshot("{\"projectCategory\":null,\"category\":\"头像\"}");
        when(orderInfoMapper.selectList(any())).thenReturn(List.of(first,second));
        var response = adminStatsService.priceDistribution(LocalDate.now(),LocalDate.now(),"角色设计");
        assertEquals(1,response.get("sampleSize"));
        assertEquals("角色设计",response.get("projectCategory"));
        assertEquals(1L,response.get("unknownProjectCategoryCount"));
        assertFalse(response.containsKey("category"));
        assertEquals(0,adminStatsService.priceDistribution(LocalDate.now(),LocalDate.now(),"头像").get("sampleSize"));
        assertEquals(2,adminStatsService.priceDistribution(LocalDate.now(),LocalDate.now(),null).get("sampleSize"));
        org.mockito.Mockito.verifyNoInteractions(projectMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void supplyMappingDeduplicatesPaintersButStallPieKeepsOriginalCategories() {
        User painter = new User();painter.setId(8L);
        com.artiv.entity.Work a=new com.artiv.entity.Work();a.setUserId(8L);a.setCategory("立绘");
        com.artiv.entity.Work b=new com.artiv.entity.Work();b.setUserId(8L);b.setCategory("Q版全身");
        com.artiv.entity.Stall stall=new com.artiv.entity.Stall();stall.setId(1L);stall.setCategory("立绘");
        when(userMapper.selectList(any())).thenReturn(List.of(painter));
        when(workMapper.selectList(any())).thenReturn(List.of(a,b));
        when(stallMapper.selectList(any())).thenReturn(List.of(stall));
        var response=adminStatsService.demand(LocalDate.now(),LocalDate.now());
        var rows=(List<Map<String,Object>>)response.get("supplyDemandGap");
        assertEquals(6,rows.size());
        var standing=rows.stream().filter(r->"立绘".equals(r.get("name"))).findFirst().orElseThrow();
        assertEquals(1L,standing.get("certifiedPainters"));assertEquals(1L,standing.get("activeStalls"));
        var pie=(List<Map<String,Object>>)response.get("stallCategories");
        assertEquals(1,pie.size());assertEquals("立绘",pie.getFirst().get("name"));assertEquals(1L,pie.getFirst().get("value"));
    }

    @Test
    void demandTrendCountsApplicationsByOwnCreatedAt() {
        LocalDate start = LocalDate.of(2026, 8, 10);
        LocalDate end = LocalDate.of(2026, 8, 27);
        Project project = new Project();
        project.setId(7L);
        project.setProjectCategory("头像");
        project.setCreatedAt(start.atTime(10, 0));

        Application applicationForOlderProject = new Application();
        applicationForOlderProject.setId(11L);
        applicationForOlderProject.setProjectId(99L);
        applicationForOlderProject.setDeleted(0);
        applicationForOlderProject.setCreatedAt(start.plusDays(4).atTime(12, 0));

        when(projectMapper.selectList(any())).thenReturn(List.of(project));
        when(applicationMapper.selectList(any()))
                .thenReturn(List.of(), List.of(applicationForOlderProject));
        when(orderInfoMapper.selectList(any())).thenReturn(List.of());
        when(stallMapper.selectList(any())).thenReturn(List.of());
        when(userMapper.selectList(any())).thenReturn(List.of());
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(orderInfoMapper.selectCount(any())).thenReturn(0L);

        Map<String, Object> result = adminStatsService.demand(start, end);

        assertEquals("DAY", result.get("granularity"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> trend = (List<Map<String, Object>>) result.get("trend");
        assertEquals(18, trend.size());
        assertEquals(start, trend.get(0).get("startDate"));
        assertEquals(start, trend.get(0).get("endDate"));
        assertEquals(1L, trend.get(4).get("secondary"));
        assertEquals(1L, trend.stream().mapToLong(item -> ((Number) item.get("secondary")).longValue()).sum());
    }

    @Test
    void rejectsStatisticsBeforePlatformStartDate() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                adminStatsService.operations(LocalDate.of(2026, 8, 9), LocalDate.of(2026, 8, 10)));

        assertEquals("统计起始日期不能早于2026-08-10", exception.getMessage());
    }

    @Test
    void rejectsStatisticsLongerThanOneYear() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                adminStatsService.operations(LocalDate.of(2026, 8, 10), LocalDate.of(2027, 8, 11)));

        assertEquals("统计日期范围必须为1至366天", exception.getMessage());
    }

    @Test
    void rejectsFutureStatisticsEndDate() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        BusinessException exception = assertThrows(BusinessException.class, () ->
                adminStatsService.operations(tomorrow, tomorrow));

        assertEquals("统计结束日期不能晚于今天", exception.getMessage());
    }

    @Test
    void priceDistributionUsesCompletedProjectOrderAmountsAndSnapshotCategory() {
        List<OrderInfo> orders = new ArrayList<>();
        for (int index = 1; index <= 10; index++) {
            OrderInfo order = new OrderInfo();
            order.setId((long) index);
            order.setProjectId(100L + index);
            order.setAmount(BigDecimal.valueOf(index * 100L));
            order.setProjectSnapshot("{\"category\":\"头像\"}");
            order.setCompletedAt(LocalDateTime.now());
            orders.add(order);
        }
        OrderInfo ignoredCategory = new OrderInfo();
        ignoredCategory.setId(99L);
        ignoredCategory.setProjectId(999L);
        ignoredCategory.setAmount(BigDecimal.valueOf(9999));
        ignoredCategory.setProjectSnapshot("{\"category\":\"立绘\"}");
        orders.add(ignoredCategory);
        when(orderInfoMapper.selectList(any())).thenReturn(orders);

        Map<String, Object> result = adminStatsService.priceDistribution(
                LocalDate.now(), LocalDate.now(), "头像");

        assertEquals(10, result.get("sampleSize"));
        assertEquals(true, result.get("sufficient"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> buckets = (List<Map<String, Object>>) result.get("buckets");
        assertEquals(List.of(1, 2, 4, 2, 1), buckets.stream().map(item -> item.get("orderCount")).toList());
        assertEquals(List.of(10, 20, 40, 20, 10), buckets.stream().map(item -> item.get("targetRate")).toList());
    }

    @Test
    void priceDistributionFallsBackToCurrentProjectCategoryForLegacySnapshot() {
        OrderInfo order = new OrderInfo();
        order.setId(1L);
        order.setProjectId(7L);
        order.setAmount(BigDecimal.valueOf(500));
        order.setCompletedAt(LocalDateTime.now());
        Project project = new Project();
        project.setId(7L);
        project.setProjectCategory("立绘");
        when(orderInfoMapper.selectList(any())).thenReturn(List.of(order));
        when(projectMapper.selectBatchIds(any())).thenReturn(List.of(project));

        Map<String, Object> result = adminStatsService.priceDistribution(
                LocalDate.now(), LocalDate.now(), "立绘");

        assertEquals(1, result.get("sampleSize"));
        assertEquals(false, result.get("sufficient"));
        assertEquals("INSUFFICIENT_SAMPLE", result.get("reason"));
    }
}
