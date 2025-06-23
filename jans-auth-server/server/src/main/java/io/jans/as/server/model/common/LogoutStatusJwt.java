package io.jans.as.server.model.common;

import java.util.Date;

/**
 * @author Yuriy Z
 */
public class LogoutStatusJwt extends AbstractToken  {

    public LogoutStatusJwt(int lifeTime) {
        super(lifeTime);
    }

    public LogoutStatusJwt(String code, Date creationDate, Date expirationDate) {
        super(code, creationDate, expirationDate);
    }
}
