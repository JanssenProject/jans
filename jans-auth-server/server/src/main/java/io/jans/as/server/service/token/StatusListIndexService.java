package io.jans.as.server.service.token;

import io.jans.model.token.TokenIndexHead;
import io.jans.orm.PersistenceEntryManager;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.configuration.BaseConfiguration;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Yuriy Z
 */
@Named
public class StatusListIndexService {

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    private BaseConfiguration baseConfiguration;

    private final AtomicInteger localIndex = new AtomicInteger(0);

    public int nextIndex() {
        final int nextIndex = localIndex.incrementAndGet();

        String dn = dn();
        final TokenIndexHead head = persistenceEntryManager.find(TokenIndexHead.class, dn);

        // todo
        return nextIndex;
    }

    private String dn() {
//        return baseConfiguration.getString("jansIndex=HEAD,jansAuth_ConfigurationEntryDN");
        return "";
    }
}
