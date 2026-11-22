package org.springframework.content.test.support.idtype;

import java.io.Serializable;

/**
 * Supplies content ids when {@link InMemoryTypeSupportContentStore} sets content.
 *
 * @author Vladimir Svyatski
 */
public interface TypeSupportIdGenerator<SID extends Serializable> {

    SID generateId();
}
