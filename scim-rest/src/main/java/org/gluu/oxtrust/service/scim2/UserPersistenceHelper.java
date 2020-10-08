package org.gluu.oxtrust.service.scim2;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.gluu.oxtrust.model.GluuGroup;
import org.gluu.oxtrust.model.scim.ScimCustomPerson;
import org.gluu.oxtrust.model.scim2.user.Email;
import org.gluu.oxtrust.service.AttributeService;
import org.gluu.oxtrust.service.IGroupService;
import org.gluu.oxtrust.service.IPersonService;
import org.gluu.oxtrust.util.ServiceUtil;
import io.jans.orm.PersistenceEntryManager;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class UserPersistenceHelper {

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    private IPersonService personService;

    @Inject
    private AttributeService attributeService;

    @Inject
    private IGroupService groupService;

    public void addCustomObjectClass(ScimCustomPerson person) {
        String[] customObjectClasses = Optional.ofNullable(person.getCustomObjectClasses()).orElse(new String[0]);
        Set<String> customObjectClassesSet = new HashSet<>(Stream.of(customObjectClasses).collect(Collectors.toList()));
        customObjectClassesSet.add(attributeService.getCustomOrigin());
        person.setCustomObjectClasses(customObjectClassesSet.toArray(new String[0]));
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
        if (person.getAttribute("oxTrustMetaLastModified") != null) {
            person.setAttribute("oxTrustMetaLastModified",
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
     * One-way sync from "oxTrustEmail" to "mail". Ultimately this is persisted so
     * "mail" will be updated by values from "oxTrustEmail".
     *
     * @param customPerson Represents the user object to be modified 
     * @return Modified user object
     * @throws Exception If (json) values in oxTrustEmail cannot be parsed 
     */
    public ScimCustomPerson syncEmailForward(ScimCustomPerson customPerson) throws Exception {

        log.info("syncing email ...");
        List<String> oxTrustEmails = customPerson.getAttributeList("oxTrustEmail");

        if (!oxTrustEmails.isEmpty()) {
            ObjectMapper mapper = ServiceUtil.getObjectMapper();
            String[] newMails = new String[oxTrustEmails.size()];

            for (int i = 0; i < newMails.length; i++) {
                newMails[i] = mapper.readValue(oxTrustEmails.get(i), Email.class).getValue();
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
