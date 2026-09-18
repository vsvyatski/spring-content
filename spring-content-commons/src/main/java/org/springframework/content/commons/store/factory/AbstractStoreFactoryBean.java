package org.springframework.content.commons.store.factory;

import internal.org.springframework.content.commons.config.StoreFragment;
import internal.org.springframework.content.commons.config.StoreFragments;
import internal.org.springframework.content.commons.store.factory.ReactiveStoreImpl;
import internal.org.springframework.content.commons.store.factory.StoreExceptionTranslatorInterceptor;
import internal.org.springframework.content.commons.store.factory.StoreFactory;
import internal.org.springframework.content.commons.store.factory.StoreImpl;
import internal.org.springframework.content.commons.store.factory.StoreMethodInterceptor;
import org.apache.commons.lang3.ClassUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.fragments.ParameterTypeAware;
import org.springframework.content.commons.store.AssociativeStore;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.ReactiveContentStore;
import org.springframework.content.commons.store.Store;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Paths;
import java.util.Collections;

public abstract class AbstractStoreFactoryBean implements BeanFactoryAware, InitializingBean, BeanClassLoaderAware,
		FactoryBean<Store<? extends Serializable>>,
		ApplicationEventPublisherAware, StoreFactory {

	protected static boolean REACTIVE_STORAGE = false;

	static {
		try {
			Class.forName("org.springframework.web.reactive.config.WebFluxConfigurationSupport");
			REACTIVE_STORAGE = true;
		} catch (ClassNotFoundException ignored) {
		}
	}

	private final Class<? extends Store> storeInterface;
	private ClassLoader classLoader;
	private ApplicationEventPublisher publisher;

	private Store<? extends Serializable> store;

	private StoreFragments storeFragments = new StoreFragments(Collections.EMPTY_LIST);

	private BeanFactory beanFactory;

	protected AbstractStoreFactoryBean(Class<? extends Store> storeInterface) {
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

	@Override
	public Class<? extends Store> getStoreInterface() {
		return this.storeInterface;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Store<Serializable> getStore() {
		return (Store<Serializable>) getObject();
	}

	@Override
	public void setBeanClassLoader(@NonNull ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void setApplicationEventPublisher(@NonNull ApplicationEventPublisher applicationEventPublisher) {
		this.publisher = applicationEventPublisher;
	}

	@Override
	public Store<? extends Serializable> getObject() {
		return initAndReturn();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends Store<? extends Serializable>> getObjectType() {
		return (Class<? extends Store<? extends Serializable>>) this.storeInterface;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		initAndReturn();
	}

	private Store<? extends Serializable> initAndReturn() {
		if (store == null) {
			store = createContentStore();
		}
		return store;
	}

	@SuppressWarnings("unchecked")
	protected Store<? extends Serializable> createContentStore() {
		Object target = getContentStoreImpl();

		ProxyFactory result = new ProxyFactory();
		result.setTarget(target);
		if (!ClassUtils.getAllInterfaces(storeInterface).contains(ReactiveContentStore.class)) {
			result.setInterfaces(new Class[]{
					storeInterface,
					Store.class,
					AssociativeStore.class,
					ContentStore.class,
					ParameterTypeAware.class
			});
		} else {
			result.setInterfaces(new Class[]{
					storeInterface,
					Store.class,
					ReactiveContentStore.class,
					ParameterTypeAware.class
			});
		}

		this.addProxyAdvice(result, beanFactory);

		StoreMethodInterceptor interceptor = new StoreMethodInterceptor();

		if (!ClassUtils.getAllInterfaces(storeInterface).contains(ReactiveContentStore.class)) {
			storeFragments.add(new StoreFragment(storeInterface, new StoreImpl(storeInterface, target, publisher, Paths.get(System.getProperty("java.io.tmpdir")))));
		} else {
			storeFragments.add(new StoreFragment(storeInterface, new ReactiveStoreImpl((ReactiveContentStore<Object>) target)));
		}
		interceptor.setStoreFragments(storeFragments);

		result.addAdvice(new StoreExceptionTranslatorInterceptor(beanFactory));
		result.addAdvice(interceptor);

		return (Store<? extends Serializable>) result.getProxy(classLoader);
	}

	@Override
	public void setBeanFactory(@NonNull BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	protected void addProxyAdvice(ProxyFactory result, BeanFactory beanFactory) {
	}

	protected abstract Object getContentStoreImpl();
}
