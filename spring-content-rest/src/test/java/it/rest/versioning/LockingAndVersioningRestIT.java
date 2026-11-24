package it.rest.versioning;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.path.json.JsonPath;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.rest.StoreRestResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.versions.LockingAndVersioningRepository;
import org.springframework.versions.test.support.jpa.VersionedJpaContentDocumentEntity;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
public class LockingAndVersioningRestIT {

    @Autowired
    private VersionedDocumentAndVersioningRepository repo;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private VersionedJpaContentDocumentEntity doc;

    public interface VersionedDocumentAndVersioningRepository extends
        JpaRepository<VersionedJpaContentDocumentEntity, Long>,
        LockingAndVersioningRepository<VersionedJpaContentDocumentEntity> {
    }

    @StoreRestResource(path = "versionedDocumentsContent")
    @SuppressWarnings("unused") // classpath / store component scan
    public interface VersionedDocumentStore extends FileSystemContentStore<VersionedJpaContentDocumentEntity, UUID> {
    }

    @Nested
    class SpringContentRESTVersioning {
        @Nested
        @DisplayName("Given a versionable entity with content")
        class VersionableEntityWithContent {
            @BeforeEach
            void setUp() {
                RestAssuredMockMvc.webAppContextSetup(webApplicationContext);

                doc = new VersionedJpaContentDocumentEntity();
                doc.setData("John");
                doc = repo.save(doc);
            }

            @Test
            void shouldBeAbleToVersionEntityAndItsContent() {
                // assert content does not exist
                given()
                    .auth().with(SecurityMockMvcRequestPostProcessors.user("paul123").password("password"))
                    .when()
                    .get("/versionedDocumentsContent/" + doc.getId())
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_NOT_FOUND);

                String newContent = "This is some new content";

                // POST the new content
                given()
                    .contentType("plain/text")
                    .body(newContent.getBytes())
                    .auth().with(SecurityMockMvcRequestPostProcessors.user("paul123").password("password"))
                    .when()
                    .put("/versionedDocumentsContent/" + doc.getId())
                    .then()
                    .statusCode(HttpStatus.SC_CREATED);

                // assert that it now exists
                var response1 = given()
                    .auth().with(SecurityMockMvcRequestPostProcessors.user("paul123").password("password"))
                    .header("accept", "plain/text")
                    .get("/versionedDocumentsContent/" + doc.getId())
                    .then()
                    .statusCode(HttpStatus.SC_OK)
                    .extract().response();
                assertThat(response1.getContentType()).startsWith("plain/text");
                assertThat(response1.asString()).isEqualTo(newContent);

                given()
                    .auth().with(SecurityMockMvcRequestPostProcessors.user("paul123").password("password"))
                    .header("accept", "application/json")
                    .put("/versionedDocuments/" + doc.getId() + "/lock")
                    .then()
                    .statusCode(HttpStatus.SC_OK);

                // POST the new content as john
                given()
                    .auth().with(SecurityMockMvcRequestPostProcessors.user("john123").password("password"))
                    .contentType("plain/text")
                    .body("john's content".getBytes())
                    .when()
                    .put("/versionedDocumentsContent/" + doc.getId())
                    .then()
                    .statusCode(409);

                JsonPath response =
                    given()
                        .auth().with(SecurityMockMvcRequestPostProcessors.user("paul123").password("password"))
                        .contentType("application/json")
                        .body("{\"number\":\"1.1\",\"label\":\"some minor changes\"}".getBytes())
                        .put("/versionedDocuments/" + doc.getId() + "/version")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .extract().jsonPath();
                assertThat((String) response.get("_links.self.href")).endsWith("versionedDocuments/" + (doc.getId() + 1));

                response =
                    given()
                        .auth().with(SecurityMockMvcRequestPostProcessors.user("paul123").password("password"))
                        .get("/versionedDocuments/findAllVersionsLatest")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .extract().jsonPath();
                assertThat((Object) response.get("_embedded.versionedDocuments[0].version")).isEqualTo("1.1");
                assertThat((Object) response.get("_embedded.versionedDocuments[0].successorId")).isNull();

                response =
                    given()
                        .auth().with(SecurityMockMvcRequestPostProcessors.user("paul123").password("password"))
                        .get("/versionedDocuments/" + (doc.getId() + 1) + "/findAllVersions")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .extract().jsonPath();
                assertThat((Object) response.get("_embedded.versionedDocuments[0].version")).isEqualTo("1.0");
                assertThat((Object) response.get("_embedded.versionedDocuments[0].successorId"))
                    .isEqualTo((int) (doc.getId() + 1));
                assertThat((Object) response.get("_embedded.versionedDocuments[1].version")).isEqualTo("1.1");
                assertThat((Object) response.get("_embedded.versionedDocuments[1].successorId")).isNull();
            }
        }
    }
}
