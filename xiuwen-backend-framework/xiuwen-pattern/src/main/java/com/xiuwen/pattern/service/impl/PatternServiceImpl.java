package com.xiuwen.pattern.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.common.utils.StringUtils;
import com.xiuwen.pattern.dto.PatternMyQueryDTO;
import com.xiuwen.pattern.entity.Pattern;
import com.xiuwen.pattern.mapper.PatternMapper;
import com.xiuwen.pattern.service.PatternService;
import com.xiuwen.pattern.vo.PatternMyVO;
import com.xiuwen.system.entity.User;
import com.xiuwen.system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * pattern 表服务实现。
 */
@Service
@Slf4j

@RequiredArgsConstructor

public class PatternServiceImpl extends ServiceImpl<PatternMapper,Pattern> implements PatternService {

    private final PatternMapper patternMapper;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final UserService userService;

    //================================我的纹样列表=================================================
    public Map<String,Object> getMyPatterns(PatternMyQueryDTO queryDTO) {
  //构建分页
        Page<Pattern> page = new Page<>(queryDTO.getPage(), queryDTO.getPageSize());
        Long userId = queryDTO.getUserId();
        if(userId==null){
            throw new BusinessException("用户未登录,无法获取我的纹样");
        }
//构建查询条件
        LambdaQueryWrapper<Pattern> wrapper = new LambdaQueryWrapper<>();
        //基础条件,所属用户＋非删除状态
        wrapper.eq(Pattern::getUserId,userId)
                .ne(Pattern::getStatus,"DELETED");
        //tab筛选
        String tab =  queryDTO.getTab();
        if(StringUtils.isNotBlank(tab)) {
            switch (tab) {
                case "favorite"://收藏纹样
                    wrapper.eq(Pattern::getIsFavorite, "1");
                    break;
                case "saved"://保存到我的纹样
                    wrapper.eq(Pattern::getIsSaved, 1);
                    break;
                case "all":
                default:
                    wrapper.and(w -> w
                            .eq(Pattern::getIsSaved, 1)
                            .or()
                            .eq(Pattern::getIsFavorite, 1)
                    );
                    break;
            }
        }else {
            wrapper.eq(Pattern::getIsSaved, "1");
        }
        // 风格筛选
        if (StringUtils.isNotBlank(queryDTO.getStyle())) {
            wrapper.eq(Pattern::getStyle, queryDTO.getStyle());
        }
        // 关键词模糊搜索（匹配 title 或 keyword）
        if (StringUtils.isNotBlank(queryDTO.getKeyword())) {
            wrapper.and(w -> w
                    .like(Pattern::getTitle, queryDTO.getKeyword())
                    .or()
                    .like(Pattern::getKeyword, queryDTO.getKeyword())
            );
        }

        wrapper.orderByDesc(Pattern::getUpdatedAt);

        Page<Pattern> result = patternMapper.selectPage(page, wrapper);
        Map<String,Object> data = new HashMap<>();
        data.put("total",result.getTotal());
        data.put("page",queryDTO.getPage());
        data.put("pageSize",queryDTO.getPageSize());
        data.put("list",convertToVOList(result.getRecords()));
        return data;
    }
    //=====================================纹样详情=======================================
    @Override
    public PatternMyVO getPatternDetail(Long patternId, Long userId) {
            Pattern pattern = getPatternByIdOrThrow(patternId);
            if("DELETED".equals(pattern.getStatus())) {
                throw new BusinessException("纹样不存在或被删除");
            }
            if(userId!=null){
                patternMapper.update(
                        null,
                        new LambdaUpdateWrapper<Pattern>()
                                .eq(Pattern::getId,patternId)
                                .setSql("view_count = view_count + 1"));
            }
        PatternMyVO vo = convertToVO(pattern);
            if(pattern.getUserId()!=null){
                User user = userService.getById(pattern.getUserId());
                if(user!=null){vo.setUserNickname(user.getNickname());}
            }
            return vo;
    }
//保存我的纹样
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePattern(Long patternId, Long userId) {
     Pattern pattern = getPatternByIdOrThrow(patternId);
     if(!pattern.getUserId().equals(userId)){
         throw new BusinessException("无权操作他人纹样");
     }
     if(Integer.valueOf(1).equals(pattern.getStatus())) {
         log.info("纹样[{}]已保存,跳过重复操作",patternId);
         return;
     }
     patternMapper.update(null,new LambdaUpdateWrapper<Pattern>()
             .eq(Pattern::getId,patternId)
             .eq(Pattern::getUserId,userId)
             .set(Pattern::getIsSaved,1)
             .set(Pattern::getUpdatedAt,LocalDateTime.now()));
     log.info("用户[{}]保存纹样[{}]到我的纹样",userId,patternId);
    }
//收藏我的纹样
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favoritePattern(Long patternId, Long userId) {
        Pattern pattern = getPatternByIdOrThrow(patternId);
        if("DELETED".equals(pattern.getStatus())) {
            throw new BusinessException("纹样不存在或者被删除");
        }
        if(!pattern.getUserId().equals(userId)){
            throw new BusinessException("无权收藏他人的纹样");
        }
        if(Integer.valueOf(1).equals(pattern.getIsFavorite())){
            log.info("该纹样[{}]已经被收藏了",patternId);
            return;
        }
        patternMapper.update(null,new LambdaUpdateWrapper<Pattern>()
                .eq(Pattern::getId,patternId)
                .eq(Pattern::getUserId,userId)
                .set(Pattern::getIsFavorite,1)
                .setSql("like_count = like_count + 1")
                .set(Pattern::getUpdatedAt,LocalDateTime.now()));
        log.info("用户[{}]收藏纹样[{}]",userId,patternId);
    }
//取消收藏纹样
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfavoritePattern(Long patternId, Long userId) {
        Pattern pattern = getPatternByIdOrThrow(patternId);
        if(!pattern.getUserId().equals(userId)){
            throw new BusinessException("无权操作他人的纹样");
        }
        if("DELETED".equals(pattern.getStatus())) {
            throw new BusinessException("纹样不存在或者被删除");
        }
        if(Integer.valueOf(0).equals(pattern.getIsFavorite())){
            log.info("纹样[{}]没有被收藏",patternId);
            return;
        }
        patternMapper.update(null,new LambdaUpdateWrapper<Pattern>()
                .eq(Pattern::getId,patternId)
                .eq(Pattern::getUserId,userId)
                .set(Pattern::getIsFavorite,0)
                .setSql("like_count = GREATEST(like_count - 1,0)")
                .set(Pattern::getUpdatedAt,LocalDateTime.now())
        );

        log.info("用户[{}]取消收藏纹样[{}]",userId,patternId);
    }
//删除纹样
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void patternDeleted(Long patternId, Long userId) {
        Pattern pattern = getPatternByIdOrThrow(patternId);
        // 权限校验：只能删除自己的纹样
        if (!pattern.getUserId().equals(userId)) {
            throw new BusinessException("无权删除他人的纹样");
        }

        // 软删除：状态改为 DELETED
        patternMapper.update(null,
                new LambdaUpdateWrapper<Pattern>()
                        .eq(Pattern::getId, patternId)
                        .eq(Pattern::getUserId, userId)
                        .set(Pattern::getStatus, "DELETED")
                        .set(Pattern::getUpdatedAt, LocalDateTime.now())
        );
        log.info("用户[{}]纹样[{}]已被删除",userId,patternId);
    }

