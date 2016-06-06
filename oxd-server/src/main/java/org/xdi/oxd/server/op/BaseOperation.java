/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.codehaus.jackson.node.POJONode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.ErrorResponseCode;
import org.xdi.oxd.common.ErrorResponseException;
import org.xdi.oxd.common.params.HasOxdIdParams;
import org.xdi.oxd.common.params.IParams;
import org.xdi.oxd.common.response.IOpResponse;
import org.xdi.oxd.server.Convertor;
import org.xdi.oxd.server.service.DiscoveryService;
import org.xdi.oxd.server.service.HttpService;
import org.xdi.oxd.server.service.SiteConfiguration;
import org.xdi.oxd.server.service.SiteConfigurationService;
import org.xdi.oxd.server.service.UmaTokenService;
import org.xdi.oxd.server.service.ValidationService;

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

    private SiteConfiguration site;

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
        return injector.getInstance(HttpService.class);
    }

    public DiscoveryService getDiscoveryService() {
        return injector.getInstance(DiscoveryService.class);
    }

    public UmaTokenService getUmaTokenService() {
        return injector.getInstance(UmaTokenService.class);
    }

    public SiteConfigurationService getSiteService() {
        return getInjector().getInstance(SiteConfigurationService.class);
    }

    public SiteConfiguration getSite() {
        if (site != null) {
            return site;
        }
        if (params instanceof HasOxdIdParams) {
            getValidationService().validate((HasOxdIdParams) params);
            HasOxdIdParams hasOxdId = (HasOxdIdParams) params;
            site = getSiteService().getSite(hasOxdId.getOxdId());
            return site;
        }
        throw new ErrorResponseException(ErrorResponseCode.BAD_REQUEST_NO_OXD_ID);
    }

    public ValidationService getValidationService() {
        return getInjector().getInstance(ValidationService.class);
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
