package com.xiuwen.pattern.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiuwen.common.constant.PatternStatus;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.pattern.dto.GeneratePatternRequest;
import com.xiuwen.pattern.dto.RegeneratePatternRequest;
import com.xiuwen.pattern.entity.Pattern;
import com.xiuwen.pattern.entity.PatternGeneration;
import com.xiuwen.pattern.service.PatternGenerateService;
import com.xiuwen.pattern.service.PatternGenerationService;
import com.xiuwen.pattern.service.PatternService;
import com.xiuwen.pattern.vo.GenerationSubmitVO;
import com.xiuwen.framework.service.AiImageService;
import com.xiuwen.framework.service.OssFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * AI 纹样生成业务实现(异步)。
 *
 * generate() 只负责校验参数、落 PROCESSING 记录并立即返回 generationId;
 * 真正的 AI 生图 + OSS 上传丢进 aiGenerationExecutor 线程池后台执行,
 * 每完成一张图更新一次进度, 前端通过状态接口轮询, 切换页面不影响任务继续。
 */
@Slf4j
@Service
public class PatternGenerateServiceImpl implements PatternGenerateService {

    /**
     * 后端内部支持的纹样风格编码。
     */
    private static final List<String> SUPPORTED_STYLES = Arrays.asList(
            "classic",
            "new_chinese",
            "embroidery",
            "lingnan_window"
    );

    /**
     * 后端内部支持的配色编码。
     */
    private static final List<String> SUPPORTED_COLORS = Arrays.asList(
            "chinese_elegant",
            "red_gold",
            "rich_color",
            "soft_elegant"
    );

    /**
     * 后端内部支持的应用场景编码。
     */
    private static final List<String> SUPPORTED_SCENES = Arrays.asList(
            "product",
            "poster",
            "clothing",
            "home",
            "package"
    );

    /**
     * 前端中文名称与后端编码映射。
     */
    private static final Map<String, String> STYLE_MAPPING = new HashMap<>();
    private static final Map<String, String> COLOR_MAPPING = new HashMap<>();
    private static final Map<String, String> SCENE_MAPPING = new HashMap<>();

    static {
        STYLE_MAPPING.put("广绣经典", "classic");
        STYLE_MAPPING.put("经典广绣", "classic");
        STYLE_MAPPING.put("传统广绣", "classic");
        STYLE_MAPPING.put("经典", "classic");
        STYLE_MAPPING.put("新中式", "new_chinese");
        STYLE_MAPPING.put("新中式风格", "new_chinese");
        STYLE_MAPPING.put("刺绣风", "embroidery");
        STYLE_MAPPING.put("刺绣纹样", "embroidery");
        STYLE_MAPPING.put("岭南花窗", "lingnan_window");

        COLOR_MAPPING.put("富贵华彩", "red_gold");
        COLOR_MAPPING.put("红金华彩", "red_gold");
        COLOR_MAPPING.put("红金配色", "red_gold");
        COLOR_MAPPING.put("国风雅韵", "chinese_elegant");
        COLOR_MAPPING.put("清雅素韵", "chinese_elegant");
        COLOR_MAPPING.put("中式雅韵", "chinese_elegant");
        COLOR_MAPPING.put("清润素韵", "soft_elegant");
        COLOR_MAPPING.put("素韵", "soft_elegant");

        SCENE_MAPPING.put("文创产品", "product");
        SCENE_MAPPING.put("文创商品", "product");
        SCENE_MAPPING.put("产品定制", "product");
        SCENE_MAPPING.put("文创定制", "product");
        SCENE_MAPPING.put("海报设计", "poster");
        SCENE_MAPPING.put("宣传海报", "poster");
        SCENE_MAPPING.put("海报", "poster");
        SCENE_MAPPING.put("服饰刺绣", "clothing");
        SCENE_MAPPING.put("家居软装", "home");
        SCENE_MAPPING.put("礼品包装", "package");
    }

    private final PatternGenerationService patternGenerationService;
    private final PatternService patternService;
    private final ObjectMapper objectMapper;
    private final OssFileService ossFileService;
    private final AiImageService aiImageService;
    private final Executor aiGenerationExecutor;

