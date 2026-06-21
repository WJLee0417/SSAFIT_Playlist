package com.ssafy.ssafit.common.response;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalCount,
        int totalPages) {

    public PageResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static <T> PageResponse<T> of(
            List<T> items,
            int page,
            int size,
            long totalCount) {
        int totalPages = totalCount == 0
                ? 0
                : (int) Math.ceil((double) totalCount / size);
        return new PageResponse<>(items, page, size, totalCount, totalPages);
    }
}
