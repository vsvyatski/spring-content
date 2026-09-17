package org.springframework.content.commons.repository;

@Deprecated
public record SetContentParams(
        long contentLength,
        boolean overwriteExistingContent,
        ContentDisposition disposition) {

    public enum ContentDisposition {
        Overwrite, CreateNew
    }
}
