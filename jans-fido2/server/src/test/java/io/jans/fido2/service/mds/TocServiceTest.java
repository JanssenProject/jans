/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.mds;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.service.document.store.model.Document;
import io.jans.service.document.store.service.DBDocumentService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TocServiceTest {

    @Spy
    @InjectMocks
    private TocService tocService;

    @Mock
    private Logger log;
    @Mock
    private AppConfiguration appConfiguration;
    @Mock
    private DBDocumentService dbDocumentService;

    private static final String TOC_FOLDER = "/etc/jans/conf/fido2/mds/toc";

    private Fido2Configuration configuration(boolean disabled, int retries) {
        Fido2Configuration cfg = new Fido2Configuration();
        cfg.setDisableMetadataService(disabled);
        cfg.setMdsDownloadStartupRetries(retries);
        cfg.setMdsDownloadStartupRetryInterval(0);
        cfg.setMdsTocsFolder(TOC_FOLDER);
        return cfg;
    }

    // ---- fetchMetadata(boolean) retry control flow ----

    @Test
    void fetchMetadata_metadataServiceDisabled_doesNothing() {
        when(appConfiguration.getFido2Configuration()).thenReturn(configuration(true, 3));

        tocService.fetchMetadata(true);

        verify(tocService, never()).fetchMetadataOnce();
        verify(tocService, never()).isTocContentMissing();
    }

    @Test
    void fetchMetadata_notStartup_singleAttemptNoRetry() {
        when(appConfiguration.getFido2Configuration()).thenReturn(configuration(false, 3));
        doNothing().when(tocService).fetchMetadataOnce();

        // retryWhenTocMissing = false => stale-TOC path: exactly one attempt, no missing-check, no retry.
        tocService.fetchMetadata(false);

        verify(tocService, times(1)).fetchMetadataOnce();
        verify(tocService, never()).isTocContentMissing();
        verify(tocService, never()).sleepBeforeRetry();
    }

    @Test
    void fetchMetadata_startupTocPresent_singleAttempt() {
        when(appConfiguration.getFido2Configuration()).thenReturn(configuration(false, 3));
        doReturn(false).when(tocService).isTocContentMissing();
        doNothing().when(tocService).fetchMetadataOnce();

        // TOC present but possibly stale: a single attempt, matching the daily timer behaviour.
        tocService.fetchMetadata(true);

        verify(tocService, times(1)).fetchMetadataOnce();
        verify(tocService, never()).sleepBeforeRetry();
    }

    @Test
    void fetchMetadata_startupTocMissingAllFail_retriesConfiguredTimes() {
        when(appConfiguration.getFido2Configuration()).thenReturn(configuration(false, 3));
        doReturn(true).when(tocService).isTocContentMissing();
        doThrow(new Fido2RuntimeException("MDS unavailable")).when(tocService).fetchMetadataOnce();
        doNothing().when(tocService).sleepBeforeRetry();

        tocService.fetchMetadata(true);

        // 3 attempts, with a wait between each (i.e. attempts - 1 sleeps).
        verify(tocService, times(3)).fetchMetadataOnce();
        verify(tocService, times(2)).sleepBeforeRetry();
    }

    @Test
    void fetchMetadata_startupRetriesFlooredToOne_singleAttempt() {
        when(appConfiguration.getFido2Configuration()).thenReturn(configuration(false, 0));
        doReturn(true).when(tocService).isTocContentMissing();
        doThrow(new Fido2RuntimeException("MDS unavailable")).when(tocService).fetchMetadataOnce();

        // A misconfigured retry count (<= 0) must still perform at least one attempt.
        tocService.fetchMetadata(true);

        verify(tocService, times(1)).fetchMetadataOnce();
        verify(tocService, never()).sleepBeforeRetry();
    }

    @Test
    void fetchMetadata_startupTocRecoversAfterFirstAttempt_stopsEarly() {
        when(appConfiguration.getFido2Configuration()).thenReturn(configuration(false, 3));
        // First check computes maxAttempts (missing); after the first attempt the TOC is present.
        doReturn(true, false).when(tocService).isTocContentMissing();
        doNothing().when(tocService).fetchMetadataOnce();
        doNothing().when(tocService).sleepBeforeRetry();

        tocService.fetchMetadata(true);

        verify(tocService, times(1)).fetchMetadataOnce();
        verify(tocService, never()).sleepBeforeRetry();
    }

    // ---- isTocContentMissing() ----

    @Test
    void isTocContentMissing_noDocuments_returnsTrue() {
        when(appConfiguration.getFido2Configuration()).thenReturn(configuration(false, 3));
        when(dbDocumentService.getDocumentsByFilePath(TOC_FOLDER)).thenReturn(Collections.emptyList());

        assertTrue(tocService.isTocContentMissing());
    }

    @Test
    void isTocContentMissing_emptyContent_returnsTrue() {
        when(appConfiguration.getFido2Configuration()).thenReturn(configuration(false, 3));
        Document document = new Document();
        document.setDocument("");
        when(dbDocumentService.getDocumentsByFilePath(TOC_FOLDER)).thenReturn(Collections.singletonList(document));

        assertTrue(tocService.isTocContentMissing());
    }

    @Test
    void isTocContentMissing_withContent_returnsFalse() {
        when(appConfiguration.getFido2Configuration()).thenReturn(configuration(false, 3));
        Document document = new Document();
        document.setDocument("some-base64-encoded-toc-blob");
        when(dbDocumentService.getDocumentsByFilePath(TOC_FOLDER))
                .thenReturn(Arrays.asList(document, new Document()));

        assertFalse(tocService.isTocContentMissing());
    }

    @Test
    void isTocContentMissing_lookupThrows_treatedAsMissing() {
        when(appConfiguration.getFido2Configuration()).thenReturn(configuration(false, 3));
        when(dbDocumentService.getDocumentsByFilePath(any())).thenThrow(new RuntimeException("DB down"));

        // A failure to determine the state must be conservative: assume missing so retries kick in.
        assertTrue(tocService.isTocContentMissing());
    }
}
