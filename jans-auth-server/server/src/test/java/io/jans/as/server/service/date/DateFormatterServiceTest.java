package io.jans.as.server.service.date;

import io.jans.as.model.common.CallerType;
import io.jans.as.model.configuration.AppConfiguration;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * @author Yuriy Z
 */
@Listeners(MockitoTestNGListener.class)
public class DateFormatterServiceTest {

    @InjectMocks
    @Spy
    private DateFormatterService dateFormatterService;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Test
    public void formatClaim_whenNotFormatter_shouldFallback() {
        Date date = new Date(10000);

        final Serializable formattedValue = dateFormatterService.formatClaim(date, CallerType.USERINFO);
        assertEquals(10L, formattedValue);
    }

    @Test
    public void formatClaim_whenExplicitClaimFormatterIsSet_shouldFormatByFormatter() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2023, Calendar.JANUARY, 11);

        Map<String, String> map = new HashMap<>();
        map.put("birthdate", "yyyy-MM-dd");

        when(appConfiguration.getDateFormatterPatterns()).thenReturn(map);

        final Serializable formattedValue = dateFormatterService.formatClaim(calendar.getTime(), "birthdate");
        assertEquals(formattedValue, "2023-01-11");
    }

    @Test
    public void formatClaim_whenUserInfoFormatterIsSet_shouldFormatByFormatter() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2023, Calendar.JANUARY, 11);

        Map<String, String> map = new HashMap<>();
        map.put("userinfo", "yyyy-MM-dd");

        when(appConfiguration.getDateFormatterPatterns()).thenReturn(map);

        final Serializable formattedValue = dateFormatterService.formatClaim(calendar.getTime(), CallerType.USERINFO);
        assertEquals(formattedValue, "2023-01-11");
    }

    @Test
    public void formatClaim_whenCommonFormatterIsSet_shouldFormatByFormatter() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2023, Calendar.JANUARY, 11);

        Map<String, String> map = new HashMap<>();
        map.put("common", "yyyy-MM-dd");

        when(appConfiguration.getDateFormatterPatterns()).thenReturn(map);

        final Serializable formattedValue = dateFormatterService.formatClaim(calendar.getTime(), CallerType.USERINFO);
        assertEquals(formattedValue, "2023-01-11");
    }
}
