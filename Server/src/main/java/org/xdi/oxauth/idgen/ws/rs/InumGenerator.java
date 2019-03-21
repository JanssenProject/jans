/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.idgen.ws.rs;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.model.base.DummyEntry;
import org.gluu.search.filter.Filter;
import org.slf4j.Logger;
import org.xdi.oxauth.model.common.IdType;
import org.xdi.oxauth.model.config.BaseDnConfiguration;
import org.xdi.oxauth.model.config.StaticConfiguration;
import org.xdi.oxauth.service.api.IdGenerator;
import org.xdi.util.INumGenerator;

/**
 * Inum ID generator. Generates inum: e.g. @!1111!0001!1234.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 26/06/2013
 */
@Stateless
@Named("inumGenerator")
public class InumGenerator {

    public static final String SEPARATOR = "!";

    private static final int MAX = 100;

	private final Pattern baseRdnPattern = Pattern.compile(".+o=([\\w\\!\\@\\.]+)$");

    @Inject
    private Logger log;
    @Inject
    private PersistenceEntryManager ldapEntryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

    public String generateId(String p_idType, String p_idPrefix) {
        final IdType idType = IdType.fromString(p_idType);
        if (idType != null) {
            return generateId(idType, p_idPrefix);
        } else {
            log.error("Unable to identify id type: {}", p_idType);
        }
        return "";
    }

    public String generateId(IdType p_idType, String p_idPrefix) {
        String inum;
        int counter = 0;

        try {
            while (true) {
                final StringBuilder sb = new StringBuilder();
                sb.append(p_idPrefix).
                        append(InumGenerator.SEPARATOR).
                        append(p_idType.getInum()).
                        append(InumGenerator.SEPARATOR);

                if ((IdType.CLIENTS == p_idType) || (IdType.PEOPLE == p_idType)) { 
                	sb.append(INumGenerator.generate(4));
                } else {
                	sb.append(INumGenerator.generate(2));
                }

                inum = sb.toString();
                if (StringUtils.isBlank(inum)) {
                    log.error("Unable to generate inum: {}", inum);
                    break;
                }

                if (!contains(inum, p_idType)) {
                    break;
                }

                /* Just to make sure it doesn't get into an infinite loop */
                if (counter > MAX) {
                    log.error("Infinite loop problem while generating new inum");
                    return "";
                }
                counter++;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            inum = e.getMessage();
        }
        log.trace("Generated inum: {}", inum);
        return inum;
    }

	public boolean contains(String inum, IdType type) {
		final String baseDn = baseDn(type);
		final Filter filter = Filter.createEqualityFilter("inum", inum);
		final List<DummyEntry> entries = ldapEntryManager.findEntries(baseDn, DummyEntry.class, filter);
		return entries != null && !entries.isEmpty();
	}

    public String baseDn(IdType p_type) {
        final BaseDnConfiguration baseDn = staticConfiguration.getBaseDn();
        switch (p_type) {
            case CLIENTS:
                return baseDn.getClients();
            case CONFIGURATION:
                return baseDn.getConfiguration();
            case ATTRIBUTE:
                return baseDn.getAttributes();
            case PEOPLE:
                return baseDn.getPeople();
        }

        // if not able to identify baseDn by type then return organization baseDn, e.g. o=gluu
        Matcher m = baseRdnPattern.matcher(baseDn.getClients());
        if (m.matches()) {
        	return m.group(1);
        }

        log.error("Use fallback DN: o=gluu, for ID generator, please check oxAuth configuration, clientDn must be valid DN");
        return "o=gluu";
    }

}
