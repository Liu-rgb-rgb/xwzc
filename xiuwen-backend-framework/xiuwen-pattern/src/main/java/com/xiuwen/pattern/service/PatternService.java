package com.xiuwen.pattern.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xiuwen.pattern.dto.PatternAdminQueryDTO;
import com.xiuwen.pattern.dto.PatternMyQueryDTO;
import com.xiuwen.pattern.entity.Pattern;
import com.xiuwen.pattern.entity.PatternAdminDetail;
import com.xiuwen.pattern.vo.PatternMyVO;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * pattern 表服务接口。
 */
@Service
public interface PatternService extends IService<Pattern> {


    Map<String, Object> getMyPatterns(PatternMyQueryDTO queryDTO);

    PatternMyVO getPatternDetail(Long patternId, Long userId);

    void savePattern(Long patternId, Long userId);

    void favoritePattern(Long patternId, Long userId);

    void unfavoritePattern(Long patternId, Long userId);

    void patternDeleted(Long patternId, Long userId);

    Map<String, Object> getPatternDownloadUrl(Long patternId, Long userId);

    // ==================== 商家端纹样管理 ====================

    /**
     * 商家端纹样分页列表
     */
    IPage<PatternAdminDetail> adminPatternList(PatternAdminQueryDTO query);

    /**
     * 设置或取消纹样推荐
     */
    void setRecommend(Long patternId, Integer isRecommend);

    /**
     * 隐藏或恢复纹样
     */
    void updatePatternStatus(Long patternId, String status);
}
