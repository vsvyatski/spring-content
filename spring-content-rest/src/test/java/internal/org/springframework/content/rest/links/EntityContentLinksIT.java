package internal.org.springframework.content.rest.links;

import internal.org.springframework.content.rest.support.JsonMappedContentEntity;
import internal.org.springframework.content.rest.support.JsonMappedContentEntityRepository;
import internal.org.springframework.content.rest.support.JsonMappedContentEntityStore;
import internal.org.springframework.content.rest.support.StoreConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.rest.config.HypermediaConfiguration;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.content.test.support.TestSupportData;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

@WebAppConfiguration
@ContextConfiguration(classes = {
    StoreConfig.class,
    DelegatingWebMvcConfiguration.class,
    RepositoryRestMvcConfiguration.class,
    RestConfiguration.class,
    HypermediaConfiguration.class})
@Transactional
@ActiveProfiles("store")
@ExtendWith(SpringExtension.class)
public class EntityContentLinksIT {

    @Autowired
    JsonMappedContentEntityRepository repository3;
    @Autowired
    JsonMappedContentEntityStore store3;

    @Autowired
    private WebApplicationContext context;

    @Nested
    @NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.INHERIT)
    class EntityLinks {
        @Nested
        @NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.INHERIT)
        @DisplayName("when entity links are enabled")
        class EntityLinksAreEnabledContentLinkTests extends ContentLinkTests {
            @BeforeEach
            void setUp() {
                MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
                JsonMappedContentEntity testEntity3 = new JsonMappedContentEntity();
                store3.setContent(testEntity3, TestSupportData.sampleTextStream());
                testEntity3 = repository3.save(testEntity3);
                this.setMvc(mvc);
                this.setUrl("/jsonMappedContentEntities/" + testEntity3.getId());
                this.setLinkRel("content");
                this.setExpectedLinkRegex(String.format("http://localhost/jsonMappedContentEntities/%s/content", testEntity3.getId()));
            }

            @Test
            void shouldProvideResponseWithContentLink() throws Exception {
                assertContentLinkInHalResponse();
            }
        }
    }
}
