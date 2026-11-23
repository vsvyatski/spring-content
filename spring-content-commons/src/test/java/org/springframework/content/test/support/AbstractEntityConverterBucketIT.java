package org.springframework.content.test.support;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.test.support.jpa.DualContentJpaLongIdMimeEntity;
import org.springframework.content.test.support.jpa.DualContentJpaLongIdMimeRepository;
import org.springframework.content.test.support.jpa.DualContentJpaLongIdMimeStore;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shared default-vs-custom entity-converter cases for S3, GCS, and Azure bucket ITs.
 * Subclasses supply {@code converterConfigs}, the bucket entity, and storage assertions.
 *
 * @author Vladimir Svyatski
 */
public abstract class AbstractEntityConverterBucketIT {

    public static final String INIT_BUCKET = "spring-eg-content";

    public static final String DEFAULT_BUCKET = "default-4f91c2";

    public static final String CUSTOM_BUCKET = "custom-8c3e17";

    protected DualContentJpaLongIdMimeEntity entity;

    protected AnnotationConfigApplicationContext context;

    protected DualContentJpaLongIdMimeRepository repo;

    protected DualContentJpaLongIdMimeStore store;

    protected static Case[] cases(Class<?> storeConfig, Class<?> customConverterConfig) {
        return new Case[]{
            new Case("Default Converter", new Class<?>[]{storeConfig}, DEFAULT_BUCKET),
            new Case("Custom Converter", new Class<?>[]{customConverterConfig, storeConfig}, CUSTOM_BUCKET)
        };
    }

    protected abstract DualContentJpaLongIdMimeEntity newEntity();

    /**
     * Context is refreshed and the JPA repository and store are bound.
     */
    protected void afterRefresh(Case testData) {
    }

    protected abstract void assertStored(Case testData);

    protected abstract void assertRemoved(Case testData, String contentId);

    @ParameterizedTest(name = "{0}")
    @MethodSource("converterConfigs")
    void shouldStoreNewContentInBucket(Case testData) {
        run(testData, () -> {
            saveEntityWithContent();
            assertStored(testData);
        });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("converterConfigs")
    void shouldHaveContentMetadata(Case testData) {
        run(testData, () -> {
            saveEntityWithContent();
            assertThat(entity.getContentId()).isNotNull();
            assertThat(entity.getContentId().trim().length()).isGreaterThan(0);
            assertThat(entity.getContentLen()).isEqualTo(27L);
        });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("converterConfigs")
    void shouldDeleteContentFromBucket(Case testData) {
        run(testData, () -> {
            saveEntityWithContent();
            String contentId = Objects.requireNonNull(entity.getContentId());
            entity = store.unsetContent(entity, PropertyPath.from("content"));
            entity = repo.save(entity);
            assertRemoved(testData, contentId);
        });
    }

    private void boot(Case testData) {
        context = new AnnotationConfigApplicationContext();
        context.register(testData.config());
        context.refresh();
        repo = context.getBean(DualContentJpaLongIdMimeRepository.class);
        store = context.getBean(DualContentJpaLongIdMimeStore.class);
        afterRefresh(testData);
    }

    private void saveEntityWithContent() {
        entity = newEntity();
        entity.setContentType("text/plain");
        entity = repo.save(entity);
        store.setContent(entity, PropertyPath.from("content"), TestSupportData.sampleTextStream());
    }

    private void run(Case testData, Runnable checks) {
        try {
            boot(testData);
            checks.run();
        } finally {
            if (context != null) {
                context.close();
            }
        }
    }

    public record Case(String name, Class<?>[] config, String bucket) {
        @Override
        public @NonNull String toString() {
            return name;
        }
    }
}
