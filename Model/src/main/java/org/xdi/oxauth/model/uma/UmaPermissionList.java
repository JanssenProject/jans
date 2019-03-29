package org.xdi.oxauth.model.uma;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author yuriyz on 05/25/2017.
 */
@IgnoreMediaTypes("application/*+json")
public class UmaPermissionList extends ArrayList<UmaPermission> {

    @JsonIgnore
    public UmaPermissionList addPermission(UmaPermission permission) {
        add(permission);
        return this;
    }

    public static UmaPermissionList instance(UmaPermission... permissions) {
        UmaPermissionList instance = new UmaPermissionList();
        Collections.addAll(instance, permissions);
        return instance;
    }
}
