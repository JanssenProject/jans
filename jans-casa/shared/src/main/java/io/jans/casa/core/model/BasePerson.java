package io.jans.casa.core.model;

import io.jans.orm.model.base.InumEntry;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.CustomObjectClass;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;

import java.util.HashSet;
import java.util.Set;

import io.jans.casa.misc.Utils;
import io.jans.casa.service.IPersistenceService;

/**
 * Serves as a minimal representation of a user (person) entry in Gluu database directory. Plugin developers can extend
 * this class by adding fields needed (with their respective getters/setters) in order to have access to more attributes.
 * Use this class in conjunction with {@link io.jans.casa.service.IPersistenceService} to CRUD users to your server.
 */
@DataEntry
@ObjectClass("jansPerson")
public class BasePerson extends InumEntry {

    @AttributeName
    private String uid;

    @CustomObjectClass
    private static String[] customObjectClasses;

    static {
        IPersistenceService ips = Utils.managedBean(IPersistenceService.class);
        Set<String> ocs = new HashSet<>(ips.getPersonOCs());
        ocs.remove("top");
        ocs.remove("jansPerson");
        setCustomObjectClasses(ocs.toArray(new String[0]));
    }

    public String getUid() {
        return uid;
    }

    public static String[] getCustomObjectClasses() {
        return customObjectClasses;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public static void setCustomObjectClasses(String[] customObjectClasses) {
        BasePerson.customObjectClasses = customObjectClasses;
    }

}
