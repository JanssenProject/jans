package org.gluu.oxd.server.op;

import com.google.inject.Injector;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.params.GetJwksParams;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.common.response.POJOResponse;
import org.gluu.oxd.server.HttpException;

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
