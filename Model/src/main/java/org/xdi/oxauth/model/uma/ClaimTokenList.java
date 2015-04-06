package org.xdi.oxauth.model.uma;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import java.util.ArrayList;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/03/2015
 */
@IgnoreMediaTypes("application/*+json")
public class ClaimTokenList extends ArrayList<ClaimToken> {

    @JsonIgnore
    public ClaimTokenList addToken(ClaimToken token) {
        add(token);
        return this;
    }
}
