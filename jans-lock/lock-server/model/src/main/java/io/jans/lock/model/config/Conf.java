/*
 * Copyright [2024] [Janssen Project]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jans.lock.model.config;

import io.jans.lock.model.config.cedarling.CedarlingPolicyConfiguration;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import jakarta.enterprise.inject.Vetoed;

/**
 * 
 * @author Yuriy Movchan Date: 12/12/2023
 */
@Vetoed
@DataEntry
@ObjectClass(value = "jansAppConf")
public class Conf {
	@DN
	private String dn;

	@JsonObject
	@AttributeName(name = "jansConfDyn")
	private AppConfiguration dynamic;

	@JsonObject
	@AttributeName(name = "jansConfPolicy")
	private CedarlingPolicyConfiguration policyConfiguration;

	@JsonObject
	@AttributeName(name = "jansConfStatic")
	private StaticConfiguration statics;

	@JsonObject
	@AttributeName(name = "jansConfErrors")
	private ErrorMessages errors;

	@AttributeName(name = "jansRevision")
	private long revision;

	public String getDn() {
		return dn;
	}

	public void setDn(String dn) {
		this.dn = dn;
	}

	public AppConfiguration getDynamic() {
		return dynamic;
	}

	public void setDynamic(AppConfiguration dynamic) {
		this.dynamic = dynamic;
	}

	public CedarlingPolicyConfiguration getPolicyConfiguration() {
		return policyConfiguration;
	}

	public void setPolicyConfiguration(CedarlingPolicyConfiguration policyConfiguration) {
		this.policyConfiguration = policyConfiguration;
	}

	public StaticConfiguration getStatics() {
		return statics;
	}

	public void setStatics(StaticConfiguration statics) {
		this.statics = statics;
	}

	public ErrorMessages getErrors() {
		return errors;
	}

	public void setErrors(ErrorMessages errors) {
		this.errors = errors;
	}

	public long getRevision() {
		return revision;
	}

	public void setRevision(long revision) {
		this.revision = revision;
	}

	@Override
	public String toString() {
		return "Conf [dn=" + dn + ", dynamic=" + dynamic + ", policyConfiguration=" + policyConfiguration + ", statics="
				+ statics + ", errors=" + errors + ", revision=" + revision + "]";
	}
}
