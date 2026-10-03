package internal.org.springframework.content.rest.it;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;

import internal.org.springframework.content.rest.support.TestEntity2;
import internal.org.springframework.content.rest.support.TestEntity2JpaStore;
import internal.org.springframework.content.rest.support.TestEntity2Repository;
import internal.org.springframework.content.rest.support.TestEntityChild;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import net.bytebuddy.utility.RandomString;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.web.context.WebApplicationContext;

import java.io.ByteArrayInputStream;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.when;

public abstract class AbstractRestIT {

    @Autowired
    private TestEntity2Repository claimRepo;

    @Autowired
    private TestEntity2JpaStore claimFormStore;

    @LocalServerPort
    int port;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private TestEntity2 existingClaim;

    
    @Nested
    class JpaRest {
        @Nested
        class SpringContentREST {
            @Nested
            class GivenAClaim {
                @BeforeEach
                void setUp() throws Throwable {
                    RestAssuredMockMvc.webAppContextSetup(webApplicationContext);

                    // delete any existing claim forms
                    Iterable<TestEntity2> existingClaims = claimRepo.findAll();
                    for (TestEntity2 existingClaim : existingClaims) {
                        if (existingClaim.getChild() != null) {
                            claimFormStore.unsetContent(existingClaim, PropertyPath.from("child"));
                        }
                    }

                    // and claims
                    for (TestEntity2 existingClaim : existingClaims) {
                        claimRepo.delete(existingClaim);
                    }

                    existingClaim = new TestEntity2();
                    claimRepo.save(existingClaim);

                }

                @Test
                void shouldBePOSTableWithNewContentWith201Created() throws Throwable {
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
                class GivenThatClaimHasExistingContent {
                    @BeforeEach
                    void setUp() throws Throwable {
                        RestAssuredMockMvc.webAppContextSetup(webApplicationContext);

                        // delete any existing claim forms
                        Iterable<TestEntity2> existingClaims = claimRepo.findAll();
                        for (TestEntity2 existingClaim : existingClaims) {
                            if (existingClaim.getChild() != null) {
                                claimFormStore.unsetContent(existingClaim, PropertyPath.from("child"));
                            }
                        }

                        // and claims
                        for (TestEntity2 existingClaim : existingClaims) {
                            claimRepo.delete(existingClaim);
                        }

                        existingClaim = new TestEntity2();
                        claimRepo.save(existingClaim);

                        existingClaim.setChild(new TestEntityChild());
                        existingClaim.getChild().setMimeType("text/plain");
                        claimFormStore.setContent(existingClaim, PropertyPath.from("child"), new ByteArrayInputStream("This is plain text content!".getBytes()));
                        claimRepo.save(existingClaim);

                    }

                    @Test
                    void shouldReturnTheContentWith200OK() throws Throwable {
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
                    void shouldBePOSTableWithNewContentWith201Created() throws Throwable {
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
                    void shouldBeDELETEableWith204NoContent() throws Throwable {
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

    protected String getId() {
        RandomString random  = new RandomString(5);
        return "/store-tests/" + random.nextString();
    }

    public static String getContextName(Class<?> configClass) {
        return configClass.getSimpleName().replaceAll("Config", "");
    }

}
