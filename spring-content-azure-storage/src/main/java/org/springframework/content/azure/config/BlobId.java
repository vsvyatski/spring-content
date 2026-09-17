package org.springframework.content.azure.config;

import java.io.Serializable;

public record BlobId(String bucket, String name) implements Serializable {
}
