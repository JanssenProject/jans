package io.jans.as.server.token.ws.rs;

import io.jans.as.model.common.ExchangeTokenType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.util.ServerUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * @author Yuriy Z
 */
@Stateless
@Named
public class TxTokenService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ApplicationAuditLogger applicationAuditLogger;

    public boolean isTxTokenFlow(HttpServletRequest httpRequest) {
        return isTxTokenFlow(httpRequest.getParameter("requested_token_type"));
    }

    public boolean isTxTokenFlow(String requestedTokenType) {
        final ExchangeTokenType exchangeTokenType = ExchangeTokenType.fromString(requestedTokenType);
        return exchangeTokenType == ExchangeTokenType.TX_TOKEN;
    }

    public Response processTxToken(ExecutionContext executionContext) {
        final JSONObject responseJson = process(executionContext);
        final String entity = responseJson.toString();
        return response(Response.ok().entity(entity), executionContext.getAuditLog());
    }

    private JSONObject process(ExecutionContext executionContext) {
        final String requestedTokenType = executionContext.getHttpRequest().getParameter("requested_token_type");
        final String subjectToken = executionContext.getHttpRequest().getParameter("subject_token");
        final String subjectTokenType = executionContext.getHttpRequest().getParameter("subject_token_type");
        final String audience = executionContext.getHttpRequest().getParameter("audience");
        final String requestContext = executionContext.getHttpRequest().getParameter("rctx");

        JSONObject responseJson = new JSONObject();
        return responseJson;
    }

    private Response response(Response.ResponseBuilder builder, OAuth2AuditLog oAuth2AuditLog) {
        builder.cacheControl(ServerUtil.cacheControl(true, false));
        builder.header("Pragma", "no-cache");

        applicationAuditLogger.sendMessage(oAuth2AuditLog);

        return builder.build();
    }
}
