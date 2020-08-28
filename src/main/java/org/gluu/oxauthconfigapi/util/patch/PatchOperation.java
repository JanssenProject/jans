package org.gluu.oxauthconfigapi.util.patch; 

import org.json.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;

import java.io.StringReader;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauthconfigapi.rest.model.AuthConfiguration;

public class PatchOperation {
	
	
	private String operation;
	private String path;
	private JSONObject value;

	
	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public JSONObject getValue() {
		return value;
	}

	public void setValue(JSONObject value) {
		this.value = value;
	}

	public JsonPatch createJsonPatch()  throws Exception {
		//JsonPatch jsonPatch = JsonPatch.fromJson(this.jsonNode);
		JsonPatch jsonPatch = null;
		System.out.println("\n\n\n jsonPatch = "+jsonPatch+"\n\n\n");
		return jsonPatch;
		
	}
	
	public AuthConfiguration applyPatchToConfig(JsonPatch jsonPatch,AuthConfiguration authConfig) throws JsonPatchException,JsonProcessingException,Exception{
		ObjectMapper objectMapper = new ObjectMapper();	
	    JsonNode patched = jsonPatch.apply(objectMapper.convertValue(authConfig, JsonNode.class));
	    return objectMapper.treeToValue(patched, AuthConfiguration.class);
	}

	@Override
	public String toString() {
		return "PatchOperation [operation=" + operation + ", path=" + path + ", value=" + value + "]";
	}
	
	
	
	
}
