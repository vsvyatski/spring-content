package internal.org.springframework.content.rest.storedrenditions;

import internal.org.springframework.content.rest.support.DualContentRenditionEntity;
import internal.org.springframework.content.rest.support.DualContentRenditionEntityRepository;
import internal.org.springframework.content.rest.support.DualContentRenditionEntityStore;
import internal.org.springframework.content.rest.support.StoreConfig;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.HandleAfterSetContent;
import org.springframework.content.commons.annotations.HandleBeforeUnsetContent;
import org.springframework.content.commons.annotations.StoreEventHandler;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.commons.store.events.AfterSetContentEvent;
import org.springframework.content.commons.store.events.BeforeUnsetContentEvent;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebAppConfiguration
@ContextConfiguration(classes = {StoredRenditionsRestIT.StoredRenditionsConfig.class, StoreConfig.class, DelegatingWebMvcConfiguration.class, RepositoryRestMvcConfiguration.class, RestConfiguration.class})
@Transactional
@ActiveProfiles("store")
@ExtendWith(SpringExtension.class)
public class StoredRenditionsRestIT {

    @Autowired
    private DualContentRenditionEntityRepository repo;
    @Autowired
    private DualContentRenditionEntityStore store;

    private DualContentRenditionEntity testEntity5;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @Configuration
    public static class StoredRenditionsConfig {

        @Bean
        public TestEventHandler eventHandler() {
            return new TestEventHandler();
        }

        // a dynamic renderer helps prove the stored rendition was returned
        @Bean
        public RenditionProvider textToHtml() {
            return new RenditionProvider() {

                @Override
                public String consumes() {
                    return "text/plain";
                }

                @Override
                public String[] produces() {
                    return new String[]{"text/html"};
                }

                @Override
                public InputStream convert(InputStream fromInputSource, String toMimeType) {
                    String input;
                    try {
                        input = IOUtils.toString(fromInputSource, StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        input = "";
                    }
                    return new ByteArrayInputStream(
                        String.format("\"<html><head><title>Dynamic Rendition</title></head><body>%s</body></html>\"", input).getBytes());
                }
            };
        }
    }

    @StoreEventHandler
    public static class TestEventHandler {

        @Autowired
        private DualContentRenditionEntityStore store;

        @HandleAfterSetContent
        @SuppressWarnings("unused") // store event dispatch
        public void onAfterSetContent(AfterSetContentEvent event) throws IOException {

            DualContentRenditionEntity entity = (DualContentRenditionEntity) event.getSource();

            long renderedLength;
            try (InputStream inputStream = store.getContent(entity, PropertyPath.from("content"))) {

                String renderedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

                UUID resourceId = UUID.randomUUID();
                store.associate(entity, PropertyPath.from("rendition"), resourceId);

                try (OutputStream stream = ((WritableResource) store.getResource(entity, PropertyPath.from("rendition"))).getOutputStream()) {
                    renderedLength = IOUtils.copyLarge(
                        new ByteArrayInputStream(String.format("<html><head><title>Stored Rendition</title></head><body>%s</body></html>", renderedContent).getBytes()),
                        stream);
                }
            }

            entity.setRenditionLen(renderedLength);
            entity.setRenditionMimeType("text/html");
        }

        @HandleBeforeUnsetContent
        @SuppressWarnings("unused") // store event dispatch
        public void onBeforeUnsetContent(BeforeUnsetContentEvent event) throws IOException {

            DualContentRenditionEntity entity = (DualContentRenditionEntity) event.getSource();

            ((DeletableResource) store.getResource(entity, PropertyPath.from("rendition"))).delete();

            entity.setRenditionId(null);
            entity.setRenditionLen(0L);
            entity.setRenditionMimeType(null);
        }
    }

    @Nested
    class StoredRenditions {
        @Nested
        @DisplayName("Given an entity with a content property")
        class EntityWithContentProperty {
            @Nested
            @DisplayName("PUT to repository id content property")
            class PutToRepositoryIdContentProperty {
                @BeforeEach
                void setUp() throws Exception {
                    mvc = MockMvcBuilders.webAppContextSetup(context).build();

                    testEntity5 = repo.save(new DualContentRenditionEntity());

                    mvc.perform(put("/dualContentRenditionEntities/" + testEntity5.getId() + "/content").content("foo").contentType("text/plain")).andExpect(status().is2xxSuccessful());
                }

