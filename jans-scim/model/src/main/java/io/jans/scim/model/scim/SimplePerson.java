package io.jans.scim.model.scim;

import io.jans.model.user.SimpleUser;
import io.jans.orm.annotation.*;

@ObjectClass("jansPerson")
public class SimplePerson extends SimpleUser {

	private static final long serialVersionUID = 757862310049638710L;

	@Version
	@AttributeName(name = "jansVersion", ignoreDuringUpdate = true)
	private Long version;

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	// version is concurrency metadata, not identity: keep the inherited equality
	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

}
