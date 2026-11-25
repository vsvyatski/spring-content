package internal.org.springframework.content.rest.it.config;

import internal.org.springframework.content.rest.support.UuidSingleContentEntity;
import internal.org.springframework.content.rest.support.mock.store.MockContentStore;
import internal.org.springframework.content.rest.support.mock.store.MockStoreFactoryBean;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = Application.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
public class PreferResourceForPutsAndPostsIT {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private PreferResourceForPutsAndPostsRepository repo;

    @Autowired
    private WebApplicationContext webApplicationContext;

    public interface PreferResourceForPutsAndPostsRepository extends CrudRepository<UuidSingleContentEntity, Long> {
    }

    @SuppressWarnings("unused") // classpath / store component scan
    private interface PreferResourceForPutsAndPostsStore extends MockContentStore<UuidSingleContentEntity, UUID> {
    }

    @Nested
    class PreferResourceForPutsAndPosts {
        @BeforeEach
        void setUp() {
            RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
        }

        @Test
        void shouldUseSetContentResourceMethod() {
            UuidSingleContentEntity entity = new UuidSingleContentEntity();
            entity = repo.save(entity);

            given()
                .contentType(MediaType.TEXT_PLAIN_VALUE)
                .body("some content".getBytes())
                .when()
                .post("/uuidSingleContentEntities/" + entity.getId());

            MockStoreFactoryBean storeFactory = context.getBean(MockStoreFactoryBean.class);

            verify(storeFactory.getMock()).setContent(org.mockito.ArgumentMatchers.isA(UuidSingleContentEntity.class), org.mockito.ArgumentMatchers.isA(PropertyPath.class), org.mockito.ArgumentMatchers.isA(Resource.class));
        }
    }
}
