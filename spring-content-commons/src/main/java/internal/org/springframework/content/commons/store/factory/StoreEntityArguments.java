package internal.org.springframework.content.commons.store.factory;

import org.jspecify.annotations.NonNull;
import org.springframework.content.commons.store.StoreEntityValidation;

import java.lang.reflect.Method;

/**
 * Detects store methods whose first parameter is a non-null entity argument.
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

    static void requireEntityArgumentIfApplicable(Method method, Object[] args) {
        if (args.length == 0 || args[0] != null) {
            return;
        }
        if (requiresNonNullEntity(method)) {
            StoreEntityValidation.requireEntity(args[0]);
        }
    }

    private static boolean hasNonNullFirstParameter(Method method) {
        return method.getParameters()[0].isAnnotationPresent(NonNull.class);
    }
}
