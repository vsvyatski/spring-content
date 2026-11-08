package it.internal.org.springframework.content.rest.controllers;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class Cors {

	private MockMvc mvc;
	private String url;

	public static Cors tests(){
		return new Cors();
	}

	public MockMvc getMvc() {
		return mvc;
	}

	public void setMvc(MockMvc mvc) {
		this.mvc = mvc;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

    @Nested
    class AnOPTIONSRequestFromAKnownHost {
        @Test
        void shouldReturnTheRelevantCORSHeadersAndOK() throws Exception {
            mvc.perform(options(url)
            						.header("Access-Control-Request-Method", "DELETE")
            						.header("Origin", "http://www.someurl.com"))
            						.andExpect(status().isOk())
            						.andExpect(header().string("Access-Control-Allow-Origin","http://www.someurl.com"));
        }
    }
    @Nested
    class AnOPTIONSRequestFromAnUnknownHost {
        @Test
        void shouldBeForbidden() throws Exception {
            mvc.perform(options(url)
            						.header("Access-Control-Request-Method", "DELETE")
            						.header("Origin", "http://www.someotherurl.com"))
            						.andExpect(status().isForbidden());
        }
    }

}
