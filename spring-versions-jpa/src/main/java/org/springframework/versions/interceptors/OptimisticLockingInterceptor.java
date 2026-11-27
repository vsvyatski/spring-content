package org.springframework.versions.interceptors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Version;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

public class OptimisticLockingInterceptor implements MethodInterceptor {

    private static final Method getContentMethod;
    private static final Method getContentPropertyPathMethod;
    private static final Method setContentMethod;
    private static final Method setContentPropertyPathMethod;
    private static final Method setContentMethodWithResource;
    private static final Method setContentMethodWithPropertyPathAndResource;
    private static final Method unsetContentMethod;
    private static final Method unsetContentPropertyPathMethod;

    static {
        getContentMethod = ReflectionUtils.findMethod(ContentStore.class, "getContent", Object.class);
        Assert.notNull(getContentMethod, "Unable to find getContent method");
        getContentPropertyPathMethod = ReflectionUtils.findMethod(ContentStore.class, "getContent", Object.class, PropertyPath.class);
        Assert.notNull(getContentPropertyPathMethod, "Unable to find getContent method");
        setContentMethod = ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, InputStream.class);
        Assert.notNull(setContentMethod, "Unable to find setContent method");
        setContentPropertyPathMethod = ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, PropertyPath.class, InputStream.class);
        Assert.notNull(setContentPropertyPathMethod, "Unable to find setContent method");
        setContentMethodWithResource = ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, Resource.class);
        Assert.notNull(setContentMethodWithResource, "Unable to find setContent method");
        setContentMethodWithPropertyPathAndResource = ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, PropertyPath.class, Resource.class);
        Assert.notNull(setContentMethodWithPropertyPathAndResource, "Unable to find setContent method");
        unsetContentMethod = ReflectionUtils.findMethod(ContentStore.class, "unsetContent", Object.class);
        Assert.notNull(unsetContentMethod, "Unable to find unsetContent method");
        unsetContentPropertyPathMethod = ReflectionUtils.findMethod(ContentStore.class, "unsetContent", Object.class, PropertyPath.class);
        Assert.notNull(unsetContentPropertyPathMethod, "Unable to find unsetContent method");
    }

    private final EntityManager em;

    public OptimisticLockingInterceptor(EntityManager em) {
        this.em = em;
    }

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Object rc = null;

        Method invokedMethod = methodInvocation.getMethod();
        if (Objects.equals(invokedMethod, getContentMethod)
            || Objects.equals(invokedMethod, getContentPropertyPathMethod)) {
            if (methodInvocation.getArguments().length > 0) {
                Object[] args = methodInvocation.getArguments();
                args[0] = lock(args[0]);
                ((ProxyMethodInvocation) methodInvocation).setArguments(args);
                rc = methodInvocation.proceed();
            }
        } else if (Objects.equals(invokedMethod, setContentMethod)
            || Objects.equals(invokedMethod, setContentPropertyPathMethod)
            || Objects.equals(invokedMethod, setContentMethodWithResource)
            || Objects.equals(invokedMethod, setContentMethodWithPropertyPathAndResource)
            || Objects.equals(invokedMethod, unsetContentMethod)
            || Objects.equals(invokedMethod, unsetContentPropertyPathMethod)) {
            if (methodInvocation.getArguments().length > 0) {
                Object[] args = methodInvocation.getArguments();
                args[0] = lock(args[0]);
                ((ProxyMethodInvocation) methodInvocation).setArguments(args);
                Object entity = methodInvocation.proceed();
                touch(entity);
                return entity;
            }
        } else {
            rc = methodInvocation.proceed();
        }
        return rc;
    }

    protected Object lock(Object entity) {
        if (em == null) {
            return entity;
        }

        if (!BeanUtils.hasAnnotatedField(entity, Version.class)) {
            return entity;
        }

        entity = em.merge(entity);
        em.lock(entity, LockModeType.OPTIMISTIC);
        return entity;
    }

    private void touch(Object domainObj) {
        Field versionField = BeanUtils.findAnnotatedField(domainObj, Version.class);
        if (versionField == null) {
            return;
        }
        Object version = BeanUtils.getAnnotatedValue(domainObj, Version.class);
        Object nextVersion;
        Class<?> fieldType = versionField.getType();
        if (fieldType == int.class || fieldType == Integer.class) {
            int current = version instanceof Number number ? number.intValue() : 0;
            nextVersion = Math.incrementExact(current);
        } else if (fieldType == long.class || fieldType == Long.class) {
            long current = version instanceof Number number ? number.longValue() : 0L;
            nextVersion = Math.incrementExact(current);
        } else {
            return;
        }
        BeanUtils.setAnnotatedValue(domainObj, Version.class, nextVersion);
    }
}
