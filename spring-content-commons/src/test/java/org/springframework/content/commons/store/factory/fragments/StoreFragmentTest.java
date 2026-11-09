package org.springframework.content.commons.store.factory.fragments;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.factory.testsupport.EnableTestStores;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = StoreFragmentTest.StoreTestConfiguration.class)
@ExtendWith(SpringExtension.class)
public class StoreFragmentTest {

    @Autowired
    private ApplicationContext context;

    @SuppressWarnings("unused") // classpath / store component scan
    public interface TestContentStore extends ContentStore<Object, Serializable>, Customization {
    }

    public interface Customization {
        String greet(String name);
    }

    @Configuration
    @EnableTestStores
    public static class StoreTestConfiguration {

        @Bean
        public String bean() {
            return "Spring Content";
        }
    }

    public static class CustomizationImpl implements Customization {

        @Autowired
        private String bean;

        private Class<?> domainClass;
        private Class<?> idClass;

        @Override
        public String greet(String name) {
            return "Hello " + bean + " " + name;
        }

        public String getBean() {
            return bean;
        }

        public void setBean(String bean) {
            this.bean = bean;
        }

        public Class<?> getDomainClass() {
            return domainClass;
        }

        public void setDomainClass(Class<?> domainClass) {
            this.domainClass = domainClass;
        }

        public Class<?> getIdClass() {
            return idClass;
        }

        public void setIdClass(Class<?> idClass) {
            this.idClass = idClass;
        }
    }

    @Nested
    class GivenAStoreDefinitionCases {
        @Nested
        class GivenTheApplicationContext {
            @Test
            void shouldSupportTheExtension() {
                assertThat(context.getBean(TestContentStore.class)).isNotNull();
                assertThat(context.getBean(CustomizationImpl.class).getBean()).isEqualTo("Spring Content");
                assertThat(context.getBean(CustomizationImpl.class).getDomainClass()).isEqualTo(Object.class);
                assertThat(context.getBean(CustomizationImpl.class).getIdClass()).isEqualTo(Serializable.class);
                assertThat(context.getBean(TestContentStore.class).greet("World")).isEqualTo("Hello Spring Content World");
            }
        }
    }
}
