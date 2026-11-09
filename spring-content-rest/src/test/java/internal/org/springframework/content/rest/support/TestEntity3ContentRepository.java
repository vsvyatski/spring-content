package internal.org.springframework.content.rest.support;

import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.rest.RestResource;

import java.io.InputStream;

@SuppressWarnings("unused") // classpath / store component scan
public interface TestEntity3ContentRepository extends FileSystemContentStore<TestEntity3, Long>, Renderable<TestEntity3> {

    @RestResource(exported = false)
    @Override
    TestEntity3 setContent(TestEntity3 property, InputStream content);
}
