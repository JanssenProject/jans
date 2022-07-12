package io.jans.ca.server.op;

import com.google.common.base.Strings;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.ExpiredObject;
import io.jans.ca.common.params.StringParam;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.POJOResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.service.RequestObjectService;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetRequestObjectOperation extends BaseOperation<StringParam> {
    private static final Logger LOG = LoggerFactory.getLogger(GetRequestObjectOperation.class);
    @Inject
    RequestObjectService requestObjectService;

    @Override
    public IOpResponse execute(StringParam params, HttpServletRequest httpServletRequest) {

        try {
            ExpiredObject expiredObject = requestObjectService.get(params.getValue());

            if (expiredObject == null || Strings.isNullOrEmpty(expiredObject.getValue())) {
                LOG.error("Request Object not found. The `request_uri` has either expired or it does not exist.");
                throw new HttpException(ErrorResponseCode.REQUEST_OBJECT_NOT_FOUND);
            }

            return new POJOResponse(expiredObject.getValue());
        } catch (HttpException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error in fetching request object. The `request_uri` has either expired or it does not exist.", e);
            throw new HttpException(ErrorResponseCode.REQUEST_OBJECT_NOT_FOUND);
        }

    }

    @Override
    public Class<StringParam> getParameterClass() {
        return StringParam.class;
    }

    @Override
    public boolean isAuthorizationRequired() {
        return false;
    }

    @Override
    public String getReturnType() {
        return MediaType.TEXT_PLAIN;
    }

}
