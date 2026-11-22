package it.rest.versioning;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.path.json.JsonPath;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.http.HttpStatus;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.rest.StoreRestResource;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.versions.LockingAndVersioningRepository;
import org.springframework.versions.jpa.config.JpaLockingAndVersioningConfig;
import org.springframework.versions.test.support.VersioningTestPackages;
import org.springframework.versions.test.support.jpa.VersionedJpaContentDocumentEntity;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {LockingAndVersioningRestIT.Application.class}, webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
public class LockingAndVersioningRestIT {

    @Autowired
    private VersionedDocumentAndVersioningRepository repo;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private VersionedJpaContentDocumentEntity doc;

    public interface VersionedDocumentAndVersioningRepository extends JpaRepository<VersionedJpaContentDocumentEntity, Long>,
        LockingAndVersioningRepository<VersionedJpaContentDocumentEntity> {
    }

    @StoreRestResource(path = "versionedDocumentsContent")
    @SuppressWarnings("unused") // classpath / store component scan
    public interface VersionedDocumentStore extends FileSystemContentStore<VersionedJpaContentDocumentEntity, UUID> {
    }

    @SpringBootApplication
    @EntityScan(basePackages = VersioningTestPackages.JPA)
    @EnableJpaRepositories(considerNestedRepositories = true,
        basePackages = {"it.rest.versioning", "org.springframework.versions"})
    @EnableTransactionManagement
    @EnableFileSystemStores(basePackages = "it.rest.versioning")
    @Import({JpaLockingAndVersioningConfig.class, RestConfiguration.class, SecurityConfiguration.class})
    public static class Application {

        public static void main(String[] args) {
            SpringApplication.run(Application.class, args);
        }
    }

    @Configuration
    @EnableWebSecurity
    public static class SecurityConfiguration {

        protected static String REALM = "SPRING_CONTENT";

        @Autowired
        public void configureGlobalSecurity(AuthenticationManagerBuilder auth) {
            // Enable if spring-doc apps supports user accounts in the future
            auth.inMemoryAuthentication()
                .withUser(User.withUsername("paul123").password("{noop}password").roles("USER"))
                .withUser(User.withUsername("john123").password("{noop}password").roles("USER"));
        }

        @Bean
        public AuthenticationEntryPoint getBasicAuthEntryPoint() {
            return new AuthenticationEntryPoint();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) {
            http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(c -> c.requestMatchers("/admin/**").hasRole("ADMIN"))
                .httpBasic(c -> c.realmName(REALM).authenticationEntryPoint(getBasicAuthEntryPoint()))
                .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

            return http.build();
        }

        @Bean
        public WebSecurityCustomizer webSecurityCustomizer() {
            return (web) -> web.ignoring().requestMatchers(HttpMethod.OPTIONS, "/**");
        }
    }

    public static class AuthenticationEntryPoint extends BasicAuthenticationEntryPoint {

        @Override
        public void commence(final @NonNull HttpServletRequest request, final HttpServletResponse response,
                             final AuthenticationException authException) throws IOException {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.addHeader("WWW-Authenticate", "Basic realm=" + getRealmName());

            PrintWriter writer = response.getWriter();
            writer.println("HTTP Status 401 : " + authException.getMessage());
        }

        @Override
        public void afterPropertiesSet() {
            setRealmName(SecurityConfiguration.REALM);
            super.afterPropertiesSet();
        }
    }

    @Nested
    class SpringContentRESTVersioning {
        @Nested
        class GivenAVersionableEntityWithContent {
            @BeforeEach
            void setUp() {
                RestAssuredMockMvc.webAppContextSetup(webApplicationContext);

                doc = new VersionedJpaContentDocumentEntity();
                doc.setData("John");
                doc = repo.save(doc);
            }

            @Test
            void shouldBeAbleToVersionAnEntityAndItsContent() {
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
                assertThat((Object) response.get("_embedded.versionedDocuments[0].successorId")).isEqualTo((int) (doc.getId() + 1));
                assertThat((Object) response.get("_embedded.versionedDocuments[1].version")).isEqualTo("1.1");
                assertThat((Object) response.get("_embedded.versionedDocuments[1].successorId")).isNull();
            }
        }
    }
}