                @Test
                void shouldStoreRendition() throws IOException {
                    testEntity5 = repo.findById(testEntity5.getId()).orElseThrow();
                    Resource r = store.getResource(testEntity5, PropertyPath.from("content"));
                    try (InputStream actual = r.getInputStream()) {
                        assertThat(IOUtils.toString(actual, StandardCharsets.UTF_8)).isEqualTo("foo");
                    }

                    r = store.getResource(testEntity5, PropertyPath.from("rendition"));
                    try (InputStream actual = r.getInputStream()) {
                        assertThat(IOUtils.toString(actual, StandardCharsets.UTF_8)).isEqualTo("<html><head><title>Stored Rendition</title></head><body>foo</body></html>");
                    }
                }

                @Nested
                @DisplayName("GET to repository id content property")
                class GetToRepositoryIdContentProperty {
                    @BeforeEach
                    void setUp() throws Exception {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity5 = repo.save(new DualContentRenditionEntity());

                        mvc.perform(put("/dualContentRenditionEntities/" + testEntity5.getId() + "/content").content("foo").contentType("text/plain")).andExpect(status().is2xxSuccessful());
                    }

                    @Test
                    void shouldGetStoredRendition() throws Exception {
                        mvc.perform(
                                get("/dualContentRenditionEntities/" + testEntity5.getId() + "/content")
                                    .accept("text/html"))
                            .andExpect(status().isOk())
                            .andExpect(content().string(not("<html><head><title>Dynamic Rendition</title></head><body>foo</body></html>")))
                            .andExpect(content().string("<html><head><title>Stored Rendition</title></head><body>foo</body></html>"));
                    }
                }

                @Nested
                @DisplayName("GET to repository id rendition property")
                class GetToRepositoryIdRenditionProperty {
                    @BeforeEach
                    void setUp() throws Exception {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity5 = repo.save(new DualContentRenditionEntity());

                        mvc.perform(put("/dualContentRenditionEntities/" + testEntity5.getId() + "/content").content("foo").contentType("text/plain")).andExpect(status().is2xxSuccessful());
                    }

                    @Test
                    void shouldReturnA405() throws Exception {
                        mvc.perform(
                                get("/dualContentRenditionEntities/" + testEntity5.getId() + "/rendition")
                                    .accept("text/html"))
                            .andExpect(status().isMethodNotAllowed());
                    }
                }

                @Nested
                @DisplayName("DELETE to repository id content property")
                class DeleteToRepositoryIdContentProperty {
                    @BeforeEach
                    void setUp() throws Exception {
                        mvc = MockMvcBuilders.webAppContextSetup(context).build();

                        testEntity5 = repo.save(new DualContentRenditionEntity());

                        mvc.perform(put("/dualContentRenditionEntities/" + testEntity5.getId() + "/content").content("foo").contentType("text/plain")).andExpect(status().is2xxSuccessful());
                    }

                    @Test
                    void shouldAlsoDeleteRendition() throws Exception {
                        Long id = testEntity5.getId();

                        mvc.perform(delete("/dualContentRenditionEntities/" + testEntity5.getId() + "/content")).andExpect(status().isNoContent());

                        Optional<DualContentRenditionEntity> fetched = repo.findById(id);
                        assertThat(fetched.isPresent()).isTrue();
                        assertThat(fetched.get().getContentId()).isNull();
                        assertThat(fetched.get().getContentLen()).isNull();
                        assertThat(fetched.get().getContentMimeType()).isNull();

                        assertThat(fetched.get().getRenditionId()).isNull();
                        assertThat(fetched.get().getRenditionLen()).isEqualTo(0L);
                        assertThat(fetched.get().getRenditionMimeType()).isNull();
                    }
                }
            }
        }
    }
}
