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
    private Integer page = 1;

    /** 每页条数 */
    private Integer pageSize = 10;

    public Integer getSafePage() {
        return page == null || page < 1 ? 1 : page;
    }

    public Integer getSafePageSize() {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    public Integer offset() {
        return (getSafePage() - 1) * getSafePageSize();
    }
}
