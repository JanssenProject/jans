package io.jans.ca.plugin.adminui.model.adminui;

import io.jans.service.document.store.model.Document;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;

public class AdminUIPolicyStore implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    @Valid
    @FormParam("document")
    @PartType(MediaType.APPLICATION_JSON)
    private Document document;

    @NotNull
    @FormParam("policyStore")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    @Schema(implementation = String.class, format="binary")
    private InputStream policyStore;

    public InputStream getPolicyStore() {
        return policyStore;
    }

    public void setPolicyStore(InputStream policyStore) {
        this.policyStore = policyStore;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    @Override
    public String toString() {
        return "AdminUIPolicyStore{" +
                "policyStore=" + policyStore +
                '}';
    }
}
