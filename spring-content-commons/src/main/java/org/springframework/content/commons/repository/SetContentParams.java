package org.springframework.content.commons.repository;

import lombok.Builder;

@Deprecated
@Builder
public record SetContentParams(
        long contentLength,
        boolean overwriteExistingContent,
        ContentDisposition disposition) {

    public static class SetContentParamsBuilder {
        private long contentLength = -1;
        private boolean overwriteExistingContent = true;
        private ContentDisposition disposition = ContentDisposition.Overwrite;
    }

    public enum ContentDisposition {
        Overwrite, CreateNew
    }
}
