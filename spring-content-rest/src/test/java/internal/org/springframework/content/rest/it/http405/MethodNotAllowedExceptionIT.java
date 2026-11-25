package internal.org.springframework.content.rest.it.http405;

import internal.org.springframework.content.rest.scoped.http405.*;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.WebApplicationContext;

import java.io.ByteArrayInputStream;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;

@SpringBootTest(classes = Application.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
public class MethodNotAllowedExceptionIT {

    @Autowired
    private UnexportedStoreMethodsEntityRepository repo;

    @Autowired
    private UnexportedStoreMethodsEntityStore store;

    @Autowired
    private UnexportedContentPropertyEntityRepository repo2;

    @Autowired
    private UnexportedContentPropertyEntityStore store2;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Nested
    @DisplayName("When content-getting methods are not exported")
    class ContentGettingMethodsAreNotExported {
        @BeforeEach
        void setUp() {
            RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
        }

        @Test
        void shouldThrow405NotAllowed() {
            UnexportedStoreMethodsEntity entity = new UnexportedStoreMethodsEntity();
            entity = store.setContent(entity, new ByteArrayInputStream("some content".getBytes()));
            entity = repo.save(entity);

            given()
                .accept(MediaType.TEXT_PLAIN_VALUE)
                .when()
                .get("/unexportedStoreMethodsEntities/" + entity.getId())
                .then()
                .statusCode(405);
        }
    }

    @Nested
    @DisplayName("When content-setting methods are not exported")
    class ContentSettingMethodsAreNotExported {
        @BeforeEach
        void setUp() {
            RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
        }

        @Test
        void shouldThrow405NotAllowed() {
            UnexportedStoreMethodsEntity entity = new UnexportedStoreMethodsEntity();
            entity = repo.save(entity);

            given()
                .contentType(MediaType.TEXT_PLAIN_VALUE)
                .body("some content".getBytes())
                .when()
                .post("/unexportedStoreMethodsEntities/" + entity.getId())
                .then()
                .statusCode(405);
        }
    }

    @Nested
    @DisplayName("When content-unsetting methods are not exported")
    class ContentUnsettingMethodsAreNotExported {
        @BeforeEach
        void setUp() {
            RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
        }

        @Test
        void shouldThrow405NotAllowed() {
            UnexportedStoreMethodsEntity entity = new UnexportedStoreMethodsEntity();
            entity = store.setContent(entity, new ByteArrayInputStream("some content".getBytes()));
            entity = repo.save(entity);

            given()
                .accept(MediaType.TEXT_PLAIN_VALUE)
                .when()
                .delete("/unexportedStoreMethodsEntities/" + entity.getId())
                .then()
                .statusCode(405);
        }
    }

    @Nested
    @DisplayName("When a content property is not exported")
    class ContentPropertyIsNotExported {
        @BeforeEach
        void setUp() {
            RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
        }

        @Test
        void shouldThrow405NotAllowedForAllRequests() {
            UnexportedContentPropertyEntity entity = new UnexportedContentPropertyEntity();
            entity = store2.setContent(entity, new ByteArrayInputStream("some content".getBytes()));
            entity = repo2.save(entity);

            given()
                .accept(MediaType.TEXT_PLAIN_VALUE)
                .when()
                .get("/unexportedContentPropertyEntities/" + entity.getId() + "/content")
                .then()
                .statusCode(405);

            given()
                .contentType(MediaType.TEXT_PLAIN_VALUE)
                .body("some content".getBytes())
                .when()
                .post("/unexportedContentPropertyEntities/" + entity.getId() + "/content")
                .then()
                .statusCode(405);

            given()
                .accept(MediaType.TEXT_PLAIN_VALUE)
                .when()
                .delete("/unexportedContentPropertyEntities/" + entity.getId() + "/content")
                .then()
                .statusCode(405);
        }
    }
}
