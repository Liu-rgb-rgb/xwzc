package com.xiuwen.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 图像生成配置。
 * 对应 application.yml：xiuwen.ai.base-url / api-key / model / image-size / timeout-seconds
 *
 * 支持 OpenAI 官方地址，也支持 OpenAI 格式的中转/代理地址（改 base-url 即可）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "xiuwen.ai")
public class AiProperties {

    /**
     * AI 接口地址。
     * 官方：https://api.openai.com/v1
     * 中转：填中转商提供的地址，例如 https://xxx.com/v1
     * 留空时默认使用 OpenAI 官方地址。
     */
    private String baseUrl;

    /**
     * API Key，例如 sk-xxxxxx
     */
    private String apiKey;

    /**
     * 图像生成模型，例如 dall-e-2 / dall-e-3（按你使用的服务商支持的模型填写）
     */
    private String model;

    /**
     * 生成图片尺寸，例如 1024x1024、512x512、256x256（dall-e-2 支持三种，dall-e-3 只支持 1024x1024）
     */
    private String imageSize = "1024x1024";

    /**
     * 调用超时时间（秒），图像生成较慢，建议 60 以上
     */
    private Integer timeoutSeconds = 120;

    /**
     * 生图服务商：qwen（阿里云百炼 Qwen-Image，默认）或 openai
     */
    private String provider = "qwen";

    /**
     * 阿里云百炼 API Key（在百炼控制台创建）
     */
    private String qwenApiKey;

    /**
     * 百炼图像生成接口地址，留空使用默认（multimodal-generation 同步接口）
     */
    private String qwenBaseUrl;

    /**
     * Qwen-Image 模型名，留空使用 qwen-image-3.0
     */
    private String qwenModel;

    /**
     * 是否让 Qwen 自动扩写提示词以提升画质，默认 true
     */
    private Boolean qwenPromptExtend = true;
}
