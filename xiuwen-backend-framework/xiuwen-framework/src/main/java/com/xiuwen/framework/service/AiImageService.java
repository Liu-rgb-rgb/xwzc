package com.xiuwen.framework.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.image.Image;
import com.theokanning.openai.image.ImageResult;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.framework.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * AI 图像生成服务。
 *
 * 封装 OpenAI images/generations 接口，返回图片的字节数组（base64 解码后）。
 */
@Slf4j
@Service
public class AiImageService {

    private final AiProperties aiProperties;

    /**
     * OpenAiService 内部持有 HTTP 连接池，这里懒加载并复用。
     */
    private volatile OpenAiService openAiService;

    public AiImageService(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    /**
     * 根据提示词生成图片。
     *
     * @param prompt 提示词
     * @param count  生成数量（1-4）
     * @return 每张图片的字节数组（PNG）
     */
    public List<byte[]> generateImages(String prompt, int count) {
        // 走阿里云百炼 Qwen-Image
        if ("qwen".equalsIgnoreCase(aiProperties.getProvider())) {
            return generateViaQwen(prompt, count);
        }
        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().trim().isEmpty()) {
            throw new BusinessException("AI 服务未配置，请先配置 xiuwen.ai.api-key");
        }

        CreateImageRequest request = CreateImageRequest.builder()
                .prompt(prompt)
                .n(count)
                .size(aiProperties.getImageSize())
                .responseFormat("b64_json")
                .build();
        // 只有配置了模型才传 model 字段（部分中转服务不需要该字段）
        if (aiProperties.getModel() != null && !aiProperties.getModel().trim().isEmpty()) {
            request.setModel(aiProperties.getModel());
        }

        try {
            ImageResult result = getOpenAiService().createImage(request);
            List<Image> images = result.getData();
            if (images == null || images.isEmpty()) {
                throw new BusinessException("AI 未返回生成结果");
            }

            List<byte[]> bytesList = new ArrayList<>();
            for (Image image : images) {
                String b64 = image.getB64Json();
                if (b64 == null || b64.isEmpty()) {
                    throw new BusinessException("AI 返回结果缺少图片数据");
                }
                bytesList.add(Base64.getDecoder().decode(b64));
            }
            return bytesList;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 图像生成失败: {}", e.getMessage(), e);
            throw new BusinessException("AI 图像生成失败，请稍后重试");
        }
    }

    /**
     * 懒加载 OpenAiService（支持自定义 baseUrl，兼容中转地址）。
     */
    private OpenAiService getOpenAiService() {
        if (openAiService == null) {
            synchronized (this) {
                if (openAiService == null) {
                    int timeout = aiProperties.getTimeoutSeconds() == null
                            ? 120
                            : aiProperties.getTimeoutSeconds();
                    String baseUrl = aiProperties.getBaseUrl();
                    if (baseUrl == null || baseUrl.trim().isEmpty()) {
                        openAiService = new OpenAiService(
                                aiProperties.getApiKey(),
                                Duration.ofSeconds(timeout)
                        );
                    } else {
                        openAiService = new OpenAiService(
                                buildApi(baseUrl.trim(), aiProperties.getApiKey(), timeout)
                        );
                    }
                }
            }
        }
        return openAiService;
    }

    /**
     * 使用自定义 baseUrl 构建 OpenAiApi（兼容中转地址）。
     */
    private OpenAiApi buildApi(String baseUrl, String apiKey, int timeoutSeconds) {
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        ObjectMapper mapper = OpenAiService.defaultObjectMapper();
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new AuthorizationInterceptor(apiKey))
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .readTimeout(Duration.ofSeconds(timeoutSeconds))
                .writeTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
        return retrofit.create(OpenAiApi.class);
    }

    /**
     * 请求头中附加 Bearer Token 的拦截器。
     */
    private static class AuthorizationInterceptor implements Interceptor {

        private final String apiKey;

        AuthorizationInterceptor(String apiKey) {
            this.apiKey = apiKey;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request().newBuilder()
                    .header("Authorization", "Bearer " + apiKey)
                    .build();
            return chain.proceed(request);
        }
    }

    // ===================== 阿里云百炼 Qwen-Image =====================

    private static final String DEFAULT_QWEN_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";
    private static final String DEFAULT_QWEN_MODEL = "qwen-image-3.0";

    private volatile RestTemplate restTemplate;

    /**
     * 通过阿里云百炼 Qwen-Image 生成图片。
     * 同步多模态生成接口，返回图片 URL，下载为字节后走原有上传链路。
     */
    @SuppressWarnings("unchecked")
    private List<byte[]> generateViaQwen(String prompt, int count) {
        String apiKey = aiProperties.getQwenApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new BusinessException("未配置百炼 API Key，请先配置 xiuwen.ai.qwen-api-key");
        }
        String url = blank(aiProperties.getQwenBaseUrl()) ? DEFAULT_QWEN_URL : aiProperties.getQwenBaseUrl().trim();
        String model = blank(aiProperties.getQwenModel()) ? DEFAULT_QWEN_MODEL : aiProperties.getQwenModel().trim();
        boolean promptExtend = aiProperties.getQwenPromptExtend() == null || aiProperties.getQwenPromptExtend();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.trim());

        Map<String, Object> textItem = new HashMap<>();
        textItem.put("text", prompt);
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(textItem);
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", content);
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(message);
        Map<String, Object> input = new HashMap<>();
        input.put("messages", messages);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("prompt_extend", promptExtend);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("input", input);
        body.put("parameters", parameters);

        List<byte[]> bytesList = new ArrayList<>();
        try {
            // Qwen-Image 每次调用生成 1 张，count 张就调用 count 次
            for (int i = 0; i < count; i++) {
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                ResponseEntity<Map> resp = getRestTemplate().postForEntity(url, entity, Map.class);
                String imageUrl = extractQwenImageUrl(resp.getBody());
                byte[] bytes = getRestTemplate().getForObject(imageUrl, byte[].class);
                if (bytes == null || bytes.length == 0) {
                    throw new BusinessException("下载 Qwen 生成图片失败");
                }
                bytesList.add(bytes);
            }
            return bytesList;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Qwen-Image 生成失败: {}", e.getMessage(), e);
            throw new BusinessException("AI 图像生成失败，请稍后重试");
        }
    }

    /** 从百炼响应取出图片 URL：output.choices[0].message.content[].image */
    @SuppressWarnings("unchecked")
    private String extractQwenImageUrl(Map<String, Object> resp) {
        if (resp == null) {
            throw new BusinessException("百炼返回为空");
        }
        Map<String, Object> output = (Map<String, Object>) resp.get("output");
        if (output == null) {
            throw new BusinessException("百炼响应缺少 output 字段");
        }
        List<Map<String, Object>> choices = (List<Map<String, Object>>) output.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new BusinessException("百炼响应缺少 choices");
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        List<Map<String, Object>> content = (List<Map<String, Object>>) message.get("content");
        if (content != null) {
            for (Map<String, Object> c : content) {
                Object img = c.get("image");
                if (img != null) {
                    return img.toString();
                }
            }
        }
        throw new BusinessException("百炼响应中未找到图片");
    }

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** 生图较慢，RestTemplate 设长超时，懒加载复用 */
    private RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            synchronized (this) {
                if (restTemplate == null) {
                    int timeout = aiProperties.getTimeoutSeconds() == null ? 120 : aiProperties.getTimeoutSeconds();
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(timeout * 1000);
                    factory.setReadTimeout(timeout * 1000);
                    restTemplate = new RestTemplate(factory);
                }
            }
        }
        return restTemplate;
    }
}
