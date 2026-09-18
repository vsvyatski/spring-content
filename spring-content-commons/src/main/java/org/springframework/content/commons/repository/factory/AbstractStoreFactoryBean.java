package org.springframework.content.commons.repository.factory;

import internal.org.springframework.content.commons.config.StoreFragment;
import internal.org.springframework.content.commons.config.StoreFragments;
import internal.org.springframework.content.commons.store.factory.*;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.fragments.ParameterTypeAware;
import org.springframework.content.commons.repository.StoreExtension;
import org.springframework.content.commons.store.AssociativeStore;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.ReactiveContentStore;
import org.springframework.content.commons.store.Store;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @deprecated This class is deprecated. Use
 * {@link org.springframework.content.commons.store.factory.AbstractStoreFactoryBean} instead.
 */
@Deprecated
public abstract class AbstractStoreFactoryBean implements BeanFactoryAware, InitializingBean, BeanClassLoaderAware,
        FactoryBean<org.springframework.content.commons.repository.Store<? extends Serializable>>,
        ApplicationEventPublisherAware {

    private static final Log logger = LogFactory.getLog(AbstractStoreFactoryBean.class);

    protected static boolean REACTIVE_STORAGE = false;

    static {
        try {
            Class.forName("org.springframework.web.reactive.config.WebFluxConfigurationSupport");
            REACTIVE_STORAGE = true;
        } catch (ClassNotFoundException ignored) {
        }
    }

    private final Class<? extends org.springframework.content.commons.repository.Store> storeInterface;
    private ClassLoader classLoader;
    private ApplicationEventPublisher publisher;

    private org.springframework.content.commons.repository.Store<? extends Serializable> store;

    @Autowired(required = false)
    private Set<StoreExtension> extensions = Collections.emptySet();
    private StoreFragments storeFragments = new StoreFragments(Collections.EMPTY_LIST);

    private BeanFactory beanFactory;

    protected AbstractStoreFactoryBean(Class<? extends org.springframework.content.commons.repository.Store> storeInterface) {
        Assert.notNull(storeInterface, "storeInterface must not be null");
        this.storeInterface = storeInterface;
    }

    public static Class<?> getDomainClass(Class<?> repositoryClass) {
        return getStoreParameter(repositoryClass, 0);
    }

    public static Class<? extends Serializable> getContentIdClass(Class<?> repositoryClass) {
        return (Class<? extends Serializable>) getStoreParameter(repositoryClass, 1);
    }

    private static Class<?> getStoreParameter(Class<?> repositoryClass, int index) {
        Class<?> clazz = null;
        Type[] types = repositoryClass.getGenericInterfaces();

        for (Type t : types) {
            if (t instanceof ParameterizedType pt) {
                if (pt.getRawType().getTypeName()
                        .equals(Store.class.getCanonicalName())) {
                    types = pt.getActualTypeArguments();
                    if (types.length != 1) {
                        throw new IllegalStateException(
                                String.format("Store class %s must have a contentId type",
                                        repositoryClass.getCanonicalName()));
                    }
                    if (types[0] instanceof Class) {
                        clazz = (Class<?>) types[0];
                    }
                } else if (pt.getRawType().getTypeName()
                        .equals(AssociativeStore.class.getCanonicalName())
                        || pt.getRawType().getTypeName()
                        .equals(ContentStore.class.getCanonicalName())) {
                    types = pt.getActualTypeArguments();
                    if (types.length != 2) {
                        throw new IllegalStateException(String.format(
                                "ContentRepository class %s must have domain and contentId types",
                                repositoryClass.getCanonicalName()));
                    }
                    if (types[index] instanceof Class) {
                        clazz = (Class<?>) types[index];
                    }
                }
            }
        }
        return clazz;
    }

    @Autowired
    public void setStoreFragments(StoreFragments storeFragments) {
        this.storeFragments = storeFragments;
    }

    public Class<? extends org.springframework.content.commons.repository.Store> getStoreInterface() {
        return this.storeInterface;
    }

    @SuppressWarnings("unchecked")
    public org.springframework.content.commons.repository.Store<Serializable> getStore() {
        return (org.springframework.content.commons.repository.Store<Serializable>) getObject();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.springframework.beans.factory.BeanClassLoaderAware#setBeanClassLoader(java.lang
     * .ClassLoader)
     */
    @Override
    public void setBeanClassLoader(@NonNull ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.context.ApplicationEventPublisherAware#
     * setApplicationEventPublisher(org.springframework.context.ApplicationEventPublisher)
     */
    @Override
    public void setApplicationEventPublisher(@NonNull ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.beans.factory.FactoryBean#getObject()
     */
    @Override
    public org.springframework.content.commons.repository.Store<? extends Serializable> getObject() {
        return initAndReturn();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends org.springframework.content.commons.repository.Store<? extends Serializable>> getObjectType() {
        return (Class<? extends org.springframework.content.commons.repository.Store<? extends Serializable>>) this.storeInterface;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.beans.factory.FactoryBean#isSingleton()
     */
    @Override
    public boolean isSingleton() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        initAndReturn();
    }

    private org.springframework.content.commons.repository.Store<? extends Serializable> initAndReturn() {
        if (store == null) {
            store = createContentStore();
        }
        return store;
    }

    @SuppressWarnings("unchecked")
    protected org.springframework.content.commons.repository.Store<? extends Serializable> createContentStore() {
        Object target = getContentStoreImpl();

        // Create proxy
        ProxyFactory result = new ProxyFactory();
        result.setTarget(target);
        if (!ClassUtils.getAllInterfaces(storeInterface).contains(ReactiveContentStore.class) && !ClassUtils.getAllInterfaces(storeInterface).contains(org.springframework.content.commons.repository.ReactiveContentStore.class)) {
            result.setInterfaces(new Class[]{
                    storeInterface,
                    org.springframework.content.commons.repository.Store.class,
                    org.springframework.content.commons.repository.AssociativeStore.class,
                    org.springframework.content.commons.repository.ContentStore.class,
                    Store.class,
                    AssociativeStore.class,
                    ContentStore.class,
                    ParameterTypeAware.class
            });
        } else {
            result.setInterfaces(new Class[]{
                    storeInterface,
                    org.springframework.content.commons.repository.Store.class,
                    org.springframework.content.commons.repository.ReactiveContentStore.class,
                    Store.class,
                    ReactiveContentStore.class,
                    ParameterTypeAware.class
            });
        }

        Map<Method, StoreExtension> extensionsMap = new HashMap<>();
        try {
            for (StoreExtension extension : extensions) {
                for (Method method : extension.getMethods()) {
                    extensionsMap.put(method, extension);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to setup extensions", e);
        }

        this.addProxyAdvice(result, beanFactory);

        StoreMethodInterceptor interceptor = new StoreMethodInterceptor();

        if (!ClassUtils.getAllInterfaces(storeInterface).contains(ReactiveContentStore.class) && !ClassUtils.getAllInterfaces(storeInterface).contains(org.springframework.content.commons.repository.ReactiveContentStore.class)) {
            storeFragments.add(new StoreFragment(storeInterface, new StoreImpl(storeInterface, target, publisher, Paths.get(System.getProperty("java.io.tmpdir")))));
        } else {
            storeFragments.add(new StoreFragment(storeInterface, new ReactiveStoreImpl((ReactiveContentStore<Object>) target)));
        }
        interceptor.setStoreFragments(storeFragments);

        result.addAdvice(new StoreExceptionTranslatorInterceptor(beanFactory));
        result.addAdvice(interceptor);

        return (org.springframework.content.commons.repository.Store<? extends Serializable>) result.getProxy(classLoader);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
     */
    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    protected void addProxyAdvice(ProxyFactory result, BeanFactory beanFactory) {
    }

    protected abstract Object getContentStoreImpl();
}
