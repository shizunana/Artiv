package com.artiv.service;

import com.artiv.entity.User;
import com.artiv.entity.OrderInfo;
import com.artiv.entity.Project;
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
        project.setCategory("立绘");
        when(orderInfoMapper.selectList(any())).thenReturn(List.of(order));
        when(projectMapper.selectBatchIds(any())).thenReturn(List.of(project));

        Map<String, Object> result = adminStatsService.priceDistribution(
                LocalDate.now(), LocalDate.now(), "立绘");

        assertEquals(1, result.get("sampleSize"));
        assertEquals(false, result.get("sufficient"));
        assertEquals("INSUFFICIENT_SAMPLE", result.get("reason"));
    }
}
