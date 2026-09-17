package org.springframework.content.commons.config;

import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.property.PropertyPath;

import java.io.Serializable;

public record ContentPropertyInfo<S, SID extends Serializable>(
        S entity,
        SID contentId,
        PropertyPath propertyPath,
        ContentProperty contentProperty) {
}
