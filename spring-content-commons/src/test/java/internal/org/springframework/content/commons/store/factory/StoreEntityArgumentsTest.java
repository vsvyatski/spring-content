package internal.org.springframework.content.commons.store.factory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.AssociativeStore;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.GetResourceParams;
import org.springframework.content.commons.store.ReactiveContentStore;
import org.springframework.content.commons.store.Store;
import org.springframework.content.commons.store.StoreEntityValidation;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;

import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoreEntityArgumentsTest {

    private static final Method setContentMethod =
        requireMethod(ContentStore.class, "setContent", Object.class, InputStream.class);
    private static final Method reactiveSetContentMethod = requireMethod(ReactiveContentStore.class, "setContent",
        Object.class, PropertyPath.class, long.class, Flux.class);
    private static final Method getResourceByIdMethod =
        requireMethod(Store.class, "getResource", Serializable.class);
    private static final Method getResourceEntityMethod =
        requireMethod(AssociativeStore.class, "getResource", Object.class, PropertyPath.class);
    private static final Method getResourceWithParamsMethod = requireMethod(AssociativeStore.class, "getResource",
        Object.class, PropertyPath.class, GetResourceParams.class);
    private static final Method associateMethod =
        requireMethod(AssociativeStore.class, "associate", Object.class, Serializable.class);
    private static final Method associateWithPathMethod = requireMethod(AssociativeStore.class, "associate",
        Object.class, PropertyPath.class, Serializable.class);

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

    @Test
    @DisplayName("requireStoreArguments rejects null propertyPath on path-bearing overloads")
    void rejectsNullPropertyPath() {
        Object entity = new Object();
        assertThatThrownBy(() -> StoreEntityArguments.requireStoreArgumentsIfApplicable(
            getResourceEntityMethod, new Object[] { entity, null }))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(StoreEntityValidation.PROPERTY_PATH_MUST_NOT_BE_NULL);
    }

    @Test
    @DisplayName("requireStoreArguments rejects null content on setContent")
    void rejectsNullContentOnSetContent() {
        Object entity = new Object();
        assertThatThrownBy(() -> StoreEntityArguments.requireStoreArgumentsIfApplicable(
            setContentMethod, new Object[] { entity, null }))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(StoreEntityValidation.CONTENT_MUST_NOT_BE_NULL);
    }

    @Test
    @DisplayName("requireStoreArguments accepts non-null content on setContent")
    void acceptsNonNullContentOnSetContent() {
        Object entity = new Object();
        StoreEntityArguments.requireStoreArgumentsIfApplicable(
            setContentMethod, new Object[] { entity, new ByteArrayInputStream(new byte[] { 1 }) });
    }

    @Test
    @DisplayName("requireStoreArguments rejects null buffer on reactive setContent")
    void rejectsNullContentOnReactiveSetContent() {
        Object entity = new Object();
        PropertyPath path = PropertyPath.from("content");
        assertThatThrownBy(() -> StoreEntityArguments.requireStoreArgumentsIfApplicable(
            reactiveSetContentMethod, new Object[] { entity, path, 0L, null }))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(StoreEntityValidation.CONTENT_MUST_NOT_BE_NULL);
    }

    @Test
    @DisplayName("requireStoreArguments rejects null content id on associate")
    void rejectsNullContentIdOnAssociate() {
        Object entity = new Object();
        assertThatThrownBy(() -> StoreEntityArguments.requireStoreArgumentsIfApplicable(
            associateMethod, new Object[] { entity, null }))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(StoreEntityValidation.CONTENT_ID_MUST_NOT_BE_NULL);
    }

    @Test
    @DisplayName("requireStoreArguments rejects null content id on property-path associate")
    void rejectsNullContentIdOnPropertyPathAssociate() {
        Object entity = new Object();
        PropertyPath path = PropertyPath.from("content");
        assertThatThrownBy(() -> StoreEntityArguments.requireStoreArgumentsIfApplicable(
            associateWithPathMethod, new Object[] { entity, path, null }))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(StoreEntityValidation.CONTENT_ID_MUST_NOT_BE_NULL);
    }

    @Test
    @DisplayName("requireStoreArguments rejects null getResourceParams on three-arg getResource")
    void rejectsNullGetResourceParams() {
        Object entity = new Object();
        PropertyPath path = PropertyPath.from("content");
        assertThatThrownBy(() -> StoreEntityArguments.requireStoreArgumentsIfApplicable(
            getResourceWithParamsMethod, new Object[] { entity, path, null }))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(StoreEntityValidation.GET_RESOURCE_PARAMS_MUST_NOT_BE_NULL);
    }

    private static Method requireMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        Method method = ReflectionUtils.findMethod(type, name, parameterTypes);
        Assert.notNull(method, () -> "Method not found: " + type.getName() + "#" + name);
        return method;
    }
}
