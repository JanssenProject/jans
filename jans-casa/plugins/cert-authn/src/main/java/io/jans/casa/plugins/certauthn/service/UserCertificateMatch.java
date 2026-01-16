package io.jans.casa.plugins.certauthn.service;

public enum UserCertificateMatch {
    SUCCESS,
    CERT_ENROLLED_ALREADY,  //Applies to enrollment scenario only
    CERT_ENROLLED_OTHER_USER,
    CERT_NOT_RECOGNIZED,    //Applies to authentication scenario only
    UNKNOWN_USER,
    UNKNOWN_ERROR
}
