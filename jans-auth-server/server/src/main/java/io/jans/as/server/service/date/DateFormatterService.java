package io.jans.as.server.service.date;

import io.jans.as.model.common.CallerType;
import io.jans.as.model.configuration.AppConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * @author Yuriy Z
 */
@ApplicationScoped
@Named
public class DateFormatterService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    public Serializable formatClaim(Date date, CallerType callerType) {
        return formatClaim(date, callerType.name().toLowerCase());
    }

    /**
     *
     * @param date date to format
     * @param patternKey pattern key. It's by intention is not enum to allow arbitrary key (not "locked" by CallerType)
     * @return formatter value
     */
    public Serializable formatClaim(Date date, String patternKey) {
        // key in map is string by intention to not "lock" it by CallerType
        final Map<String, String> formatterMap = appConfiguration.getDateFormatterPatterns();

        if (formatterMap.isEmpty()) {
            return formatClaimFallback(date);
        }

        final String explicitFormatter = formatterMap.get(patternKey);
        if (StringUtils.isNotBlank(explicitFormatter)) {
            return new SimpleDateFormat(explicitFormatter).format(date);
        }

        final String commonFormatter = formatterMap.get(CallerType.COMMON.name().toLowerCase());
        if (StringUtils.isNotBlank(commonFormatter)) {
            return new SimpleDateFormat(commonFormatter).format(date);
        }

        return formatClaimFallback(date);
    }

    public Serializable formatClaimFallback(Date date) {
        return date.getTime() / 1000;
    }
}
