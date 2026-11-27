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

/**
 * Reflection helpers for Spring Content domain objects whose content-related state is marked with annotations such as
 * {@link org.springframework.content.commons.annotations.ContentId} and
 * {@link org.springframework.content.commons.annotations.ContentLength}.
 * <p>
 * Naming distinguishes <strong>metadata</strong> from <strong>values</strong>:
 * <ul>
 * <li>{@code find*} and {@code has*} inspect the type for an annotated field (inheritance and JavaBeans property paths
 * are considered). They do not read the current property value.</li>
 * <li>{@link #getAnnotatedFieldType(Object, Class) getAnnotatedFieldType} returns declared field type metadata only;
 * it does not read an instance value.</li>
 * <li>{@code set*} and {@link #getAnnotatedValue(Object, Class) getAnnotatedValue} read or write the live value on an
 * instance, preferring the JavaBeans accessor when one exists.</li>
 * </ul>
 * A non-null {@linkplain #getAnnotatedValue(Object, Class) annotated value} therefore implies an annotated field
 * exists, but {@linkplain #hasAnnotatedField(Object, Class) hasAnnotatedField} may be {@code true} while the value is
 * still {@code null}.
 *
 * @author Vladimir Svyatski
 */
public final class BeanUtils {

    private static final Condition MATCHING_CONDITION = field -> true;

    private BeanUtils() {
    }

    /**
     * Returns whether {@code domainObj}'s class declares a field (or property) annotated with
     * {@code annotationClass}, including supertypes.
     *
     * @param domainObj       the instance whose type is inspected
     * @param annotationClass the annotation to match
     * @return {@code true} when an annotated member exists (the value may still be {@code null})
     */
    public static boolean hasAnnotatedField(Object domainObj, Class<? extends Annotation> annotationClass)
        throws SecurityException, BeansException {
        return findAnnotatedField(domainObj, annotationClass) != null;
    }

    /**
     * Locates the first field on {@code domainObj}'s class hierarchy annotated with {@code annotationClass}.
     *
     * @param domainObj       the instance whose type is searched
     * @param annotationClass the annotation to match
     * @return the field, or {@code null} when none is found
     */
    public static Field findAnnotatedField(Object domainObj, Class<? extends Annotation> annotationClass)
        throws SecurityException, BeansException {
        BeanWrapper wrapper = new BeanWrapperImpl(domainObj);
        return findAnnotatedField(domainObj.getClass(), annotationClass, wrapper);
    }

    /**
     * Locates the first field on {@code domainObjClass} and its supertypes annotated with {@code annotationClass}.
     *
     * @param domainObjClass  the type to search
     * @param annotationClass the annotation to match
     * @return the field, or {@code null} when none is found
     */
    public static Field findAnnotatedField(Class<?> domainObjClass, Class<? extends Annotation> annotationClass)
        throws SecurityException, BeansException {
        BeanWrapper wrapper = new BeanWrapperImpl(domainObjClass);
        return findAnnotatedField(domainObjClass, annotationClass, wrapper);
    }

    private static Field findAnnotatedField(Class<?> domainObjClass, Class<? extends Annotation> annotationClass,
                                            BeanWrapper wrapper) {
        PropertyDescriptor[] descriptors = wrapper.getPropertyDescriptors();
        for (PropertyDescriptor descriptor : descriptors) {
            Field candidate = getField(domainObjClass, descriptor.getName());
            if (candidate != null && candidate.getAnnotation(annotationClass) != null) {
                return candidate;
            }
        }

        for (Field field : getAllFields(domainObjClass)) {
            if (field.getAnnotation(annotationClass) != null) {
                return field;
            }
        }
        return null;
    }

    /**
     * Returns every field on {@code domainObjClass} and its supertypes annotated with {@code annotationClass}.
     *
     * @param domainObjClass  the type to search
     * @param annotationClass the annotation to match
     * @param wrapper         bean wrapper for the type (used for property descriptors)
     * @return matching fields, possibly empty
     */
    public static Field[] findAnnotatedFields(Class<?> domainObjClass, Class<? extends Annotation> annotationClass,
                                              BeanWrapper wrapper) {
        List<Field> fields = new ArrayList<>();

        PropertyDescriptor[] descriptors = wrapper.getPropertyDescriptors();
        for (PropertyDescriptor descriptor : descriptors) {
            Field candidate = getField(domainObjClass, descriptor.getName());
            if (candidate != null && candidate.getAnnotation(annotationClass) != null) {
                fields.add(candidate);
            }
        }

        for (Field field : getAllFields(domainObjClass)) {
            if (field.getAnnotation(annotationClass) != null && !fields.contains(field)) {
                fields.add(field);
            }
        }
        return fields.toArray(Field[]::new);
    }

    /**
     * Returns the declared type of the first field annotated with {@code annotationClass} on {@code domainObj}'s class
     * hierarchy, or {@code null} when no such field exists.
     *
     * @param domainObj       the instance whose type is inspected
     * @param annotationClass the annotation to match
     * @return the field type, or {@code null} when no annotated member exists
     */
    public static @Nullable Class<?> getAnnotatedFieldType(Object domainObj, Class<? extends Annotation> annotationClass)
        throws SecurityException, BeansException {
        Field field = findAnnotatedField(domainObj, annotationClass);
        if (field != null) {
            return field.getType();
        }
        return null;
    }

    /**
     * Reads the current value of the first field (or property) annotated with {@code annotationClass} on
     * {@code domainObj}.
     *
     * @param domainObj       the instance to read from
     * @param annotationClass the annotation to match
     * @return the value, or {@code null} when no annotated member exists, the value is unset, or it cannot be read
     */
    public static @Nullable Object getAnnotatedValue(Object domainObj, Class<? extends Annotation> annotationClass)
        throws SecurityException, BeansException {
        Field field = findAnnotatedField(domainObj, annotationClass);
        if (field != null && field.getAnnotation(annotationClass) != null) {
            try {
                PropertyDescriptor descriptor = org.springframework.beans.BeanUtils
                    .getPropertyDescriptor(domainObj.getClass(), field.getName());
                if (descriptor != null) {
                    BeanWrapper wrapper = new BeanWrapperImpl(domainObj);
                    return wrapper.getPropertyValue(field.getName());
                }
                return ReflectionUtils.getField(field, domainObj);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    /**
     * Writes {@code value} to the first field annotated with {@code annotationClass} on {@code domainObj}'s class
     * hierarchy.
     *
     * @param domainObj       the instance to update
     * @param annotationClass the annotation to match
     * @param value           the value to assign (can be {@code null})
     */
    public static void setAnnotatedValue(Object domainObj, Class<? extends Annotation> annotationClass,
                                         @Nullable Object value) {
        setAnnotatedValueConditionally(domainObj, annotationClass, value, MATCHING_CONDITION);
    }

    /**
     * Writes {@code value} only when {@code condition} accepts the annotated field (or when {@code condition} is
     * {@code null}, in which case writing is unconditional).
     *
     * @param domainObj       the instance to update
     * @param annotationClass the annotation to match
     * @param value           the value to assign (can be {@code null})
     * @param condition       optional guard on the matched {@link Field}; {@code null} means always apply
     */
    public static void setAnnotatedValueConditionally(Object domainObj, Class<? extends Annotation> annotationClass,
                                                      @Nullable Object value, @Nullable Condition condition) {
        Field field = findAnnotatedField(domainObj, annotationClass);
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

    /**
     * Returns the default value for a primitive type, or {@code null} for reference types.
     */
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
        return null;
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
}
