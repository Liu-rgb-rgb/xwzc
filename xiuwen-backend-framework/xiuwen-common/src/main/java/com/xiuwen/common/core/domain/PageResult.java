package com.xiuwen.common.core.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 通用分页响应对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 总条数 */
    private Long total;

    /** 当前页 */
    private Long page;

    /** 每页条数 */
    private Long pageSize;

    /** 当前页数据 */
    private List<T> list;

    public static <T> PageResult<T> empty() {
        return new PageResult<>(0L, 1L, 10L, Collections.emptyList());
    }

    public static <T> PageResult<T> of(Long total, Long page, Long pageSize, List<T> list) {
        return new PageResult<>(total == null ? 0L : total,
                page == null ? 1L : page,
                pageSize == null ? 10L : pageSize,
                list == null ? Collections.emptyList() : list);
    }
}
