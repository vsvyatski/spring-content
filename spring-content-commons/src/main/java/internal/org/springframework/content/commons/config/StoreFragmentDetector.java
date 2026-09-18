package internal.org.springframework.content.commons.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.data.util.Lazy;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class StoreFragmentDetector {

    private static final Log LOGGER = LogFactory.getLog(StoreFragmentDetector.class);

    private static final String CUSTOM_IMPLEMENTATION_RESOURCE_PATTERN = "**/*%s.class";

    private final Environment environment;
    private final ResourceLoader resourceLoader;
    private final String postfix;
    private final Set<String> basePackages;
    private final MetadataReaderFactory metadataReaderFactory;
    private final Lazy<Set<BeanDefinition>> implementationCandidates;

    public StoreFragmentDetector(Environment environment, ResourceLoader loader, String postfix, String[] basePackages, MetadataReaderFactory metadataReaderFactory) {
        this.environment = environment;
        this.resourceLoader = loader;
        this.postfix = postfix;

        this.basePackages = new HashSet<>(Arrays.asList(basePackages));
        this.basePackages.add("org.springframework.content.fragments");
        this.basePackages.add("internal.org.springframework.content.fragments");

        this.metadataReaderFactory = metadataReaderFactory;
        this.implementationCandidates = Lazy.of(this::findCandidateBeanDefinitions);
    }

    public Set<BeanDefinition> getBeanDefinitions() {
        return implementationCandidates.get();
    }

    private Set<BeanDefinition> findCandidateBeanDefinitions() {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false, environment);
        provider.setResourceLoader(resourceLoader);
        provider.setResourcePattern(format(CUSTOM_IMPLEMENTATION_RESOURCE_PATTERN, postfix));
        provider.setMetadataReaderFactory(metadataReaderFactory);
        provider.addIncludeFilter((reader, factory) -> true);

        return basePackages.stream()
                .flatMap(it -> provider.findCandidateComponents(it).stream())
                .collect(Collectors.toSet());
    }

    public StoreFragmentDefinition detectCustomImplementation(String interfaceName, String storeInterface) {

        Predicate<BeanDefinition> predicate = new InterfaceNamePredicate(interfaceName, basePackages, postfix);

        List<BeanDefinition> definitions = implementationCandidates.get().stream().filter(predicate).toList();

        if (definitions.isEmpty()) {
            throw new IllegalStateException(format("No implementation found for store interface %s", interfaceName));
        }

        if (definitions.size() > 1) {
            LOGGER.info(String.format("Found implementations found for %s.  Using %s", interfaceName,
                    definitions.get(0).getBeanClassName()));
        }

        StoreFragmentDefinition def = new StoreFragmentDefinition(interfaceName, definitions.get(0));
        def.setStoreInterfaceName(storeInterface);
        return def;
    }

    private record InterfaceNamePredicate(String interfaceName, Set<String> basePackages,
                                          String postfix) implements Predicate<BeanDefinition> {

        @Override
        public boolean test(BeanDefinition definition) {
            Assert.notNull(definition, "BeanDefinition must not be null!");

            String beanClassName = definition.getBeanClassName();

            if (beanClassName == null /*|| isExcluded(beanClassName, getExcludeFilters())*/) {
                return false;
            }

            String beanPackage = ClassUtils.getPackageName(beanClassName);
            String shortName = ClassUtils.getShortName(beanClassName);
            String localName = shortName.substring(shortName.lastIndexOf('.') + 1);

            return localName.equals(getImplementationClassName(interfaceName))
                    && basePackages.stream().anyMatch(beanPackage::startsWith);
        }

        private String getImplementationClassName(String interfaceName) {
            String shortName = ClassUtils.getShortName(interfaceName);
            String localName = shortName.substring(shortName.lastIndexOf('.') + 1);
            return localName.concat(postfix);
        }
    }
}
