package com.xiuwen.common.core.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用分页请求参数。
 */
@Data
public class PageQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 当前页，从 1 开始 */
    private Long page = 1L;

    /** 每页条数 */
    private Long pageSize = 10L;

    public Long getSafePage() {
        return page == null || page < 1 ? 1L : page;
    }

    public Long getSafePageSize() {
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }
        return Math.min(pageSize, 100L);
    }

    public Long offset() {
        return (getSafePage() - 1L) * getSafePageSize();
    }
}
