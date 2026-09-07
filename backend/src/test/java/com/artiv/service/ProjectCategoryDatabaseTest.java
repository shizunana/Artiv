package com.artiv.service;

import com.artiv.common.*;
import com.artiv.dto.*;
import com.artiv.entity.*;
import com.artiv.mapper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 只允许显式隔离库；每例事务回滚，禁止连接 artiv 主库。模型禁用，文件预览模拟。 */
@SpringBootTest(properties = "artiv.guide.enabled=false")
@EnabledIfEnvironmentVariable(named="ARTIV_CATEGORY_DB_TEST", matches="true")
@Transactional
class ProjectCategoryDatabaseTest {
    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) {
        String database = System.getenv("ARTIV_CATEGORY_TEST_DATABASE");
        if (database == null || !database.matches("artiv_category_test_[A-Za-z0-9_]+")) throw new IllegalStateException("Isolated test database required");
        properties.add("spring.datasource.url", () -> "jdbc:mysql://127.0.0.1:3306/" + database + "?characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true");
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired ProjectService projects;
    @Autowired ProjectMapper projectMapper;
    @Autowired UserMapper users;
    @Autowired WorkMapper works;
    @Autowired StallMapper stalls;
    @Autowired OrderService orders;
    @Autowired ApplicationService applications;
    @Autowired AdminStatsService stats;
    @Autowired ObjectMapper json;
    @MockBean OrderPreviewService previews;
    User employer, painter;

    @BeforeEach void fixture() {
        assertTrue(jdbc.queryForObject("SELECT DATABASE()", String.class).startsWith("artiv_category_test_"));
        employer = user(1); painter = user(2);
        for (int i=0;i<4;i++) {
            Work work = new Work(); work.setUserId(painter.getId()); work.setTitle("隔离测试作品"+i);
            work.setCategory("立绘"); work.setImage("/uploads/test.png"); work.setVisibility(1); work.setDeleted(0); works.insert(work);
        }
        when(previews.createWatermarkedPreview(anyString(), anyString())).thenReturn("/uploads/test-preview.png");
    }
    User user(int role) {
        User user = new User(); String unique = "cat_"+UUID.randomUUID().toString().substring(0,12);
        user.setUsername(unique); user.setNickname(unique); user.setPassword("isolated-test-not-login");
        user.setRole(role); user.setCertStatus(2); user.setStatus(0); user.setDeleted(0); user.setBalance(new BigDecimal("10000.00"));
        users.insert(user); return user;
    }
    ProjectRequest request(String category) {
        ProjectRequest request = new ProjectRequest(); request.setTitle("隔离六类测试"); request.setDescription("不写主库");
        request.setProjectCategory(category); request.setDeadline(LocalDate.now().plusDays(10));
        request.setBudgetMin(new BigDecimal("100")); request.setBudgetMax(new BigDecimal("200"));
        request.setSubject(List.of("古风")); request.setPhases("[{\"name\":\"草稿\"},{\"name\":\"终稿\"}]"); return request;
    }
    OrderInfo order(Project project) {
        Application a = new Application(); a.setProjectId(project.getId()); a.setPainterId(painter.getId());
        a.setIntro("隔离测试应征"); a.setQuote(new BigDecimal("150.00"));
        applications.create(a); return orders.createProjectOrder(project.getId(), a.getId(), employer.getId());
    }
    void deliver(OrderInfo order) {
        orders.accept(order.getId(), painter.getId());
        for (int i=0;i<2;i++) {
            OrderStageSubmitRequest submit = new OrderStageSubmitRequest(); submit.setImages(List.of("/uploads/test.png"));
            orders.submitStage(order.getId(), painter.getId(), submit);
            OrderStageReviewRequest review = new OrderStageReviewRequest(); review.setAction("approve");
            orders.reviewStage(order.getId(), employer.getId(), review);
        }
    }
    @ParameterizedTest @ValueSource(strings={"头像","立绘","角色设计","插画","平面设计","自设/OC"})
    void sixCategoriesPersistFilterAndCompleteWithoutChangingMoney(String category) throws Exception {
        Project p = projects.create(request(category), employer.getId());
        assertEquals(category, jdbc.queryForObject("SELECT project_category FROM project WHERE id=?", String.class,p.getId()));
        assertEquals(category, projectMapper.selectById(p.getId()).getProjectCategory());
        assertEquals(1, projects.listProjects(1,12,null,null,null,null,null,null,null,null,category,"古风").getTotal());
        String other = category.equals("头像") ? "插画" : "头像";
        assertEquals(0, projects.listProjects(1,12,null,null,null,null,null,null,null,null,other,null).getTotal());
        OrderInfo o = order(p);
        var snapshot = json.readTree(o.getProjectSnapshot());
        assertEquals(category, snapshot.path("projectCategory").asText()); assertFalse(snapshot.has("category"));
        assertEquals("古风", snapshot.path("subject").asText()); assertNull(o.getStallId());
        assertEquals(new BigDecimal("9850.00"), users.selectById(employer.getId()).getBalance());
        deliver(o);
        assertEquals(3, jdbc.queryForObject("SELECT status FROM order_info WHERE id=?",Integer.class,o.getId()));
        assertEquals(new BigDecimal("10150.00"), users.selectById(painter.getId()).getBalance());
        assertEquals(new BigDecimal("9850.00"), users.selectById(employer.getId()).getBalance());
        assertEquals(1, stats.priceDistribution(LocalDate.now(),LocalDate.now(),category).get("sampleSize"));
        assertEquals(0, stats.priceDistribution(LocalDate.now(),LocalDate.now(),other).get("sampleSize"));
        assertThrows(BusinessException.class,()->orders.accept(o.getId(),painter.getId()));
        assertEquals(new BigDecimal("10150.00"), users.selectById(painter.getId()).getBalance());
    }
    @Test void cancellationAndStallSourceRemainIndependent() {
        Project p = projects.create(request("角色设计"),employer.getId()); OrderInfo o=order(p);
        orders.cancel(o.getId(),employer.getId(),"隔离测试取消");
        assertEquals(new BigDecimal("10000.00"), users.selectById(employer.getId()).getBalance());
        assertThrows(BusinessException.class,()->orders.cancel(o.getId(),employer.getId(),"重复取消"));
        Stall stall = new Stall(); stall.setPainterId(painter.getId()); stall.setTitle("隔离橱窗"); stall.setCategory("Live2d");
        stall.setPrice(new BigDecimal("80.00")); stall.setStock(2); stall.setStatus(1); stall.setDeleted(0);
        stall.setPhases("[{\"name\":\"草稿\"},{\"name\":\"终稿\"}]"); stalls.insert(stall);
        OrderInfo purchase=orders.createStallPurchase(stall.getId(),employer.getId()); deliver(purchase);
        assertNull(purchase.getProjectId());
        assertEquals(0, stats.priceDistribution(LocalDate.now(),LocalDate.now(),null).get("sampleSize"));
        assertEquals("Live2d",stalls.selectById(stall.getId()).getCategory());
        var operations=stats.operations(LocalDate.now(),LocalDate.now());
        assertTrue(((List<?>)operations.get("projectTopCategories")).isEmpty());
        assertEquals(1,((List<?>)operations.get("stallTopCategories")).size());
        assertEquals(new BigDecimal("9920.00"), users.selectById(employer.getId()).getBalance());
        assertEquals(new BigDecimal("10080.00"), users.selectById(painter.getId()).getBalance());
    }
    @Test void unionPaginationSubjectAndClearUseRealDatabase() {
        Project a = projects.create(request("头像"), employer.getId());
        Project b = projects.create(request("插画"), employer.getId());
        projects.create(request("立绘"), employer.getId());
        var cats = List.of("头像","插画");
        var page1 = projects.listProjectsByCategories(1,1,null,null,null,null,null,null,null,null,cats,null);
        var page2 = projects.listProjectsByCategories(2,1,null,null,null,null,null,null,null,null,cats,null);
        assertEquals(2,page1.getTotal()); assertEquals(2,page2.getTotal());
        assertNotEquals(page1.getRecords().getFirst().getId(),page2.getRecords().getFirst().getId());
        ProjectContentUpdateRequest edit = new ProjectContentUpdateRequest();
        edit.setTitle(a.getTitle()); edit.setDescription(a.getDescription()); edit.setProjectCategory("头像");
        edit.setBudgetMin(a.getBudgetMin()); edit.setBudgetMax(a.getBudgetMax()); edit.setPhases(a.getPhases()); edit.setSubject(List.of());
        projects.updateContent(a.getId(),edit,employer.getId());
        assertNull(projectMapper.selectById(a.getId()).getSubject());
        assertEquals(1,projects.listProjects(1,12,null,null,null,null,null,null,null,null,"头像",null).getTotal());
        assertEquals(0,projects.listProjects(1,12,null,null,null,null,null,null,null,null,"头像","古风").getTotal());
        edit.setSubject(List.of("场景"));
        assertThrows(BusinessException.class, () -> projects.updateContent(a.getId(),edit,employer.getId()));
        assertNull(projectMapper.selectById(a.getId()).getSubject());
        for (String column : List.of("mode","moderation_status","deleted","status")) {
            jdbc.update("UPDATE project SET "+column+"=1 WHERE id=?",b.getId());
            assertEquals(1,projects.listProjectsByCategories(1,12,null,null,null,null,null,null,null,null,cats,null).getTotal());
            jdbc.update("UPDATE project SET "+column+"=0 WHERE id=?",b.getId());
        }
        jdbc.update("UPDATE project SET deadline=? WHERE id=?",LocalDate.now().minusDays(1),b.getId());
        assertEquals(1,projects.listProjectsByCategories(1,12,null,null,null,null,null,null,null,null,cats,null).getTotal());
    }

    @Test void editVisibilityAndInvalidValuesAreEnforcedByRealQueries() {
        Project p=projects.create(request("插画"),employer.getId());
        ProjectContentUpdateRequest edit=new ProjectContentUpdateRequest(); edit.setTitle(p.getTitle());edit.setDescription(p.getDescription());
        edit.setBudgetMin(p.getBudgetMin());edit.setBudgetMax(p.getBudgetMax());edit.setProjectCategory("自设/OC");
        edit.setPhases(p.getPhases()); projects.updateContent(p.getId(),edit,employer.getId());
        assertEquals("自设/OC",projectMapper.selectById(p.getId()).getProjectCategory());
        for(String column:List.of("mode","moderation_status","deleted")) {
            jdbc.update("UPDATE project SET "+column+"=1 WHERE id=?",p.getId());
            assertEquals(0,projects.listProjects(1,12,null,null,null,null,null,null,null,null,"自设/OC",null).getTotal());
            jdbc.update("UPDATE project SET "+column+"=0 WHERE id=?",p.getId());
        }
        for(String invalid:List.of("服设","Live2d","其他","头像,立绘")) {
            assertThrows(BusinessException.class,()->projects.create(request(invalid),employer.getId()));
            assertThrows(BusinessException.class,()->projects.listProjects(1,12,null,null,null,null,null,null,null,null,invalid,null));
        }
    }
}
