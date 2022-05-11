/*
  All rights reserved -- Copyright 2015 Gluu Inc.
 */
package io.jans.ca.server.op;

import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.ca.common.Command;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.params.HasRpIdParams;
import io.jans.ca.common.params.IParams;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.configuration.ApiAppConfiguration;
import io.jans.ca.server.configuration.model.Rp;
import io.jans.ca.server.service.ServiceProvider;
import io.jans.ca.server.service.ValidationService;
import io.jans.ca.server.utils.Convertor;

/**
 * Base abstract class for all operations.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/08/2013
 */

public abstract class BaseOperation<T extends IParams> implements IOperation<T> {

    private final Command command;
    private final Class<T> parameterClass;
    private final T params;

    private ServiceProvider serviceProvider;

    /**
     * Base constructor
     *
     * @param command command
     */
    protected BaseOperation(Command command, Class<T> parameterClass) {
        this.command = command;
        this.parameterClass = parameterClass;
        this.params = Convertor.asParams(parameterClass, command);
    }

    protected BaseOperation(Command command, ServiceProvider serviceProvider, Class<T> parameterClass) {
        this.command = command;
        this.parameterClass = parameterClass;
        this.params = Convertor.asParams(parameterClass, command);
        this.serviceProvider = serviceProvider;
    }

    public ValidationService getValidationService() {
        return serviceProvider.getValidationService();
    }

    @Override
    public Class<T> getParameterClass() {
        return parameterClass;
    }

    public T getParams() {
        return params;
    }


    public AuthCryptoProvider getCryptoProvider() throws Exception {
        ApiAppConfiguration conf = serviceProvider.getConfigurationService().find();
        return new AuthCryptoProvider(conf.getCryptProviderKeyStorePath(), conf.getCryptProviderKeyStorePassword(), conf.getCryptProviderDnName());
    }

    public Rp getRp() {
        if (params instanceof HasRpIdParams) {
            serviceProvider.getValidationService().validate((HasRpIdParams) params);
            HasRpIdParams hasRpId = (HasRpIdParams) params;
            return serviceProvider.getRpSyncService().getRp(hasRpId.getRpId());
        }
        throw new HttpException(ErrorResponseCode.BAD_REQUEST_NO_RP_ID);
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
