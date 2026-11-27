package internal.org.springframework.content.commons.config;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.util.List;

public class StoreFragmentsFactoryBean implements FactoryBean<StoreFragments>, BeanFactoryAware, InitializingBean {

    private final List<String> fragmentBeanNames;
    private BeanFactory beanFactory;
    private StoreFragments storeFragments;

    public StoreFragmentsFactoryBean(List<String> fragmentBeanNames) {
        Assert.notNull(fragmentBeanNames, "Fragment bean names must not be null!");
        this.fragmentBeanNames = fragmentBeanNames;
    }

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void afterPropertiesSet() {

        storeFragments = new StoreFragments(fragmentBeanNames.stream()
            .map(it -> (StoreFragment<?>) beanFactory.getBean(it, StoreFragment.class))
            .toList());
    }

    @Override
    public StoreFragments getObject() throws Exception {
        return storeFragments;
    }

    @Override
    public Class<?> getObjectType() {
        return StoreFragments.class;
    }
}
