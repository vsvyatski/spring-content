package org.springframework.content.rest.config;

public record SetContentParams(ContentDisposition disposition) {

    private enum ContentDisposition {
        Overwrite, CreateNew
    }
}