    @Override
    public Map<String, Object> getPatternDownloadUrl(Long patternId, Long userId) {
        Pattern pattern = getPatternByIdOrThrow(patternId);

        // 权限校验：只能下载自己的纹样（或已保存的）
        if (!pattern.getUserId().equals(userId) && !Integer.valueOf(1).equals(pattern.getIsSaved())) {
            throw new BusinessException("无权下载此纹样");
        }

        if (StrUtil.isBlank(pattern.getImageUrl())) {
            throw new BusinessException("纹样原图不存在，无法下载");
        }

        // 生成带签名的临时下载链接（实际项目中对接对象存储服务）
        // 这里以拼接方式示意，生产环境替换为 OSS/MinIO/COS 等 SDK 调用
        String downloadUrl = generateSignedDownloadUrl(pattern.getImageUrl());
        String fileName = (StrUtil.isNotBlank(pattern.getTitle()) ? pattern.getTitle() : "纹样_" + patternId) + ".png";

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("downloadUrl", downloadUrl);
        data.put("fileName", fileName);
        data.put("expiresIn", 600); // 有效期 600 秒

        return data;
    }


    //====================内部方法========================
    private Pattern getPatternByIdOrThrow(Long patternId) {
        Pattern pattern = patternMapper.selectById(patternId);
        if(pattern==null){
            throw new BusinessException("纹样不存在");
        }
        return pattern;
    }
    private  PatternMyVO convertToVO(Pattern pattern) {
        PatternMyVO vo = new PatternMyVO();
        vo.setId(pattern.getId());
        vo.setGenerationId(pattern.getGenerationId());
        vo.setUserId(pattern.getUserId());
        vo.setTitle(pattern.getTitle());
        vo.setImageUrl(pattern.getImageUrl());
        vo.setThumbnailUrl(pattern.getThumbnailUrl());
        vo.setKeyword(pattern.getKeyword());
        vo.setStyle(pattern.getStyle());
        if (StringUtils.isNotBlank(pattern.getElements())) {
            vo.setElements(parseElements(pattern.getElements()));
        } else {
            vo.setElements(Collections.emptyList());
        }

        vo.setColorTheme(pattern.getColorTheme());
        vo.setUsageScene(pattern.getUsageScene());
        vo.setDescription(pattern.getDescription());
        vo.setIsSaved(pattern.getIsSaved());
        vo.setIsFavorite(pattern.getIsFavorite());
        vo.setIsRecommend(pattern.getIsRecommend());
        vo.setViewCount(pattern.getViewCount() != null ? pattern.getViewCount() : 0);
        vo.setLikeCount(pattern.getLikeCount() != null ? pattern.getLikeCount() : 0);
        vo.setUseCount(pattern.getUseCount() != null ? pattern.getUseCount() : 0);
        vo.setStatus(pattern.getStatus());
        vo.setCreatedAt(pattern.getCreatedAt() != null ? pattern.getCreatedAt().format(FMT) : null);
        vo.setUpdatedAt(pattern.getUpdatedAt() != null ? pattern.getUpdatedAt().format(FMT) : null);

        return vo;

    }
    private List<PatternMyVO> convertToVOList(List<Pattern> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return Collections.emptyList();
        }
        return patterns.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 解析 elements JSON 字符串为 List
     * 数据库存储格式：["牡丹","凤凰","祥云"]
     */
    private List<String> parseElements(String elementsJson) {
        try {
            // 使用 Hutool 或 Jackson 解析 JSON 数组
            // 此处用简单方式处理，实际项目建议注入 ObjectMapper
            return cn.hutool.json.JSONUtil.toList(elementsJson, String.class);
        } catch (Exception e) {
            log.warn("解析纹样元素 JSON 失败: {}", elementsJson, e);
            return Collections.emptyList();
        }
    }

    /**
     * 生成带签名的临时下载 URL
     * 实际项目中对接 OSS SDK（阿里云 OSS / MinIO / 腾讯 COS 等）
     * 这里仅作示意，需替换为真实实现
     */
    private String generateSignedDownloadUrl(String imageUrl) {
        // TODO: 替换为实际的 OSS 签名 URL 生成逻辑
        // 示例：return ossClient.generatePresignedUrl(bucketName, objectKey, expiration);
        return imageUrl + (imageUrl.contains("?") ? "&" : "?") + "download=true&expires=" + (System.currentTimeMillis() + 600_000);
    }


}

