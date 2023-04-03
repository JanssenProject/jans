package io.jans.ca.plugin.adminui.model.auth;

public class SSARequest {
    private String ssa;
    public String getSsa() {
        return ssa;
    }

    public void setSsa(String ssa) {
        this.ssa = ssa;
    }

    @Override
    public String toString() {
        return "SSARequest{" +
                "ssa='" + ssa + '\'' +
                '}';
    }
}
