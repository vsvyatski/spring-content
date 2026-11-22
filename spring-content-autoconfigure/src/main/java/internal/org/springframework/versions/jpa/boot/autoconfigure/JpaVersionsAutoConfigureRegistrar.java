package internal.org.springframework.versions.jpa.boot.autoconfigure;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

public class JpaVersionsAutoConfigureRegistrar implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(
        @NonNull AnnotationMetadata importingClassMetadata, @NonNull BeanDefinitionRegistry registry
    ) {
        AutoConfigurationPackages.register(registry, "org.springframework.versions");
    }
}
