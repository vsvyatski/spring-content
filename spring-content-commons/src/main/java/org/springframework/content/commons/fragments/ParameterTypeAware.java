package org.springframework.content.commons.fragments;

/**
 * Fragment wiring hook: {@link org.springframework.content.commons.config.AbstractStoreBeanDefinitionRegistrar}
 * sets store interface type arguments when the implementation supports it.
 */
public interface ParameterTypeAware {

    void setGenericArguments(Class<?>[] arguments);
}
