package io.jans.ca.server.op;

import io.jans.ca.common.Command;
import io.jans.ca.common.CommandType;
import io.jans.ca.common.params.GetJwksParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.POJOResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.service.KeyGeneratorService;
import io.jans.ca.server.service.ServiceProvider;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@RequestScoped
@Named
public class GetRpJwksOperation extends TemplateOperation<GetJwksParams> {
    @Inject
    KeyGeneratorService keyGeneratorService;

    @Override
    public IOpResponse execute(GetJwksParams params, HttpServletRequest httpServletRequest) {

        try {
            return new POJOResponse(keyGeneratorService.getKeys());
        } catch (HttpException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Class<GetJwksParams> getParameterClass() {
        return GetJwksParams.class;
    }

    @Override
    public CommandType getCommandType() {
        return CommandType.GET_RP_JWKS;
    }
}
