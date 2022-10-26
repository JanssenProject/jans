package io.jans.as.common.service.common;

import io.jans.as.model.common.IdType;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.testng.MockitoTestNGListener;
import org.slf4j.Logger;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.regex.Pattern;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

@Listeners(MockitoTestNGListener.class)
public class InumServiceTest {

    @InjectMocks
    private InumService inumService;

    @Mock
    private Logger log;

    @Mock
    private ExternalIdGeneratorService externalIdGenerationService;

    private final static Pattern UUID_REGEX_PATTERN =
            Pattern.compile("^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$");

    @Test
    public void generateClientInum_isEnabledFalse_notNull(){
        Mockito.doReturn(false).when(externalIdGenerationService).isEnabled();
        String id = inumService.generateClientInum();
        assertTrue(UUID_REGEX_PATTERN.matcher(id).matches());
        assertNotNull(id);

        verify(externalIdGenerationService).isEnabled();
        verifyNoMoreInteractions(externalIdGenerationService,log);
    }

    @Test
    public void generatePeopleInum_executeExternalDefaultGenerateIdMethodNull_notNull(){
        Mockito.doReturn(true).when(externalIdGenerationService).isEnabled();
        Mockito.doReturn(null).when(externalIdGenerationService)
                .executeExternalDefaultGenerateIdMethod(anyString(), anyString(), anyString());

        String id = inumService.generatePeopleInum();
        assertTrue(UUID_REGEX_PATTERN.matcher(id).matches());
        assertNotNull(id);

        verifyInteractions(IdType.PEOPLE.getType());
    }

    @Test
    public void generateClientInum_executeExternalDefaultGenerateIdMethodNull_notNull(){
        Mockito.doReturn(true).when(externalIdGenerationService).isEnabled();
        Mockito.doReturn(null).when(externalIdGenerationService)
                .executeExternalDefaultGenerateIdMethod(anyString(), anyString(), anyString());

        String id = inumService.generateClientInum();
        assertTrue(UUID_REGEX_PATTERN.matcher(id).matches());
        assertNotNull(id);

        verifyInteractions(IdType.CLIENTS.getType());
    }

    @Test
    public void generateClientInum_executeExternalDefaultGenerateIdMethodEmpty_notNull(){
        Mockito.doReturn(true).when(externalIdGenerationService).isEnabled();
        Mockito.doReturn("").when(externalIdGenerationService)
                .executeExternalDefaultGenerateIdMethod(anyString(), anyString(), anyString());

        String id = inumService.generateClientInum();
        assertTrue(UUID_REGEX_PATTERN.matcher(id).matches());
        assertNotNull(id);

        verifyInteractions(IdType.CLIENTS.getType());
    }

    @Test
    public void generateClientInum_executeExternalDefaultGenerateIdMethodReturnValueNotUUID_notNull(){
        final String generatedId = "9fcea0f4" ;
        Mockito.doReturn(true).when(externalIdGenerationService).isEnabled();
        Mockito.doReturn(generatedId).when(externalIdGenerationService)
                .executeExternalDefaultGenerateIdMethod(anyString(), anyString(), anyString());

        String id = inumService.generateClientInum();
        assertNotNull(id);
        assertEquals(id, generatedId);
        assertFalse(UUID_REGEX_PATTERN.matcher(id).matches());

        verifyInteractions(IdType.CLIENTS.getType());
    }

    private void verifyInteractions(String idType){
        verify(externalIdGenerationService).isEnabled();
        verify(externalIdGenerationService).executeExternalDefaultGenerateIdMethod(eq("oxauth"), eq(idType), eq(""));

        verifyNoMoreInteractions(externalIdGenerationService,log);
    }

}
