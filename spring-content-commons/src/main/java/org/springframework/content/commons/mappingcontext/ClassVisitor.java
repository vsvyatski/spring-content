package org.springframework.content.commons.mappingcontext;

import java.lang.reflect.Field;

public interface ClassVisitor {

    boolean visitClass(String path, Class<?> clazz);

    default boolean visitFieldBefore(String path, Class<?> clazz, Field f) {
        return true;
    }

    boolean visitField(String path, Class<?> clazz, Field f);

    default boolean visitFieldAfter(String path, Class<?> clazz, Field f) {
        return true;
    }

    void visitClassEnd(String path, Class<?> clazz);
}
