package internal.org.springframework.content.rest.storeresolver;

import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import internal.org.springframework.content.rest.support.TestEntity2;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;

@SpringBootTest(classes = Application.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
public class StoreResolverRestConfigurationIT {

    @Autowired
    private Application.TEntityRepository repo;

    @Autowired
    private Application.TEntityJpaStore jpaStore;

    @Autowired
    private Application.TEntityFsStore fsStore;

    @LocalServerPort
    int port;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private Application.TEntity tEntity;

    private TestEntity2 existingClaim;

    @Nested
    class JpaRest {
        @Nested
        class GivenThatClaimHasExistingContent {
            @BeforeEach
            void setUp() {
                RestAssuredMockMvc.webAppContextSetup(webApplicationContext);

                tEntity = new Application.TEntity();
                tEntity = repo.save(tEntity);
            }

            @Test
            void shouldReturnTheContentFromTheCorrectStore() throws IOException {
                assertThat(jpaStore).isNotNull();
                assertThat(fsStore).isNotNull();
                assertThat(repo).isNotNull();

                String newContent = "This is some new content";

                given()
                        .contentType("text/plain")
                        .body(newContent.getBytes())
                        .when()
                        .post("/tEntities/" + tEntity.getId())
                        .then()
                        .statusCode(HttpStatus.SC_CREATED);

                // refetch
                tEntity = repo.findById(tEntity.getId()).orElse(null);

                try (InputStream is = fsStore.getContent(tEntity)) {
                    assertEquals(newContent, IOUtils.toString(is, Charset.defaultCharset()));
                }

                try (InputStream is = jpaStore.getContent(tEntity)) {
                    assertThat(is).isNull();
                }
            }

        }

    }

}
