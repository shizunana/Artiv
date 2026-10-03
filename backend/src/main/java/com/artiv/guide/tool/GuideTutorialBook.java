package com.artiv.guide.tool;

import com.artiv.guide.client.GuideModelException;
import com.artiv.mapper.UserMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/** 固定打包资源，按完整主题读取；不接受路径，不返回内部来源。 */
@Component
public class GuideTutorialBook {
    private static final List<String> FILES = List.of("01-账号与身份.md", "02-浏览与收藏.md",
            "03-企划与应征邀请.md", "04-作品与橱窗管理.md", "05-订单交付与评价.md",
            "06-消息与安全互动.md", "07-艺绘向导.md", "08-管理员操作.md");
    private static final Pattern HEADING = Pattern.compile("(?m)^## ([A-Z]+-\\d{2}) ([^\\r\\n]+)\\r?$");
    private record Topic(String topicId, String title, String body, boolean adminOnly) { }
    private final List<Topic> topics;
    private final UserMapper users;

    public GuideTutorialBook(UserMapper users) throws IOException {
        this.users = users;
        var loaded = new ArrayList<Topic>();
        for (String file : FILES) {
            boolean adminOnly = file.startsWith("07-") || file.startsWith("08-");
            String markdown;
            try (var in = new ClassPathResource("guide/tutorials/" + file).getInputStream()) {
                markdown = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            var matcher = HEADING.matcher(markdown);
            String id = null, title = null;
            int bodyStart = 0;
            while (matcher.find()) {
                if (id != null) loaded.add(new Topic(id, title, markdown.substring(bodyStart, matcher.start()).strip(), adminOnly));
                id = matcher.group(1); title = matcher.group(2); bodyStart = matcher.end();
            }
            if (id == null) throw new IllegalStateException("教程缺少主题");
            loaded.add(new Topic(id, title, markdown.substring(bodyStart).strip(), adminOnly));
        }
        if (loaded.size() != 61 || loaded.stream().map(Topic::topicId).distinct().count() != loaded.size())
            throw new IllegalStateException("教程主题数量或编号无效");
        topics = List.copyOf(loaded);
    }

    public Map<String, Object> read(Long owner, String keyword, List<String> ids) {
        if (ids == null || ids.size() > 3 || (keyword != null && (!ids.isEmpty() || keyword.isBlank() || keyword.length() > 60)))
            throw new GuideModelException("教程查询参数无效");
        boolean admin = false;
        if (owner != null && owner > 0 && users != null) {
            var user = users.selectById(owner);
            admin = user != null && Integer.valueOf(3).equals(user.getRole())
                    && Integer.valueOf(0).equals(user.getStatus()) && Integer.valueOf(0).equals(user.getDeleted());
        }
        final boolean verifiedAdmin = admin;
        var accessible = topics.stream().filter(t -> !t.adminOnly() || verifiedAdmin).toList();
        boolean catalog = keyword == null && ids.isEmpty();
        List<Topic> selected;
        if (!ids.isEmpty()) {
            // 一条越权/不存在主题即整批拒绝，不暴露该主题标题。
            selected = ids.stream().distinct().map(id -> accessible.stream().filter(t -> t.topicId().equals(id))
                    .findFirst().orElseThrow(() -> new GuideModelException("教程不存在或不在当前账号可读范围"))).toList();
        } else if (catalog) selected = accessible;
        else {
            var terms = terms(keyword);
            selected = accessible.stream().filter(t -> score(t, terms) > 0)
                    .sorted(Comparator.<Topic>comparingInt(t -> score(t, terms)).reversed().thenComparing(Topic::topicId))
                    .limit(3).toList();
        }
        var result = new ArrayList<Map<String, String>>();
        for (var topic : selected) {
            var item = new LinkedHashMap<String, String>();
            item.put("topicId", topic.topicId()); item.put("title", topic.title());
            item.put("audience", topic.adminOnly() ? "仅正常管理员" : "普通平台教程，具体操作前提见正文");
            if (!catalog) item.put("body", topic.body());
            result.add(item);
        }
        return Map.of("sourceType", "platform_tutorials", "mode", catalog ? "catalog" : "content",
                "topics", result, "instruction", catalog ? "按问题选择主题编号再次读取正文，不凭目录猜操作。"
                        : "只用相关主题，保留前提与限制；无匹配请查目录或澄清。不得显示主题编号或内部来源。教程不代表已执行业务操作。");
    }

    private Set<String> terms(String keyword) {
        var terms = new LinkedHashSet<String>();
        for (String word : keyword.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (word.length() < 2) continue;
            terms.add(word);
            for (int i = 0; i < word.length() - 1; i++) {
                String pair = word.substring(i, i + 2);
                if (pair.matches("[\\p{IsHan}]{2}")) terms.add(pair);
            }
        }
        return terms;
    }
    private int score(Topic topic, Set<String> terms) {
        String title = topic.title().toLowerCase(Locale.ROOT), body = topic.body().toLowerCase(Locale.ROOT);
        return terms.stream().mapToInt(term -> (title.contains(term) ? 10 : 0) + (body.contains(term) ? 1 : 0)).sum();
    }
}
