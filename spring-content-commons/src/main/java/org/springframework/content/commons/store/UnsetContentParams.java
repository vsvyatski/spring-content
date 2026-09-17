package org.springframework.content.commons.store;

import lombok.Builder;

@Builder
public record UnsetContentParams(Disposition disposition) {

    public static class UnsetContentParamsBuilder {
        private Disposition disposition = Disposition.Remove;
    }

    public enum Disposition {
        Keep, Remove
    }
}
