package org.springframework.content.commons.utils;

import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.lang.reflect.Field;
import java.util.UUID;

public final class ContentPropertyUtils {

    private static boolean IS_JPA_PRESENT = false;
    private static boolean IS_MONGO_PRESENT = false;

    static {
        try {
            Class.forName("jakarta.persistence.OneToOne");
            IS_JPA_PRESENT = true;
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class.forName("org.springframework.data.mongodb.core.mapping.DBRef");
            IS_MONGO_PRESENT = true;
        } catch (ClassNotFoundException ignored) {
        }
    }


    private ContentPropertyUtils() {
    }

    public static boolean isPrimitiveContentPropertyClass(Class<?> clazz) {
        return clazz.isPrimitive() || clazz.equals(UUID.class) || clazz.equals(String.class);
    }

    /**
     * Counts {@link ContentId} fields on {@code type} and its superclasses (declared fields per level).
     */
    public static int contentIdFieldCount(Class<?> type) {
        int count = 0;
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(ContentId.class)) {
                    count++;
                }
            }
        }
        return count;
    }

    public static boolean isWrapperType(Class<?> type) {

        return Boolean.class.equals(type) ||
                Byte.class.equals(type) ||
                Character.class.equals(type) ||
                Double.class.equals(type) ||
                Float.class.equals(type) ||
                Integer.class.equals(type) ||
                Long.class.equals(type) ||
                Short.class.equals(type);
    }


    public static boolean isRelationshipField(Field f) {

        if (IS_JPA_PRESENT) {
            if (f.getAnnotation(OneToOne.class) != null ||
                    f.getAnnotation(OneToMany.class) != null ||
                    f.getAnnotation(ManyToOne.class) != null ||
                    f.getAnnotation(ManyToMany.class) != null) {
                return true;
            }
        }
        if (IS_MONGO_PRESENT) {
            return f.getAnnotation(DBRef.class) != null;
        }
        return false;
    }
}
