package com.artiv.guide.tool;

import com.artiv.guide.client.*;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.function.*;

/** 每轮最多三次受控读取，最后单独流式回答；工具返回的数据不是指令。 */
@Component
public class GuideAnswerRunner {
    public static final String SYSTEM = com.artiv.guide.GuidePrompts.SYSTEM;
    private final GuideToolRegistry tools;
    public GuideAnswerRunner(GuideToolRegistry tools) { this.tools = tools; }
    public GuideModelClient.Reply answer(GuideModelClient client, List<GuideModelClient.Message> history,
            Consumer<String> delta, BiConsumer<String, Object> event, Consumer<List<GuideReference>> saveReferences,
            GuideCancellation cancellation) {
        return answer(client, history, delta, event, saveReferences, cancellation, null);
    }
    public GuideModelClient.Reply answer(GuideModelClient client, List<GuideModelClient.Message> history,
            Consumer<String> delta, BiConsumer<String, Object> event, Consumer<List<GuideReference>> saveReferences,
            GuideCancellation cancellation, Long owner) {
        var context = new ArrayList<>(history);
        // 为工具消息预留位置，不把旧工具结果作为下一轮的实时依据。
        while (context.size() > 20) { context.remove(1); context.remove(1); }
        var references = new LinkedHashMap<String, GuideReference>();
        var executed = new HashSet<String>();
        GuideModelClient.Usage total = new GuideModelClient.Usage(0L, 0L);
        int reads = 0;
        for (int round = 0; round < 3 && reads < 3; round++) {
            cancellation.check();
            event.accept("tool_status", Map.of("message", "正在确认问题与可用依据…"));
            var choice = client.complete(new GuideModelClient.Request(List.copyOf(context), false, List.of(tools.definition()), false));
            cancellation.check();
            total = sum(total, choice.usage());
            if (choice.toolCalls().isEmpty()) break;
            if (choice.toolCalls().size() > 3 - reads) throw new GuideModelException("本轮只读查询超过三次，请缩小问题范围");
            if (choice.toolCalls().stream().map(GuideModelClient.ToolCall::id).distinct().count() != choice.toolCalls().size()
                    || choice.toolCalls().stream().anyMatch(call -> !tools.definition().name().equals(call.name())))
                throw new GuideModelException("模型工具调用未授权或标识重复");
            context.add(GuideModelClient.Message.assistant(choice));
            for (var call : choice.toolCalls()) {
            cancellation.check();
            if (!executed.add(call.name() + call.arguments())) throw new GuideModelException("检测到重复工具读取，已停止；请调整问题");
            event.accept("tool_status", Map.of("message", "正在读取权限范围内的 Artiv 依据…"));
            var result = owner == null ? tools.execute(call, cancellation) : tools.execute(call, cancellation, owner);
            cancellation.check();
            reads++;
            context.add(GuideModelClient.Message.tool(call.id(), result.content()));
            for (var ref : result.references()) references.put(ref.kind() + ":" + ref.id(), ref);
            var safeRefs = List.copyOf(references.values());
            saveReferences.accept(safeRefs);
            event.accept("references", safeRefs);
            }
        }
        cancellation.check();
        var cardKeys = references.values().stream().filter(ref -> ref.card() != null)
                .map(ref -> ref.kind() + ":" + ref.id()).toList();
        if (!cardKeys.isEmpty()) {
            context.add(new GuideModelClient.Message("system",
                    "前端会在本条回复下自动展示以下已核验资源的可点击卡片：" + String.join("、", cardKeys)
                    + "。用户找企划、橱窗或画师时，正文只需1至3句简短说明，说明本页范围及必要提醒，引导点击下方卡片；不要再逐条列举候选，不要重复卡片的标题、预算、题材、截稿等详情，不输出Markdown列表或链接。"
                    + "卡片是本轮实际查询到的候选，不保证满足无法筛选的主观偏好，不称为全平台最佳。若用户明确问详情、比较或具体问题，仍应回答问题所需的分析，不能只让用户点卡片。", null, null, List.of()));
        }
        context.add(new GuideModelClient.Message("system", "现在直接回答，不再调用工具。角色需求讨论：尚未明确创作目标时，只概括1至2个可见特点并直接问1至2个关键问题，不提前生成需求说明；目标已明确且信息足够或用户要求直接总结时，输出约稿需求说明。不把图片可见元素自动列为需保留，不添加用户没说的要求。纯文本排版，不使用星号加粗、分隔线、HTML或链接。平台规则和候选只能依据本轮已读工具结果，没有结果不能编造；区分一般建议，不自动放宽条件。", null, null, List.of()));
        event.accept("tool_status", Map.of("message", "正在整理回答…"));
        var reply = client.stream(new GuideModelClient.Request(List.copyOf(context), false, List.of(), false), delta, cancellation);
        return new GuideModelClient.Reply(reply.text(), List.of(), sum(total, reply.usage()), reply.finishReason(), reply.elapsedMillis());
    }
    private GuideModelClient.Usage sum(GuideModelClient.Usage a, GuideModelClient.Usage b) {
        return new GuideModelClient.Usage(add(a == null ? null : a.inputTokens(), b == null ? null : b.inputTokens()),
                add(a == null ? null : a.outputTokens(), b == null ? null : b.outputTokens()));
    }
    private Long add(Long a, Long b) { return a == null || b == null ? null : a + b; }
}
