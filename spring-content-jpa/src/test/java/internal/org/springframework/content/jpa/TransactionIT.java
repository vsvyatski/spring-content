package internal.org.springframework.content.jpa;

import internal.org.springframework.content.jpa.StoreIT.H2Config;
import internal.org.springframework.content.jpa.StoreIT.HSQLConfig;
import internal.org.springframework.content.jpa.StoreIT.MySqlConfig;
import internal.org.springframework.content.jpa.StoreIT.PostgresConfig;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class TransactionIT {

    private static Class<?>[] CONFIG_CLASSES = new Class[]{
            H2Config.class,
            HSQLConfig.class,
            MySqlConfig.class,
            PostgresConfig.class
//            SqlServerConfig.class,
//            StoreIT.OracleConfig.class
    };

    private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    // for postgres (large object api operations must be in a transaction)
    private PlatformTransactionManager ptm;

    private TestEntityRepository repo;
    private TestEntityContentRepository store;
    private DbService dbService;

    private TestEntity te;

    private static String getContextName(Class<?> configClass) {
        return configClass.getSimpleName().replaceAll("Config", "");
    }

    public interface TestEntityRepository extends JpaRepository<TestEntity, String> {
    }

    public interface TestEntityContentRepository extends ContentStore<TestEntity, String> {
    }

    @Configuration
    @EnableJpaRepositories(considerNestedRepositories = true)
    @EnableJpaStores
    public static class TestConfig {

        @Bean
        public DbService dbService() {
            return new DbService();
        }
    }

    @Component
    public static class DbService {

        @Transactional
        public TestEntity doSomeDbStuff(TestEntityContentRepository store, TestEntity te) {
            te = store.setContent(te, new ByteArrayInputStream("Spring Content World!".getBytes()));
            throw new RuntimeException("badness");
        }
    }

    static Stream<Class<?>> databaseConfigs() {
        return Stream.of(CONFIG_CLASSES);
    }

    @Nested
    class TransactionTest {
        @ParameterizedTest(name = "{0}")
        @MethodSource("internal.org.springframework.content.jpa.TransactionIT#databaseConfigs")
        void shouldNotCommitChangesToContent(Class<?> configClass) {
            context = new AnnotationConfigApplicationContext();
            context.register(TestConfig.class);
            context.register(configClass);
            context.refresh();

            repo = context.getBean(TestEntityRepository.class);
            store = context.getBean(TestEntityContentRepository.class);
            dbService = context.getBean(DbService.class);
            ptm = context.getBean(PlatformTransactionManager.class);

            te = new TestEntity();
            te = repo.save(te);
            assertThat(te.getId()).isNotNull();
            assertThat(te.getContentId()).isNull();

            try {
                try {
                    te = dbService.doSomeDbStuff(store, te);
                } catch (Exception e) {
                    ContentStoreIT.doInTransaction(ptm, () -> {
                        try (InputStream result = store.getContent(te)) {
                            assertThat(result).isNull();
                        } catch (IOException e1) {
                        }
                        return null;
                    });
                }
            } finally {
                context.close();
            }
        }
    }

    @Entity
    @Table(name = "test_entities")
    public class TestEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        @ContentId
        private String contentId;

        public TestEntity() {
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getContentId() {
            return contentId;
        }

        public void setContentId(String contentId) {
            this.contentId = contentId;
        }
    }
}
