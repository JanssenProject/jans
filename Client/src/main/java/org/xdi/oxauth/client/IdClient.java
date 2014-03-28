package org.xdi.oxauth.client;

import org.xdi.oxauth.client.service.ClientFactory;
import org.xdi.oxauth.client.service.IdGenerationService;
import org.xdi.oxauth.model.common.Id;
import org.xdi.oxauth.model.common.IdType;

/**
 * Id endpoint client.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 27/06/2013
 */

public class IdClient {

    /**
     * Avoid instance creation
     */
    private IdClient() {
    }

    /**
     * Request id.
     *
     * @param p_url    url
     * @param p_prefix prefix
     * @param p_type   type
     * @return id
     */
    public static Id generateId(String p_url, String p_prefix, IdType p_type, String p_authorization) {
        return idService(p_url).generateId(p_prefix, p_type.getType(), p_authorization);
    }

    /**
     * Generates id with rpt.
     *
     * @param p_url id gen url
     * @param p_prefix id prefix
     * @param p_type id type
     * @param p_rpt rpt
     * @return generated id
     */
    public static Id generateIdWithRpt(String p_url, String p_prefix, IdType p_type, String p_rpt) {
        return generateId(p_url, p_prefix, p_type, "Bearer " + p_rpt);
    }


    /**
     * Id service.
     *
     * @param p_url url
     * @return id service
     */
    public static IdGenerationService idService(String p_url) {
        return ClientFactory.instance().createIdGenerationService(p_url);
    }
}
