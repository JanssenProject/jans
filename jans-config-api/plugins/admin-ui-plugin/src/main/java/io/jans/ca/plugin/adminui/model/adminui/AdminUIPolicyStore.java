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

public class AdminUIPolicyStore {

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

    /**
     * Retrieve the uploaded policy store binary stream.
     *
     * @return the `policyStore` InputStream containing the binary content submitted via the multipart form field "policyStore"
     */
    public InputStream getPolicyStore() {
        return policyStore;
    }

    /**
     * Assigns the input stream that contains the binary policy store.
     *
     * @param policyStore the input stream for the policy store data
     */
    public void setPolicyStore(InputStream policyStore) {
        this.policyStore = policyStore;
    }

    /**
     * Retrieves the JSON `Document` supplied in the multipart form field named "document".
     *
     * @return the `Document` parsed from the multipart "document" field
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Assigns the JSON document corresponding to the multipart form field named "document".
     *
     * @param document the JSON `Document` bound from the multipart form part named "document"
     */
    public void setDocument(Document document) {
        this.document = document;
    }

    /**
     * Produce a string representation of this AdminUIPolicyStore that includes the `policyStore` field.
     *
     * @return a string containing the class name and the `policyStore` value
     */
    @Override
    public String toString() {
        return "AdminUIPolicyStore{" +
                "document=" + document +
                ", policyStore=" + policyStore +
                '}';
    }
}
