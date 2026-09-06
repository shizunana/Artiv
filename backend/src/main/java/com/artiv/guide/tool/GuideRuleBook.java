package com.artiv.guide.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class GuideRuleBook {
    public record Rule(String ruleId, String title, String version, String verifiedAt, List<String> sources, String body) { }
    private final List<Rule> rules;
    public GuideRuleBook(ObjectMapper json) throws java.io.IOException {
        try (var input = new ClassPathResource("guide/platform-rules.json").getInputStream()) {
            rules = List.of(json.readValue(input, Rule[].class));
        }
        if (rules.isEmpty() || rules.size() > 30 || rules.stream().map(Rule::ruleId).distinct().count() != rules.size())
            throw new IllegalStateException("向导规则库无效");
    }
    public List<GuideReference> references() {
        return rules.stream().map(r -> new GuideReference("rule", r.ruleId(), r.title(),
                r.body() + "（规则版本 " + r.version() + "）", r.verifiedAt())).toList();
    }
}
