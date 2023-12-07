package io.jans.casa.plugins.authnmethod.service;

import com.fasterxml.jackson.core.type.TypeReference;
import io.jans.casa.core.model.PersonMobile;
import io.jans.casa.core.pojo.VerifiedMobile;
import io.jans.casa.misc.Utils;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * An app. scoped bean to serve the purpose of sending SMS
 * @author jgomer
 * @author Stefan Andersson
 */
abstract public class MobilePhoneService extends BaseService {

    @Inject
    private Logger logger;

    abstract public void reloadConfiguration();

    abstract public SMSDeliveryStatus sendSMS(String number, String body);

    public boolean isNumberRegistered(String number) {

        PersonMobile person = new PersonMobile();
        person.setMobile(Collections.singletonList(number));
        person.setBaseDn(persistenceService.getPeopleDn());
        return persistenceService.count(person) > 0;

    }

    public boolean updateMobilePhonesAdd(String userId, List<VerifiedMobile> mobiles, VerifiedMobile newPhone) {

        boolean success = false;
        try {
            List<VerifiedMobile> vphones = new ArrayList<>(mobiles);
            if (newPhone != null) {
                vphones.add(newPhone);
            }

            List<String> numbers = vphones.stream().map(VerifiedMobile::getNumber).collect(Collectors.toList());
            String json = numbers.size() > 0 ? mapper.writeValueAsString(Collections.singletonMap("phones", vphones)) : null;

            logger.debug("Updating phones for user '{}'", userId);
            PersonMobile person = persistenceService.get(PersonMobile.class, persistenceService.getPersonDn(userId));
            person.setMobileDevices(json);
            person.setMobile(numbers);

            success = persistenceService.modify(person);

            if (success && newPhone != null) {
                //modify list only if LDAP update took place
                mobiles.add(newPhone);
                logger.debug("Added {}", newPhone.getNumber());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return success;

    }

    public boolean addPhone(String userId, VerifiedMobile newPhone) {
        return updateMobilePhonesAdd(userId, getVerifiedPhones(userId), newPhone);
    }

    public int getPhonesTotal(String userId) {

        int total = 0;
        try {
            PersonMobile person = persistenceService.get(PersonMobile.class, persistenceService.getPersonDn(userId));
            total = person.getMobile().size();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return total;

    }

    public List<VerifiedMobile> getVerifiedPhones(String userId) {

        List<VerifiedMobile> phones = new ArrayList<>();
        try {
            PersonMobile person = persistenceService.get(PersonMobile.class, persistenceService.getPersonDn(userId));
            String json = person.getMobileDevices();
            json = Utils.isEmpty(json) ? "[]" : mapper.readTree(json).get("phones").toString();

            List<VerifiedMobile> vphones = mapper.readValue(json, new TypeReference<List<VerifiedMobile>>() { });
            phones = person.getMobile().stream().map(str -> getExtraPhoneInfo(str, vphones)).sorted()
                    .collect(Collectors.toList());
            logger.trace("getVerifiedPhones. User '{}' has {}", userId, phones.stream().map(VerifiedMobile::getNumber).collect(Collectors.toList()));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return phones;

    }

    /**
     * Creates an instance of VerifiedMobile by looking up in the list of VerifiedPhones passed. If the item is not found
     * in the list, it means the user had already that phone added by means of another application, ie. admin-ui. In this
     * case the resulting object will not have properties like nickname, etc. Just the phone number
     * @param number Phone number (LDAP attribute "mobile" inside a user entry)
     * @param list List of existing phones enrolled. Ideally, there is an item here corresponding to the uid number passed
     * @return VerifiedMobile object
     */
    private VerifiedMobile getExtraPhoneInfo(String number, List<VerifiedMobile> list) {
        //Complements current phone with extra info in the list if any
        VerifiedMobile phone = new VerifiedMobile(number);

        Optional<VerifiedMobile> extraInfoPhone = list.stream().filter(ph -> number.equals(ph.getNumber())).findFirst();
        if (extraInfoPhone.isPresent()) {
            phone.setAddedOn(extraInfoPhone.get().getAddedOn());
            phone.setNickName(extraInfoPhone.get().getNickName());
        }
        return phone;
    }

}
