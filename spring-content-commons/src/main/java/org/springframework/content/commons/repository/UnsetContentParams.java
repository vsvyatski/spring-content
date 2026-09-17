package org.springframework.content.commons.repository;

@Deprecated
public record UnsetContentParams(Disposition disposition) {

    public enum Disposition {
        Keep, Remove
    }
}
