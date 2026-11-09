package org.springframework.content.commons.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RUNTIME)
public @interface HandleBeforeSetContent {
}
