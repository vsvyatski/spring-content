package org.springframework.content.elasticsearch;

import java.net.URI;

import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;

public class ElasticsearchTestContainer extends ElasticsearchContainer {

    private static final DockerImageName IMAGE_NAME = DockerImageName.parse("paulcwarren/elasticsearch").asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch");

    // 9200:9200 -p 9300:9300 -e "discovery.type=single-node"

    private ElasticsearchTestContainer() {
        super(IMAGE_NAME);
        start();

//        try {
//            ExecResult r = execInContainer("sh", "-c", "bin/elasticsearch-plugin install --batch ingest-attachment");
//            int i=0;
//        } catch (IOException | InterruptedException e) {
//            throw new RuntimeException("Failed to setup solr container", e);
//        }
    }

    public static ElasticsearchClient client() {
        Rest5Client restClient = Rest5Client.builder(URI.create(getUrl())).build();
        return new ElasticsearchClient(new Rest5ClientTransport(restClient, new JacksonJsonpMapper()));
    }

    public static String getUrl() {
        return String.format("http://%s:%d", Singleton.INSTANCE.getHost(), Singleton.INSTANCE.getMappedPort(9200));
    }

    @SuppressWarnings("unused") // Serializable safe singleton usage
    protected ElasticsearchTestContainer readResolve() {
        return Singleton.INSTANCE;
    }

    private static class Singleton {
        private static final ElasticsearchTestContainer INSTANCE = new ElasticsearchTestContainer();
    }
}
