package org.springframework.content.solr;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

public class SolrTestContainer extends SolrContainer {

    private static final String CONNECTION_URL = "http://%s:%d/solr/solr";
    private static final DockerImageName IMAGE_NAME = DockerImageName.parse("paulcwarren/solr").asCompatibleSubstituteFor("solr");

    private SolrTestContainer() {
        super(IMAGE_NAME);
        start();

        try {
            execInContainer("sh", "-c", "solr create_collection -c solr -d /opt/solr/server/solr/configsets/_default/");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to setup solr container", e);
        }
    }

    public static SolrClient getSolrClient() {
        String solrUrl = String.format(
                CONNECTION_URL,
                Singleton.INSTANCE.getHost(),
                Singleton.INSTANCE.getMappedPort(SolrContainer.SOLR_PORT));

        // noinspection resource
        return new HttpJdkSolrClient.Builder(solrUrl).build();
    }

    @SuppressWarnings("unused") // Serializable safe singleton usage
    protected SolrTestContainer readResolve() {
        return Singleton.INSTANCE;
    }

    private static class Singleton {
        private static final SolrTestContainer INSTANCE = new SolrTestContainer();
    }
}
