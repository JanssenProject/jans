/*
  All rights reserved -- Copyright 2015 Gluu Inc.
 */
package io.jans.ca.server.op;

import com.google.inject.Injector;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.ca.common.Command;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.params.HasRpIdParams;
import io.jans.ca.common.params.IParams;
import io.jans.ca.server.Convertor;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.RpServerConfiguration;
import io.jans.ca.server.service.*;

/**
 * Base abstract class for all operations.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/08/2013
 *
 */

public abstract class BaseOperation<T extends IParams> implements IOperation<T> {

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

    public RequestObjectService getRequestObjectService() {
        return getInstance(RequestObjectService.class);
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

    public RpSyncService getRpSyncService() {
        return getInstance(RpSyncService.class);
    }

    public ConfigurationService getConfigurationService() {
        return getInstance(ConfigurationService.class);
    }

    public KeyGeneratorService getKeyGeneratorService() {
        return getInstance(KeyGeneratorService.class);
    }

    public AuthCryptoProvider getCryptoProvider() throws Exception {
        RpServerConfiguration conf = getConfigurationService().get();
        return new AuthCryptoProvider(conf.getCryptProviderKeyStorePath(), conf.getCryptProviderKeyStorePassword(), conf.getCryptProviderDnName());
    }

    public OpClientFactory getOpClientFactory() {
        return getInstance(OpClientFactory.class);
    }

    public Rp getRp() {
        if (params instanceof HasRpIdParams) {
            getValidationService().validate((HasRpIdParams) params);
            HasRpIdParams hasRpId = (HasRpIdParams) params;
            return getRpSyncService().getRp(hasRpId.getRpId());
        }
        throw new HttpException(ErrorResponseCode.BAD_REQUEST_NO_RP_ID);
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
}
