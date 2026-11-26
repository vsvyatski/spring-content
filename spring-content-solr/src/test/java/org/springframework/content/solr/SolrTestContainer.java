package org.springframework.content.solr;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.testcontainers.solr.SolrContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

/**
 * Solr {@link SolrIT} fixture: official Apache Solr image with a pre-created {@code solr} collection.
 *
 * @author Vladimir Svyatski
 */
public class SolrTestContainer extends SolrContainer {

    private static final String COLLECTION = "solr";
    private static final String CONFIGSET = "sample_techproducts_configs";
    private static final DockerImageName IMAGE = DockerImageName.parse(solrImage());

    private static String solrImage() {
        String image = System.getProperty("spring.content.test.solr.image");
        if (image == null) {
            image = System.getProperty("spring-content.test.solr.image", "solr:9.7.0");
        }
        return image;
    }

    @SuppressWarnings("resource")
    private SolrTestContainer() {
        super(IMAGE);
        withZookeeper(true);
        start();
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        try {
            var result = execInContainer(
                "solr", "create",
                "-c", COLLECTION,
                "-d", CONFIGSET,
                "-shards", "1",
                "-replicationFactor", "1");
            if (result.getExitCode() != 0) {
                throw new IllegalStateException(
                    "Unable to create Solr collection:\n" + result.getStdout() + "\n" + result.getStderr());
            }
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Unable to create Solr collection", e);
        }
    }

    public static SolrClient getSolrClient() {
        SolrTestContainer instance = Singleton.INSTANCE;
        String solrUrl = String.format("http://%s:%d/solr/%s", instance.getHost(), instance.getSolrPort(), COLLECTION);
        return new HttpJdkSolrClient.Builder(solrUrl).build();
    }

    @SuppressWarnings("unused") // Serializable safe singleton usage
    protected SolrTestContainer readResolve() {
        return Singleton.INSTANCE;
    }

    private static class Singleton {
        private static final SolrTestContainer INSTANCE = open();

        private static SolrTestContainer open() {
            return new SolrTestContainer();
        }
    }
}
