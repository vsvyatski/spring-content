package org.springframework.content.commons.utils;

import java.lang.reflect.Field;

public interface Condition {
    boolean matches(Field field);
}
