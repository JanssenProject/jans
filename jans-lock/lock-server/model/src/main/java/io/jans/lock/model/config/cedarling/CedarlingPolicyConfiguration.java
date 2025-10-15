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

package io.jans.lock.model.config.cedarling;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.jans.doc.annotation.DocProperty;
import io.jans.lock.model.config.Configuration;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.enterprise.inject.Vetoed;

/**
 * Janssen Project Lock Policy configuration
 *
 * @author Yuriy Movchan Date: 12/12/2023
 */
@Vetoed
@JsonDeserialize(using = CedarlingPolicyConfigurationDeserializer.class)
@JsonSerialize(using = CedarlingPolicyConfigurationSerializer.class)
/*
 * Class wrapper to store unescaped JSON 
 */
public class CedarlingPolicyConfiguration implements Configuration {

	@DocProperty(description = "Lock Cedarling policy store")
	@Schema(description = "Lock Cedarling policy store")
	private String policy;

	public String getPolicy() {
		return policy;
	}

	public void setPolicy(String policy) {
		this.policy = policy;
	}

	@Override
	public String toString() {
		return "CedarlingPolicyConfiguration [policy=" + policy + "]";
	}

}

class CedarlingPolicyConfigurationDeserializer extends JsonDeserializer<CedarlingPolicyConfiguration> {
	@Override
	public CedarlingPolicyConfiguration deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		CedarlingPolicyConfiguration res = new CedarlingPolicyConfiguration();

		// Validates JSON integrity and store in string property
		JsonNode rootNode = p.getCodec().readTree(p);
		res.setPolicy(rootNode.toString());

		return res;
	}
}

class CedarlingPolicyConfigurationSerializer extends JsonSerializer<CedarlingPolicyConfiguration> {
	@Override
	public void serialize(CedarlingPolicyConfiguration value, JsonGenerator gen, SerializerProvider serializers)
			throws IOException {
		gen.writeRawValue(value.getPolicy());
	}
}