    /** 自身代理,用于让 @Transactional 在拆分的落库方法上生效(同类直接调用不走代理,事务会失效)。 */
    @Lazy
    @Autowired
    private PatternGenerateServiceImpl self;

    public PatternGenerateServiceImpl(PatternGenerationService patternGenerationService,
                                      PatternService patternService,
                                      ObjectMapper objectMapper,
                                      OssFileService ossFileService,
                                      AiImageService aiImageService,
                                      @Qualifier("aiGenerationExecutor") Executor aiGenerationExecutor) {
        this.patternGenerationService = patternGenerationService;
        this.patternService = patternService;
        this.objectMapper = objectMapper;
        this.ossFileService = ossFileService;
        this.aiImageService = aiImageService;
        this.aiGenerationExecutor = aiGenerationExecutor;
    }

    /**
     * 提交生成任务:校验参数、落 PROCESSING 记录后立即返回 generationId。
     *
     * AI 生图与 OSS 上传在线程池后台执行(见 executeGeneration),
     * 不再占用 HTTP 请求线程, 前端凭 generationId 轮询进度。
     */
    @Override
    public GenerationSubmitVO generate(Long userId, GeneratePatternRequest request) {
        if (userId == null) {
            throw new BusinessException("当前用户信息不存在");
        }
        if (request == null) {
            throw new BusinessException("生成参数不能为空");
        }

        normalizeRequest(request);
        validateRequest(request);

        LocalDateTime now = LocalDateTime.now();
        String keyword = request.getKeyword().trim();
        String elementsJson = toJson(request.getElements());
        String elementsText = String.join("、", request.getElements());
        String promptText = buildPromptText(request, elementsText);

        PatternGeneration generation = buildGeneration(
                userId,
                request,
                keyword,
                elementsJson,
                promptText,
                now
        );
        generation.setProgress(0);
        generation.setUpdatedAt(now);

        // 第一步:独立事务先落 PROCESSING 记录,确保生成失败也能留痕
        self.saveGenerationInitially(generation);

        // 第二步:丢进线程池后台执行, 立即返回
        Long generationId = generation.getId();
        aiGenerationExecutor.execute(
                () -> executeGeneration(userId, request, generationId, keyword, elementsJson, promptText)
        );

        GenerationSubmitVO response = new GenerationSubmitVO();
        response.setGenerationId(generationId);
        response.setStatus(generation.getStatus());
        return response;
    }

    /**
     * 后台线程执行:逐张调用 AI 生成并上传 OSS,每完成一张落库一张并推进进度。
     * 全部完成置 SUCCESS, 中途异常置 FAILED 并记录原因(已完成的纹样保留)。
     */
    private void executeGeneration(Long userId,
                                   GeneratePatternRequest request,
                                   Long generationId,
                                   String keyword,
                                   String elementsJson,
                                   String promptText) {
        int count = request.getGenerateCount();
        try {
            for (int index = 1; index <= count; index++) {
                List<byte[]> images = aiImageService.generateImages(promptText, 1);
                if (images == null || images.isEmpty()) {
                    throw new BusinessException("AI 未返回生成结果");
                }
                String imageUrl = ossFileService.uploadBytes(images.get(0), ".png", "PATTERN");
                self.recordPatternCompleted(
                        userId, request, generationId, keyword, elementsJson, imageUrl, index, count
                );
            }
            self.markGenerationSuccess(generationId);
            log.info("AI 生成任务 {} 完成, 共生成 {} 张纹样", generationId, count);
        } catch (Exception e) {
            log.error("AI 生成任务 {} 失败: {}", generationId, e.getMessage(), e);
            self.markGenerationFailed(generationId, e.getMessage());
        }
    }

    /**
     * 基于历史生成记录重新生成，并创建新的生成记录。
     */
    @Override
    public GenerationSubmitVO regenerate(Long userId,
                                         RegeneratePatternRequest regenerateRequest) {
        if (userId == null) {
            throw new BusinessException("当前用户信息不存在");
        }
        if (regenerateRequest == null || regenerateRequest.getGenerationId() == null) {
            throw new BusinessException("原生成记录 ID 不能为空");
        }

        PatternGeneration source = patternGenerationService.getById(
                regenerateRequest.getGenerationId()
        );
        if (source == null || Integer.valueOf(1).equals(source.getDeleted())) {
            throw new BusinessException("原生成记录不存在");
        }
        if (!userId.equals(source.getUserId())) {
            throw new BusinessException("无权重新生成他人的纹样");
        }

        GeneratePatternRequest request = buildRequestFromGeneration(
                source,
                regenerateRequest
        );
        return generate(userId, request);
    }

