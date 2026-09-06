package com.artiv.guide.tool;

import com.artiv.guide.client.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import org.springframework.stereotype.Component;
import java.util.*;

/** 唯一工具入口：固定操作枚举、严格参数校验，无反射/SQL/HTTP/业务写操作入口。 */
@Component
public class GuideToolRegistry {
    public record Result(String content, List<GuideReference> references) { }
    private final ObjectMapper json;
    private final GuideRuleBook rules;
    private final GuidePublicReader publicReader;
    private final JsonNode options;
    public GuideToolRegistry(ObjectMapper json, GuideRuleBook rules, GuidePublicReader publicReader) throws java.io.IOException {
        this.json = json; this.rules = rules; this.publicReader = publicReader;
        try (var in = new org.springframework.core.io.ClassPathResource("guide/search-options.json").getInputStream()) { options = json.readTree(in); }
    }
    public GuideModelClient.ToolDefinition definition() {
        var schema = json.createObjectNode().put("type", "object").put("additionalProperties", false);
        schema.putArray("required").add("operation");
        var properties = schema.putObject("properties");
        properties.putObject("operation").put("type", "string").putArray("enum")
                .add("rules").add("search_projects").add("project_detail").add("search_artists").add("artist_detail");
        properties.putObject("id").put("type", "integer").put("minimum", 1);
        properties.putObject("page").put("type", "integer").put("minimum", 1).put("maximum", 10);
        properties.putObject("keyword").put("type", "string").put("maxLength", 60);
        properties.putObject("category").put("type", "string").set("enum", options.path("categories"));
        properties.putObject("subject").put("type", "string").set("enum", options.path("subjects"));
        properties.putObject("budgetMin").put("type", "number").put("minimum", 0).put("maximum", 10000000);
        properties.putObject("budgetMax").put("type", "number").put("minimum", 0).put("maximum", 10000000);
        return new GuideModelClient.ToolDefinition("read_artiv", "只读 Artiv。rules 只接 operation；涉及平台规则必须读取。"
                + "search_projects 支持 page/keyword/category/subject/budgetMin/budgetMax；search_artists 只支持 page/keyword/category/subject。每页最多5条，不自动放宽条件。"
                + "project_detail/artist_detail 只接 operation 和正整数 id。画师详情含最多3条近期公开评价及公开作品文字，无图片分析，无私人订单和写操作。未指定筛选不填对应字段。", schema);
    }
    public Result execute(GuideModelClient.ToolCall call, GuideCancellation cancellation) {
        cancellation.check();
        if (!"read_artiv".equals(call.name()) || call.arguments() == null || call.arguments().length() > 4096)
            throw new GuideModelException("工具名称或参数未授权");
        try {
            var args = json.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .with(JsonParser.Feature.STRICT_DUPLICATE_DETECTION).readTree(call.arguments());
            if (!args.isObject() || !args.path("operation").isTextual()) throw invalid();
            String operation = args.path("operation").asText();
            if ("rules".equals(operation)) {
                allowed(args, Set.of("operation"));
                var refs = rules.references();
                return new Result(json.writeValueAsString(Map.of("sourceType", "platform_rules", "rules", refs)), refs);
            }
            GuidePublicReader.ReadResult result;
            if (Set.of("project_detail", "artist_detail").contains(operation)) {
                allowed(args, Set.of("operation", "id"));
                long id = integer(args.path("id"), 1, Long.MAX_VALUE);
                result = operation.equals("project_detail") ? publicReader.project(id) : publicReader.artist(id);
            } else if (Set.of("search_projects", "search_artists").contains(operation)) {
                allowed(args, operation.equals("search_projects") ? Set.of("operation", "page", "keyword", "category", "subject", "budgetMin", "budgetMax")
                        : Set.of("operation", "page", "keyword", "category", "subject"));
                int page = args.has("page") ? (int) integer(args.path("page"), 1, 10) : 1;
                String keyword = text(args, "keyword", 60), category = option(args, "category", "categories"), subject = option(args, "subject", "subjects");
                var minimum = money(args, "budgetMin"); var maximum = money(args, "budgetMax");
                if (minimum != null && maximum != null && minimum.compareTo(maximum) > 0) throw invalid();
                cancellation.check();
                result = operation.equals("search_projects") ? publicReader.projects(page, keyword, category, subject, minimum, maximum)
                        : publicReader.artists(page, keyword, category, subject);
            } else throw invalid();
            cancellation.check();
            String content = json.writeValueAsString(Map.of("sourceType", "public_query", "untrustedUserContent", true, "data", result.data()));
            if (content.length() > 14000) throw new GuideModelException("公开结果超过容量限制，请缩小查询范围");
            return new Result(content, result.references());
        } catch (GuideModelException e) { throw e; }
        catch (java.util.concurrent.CancellationException e) { throw e; }
        catch (com.artiv.common.BusinessException e) { throw new GuideModelException("该资源不存在或当前不在向导公开范围内，不能确认其详情"); }
        catch (Exception e) { throw new GuideModelException("工具参数或公开依据读取失败，未自动重试"); }
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
