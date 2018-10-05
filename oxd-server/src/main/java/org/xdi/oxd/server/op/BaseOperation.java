/*
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.codehaus.jackson.node.POJONode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.model.crypto.OxAuthCryptoProvider;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.ErrorResponseCode;
import org.xdi.oxd.common.ErrorResponseException;
import org.xdi.oxd.common.params.HasOxdIdParams;
import org.xdi.oxd.common.params.IParams;
import org.xdi.oxd.common.response.IOpResponse;
import org.xdi.oxd.server.Configuration;
import org.xdi.oxd.server.Convertor;
import org.xdi.oxd.server.service.*;

/**
 * Base abstract class for all operations.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/08/2013
 */

public abstract class BaseOperation<T extends IParams> implements IOperation<T> {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(BaseOperation.class);

    private final Command command;
    private final Injector injector;
    private final Class<T> parameterClass;
    private final T params;

    /**
     * Base constructor
     *
     * @param command command
     */
    protected BaseOperation(Command command, final Injector injector, Class<T> parameterClass) {
        this.injector = injector;
        this.command = command;
        this.parameterClass = parameterClass;
        this.params = Convertor.asParams(parameterClass, command);
    }

    @Override
    public Class<T> getParameterClass() {
        return parameterClass;
    }

    public T getParams() {
        return params;
    }

    /**
     * Gets injector.
     *
     * @return injector
     */
    public Injector getInjector() {
        return injector;
    }

    public HttpService getHttpService() {
        return getInstance(HttpService.class);
    }

    public IntrospectionService getIntrospectionService() {
        return getInstance(IntrospectionService.class);
    }

    public PublicOpKeyService getKeyService() {
        return getInstance(PublicOpKeyService.class);
    }

    public <T> T getInstance(Class<T> type) {
        return injector.getInstance(type);
    }

    public StateService getStateService() {
        return getInstance(StateService.class);
    }

    public DiscoveryService getDiscoveryService() {
        return getInstance(DiscoveryService.class);
    }

    public UmaTokenService getUmaTokenService() {
        return getInstance(UmaTokenService.class);
    }

    public RpService getRpService() {
        return getInstance(RpService.class);
    }

    public ConfigurationService getConfigurationService() {
        return getInstance(ConfigurationService.class);
    }

    public OxAuthCryptoProvider getCryptoProvider() throws Exception {
        Configuration conf = getConfigurationService().get();
        return new OxAuthCryptoProvider(conf.getCryptProviderKeyStorePath(), conf.getCryptProviderKeyStorePassword(), conf.getCryptProviderDnName());
    }

    public Rp getRp() {
        if (params instanceof HasOxdIdParams) {
            getValidationService().validate((HasOxdIdParams) params);
            HasOxdIdParams hasOxdId = (HasOxdIdParams) params;
            return getRpService().getRp(hasOxdId.getOxdId());
        }
        throw new ErrorResponseException(ErrorResponseCode.BAD_REQUEST_NO_OXD_ID);
    }

    public ValidationService getValidationService() {
        return getInstance(ValidationService.class);
    }

    /**
     * Returns command
     *
     * @return command
     */
    public Command getCommand() {
        return command;
    }

    /**
     * Ok response for operation
     *
     * @param p_data response
     * @return ok response with data
     */
    public CommandResponse okResponse(IOpResponse p_data) {
        if (p_data == null) {
            return CommandResponse.createInternalError();
        }
        return CommandResponse.ok().setData(new POJONode(p_data));
    }
}
