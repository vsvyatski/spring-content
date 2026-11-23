package internal.org.springframework.content.rest.it;

import internal.org.springframework.content.rest.support.AuditedNestedContentChild;
import internal.org.springframework.content.rest.support.AuditedNestedContentEntity;
import internal.org.springframework.content.rest.support.AuditedNestedContentEntityJpaStore;
import internal.org.springframework.content.rest.support.AuditedNestedContentEntityRepository;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.web.context.WebApplicationContext;

import java.io.ByteArrayInputStream;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.when;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractRestIT {

    @Autowired
    private AuditedNestedContentEntityRepository claimRepo;
    @Autowired
    private AuditedNestedContentEntityJpaStore claimFormStore;
    @Autowired
    private WebApplicationContext webApplicationContext;

    private AuditedNestedContentEntity existingClaim;

    @Nested
   class JpaRest {
        @Nested
        class SpringContentREST {
            @Nested
            @DisplayName("Given a claim")
            class Claim {
                @BeforeEach
                void setUp() {
                    RestAssuredMockMvc.webAppContextSetup(webApplicationContext);

                    // delete any existing claim forms
                    Iterable<AuditedNestedContentEntity> existingClaims = claimRepo.findAll();
                    for (AuditedNestedContentEntity existingClaim : existingClaims) {
                        if (existingClaim.getChild() != null) {
                            claimFormStore.unsetContent(existingClaim, PropertyPath.from("child"));
                        }
                    }

                    // and claims
                    for (AuditedNestedContentEntity existingClaim : existingClaims) {
                        claimRepo.delete(existingClaim);
                    }

                    existingClaim = new AuditedNestedContentEntity();
                    claimRepo.save(existingClaim);
                }

                @Test
                void shouldBePOSTableWithNewContentWith201Created() {
                    // assert content does not exist
                    when()
                        .get("/files/" + existingClaim.getId() + "/child")
                        .then()
                        .assertThat()
                        .statusCode(HttpStatus.SC_NOT_FOUND);

                    String newContent = "This is some new content";

                    // POST the new content
                    given()
                        .contentType("text/plain")
                        .body(newContent.getBytes())
                        .when()
                        .post("/files/" + existingClaim.getId() + "/child")
                        .then()
                        .statusCode(HttpStatus.SC_CREATED);

                    // assert that it now exists
                    var response1 = given()
                        .header("accept", "text/plain")
                        .get("/files/" + existingClaim.getId() + "/child")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .extract().response();
                    assertThat(response1.getContentType()).startsWith("text/plain");
                    assertThat(response1.asString()).isEqualTo(newContent);
                }

                @Nested

                @DisplayName("Given that claim has existing content")

                class ClaimHasExistingContent {
                    @BeforeEach
                    void setUp() {
                        RestAssuredMockMvc.webAppContextSetup(webApplicationContext);

                        // delete any existing claim forms
                        Iterable<AuditedNestedContentEntity> existingClaims = claimRepo.findAll();
                        for (AuditedNestedContentEntity existingClaim : existingClaims) {
                            if (existingClaim.getChild() != null) {
                                claimFormStore.unsetContent(existingClaim, PropertyPath.from("child"));
                            }
                        }

                        // and claims
                        for (AuditedNestedContentEntity existingClaim : existingClaims) {
                            claimRepo.delete(existingClaim);
                        }

                        existingClaim = new AuditedNestedContentEntity();
                        claimRepo.save(existingClaim);

                        existingClaim.setChild(new AuditedNestedContentChild());
                        existingClaim.getChild().setMimeType("text/plain");
                        claimFormStore.setContent(existingClaim, PropertyPath.from("child"), new ByteArrayInputStream("This is plain text content!".getBytes()));
                        claimRepo.save(existingClaim);
                    }

                    @Test
                    void shouldReturnTheContentWith200OK() {
                        var response2 = given()
                            .header("accept", "text/plain")
                            .get("/files/" + existingClaim.getId() + "/child")
                            .then()
                            .statusCode(HttpStatus.SC_OK)
                            .extract().response();
                        assertThat(response2.getContentType()).startsWith("text/plain");
                        assertThat(response2.asString()).isEqualTo("This is plain text content!");
                    }

                    @Test
                    void shouldBePOSTableWithNewContentWith201Created() {
                        String newContent = "This is new content";

                        given()
                            .contentType("text/plain")
                            .body(newContent.getBytes())
                            .when()
                            .post("/files/" + existingClaim.getId() + "/child")
                            .then()
                            .statusCode(HttpStatus.SC_OK);

                        var response3 = given()
                            .header("accept", "text/plain")
                            .get("/files/" + existingClaim.getId() + "/child")
                            .then()
                            .statusCode(HttpStatus.SC_OK)
                            .extract().response();
                        assertThat(response3.getContentType()).startsWith("text/plain");
                        assertThat(response3.asString()).isEqualTo(newContent);
                    }

                    @Test
                    void shouldBeDeletableWith204NoContent() {
                        given()
                            .delete("/files/" + existingClaim.getId() + "/child")
                            .then()
                            .assertThat()
                            .statusCode(HttpStatus.SC_NO_CONTENT);

                        // and make sure that it is really gone
                        when()
                            .get("/files/" + existingClaim.getId() + "/child")
                            .then()
                            .assertThat()
                            .statusCode(HttpStatus.SC_NOT_FOUND);
                    }
                }
            }
        }
    }
}
