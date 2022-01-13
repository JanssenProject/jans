package io.jans.ca.server.op;

import com.google.inject.Injector;
import io.jans.ca.common.Command;
import io.jans.ca.common.params.GetJwksParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.POJOResponse;
import io.jans.ca.server.HttpException;

public class GetRpJwksOperation extends BaseOperation<GetJwksParams> {

    protected GetRpJwksOperation(Command command, Injector injector) {
        super(command, injector, GetJwksParams.class);
    }

    @Override
    public IOpResponse execute(GetJwksParams params) {

        try {
            return new POJOResponse(getKeyGeneratorService().getKeys());
        } catch (HttpException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
