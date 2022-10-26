/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.idgen.ws.rs;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.IdType;
import io.jans.as.model.config.BaseDnConfiguration;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.persistence.model.configuration.GluuConfiguration;
import io.jans.model.GluuAttribute;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.base.DummyEntry;
import io.jans.orm.search.filter.Filter;
import io.jans.util.INumGenerator;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public String generateId(String idTypeStr, String idPrefix) {
        final IdType idType = IdType.fromString(idTypeStr);
        if (idType != null) {
            return generateId(idType, idPrefix);
        } else {
            log.error("Unable to identify id type: {}", idTypeStr);
        }
        return "";
    }

    public String generateId(IdType idType, String idPrefix) {
        String inum;
        int counter = 0;

        try {
            while (true) {
                final StringBuilder sb = new StringBuilder();
                sb.append(idPrefix).
                        append(InumGenerator.SEPARATOR).
                        append(idType.getInum()).
                        append(InumGenerator.SEPARATOR);

                if ((IdType.CLIENTS == idType) || (IdType.PEOPLE == idType)) {
                    sb.append(INumGenerator.generate(4));
                } else {
                    sb.append(INumGenerator.generate(2));
                }

                inum = sb.toString();
                if (StringUtils.isBlank(inum)) {
                    log.error("Unable to generate inum: {}", inum);
                    break;
                }

                if (!contains(inum, idType)) {
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
        Class<?> entryClass = getEntryClass(type);
        final List<?> entries = ldapEntryManager.findEntries(baseDn, entryClass, filter);
        return entries != null && !entries.isEmpty();
    }

    private Class<?> getEntryClass(IdType type) {
		switch (type) {
			case CLIENTS:
				return Client.class;
			case CONFIGURATION:
				return GluuConfiguration.class;
			case ATTRIBUTE:
				return GluuAttribute.class;
			case PEOPLE:
				return User.class;
		}

       return DummyEntry.class;
	}

    public String baseDn(IdType type) {
        final BaseDnConfiguration baseDn = staticConfiguration.getBaseDn();
        switch (type) {
            case CLIENTS:
                return baseDn.getClients();
            case CONFIGURATION:
                return baseDn.getConfiguration();
            case ATTRIBUTE:
                return baseDn.getAttributes();
            case PEOPLE:
                return baseDn.getPeople();
        }

        // if not able to identify baseDn by type then return organization baseDn, e.g. o=jans
        Matcher m = baseRdnPattern.matcher(baseDn.getClients());
        if (m.matches()) {
            return m.group(1);
        }

        log.error("Use fallback DN: o=jans, for ID generator, please check Jans Auth configuration, clientDn must be valid DN");
        return "o=jans";
    }

}
