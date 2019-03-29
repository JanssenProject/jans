/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.common;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/03/2013
 */

public class Holder<T> {

    private T m_t;

    public Holder() {
    }

    public Holder(T p_t) {
        m_t = p_t;
    }

    public T getT() {
        return m_t;
    }

    public void setT(T p_t) {
        m_t = p_t;
    }
}
