package it.internal.org.springframework.content.rest.controllers;

import internal.org.springframework.content.rest.support.FullyQualifiedLinksConfig;
import internal.org.springframework.content.rest.support.UuidSingleContentEntity;
import internal.org.springframework.content.rest.support.UuidSingleContentEntityRepository;
import internal.org.springframework.content.rest.support.UuidSingleContentEntityStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

@WebAppConfiguration
@ContextConfiguration(classes = {
    FullyQualifiedLinksConfig.class,
    DelegatingWebMvcConfiguration.class,
    RepositoryRestMvcConfiguration.class,
    RestConfiguration.class})
@Transactional
@ExtendWith(SpringExtension.class)
public class FullyQualifiedLinksIT {

    @Autowired
    private UuidSingleContentEntityRepository repo3;

    @Autowired
    private UuidSingleContentEntityStore store3;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    private UuidSingleContentEntity testEntity3;

    private Content contentTests;

    @Nested
    class ContextPathContentTests {
        @Nested
        @DisplayName("given an entity is the subject of a repository and storage")
        class GivenAnEntityIsTheSubjectOfARepositoryAndStorageContent extends Content {
            @BeforeEach
            void setUp() {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();
                testEntity3 = repo3.save(new UuidSingleContentEntity());
                testEntity3 = repo3.save(testEntity3);
                this.setMvc(mvc);
                this.setUrl("/testEntitiesContent/" + testEntity3.getId() + "/content");
                this.setEntity(testEntity3);
                this.setRepository(repo3);
                this.setStore(store3);
            }
        }
    }
}
