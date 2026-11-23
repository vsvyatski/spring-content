package it.internal.org.springframework.content.rest.controllers.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CorsTests {

    private MockMvc mvc;
    private String url;

    public static CorsTests tests() {
        return new CorsTests();
    }

    public MockMvc getMvc() {
        return mvc;
    }

    public void setMvc(MockMvc mvc) {
        this.mvc = mvc;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Nested
    @DisplayName("An OPTIONS request from a known host")
    class OptionsRequestFromKnownHost {
        @Test
        void shouldReturnTheRelevantCORSHeadersAndOK() throws Exception {
            mvc.perform(options(url)
                    .header("Access-Control-Request-Method", "DELETE")
                    .header("Origin", "http://www.someurl.com"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://www.someurl.com"));
        }
    }

    @Nested
    @DisplayName("An OPTIONS request from an unknown host")
    class OptionsRequestFromUnknownHost {
        @Test
        void shouldBeForbidden() throws Exception {
            mvc.perform(options(url)
                    .header("Access-Control-Request-Method", "DELETE")
                    .header("Origin", "http://www.someotherurl.com"))
                .andExpect(status().isForbidden());
        }
    }
}
