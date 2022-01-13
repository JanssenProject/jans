/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

/**
 * An enumeration of all possible (formatting) validations applicable to attributes of SCIM resources.
 */
/*
 * Created by jgomer on 2017-09-15.
 */
public enum Validations {EMAIL, PHONE, PHOTO, COUNTRY, LOCALE, TIMEZONE;
    //Supporting X.509 validation would add unnecessary overhead...

    private static final Pattern EmailPattern = Pattern.compile("^.+@.+$");

    private static Set<String> validCountries;
    private static Set<String> validTimeZones;

    static {
        validCountries=new HashSet<>(Arrays.asList(Locale.getISOCountries()));
        validTimeZones=new HashSet<>(Arrays.asList(TimeZone.getAvailableIDs()));
    }

    /**
     * This method receives a validation "type" and the object upon which to perform the validation.
     * @param validation An {@link Validations enum constant} that specifies which validation should be applied
     * @param value A non-null object target of validation
     * @return A boolean value (true/false) for success/failed validation.
     */
    public static boolean apply(Validations validation, Object value){

        boolean pass=false;
        switch (validation){
            case EMAIL:
                pass=validateEmail(value.toString());
                //From spec: The value SHOULD be specified according to [RFC5321].
                break;
            case PHONE:
                pass=true;
                /*
                From spec: The value SHOULD be specified according to the format defined in [RFC3966], e.g.,
                'tel:+1-201-555-0123'.
                IMPORTANT NOTE: Implementing this validation will rise compatibility problems in terms of data current customers
                handle. More info at https://support.gluu.org/identity-management/5577/scim-address-validator-configurable-validators
                 */
                break;
            case PHOTO:
                pass=validateURI(value.toString());
                //From spec: A URI that is a uniform resource locator (as defined in Section 1.1.3 of [RFC3986])
                break;
            case COUNTRY:
                pass=validateCountry(value.toString());
                //From spec: When specified, the value MUST be in ISO 3166-1 "alpha-2" code format [ISO3166]
                break;
            /*
            case X509:
                pass=true;
                //From spec: Each value contains exactly one DER-encoded X.509 certificate (see Section 4 of [RFC5280])
                break;
                */
            case LOCALE:
                pass=validateLocale(value.toString());
                /*
                From spec: A valid value is a language tag as defined in [RFC5646]. Computer languages are explicitly
                excluded.
                 */
                break;
            case TIMEZONE:
                pass=validateTimezone(value.toString());
                /*
                From spec:  The User's time zone, in IANA Time Zone database format [RFC6557], also known as the "Olson"
                time zone database format [Olson-TZ] (e.g., "America/Los_Angeles").
                 */
                break;
        }
        return pass;

    }

    private static boolean validateEmail(String val){
        return EmailPattern.matcher(val).matches();
    }

    private static boolean validateCountry(String val){
        return validCountries.contains(val.toUpperCase());
    }

    private static boolean validateTimezone(String val){
        return validTimeZones.contains(val);
    }

    private static boolean validateURI(String val){

        boolean valid=true;
        try {
            new URI(val);
        }
        catch (Exception e){
            valid=false;
        }
        return valid;

    }

    private static boolean validateLocale(String val) {

        val = val.replaceAll("_", "-");
        try {
            new Locale.Builder().setLanguageTag(val);
            return true;
        } catch (Exception e) {
            return false;
        }

    }

}
