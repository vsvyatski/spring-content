package internal.org.springframework.content.commons.store.factory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.AssociativeStore;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.Store;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class StoreEntityArgumentsTest {

    private static final Method setContentMethod =
        requireMethod(ContentStore.class, "setContent", Object.class, InputStream.class);
    private static final Method getResourceByIdMethod =
        requireMethod(Store.class, "getResource", Serializable.class);
    private static final Method getResourceEntityMethod =
        requireMethod(AssociativeStore.class, "getResource", Object.class, PropertyPath.class);

    @Test
    @DisplayName("setContent requires a non-null entity argument")
    void setContentRequiresEntity() {
        assertThat(StoreEntityArguments.requiresNonNullEntity(setContentMethod)).isTrue();
    }

    @Test
    @DisplayName("getResource by content id does not require entity validation")
    void getResourceByIdSkipsEntityValidation() {
        assertThat(StoreEntityArguments.requiresNonNullEntity(getResourceByIdMethod)).isFalse();
    }

    @Test
    @DisplayName("getResource by entity and property path requires a non-null entity argument")
    void getResourceByEntityRequiresEntity() {
        assertThat(StoreEntityArguments.requiresNonNullEntity(getResourceEntityMethod)).isTrue();
    }

    private static Method requireMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        Method method = ReflectionUtils.findMethod(type, name, parameterTypes);
        Assert.notNull(method, () -> "Method not found: " + type.getName() + "#" + name);
        return method;
    }
}
