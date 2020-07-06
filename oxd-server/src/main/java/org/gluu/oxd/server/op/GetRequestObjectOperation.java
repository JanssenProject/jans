package org.gluu.oxd.server.op;

import com.google.inject.Injector;
import io.dropwizard.util.Strings;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.common.ExpiredObject;
import org.gluu.oxd.common.params.StringParam;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.common.response.POJOResponse;
import org.gluu.oxd.server.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetRequestObjectOperation extends BaseOperation<StringParam> {
    private static final Logger LOG = LoggerFactory.getLogger(GetRequestObjectOperation.class);

    protected GetRequestObjectOperation(Command command, Injector injector) {
        super(command, injector, StringParam.class);
    }

    @Override
    public IOpResponse execute(StringParam params) {

        try {
            ExpiredObject expiredObject = getRequestObjectService().get(params.getValue());

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