    /**
     * 统一转换前端提交的中文选项。
     */
    private void normalizeRequest(GeneratePatternRequest request) {
        request.setStyle(normalizeValue(request.getStyle(), STYLE_MAPPING));
        request.setColorTheme(normalizeValue(request.getColorTheme(), COLOR_MAPPING));
        request.setUsageScene(normalizeValue(request.getUsageScene(), SCENE_MAPPING));

        if (request.getKeyword() != null) {
            request.setKeyword(request.getKeyword().trim());
        }
        if (request.getDescription() != null) {
            request.setDescription(request.getDescription().trim());
        }
        if (request.getReferenceImageUrl() != null) {
            request.setReferenceImageUrl(request.getReferenceImageUrl().trim());
        }
    }

    /**
     * 创建 AI 生成记录。
     */
    private PatternGeneration buildGeneration(Long userId,
                                              GeneratePatternRequest request,
                                              String keyword,
                                              String elementsJson,
                                              String promptText,
                                              LocalDateTime now) {
        PatternGeneration generation = new PatternGeneration();
        generation.setUserId(userId);
        generation.setKeyword(keyword);
        generation.setStyle(request.getStyle());
        generation.setElements(elementsJson);
        generation.setColorTheme(request.getColorTheme());
        generation.setUsageScene(request.getUsageScene());
        generation.setDescription(request.getDescription());
        generation.setReferenceImageUrl(request.getReferenceImageUrl());
        generation.setPromptText(promptText);
        generation.setGenerateCount(request.getGenerateCount());
        generation.setStatus("PROCESSING");
        generation.setErrorMessage(null);
        generation.setDeleted(0);
        generation.setCreatedAt(now);
        return generation;
    }

    /**
     * 创建单张已完成纹样的记录。
     */
    private Pattern buildPattern(Long userId,
                                 GeneratePatternRequest request,
                                 Long generationId,
                                 String keyword,
                                 String elementsJson,
                                 String imageUrl,
                                 int index) {
        LocalDateTime now = LocalDateTime.now();
        Pattern pattern = new Pattern();
        pattern.setGenerationId(generationId);
        pattern.setUserId(userId);
        pattern.setTitle(buildPatternTitle(request, index));
        pattern.setImageUrl(imageUrl);
        pattern.setThumbnailUrl(imageUrl);
        pattern.setKeyword(keyword);
        pattern.setStyle(request.getStyle());
        pattern.setElements(elementsJson);
        pattern.setColorTheme(request.getColorTheme());
        pattern.setUsageScene(request.getUsageScene());
        pattern.setDescription(request.getDescription());
        pattern.setIsSaved(0);
        pattern.setIsFavorite(0);
        pattern.setIsRecommend(0);
        pattern.setViewCount(0);
        pattern.setLikeCount(0);
        pattern.setUseCount(0);
        pattern.setStatus(PatternStatus.NORMAL);
        pattern.setDeleted(0);
        pattern.setCreatedAt(now);
        pattern.setUpdatedAt(now);
        return pattern;
    }

