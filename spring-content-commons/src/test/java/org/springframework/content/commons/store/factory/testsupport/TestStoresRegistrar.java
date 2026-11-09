package org.springframework.content.commons.store.factory.testsupport;

import org.springframework.content.commons.config.AbstractStoreBeanDefinitionRegistrar;

import java.lang.annotation.Annotation;

public class TestStoresRegistrar extends AbstractStoreBeanDefinitionRegistrar {

    @Override
    protected Class<? extends Annotation> getAnnotation() {
        return EnableTestStores.class;
    }

    @Override
    protected Class<?>[] getSignatureTypes() {
        return new Class[]{TestContentStore.class};
    }
}
