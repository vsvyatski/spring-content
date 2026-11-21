package internal.org.springframework.content.jpa;

import internal.org.springframework.content.jpa.StoreIT.H2Config;
import internal.org.springframework.content.jpa.StoreIT.HSQLConfig;
import internal.org.springframework.content.jpa.StoreIT.MySqlConfig;
import internal.org.springframework.content.jpa.StoreIT.PostgresConfig;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.content.test.support.jpa.SingleContentJpaLongIdEntity;
import org.springframework.content.test.support.jpa.SingleContentJpaLongIdRepository;
import org.springframework.content.test.support.jpa.SingleContentJpaLongIdStore;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    private SingleContentJpaLongIdRepository repo;
    private SingleContentJpaLongIdStore store;
    private DbService dbService;

    private SingleContentJpaLongIdEntity entity;

    @Configuration
    public static class DbServiceConfig {

        @Bean
        public DbService dbService() {
            return new DbService();
        }
    }

    @Component
    public static class DbService {

        @Transactional
        public SingleContentJpaLongIdEntity doSomeDbStuff(SingleContentJpaLongIdStore store, SingleContentJpaLongIdEntity entity) {
            entity = store.setContent(entity, new ByteArrayInputStream("Spring Content World!".getBytes()));
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
            context.register(StoreIT.TestConfig.class);
            context.register(DbServiceConfig.class);
            context.register(configClass);
            context.refresh();

            repo = context.getBean(SingleContentJpaLongIdRepository.class);
            store = context.getBean(SingleContentJpaLongIdStore.class);
            dbService = context.getBean(DbService.class);
            ptm = context.getBean(PlatformTransactionManager.class);

            entity = new SingleContentJpaLongIdEntity();
            entity = repo.save(entity);
            assertThat(entity.getId()).isNotNull();
            assertThat(entity.getContentId()).isNull();

            try {
                try {
                    entity = dbService.doSomeDbStuff(store, entity);
                } catch (Exception e) {
                    ContentStoreIT.doInTransaction(ptm, () -> {
                        try (InputStream result = store.getContent(entity)) {
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
}