package org.xdi.oxauth.model.jwt;

/**
 * @author Javier Rojas Blum
 * @version Jun 10, 2015
 */
public class JwtSubClaimObject extends JwtClaimSet {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
