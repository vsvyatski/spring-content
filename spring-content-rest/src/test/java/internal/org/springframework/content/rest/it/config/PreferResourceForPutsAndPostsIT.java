package internal.org.springframework.content.rest.it.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import internal.org.springframework.content.rest.it.SecurityConfiguration;
import internal.org.springframework.content.rest.support.TestEntity;
import internal.org.springframework.content.rest.support.TestEntity2;
import internal.org.springframework.content.rest.support.mockstore.EnableMockStores;
import internal.org.springframework.content.rest.support.mockstore.MockContentStore;
import internal.org.springframework.content.rest.support.mockstore.MockStoreFactoryBean;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.rest.config.ContentRestConfigurer;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.util.UUID;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = PreferResourceForPutsAndPostsIT.Application.class, webEnvironment=WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
public class PreferResourceForPutsAndPostsIT {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private PreferResourceForPutsAndPostsRepository repo;

    @Autowired
    private PreferResourceForPutsAndPostsStore store;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private TestEntity2 existingClaim;

    
    @Nested
    class PreferResourceForPutsAndPosts {
        @BeforeEach
        void setUp() {
            RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
        }

        @Test
        void shouldUseTheSetContentSResourceMethod() {
            TestEntity tentity = new TestEntity();
            tentity = repo.save(tentity);

            given()
                    .contentType("text/plain")
                    .body("some content".getBytes())
                    .when()
                    .post("/testEntities/" + tentity.getId());

            MockStoreFactoryBean storeFactory = context.getBean(MockStoreFactoryBean.class);

            verify(storeFactory.getMock()).setContent(org.mockito.ArgumentMatchers.isA(TestEntity.class), org.mockito.ArgumentMatchers.isA(PropertyPath.class), (Resource)org.mockito.ArgumentMatchers.isA(Resource.class));
        }

    }

    public interface PreferResourceForPutsAndPostsRepository extends CrudRepository<TestEntity, Long> {
    }

    private interface PreferResourceForPutsAndPostsStore extends MockContentStore<TestEntity, UUID> {
    }

    @SpringBootApplication(exclude = {
            MongoAutoConfiguration.class,
            DataMongoAutoConfiguration.class,
            DataMongoRepositoriesAutoConfiguration.class
    })
    public static class Application {

       public static void main(String[] args) {
            SpringApplication.run(Application.class, args);
        }

       @Configuration
       @Import({RestConfiguration.class, SecurityConfiguration.class})
       @EnableJpaRepositories(basePackages="internal.org.springframework.content.rest.it.config", considerNestedRepositories = true)
       @EnableMockStores(basePackages="internal.org.springframework.content.rest.it.config")
       public static class AppConfig {

           @Value("/org/springframework/content/jpa/schema-drop-h2.sql")
           private ClassPathResource dropReopsitoryTables;

           @Value("/org/springframework/content/jpa/schema-h2.sql")
           private ClassPathResource dataReopsitorySchema;

           @Bean
           DataSourceInitializer datasourceInitializer(DataSource dataSource) {
               ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();

               databasePopulator.addScript(dropReopsitoryTables);
               databasePopulator.addScript(dataReopsitorySchema);
               databasePopulator.setIgnoreFailedDrops(true);

               DataSourceInitializer initializer = new DataSourceInitializer();
               initializer.setDataSource(dataSource);
               initializer.setDatabasePopulator(databasePopulator);

               return initializer;
           }

           @Bean
           public DataSource dataSource() {
               EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
               return builder.setType(EmbeddedDatabaseType.H2).build();
           }

           @Bean
           public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
               HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
               vendorAdapter.setDatabase(Database.H2);
               vendorAdapter.setGenerateDdl(true);

               LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
               factory.setJpaVendorAdapter(vendorAdapter);
               factory.setPackagesToScan("internal.org.springframework.content.rest.support");
               factory.setDataSource(dataSource());

               return factory;
           }

           @Bean
           public PlatformTransactionManager transactionManager() {
               JpaTransactionManager txManager = new JpaTransactionManager();
               txManager.setEntityManagerFactory(entityManagerFactory().getObject());
               return txManager;
           }

           @Bean
           public ContentRestConfigurer configurer() {
               return new ContentRestConfigurer() {
                   @Override
                   public void configure(RestConfiguration config) {
                       config.forDomainType(TestEntity.class).putAndPostPreferResource();
                   }
               };
           }
       }
    }

}
