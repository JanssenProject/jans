package io.jans.ca.server.op;

import com.google.common.base.Strings;
import io.jans.ca.common.Command;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.ExpiredObject;
import io.jans.ca.common.params.StringParam;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.POJOResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.service.RequestObjectService;
import io.jans.ca.server.service.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetRequestObjectOperation extends BaseOperation<StringParam> {
    private static final Logger LOG = LoggerFactory.getLogger(GetRequestObjectOperation.class);

    private RequestObjectService requestObjectService;

    public GetRequestObjectOperation(Command command, ServiceProvider serviceProvider) {
        super(command, serviceProvider, StringParam.class);
        this.requestObjectService = serviceProvider.getRequestObjectService();
    }

    @Override
    public IOpResponse execute(StringParam params) {

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
}
