package com.xiuwen.pattern.service.impl;

import com.xiuwen.common.constant.PatternStatus;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.pattern.dto.GeneratePatternRequest;
import com.xiuwen.pattern.entity.Pattern;
import com.xiuwen.pattern.entity.PatternGeneration;
import com.xiuwen.pattern.service.PatternGenerateService;
import com.xiuwen.pattern.service.PatternGenerationService;
import com.xiuwen.pattern.service.PatternService;
import com.xiuwen.pattern.vo.GeneratePatternResponse;
import com.xiuwen.pattern.vo.PatternItemVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;



/**
 * AI纹样生成业务实现。
 *
 * 第一版使用固定Mock图片，
 * 后续替换为真实AI调用。
 */
@Service
public class PatternGenerateServiceImpl
        implements PatternGenerateService {

    /**
     * 当前已经放入项目中的Mock纹样图片。
     */
    private static final List<String> MOCK_IMAGE_URLS =
            Arrays.asList(
                    "/api/uploads/demo/pattern/"
                            + "peony-phoenix-pattern-01.jpg",

                    "/api/uploads/demo/pattern/"
                            + "peony-phoenix-pattern-02.jpg",

                    "/api/uploads/demo/pattern/"
                            + "lingnan-window-pattern-01.jpg",

                    "/api/uploads/demo/pattern/"
                            + "round-flower-pattern-01.jpg",

                    "/api/uploads/demo/pattern/"
                            + "lion-dance-pattern-01.jpg"
            );

    /**
     * 后端内部支持的纹样风格编码。
     */
    private static final List<String> SUPPORTED_STYLES =
            Arrays.asList(
                    "classic",
                    "new_chinese"
            );

    /**
     * 后端内部支持的配色编码。
     */
    private static final List<String> SUPPORTED_COLORS =
            Arrays.asList(
                    "chinese_elegant",
                    "red_gold"
            );

    /**
     * 后端内部支持的应用场景编码。
     */
    private static final List<String> SUPPORTED_SCENES =
            Arrays.asList(
                    "product",
                    "poster"
            );

    /**
     * 前端中文风格名称与后端编码映射。
     */
    private static final Map<String, String> STYLE_MAPPING =
            new HashMap<String, String>();

    /**
     * 前端中文配色名称与后端编码映射。
     */
    private static final Map<String, String> COLOR_MAPPING =
            new HashMap<String, String>();

    /**
     * 前端中文应用场景与后端编码映射。
     */
    private static final Map<String, String> SCENE_MAPPING =
            new HashMap<String, String>();

    static {
        /*
         * 纹样风格
         */
        STYLE_MAPPING.put(
                "经典广绣",
                "classic"
        );

        STYLE_MAPPING.put(
                "传统广绣",
                "classic"
        );

        STYLE_MAPPING.put(
                "经典",
                "classic"
        );

        STYLE_MAPPING.put(
                "新中式",
                "new_chinese"
        );

        STYLE_MAPPING.put(
                "新中式风格",
                "new_chinese"
        );

        /*
         * 配色方案
         */
        COLOR_MAPPING.put(
                "富贵华彩",
                "red_gold"
        );

        COLOR_MAPPING.put(
                "红金华彩",
                "red_gold"
        );

        COLOR_MAPPING.put(
                "红金配色",
                "red_gold"
        );

        COLOR_MAPPING.put(
                "国风雅韵",
                "chinese_elegant"
        );

        COLOR_MAPPING.put(
                "清雅素韵",
                "chinese_elegant"
        );

        COLOR_MAPPING.put(
                "中式雅韵",
                "chinese_elegant"
        );

        /*
         * 应用场景
         */
        SCENE_MAPPING.put(
                "文创产品",
                "product"
        );

        SCENE_MAPPING.put(
                "产品定制",
                "product"
        );

        SCENE_MAPPING.put(
                "文创定制",
                "product"
        );

        SCENE_MAPPING.put(
                "海报设计",
                "poster"
        );

        SCENE_MAPPING.put(
                "宣传海报",
                "poster"
        );

        SCENE_MAPPING.put(
                "海报",
                "poster"
        );
    }

    private final PatternGenerationService
            patternGenerationService;

    private final PatternService patternService;

    private final ObjectMapper objectMapper;
    public PatternGenerateServiceImpl(
            PatternGenerationService
                    patternGenerationService,
            PatternService patternService,
            ObjectMapper objectMapper) {

        this.patternGenerationService =
                patternGenerationService;

        this.patternService = patternService;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建生成记录、生成Mock纹样并返回结果。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GeneratePatternResponse generate(
            Long userId,
            GeneratePatternRequest request) {

        if (userId == null) {
            throw new BusinessException(
                    "当前用户信息不存在"
            );
        }

        if (request == null) {
            throw new BusinessException(
                    "生成参数不能为空"
            );
        }

        /*
         * 兼容前端提交的中文选项。
         *
         * 如果前端传的是英文编码，
         * normalizeValue会直接保留。
         */
        request.setStyle(
                normalizeValue(
                        request.getStyle(),
                        STYLE_MAPPING
                )
        );

        request.setColorTheme(
                normalizeValue(
                        request.getColorTheme(),
                        COLOR_MAPPING
                )
        );

        request.setUsageScene(
                normalizeValue(
                        request.getUsageScene(),
                        SCENE_MAPPING
                )
        );

        /*
         * 必须先转换中文，再校验。
         */
        validateRequest(request);

        LocalDateTime now =
                LocalDateTime.now();

        String elementsText =
                String.join(
                        ",",
                        request.getElements()
                );
        String elementsJson = toJson(request.getElements());

        String keyword =
                request.getElements().get(0);

        String promptText =
                buildPromptText(
                        request,
                        elementsText
                );

        /*
         * 1. 创建本次AI生成任务记录。
         */
        PatternGeneration generation =
                new PatternGeneration();

        generation.setUserId(userId);
        generation.setKeyword(keyword);
        generation.setStyle(
                request.getStyle()
        );
        generation.setElements(
                elementsJson
        );
        generation.setUsageScene(
                request.getUsageScene()
        );
        generation.setColorTheme(
                request.getColorTheme()
        );
        generation.setPromptText(
                promptText
        );
        generation.setGenerateCount(
                request.getCount()
        );
        generation.setStatus(
                "SUCCESS"
        );
        generation.setErrorMessage(null);
        generation.setDeleted(0);
        generation.setCreatedAt(now);

        boolean generationSaved =
                patternGenerationService.save(
                        generation
                );

        if (!generationSaved
                || generation.getId() == null) {

            throw new BusinessException(
                    "生成任务保存失败"
            );
        }

        /*
         * 2. 根据用户参数选择Mock图片。
         */
        List<String> imageUrls =
                selectMockImages(request);

        /*
         * 3. 创建纹样记录。
         *
         * 前端后续操作需要patternId，
         * 所以生成阶段先创建pattern记录。
         */
        List<Pattern> patterns =
                new ArrayList<Pattern>();

        for (int i = 0;
             i < imageUrls.size();
             i++) {

            String imageUrl =
                    imageUrls.get(i);

            Pattern pattern =
                    new Pattern();

            pattern.setGenerationId(
                    generation.getId()
            );

            pattern.setUserId(userId);

            pattern.setTitle(
                    buildPatternTitle(
                            request,
                            i + 1
                    )
            );

            pattern.setImageUrl(
                    imageUrl
            );

            pattern.setThumbnailUrl(
                    imageUrl
            );

            pattern.setKeyword(
                    keyword
            );

            pattern.setStyle(
                    request.getStyle()
            );

            pattern.setElements(
                    elementsJson
            );

            pattern.setUsageScene(
                    request.getUsageScene()
            );

            pattern.setColorTheme(
                    request.getColorTheme()
            );

            pattern.setDescription(
                    request.getDescription()
            );

            pattern.setIsSaved(0);
            pattern.setIsFavorite(0);
            pattern.setIsRecommend(0);
            pattern.setViewCount(0);
            pattern.setLikeCount(0);
            pattern.setUseCount(0);

            pattern.setStatus(
                    PatternStatus.NORMAL
            );

            pattern.setDeleted(0);
            pattern.setCreatedAt(now);
            pattern.setUpdatedAt(now);

            patterns.add(pattern);
        }

        boolean patternSaved =
                patternService.saveBatch(
                        patterns
                );

        if (!patternSaved) {
            throw new BusinessException(
                    "纹样结果保存失败"
            );
        }

        /*
         * 4. 转换成前端需要的返回结构。
         */
        List<PatternItemVO> patternItems =
                new ArrayList<PatternItemVO>();

        for (Pattern pattern : patterns) {

            PatternItemVO item =
                    new PatternItemVO();

            item.setId(
                    pattern.getId()
            );

            item.setTitle(
                    pattern.getTitle()
            );

            item.setImageUrl(
                    pattern.getImageUrl()
            );

            item.setThumbnailUrl(
                    pattern.getThumbnailUrl()
            );

            item.setStyle(
                    pattern.getStyle()
            );

            item.setIsFavorite(false);

            item.setCreatedAt(
                    pattern.getCreatedAt()
            );

            patternItems.add(item);
        }

        GeneratePatternResponse response =
                new GeneratePatternResponse();

        response.setGenerationId(
                generation.getId()
        );

        response.setStatus(
                "SUCCESS"
        );

        response.setPatterns(
                patternItems
        );

        return response;
    }
    /**
     * 将纹样元素列表转换成合法JSON字符串。
     *
     * 示例：
     * ["牡丹", "凤凰"]
     * 转换为：
     * ["牡丹","凤凰"]
     */
    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    "纹样元素数据转换失败"
            );
        }
    }
    /**
     * 将前端中文名称转换为后端内部编码。
     *
     * 英文编码会直接保留。
     */
    private String normalizeValue(
            String value,
            Map<String, String> mapping) {

        if (value == null
                || value.trim().isEmpty()) {
            return value;
        }

        String trimmedValue =
                value.trim();

        String mappedValue =
                mapping.get(trimmedValue);

        if (mappedValue != null) {
            return mappedValue;
        }

        return trimmedValue;
    }

    /**
     * 校验前端提交的数据。
     */
    private void validateRequest(
            GeneratePatternRequest request) {

        if (request == null) {
            throw new BusinessException(
                    "生成参数不能为空"
            );
        }

        /*
         * 校验纹样风格。
         */
        if (request.getStyle() == null
                || request.getStyle()
                .trim()
                .isEmpty()) {

            throw new BusinessException(
                    "请选择纹样风格"
            );
        }

        if (!SUPPORTED_STYLES.contains(
                request.getStyle())) {

            throw new BusinessException(
                    "暂不支持该纹样风格："
                            + request.getStyle()
            );
        }

        /*
         * 校验纹样元素。
         */
        if (request.getElements() == null
                || request.getElements()
                .isEmpty()) {

            throw new BusinessException(
                    "请至少选择一个纹样元素"
            );
        }

        /*
         * 校验配色方案。
         */
        if (request.getColorTheme() == null
                || request.getColorTheme()
                .trim()
                .isEmpty()) {

            throw new BusinessException(
                    "请选择配色方案"
            );
        }

        if (!SUPPORTED_COLORS.contains(
                request.getColorTheme())) {

            throw new BusinessException(
                    "暂不支持该配色方案："
                            + request.getColorTheme()
            );
        }

        /*
         * 校验应用场景。
         */
        if (request.getUsageScene() == null
                || request.getUsageScene()
                .trim()
                .isEmpty()) {

            throw new BusinessException(
                    "请选择应用场景"
            );
        }

        if (!SUPPORTED_SCENES.contains(
                request.getUsageScene())) {

            throw new BusinessException(
                    "暂不支持该应用场景："
                            + request.getUsageScene()
            );
        }

        /*
         * 校验生成数量。
         */
        if (request.getCount() == null
                || request.getCount() < 1
                || request.getCount() > 4) {

            throw new BusinessException(
                    "生成数量必须为1至4张"
            );
        }
    }

    /**
     * 生成Mock提示词。
     *
     * 后续接入真实AI时，
     * 这段内容可以传给AI服务。
     */
    private String buildPromptText(
            GeneratePatternRequest request,
            String elementsText) {

        StringBuilder prompt =
                new StringBuilder();

        prompt.append(
                "生成一组广绣纹样，"
        );

        prompt.append("风格：")
                .append(
                        getStyleDisplayName(
                                request.getStyle()
                        )
                )
                .append("；");

        prompt.append("元素：")
                .append(elementsText)
                .append("；");

        prompt.append("配色：")
                .append(
                        getColorDisplayName(
                                request.getColorTheme()
                        )
                )
                .append("；");

        prompt.append("应用场景：")
                .append(
                        getSceneDisplayName(
                                request.getUsageScene()
                        )
                )
                .append("；");

        if (request.getDescription() != null
                && !request.getDescription()
                .trim()
                .isEmpty()) {

            prompt.append("用户描述：")
                    .append(
                            request
                                    .getDescription()
                                    .trim()
                    )
                    .append("；");
        }

        prompt.append(
                "广绣丝线质感，构图完整，"
        );

        prompt.append(
                "高清，无文字，无水印。"
        );

        return prompt.toString();
    }

    /**
     * 将风格编码转换为中文名称。
     */
    private String getStyleDisplayName(
            String style) {

        if ("classic".equals(style)) {
            return "经典广绣";
        }

        if ("new_chinese".equals(style)) {
            return "新中式";
        }

        return style;
    }

    /**
     * 将配色编码转换为中文名称。
     */
    private String getColorDisplayName(
            String colorTheme) {

        if ("red_gold".equals(
                colorTheme)) {
            return "富贵华彩";
        }

        if ("chinese_elegant".equals(
                colorTheme)) {
            return "国风雅韵";
        }

        return colorTheme;
    }

    /**
     * 将应用场景编码转换为中文名称。
     */
    private String getSceneDisplayName(
            String usageScene) {

        if ("product".equals(
                usageScene)) {
            return "文创产品";
        }

        if ("poster".equals(
                usageScene)) {
            return "海报设计";
        }

        return usageScene;
    }

    /**
     * 根据用户选项调整Mock图片顺序。
     */
    private List<String> selectMockImages(
            GeneratePatternRequest request) {

        Set<String> orderedImages =
                new LinkedHashSet<String>();

        /*
         * 用户选择醒狮时，
         * 优先返回醒狮纹样。
         */
        if (containsElement(
                request.getElements(),
                "醒狮")) {

            orderedImages.add(
                    "/api/uploads/demo/pattern/"
                            + "lion-dance-pattern-01.jpg"
            );
        }

        /*
         * 新中式风格优先返回岭南花窗。
         */
        if ("new_chinese".equals(
                request.getStyle())) {

            orderedImages.add(
                    "/api/uploads/demo/pattern/"
                            + "lingnan-window-pattern-01.jpg"
            );
        }

        /*
         * 用户选择团花或莲花时，
         * 优先返回圆形团花纹样。
         */
        if (containsElement(
                request.getElements(),
                "团花")
                || containsElement(
                request.getElements(),
                "莲花")) {

            orderedImages.add(
                    "/api/uploads/demo/pattern/"
                            + "round-flower-pattern-01.jpg"
            );
        }

        /*
         * 加入默认Mock图片。
         */
        orderedImages.addAll(
                MOCK_IMAGE_URLS
        );

        List<String> result =
                new ArrayList<String>(
                        orderedImages
                );

        return new ArrayList<String>(
                result.subList(
                        0,
                        request.getCount()
                )
        );
    }

    /**
     * 判断用户选择的元素中
     * 是否包含指定关键词。
     */
    private boolean containsElement(
            List<String> elements,
            String keyword) {

        if (elements == null) {
            return false;
        }

        for (String element : elements) {

            if (element != null
                    && element.contains(
                    keyword)) {

                return true;
            }
        }

        return false;
    }

    /**
     * 构建前端显示的纹样标题。
     */
    private String buildPatternTitle(
            GeneratePatternRequest request,
            int index) {

        StringBuilder title =
                new StringBuilder();

        int limit =
                Math.min(
                        request.getElements()
                                .size(),
                        2
                );

        for (int i = 0;
             i < limit;
             i++) {

            if (i > 0) {
                title.append("·");
            }

            title.append(
                    request.getElements()
                            .get(i)
            );
        }

        title.append(
                "广绣纹样"
        );

        if (request.getCount() > 1) {
            title.append(" ")
                    .append(index);
        }

        return title.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GeneratePatternResponse regenerate(Long userId, Long generationId) {
       if(userId == null ){
           throw new BusinessException("当前用户信息不存在");
       }
       if(generationId == null){
           throw new BusinessException("原生成记录id不能为空");
       }
        PatternGeneration sourceGeneration =
                patternGenerationService.getById(generationId);
       if(sourceGeneration == null|| Integer.valueOf(1).equals(
               sourceGeneration.getDeleted())){
           throw new BusinessException("原生记录不存在");
       }
       if(!userId.equals(sourceGeneration.getUserId())){
           throw new BusinessException("无权生成他人的纹样");
       }
      GeneratePatternRequest request = buildRequestFromGeneration(sourceGeneration);
       return generate(userId, request);
    }

    private GeneratePatternRequest buildRequestFromGeneration(PatternGeneration generation) {
        GeneratePatternRequest request = new GeneratePatternRequest();
        request.setStyle(generation.getStyle());
        request.setUsageScene(generation.getUsageScene());
        request.setColorTheme(generation.getColorTheme());
        request.setCount(generation.getGenerateCount());
        request.setElements(parseElementsJson(generation.getElements()));
        request.setDescription(null);
        return request;
    }
    private List<String> parseElementsJson(String elementsJson) {
        try {
            return objectMapper.readValue(
                    elementsJson,
                    objectMapper.getTypeFactory()
                            .constructCollectionType(
                                    List.class,
                                    String.class
                            )
            );
        } catch (Exception e) {
            throw new BusinessException("原生成记录元素数据解析失败");
        }
    }
}