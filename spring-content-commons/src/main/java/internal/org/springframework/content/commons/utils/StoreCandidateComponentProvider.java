package internal.org.springframework.content.commons.utils;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.content.commons.store.ReactiveContentStore;
import org.springframework.content.commons.store.Store;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AssignableTypeFilter;

public class StoreCandidateComponentProvider
        extends ClassPathScanningCandidateComponentProvider {

    public StoreCandidateComponentProvider(boolean useDefaultFilters, Environment env) {
        super(useDefaultFilters, env);
        this.addIncludeFilter(new InterfaceTypeFilter(Store.class));
        this.addIncludeFilter(new InterfaceTypeFilter(ReactiveContentStore.class));
    }

    @Override
    protected boolean isCandidateComponent(@NonNull AnnotatedBeanDefinition beanDefinition) {
        return true;
    }

    /**
     * Stolen from Oliver Gierke
     */
    private static class InterfaceTypeFilter extends AssignableTypeFilter {

        /**
         * Creates a new {@link InterfaceTypeFilter}.
         *
         * @param targetType a type to create the filter for
         */
        public InterfaceTypeFilter(Class<?> targetType) {
            super(targetType);
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.springframework.core.type.filter.AbstractTypeHierarchyTraversingFilter#
         * match(org.springframework.core.type.classreading.MetadataReader,
         * org.springframework.core.type.classreading.MetadataReaderFactory)
         */
        @Override
        public boolean match(MetadataReader metadataReader,
                             @NonNull MetadataReaderFactory metadataReaderFactory) throws IOException {

            return metadataReader.getClassMetadata().isInterface()
                    && super.match(metadataReader, metadataReaderFactory);
        }
    }
}
