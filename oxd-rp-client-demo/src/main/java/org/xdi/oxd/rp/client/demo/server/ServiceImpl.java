package org.xdi.oxd.rp.client.demo.server;

import com.google.common.base.Preconditions;
import com.google.gwt.thirdparty.guava.common.base.Strings;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import org.apache.log4j.Logger;
import org.xdi.oxd.common.response.CheckIdTokenResponse;
import org.xdi.oxd.rp.client.HrefDetails;
import org.xdi.oxd.rp.client.RpClient;
import org.xdi.oxd.rp.client.RpClientFactory;
import org.xdi.oxd.rp.client.RpClientUtils;
import org.xdi.oxd.rp.client.demo.client.Service;
import org.xdi.oxd.rp.client.demo.shared.TokenDetails;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 19/10/2015
 */

public class ServiceImpl extends RemoteServiceServlet implements Service {

    private static final Logger LOG = Logger.getLogger(ServiceImpl.class);

    private RpClient rpClient;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        try {
            final Configuration c = Configuration.getInstance();
            Preconditions.checkNotNull(c);
            Preconditions.checkState(notEmpty(c.getOxdHost()), "oxd server host can not be empty. Please correct 'oxd_host' in oxd-rp-demo.json configuration file.");
            Preconditions.checkState(c.getOxdPort() > 0, "oxd server port can not be empty or less then zero. Please correct 'oxd_port' in oxd-rp-demo.json configuration file.");
            Preconditions.checkState(notEmpty(c.getSitePublicUrl()), "Site public url can not be blank. Please correct 'site_public_url' in oxd-rp-demo.json configuration file.");

            // register site
            rpClient = RpClientFactory.newSocketClient(c.getOxdHost(), c.getOxdPort());
            rpClient.register(c.getSitePublicUrl());

            // make sure oxd id is not blank
            Preconditions.checkState(notEmpty(rpClient.getOxdId()));
            LOG.info("Demo Servlet is started successfully.");
            LOG.info("Site is registered at oxD Server, oxd id : " + rpClient.getOxdId());
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw e;
        }
    }

    private static boolean notEmpty(String s) {
        return !Strings.isNullOrEmpty(s);
    }

    @Override
    public String getAuthorizationUrl() {
        return rpClient.getAuthorizationUrl();
    }

    @Override
    public TokenDetails getTokenDetails(String href) {
        final HrefDetails hrefDetails = RpClientUtils.parseHref(href);

        if (hrefDetails.hasIdToken()) {
            CheckIdTokenResponse validationResponse = rpClient.validateIdToken(hrefDetails.getIdToken());

            TokenDetails tokenDetails = new TokenDetails();
            if (validationResponse.isActive()) {
                tokenDetails.setAccessToken(hrefDetails.getAccessToken());
                tokenDetails.setIdToken(hrefDetails.getIdToken());
                tokenDetails.setCode(hrefDetails.getCode());
                tokenDetails.setClaims(validationResponse.getClaims());
            }
            return tokenDetails;
        }

        // for now unsupported operation, but it's as easy as request tokens by code
        throw new UnsupportedOperationException();
    }

    @Override
    public void destroy() {
        super.destroy();
        RpClientFactory.close(rpClient);
    }
}