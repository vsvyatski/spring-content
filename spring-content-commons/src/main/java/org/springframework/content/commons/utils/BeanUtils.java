package org.springframework.content.commons.utils;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class BeanUtils {

    private static final Condition MATCHING_CONDITION = field -> true;

    private BeanUtils() {
    }

    public static boolean hasFieldWithAnnotation(Object domainObj,
                                                 Class<? extends Annotation> annotationClass)
        throws SecurityException, BeansException {

        Field field = findFieldWithAnnotation(domainObj, annotationClass);
        return field != null && field.getAnnotation(annotationClass) != null;
    }

    public static Field findFieldWithAnnotation(Object domainObj,
                                                Class<? extends Annotation> annotationClass)
        throws SecurityException, BeansException {
        BeanWrapper wrapper = new BeanWrapperImpl(domainObj);
        return findFieldWithAnnotation(domainObj.getClass(), annotationClass, wrapper);
    }

    public static Field findFieldWithAnnotation(Class<?> domainObjClass,
                                                Class<? extends Annotation> annotationClass)
        throws SecurityException, BeansException {

        BeanWrapper wrapper = new BeanWrapperImpl(domainObjClass);
        return findFieldWithAnnotation(domainObjClass, annotationClass, wrapper);
    }

    private static Field findFieldWithAnnotation(Class<?> domainObjClass,
                                                 Class<? extends Annotation> annotationClass, BeanWrapper wrapper) {
        PropertyDescriptor[] descriptors = wrapper.getPropertyDescriptors();
        for (PropertyDescriptor descriptor : descriptors) {
            Field candidate = getField(domainObjClass, descriptor.getName());
            if (candidate != null) {
                if (candidate.getAnnotation(annotationClass) != null) {
                    return candidate;
                }
            }
        }

        for (Field field : getAllFields(domainObjClass)) {
            if (field.getAnnotation(annotationClass) != null) {
                return field;
            }
        }
        return null;
    }

    public static Field[] findFieldsWithAnnotation(Class<?> domainObjClass,
                                                   Class<? extends Annotation> annotationClass, BeanWrapper wrapper) {
        List<Field> fields = new ArrayList<>();

        PropertyDescriptor[] descriptors = wrapper.getPropertyDescriptors();
        for (PropertyDescriptor descriptor : descriptors) {
            Field candidate = getField(domainObjClass, descriptor.getName());
            if (candidate != null) {
                if (candidate.getAnnotation(annotationClass) != null) {
                    fields.add(candidate);
                }
            }
        }

        for (Field field : getAllFields(domainObjClass)) {
            if (field.getAnnotation(annotationClass) != null) {
                if (!fields.contains(field)) {
                    fields.add(field);
                }
            }
        }
        return fields.toArray(new Field[]{});
    }

    private static List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            getAllFields(fields, type.getSuperclass());
        }

        return fields;
    }

    private static void getAllFields(List<Field> fields, Class<?> type) {
        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            getAllFields(fields, type.getSuperclass());
        }
    }

    private static Field getField(Class<?> type, String fieldName) {
        for (Field field : getAllFields(type)) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    public static Class<?> getFieldWithAnnotationType(Object domainObj,
                                                      Class<? extends Annotation> annotationClass)
        throws SecurityException, BeansException {
        Class<?> type = null;

        Field field = findFieldWithAnnotation(domainObj, annotationClass);
        if (field != null && field.getAnnotation(annotationClass) != null) {
            type = field.getType();
        }

        return type;
    }

    public static Object getFieldWithAnnotation(Object domainObj, Class<? extends Annotation> annotationClass)
        throws SecurityException, BeansException {
        Object value;

        Field field = findFieldWithAnnotation(domainObj, annotationClass);
        if (field != null && field.getAnnotation(annotationClass) != null) {
            try {
                PropertyDescriptor descriptor = org.springframework.beans.BeanUtils
                    .getPropertyDescriptor(domainObj.getClass(), field.getName());
                if (descriptor != null) {
                    BeanWrapper wrapper = new BeanWrapperImpl(domainObj);
                    value = wrapper.getPropertyValue(field.getName());
                } else {
                    value = ReflectionUtils.getField(field, domainObj);
                }
                return value;
            } catch (IllegalArgumentException ignored) {
            }
        }

        return null;
    }

    /**
     * Sets object's field annotated with annotationClass to value.
     *
     * @param domainObj       the object containing the field
     * @param annotationClass the annotation to look for
     * @param value           the value to set
     */
    public static void setFieldWithAnnotation(Object domainObj,
                                              Class<? extends Annotation> annotationClass, @Nullable Object value) {
        setFieldWithAnnotationConditionally(domainObj, annotationClass, value,
            MATCHING_CONDITION);
    }

    /**
     * Sets object's field annotated with annotationClass to value only if the condition
     * matches.
     *
     * @param domainObj       the object containing the field
     * @param annotationClass the annotation to look for
     * @param value           the value to set
     * @param condition       the condition that must be satisfied to allow the match
     */
    public static void setFieldWithAnnotationConditionally(Object domainObj,
                                                           Class<? extends Annotation> annotationClass,
                                                           @Nullable Object value, @Nullable Condition condition) {

        Field field = findFieldWithAnnotation(domainObj, annotationClass);
        if (field != null && field.getAnnotation(annotationClass) != null
            && (condition == null || condition.matches(field))) {
            try {
                PropertyDescriptor descriptor = org.springframework.beans.BeanUtils
                    .getPropertyDescriptor(domainObj.getClass(), field.getName());
                if (descriptor != null) {
                    BeanWrapper wrapper = new BeanWrapperImpl(domainObj);
                    wrapper.setPropertyValue(field.getName(), value);
                } else {
                    ReflectionUtils.setField(field, domainObj, value);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public static Object getDefaultValueForType(Class<?> fieldType) {
        if (fieldType == int.class || fieldType == double.class) {
            return 0;
        } else if (fieldType == boolean.class) {
            return false;
        } else if (fieldType == char.class) {
            return '\u0000';
        } else if (fieldType == byte.class) {
            return (byte) 0;
        } else if (fieldType == short.class) {
            return (short) 0;
        } else if (fieldType == long.class) {
            return 0L;
        } else if (fieldType == float.class) {
            return 0.0f;
        }
        return null; // For non-primitive types
    }
}
