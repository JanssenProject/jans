package org.xdi.oxd.server.service;

import com.google.common.base.Strings;
import org.xdi.oxd.common.ErrorResponseCode;
import org.xdi.oxd.common.ErrorResponseException;
import org.xdi.oxd.common.params.HasOxdIdParams;
import org.xdi.oxd.common.params.IParams;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/05/2016
 */

public class ValidationService {

    //private static final Logger LOG = LoggerFactory.getLogger(ValidationService.class);

    public void notNull(IParams params) {
        if (params == null) {
            throw new ErrorResponseException(ErrorResponseCode.INTERNAL_ERROR_NO_PARAMS);
        }
    }

    public void notBlankOxdId(String oxdId) {
        if (Strings.isNullOrEmpty(oxdId)) {
            throw new ErrorResponseException(ErrorResponseCode.BAD_REQUEST_NO_OXD_ID);
        }
    }

    public void notBlankOpHost(String opHost) {
        if (Strings.isNullOrEmpty(opHost)) {
            throw new ErrorResponseException(ErrorResponseCode.INVALID_OP_HOST);
        }
    }

    public void validate(IParams params) {
        notNull(params);
        if (params instanceof HasOxdIdParams) {
            validate((HasOxdIdParams) params);
        }
    }

    public void validate(HasOxdIdParams params) {
        notNull(params);
        notBlankOxdId(params.getOxdId());
    }

    public SiteConfiguration validate(SiteConfiguration site) {
        if (site == null) {
            throw new ErrorResponseException(ErrorResponseCode.INVALID_OXD_ID);
        }

        notBlankOxdId(site.getOxdId());
        notBlankOpHost(site.getOpHost());
        return site;
    }
}
