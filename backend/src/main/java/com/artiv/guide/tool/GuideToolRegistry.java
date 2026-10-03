package com.artiv.guide.tool;

import com.artiv.guide.client.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import org.springframework.stereotype.Component;
import java.util.*;

/** 唯一工具入口：固定操作枚举、严格参数校验，无反射/SQL/HTTP/业务写操作入口。 */
@Component
@lombok.extern.slf4j.Slf4j
public class GuideToolRegistry {
    public record Result(String content, List<GuideReference> references) { }
    private final ObjectMapper json;
    private final GuideRuleBook rules;
    private final GuidePublicReader publicReader;
    private final JsonNode options;
    private final com.artiv.service.GuideBusinessReadService business;
    private final GuideTutorialBook tutorials;
    public GuideToolRegistry(ObjectMapper json, GuideRuleBook rules, GuidePublicReader publicReader) throws java.io.IOException {
        this(json, rules, publicReader, null);
    }
    public GuideToolRegistry(ObjectMapper json, GuideRuleBook rules, GuidePublicReader publicReader, com.artiv.service.GuideBusinessReadService business) throws java.io.IOException {
        this(json, rules, publicReader, business, null);
    }
    @org.springframework.beans.factory.annotation.Autowired
    public GuideToolRegistry(ObjectMapper json, GuideRuleBook rules, GuidePublicReader publicReader,
            com.artiv.service.GuideBusinessReadService business, GuideTutorialBook tutorials) throws java.io.IOException {
        this.json = json; this.rules = rules; this.publicReader = publicReader;
        this.business = business;
        this.tutorials = tutorials;
        try (var in = new org.springframework.core.io.ClassPathResource("guide/search-options.json").getInputStream()) { options = json.readTree(in); }
    }
    public GuideModelClient.ToolDefinition definition() {
        var schema = json.createObjectNode().put("type", "object").put("additionalProperties", false);
        schema.putArray("required").add("operation");
        var properties = schema.putObject("properties");
        properties.putObject("operation").put("type", "string").putArray("enum")
                .add("rules").add("search_projects").add("project_detail").add("search_stalls").add("stall_detail").add("search_artists").add("artist_detail")
                .add("my_projects").add("my_project").add("my_orders").add("my_order").add("my_invitations").add("my_invitation")
                .add("recommend_artists_by_category").add("tutorials");
        properties.putObject("topicIds").put("type", "array").put("minItems", 1).put("maxItems", 3)
                .putObject("items").put("type", "string").put("pattern", "^[A-Z]+-[0-9]{2}$");
        var categories = properties.putObject("categories").put("type", "array").put("minItems", 1).put("maxItems", 10);
        categories.putObject("items").put("type", "string").set("enum", options.path("categories"));
        properties.putObject("id").put("type", "integer").put("minimum", 1);
        properties.putObject("page").put("type", "integer").put("minimum", 1).put("maximum", 10);
        properties.putObject("keyword").put("type", "string").put("maxLength", 60);
        properties.putObject("category").put("type", "string").set("enum", options.path("categories"));
        properties.putObject("projectCategory").put("type", "string").set("enum", json.valueToTree(com.artiv.common.ProjectCategoryRules.OPTIONS));
        var subjectSchema = properties.putObject("subject").put("type", "string");
        subjectSchema.set("enum", options.path("subjects"));
        subjectSchema.put("description", "橱窗和画师查询使用原题材；企划仅允许对应类型题材：" + options.path("projectSubjectsByCategory"));
        properties.putObject("budgetMin").put("type", "number").put("minimum", 0).put("maximum", 10000000);
        properties.putObject("budgetMax").put("type", "number").put("minimum", 0).put("maximum", 10000000);
        properties.putObject("priceMin").put("type", "number").put("minimum", 0).put("maximum", 10000000);
        properties.putObject("priceMax").put("type", "number").put("minimum", 0).put("maximum", 10000000);
        return new GuideModelClient.ToolDefinition("read_artiv", "唯一函数名 read_artiv，以下名称全部是 operation 参数值而不是函数名。只读 Artiv。rules 只接 operation；涉及平台规则必须读取。"
                + "tutorials 用于如何使用、入口、步骤问题：只接 operation 与可选 keyword 或 topicIds，二者互斥。关键词尽量简短具体如‘发布企划’‘删除邀请’‘修改橱窗’，按匹配返回最多3篇完整教程；不带筛选返回当前账号可读目录，随后用 topicIds 读取最多3篇正文。覆盖账号认证、浏览收藏、企划邀请应征、作品橱窗、订单交付评价、消息屏蔽举报；管理员功能由服务端验证后才可读。不要用实时搜索代替操作教程，不把目录当正文。"
                + "search_projects 支持 page/keyword/projectCategory/subject/budgetMin/budgetMax，企划类型为独立六类；search_stalls 支持 page/keyword/category/subject/priceMin/priceMax；search_artists 只支持 page/keyword/category/subject。橱窗和画师的 category 均为作品原十类。禁止混用两种分类字段。每页最多5条，不自动放宽条件。"
                + "project_detail/stall_detail/artist_detail 只接 operation 和正整数 id。画师详情含最多3条近期公开评价及公开作品文字。"
                + "画师类型匹配公开作品或在售橱窗，题材仅匹配个人标签。用户要每种类型各推荐一位时必须用 recommend_artists_by_category，只接 categories 数组和可选 subject，不传 categories 默认原十类；一次调用即可完成，禁止拆成十个调用。普通推荐用 search_artists，每页最多5位；keyword 只放明确姓名或搜索词，不填‘画师/推荐’等意图词。"
                + "my_projects/my_orders/my_invitations 只接 operation/page，查询本人企划、本人参与订单、本人收到且未隐藏邀请的状态；每页5条。"
                + "my_project/my_order/my_invitation 只接 operation/id。身份由服务端传入，绝不接受 userId/owner/actor/role。无权/无记录如实说明，不冒用他人数据。全部只读，未指定筛选不填对应字段。", schema);
    }
    public Result execute(GuideModelClient.ToolCall call, GuideCancellation cancellation) {
        return execute(call, cancellation, null);
    }
    public Result execute(GuideModelClient.ToolCall call, GuideCancellation cancellation, Long owner) {
        long started = System.nanoTime();
        boolean completed = false;
        cancellation.check();
        if (!"read_artiv".equals(call.name()) || call.arguments() == null || call.arguments().length() > 4096)
            throw new GuideModelException("工具名称或参数未授权");
        try {
            var args = json.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .with(JsonParser.Feature.STRICT_DUPLICATE_DETECTION).readTree(call.arguments());
            if (!args.isObject() || !args.path("operation").isTextual()) throw invalid();
            String operation = args.path("operation").asText();
            if ("tutorials".equals(operation)) {
                allowed(args, Set.of("operation", "keyword", "topicIds"));
                if (tutorials == null || (args.has("keyword") && args.has("topicIds"))) throw invalid();
                String keyword = text(args, "keyword", 60);
                var ids = new ArrayList<String>();
                if (args.has("topicIds")) {
                    var values = args.path("topicIds");
                    if (!values.isArray() || values.isEmpty() || values.size() > 3) throw invalid();
                    for (var value : values) {
                        if (!value.isTextual() || !value.asText().matches("[A-Z]+-[0-9]{2}")) throw invalid();
                        ids.add(value.asText());
                    }
                }
                var data = tutorials.read(owner, keyword, ids);
                cancellation.check();
                completed = true;
                // 教程仅进入本轮模型上下文，不作为 references 下发/持久化到浏览器。
                return new Result(json.writeValueAsString(data), List.of());
            }
            if ("rules".equals(operation)) {
                allowed(args, Set.of("operation"));
                var refs = rules.references();
                completed = true;
                return new Result(json.writeValueAsString(Map.of("sourceType", "platform_rules", "rules", refs)), refs);
            }
            GuidePublicReader.ReadResult result;
            boolean privateRead = Set.of("my_projects", "my_project", "my_orders", "my_order", "my_invitations", "my_invitation").contains(operation);
            if (privateRead) {
                boolean detail = Set.of("my_project", "my_order", "my_invitation").contains(operation);
                allowed(args, detail ? Set.of("operation", "id") : Set.of("operation", "page"));
                if (owner == null || owner <= 0 || business == null) throw new GuideModelException("没有经过验证的本人身份，不能读取业务记录");
                Long id = detail ? integer(args.path("id"), 1, Long.MAX_VALUE) : null;
                int page = args.has("page") ? (int) integer(args.path("page"), 1, 10) : 1;
                result = business.read(owner, operation, id, page);
            } else if ("recommend_artists_by_category".equals(operation)) {
                allowed(args, Set.of("operation", "categories", "subject"));
                var requested = new LinkedHashSet<String>();
                var values = args.has("categories") ? args.path("categories") : options.path("categories");
                if (!values.isArray() || values.isEmpty() || values.size() > 10) throw invalid();
                for (var value : values) {
                    if (!value.isTextual() || !java.util.stream.StreamSupport.stream(options.path("categories").spliterator(), false).anyMatch(value::equals)) throw invalid();
                    requested.add(value.asText());
                }
                result = publicReader.artistsByCategory(List.copyOf(requested), option(args, "subject", "subjects"), cancellation);
            } else if (Set.of("project_detail", "stall_detail", "artist_detail").contains(operation)) {
                allowed(args, Set.of("operation", "id"));
                long id = integer(args.path("id"), 1, Long.MAX_VALUE);
                result = operation.equals("project_detail") ? publicReader.project(id)
                        : operation.equals("stall_detail") ? publicReader.stall(id) : publicReader.artist(id);
            } else if (Set.of("search_projects", "search_stalls", "search_artists").contains(operation)) {
                allowed(args, operation.equals("search_projects") ? Set.of("operation", "page", "keyword", "projectCategory", "subject", "budgetMin", "budgetMax")
                        : operation.equals("search_stalls") ? Set.of("operation", "page", "keyword", "category", "subject", "priceMin", "priceMax")
                        : Set.of("operation", "page", "keyword", "category", "subject"));
                int page = args.has("page") ? (int) integer(args.path("page"), 1, 10) : 1;
                String keyword = text(args, "keyword", 60), subject = option(args, "subject", operation.equals("search_projects") ? "projectSubjects" : "subjects");
                String category = operation.equals("search_projects")
                        ? com.artiv.common.ProjectCategoryRules.optional(text(args, "projectCategory", 50))
                        : option(args, "category", "categories");
                var minimum = money(args, operation.equals("search_stalls") ? "priceMin" : "budgetMin");
                var maximum = money(args, operation.equals("search_stalls") ? "priceMax" : "budgetMax");
                if (operation.equals("search_projects")) {
                    try { subject = com.artiv.common.ProjectSubjectRules.query(category == null ? List.of() : List.of(category), subject); }
                    catch (com.artiv.common.BusinessException e) { throw invalid(); }
                }
                if (minimum != null && maximum != null && minimum.compareTo(maximum) > 0) throw invalid();
                cancellation.check();
                result = operation.equals("search_projects") ? publicReader.projects(page, keyword, category, subject, minimum, maximum)
                        : operation.equals("search_stalls") ? publicReader.stalls(page, keyword, category, subject, minimum, maximum)
                        : publicReader.artists(page, keyword, category, subject);
            } else throw invalid();
            cancellation.check();
            String content = json.writeValueAsString(Map.of("sourceType", privateRead ? "private_query" : "public_query", "untrustedUserContent", true, "data", result.data()));
            if (content.length() > 14000) throw new GuideModelException("公开结果超过容量限制，请缩小查询范围");
            completed = true;
            return new Result(content, result.references());
        } catch (GuideModelException e) { throw e; }
        catch (java.util.concurrent.CancellationException e) { throw e; }
        catch (com.artiv.common.BusinessException e) { throw new GuideModelException("该资源不存在或不在当前账号允许读取的范围内，不能确认其详情"); }
        catch (Exception e) { throw new GuideModelException("工具参数或公开依据读取失败，未自动重试"); }
        finally {
            log.info("guide_tool owner={} tool=read_artiv status={} durationMs={}", owner, completed ? "complete" : "failed",
                    java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        }
    }
    private void allowed(JsonNode args, Set<String> allowed) { args.fieldNames().forEachRemaining(key -> { if (!allowed.contains(key)) throw invalid(); }); }
    private String text(JsonNode args, String key, int max) {
        if (!args.has(key)) return null;
        var value = args.get(key);
        if (!value.isTextual() || value.asText().isBlank() || value.asText().length() > max || value.asText().contains("%") || value.asText().contains("_")) throw invalid();
        return value.asText().trim();
    }
    private String option(JsonNode args, String key, String values) {
        String value = text(args, key, 60);
        if (value != null && !java.util.stream.StreamSupport.stream(options.path(values).spliterator(), false).anyMatch(n -> n.asText().equals(value))) throw invalid();
        return value;
    }
    private long integer(JsonNode value, long min, long max) {
        if (!value.isIntegralNumber() || !value.canConvertToLong() || value.asLong() < min || value.asLong() > max) throw invalid();
        return value.asLong();
    }
    private java.math.BigDecimal money(JsonNode args, String key) {
        if (!args.has(key)) return null;
        var value = args.get(key);
        if (!value.isNumber() || value.decimalValue().signum() < 0 || value.decimalValue().compareTo(new java.math.BigDecimal("10000000")) > 0 || value.decimalValue().scale() > 2) throw invalid();
        return value.decimalValue();
    }
    private GuideModelException invalid() { return new GuideModelException("工具参数不符合允许范围，请使用已有类型/题材与合理筛选条件"); }
}
