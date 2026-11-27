package org.springframework.versions.impl;

import internal.org.springframework.versions.AuthenticationFacade;
import internal.org.springframework.versions.LockingService;
import internal.org.springframework.versions.jpa.CloningService;
import internal.org.springframework.versions.jpa.EntityInformationFacade;
import internal.org.springframework.versions.jpa.VersioningService;
import jakarta.persistence.*;
import org.apache.commons.text.StringSubstitutor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.versions.*;

import java.lang.reflect.Field;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

@Repository
public class LockingAndVersioningRepositoryImpl<T> implements LockingAndVersioningRepository<T> {

    private EntityManager em;
    private EntityInformationFacade entityInfo;
    private final Map<Class<?>, EntityInformation<T, ?>> entityInformationMap = new ConcurrentHashMap<>();
    private AuthenticationFacade auth;
    private LockingService lockingService;
    private VersioningService versioner;
    private CloningService cloner;

    @Autowired(required = false)
    public LockingAndVersioningRepositoryImpl() {
    }

    @Autowired(required = false)
    public LockingAndVersioningRepositoryImpl(EntityManager em, EntityInformationFacade entityInfo, AuthenticationFacade auth, LockingService locker, VersioningService versioner, CloningService cloner) {
        this.em = em;
        this.entityInfo = entityInfo;
        this.auth = auth;
        this.lockingService = locker;
        this.versioner = versioner;
        this.cloner = cloner;
    }

    @Override
    @Transactional
    public <S extends T> S lock(S entity) {
        Authentication authentication = auth.getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("no principal");
        }
        Object id = BeanUtils.getAnnotatedValue(entity, Id.class);
        if (id == null) {
            id = BeanUtils.getAnnotatedValue(entity, org.springframework.data.annotation.Id.class);
        }
        if (id == null) {
            throw new IllegalStateException("@Id missing");
        }

        String principal = authentication.getName();
        Principal lockOwner = lockingService.lockOwner(id);
        if (lockOwner != null && !principal.equals(lockOwner.getName())) {
            throw new LockOwnerException(format("not lock owner: %s has lock owner '%s'", id, lockOwner.getName()));
        }

        if (lockingService.lock(id, authentication)) {
            BeanUtils.setAnnotatedValue(entity, LockOwner.class, authentication.getName());
            return entity;
        }

