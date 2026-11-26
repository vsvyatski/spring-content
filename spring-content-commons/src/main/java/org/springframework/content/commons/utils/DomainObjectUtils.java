package org.springframework.content.commons.utils;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.annotation.Id;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public final class DomainObjectUtils {

    /**
     * Fully qualified name of {@code jakarta.persistence.Id}, the JPA entity primary-key marker (Jakarta successor to
     * {@code javax.persistence.Id}).
     */
    public static final String JAKARTA_PERSISTENCE_ID_TYPE = "jakarta.persistence.Id";

    /**
     * Fully qualified name of {@code org.springframework.data.annotation.Id}, Spring Data's portable identifier
     * marker for mapped domain types (JPA, MongoDB, and other stores). It is not superseded by Jakarta Persistence
     * {@code @Id}; both annotations often appear together when the content ID field is also the entity ID.
     */
    public static final String SPRING_DATA_ID_TYPE = "org.springframework.data.annotation.Id";

    private static boolean JAKARTA_PERSISTENCE_ID_CLASS_PRESENT = false;

    static {
        try {
            JAKARTA_PERSISTENCE_ID_CLASS_PRESENT = DomainObjectUtils.class.getClassLoader()
                .loadClass("jakarta.persistence.Id") != null;
        } catch (ClassNotFoundException ignored) {
        }
    }

    private DomainObjectUtils() {
    }

    public static Object getId(Object entity) {

        Object id = null;

        if (JAKARTA_PERSISTENCE_ID_CLASS_PRESENT && BeanUtils.hasFieldWithAnnotation(entity, jakarta.persistence.Id.class)) {

            id = BeanUtils.getFieldWithAnnotation(entity, jakarta.persistence.Id.class);
            if (id == null) {
                PropertyDescriptor[] propertyDescriptors = new BeanWrapperImpl(entity).getPropertyDescriptors();
                for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                    if (AnnotationUtils.findAnnotation(propertyDescriptor.getReadMethod(),
                        jakarta.persistence.Id.class) != null) {
                        return new BeanWrapperImpl(entity).getPropertyValue(propertyDescriptor.getName());
                    }
                }
            }
        } else if (BeanUtils.hasFieldWithAnnotation(entity, Id.class)) {
            id = BeanUtils.getFieldWithAnnotation(entity, Id.class);
            if (id == null) {
                PropertyDescriptor[] propertyDescriptors = new BeanWrapperImpl(entity).getPropertyDescriptors();
                for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                    if (AnnotationUtils.findAnnotation(propertyDescriptor.getReadMethod(),
                        Id.class) != null) {
                        return new BeanWrapperImpl(entity).getPropertyValue(propertyDescriptor.getName());
                    }
                }
            }
        }

        return id;
    }

    public static Field getIdField(Class<?> domainClass) {

        if (JAKARTA_PERSISTENCE_ID_CLASS_PRESENT &&
            BeanUtils.findFieldWithAnnotation(domainClass, jakarta.persistence.Id.class) != null) {
            return BeanUtils.findFieldWithAnnotation(domainClass, jakarta.persistence.Id.class);
        } else if (BeanUtils.findFieldWithAnnotation(domainClass, Id.class) != null) {
            return BeanUtils.findFieldWithAnnotation(domainClass, Id.class);
        }

        return null;
    }

    /**
     * Whether {@code annotation} is {@code jakarta.persistence.Id} or Spring Data {@code @Id}.
     */
    public static boolean isEntityIdAnnotation(Annotation annotation) {
        return isEntityIdAnnotationTypeName(annotation.annotationType().getName());
    }

    public static boolean isEntityIdAnnotationTypeName(String typeName) {
        return JAKARTA_PERSISTENCE_ID_TYPE.equals(typeName) || SPRING_DATA_ID_TYPE.equals(typeName);
    }

    /**
     * Whether a {@code @ContentId} field may be cleared without touching a co-located entity ID.
     */
    public static boolean contentIdFieldMayBeCleared(Field field) {
        for (Annotation annotation : field.getAnnotations()) {
            if (isEntityIdAnnotation(annotation)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Same as {@link #contentIdFieldMayBeCleared(Field)} for a bean property type descriptor.
     */
    public static boolean contentIdPropertyMayBeCleared(TypeDescriptor descriptor) {
        for (Annotation annotation : descriptor.getAnnotations()) {
            if (isEntityIdAnnotation(annotation)) {
                return false;
            }
        }
        return true;
    }
}
