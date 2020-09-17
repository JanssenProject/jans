package org.gluu.oxauth.service.common;

import org.gluu.util.StringHelper;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.UUID;

/**
 * @author gasmyr on 9/17/20.
 */
@ApplicationScoped
public class IdGenService {
    public static final int MAX_IDGEN_TRY_COUNT = 10;

    @Inject
    private ExternalIdGeneratorService externalIdGenerationService;

    public String generateId(String idType) {

        if (externalIdGenerationService.isEnabled()) {
            final String generatedId = externalIdGenerationService.executeExternalDefaultGenerateIdMethod("oxtrust",
                    idType, "");

            if (StringHelper.isNotEmpty(generatedId)) {
                return generatedId;
            }
        }

        return generateDefaultId();
    }

    public String generateDefaultId() {

        return UUID.randomUUID().toString();
    }
}
