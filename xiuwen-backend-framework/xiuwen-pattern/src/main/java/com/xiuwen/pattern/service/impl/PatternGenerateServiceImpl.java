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
import com.xiuwen.pattern.vo.GeneratePatternResponse;
import com.xiuwen.pattern.vo.PatternItemVO;
import com.xiuwen.framework.service.OssFileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI 纹样生成业务实现。
 *
 * 第一版使用固定 Mock 图片，后续替换为真实 AI 调用。
 */
@Service
public class PatternGenerateServiceImpl implements PatternGenerateService {
    /**
     * 当前已经放入项目中的 Mock 纹样图片。
     */
    private static final List<String> MOCK_IMAGE_PATHS = Arrays.asList(
            "demo/pattern/peony-phoenix-pattern-01.jpg",
            "demo/pattern/peony-phoenix-pattern-02.jpg",
            "demo/pattern/lingnan-window-pattern-01.jpg",
            "demo/pattern/round-flower-pattern-01.jpg",
            "demo/pattern/lion-dance-pattern-01.jpg"
    );

    /**
     * 后端内部支持的纹样风格编码。
     */
    private static final List<String> SUPPORTED_STYLES = Arrays.asList(
            "classic",
            "new_chinese"
    );

    /**
     * 后端内部支持的配色编码。
     */
    private static final List<String> SUPPORTED_COLORS = Arrays.asList(
            "chinese_elegant",
            "red_gold"
    );

    /**
     * 后端内部支持的应用场景编码。
     */
    private static final List<String> SUPPORTED_SCENES = Arrays.asList(
            "product",
            "poster"
    );

    /**
     * 前端中文名称与后端编码映射。
     */
    private static final Map<String, String> STYLE_MAPPING = new HashMap<>();
    private static final Map<String, String> COLOR_MAPPING = new HashMap<>();
    private static final Map<String, String> SCENE_MAPPING = new HashMap<>();

    static {
        STYLE_MAPPING.put("经典广绣", "classic");
        STYLE_MAPPING.put("传统广绣", "classic");
        STYLE_MAPPING.put("经典", "classic");
        STYLE_MAPPING.put("新中式", "new_chinese");
        STYLE_MAPPING.put("新中式风格", "new_chinese");

        COLOR_MAPPING.put("富贵华彩", "red_gold");
        COLOR_MAPPING.put("红金华彩", "red_gold");
        COLOR_MAPPING.put("红金配色", "red_gold");
        COLOR_MAPPING.put("国风雅韵", "chinese_elegant");
        COLOR_MAPPING.put("清雅素韵", "chinese_elegant");
        COLOR_MAPPING.put("中式雅韵", "chinese_elegant");

        SCENE_MAPPING.put("文创产品", "product");
        SCENE_MAPPING.put("产品定制", "product");
        SCENE_MAPPING.put("文创定制", "product");
        SCENE_MAPPING.put("海报设计", "poster");
        SCENE_MAPPING.put("宣传海报", "poster");
        SCENE_MAPPING.put("海报", "poster");
    }

    private final PatternGenerationService patternGenerationService;
    private final PatternService patternService;
    private final ObjectMapper objectMapper;
    private final OssFileService ossFileService;

    public PatternGenerateServiceImpl(PatternGenerationService patternGenerationService,
                                      PatternService patternService,
                                      ObjectMapper objectMapper,
                                      OssFileService ossFileService) {
        this.patternGenerationService = patternGenerationService;
        this.patternService = patternService;
        this.objectMapper = objectMapper;
        this.ossFileService = ossFileService;
    }

    /**
     * 创建生成记录、生成 Mock 纹样并返回结果。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GeneratePatternResponse generate(Long userId, GeneratePatternRequest request) {
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

        boolean generationSaved = patternGenerationService.save(generation);
        if (!generationSaved || generation.getId() == null) {
            throw new BusinessException("生成任务保存失败");
        }

        List<String> imageUrls = selectMockImages(request);
        List<Pattern> patterns = buildPatterns(
                userId,
                request,
                generation.getId(),
                keyword,
                elementsJson,
                imageUrls,
                now
        );

        boolean patternSaved = patternService.saveBatch(patterns);
        if (!patternSaved) {
            throw new BusinessException("纹样结果保存失败");
        }

        GeneratePatternResponse response = new GeneratePatternResponse();
        response.setGenerationId(generation.getId());
        response.setStatus("SUCCESS");
        response.setPatterns(toPatternItems(patterns));
        return response;
    }

    /**
     * 基于历史生成记录重新生成，并创建新的生成记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GeneratePatternResponse regenerate(Long userId,
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
        generation.setStatus("SUCCESS");
        generation.setErrorMessage(null);
        generation.setDeleted(0);
        generation.setCreatedAt(now);
        return generation;
    }

    /**
     * 创建本次生成的纹样记录。
     */
    private List<Pattern> buildPatterns(Long userId,
                                        GeneratePatternRequest request,
                                        Long generationId,
                                        String keyword,
                                        String elementsJson,
                                        List<String> imageUrls,
                                        LocalDateTime now) {
        List<Pattern> patterns = new ArrayList<>();

        for (int i = 0; i < imageUrls.size(); i++) {
            String imageUrl = imageUrls.get(i);

            Pattern pattern = new Pattern();
            pattern.setGenerationId(generationId);
            pattern.setUserId(userId);
            pattern.setTitle(buildPatternTitle(request, i + 1));
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
            patterns.add(pattern);
        }

        return patterns;
    }

    /**
     * 转换成前端需要的生成结果。
     */
    private List<PatternItemVO> toPatternItems(List<Pattern> patterns) {
        List<PatternItemVO> items = new ArrayList<>();

        for (Pattern pattern : patterns) {
            PatternItemVO item = new PatternItemVO();
            item.setId(pattern.getId());
            item.setTitle(pattern.getTitle());
            item.setImageUrl(pattern.getImageUrl());
            item.setThumbnailUrl(pattern.getThumbnailUrl());
            item.setStyle(pattern.getStyle());
            item.setIsFavorite(false);
            item.setCreatedAt(pattern.getCreatedAt());
            items.add(item);
        }

        return items;
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
     * 根据用户选项调整 Mock 图片顺序。
     */
    private List<String> selectMockImages(GeneratePatternRequest request) {
        String ossDomain = ossFileService.getOssDomain();
        Set<String> orderedImages = new LinkedHashSet<>();

        if (containsElement(request.getElements(), "醒狮")) {
            orderedImages.add(ossDomain + "demo/pattern/lion-dance-pattern-01.jpg");
        }
        if ("new_chinese".equals(request.getStyle())) {
            orderedImages.add(ossDomain + "demo/pattern/lingnan-window-pattern-01.jpg");
        }
        if (containsElement(request.getElements(), "团花")
                || containsElement(request.getElements(), "莲花")) {
            orderedImages.add(ossDomain + "demo/pattern/round-flower-pattern-01.jpg");
        }

        for (String path : MOCK_IMAGE_PATHS) {
            orderedImages.add(ossDomain + path);
        }
        List<String> result = new ArrayList<>(orderedImages);
        return new ArrayList<>(
                result.subList(0, request.getGenerateCount())
        );
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
            return "经典广绣";
        }
        if ("new_chinese".equals(style)) {
            return "新中式";
        }
        return style;
    }

    private String getColorDisplayName(String colorTheme) {
        if ("red_gold".equals(colorTheme)) {
            return "富贵华彩";
        }
        if ("chinese_elegant".equals(colorTheme)) {
            return "国风雅韵";
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
        return usageScene;
    }
}