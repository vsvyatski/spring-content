package internal.org.springframework.content.rest.support;

import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.fs.store.FileSystemContentStore;

import java.util.UUID;

public interface TestEntity8Store extends FileSystemContentStore<TestEntity8, UUID>, Renderable<TestEntity8> {
}
