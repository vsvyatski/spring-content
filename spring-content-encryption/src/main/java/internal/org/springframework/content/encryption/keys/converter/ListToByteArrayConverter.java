package internal.org.springframework.content.encryption.keys.converter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ListToByteArrayConverter implements ConditionalGenericConverter {
    private final ConversionService conversionService;

    public ListToByteArrayConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public boolean matches(@NonNull TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (targetType.getObjectType() != byte[].class) {
            return false;
        }
        if (!sourceType.isCollection()) {
            return false;
        }
        TypeDescriptor elementType = sourceType.getElementTypeDescriptor();
        if (elementType == null) {
            return false;
        }
        return conversionService.canConvert(elementType, targetType);
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Set.of(new ConvertiblePair(Collection.class, byte[].class));
    }

    @Override
    @Nullable
    public Object convert(@Nullable Object source, @NonNull TypeDescriptor sourceType, @NonNull TypeDescriptor targetType) {
        if (!(source instanceof Collection<?> collection)) {
            return null;
        }
        TypeDescriptor elementType = sourceType.getElementTypeDescriptor();
        if (elementType == null) {
            return null;
        }
        List<byte[]> converted = collection.stream()
            .map(item -> toByteArray(item, elementType, targetType))
            .toList();
        var convertedTotalSize = converted.stream().mapToInt(b -> b.length).sum();

        var bb = ByteBuffer.allocate(Character.BYTES + Integer.BYTES + converted.size() * Integer.BYTES + convertedTotalSize);
        bb.putChar('L'); // Marker
        bb.putInt(converted.size()); // Length of converted list
        for (var item : converted) {
            bb.putInt(item.length);
            bb.put(item);
        }

        return bb.array();
    }

    private byte[] toByteArray(Object item, TypeDescriptor sourceElementType, TypeDescriptor targetType) {
        Object converted = conversionService.convert(item, sourceElementType, targetType);
        if (!(converted instanceof byte[] bytes)) {
            throw new IllegalStateException(
                "Expected byte[] from conversion to " + targetType.getType().getName());
        }
        return bytes;
    }
}
