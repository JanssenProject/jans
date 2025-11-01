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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.enterprise.inject.Vetoed;

/**
 * 
 * @author Yuriy Movchan Date: 12/12/2023
 */
@Vetoed
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseDnConfiguration {
	private String configuration;

	private String people;
	private String attributes;
    private String sessions;
    private String tokens;
	private String scripts;
	private String metric;
	private String stat;
	private String audit;

	/**
	 * Gets the base DN for configuration entries.
	 *
	 * @return the configuration base DN, or null if not set
	 */
	public String getConfiguration() {
		return configuration;
	}

	public void setConfiguration(String configuration) {
		this.configuration = configuration;
	}

	public String getAttributes() {
		return attributes;
	}

	public void setAttributes(String attributes) {
		this.attributes = attributes;
	}

	public String getSessions() {
		return sessions;
	}

	public void setSessions(String sessions) {
		this.sessions = sessions;
	}

	public String getTokens() {
		return tokens;
	}

	public void setTokens(String tokens) {
		this.tokens = tokens;
	}

	public String getScripts() {
		return scripts;
	}

	public void setScripts(String scripts) {
		this.scripts = scripts;
	}

	public String getPeople() {
		return people;
	}

	public void setPeople(String people) {
		this.people = people;
	}

	public String getMetric() {
		return metric;
	}

	public void setMetric(String metric) {
		this.metric = metric;
	}

	public String getStat() {
		return stat;
	}

	/**
	 * Sets the base DN for statistics entries.
	 *
	 * @param stat the base DN to use for statistics
	 */
	public void setStat(String stat) {
		this.stat = stat;
	}

	/**
	 * Gets the base DN for audit entries.
	 *
	 * @return the audit base DN, or {@code null} if not set
	 */
	public String getAudit() {
		return audit;
	}

	/**
	 * Sets the audit base DN.
	 *
	 * @param audit the base DN for audit entries
	 */
	public void setAudit(String audit) {
		this.audit = audit;
	}

}