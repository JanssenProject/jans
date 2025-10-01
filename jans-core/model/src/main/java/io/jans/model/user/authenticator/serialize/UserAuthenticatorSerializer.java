package io.jans.model.user.authenticator.serialize;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.jans.model.user.authenticator.UserAuthenticator;
import io.jans.model.user.authenticator.UserAuthenticatorList;

/**
 * User authenticators serializer
 *
 * @author Yuriy Movchan Date: 03/28/2024
 */
public class UserAuthenticatorSerializer extends StdSerializer<UserAuthenticatorList> {

    public UserAuthenticatorSerializer() {
        this(null);
    }
  
    public UserAuthenticatorSerializer(Class<UserAuthenticatorList> t) {
        super(t);
    }

	private static final long serialVersionUID = -316694375728956632L;

	@Override
	public void serialize(UserAuthenticatorList value, JsonGenerator gen, SerializerProvider provider)
			throws IOException {
		gen.writeStartObject();

		if (value.isEmpty()) {
	        gen.writeEndObject();
			return;
		}
		
		for (UserAuthenticator authenticator : value.getAuthenticators()) {
	        gen.writeObjectField(authenticator.getId(), authenticator);
		}

		gen.writeEndObject();
	}
}
