/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.uma;

/**
 * @author Yuriy Movchan Date: 10/03/2012
 */
public class UmaConstants {

    private UmaConstants() {
    }

    /*
    * yuriyz 01/04/2013 : as it was emailed by Eve:
    * We've been removing all the specialized content type extensions,
    * and just sticking with application/json. I'll add an issue on our side
    * to update the specs to remove those last few instances of application/xxx+json.
    */
    public static final String JSON_MEDIA_TYPE = "application/json";

    public static final String GATHERING_ID = "gathering_id";

    public static final String NO_SCRIPT = "no_script";

}
