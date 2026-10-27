

package io.jans.configapi.core.model.role;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;


@DataEntry
@ObjectClass(value = "jansAppConf")
public class RolePermissionMappingConf {
    @DN
    private String dn;

    @JsonObject
    @AttributeName(name = "jansConfDyn")
    private DynamicConfig dynamic;

    @AttributeName(name = "jansRevision")
    private long revision;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }

    public DynamicConfig getDynamic() {
        return dynamic;
    }

    public void setDynamic(DynamicConfig dynamic) {
        this.dynamic = dynamic;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Conf");
        sb.append("{dn='").append(dn).append('\'');
        sb.append(", dynamic='").append(dynamic).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
