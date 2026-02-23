package com.cobasesys.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private long total;
    private int page;
    private int pageSize;
    private List<T> items;

    public static <T> PageResult<T> from(Page<T> page) {
        return new PageResult<>(
                page.getTotalElements(),
                page.getNumber() + 1,
                page.getSize(),
                page.getContent()
        );
    }
}
