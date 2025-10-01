/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jwt;

/**
 * @author Javier Rojas Blum
 * @version September 30, 2021
 */
public final class JwtClaimName {

    // JWT
    /**
     * Expiration time on or after which the ID Token must not be accepted for processing.
     * The processing of this parameter requires that the current date/time must be before
     * the expiration date/time listed in the value.
     */
    public static final String EXPIRATION_TIME = "exp"; // ID Token
    public static final String NOT_BEFORE = "nbf";
    /**
     * Time at which the JWT was issued. Its value is a JSON number representing the number
     * of seconds from 1970-01-01T0:0:0Z as measured in UTC until the date/time.
     */
    public static final String ISSUED_AT = "iat"; // ID Token
    /**
     * Issuer Identifier for the Issuer of the response.
     * The iss value is a case sensitive URL using the https scheme that contains scheme,
     * host, and optionally, port number and path components and no query or fragment components.
     */
    public static final String ISSUER = "iss"; // ID Token
    /**
     * Audience(s) that this ID Token is intended for.
     * It must contain the OAuth 2.0 client_id of the Relying Party as an audience value.
     * It may also contain identifiers for other audiences. In the general case, the aud
     * value is an array of case sensitive strings.
     * In the common special case when there is one audience, the aud value may be a single
     * case sensitive string.
     */
    public static final String AUDIENCE = "aud"; // ID Token
    public static final String PRINCIPAL = "prn";
    public static final String JWT_ID = "jti";
    public static final String TYPE = "typ";

    /**
     * Authentication Methods References.
     * <p>
     * JSON array of strings that are identifiers for authentication methods used in the authentication.
     * For instance, values might indicate that both password and OTP authentication methods were used.
     * The definition of particular values to be used in the amr Claim is beyond the scope of this specification.
     * Parties using this claim will need to agree upon the meanings of the values used, which may be context-specific.
     * The amr value is an array of case sensitive strings.
     */
    public static final String AUTHENTICATION_METHOD_REFERENCES = "amr";

    /**
     * A locally unique and never reassigned identifier within the Issuer for the End-User,
     * which is intended to be consumed by the Client.
     */
    public static final String SUBJECT_IDENTIFIER = "sub"; // User Info

    public static final String TOKEN_BINDING_HASH = "tbh"; // token binding hash

    public static final String CNF = "cnf";
    public static final String JKT = "jkt";

    /**
     * Authorized party - the party to which the ID Token was issued.
     * If present, it must contain the OAuth 2.0 Client ID of this party.
     * This Claim is only needed when the ID Token has a single audience value and that
     * audience is different than the authorized party.
     * It may be included even when the authorized party is the same as the sole audience.
     */
    public static final String AUTHORIZED_PARTY = "azp"; // ID Token
    /**
     * Authentication Context Class Reference.
     * String specifying an Authentication Context Class Reference value that identifies the
     * Authentication Context Class that the authentication performed satisfied.
     */
    public static final String AUTHENTICATION_CONTEXT_CLASS_REFERENCE = "acr"; // ID Token
    /**
     * String value used to associate a Client session with an ID Token, and to mitigate replay attacks.
     * The value is passed through unmodified from the Authentication Request to the ID Token.
     * If present in the ID Token, Clients must verify that the nonce Claim Value is equal to the value
     * of the nonce parameter sent in the Authentication Request.
     * If present in the Authentication Request, Authorization Servers must include a nonce Claim in the
     * ID Token with the Claim Value being the nonce value sent in the Authentication Request.
     * Authorization Servers should perform no other processing on nonce values used.
     * The nonce value is a case sensitive string.
     */
    public static final String NONCE = "nonce";
    /**
     * Time when the End-User authentication occurred.
     * Its value is a JSON number representing the number of seconds from 1970-01-01T0:0:0Z
     * as measured in UTC until the date/time.
     * When a max_age request is made or when auth_time is requested as an Essential Claim,
     * then this Claim is required; otherwise, its inclusion is optional.
     */
    public static final String AUTHENTICATION_TIME = "auth_time";
    public static final String ACCESS_TOKEN_HASH = "at_hash";
    public static final String CODE_HASH = "c_hash";
    public static final String STATE_HASH = "s_hash";

