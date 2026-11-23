package internal.org.springframework.content.rest.store.resolver;

import internal.org.springframework.content.rest.scoped.store.resolver.StoreResolverEntityFsStore;
import internal.org.springframework.content.rest.scoped.store.resolver.StoreResolverEntityJpaStore;
import internal.org.springframework.content.rest.scoped.store.resolver.StoreResolverEntityRepository;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.content.test.support.jpa.fixture.SingleContentJpaLongIdMimeEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = Application.class, webEnvironment = WebEnvironment.MOCK)
@ExtendWith(SpringExtension.class)
public class StoreResolverRestConfigurationIT {

    @Autowired
    private StoreResolverEntityRepository repo;
    @Autowired
    private StoreResolverEntityJpaStore jpaStore;
    @Autowired
    private StoreResolverEntityFsStore fsStore;
    @Autowired
    private WebApplicationContext webApplicationContext;

    private SingleContentJpaLongIdMimeEntity tEntity;

    @Nested
    class JpaRest {
        @Nested
        @DisplayName("Given that claim has existing content")
        class ClaimHasExistingContent {
            @BeforeEach
            void setUp() {
                RestAssuredMockMvc.webAppContextSetup(webApplicationContext);

                tEntity = new SingleContentJpaLongIdMimeEntity();
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
                    .post("/storeResolverEntities/" + tEntity.getId())
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
