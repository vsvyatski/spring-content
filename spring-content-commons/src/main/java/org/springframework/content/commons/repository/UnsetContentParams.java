package org.springframework.content.commons.repository;

import lombok.Builder;

@Deprecated
@Builder
public record UnsetContentParams(Disposition disposition) {

    public static class UnsetContentParamsBuilder {
        private Disposition disposition = Disposition.Remove;
    }

    public enum Disposition {
        Keep, Remove
    }
}
