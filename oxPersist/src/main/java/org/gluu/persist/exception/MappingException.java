/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.exception;

/**
 * An exception is a result of something screwy in the O-R mappings.
 */
public class MappingException extends BasePersistenceException {

    private static final long serialVersionUID = 1113352885909511209L;

    public MappingException(String msg, Throwable root) {
        super(msg, root);
    }

    public MappingException(Throwable root) {
        super(root);
    }

    public MappingException(String s) {
        super(s);
    }

}
