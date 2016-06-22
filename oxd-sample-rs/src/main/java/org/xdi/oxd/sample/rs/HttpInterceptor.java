package org.xdi.oxd.sample.rs;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.log4j.Logger;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.interception.PreProcessInterceptor;
import org.xdi.oxd.client.CommandClient;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.RsCheckAccessParams;
import org.xdi.oxd.common.response.RsCheckAccessResponse;
import org.xdi.util.StringHelper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/06/2016
 */

@Provider
@ServerInterceptor
public class HttpInterceptor implements PreProcessInterceptor {

    private static final Logger LOG = Logger.getLogger(HttpInterceptor.class);

    public HttpInterceptor() {
    }

    @Override
    public ServerResponse preProcess(HttpRequest request, ResourceMethod method) throws WebApplicationException {
        String path = getPath(request);
        String httpMethod = request.getHttpMethod();
        String rpt = getRpt(request.getHttpHeaders());
        LOG.info("Request path: " + path + ", httpMethod: " + httpMethod + ", rpt: " + rpt);

        CommandClient client = null;
        try {
            final RsCheckAccessParams commandParams = new RsCheckAccessParams();
            commandParams.setOxdId(RsServlet.getOxdId());
            commandParams.setHttpMethod(httpMethod);
            commandParams.setPath(path);
            commandParams.setRpt(rpt);

            final RsCheckAccessResponse resp = client.send(new Command(CommandType.RS_PROTECT, commandParams)).dataAsResponse(RsCheckAccessResponse.class);
            Preconditions.checkNotNull(resp);

            if ("granted".equalsIgnoreCase(resp.getAccess())) {
                 return null; // grant access to resources
            } else if ("denied".equalsIgnoreCase(resp.getAccess())) {
                if (Strings.isNullOrEmpty(resp.getWwwAuthenticateHeader())) {
                    LOG.error("WWW-Authenticate is not returned from oxD Server.");
                }
                if (Strings.isNullOrEmpty(resp.getTicket())) {
                    LOG.error("Ticket is not returned from oxD Server.");
                }

                return (ServerResponse) Response.status(Response.Status.FORBIDDEN)
                                        .header("WWW-Authenticate", resp.getWwwAuthenticateHeader())
                                        .entity(resp.getTicket())
                                        .build();
            }

        } catch (Exception e) {
            LOG.error("Failed to check access to resource. Forbid access. " + e.getMessage(), e);
        } finally {
            CommandClient.closeQuietly(client);
        }

        return (ServerResponse) Response.status(Response.Status.FORBIDDEN).build();
    }

    private String getPath(HttpRequest request) {
        if (request.getUri() != null && request.getUri().getAbsolutePath() != null) {
            return request.getUri().getAbsolutePath().getPath();
        }
        return null;
    }

    public static String getRptFromAuthorization(String authorizationHeader) {
        if (StringHelper.isNotEmpty(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring("Bearer ".length());
        }
        return null;
    }

    public static String getRpt(HttpHeaders httpHeaders) {
        if (httpHeaders != null) {
            final List<String> authHeaders = httpHeaders.getRequestHeader("Authorization");
            if (authHeaders != null && !authHeaders.isEmpty()) {
                final String authorization = authHeaders.get(0);
                return getRptFromAuthorization(authorization);
            }
        }
        return "";
    }
}
