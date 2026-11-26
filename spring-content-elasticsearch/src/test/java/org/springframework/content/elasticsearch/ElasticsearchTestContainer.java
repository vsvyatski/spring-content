package org.springframework.content.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.HttpHost;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Elasticsearch {@link ElasticsearchIT} fixture aligned with Boot's {@code elasticsearch-java} client (9.x).
 *
 * @author Vladimir Svyatski
 */
public class ElasticsearchTestContainer extends ElasticsearchContainer {

    /**
     * Last argument to the official image {@code docker-entrypoint.sh} (see image {@code CMD}).
     */
    @SuppressWarnings("SpellCheckingInspection")
    private static final String ELASTICSEARCH_IMAGE_ENTRYPOINT = "eswrapper";

    private static final DockerImageName IMAGE = DockerImageName.parse(elasticsearchImage());

    private static String elasticsearchImage() {
        String image = System.getProperty("spring.content.test.elasticsearch.image");
        if (image == null) {
            image = System.getProperty("spring-content.test.elasticsearch.image",
                "docker.elastic.co/elasticsearch/elasticsearch:9.4.0");
        }
        return image;
    }

    @SuppressWarnings("resource")
    private ElasticsearchTestContainer() {
        super(IMAGE);
        withCommand(
            "/bin/bash",
            "-c",
            "if ! bin/elasticsearch-plugin list | grep -q ingest-attachment; then "
                + "bin/elasticsearch-plugin install --batch ingest-attachment; fi; "
                + "exec /bin/tini -s -- /usr/local/bin/docker-entrypoint.sh "
                + ELASTICSEARCH_IMAGE_ENTRYPOINT);
        start();
    }

    public static ElasticsearchClient client() {
        ElasticsearchTestContainer instance = Singleton.INSTANCE;
        String host = instance.getHost();
        int port = instance.getMappedPort(9200);
        BasicCredentialsProvider credentials = new BasicCredentialsProvider();
        credentials.setCredentials(
            new AuthScope(host, port),
            new UsernamePasswordCredentials("elastic", ELASTICSEARCH_DEFAULT_PASSWORD.toCharArray()));
        Rest5Client restClient = Rest5Client.builder(new HttpHost("https", host, port))
            .setSSLContext(instance.createSslContextFromCa())
            .setHttpClientConfigCallback(http -> http.setDefaultCredentialsProvider(credentials))
            .build();
        return new ElasticsearchClient(new Rest5ClientTransport(restClient, new JacksonJsonpMapper()));
    }

    public static String getUrl() {
        ElasticsearchTestContainer instance = Singleton.INSTANCE;
        return formatHttpsUrl(instance.getHost(), instance.getMappedPort(9200));
    }

    private static String formatHttpsUrl(String host, int port) {
        return String.format("https://%s:%d", host, port);
    }

    @SuppressWarnings("unused") // Serializable safe singleton usage
    protected ElasticsearchTestContainer readResolve() {
        return Singleton.INSTANCE;
    }

    private static class Singleton {
        private static final ElasticsearchTestContainer INSTANCE = open();

        private static ElasticsearchTestContainer open() {
            return new ElasticsearchTestContainer();
        }
    }
}
