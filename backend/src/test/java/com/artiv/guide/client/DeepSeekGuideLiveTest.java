package com.artiv.guide.client;

import com.artiv.config.GuideConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** 显式启用才收费，普通 mvn test 跳过。最多四次、无重试、仅合成图与固定工具。 */
@EnabledIfEnvironmentVariable(named = "ARTIV_GUIDE_LIVE_TEST", matches = "true")
class DeepSeekGuideLiveTest {
    @Test
    void verifyTextSingleImageAndReadOnlyToolRoundTrip() throws Exception {
        String key = System.getenv("ARTIV_GUIDE_API_KEY");
        assertTrue(key != null && !key.isBlank(), "请在后端测试进程配置 ARTIV_GUIDE_API_KEY");
        var config = new GuideConfig();
        config.setEnabled(true);
        config.setMaxOutputTokens(512);
        config.setTimeoutSeconds(60);
        config.setApiKey(key);
        var client = new DeepSeekGuideModelClient(config, new ObjectMapper());

        var text = client.complete(new GuideModelClient.Request(
                List.of(GuideModelClient.Message.user("这是接口连通性测试，请只回复 ARTIV_OK。")), false, List.of(), false));
        record("text", text);
        assertTrue(text.text().contains("ARTIV_OK"), "文本验证未得到预期标记");

        var vision = client.complete(new GuideModelClient.Request(List.of(GuideModelClient.Message.image(
                "请用中文说明白色背景上左、右两个图形各是什么颜色和形状。不要猜测图外内容。", GuideProbeFixture.image())),
                true, List.of(), false));
        record("single_image", vision);
        assertTrue(vision.text().contains("红") && vision.text().contains("蓝")
                && vision.text().contains("方") && vision.text().contains("圆"), "单图验证未识别合成色块");

        var fixture = new GuideProbeFixture();
        var history = new ArrayList<GuideModelClient.Message>();
        history.add(GuideModelClient.Message.user("调用 read_probe_fixture 获取固定测试数据，然后原样回复它的 marker 字段，不能自行编造。"));
        var tool = client.complete(new GuideModelClient.Request(history, false, List.of(fixture.tool()), true));
        record("tool_request", tool);
        assertEquals(1, tool.toolCalls().size(), "必须得到一次真实工具请求");
        var call = tool.toolCalls().getFirst();
        history.add(GuideModelClient.Message.assistant(tool));
        history.add(GuideModelClient.Message.tool(call.id(), fixture.execute(call)));
        var answer = client.complete(new GuideModelClient.Request(history, false, List.of(), false));
        record("tool_result", answer);
        assertTrue(answer.text().contains(GuideProbeFixture.MARKER), "模型未使用实际工具返回值");
    }

    private static void record(String stage, GuideModelClient.Reply reply) {
        // 仅输出阶段、耗时、token 数和结束原因；不输出 Key、正文、图片或工具参数。
        System.out.printf("GUIDE_LIVE stage=%s model=deepseek-v4-flash-vision-exp elapsedMs=%d inputTokens=%s outputTokens=%s finish=%s%n",
                stage, reply.elapsedMillis(), reply.usage().inputTokens(), reply.usage().outputTokens(), reply.finishReason());
        assertNotNull(reply.usage().inputTokens(), "缺少真实用量统计");
        assertNotNull(reply.usage().outputTokens(), "缺少真实用量统计");
    }
}
