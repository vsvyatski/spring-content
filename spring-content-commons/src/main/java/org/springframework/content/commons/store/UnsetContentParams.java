package org.springframework.content.commons.store;

public record UnsetContentParams(Disposition disposition) {

    public enum Disposition {
        Keep, Remove
    }
}
