package org.springframework.content.commons.property;

public record PropertyPath(String name) {

    public static PropertyPath from(String name) {
        return new PropertyPath(name);
    }
}
