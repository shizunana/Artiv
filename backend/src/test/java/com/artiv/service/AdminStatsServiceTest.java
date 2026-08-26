package com.artiv.service;

import com.artiv.entity.User;
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
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

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
}
