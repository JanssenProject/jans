package io.jans.ca.server.op;

import io.jans.ca.common.Command;
import io.jans.ca.common.params.GetJwksParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.POJOResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.service.KeyGeneratorService;
import io.jans.ca.server.service.ServiceProvider;

public class GetRpJwksOperation extends BaseOperation<GetJwksParams> {

    private KeyGeneratorService keyGeneratorService;

    public GetRpJwksOperation(Command command, ServiceProvider serviceProvider) {
        super(command, serviceProvider, GetJwksParams.class);
        this.keyGeneratorService = serviceProvider.getKeyGeneratorService();
    }

    @Override
    public IOpResponse execute(GetJwksParams params) {

        try {
            return new POJOResponse(keyGeneratorService.getKeys());
        } catch (HttpException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
