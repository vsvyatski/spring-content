package org.springframework.content.mongo.boot;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.MongoDBContainer;

public class MongoTestContainer extends MongoDBContainer {

    private static final String DOCKER_IMAGE_NAME = "mongo:4.4.30";

    private MongoTestContainer() {
        super(DOCKER_IMAGE_NAME);
        start();
    }

    // Strip the db name from the end of the replicaSetUrl
    public static String getTestDbName() {
        return StringUtils.substringAfterLast(INSTANCE.getReplicaSetUrl(), "/");
    }

    public static String getTestDbUrl() {
        return StringUtils.substringBeforeLast(INSTANCE.getReplicaSetUrl(), "/");
    }

    public static MongoClient getMongoClient() {
        // noinspection resource
        return MongoClients.create(getTestDbUrl());
    }

    @SuppressWarnings("unused") // Serializable safe singleton usage
    protected MongoTestContainer readResolve() {
        return INSTANCE;
    }

    private static final MongoTestContainer INSTANCE = new MongoTestContainer();
}
