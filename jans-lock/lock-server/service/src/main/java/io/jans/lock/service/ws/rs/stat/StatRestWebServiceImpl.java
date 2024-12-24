package io.jans.lock.service.ws.rs.stat;

import static io.jans.as.model.util.Util.escapeLog;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import io.jans.as.model.config.Constants;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.error.StatErrorResponseType;
import io.jans.lock.service.stat.StatResponseService;
import io.jans.lock.util.ServerUtil;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.exporter.common.TextFormat;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Provides server with basic statistic
 *
 * @author Yuriy Movchan Date: 12/02/2024
 */
@Dependent
@Path("/internal/stat")
public class StatRestWebServiceImpl implements StatRestWebService {

    @Inject
    private Logger log;

    @Inject
    private StatResponseService statResponseService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private AppConfiguration appConfiguration;

    @Override
    public Response statGet(@HeaderParam("Authorization") String authorization,
                            @QueryParam("month") String months,
                            @QueryParam("start-month") String startMonth,
                            @QueryParam("end-month") String endMonth,
                            @QueryParam("format") String format) {
        return stat(authorization, months, startMonth, endMonth, format);
    }

    @Override
    public Response statPost(@HeaderParam("Authorization") String authorization,
                             @FormParam("month") String months,
                             @FormParam("start-month") String startMonth,
                             @FormParam("end-month") String endMonth,
                             @FormParam("format") String format) {
        return stat(authorization, months, startMonth, endMonth, format);
    }

	public static String createOpenMetricsResponse(StatResponse statResponse) throws IOException {
		Writer writer = new StringWriter();
		CollectorRegistry registry = new CollectorRegistry();

		final Counter usersCounter = Counter.build().name("monthly_active_users").labelNames(Constants.MONTH)
				.help("Monthly active users").register(registry);

		final Counter clientsCounter = Counter.build().name("monthly_active_clients").labelNames(Constants.MONTH)
				.help("Monthly active clients").register(registry);

		Map<String, Counter> counterMap = new HashMap<String, Counter>();
		for (Map.Entry<String, StatResponseItem> entry : statResponse.getResponse().entrySet()) {
			final String month = entry.getKey();
			final StatResponseItem item = entry.getValue();

			usersCounter.labels(month).inc(item.getMonthlyActiveUsers());

			clientsCounter.labels(month).inc(item.getMonthlyActiveClients());

			for (Map.Entry<String, Map<String, Long>> operationTypeEntry : item.getOperationsByType().entrySet()) {
				final String operationType = operationTypeEntry.getKey();
				final Map<String, Long> operationTypeMap = operationTypeEntry.getValue();

				for (Map.Entry<String, Long> operationEntry : operationTypeMap.entrySet()) {
					final String operation = operationEntry.getKey();

					Counter operationCounter;
					if (counterMap.containsKey(operationType)) {
						operationCounter = counterMap.get(operationType);
					} else {
						operationCounter = Counter.build()
								.name(operationType)
								.labelNames(Constants.MONTH, "decision")
								.help(operationType).register(registry);
						counterMap.put(operationType, operationCounter);
					}

					operationCounter.labels(month, operation).inc(getOperationCount(operationTypeMap, operation));
				}
			}
		}

		TextFormat.write004(writer, registry.metricFamilySamples());

		return writer.toString();
	}

    private static long getOperationCount(Map<String, Long> map, String key) {
        Long v = map.get(key);
        return v != null ? v : 0;
    }

	public Response stat(String authorization, String monthsParam, String startMonth, String endMonth, String format) {
		if (log.isDebugEnabled()) {
			log.debug("Attempting to request stat, month: {}, startMonth: {}, endMonth: {}, format: {}",
					escapeLog(monthsParam), escapeLog(startMonth), escapeLog(endMonth), escapeLog(format));
		}

		if (!appConfiguration.isStatEnabled()) {
			throw errorResponseFactory.createWebApplicationException(Response.Status.FORBIDDEN, StatErrorResponseType.ACCESS_DENIED, "Future stat is disabled on server.");
		}

        final Set<String> months = validateMonths(monthsParam, startMonth, endMonth);

		try {
			if (log.isTraceEnabled()) {
				log.trace("Recognized months: {}", escapeLog(months));
			}
			final StatResponse statResponse = statResponseService.buildResponse(months);

			final String responseAsStr;
			if ("openmetrics".equalsIgnoreCase(format)) {
				responseAsStr = createOpenMetricsResponse(statResponse);
			} else if ("jsonmonth".equalsIgnoreCase(format)) {
				responseAsStr = ServerUtil.asJson(statResponse);
			} else {
				responseAsStr = ServerUtil.asJson(new FlatStatResponse(new ArrayList<>(statResponse.getResponse().values())));
			}

			if (log.isTraceEnabled()) {
				log.trace("Stat: {}", responseAsStr);
			}
			return Response.ok().entity(responseAsStr).build();
		} catch (WebApplicationException e) {
			if (log.isTraceEnabled()) {
				log.trace(e.getMessage(), e);
			}
			throw e;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON_TYPE).build();
		}
	}

    private Set<String> validateMonths(String months, String startMonth, String endMonth) {
        if (!Months.isValid(months, startMonth, endMonth)) {
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, StatErrorResponseType.INVALID_REQUEST, "`month` or `start-month`/`end-month` parameter(s) can't be blank and should be in format yyyyMM (e.g. 202012)");
        }

        months = ServerUtil.urlDecode(months);

        Set<String> monthList = Months.getMonths(months, startMonth, endMonth);

        if (monthList.isEmpty()) {
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, StatErrorResponseType.INVALID_REQUEST, "Unable to identify months. Check `month` or `start-month`/`end-month` parameter(s). It can't be blank and should be in format yyyyMM (e.g. 202012). start-month must be before end-month");
        }

        return monthList;
    }

}