    /**
     * 校验生成参数。
     */
    private void validateRequest(GeneratePatternRequest request) {
        if (!hasText(request.getKeyword())) {
            throw new BusinessException("请输入纹样主题关键词");
        }
        if (request.getKeyword().length() > 255) {
            throw new BusinessException("纹样主题关键词不能超过 255 个字符");
        }
        if (!hasText(request.getStyle())) {
            throw new BusinessException("请选择纹样风格");
        }
        if (!SUPPORTED_STYLES.contains(request.getStyle())) {
            throw new BusinessException("暂不支持该纹样风格：" + request.getStyle());
        }
        if (request.getElements() == null || request.getElements().isEmpty()) {
            throw new BusinessException("请至少选择一个纹样元素");
        }
        if (request.getElements().size() > 8) {
            throw new BusinessException("纹样元素最多选择 8 个");
        }
        for (String element : request.getElements()) {
            if (!hasText(element)) {
                throw new BusinessException("纹样元素不能为空");
            }
        }
        if (!hasText(request.getColorTheme())) {
            throw new BusinessException("请选择配色方案");
        }
        if (!SUPPORTED_COLORS.contains(request.getColorTheme())) {
            throw new BusinessException("暂不支持该配色方案：" + request.getColorTheme());
        }
        if (!hasText(request.getUsageScene())) {
            throw new BusinessException("请选择应用场景");
        }
        if (!SUPPORTED_SCENES.contains(request.getUsageScene())) {
            throw new BusinessException("暂不支持该应用场景：" + request.getUsageScene());
        }
        if (request.getDescription() != null
                && request.getDescription().length() > 500) {
            throw new BusinessException("补充描述不能超过 500 个字符");
        }
        if (request.getGenerateCount() == null
                || request.getGenerateCount() < 1
                || request.getGenerateCount() > 4) {
            throw new BusinessException("生成数量必须为 1 至 4 张");
        }
    }

    /**
     * 生成 Mock 提示词。
     */
    private String buildPromptText(GeneratePatternRequest request,
                                   String elementsText) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("生成一组广绣纹样，");
        prompt.append("主题：").append(request.getKeyword()).append("；");
        prompt.append("风格：")
                .append(getStyleDisplayName(request.getStyle()))
                .append("；");
        prompt.append("元素：").append(elementsText).append("；");
        prompt.append("配色：")
                .append(getColorDisplayName(request.getColorTheme()))
                .append("；");
        prompt.append("应用场景：")
                .append(getSceneDisplayName(request.getUsageScene()))
                .append("；");

        if (hasText(request.getDescription())) {
            prompt.append("用户描述：")
                    .append(request.getDescription())
                    .append("；");
        }
        if (hasText(request.getReferenceImageUrl())) {
            prompt.append("参考图：")
                    .append(request.getReferenceImageUrl())
                    .append("；");
        }

