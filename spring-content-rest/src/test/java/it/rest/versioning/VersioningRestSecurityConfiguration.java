package it.rest.versioning;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Basic-auth users for locking/versioning REST integration tests.
 *
 * @author Vladimir Svyatski
 */
@Configuration
@EnableWebSecurity
public class VersioningRestSecurityConfiguration {

    static final String REALM = "SPRING_CONTENT";

    @Autowired
    public void configureGlobalSecurity(AuthenticationManagerBuilder auth) {
        auth.inMemoryAuthentication()
            .withUser(User.withUsername("paul123").password("{noop}password").roles("USER"))
            .withUser(User.withUsername("john123").password("{noop}password").roles("USER"));
    }

    @Bean
    public VersioningRestAuthenticationEntryPoint versioningRestAuthenticationEntryPoint() {
        return new VersioningRestAuthenticationEntryPoint();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(c -> c.requestMatchers("/admin/**").hasRole("ADMIN"))
            .httpBasic(c -> c.realmName(REALM).authenticationEntryPoint(versioningRestAuthenticationEntryPoint()))
            .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(HttpMethod.OPTIONS, "/**");
    }

    public static class VersioningRestAuthenticationEntryPoint extends BasicAuthenticationEntryPoint {

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
            setRealmName(REALM);
            super.afterPropertiesSet();
        }
    }
}
