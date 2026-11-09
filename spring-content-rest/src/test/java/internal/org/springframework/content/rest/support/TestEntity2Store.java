package internal.org.springframework.content.rest.support;

import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.rest.StoreRestResource;

import java.util.UUID;

@StoreRestResource(path = "files")
public interface TestEntity2Store extends FileSystemContentStore<TestEntity2, UUID> {
}
