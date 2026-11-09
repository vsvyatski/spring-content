package org.springframework.content.rest;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface FulltextEntityLookupQuery {
}
