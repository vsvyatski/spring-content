package it.internal.org.springframework.content.rest.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static java.lang.String.format;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class LastModifiedDate {

    private MockMvc mvc;
    private String url;
    private Date lastModifiedDate;
    private String etag;
    private String content;

    public void setMvc(MockMvc mvc) {
        this.mvc = mvc;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public void setEtag(String etag) {
        LastModifiedDate.this.etag = format("\"%s\"", etag);
    }

    public void setContent(String content) {
        this.content = content;
    }

    
    @Nested
    class AGETRequestToStoreIdWithNoHeaders {
        @Test
        void shouldReturnTheContentWithTheLastModifiedHeader() throws Exception {
            MockHttpServletResponse response = mvc
                    .perform(get(url)
                            .accept("text/plain"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse();

            assertThat(response).isNotNull();
            assertThat(response.getContentAsString()).isEqualTo(content);
            assertThat(isWithinASecond(lastModifiedDate).matches(response.getHeader("last-modified"))).isTrue();
        }

    }

    @Nested
    class AGETRequestToStoreIdWithAnIfModifiedSinceDateBeforeTheEntitySModifiedDate {
        @Test
        void shouldRespondWith200AndTheContent() throws Exception {
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            Calendar cal = Calendar.getInstance();
            cal.setTime(lastModifiedDate);
            cal.add(Calendar.DATE, -1);
            String ifModifiedSince = format.format(cal.getTime());

            MockHttpServletResponse response = mvc
                    .perform(get(url)
                            .accept("text/plain")
                            .header("if-modified-since", ifModifiedSince))
                    .andExpect(status().isOk())
                    .andReturn().getResponse();

            assertThat(response).isNotNull();
            assertThat(response.getContentAsString()).isEqualTo(content);
            assertThat(isWithinASecond(lastModifiedDate).matches(response.getHeader("last-modified"))).isTrue();
        }

    }

    @Nested
    class AGETRequestToStoreIdWithAnIfModifiedSinceDateTheSameAsTheEntitySModifiedDate {
        @Test
        void shouldRespondWith304NotModified() throws Exception {
            mvc.perform(get(url)
                    .accept("text/plain")
                    .header("if-modified-since", toHeaderDateFormat(lastModifiedDate)))
                    .andExpect(status().isNotModified())
                    .andExpect(content().string(""));
        }

    }

    @Nested
    class AGETRequestToStoreIdWithAnIfUnmodifiedSinceDateBeforeTheEntitySModifiedDate {
        @Test
        void shouldRespondWith412PreconditionFailed() throws Exception {
            mvc.perform(get(url)
                    .accept("text/plain")
                    .header("if-unmodified-since", toHeaderDateFormat(addDays(lastModifiedDate, -1))))
                    .andExpect(status().isPreconditionFailed())
                    .andReturn();
        }

    }

    @Nested
    class AGETRequestToStoreIdWithAnIfUnmodifiedSinceDateTheSameAsTheEntitySModifiedDate {
        @Test
        void shouldRespondWith200AndTheContent() throws Exception {
            mvc.perform(get(url)
                    .accept("text/plain")
                    .header("if-unmodified-since", isWithinASecond(lastModifiedDate)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(content));
        }

    }

    @Nested
    class APUTToStoreIdWithAnIfUnmodifiedSinceDateBeforeTheEntitySModifiedDate {
        @Test
        void shouldRespondWith412PreconditionFailed() throws Exception {
            mvc.perform(put(url)
                    .content("Hello Modified Spring Content World!")
                    .contentType("text/plain")
                    .header("if-unmodified-since", toHeaderDateFormat(addDays(lastModifiedDate, -1))))
                    .andExpect(status().isPreconditionFailed());
        }

    }

    @Nested
    class APUTToStoreIdWithAnIfUnmodifiedSinceDateTheSameAsTheEntitySModifiedDate {
        @Test
        void shouldUpdateTheContent() throws Exception {
            mvc.perform(put(url)
                    .content("Hello Modified Spring Content World!")
                    .contentType("text/plain")
                    .header("if-unmodified-since", toHeaderDateFormat(lastModifiedDate)))
                    .andExpect(status().isOk());
        }

    }

    @Nested
    class APUTToStoreIdWithAMatchingIfUnmodifiedSinceHeaderAndAMatchingIfNoneMatchHeader {
        @Test
        void shouldRespondWithA412PreconditionFailed() throws Exception {
            if (etag != null) {
                mvc.perform(put(url)
                        .content("Hello Modified Spring Content World!")
                        .contentType("text/plain")
                        .header("if-unmodified-since", toHeaderDateFormat(lastModifiedDate))
                        .header("if-none-match", etag))
                        .andExpect(status().isPreconditionFailed());
            }
        }

    }

    @Nested
    class ADELETEToStoreIdWithAnIfUnmodifiedSinceDateBeforeTheEntitySModifiedDate {
        @Test
        void shouldRespondWith412PreconditionFailed() throws Exception {
            mvc.perform(delete(url)
                    .header("if-unmodified-since", toHeaderDateFormat(addDays(lastModifiedDate, -1))))
                    .andExpect(status().isPreconditionFailed());
        }

    }

    @Nested
    class ADELETEToStoreIdWithAnIfUnmodifiedSinceDateTheSameAsTheEntitySModifiedDate {
        @Test
        void shouldUpdateTheContent() throws Exception {
            mvc.perform(delete(url)
                    .header("if-unmodified-since", toHeaderDateFormat(lastModifiedDate)))
                    .andExpect(status().isNoContent());
        }

    }

    public static Matcher<String> isWithinASecond(final Date expectedDate) {
        return new TypeSafeMatcher<String>() {

            @Override
            protected void describeMismatchSafely(String foo, Description description) {
                description.appendText("was ").appendValue(foo);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Date ").appendValue(expectedDate);
            }

            @Override
            protected boolean matchesSafely(String actualDate) {
                Instant instant = Instant.ofEpochMilli(expectedDate.getTime());
                LocalDateTime expectedDateTime = LocalDateTime.ofInstant(instant, ZoneId.of("GMT"));

                LocalDateTime actualDateTime = LocalDateTime.parse(actualDate, DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH));

                long diff = ChronoUnit.SECONDS.between(expectedDateTime, actualDateTime);
                return diff <= 1;
            }
        };
    }

    private static String toHeaderDateFormat(Date dt) {
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(dt);
    }

    private static Date addDays(Date dt, int n) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(dt);
        cal.add(Calendar.DATE, n);
        return cal.getTime();
    }
}
