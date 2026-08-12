package com.sanjay.aisecurity.common;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Paginated API Response Wrapper.
 *
 * <p>Encapsulates paginated query results with full pagination metadata.
 * Used by endpoints returning collections with pagination, sorting,
 * and search support.</p>
 *
 * <p>Example JSON output:</p>
 * <pre>{@code
 * {
 *   "content": [...],
 *   "pageNumber": 0,
 *   "pageSize": 10,
 *   "totalElements": 47,
 *   "totalPages": 5,
 *   "first": true,
 *   "last": false
 * }
 * }</pre>
 *
 * @param <T> the type of each element in the content list
 * @author Sanjay
 * @version 1.0.0
 */
@Getter
@Builder
public class PaginationResponse<T> {

    /** The list of items for the current page. */
    private List<T> content;

    /** Zero-based current page number. */
    private int pageNumber;

    /** Number of elements requested per page. */
    private int pageSize;

    /** Total number of matching elements across all pages. */
    private long totalElements;

    /** Total number of pages available. */
    private int totalPages;

    /** Whether this is the first page. */
    private boolean first;

    /** Whether this is the last page. */
    private boolean last;

    /**
     * Factory method to build a {@code PaginationResponse} from a
     * Spring Data {@link org.springframework.data.domain.Page} result.
     *
     * @param page    the Spring Data Page result
     * @param content the mapped content list (DTO-converted from entities)
     * @param <T>     content element type
     * @return populated {@code PaginationResponse}
     */
    public static <T> PaginationResponse<T> from(
            org.springframework.data.domain.Page<?> page,
            List<T> content) {

        return PaginationResponse.<T>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
