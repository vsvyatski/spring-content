package internal.org.springframework.content.rest.it.cache.control;

import internal.org.springframework.content.rest.support.UuidSingleContentEntity;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.WebApplicationContext;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;

@SpringBootTest(classes = Application.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
public class CacheControlIT {

    @Autowired
    private CacheControlRepository repo;
    @Autowired
    private CacheControlStore store;
    @Autowired
    private WebApplicationContext webApplicationContext;

    public interface CacheControlRepository extends CrudRepository<UuidSingleContentEntity, Long> {
    }

    private interface CacheControlStore extends FileSystemContentStore<UuidSingleContentEntity, UUID> {
    }

    @Nested
   class CacheControlCases {
        @BeforeEach
        void setUp() {
            RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
        }

        @Test
        void givenThePatternMatchesItShouldApplyTheConfiguredCacheSettings() {
            UuidSingleContentEntity entity = new UuidSingleContentEntity();
            entity = repo.save(entity);
            entity = store.setContent(entity, new ByteArrayInputStream("some content".getBytes()));
            entity.setMimeType("text/plain");
            entity = repo.save(entity);

            given()
                .accept("text/plain")
                .when()
                .get("/uuidSingleContentEntities/" + entity.getId())
                .then()
                .statusCode(200)
                .header("Cache-Control", "max-age=60");
        }

        @Test
        void givenThePatternDoesntMatchItShouldNotApplyTheConfiguredCacheSettings() {
            UuidSingleContentEntity entity = new UuidSingleContentEntity();
            entity = repo.save(entity);
            entity = store.setContent(entity, new ByteArrayInputStream("some content".getBytes()));
            entity.setMimeType("text/plain");
            entity = repo.save(entity);

            given()
                .accept("text/plain")
                .when()
                .get("/uuidSingleContentEntities/" + entity.getId() + "/content")
                .then()
                .statusCode(200)
                .header("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        }
    }
}
