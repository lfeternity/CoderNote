package com.codernote.platform.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private Long total;
    private Long pageNo;
    private Long pageSize;
    private List<T> records;

    public static <T> PageResult<T> empty(long pageNo, long pageSize) {
        return new PageResult<>(0L, pageNo, pageSize, Collections.emptyList());
    }
}
