package internal.org.springframework.content.jpa;

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

import static org.assertj.core.api.Assertions.assertThat;

public class TransactionIT {

    private SingleContentJpaLongIdStore store;

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
        public void doSomeDbStuff(SingleContentJpaLongIdStore store, SingleContentJpaLongIdEntity entity) {
            store.setContent(entity, new ByteArrayInputStream("Spring Content World!".getBytes()));
            throw new RuntimeException("badness");
        }
    }

    @Nested
   class TransactionTest {
        @ParameterizedTest(name = "{0}")
        @MethodSource("internal.org.springframework.content.jpa.StoreIT#databaseConfigs")
        void shouldNotCommitChangesToContent(Class<?> configClass) {
            AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
            context.register(StoreIT.TestConfig.class);
            context.register(DbServiceConfig.class);
            context.register(configClass);
            context.refresh();

            SingleContentJpaLongIdRepository repo = context.getBean(SingleContentJpaLongIdRepository.class);
            store = context.getBean(SingleContentJpaLongIdStore.class);
            DbService dbService = context.getBean(DbService.class);
            // for Postgres (large object api operations must be in a transaction)
            PlatformTransactionManager ptm = context.getBean(PlatformTransactionManager.class);

            entity = new SingleContentJpaLongIdEntity();
            entity = repo.save(entity);
            assertThat(entity.getId()).isNotNull();
            assertThat(entity.getContentId()).isNull();

            try {
                try {
                    dbService.doSomeDbStuff(store, entity);
                } catch (Exception e) {
                    ContentStoreIT.runInTransaction(ptm, () -> {
                        try (InputStream result = store.getContent(entity)) {
                            ContentStoreIT.assertNullInputStream(result);
                        } catch (IOException ignored) {
                        }
                    });
                }
            } finally {
                context.close();
            }
        }
    }
}
