package org.springframework.content.commons.io;

import org.springframework.core.io.Resource;

import java.io.Serializable;

public interface IdentifiableResource extends Resource {

    Serializable getId();

    void setId(Serializable id);
}
