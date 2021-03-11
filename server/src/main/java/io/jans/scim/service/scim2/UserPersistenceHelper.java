/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service.scim2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.ldap.impl.LdapEntryManagerFactory;
import io.jans.scim.model.GluuGroup;
import io.jans.scim.model.scim.ScimCustomPerson;
import io.jans.scim.model.scim2.user.Email;
import io.jans.scim.service.AttributeService;
import io.jans.scim.service.GroupService;
import io.jans.scim.service.PersonService;
import io.jans.scim.util.ServiceUtil;

@ApplicationScoped
public class UserPersistenceHelper {

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    private PersonService personService;

    @Inject
    private AttributeService attributeService;

    @Inject
    private GroupService groupService;

    public void addCustomObjectClass(ScimCustomPerson person) {
		if (LdapEntryManagerFactory.PERSISTENCE_TYPE.equals(persistenceEntryManager.getPersistenceType())) {
	        String[] customObjectClasses = Optional.ofNullable(person.getCustomObjectClasses()).orElse(new String[0]);
	        Set<String> customObjectClassesSet = new HashSet<>(Stream.of(customObjectClasses).collect(Collectors.toList()));
	        customObjectClassesSet.add(attributeService.getCustomOrigin());
	        person.setCustomObjectClasses(customObjectClassesSet.toArray(new String[0]));
		}
    }

    public void addPerson(ScimCustomPerson person) throws Exception {
        //It is guaranteed that no duplicate UID occurs when this method is called
        person.setCreationDate(new Date());
        persistenceEntryManager.persist(person);
    }

    public ScimCustomPerson getPersonByInum(String inum) {

        ScimCustomPerson person = null;
        try {
            person = persistenceEntryManager.find(ScimCustomPerson.class, personService.getDnForPerson(inum));
        } catch (Exception e) {
            log.error("Failed to find Person by Inum " + inum, e);
        }
        return person;

    }

    public void updatePerson(ScimCustomPerson person) {

        Date updateDate = new Date();
        person.setUpdatedAt(updateDate);
        if (person.getAttribute("jansMetaLastMod") != null) {
            person.setAttribute("jansMetaLastMod",
                    ISODateTimeFormat.dateTime().withZoneUTC().print(updateDate.getTime()));
        }
        persistenceEntryManager.merge(person);

    }

    /**
     * Delete a person from a group
     *
     * @throws Exception
     */
    public void deleteUserFromGroup(ScimCustomPerson person, String dn) throws Exception {

        List<String> groups = person.getMemberOf();
        for (String oneGroup : groups) {

            GluuGroup aGroup = groupService.getGroupByDn(oneGroup);
            List<String> tempGroupMembers = new ArrayList<>(
                    Optional.ofNullable(aGroup.getMembers()).orElse(Collections.emptyList()));

            if (tempGroupMembers.contains(dn)) {
                tempGroupMembers.remove(dn);
                aGroup.setMembers(tempGroupMembers.isEmpty() ? null : tempGroupMembers);
                groupService.updateGroup(aGroup);
            }
        }

    }

    /**
     * One-way sync from "jansEmail" to "mail". Ultimately this is persisted so
     * "mail" will be updated by values from "jansEmail".
     *
     * @param customPerson Represents the user object to be modified 
     * @return Modified user object
     * @throws Exception If (json) values in excludeEmail cannot be parsed 
     */
    public ScimCustomPerson syncEmailForward(ScimCustomPerson customPerson) throws Exception {

        log.info("syncing email ...");
        List<String> excludeEmails = customPerson.getAttributeList("jansEmail");

        if (!excludeEmails.isEmpty()) {
            ObjectMapper mapper = ServiceUtil.getObjectMapper();
            String[] newMails = new String[excludeEmails.size()];

            for (int i = 0; i < newMails.length; i++) {
                newMails[i] = mapper.readValue(excludeEmails.get(i), Email.class).getValue();
            }
            customPerson.setAttribute("mail", newMails);
        } else {
            customPerson.setAttribute("mail", new String[0]);
        }
        return customPerson;

    }

    public void removePerson(ScimCustomPerson person) {
        persistenceEntryManager.removeRecursively(person.getDn());
    }

}
