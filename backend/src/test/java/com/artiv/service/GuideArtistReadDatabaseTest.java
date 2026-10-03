package com.artiv.service;

import com.artiv.guide.client.GuideCancellation;
import com.artiv.guide.tool.GuidePublicReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** 显式启用的本机只读核对，不建用户、不补标签、不写业务表、不调用模型。 */
@SpringBootTest(properties = "artiv.guide.enabled=false")
@EnabledIfEnvironmentVariable(named = "ARTIV_GUIDE_ARTIST_READ_TEST", matches = "true")
@Transactional(readOnly = true)
class GuideArtistReadDatabaseTest {
    @Autowired ArtistService artists;
    @Autowired GuidePublicReader reader;
    @Autowired JdbcTemplate jdbc;
    private static final List<String> CATEGORIES = List.of("头像", "Q版全身", "半身像", "立绘", "组合页", "服设", "壁纸", "表情包", "Live2d", "平面设计");

    @Test void guideCountsMatchPublicWorksOrAvailableStallsWhileHallRemainsWorkOnly() {
        for (String category : CATEGORIES) {
            long expected = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM user u WHERE u.cert_status=2 AND u.status=0 AND u.deleted=0
                    AND (EXISTS (SELECT 1 FROM work w WHERE w.user_id=u.id AND w.visibility=1 AND w.deleted=0 AND w.category=?)
                    OR EXISTS (SELECT 1 FROM stall s WHERE s.painter_id=u.id AND s.deleted=0 AND s.status=1 AND s.stock>0 AND s.category=?))
                    """, Long.class, category, category);
            var page = artists.listGuideArtists(1, 5, category, null, null);
            assertEquals(expected, page.getTotal(), category);
            assertEquals(Math.min(5, expected), page.getRecords().size(), category);
            assertEquals(page.getRecords().size(), page.getRecords().stream().map(a -> a.getId()).distinct().count());
            assertTrue(page.getRecords().stream().allMatch(a -> a.getCategories().contains(category)), category);
            long hallCount = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM user u WHERE u.cert_status=2 AND u.status=0 AND u.deleted=0
                    AND EXISTS (SELECT 1 FROM work w WHERE w.user_id=u.id AND w.visibility=1 AND w.deleted=0 AND w.category=?)
                    """, Long.class, category);
            assertEquals(hallCount, artists.listArtists(1, 5, category, null, null).getTotal());
        }
    }
    @Test void styleUsesOnlyPersonalTagsAndBatchRemainsBounded() {
        long expected = jdbc.queryForObject("""
                SELECT COUNT(*) FROM user WHERE cert_status=2 AND status=0 AND deleted=0
                AND FIND_IN_SET(?, REPLACE(COALESCE(tags,''),' ',''))>0
                """, Long.class, "日系");
        assertEquals(expected, artists.listGuideArtists(1, 5, null, "日系", null).getTotal());
        var batch = reader.artistsByCategory(CATEGORIES, null, new GuideCancellation());
        assertTrue(batch.references().size() <= 11);
        assertEquals(batch.references().size(), batch.references().stream().map(r -> r.kind()+":"+r.id()).distinct().count());
    }
}
