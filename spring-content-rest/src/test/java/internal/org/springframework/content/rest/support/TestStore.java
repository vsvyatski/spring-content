package internal.org.springframework.content.rest.support;

import org.springframework.content.fs.store.FileSystemStore;
import org.springframework.content.rest.StoreRestResource;

@StoreRestResource(path = "testStore")
public interface TestStore extends FileSystemStore<String> {
}
