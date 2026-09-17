package org.springframework.content.rest.config;

import lombok.Builder;

@Builder
public record SetContentParams(ContentDisposition disposition) {

    public static class SetContentParamsBuilder {
        private ContentDisposition disposition = ContentDisposition.Overwrite;
    }

    private enum ContentDisposition {
        Overwrite, CreateNew
    }
}
