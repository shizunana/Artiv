package com.artiv.service;

import com.artiv.dto.ReportCreateRequest;
import com.artiv.entity.ReportCase;
import com.artiv.entity.ReportEvidence;
import com.artiv.entity.ReportSubmission;
import com.artiv.entity.User;
import com.artiv.mapper.ReportCaseMapper;
import com.artiv.mapper.ReportEvidenceMapper;
import com.artiv.mapper.ReportSubmissionMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.service.impl.ReportServiceImpl;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    @Mock private ReportCaseMapper reportCaseMapper;
    @Mock private ReportSubmissionMapper submissionMapper;
    @Mock private ReportEvidenceMapper evidenceMapper;
    @Mock private UserMapper userMapper;
    @Mock private AdminAuditService adminAuditService;
    @Mock private AccountRestrictionService accountRestrictionService;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks private ReportServiceImpl reportService;

    @BeforeAll
    static void initializeMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ReportCase.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ReportSubmission.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ReportEvidence.class);
    }

    @Test
    void submitCreatesOneAggregatedCaseAndStoresEvidence() {
        User target = activeUser(2L);
        when(userMapper.selectById(2L)).thenReturn(target);
        when(reportCaseMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            ((ReportCase) invocation.getArgument(0)).setId(10L);
            return 1;
        }).when(reportCaseMapper).insert(any(ReportCase.class));
        doAnswer(invocation -> {
            ((ReportSubmission) invocation.getArgument(0)).setId(20L);
            return 1;
        }).when(submissionMapper).insert(any(ReportSubmission.class));
        when(submissionMapper.selectOne(any())).thenReturn(null);
        when(submissionMapper.selectCount(any())).thenReturn(1L);

        var result = reportService.submit(1L, request());

        assertTrue(result.isRecorded());
        assertEquals(1, result.getReporterCount());
        verify(evidenceMapper).insert(any(ReportEvidence.class));
        verify(reportCaseMapper).update(any(), any());
    }

    @Test
    void submitDoesNotCountTheSameReporterTwice() {
        User target = activeUser(2L);
        ReportCase reportCase = new ReportCase();
        reportCase.setId(10L);
        reportCase.setTargetId(2L);
        reportCase.setStatus(0);
        ReportSubmission previous = new ReportSubmission();
        previous.setId(20L);
        when(userMapper.selectById(2L)).thenReturn(target);
        when(reportCaseMapper.selectOne(any())).thenReturn(reportCase);
        when(submissionMapper.selectOne(any())).thenReturn(previous);
        when(submissionMapper.selectCount(any())).thenReturn(1L);

        var result = reportService.submit(1L, request());

        assertFalse(result.isRecorded());
        assertEquals(1, result.getReporterCount());
    }

    private ReportCreateRequest request() {
        ReportCreateRequest request = new ReportCreateRequest();
        request.setTargetUserId(2L);
        request.setCategory("HARASSMENT");
        request.setReason("对方持续发送骚扰信息，需要管理员核查处理。 ");
        request.setImages(List.of("/uploads/report.png"));
        return request;
    }

    private User activeUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("target");
        user.setNickname("目标用户");
        user.setDeleted(0);
        return user;
    }
}
