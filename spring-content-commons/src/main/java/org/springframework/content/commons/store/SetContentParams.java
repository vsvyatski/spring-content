package org.springframework.content.commons.store;

public record SetContentParams(
        long contentLength,
        boolean overwriteExistingContent,
        ContentDisposition disposition) {

    public enum ContentDisposition {
        Overwrite, CreateNew
    }
}
