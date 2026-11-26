package internal.org.springframework.content.commons.store.factory;

import org.jspecify.annotations.NonNull;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.GetResourceParams;
import org.springframework.content.commons.store.SetContentParams;
import org.springframework.content.commons.store.StoreEntityValidation;
import org.springframework.content.commons.store.UnsetContentParams;
import org.springframework.core.io.Resource;

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
            StoreEntityValidation.requireEntity(args[0]);
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length && i < args.length; i++) {
            Object arg = args[i];
            if (arg != null) {
                continue;
            }
            Class<?> parameterType = parameterTypes[i];
            if (parameterType == PropertyPath.class) {
                StoreEntityValidation.requirePropertyPath(null);
            } else if (parameterType == GetResourceParams.class) {
                StoreEntityValidation.requireGetResourceParams(null);
            } else if (parameterType == SetContentParams.class) {
                StoreEntityValidation.requireSetContentParams(null);
            } else if (parameterType == UnsetContentParams.class) {
                StoreEntityValidation.requireUnsetContentParams(null);
            } else if (parameterType == InputStream.class) {
                StoreEntityValidation.requireContentInputStream(null);
            } else if (parameterType == Resource.class) {
                StoreEntityValidation.requireContentResource(null);
            }
        }
        if ("associate".equals(method.getName()) && args[args.length - 1] == null) {
            StoreEntityValidation.requireContentId(null);
        }
    }

    private static boolean hasNonNullFirstParameter(Method method) {
        return method.getParameters()[0].isAnnotationPresent(NonNull.class);
    }
}
