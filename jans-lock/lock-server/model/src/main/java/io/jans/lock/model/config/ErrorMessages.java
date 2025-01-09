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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.model.error.ErrorMessage;
import jakarta.enterprise.inject.Vetoed;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Base interface for all Jans Auth configurations
 *
 * @author Yuriy Movchan Date: 12/18/2023
 */
@Vetoed
@XmlRootElement(name = "errors")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorMessages implements Configuration {

    @XmlElementWrapper(name = "common")
    @XmlElement(name = "error")
    private List<ErrorMessage> common;

    @XmlElementWrapper(name = "stat")
    @XmlElement(name = "error")
    private List<ErrorMessage> stat;

    public List<ErrorMessage> getCommon() {
        return common;
    }

    public void setCommon(List<ErrorMessage> common) {
        this.common = common;
    }

	public List<ErrorMessage> getStat() {
		return stat;
	}

	public void setStat(List<ErrorMessage> stat) {
		this.stat = stat;
	}

}
