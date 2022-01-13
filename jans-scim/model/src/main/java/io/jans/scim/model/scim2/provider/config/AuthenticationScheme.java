package io.jans.scim.model.scim2.provider.config;

import io.jans.scim.model.scim2.annotations.Attribute;
import io.jans.scim.model.scim2.AttributeDefinition;

/**
 * This class represents the <code>authenticationSchemes</code> complex attribute in the Service Provider Config
 * (see section 5 of RFC 7643).
 */
public class AuthenticationScheme {

    @Attribute(description = "The authentication scheme.",
            isRequired = true,
            canonicalValues = {"oauth", "oauth2", "oauthbearertoken", "httpbasic", "httpdigest", "uma"},
            mutability = AttributeDefinition.Mutability.READ_ONLY)
    private String type;

    @Attribute(description = "The common authentication scheme name, e.g., HTTP Basic.",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.READ_ONLY)
	private String name;

    @Attribute(description = "A description of the authentication scheme.",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.READ_ONLY)
	private String description;

    @Attribute(description = "An HTTP-addressable URL pointing to the authentication scheme's specification.",
            mutability = AttributeDefinition.Mutability.READ_ONLY)
	private String specUri;

    @Attribute(description = "An HTTP-addressable URL pointing to the authentication scheme's usage documentation.",
            mutability = AttributeDefinition.Mutability.READ_ONLY)
	private String documentationUri;

    @Attribute(description = "Whether it's the preferred authentication scheme for service usage",
            mutability = AttributeDefinition.Mutability.READ_ONLY,
            type = AttributeDefinition.Type.BOOLEAN)
    private boolean primary;

    /**
     * Creates an instance of AuthenticationScheme with all its fields unassigned.
     */
    public AuthenticationScheme(){}

	/**
	 * Creates an instance of AuthenticationScheme using the parameter values passed.
	 * @param name The common authentication scheme name.
	 * @param description The description of the authentication scheme.
	 * @param specUri An HTTP addressable URL pointing to the authentication scheme's specification.
	 * @param documentationUri An HTTP addressable URL pointing to the authentication scheme's usage documentation.
	 * @param type The type of authentication scheme, e.g. "oauthbearertoken", "httpbasic", etc.
     * @param primary A boolean value specifying if current scheme is the preference
	 */
	public AuthenticationScheme(String name, String description, String specUri,
                                String documentationUri, String type, boolean primary) {
		this.name = name;
		this.description = description;
		this.specUri = specUri;
		this.documentationUri = documentationUri;
		this.type = type;
		this.primary=primary;
	}

	/**
	 * Retrieves the name of the authentication scheme.
	 * @return The name of the authentication scheme.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves the description of the authentication scheme.
	 * @return The description of the Authentication Scheme.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Retrieves the HTTP URL of the authentication scheme's specification.
	 * @return A string representing a URL
	 */
	public String getSpecUri() {
		return specUri;
	}

	/**
	 * Retrieves the HTTP URL pointing of the authentication scheme's usage documentation.
     * @return A string representing a URL
	 */
	public String getDocumentationUri() {
		return documentationUri;
	}

	/**
	 * Retrieves the type of authentication scheme.
	 * @return The type of authentication scheme.
	 */
	public String getType() {
		return type;
	}

    /**
     * Whether this AuthenticationScheme is the preferred authentication scheme for service usage
     * @return A boolean value
     */
    public boolean isPrimary() {
        return primary;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSpecUri(String specUri) {
        this.specUri = specUri;
    }

    public void setDocumentationUri(String documentationUri) {
        this.documentationUri = documentationUri;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    /**
	 * Convenience method that creates a new AuthenticationScheme instance of type HTTP BASIC.
     * @param primary A boolean value for the "primary" field of object
	 * @return An AuthenticationScheme object
	 */
	public static AuthenticationScheme createBasic(boolean primary) {
		return new AuthenticationScheme(
				"Http Basic",
				"The HTTP Basic Access Authentication scheme. This scheme is not "
						+ "considered to be a secure method of user authentication (unless "
						+ "used in conjunction with some external secure system such as "
						+ "SSL), as the user name and password are passed over the network "
						+ "as cleartext.",
				"http://www.ietf.org/rfc/rfc2617.txt",
				"http://en.wikipedia.org/wiki/Basic_access_authentication",
				"httpbasic", primary);
	}

	/**
	 * Convenience method that creates a new AuthenticationScheme instances of type OAuth 2.
     * @param primary A boolean value for the "primary" field of object
     * @return An AuthenticationScheme object
	 */
	public static AuthenticationScheme createOAuth2(boolean primary) {
		return new AuthenticationScheme(
				"OAuth 2.0", "OAuth2 Bearer Token Authentication Scheme.",
				"http://tools.ietf.org/html/rfc6749", "http://tools.ietf.org/html/rfc6749",
				"oauth2", primary);
	}

}
