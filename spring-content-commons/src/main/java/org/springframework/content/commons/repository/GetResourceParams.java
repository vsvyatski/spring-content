package org.springframework.content.commons.repository;

import lombok.Builder;

/**
 * @deprecated This class is deprecated. Use {@link org.springframework.content.commons.store.GetResourceParams}
 * instead.
 */
@Deprecated
@Builder
public record GetResourceParams(String range) {
}