        prompt.append("广绣丝线质感，构图完整，高清，无文字，无水印。");
        return prompt.toString();
    }

    /**
     * 独立事务：先落一条 PROCESSING 状态的生成记录，保证后续生成失败也能留下痕迹。
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveGenerationInitially(PatternGeneration generation) {
        if (!patternGenerationService.save(generation) || generation.getId() == null) {
            throw new BusinessException("生成任务保存失败");
        }
    }

    /**
     * 独立事务：某张图片生成完成后保存该纹样，并推进生成进度(已完成数/总数)。
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordPatternCompleted(Long userId,
                                       GeneratePatternRequest request,
                                       Long generationId,
                                       String keyword,
                                       String elementsJson,
                                       String imageUrl,
                                       int index,
                                       int total) {
        Pattern pattern = buildPattern(userId, request, generationId, keyword, elementsJson, imageUrl, index);
        if (!patternService.save(pattern)) {
            throw new BusinessException("纹样结果保存失败");
        }
        PatternGeneration update = new PatternGeneration();
        update.setId(generationId);
        update.setProgress(index * 100 / total);
        update.setUpdatedAt(LocalDateTime.now());
        patternGenerationService.updateById(update);
    }

    /**
     * 独立事务：全部图片完成后把生成记录置为 SUCCESS。
     */
    @Transactional(rollbackFor = Exception.class)
    public void markGenerationSuccess(Long generationId) {
        PatternGeneration update = new PatternGeneration();
        update.setId(generationId);
        update.setStatus("SUCCESS");
        update.setProgress(100);
        update.setUpdatedAt(LocalDateTime.now());
        patternGenerationService.updateById(update);
    }

    /**
     * 独立事务：生成失败时把记录置为 FAILED 并记录错误原因。
     */
    @Transactional(rollbackFor = Exception.class)
    public void markGenerationFailed(Long generationId, String errorMessage) {
        PatternGeneration update = new PatternGeneration();
        update.setId(generationId);
        update.setStatus("FAILED");
        if (errorMessage != null && errorMessage.length() > 500) {
            errorMessage = errorMessage.substring(0, 500);
        }
        update.setErrorMessage(errorMessage);
        update.setUpdatedAt(LocalDateTime.now());
        patternGenerationService.updateById(update);
    }

    /**
     * 构建前端显示的纹样标题。
     */
    private String buildPatternTitle(GeneratePatternRequest request, int index) {
        String suffix = request.getGenerateCount() > 1
                ? "广绣纹样 " + index
                : "广绣纹样";
        String keyword = request.getKeyword();
        int maxKeywordLength = 100 - suffix.length();
        if (keyword.length() > maxKeywordLength) {
            keyword = keyword.substring(0, maxKeywordLength);
        }
        return keyword + suffix;
    }
    /**
     * 将历史记录和本次覆盖参数合并。
     */
    private GeneratePatternRequest buildRequestFromGeneration(
            PatternGeneration source,
            RegeneratePatternRequest override) {
        GeneratePatternRequest request = new GeneratePatternRequest();
        request.setKeyword(
                hasText(override.getKeyword())
                        ? override.getKeyword()
                        : source.getKeyword()
        );
        request.setStyle(
                hasText(override.getStyle())
                        ? override.getStyle()
                        : source.getStyle()
        );
        request.setElements(
                override.getElements() != null
                        ? override.getElements()
                        : parseElementsJson(source.getElements())
        );
        request.setColorTheme(
                hasText(override.getColorTheme())
                        ? override.getColorTheme()
                        : source.getColorTheme()
        );
        request.setUsageScene(
                hasText(override.getUsageScene())
                        ? override.getUsageScene()
                        : source.getUsageScene()
        );
        request.setDescription(
                override.getDescription() != null
                        ? override.getDescription()
                        : source.getDescription()
        );
        request.setReferenceImageUrl(source.getReferenceImageUrl());
        request.setGenerateCount(
                override.getGenerateCount() != null
                        ? override.getGenerateCount()
                        : source.getGenerateCount()
        );
        return request;
    }

    /**
     * 将纹样元素列表转换成 JSON 字符串。
     */
    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new BusinessException("纹样元素数据转换失败");
        }
    }

    /**
     * 将历史记录中的元素 JSON 解析为列表。
     */
    private List<String> parseElementsJson(String elementsJson) {
        if (!hasText(elementsJson)) {
            throw new BusinessException("原生成记录元素数据为空");
        }

        try {
            return objectMapper.readValue(
                    elementsJson,
                    objectMapper.getTypeFactory().constructCollectionType(
                            List.class,
                            String.class
                    )
            );
        } catch (Exception e) {
            throw new BusinessException("原生成记录元素数据解析失败");
        }
    }

    /**
     * 将前端中文名称转换为后端内部编码。
     */
    private String normalizeValue(String value,
                                  Map<String, String> mapping) {
        if (!hasText(value)) {
            return value;
        }

        String trimmedValue = value.trim();
        String mappedValue = mapping.get(trimmedValue);
        return mappedValue == null ? trimmedValue : mappedValue;
    }

    /**
     * 判断元素中是否包含指定关键词。
     */
    private boolean containsElement(List<String> elements,
                                    String keyword) {
        if (elements == null) {
            return false;
        }

        for (String element : elements) {
            if (element != null && element.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String getStyleDisplayName(String style) {
        if ("classic".equals(style)) {
            return "广绣经典";
        }
        if ("new_chinese".equals(style)) {
            return "新中式";
        }
        if ("embroidery".equals(style)) {
            return "刺绣纹样";
        }
        if ("lingnan_window".equals(style)) {
            return "岭南花窗";
        }
        return style;
    }

    private String getColorDisplayName(String colorTheme) {
        if ("red_gold".equals(colorTheme) || "rich_color".equals(colorTheme)) {
            return "富贵华彩";
        }
        if ("chinese_elegant".equals(colorTheme)) {
            return "国风雅韵";
        }
        if ("soft_elegant".equals(colorTheme)) {
            return "清润素韵";
        }
        return colorTheme;
    }

    private String getSceneDisplayName(String usageScene) {
        if ("product".equals(usageScene)) {
            return "文创产品";
        }
        if ("poster".equals(usageScene)) {
            return "海报设计";
        }
        if ("clothing".equals(usageScene)) {
            return "服饰刺绣";
        }
        if ("home".equals(usageScene)) {
            return "家居软装";
        }
        if ("package".equals(usageScene)) {
            return "礼品包装";
        }
        return usageScene;
    }
}