        throw new LockingAndVersioningException(format("failed to lock %s", id));
    }

    @Override
    @Transactional
    public <S extends T> S unlock(S entity) {
        Authentication authentication = auth.getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("no principal");
        }
        Object id = BeanUtils.getAnnotatedValue(entity, Id.class);
        if (id == null) {
            id = BeanUtils.getAnnotatedValue(entity, org.springframework.data.annotation.Id.class);
        }
        if (id == null) {
            throw new IllegalStateException("@Id missing");
        }
        String principal = authentication.getName();
        Principal lockOwner = lockingService.lockOwner(id);
        if (lockOwner == null || !principal.equals(lockOwner.getName())) {
            throw new LockOwnerException(
                format("not lock owner: %s has lock owner '%s'", id, lockOwner != null ? lockOwner.getName() : ""));
        }

        BeanUtils.setAnnotatedValue(entity, LockOwner.class, null);

        if (lockingService.unlock(id, authentication)) {
            return entity;
        }
        throw new LockingAndVersioningException(format("failed to unlock %s", id));
    }

    @Override
    @Transactional
    public <S extends T> S save(S entity) {
        EntityInformation<T, ?> entityInformation = this.getEntityInformation(entity.getClass());
        Assert.notNull(entityInformation, String.format("Unable to get entity information for entity class '%s'", entity.getClass()));

        if (entityInformation.isNew(entity)) {
            BeanUtils.setAnnotatedValue(entity, VersionNumber.class, "1.0");

            em.persist(entity);
            return entity;
        }

        Object id = getId(entity);
        if (id == null) return null;

        Authentication authentication = auth.getAuthentication();
        Principal lockOwner = lockingService.lockOwner(id);
        if (mayMergeOnSave(authentication, lockOwner)) {
            return em.merge(entity);
        }
        throw new LockOwnerException(format("entity not locked by you. lock owner: %s", lockOwner.getName()));
    }

    @Override
    @Transactional
    public <S extends T> S workingCopy(S currentVersion) {

        Authentication authentication = auth.getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("no principal");
        }

        Object id = getId(currentVersion);
        if (id == null) {
            throw new IllegalStateException("not saved");
        }

        if (isNotHead(currentVersion)) {
            throw new LockingAndVersioningException("not head");
        }

        Principal lockOwner = lockingService.lockOwner(id);
        if (lockOwner == null || !authentication.getName().equals(lockOwner.getName())) {
            throw new LockOwnerException("not lock owner");
        }

        S ancestorRoot;
        if (isAncestralRoot(currentVersion)) {
            currentVersion = establishAncestralRootAndMerge(currentVersion);
            ancestorRoot = currentVersion;
        } else {
            Object ancestorRootId = getAncestralRootId(currentVersion);
            ancestorRoot = findEntity(currentVersion.getClass(), ancestorRootId);
            if (ancestorRoot == null) {
                throw new LockingAndVersioningException(format("ancestor root not found: %s", ancestorRootId));
            }
        }

        S newVersion = cloneVersion(currentVersion);

        String versionNumber = Objects.toString(BeanUtils.getAnnotatedValue(currentVersion, VersionNumber.class), "");
        newVersion = establishSuccessor(newVersion, versionNumber, "~~PWC~~", ancestorRoot, currentVersion);

        em.persist(newVersion);

        newVersion = this.lock(newVersion);
        newVersion = em.merge(newVersion);

        return newVersion;
    }

    @Override
    @Transactional
    public <S extends T> S version(S currentVersion, VersionInfo info) {

        Authentication authentication = auth.getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("no principal");
        }

        Object id = getId(currentVersion);
        if (id == null) return null;

        if (isNotHead(currentVersion)) {
            throw new LockingAndVersioningException("not head");
        }

        Principal lockOwner = lockingService.lockOwner(id);
        if (lockOwner == null || !authentication.getName().equals(lockOwner.getName())) {
            throw new LockOwnerException("not lock owner");
        }

        S newVersion;
        if (!isPrivateWorkingCopy(currentVersion)) {

            S ancestorRoot;
            if (isAncestralRoot(currentVersion)) {
                currentVersion = establishAncestralRoot(currentVersion);
                ancestorRoot = currentVersion;
            } else {
                Object ancestorRootId = getAncestralRootId(currentVersion);
                ancestorRoot = findEntity(currentVersion.getClass(), ancestorRootId);
                if (ancestorRoot == null) {
                    throw new LockingAndVersioningException(format("ancestor root not found: %s", ancestorRootId));
                }
            }

            newVersion = cloneVersion(currentVersion);

            this.unlock(currentVersion);

            newVersion = establishSuccessor(newVersion, info.getNumber(), info.getLabel(), ancestorRoot, currentVersion);
            em.persist(newVersion);

            newVersion = this.lock(newVersion);
            newVersion = em.merge(newVersion);
        } else {

            newVersion = currentVersion;
            BeanUtils.setAnnotatedValue(newVersion, VersionNumber.class, info.getNumber());
            BeanUtils.setAnnotatedValue(newVersion, VersionLabel.class, info.getLabel());
            newVersion = em.merge(newVersion);

            currentVersion = findEntity(newVersion.getClass(), BeanUtils.getAnnotatedValue(newVersion, AncestorId.class));
            this.unlock(currentVersion);
        }

        currentVersion = establishAncestor(currentVersion, newVersion);
        em.merge(currentVersion);

        return newVersion;
    }

    @Override
    public <S extends T> List<S> findAllVersionsLatest(Class<S> entityClass) {

        String sql = "select t from ${entityClass} t where t.${successorId} IS NULL and t.${id} NOT IN (select f1.${id} FROM ${entityClass} f1 inner join ${entityClass} f2 on f1.${ancestorId} = f2.${id} and f2.${successorId} IS NULL)";

        StringSubstitutor sub = new StringSubstitutor(getAttributeMap(entityClass));
        sql = sub.replace(sql);

        TypedQuery<S> q = em.createQuery(sql, entityClass);

        try {
            return q.getResultList();
        } catch (NoResultException nre) {
            return new ArrayList<>();
        }
    }

    @Override
    public <S extends T> List<S> findAllVersions(S entity) {

        return this.findAllVersions(entity, Sort.unsorted());
    }

    @Override
    public <S extends T> List<S> findAllVersions(S entity, Sort sort) {

        if (isAncestralRoot(entity)) {
            return Collections.singletonList(entity);
        }

        StringBuilder builder = new StringBuilder();
        if (sort.isSorted()) {
            builder.append("order by ");

            final AtomicInteger sortIndex = new AtomicInteger();
            sort.forEach((property) -> {
                if (sortIndex.getAndIncrement() > 0) {
                    builder.append(",");
                }
                builder.append("t.");
                builder.append(property.getProperty());
                builder.append(" ");
                builder.append(property.getDirection());
            });
        }

        String sql = "select t from ${entityClass} t where t.${ancestorRootId} = " + getAncestralRootId(entity);

        if (!builder.isEmpty()) {
            sql = sql + " " + builder;
        }

        StringSubstitutor sub = new StringSubstitutor(getAttributeMap(entity.getClass()));
        sql = sub.replace(sql);

        @SuppressWarnings("unchecked")
        Class<S> entityType = (Class<S>) entity.getClass();
        TypedQuery<S> q = em.createQuery(sql, entityType);

        try {
            return q.getResultList();
        } catch (NoResultException nre) {
            return new ArrayList<>();
        }
    }

    @Override
    public void delete(T entity) {

        Authentication authentication = auth.getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("no principal");
        }

        Object id = this.getId(entity);
        if (id == null) return;

        if (isNotHead(entity)) {
            throw new LockingAndVersioningException("not head");
        }

        boolean relock = false;
        if (lockingService.lockOwner(id) != null && !lockingService.isLockOwner(id, authentication)) {
            throw new LockOwnerException("not lock owner");
        } else if (lockingService.lockOwner(id) != null && lockingService.isLockOwner(id, authentication)) {
            relock = true;
        }

        Object ancestorRootId = getAncestralRootId(entity);
        Object ancestorId = getAncestorId(entity);

        if (ancestorId == null) {
            ancestorId = ancestorRootId;
        }

        if (ancestorId != null) {
            T ancestor = findEntity(entity.getClass(), ancestorId);

            Principal lockOwner = lockingService.lockOwner(ancestorId);
            if (lockOwner != null && !Objects.equals(authentication.getName(), lockOwner.getName())) {
                throw new LockOwnerException(format("Not lock owner %s", ancestorId));
            } else if (lockOwner == null && relock) {
                lockingService.lock(ancestorId, authentication);
            }

            BeanUtils.setAnnotatedValue(ancestor, SuccessorId.class, null);

            if (ancestorRootId.equals(ancestorId)) {
                BeanUtils.setAnnotatedValue(ancestor, AncestorRootId.class, null);
            }
        }

        lockingService.unlock(id, authentication);

        em.remove(em.contains(entity) ? entity : em.merge(entity));
    }

    @Override
    @Transactional
    public void deleteAllVersions(T entity) {

        Authentication authentication = auth.getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("no principal");
        }

        Object id = this.getId(entity);
        if (id == null) return;

        if (isNotHead(entity)) {
            throw new LockingAndVersioningException("not head");
        }

        if (lockingService.lockOwner(id) != null && !lockingService.isLockOwner(id, authentication)) {
            throw new LockOwnerException("not lock owner");
        }

        // issue #2039: if no version series has been established yet then delete the entity
        if (isAncestralRoot(entity)) {
            this.delete(entity);
            return;
        }

        String sql = "delete from ${entityClass} t where t.${ancestorRootId} = " + getAncestralRootId(entity);

        StringSubstitutor sub = new StringSubstitutor(getAttributeMap(entity.getClass()));
        sql = sub.replace(sql);

        Query q = em.createQuery(sql);
        q.executeUpdate();
    }

    protected <E> boolean isNotHead(E entity) {
        if (!BeanUtils.hasAnnotatedField(entity, SuccessorId.class)) {
            return true;
        }
        return BeanUtils.getAnnotatedValue(entity, SuccessorId.class) != null;
    }

    protected <E extends T> boolean isAncestralRoot(E entity) {
        if (BeanUtils.hasAnnotatedField(entity, AncestorRootId.class)) {
            return BeanUtils.getAnnotatedValue(entity, AncestorRootId.class) == null;
        }
        return false;
    }

    @Override
    public <S extends T> boolean isPrivateWorkingCopy(S entity) {

        String sql = "select count(f1.${id}) FROM ${entityClass} f1 inner join ${entityClass} f2 on f1.${ancestorId} = f2.${id} and f2.${successorId} IS NULL where f1.${id} = :id";

        Map<String, String> attributes = getAttributeMap(entity.getClass());
        StringSubstitutor sub = new StringSubstitutor(attributes);
        sql = sub.replace(sql);

        TypedQuery<Long> q = em.createQuery(sql, Long.class);
        q.setParameter("id", BeanUtils.getAnnotatedValue(entity, Id.class));

        return (q.getSingleResult() == 1L);
    }

    @Override
    public <S extends T> S findWorkingCopy(S entity) {

        String sql = "select f1 FROM ${entityClass} f1 inner join ${entityClass} f2 on f1.${ancestorId} = f2.${id} and f2.${successorId} IS NULL where f1.${ancestorRootId} = :id";

        Map<String, String> attributes = getAttributeMap(entity.getClass());
        StringSubstitutor sub = new StringSubstitutor(attributes);
        sql = sub.replace(sql);

        @SuppressWarnings("unchecked")
        Class<S> entityType = (Class<S>) entity.getClass();
        TypedQuery<S> q = em.createQuery(sql, entityType);

        q.setParameter("id", getAncestralRootId(entity));

        try {
            return q.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

    private Map<String, String> getAttributeMap(Class<?> entityClass) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("id", idAttribute(entityClass));
        attributes.put("ancestorId", ancestorIdAttribute(entityClass));
        attributes.put("ancestorRootId", ancestorRootIdAttribute(entityClass));
        attributes.put("successorId", successorIdAttribute(entityClass));
        attributes.put("entityClass", entityClass.getName());
        return attributes;
    }

    private String idAttribute(Class<?> entityClass) {
        Field idField = BeanUtils.findAnnotatedField(entityClass, Id.class);
        if (idField == null) {
            idField = BeanUtils.findAnnotatedField(entityClass, org.springframework.data.annotation.Id.class);
        }
        if (idField == null) {
            throw new IllegalStateException(format("Entity class is missing @Id field: %s", entityClass.getCanonicalName()));
        }
        return idField.getName();
    }

    private String successorIdAttribute(Class<?> entityClass) {
        Field successorIdField = BeanUtils.findAnnotatedField(entityClass, SuccessorId.class);
        if (successorIdField == null) {
            throw new IllegalStateException(format("Entity class is missing @SuccessorId field: %s", entityClass.getCanonicalName()));
        }
        return successorIdField.getName();
    }

    private String ancestorIdAttribute(Class<?> entityClass) {
        Field ancestorIdField = BeanUtils.findAnnotatedField(entityClass, AncestorId.class);
        if (ancestorIdField == null) {
            throw new IllegalStateException(format("Entity class is missing @AncestorId field: %s", entityClass.getCanonicalName()));
        }
        return ancestorIdField.getName();
    }

    private String ancestorRootIdAttribute(Class<?> entityClass) {
        Field ancestorRootIdField = BeanUtils.findAnnotatedField(entityClass, AncestorRootId.class);
        if (ancestorRootIdField == null) {
            throw new IllegalStateException(format("Entity class is missing @AncestorRootId field: %s", entityClass.getCanonicalName()));
        }
        return ancestorRootIdField.getName();
    }

    protected <E> Object getAncestralRootId(E entity) {
        return BeanUtils.getAnnotatedValue(entity, AncestorRootId.class);
    }

    protected <E> Object getAncestorId(E entity) {
        return BeanUtils.getAnnotatedValue(entity, AncestorId.class);
    }

    protected <E> Object getId(E entity) {
        Object id = BeanUtils.getAnnotatedValue(entity, Id.class);
        if (id == null) {
            id = BeanUtils.getAnnotatedValue(entity, org.springframework.data.annotation.Id.class);
        }
        return id;
    }

    private static boolean mayMergeOnSave(@Nullable Authentication authentication, Principal lockOwner) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return lockOwner == null;
        }
        return lockOwner == null || Objects.equals(authentication.getName(), lockOwner.getName());
    }

    @SuppressWarnings("unchecked")
    private <S extends T> S cloneVersion(S currentVersion) {
        return (S) cloner.clone(currentVersion);
    }

    @SuppressWarnings("unchecked")
    private <S extends T> S establishAncestralRoot(S currentVersion) {
        return (S) versioner.establishAncestralRoot(currentVersion);
    }

    private <S extends T> S establishAncestralRootAndMerge(S currentVersion) {
        S updated = establishAncestralRoot(currentVersion);
        em.merge(updated);
        return updated;
    }

    @SuppressWarnings("unchecked")
    private <S extends T> S establishSuccessor(S candidate, String versionNo, String versionLabel,
                                               S ancestralRoot, S ancestor) {
        return (S) versioner.establishSuccessor(candidate, versionNo, versionLabel, ancestralRoot, ancestor);
    }

    @SuppressWarnings("unchecked")
    private <S extends T> S establishAncestor(S entity, S successor) {
        return (S) versioner.establishAncestor(entity, successor);
    }

    @SuppressWarnings("unchecked")
    private <S extends T> S findEntity(Class<?> entityClass, Object id) {
        return (S) em.find(entityClass, id);
    }

    @SuppressWarnings("unchecked")
    protected EntityInformation<T, ?> getEntityInformation(Class<?> entityClass) {
        EntityInformation<T, ?> entityInformation = this.entityInformationMap.get(entityClass);
        if (entityInformation == null) {
            entityInformation = (EntityInformation<T, ?>) this.entityInfo.getEntityInformation(entityClass, em);
            this.entityInformationMap.put(entityClass, entityInformation);
        }
        return entityInformation;
    }
}
