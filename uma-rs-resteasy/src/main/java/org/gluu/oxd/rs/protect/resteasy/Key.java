package org.gluu.oxd.rs.protect.resteasy;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 19/04/2016
 */

public class Key {

    private String path;
    private List<String> httpMethods;

    public Key() {
    }

    public Key(String path, List<String> httpMethod) {
        this.path = path;
        this.httpMethods = httpMethod;
    }

    public String getResourceName() {
        return httpMethods + " " + path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getHttpMethods() {
        return httpMethods;
    }

    public void setHttpMethods(List<String> httpMethods) {
        this.httpMethods = httpMethods;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Key key = (Key) o;

        if (httpMethods != null ? !httpMethods.equals(key.httpMethods) : key.httpMethods != null) return false;
        if (path != null ? !path.equals(key.path) : key.path != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = path != null ? path.hashCode() : 0;
        result = 31 * result + (httpMethods != null ? httpMethods.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Key");
        sb.append("{path='").append(path).append('\'');
        sb.append(", httpMethods='").append(httpMethods).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
