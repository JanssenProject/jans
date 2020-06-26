package org.gluu.oxd.server.op;

import com.google.inject.Injector;
import io.dropwizard.util.Strings;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.common.ExpiredObject;
import org.gluu.oxd.common.params.GetRequestObjectJwtParams;
import org.gluu.oxd.common.response.GetRequestObjectJwtResponse;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.server.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetRequestObjectJwtOperation extends BaseOperation<GetRequestObjectJwtParams> {
    private static final Logger LOG = LoggerFactory.getLogger(GetRequestUriOperation.class);

    protected GetRequestObjectJwtOperation(Command command, Injector injector) {
        super(command, injector, GetRequestObjectJwtParams.class);
    }

    @Override
    public IOpResponse execute(GetRequestObjectJwtParams params) {

        try {
            ExpiredObject expiredObject = getStateService().getRequestObject(params.getRequestObjectId());

            if (expiredObject == null || Strings.isNullOrEmpty(expiredObject.getValue())) {
                LOG.error("Request Object not found. The `request_uri` has either expired or it does not exist.");
                throw new HttpException(ErrorResponseCode.REQUEST_OBJECT_NOT_FOUND);
            }

            final GetRequestObjectJwtResponse response = new GetRequestObjectJwtResponse();
            response.setRequestObject(expiredObject.getValue());
            return response;
        } catch (HttpException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error in fetching request object. The `request_uri` has either expired or it does not exist.", e);
            throw new HttpException(ErrorResponseCode.REQUEST_OBJECT_NOT_FOUND);
        }

    }
}
