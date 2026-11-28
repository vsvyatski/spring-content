package internal.org.springframework.content.encryption.keys.converter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class ByteArrayToListConverter implements ConditionalGenericConverter {
    private final ConversionService conversionService;

    public ByteArrayToListConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, @NonNull TypeDescriptor targetType) {
        if (sourceType.getObjectType() != byte[].class) {
            return false;
        }
        if (!targetType.isCollection()) {
            return false;
        }
        TypeDescriptor elementType = targetType.getElementTypeDescriptor();
        if (elementType == null) {
            return false;
        }
        return conversionService.canConvert(sourceType, elementType);
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Set.of(new ConvertiblePair(byte[].class, Collection.class));
    }

    @Override
    @Nullable
    public Object convert(@Nullable Object source, @NonNull TypeDescriptor sourceType, @NonNull TypeDescriptor targetType) {
        if (source == null) {
            return null;
        }
        TypeDescriptor elementType = targetType.getElementTypeDescriptor();
        if (elementType == null) {
            return null;
        }
        var list = new ArrayList<>();
        var bb = ByteBuffer.wrap((byte[]) source);

        if (bb.getChar() != 'L') { // Marker check
            return null;
        }

        int length = bb.getInt();

        for (int i = 0; i < length; i++) {
            var itemSize = bb.getInt();
            var item = new byte[itemSize];
            bb.get(item);
            list.add(conversionService.convert(item, elementType));
        }
        return list;
    }
}
