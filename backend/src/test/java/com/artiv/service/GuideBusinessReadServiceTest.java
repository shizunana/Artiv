package com.artiv.service;

import com.artiv.common.BusinessException;
import com.artiv.entity.*;
import com.artiv.mapper.*;
import com.artiv.guide.client.*;
import com.artiv.guide.tool.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class GuideBusinessReadServiceTest {
    ProjectService projects = mock(ProjectService.class);
    OrderInfoMapper orders = mock(OrderInfoMapper.class);
    OrderStageMapper stages = mock(OrderStageMapper.class);
    InvitationMapper invitations = mock(InvitationMapper.class);
    ProjectMapper projectMapper = mock(ProjectMapper.class);
    ObjectMapper json = new ObjectMapper();
    GuideBusinessReadService reader = new GuideBusinessReadService(projects, orders, stages, invitations, projectMapper, json);
    @BeforeEach void metadata() {
        var config = new com.baomidou.mybatisplus.core.MybatisConfiguration(); config.setMapUnderscoreToCamelCase(true);
        for (Class<?> type : List.of(OrderInfo.class, OrderStage.class, Invitation.class, Project.class))
            com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(config, "guide-tests"), type);
    }
    @AfterEach void noWrites() {
        for (Object mapper : List.of(orders, stages, invitations, projectMapper))
            assertTrue(mockingDetails(mapper).getInvocations().stream().allMatch(i -> i.getMethod().getName().startsWith("select")), "read path must never write");
    }
    Project project() {
        var p = new Project(); p.setId(11L); p.setEmployerId(1L); p.setDeleted(0); p.setStatus(0); p.setDeadline(LocalDate.now());
        p.setTitle("本人企划"); p.setContentEditable(false); p.setDeletable(false); p.setDeleteBlockReason("存在关联订单"); p.setDescription("SECRET_DESCRIPTION"); p.setImages("SECRET_IMAGE"); return p;
    }
    OrderInfo order(int status) {
        var o = new OrderInfo(); o.setId(21L); o.setEmployerId(1L); o.setPainterId(2L); o.setDeleted(0); o.setStatus(status);
        o.setCurrentStage(1); o.setPhaseSnapshot("[{\"name\":\"构思\"},{\"name\":\"线稿核对\"}]");
        o.setProjectSnapshot("SECRET_SNAPSHOT"); o.setAfterSaleReason("SECRET_REASON"); return o;
    }
    Invitation invitation(int type, int status) {
        var i = new Invitation(); i.setId(31L); i.setProjectId(11L); i.setPainterId(1L); i.setPainterHidden(0); i.setDeleted(0);
        i.setInvitationType(type); i.setStatus(status); i.setMessage("SECRET_MESSAGE"); return i;
    }
    @Test void projectUsesOriginalEligibilityAndExcludesPrivateBody() throws Exception {
        when(projects.getGuideSummary(11L, 1L)).thenReturn(project());
        String body = json.writeValueAsString(reader.read(1, "my_project", 11L, 1));
        assertTrue(body.contains("存在关联订单")); assertTrue(body.contains("\"contentEditable\":false")); assertFalse(body.contains("SECRET"));
        verify(projects).getGuideSummary(11L, 1L); verifyNoMoreInteractions(projects);
    }
    @Test void foreignProjectFailsClosedEvenIfDependencyReturnsIt() {
        var p = project(); p.setEmployerId(8L); when(projects.getGuideSummary(11L, 1L)).thenReturn(p);
        assertThrows(BusinessException.class, () -> reader.read(1, "my_project", 11L, 1));
    }
    @Test void ownedProjectPageIsBoundedAndNotAllTodos() throws Exception {
        when(projects.listMine(2, 5, 1L, null)).thenReturn(new Page<Project>(2, 5, 17).setRecords(List.of(project())));
        when(projects.getGuideSummary(11L, 1L)).thenReturn(project());
        var data = json.valueToTree(reader.read(1, "my_projects", null, 2).data());
        assertEquals(5, data.path("pageSize").asInt()); assertTrue(data.path("hasMore").asBoolean()); assertTrue(data.path("scope").asText().contains("不是全平台"));
    }
    @Test void orderSqlScopesBothParticipantsAndProjectsOnlyNecessaryColumns() throws Exception {
        when(orders.selectPage(any(Page.class), any())).thenAnswer(i -> {
            var q = i.<Wrapper<OrderInfo>>getArgument(1); String sql = q.getSqlSegment();
            assertTrue(sql.contains("employer_id") && sql.contains("painter_id") && sql.contains(" OR ") && sql.contains("deleted"), sql);
            assertFalse(q.getSqlSelect().contains("amount")); assertFalse(q.getSqlSelect().contains("project_snapshot"));
            return new Page<OrderInfo>(1, 5, 1).setRecords(List.of(order(2)));
        });
        String employer = json.writeValueAsString(reader.read(1, "my_order", 21L, 1));
        String painter = json.writeValueAsString(reader.read(2, "my_order", 21L, 1));
        assertTrue(employer.contains("再人工决定通过")); assertTrue(painter.contains("等待企划方验收"));
        assertTrue(employer.contains("线稿核对")); assertFalse(employer.contains("SECRET"));
        assertThrows(BusinessException.class, () -> reader.read(3, "my_order", 21L, 1));
    }
    @Test void allOrderStatusesAndRevisionAreExplainedWithoutMakingAnAction() throws Exception {
        for (int status = 0; status <= 5; status++) {
            when(orders.selectPage(any(Page.class), any())).thenReturn(new Page<OrderInfo>(1, 5, 1).setRecords(List.of(order(status))));
            String body = json.writeValueAsString(reader.read(2, "my_order", 21L, 1)); assertFalse(body.contains("未知状态"));
        }
        var stage = new OrderStage(); stage.setStatus(2); when(stages.selectOne(any())).thenReturn(stage);
        when(orders.selectPage(any(Page.class), any())).thenReturn(new Page<OrderInfo>(1, 5, 1).setRecords(List.of(order(1))));
        assertTrue(json.writeValueAsString(reader.read(2, "my_order", 21L, 1)).contains("重新提交"));
    }
    @Test void invitationsDistinguishStoredPendingFromExpiredWithoutWriting() throws Exception {
        var i = invitation(1, 0); var p = project(); p.setMode(0); p.setModerationStatus(0); p.setDeadline(LocalDate.now().minusDays(1));
        when(invitations.selectPage(any(Page.class), any())).thenReturn(new Page<Invitation>(1, 5, 1).setRecords(List.of(i)));
        when(projectMapper.selectOne(any())).thenReturn(p);
        var data = json.valueToTree(reader.read(1, "my_invitation", 31L, 1).data()).path("records").get(0);
        assertEquals("待处理", data.path("storedStatus").asText()); assertTrue(data.path("pendingStoredButUnavailable").asBoolean());
        assertEquals(0, i.getStatus()); assertFalse(data.toString().contains("SECRET"));
        p.setDeadline(LocalDate.now()); assertFalse(json.valueToTree(reader.read(1, "my_invitation", 31L, 1).data()).path("records").get(0).path("pendingStoredButUnavailable").asBoolean());
    }
    @Test void hiddenForeignDeletedInvitationsAndMissingRecordsAreRejected() {
        for (int which = 0; which < 3; which++) {
            var i = invitation(0, 0); if (which == 0) i.setPainterHidden(1); if (which == 1) i.setPainterId(2L); if (which == 2) i.setDeleted(1);
            when(invitations.selectPage(any(Page.class), any())).thenReturn(new Page<Invitation>(1, 5, 1).setRecords(List.of(i)));
            assertThrows(BusinessException.class, () -> reader.read(1, "my_invitation", 31L, 1));
        }
        verifyNoInteractions(projectMapper);
    }
    @Test void identityCannotBeProvidedThroughToolArgumentsOrMissingContext() throws Exception {
        var registry = new GuideToolRegistry(json, new GuideRuleBook(json), mock(GuidePublicReader.class), reader);
        for (String extra : List.of(",\"actorId\":1", ",\"owner\":1", ",\"role\":3", ",\"userId\":1"))
            assertThrows(GuideModelException.class, () -> registry.execute(new GuideModelClient.ToolCall("a", "read_artiv", "{\"operation\":\"my_orders\"" + extra + "}"), new GuideCancellation(), 1L));
        assertThrows(GuideModelException.class, () -> registry.execute(new GuideModelClient.ToolCall("a", "read_artiv", "{\"operation\":\"my_orders\"}"), new GuideCancellation()));
        verifyNoInteractions(orders);
    }
    @Test void workerUsesExplicitOwnerAndPrivateReferencesAreNotNavigationLinks() throws Exception {
        var service = mock(GuideBusinessReadService.class);
        when(service.read(7, "my_orders", null, 1)).thenReturn(new GuidePublicReader.ReadResult(Map.of("records", List.of()), List.of(new GuideReference("private_query", "orders", "本人订单", "空", "now"))));
        var registry = new GuideToolRegistry(json, new GuideRuleBook(json), mock(GuidePublicReader.class), service);
        var result = registry.execute(new GuideModelClient.ToolCall("a", "read_artiv", "{\"operation\":\"my_orders\"}"), new GuideCancellation(), 7L);
        assertTrue(result.content().contains("private_query")); verify(service).read(7, "my_orders", null, 1);
    }
}