    // User Info
    /**
     * End-User's full name in displayable form including all name parts.
     */
    public static final String NAME = "name";
    /**
     * Given name or first name of the End-User.
     */
    public static final String GIVEN_NAME = "given_name";
    /**
     * Surname or last name of the End-User.
     */
    public static final String FAMILY_NAME = "family_name";
    /**
     * Middle name of the End-User.
     */
    public static final String MIDDLE_NAME = "middle_name";
    /**
     * Casual name of the End-User.
     * For instance, a nickname value of Mike might be returned alongside a given_name value of Michael.
     */
    public static final String NICKNAME = "nickname";
    /**
     * Shorthand name that the End-User wishes to be referred to at the RP, such as janedoe or j.doe.
     */
    public static final String PREFERRED_USERNAME = "preferred_username";
    /**
     * URL of the End-User's profile page.
     */
    public static final String PROFILE = "profile";
    /**
     * URL of the End-User's profile picture.
     */
    public static final String PICTURE = "picture";
    /**
     * URL of the End-User's web page or blog.
     */
    public static final String WEBSITE = "website";
    /**
     * The End-User's preferred e-mail address.
     */
    public static final String EMAIL = "email";
    /**
     * The End-User's preferred userName.
     */
    public static final String USER_NAME = "user_name";
    /**
     * True if the End-User's e-mail address has been verified; otherwise false.
     */
    public static final String EMAIL_VERIFIED = "email_verified";
    /**
     * The End-User's gender: Values defined by this specification are female and male.
     * Other values MAY be used when neither of the defined values are applicable.
     */
    public static final String GENDER = "gender";
    /**
     * The End-User's birthday.
     */
    public static final String BIRTHDATE = "birthdate";
    /**
     * String from zoneinfo time zone database. For example, Europe/Paris or America/Los_Angeles.
     */
    public static final String ZONEINFO = "zoneinfo";
    /**
     * The End-User's locale, represented as a BCP47 (RFC5646) language tag.
     * This is typically an ISO 639-1 Alpha-2 (ISO639‑1) language code in lowercase and an ISO 3166-1 Alpha-2 (ISO3166‑1)
     * country code in uppercase, separated by a dash. For example, en-US or fr-CA.
     */
    public static final String LOCALE = "locale";
    /**
     * The End-User's preferred telephone number.
     * E.164 is RECOMMENDED as the format of this Claim. For example, +1 (425) 555-1212 or +56 (2) 687 2400.
     */
    public static final String PHONE_NUMBER = "phone_number";
    /**
     * True if the End-User's phone number has been verified; otherwise false. When this Claim Value is true,
     * this means that the OP took affirmative steps to ensure that this phone number was controlled by the
     * End-User at the time the verification was performed. The means by which a phone number is verified is
     * context-specific, and dependent upon the trust framework or contractual agreements within which the
     * parties are operating. When true, the phone_number Claim MUST be in E.164 format and any extensions
     * MUST be represented in RFC 3966 format.
     */
    public static final String PHONE_NUMBER_VERIFIED = "phone_number_verified";
    /**
     * The End-User's preferred address.
     */
    public static final String ADDRESS = "address";
    /**
     * Time the End-User's information was last updated.
     */
    public static final String UPDATED_AT = "updated_at";
    /**
     * The full mailing address, formatted for display or use with a mailing label.
     */
    public static final String ADDRESS_FORMATTED = "formatted";
    /**
     * The full street address component, which may include house number, street name, PO BOX,
     * and multi-line extended street address information.
     */
    public static final String ADDRESS_STREET_ADDRESS = "street_address";
    /**
     * The city or locality component.
     */
    public static final String ADDRESS_LOCALITY = "locality";
    /**
     * The state, province, prefecture or region component.
     */
    public static final String ADDRESS_REGION = "region";
    /**
     * The zip code or postal code component.
     */
    public static final String ADDRESS_POSTAL_CODE = "postal_code";
    /**
     * The country name component.
     */
    public static final String ADDRESS_COUNTRY = "country";

    // Custom attributes
    public static final String JANS_OPENID_CONNECT_VERSION = "jansOpenIDConnectVersion";

    // CIBA
    public static final String REFRESH_TOKEN_HASH = "urn:openid:params:jwt:claim:rt_hash";
    public static final String AUTH_REQ_ID = "urn:openid:params:jwt:claim:auth_req_id";

    /**
     * The caller references the constants using <tt>JwtClaimName.TYPE</tt>,
     * and so on. Thus, the caller should be prevented from constructing objects of
     * this class, by declaring this private constructor.
     */
    private JwtClaimName() {
        // this prevents even the native class from calling this constructor as well
        throw new AssertionError();
    }
}
