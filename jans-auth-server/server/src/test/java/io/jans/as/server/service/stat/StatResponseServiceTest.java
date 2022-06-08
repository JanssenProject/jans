package io.jans.as.server.service.stat;

import io.jans.orm.PersistenceEntryManager;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Yuriy Zabrovarnyy
 */
@Listeners(MockitoTestNGListener.class)
public class StatResponseServiceTest {

    @InjectMocks
    private StatResponseService statResponseService;

    @Mock
    private PersistenceEntryManager entryManager;

    @Mock
    private StatService statService;

    @Mock
    private Logger log; // required, don't remove

    @Test
    public void buildResponse_whenCalled_shouldInvokeEntityManagerOneTimeBecauseSecondTimeResponseMustBeCached() {
        when(entryManager.findEntries(any(), any(), any())).thenReturn(new ArrayList<>());
        when(statService.getBaseDn()).thenReturn("");

        statResponseService.buildResponse(Collections.singleton("01"));
        statResponseService.buildResponse(Collections.singleton("01"));
        statResponseService.buildResponse(Collections.singleton("01"));

        // must be called exactly 1 time, all further calls should use cached response
        verify(entryManager, times(1)).findEntries(any(), any(), any());
    }
}
