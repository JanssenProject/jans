package io.jans.model.user.authenticator.serialize;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import io.jans.model.user.authenticator.UserAuthenticator;
import io.jans.model.user.authenticator.UserAuthenticatorList;

/**
 * User authenticators deserializer
 *
 * @author Yuriy Movchan Date: 03/28/2024
 */
public class UserAuthenticatorDeserializer extends StdDeserializer<UserAuthenticatorList> {

    public UserAuthenticatorDeserializer() {
        this(null);
    }
  
    public UserAuthenticatorDeserializer(Class<UserAuthenticatorList> t) {
        super(t);
    }

	private static final long serialVersionUID = -316694375728956632L;

	@Override
	public UserAuthenticatorList deserialize(JsonParser p, DeserializationContext ctxt)
			throws IOException, JacksonException {

		UserAuthenticatorList userAuthenticatorList = new UserAuthenticatorList();

		String id = null;
		while (p.nextToken() != JsonToken.END_OBJECT) {
			JsonToken token = p.getCurrentToken();

			if (token == JsonToken.FIELD_NAME) {
				id = p.getCurrentName();
			} else if (token == JsonToken.START_OBJECT) {
				UserAuthenticator userAuthenticator = ctxt.readValue(p, UserAuthenticator.class);
				userAuthenticator.setId(id);

				userAuthenticatorList.addAuthenticator(userAuthenticator);
			}
		}

		return userAuthenticatorList;
	}

}
