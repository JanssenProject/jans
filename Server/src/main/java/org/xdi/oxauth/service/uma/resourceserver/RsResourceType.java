/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.uma.resourceserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/07/2013
 */

public enum RsResourceType {
    ID_GENERATION("internal_generate_id");

    private final String m_value;
    private final List<RsScopeType> m_scopeTypes;

    private RsResourceType(String p_value, RsScopeType... p_types) {
        m_value = p_value;
        if (p_types != null) {
            m_scopeTypes = new ArrayList<RsScopeType>(Arrays.asList(p_types));
        } else {
            m_scopeTypes = Collections.emptyList();
        }
    }

    public String getValue() {
        return m_value;
    }

    public List<RsScopeType> getScopeTypes() {
        return m_scopeTypes;
    }
}
