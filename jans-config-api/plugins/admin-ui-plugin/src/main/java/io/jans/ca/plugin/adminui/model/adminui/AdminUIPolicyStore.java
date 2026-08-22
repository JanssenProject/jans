package io.jans.ca.plugin.adminui.model.adminui;

import com.fasterxml.jackson.annotation.JsonView;
import io.jans.ca.plugin.adminui.model.adminui.PolicyStoreViews.Create;
import io.jans.ca.plugin.adminui.model.adminui.PolicyStoreViews.Edit;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;

    @DataEntry(sortBy = {"jansStatus"})
    @ObjectClass(value = "adminUIPolicyStore")
    public class AdminUIPolicyStore {
        // Read-only on edit: settable only when creating the policy store.
        @DN
        @JsonView(Create.class)
        @Schema(description = "Distinguished Name (DN) of the policy store entry. Server-generated on create; read-only and ignored on edit.", accessMode = Schema.AccessMode.READ_ONLY, example = "inum=e1a2b3c4-1234-5678-9abc-def012345678,ou=policy-stores,ou=admin-ui,o=jans")
        private String dn;
        @AttributeName(
                ignoreDuringUpdate = true
        )
        @JsonView(Create.class)
        @Schema(description = "Unique identifier (inum) of the policy store. Server-generated on create; read-only and ignored on edit.", accessMode = Schema.AccessMode.READ_ONLY, example = "e1a2b3c4-1234-5678-9abc-def012345678")
        private String inum;
        // Editable metadata: writable on both create and edit.
        @AttributeName(name = "displayname")
        @JsonView({Create.class, Edit.class})
        @Schema(description = "Human-readable display name of the policy store.", example = "Admin UI Cedarling Policy Store")
        private String displayname;
        @AttributeName(name = "description")
        @JsonView({Create.class, Edit.class})
        @Schema(description = "Free-text description of the policy store and its purpose.", example = "Cedarling policy store used to derive Admin UI role-to-scope mappings")
        private String description;
        // Read-only on edit: the policy store document itself is immutable once uploaded.
        @AttributeName(name = "document")
        @JsonView(Create.class)
        @Schema(description = "Base64-encoded Cedar policy store archive (.cjar zip). Required on create; immutable and ignored on edit.", example = "UEsDBBQACAgIAABb...==")
        private String policyStore;
        // Read-only on edit: owner cannot be reassigned via edit.
        @AttributeName(name = "jansUsrDN")
        @JsonView(Create.class)
        @Schema(description = "DN of the user who owns the policy store. Set at create time; cannot be reassigned via edit.", example = "inum=abc123,ou=people,o=jans")
        private String jansUsrDN;
        // Editable: status may be changed on both create and edit.
        @AttributeName(name = "jansStatus")
        @JsonView({Create.class, Edit.class})
        @Schema(description = "Status of the policy store. Only one store should be 'active' at a time; newly created stores default to 'inactive'.", allowableValues = {"active", "inactive"}, example = "active")
        private String jansStatus;
        // Read-only on edit: creation timestamp is fixed at create time.
        @AttributeName(name = "creationDate")
        @JsonView(Create.class)
        @Schema(description = "Timestamp when the policy store was created. Server-managed and fixed at create time.", accessMode = Schema.AccessMode.READ_ONLY, example = "2026-07-14T10:15:30.000Z")
        private Date creationDate = new Date();
        // Server-managed: overwritten on every create/edit, so any client-supplied value is ignored.
        @AttributeName(name = "jansLastUpd")
        @Schema(description = "Timestamp of the last update. Server-managed and overwritten on every create/edit; any client-supplied value is ignored.", accessMode = Schema.AccessMode.READ_ONLY, example = "2026-07-14T11:20:45.000Z")
        private Date jansLastUpd;

        public String getDn() {
            return dn;
        }

        public void setDn(String dn) {
            this.dn = dn;
        }

        public String getInum() {
            return inum;
        }

        public void setInum(String inum) {
            this.inum = inum;
        }

        public String getDisplayname() {
            return displayname;
        }

        public void setDisplayname(String displayname) {
            this.displayname = displayname;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getPolicyStore() {
            return policyStore;
        }

        public void setPolicyStore(String policyStore) {
            this.policyStore = policyStore;
        }

        public String getJansUsrDN() {
            return jansUsrDN;
        }

        public void setJansUsrDN(String jansUsrDN) {
            this.jansUsrDN = jansUsrDN;
        }

        public String getJansStatus() {
            return jansStatus;
        }

        public void setJansStatus(String jansStatus) {
            this.jansStatus = jansStatus;
        }

        public Date getCreationDate() {
            return creationDate;
        }

        public void setCreationDate(Date creationDate) {
            this.creationDate = creationDate;
        }

        public Date getJansLastUpd() {
            return jansLastUpd;
        }

        public void setJansLastUpd(Date jansLastUpd) {
            this.jansLastUpd = jansLastUpd;
        }

        @Override
        public String toString() {
            return "AdminUIPolicyStore{" +
                    "dn='" + dn + '\'' +
                    ", inum='" + inum + '\'' +
                    ", displayname='" + displayname + '\'' +
                    ", description='" + description + '\'' +
                    ", policyStore='" + policyStore + '\'' +
                    ", jansUsrDN='" + jansUsrDN + '\'' +
                    ", jansStatus='" + jansStatus + '\'' +
                    ", creationDate=" + creationDate +
                    ", jansLastUpd=" + jansLastUpd +
                    '}';
        }
    }
