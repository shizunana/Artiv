package com.artiv.guide.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;

/** 仅测试源码：不注册为业务工具、不读取数据库或用户图片。 */
final class GuideProbeFixture {
    static final String TOOL_NAME = "read_probe_fixture";
    static final String MARKER = "ARTIV_PROBE_731";
    private final ObjectMapper json = new ObjectMapper();

    GuideModelClient.ToolDefinition tool() throws Exception {
        return new GuideModelClient.ToolDefinition(TOOL_NAME, "读取固定的模拟测试数据，返回测试标记；不查询真实业务。",
                json.readTree("{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false}"));
    }

    String execute(GuideModelClient.ToolCall call) throws Exception {
        if (!TOOL_NAME.equals(call.name()) || call.arguments().length() > 256) {
            throw new GuideModelException("拒绝未授权测试工具");
        }
        var args = json.reader().with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .readTree(call.arguments());
        if (args == null || !args.isObject() || args.size() != 0) throw new GuideModelException("测试工具只接受空对象参数");
        return "{\"source\":\"synthetic_test_fixture\",\"marker\":\"" + MARKER + "\"}";
    }

    static String image() throws Exception {
        var image = new BufferedImage(384, 384, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, 384, 384);
            graphics.setColor(Color.RED);
            graphics.fillRect(32, 128, 96, 96);
            graphics.setColor(Color.BLUE);
            graphics.fillOval(240, 128, 96, 96);
        } finally { graphics.dispose(); }
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes.toByteArray());
    }
}
