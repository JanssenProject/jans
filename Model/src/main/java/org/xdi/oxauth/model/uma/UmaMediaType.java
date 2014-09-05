/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.uma;

/**
 * Holds uma specific media types.
 *
 * yuriyz 01/04/2013 : as it was emailed by Eve:
 * We've been removing all the specialized content type extensions,
 * and just sticking with application/json. I'll add an issue on our side
 * to update the specs to remove those last few instances of application/xxx+json.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 28/03/2013
 */

public enum UmaMediaType {
    JSON,
    RESOURCE_SET,
    RESOURCE_SET_STATUS,
    REQUESTED_PERMISSION,
    PERMISSION_TICKET,
    RPT_STATUS;

    public static final String JSON_VALUE = "application/json";
    public static final String RESOURCE_SET_VALUE = "application/uma-resource-set+json";
    public static final String RESOURCE_SET_STATUS_VALUE = "application/uma-status+json";
    public static final String REQUESTED_PERMISSION_VALUE = "application/uma-requested-permission+json";
    public static final String PERMISSION_TICKET_VALUE = "application/uma-permission-ticket+json";
    public static final String RPT_STATUS_VALUE = "application/uma-rpt-status+json";

    // use static initializer instead of constructor to avoid illegal forwarding
    static {
        JSON.m_value = JSON_VALUE;
        RESOURCE_SET.m_value = RESOURCE_SET_VALUE;
        RESOURCE_SET_STATUS.m_value = RESOURCE_SET_STATUS_VALUE;
        REQUESTED_PERMISSION.m_value = REQUESTED_PERMISSION_VALUE;
        PERMISSION_TICKET.m_value = PERMISSION_TICKET_VALUE;
        RPT_STATUS.m_value = RPT_STATUS_VALUE;
    }

    private String m_value;

    public String getValue() {
        return m_value;
    }
}
