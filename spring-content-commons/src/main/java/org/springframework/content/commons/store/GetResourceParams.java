package org.springframework.content.commons.store;

import lombok.Builder;

@Builder
public record GetResourceParams(String range) {
}
