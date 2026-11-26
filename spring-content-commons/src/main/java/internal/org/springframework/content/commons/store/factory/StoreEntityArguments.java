package internal.org.springframework.content.commons.store.factory;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.GetResourceParams;
import org.springframework.content.commons.store.SetContentParams;
import org.springframework.content.commons.store.StoreEntityValidation;
import org.springframework.content.commons.store.UnsetContentParams;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.lang.reflect.Method;

/**
 * Detects store methods whose arguments are validated at the proxy boundary.
 *
 * @author Vladimir Svyatski
 */
final class StoreEntityArguments {

    private StoreEntityArguments() {
    }

    static boolean requiresNonNullEntity(Method method) {
        if (method.getParameterCount() == 0) {
            return false;
        }
        if (hasNonNullFirstParameter(method)) {
            return true;
        }
        return switch (method.getName()) {
            case "setContent", "unsetContent", "getContent", "associate", "unassociate" -> true;
            case "getResource" -> method.getParameterCount() >= 2;
            default -> false;
        };
    }

    static void requireStoreArgumentsIfApplicable(Method method, Object[] args) {
        if (args.length == 0) {
            return;
        }
        if (args[0] == null && requiresNonNullEntity(method)) {
            throw new IllegalArgumentException(StoreEntityValidation.ENTITY_MUST_NOT_BE_NULL);
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length && i < args.length; i++) {
            if (args[i] != null) {
                continue;
            }
            String message = requiredArgumentMessage(parameterTypes[i]);
            if (message != null) {
                throw new IllegalArgumentException(message);
            }
        }
        if ("associate".equals(method.getName()) && args[args.length - 1] == null) {
            throw new IllegalArgumentException(StoreEntityValidation.CONTENT_ID_MUST_NOT_BE_NULL);
        }
    }

    private static @Nullable String requiredArgumentMessage(Class<?> parameterType) {
        if (parameterType == PropertyPath.class) {
            return StoreEntityValidation.PROPERTY_PATH_MUST_NOT_BE_NULL;
        }
        if (parameterType == GetResourceParams.class) {
            return StoreEntityValidation.GET_RESOURCE_PARAMS_MUST_NOT_BE_NULL;
        }
        if (parameterType == SetContentParams.class) {
            return StoreEntityValidation.SET_CONTENT_PARAMS_MUST_NOT_BE_NULL;
        }
        if (parameterType == UnsetContentParams.class) {
            return StoreEntityValidation.UNSET_CONTENT_PARAMS_MUST_NOT_BE_NULL;
        }
        if (parameterType == InputStream.class || parameterType == Flux.class) {
            return StoreEntityValidation.CONTENT_MUST_NOT_BE_NULL;
        }
        if (parameterType == Resource.class) {
            return StoreEntityValidation.CONTENT_MUST_NOT_BE_NULL;
        }
        return null;
    }

    private static boolean hasNonNullFirstParameter(Method method) {
        return method.getParameters()[0].isAnnotationPresent(NonNull.class);
    }
}
