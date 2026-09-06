package com.artiv.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** 显式启用的本机只读集成检查；不创建业务样例，不向模型发送任何业务数据。 */
@SpringBootTest(properties = {"artiv.guide.enabled=false", "mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl"})
@EnabledIfEnvironmentVariable(named = "ARTIV_GUIDE_READ_DB_TEST", matches = "true")
class GuideBusinessReadDatabaseTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired GuideBusinessReadService reader;
    @Test void actualAdministratorReadsLeaveDatabaseAndUploadsUnchanged() throws Exception {
        Long owner = jdbc.queryForObject("SELECT id FROM user WHERE username='admin' AND role=3 AND status=0 AND deleted=0", Long.class);
        assertNotNull(owner);
        var before = checksums(); var filesBefore = files();
        for (String operation : List.of("my_projects", "my_orders", "my_invitations")) {
            var result = reader.read(owner, operation, null, 1);
            assertEquals("private_query", result.references().getFirst().kind());
        }
        assertEquals(before, checksums(), "数据库业务表不应因读取改变；并行人工写入也会导致此检查失败");
        assertEquals(filesBefore, files(), "读取不得生成水印或修改上传文件");
    }
    private Map<String, Object> checksums() {
        Map<String, Object> result = new TreeMap<>();
        for (String table : jdbc.queryForList("SHOW TABLES", String.class)) {
            if (!table.matches("[a-z_]+")) throw new IllegalStateException("Unexpected table name");
            var row = jdbc.queryForMap("CHECKSUM TABLE `" + table + "`");
            assertNotNull(row.get("Checksum"), "表校验和必须可用"); result.put(table, row.get("Checksum"));
        }
        return result;
    }
    private Map<String, String> files() throws Exception {
        var result = new TreeMap<String, String>(); var root = Path.of("uploads");
        if (!Files.exists(root)) return result;
        try (var paths = Files.walk(root)) {
            for (var path : paths.filter(Files::isRegularFile).toList())
                result.put(root.relativize(path).toString(), Files.size(path) + ":" + Files.getLastModifiedTime(path));
        }
        return result;
    }
}
