package org.springframework.content.mongo.config;

import internal.org.springframework.content.mongo.config.MongoContentStoresRegistrar;
import internal.org.springframework.content.mongo.config.MongoStoreConfiguration;
import internal.org.springframework.content.mongo.config.MongoStoreFactoryBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactoryBean;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({MongoContentStoresRegistrar.class, MongoStoreConfiguration.class})
public @interface EnableMongoStores {

    /**
     * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation
     * declarations e.g.: {@code @EnableJpaRepositories("org.my.pkg")} instead of
     * {@code @EnableJpaRepositories(basePackages="org.my.pkg")}.
     */
    String[] value() default {};

    /**
     * Base packages to scan for annotated components. {@link #value()} is an alias for
     * (and mutually exclusive with) this attribute. Use {@link #basePackageClasses()} for
     * a type-safe alternative to String-based package names.
     */
    String[] basePackages() default {};

    /**
     * Type-safe alternative to {@link #basePackages()} for specifying the packages to
     * scan for annotated components. The package of each class specified will be scanned.
     * Consider creating a special no-op marker class or interface in each package that
     * serves no purpose other than being referenced by this attribute.
     */
    Class<?>[] basePackageClasses() default {};

    /**
     * Returns the {@link FactoryBean} class to be used for each repository instance.
     * Defaults to {@link MongoRepositoryFactoryBean}.
     *
     * @return
     */
    Class<?> storeFactoryBeanClass() default MongoStoreFactoryBean.class;

}
