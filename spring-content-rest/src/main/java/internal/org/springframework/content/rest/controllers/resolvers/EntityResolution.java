package internal.org.springframework.content.rest.controllers.resolvers;

import org.springframework.content.commons.property.PropertyPath;

public record EntityResolution(Object entity, PropertyPath property) {
}
