package internal.org.springframework.content.jpa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestFactory;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.transaction.Transactional;

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

import internal.org.springframework.content.jpa.StoreIT.H2Config;
import internal.org.springframework.content.jpa.StoreIT.HSQLConfig;
import internal.org.springframework.content.jpa.StoreIT.MySqlConfig;
import internal.org.springframework.content.jpa.StoreIT.PostgresConfig;
import internal.org.springframework.content.jpa.StoreIT.SqlServerConfig;

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

	private TestEntityRepository repo = null;
	private TestEntityContentRepository store = null;
	private DbService dbService = null;

	private TestEntity te = null;

    @Nested
    class TransactionTest {
        @TestFactory
        java.util.stream.Stream<org.junit.jupiter.api.DynamicNode> generatedCases() {
            java.util.List<org.junit.jupiter.api.DynamicNode> tests = new java.util.ArrayList<>();
            for (Class<?> configClass : CONFIG_CLASSES) {
                tests.add(org.junit.jupiter.api.DynamicTest.dynamicTest("should not commit changes to content", () -> {
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
                        		} catch (IOException e1) {}
                        		return null;
                        	});
                        }

                    } finally {
                        context.close();

                    }
                }));
            }
            return tests.stream();
        }

    }

	private static String getContextName(Class<?> configClass) {
		return configClass.getSimpleName().replaceAll("Config", "");
	}

	@Configuration
	@EnableJpaRepositories(considerNestedRepositories=true)
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

	@Entity
	@Table(name="test_entities")
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

	public interface TestEntityRepository extends JpaRepository<TestEntity, String> {
	}

	public interface TestEntityContentRepository extends ContentStore<TestEntity, String> {
	}
}
