package internal.org.springframework.content.rest.it;

import internal.org.springframework.content.rest.support.AuditedNestedContentEntity;
import internal.org.springframework.content.rest.support.AuditedNestedContentEntityJpaStore;
import internal.org.springframework.content.rest.support.AuditedNestedContentEntityRepository;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.http.MediaType;
import org.springframework.web.context.WebApplicationContext;

import java.io.ByteArrayInputStream;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.when;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shared Spring Content REST scenarios for dialect {@code *RestIT} classes.
 * <p>
 * {@link org.springframework.boot.test.context.SpringBootTest} belongs on each concrete dialect IT, not here: nested
 * test classes would not inherit it from a subclass ({@code H2RestIT} is a supertype, not a JUnit enclosing class).
 *
 * @author Vladimir Svyatski
 */
public abstract class AbstractRestIT {

    @Autowired
    private AuditedNestedContentEntityRepository claimRepo;
    @Autowired
    private AuditedNestedContentEntityJpaStore claimFormStore;
    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUpRestAssured() {
        RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
    }

    private void deleteAllClaims() {
        Iterable<AuditedNestedContentEntity> existingClaims = claimRepo.findAll();
        for (AuditedNestedContentEntity existingClaim : existingClaims) {
            if (existingClaim.getChild() != null) {
                claimFormStore.unsetContent(existingClaim, PropertyPath.from("child"));
            }
        }
        for (AuditedNestedContentEntity existingClaim : existingClaims) {
            claimRepo.delete(existingClaim);
        }
    }

    private AuditedNestedContentEntity newClaim() {
        deleteAllClaims();
        AuditedNestedContentEntity claim = new AuditedNestedContentEntity();
        claimRepo.save(claim);
        return claim;
    }

    @Test
    void shouldBePostableWithNewContentWith201Created() {
        AuditedNestedContentEntity existingClaim = newClaim();

        when()
            .get("/files/" + existingClaim.getId() + "/child")
            .then()
            .assertThat()
            .statusCode(HttpStatus.SC_NOT_FOUND);

        String newContent = "This is some new content";

        given()
            .contentType(MediaType.TEXT_PLAIN_VALUE)
            .body(newContent.getBytes())
            .when()
            .post("/files/" + existingClaim.getId() + "/child")
            .then()
            .statusCode(HttpStatus.SC_CREATED);

        var response1 = given()
            .header("accept", MediaType.TEXT_PLAIN_VALUE)
            .get("/files/" + existingClaim.getId() + "/child")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().response();
        assertThat(response1.getContentType()).startsWith(MediaType.TEXT_PLAIN_VALUE);
        assertThat(response1.asString()).isEqualTo(newContent);
    }

    @Test
    @DisplayName("Given that claim has existing content, should return content with 200 OK")
    void shouldReturnContentWith200OK() {
        AuditedNestedContentEntity existingClaim = newClaim();
        existingClaim.getChild().setMimeType(MediaType.TEXT_PLAIN_VALUE);
        claimFormStore.setContent(
            existingClaim,
            PropertyPath.from("child"),
            new ByteArrayInputStream("This is plain text content!".getBytes()));
        claimRepo.save(existingClaim);

        var response2 = given()
            .header("accept", MediaType.TEXT_PLAIN_VALUE)
            .get("/files/" + existingClaim.getId() + "/child")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().response();
        assertThat(response2.getContentType()).startsWith(MediaType.TEXT_PLAIN_VALUE);
        assertThat(response2.asString()).isEqualTo("This is plain text content!");
    }

    @Test
    @DisplayName("Given that claim has existing content, should be postable with new content with 200 OK")
    void shouldBePostableWithNewContentWith200OkWhenContentExists() {
        AuditedNestedContentEntity existingClaim = newClaim();
        existingClaim.getChild().setMimeType(MediaType.TEXT_PLAIN_VALUE);
        claimFormStore.setContent(
            existingClaim,
            PropertyPath.from("child"),
            new ByteArrayInputStream("This is plain text content!".getBytes()));
        claimRepo.save(existingClaim);

        String newContent = "This is new content";

        given()
            .contentType(MediaType.TEXT_PLAIN_VALUE)
            .body(newContent.getBytes())
            .when()
            .post("/files/" + existingClaim.getId() + "/child")
            .then()
            .statusCode(HttpStatus.SC_OK);

        var response3 = given()
            .header("accept", MediaType.TEXT_PLAIN_VALUE)
            .get("/files/" + existingClaim.getId() + "/child")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .extract().response();
        assertThat(response3.getContentType()).startsWith(MediaType.TEXT_PLAIN_VALUE);
        assertThat(response3.asString()).isEqualTo(newContent);
    }

    @Test
    @DisplayName("Given that claim has existing content, should be deletable with 204 No Content")
    void shouldBeDeletableWith204NoContent() {
        AuditedNestedContentEntity existingClaim = newClaim();
        existingClaim.getChild().setMimeType(MediaType.TEXT_PLAIN_VALUE);
        claimFormStore.setContent(
            existingClaim,
            PropertyPath.from("child"),
            new ByteArrayInputStream("This is plain text content!".getBytes()));
        claimRepo.save(existingClaim);

        given()
            .delete("/files/" + existingClaim.getId() + "/child")
            .then()
            .assertThat()
            .statusCode(HttpStatus.SC_NO_CONTENT);

        when()
            .get("/files/" + existingClaim.getId() + "/child")
            .then()
            .assertThat()
            .statusCode(HttpStatus.SC_NOT_FOUND);
    }
}
