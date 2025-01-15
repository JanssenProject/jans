package io.jans.fido2.service.util;

import io.jans.fido2.model.attestation.AttestationErrorResponseType;
import io.jans.fido2.model.error.ErrorResponseFactory;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.kerby.asn1.parse.Asn1Container;
import org.apache.kerby.asn1.parse.Asn1ParseResult;
import org.apache.kerby.asn1.parse.Asn1Parser;
import org.apache.kerby.asn1.type.Asn1OctetString;

import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.cert.X509Certificate;

@ApplicationScoped
public class AppleUtilService {

    private static final String KEY_DESCRIPTION_OID = "1.2.840.113635.100.8.2";

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    /*-
	[
	   {
	       "type": "OBJECT_IDENTIFIER",
	       "data": "1.2.840.113635.100.8.2"
	   },
	   {
	       "type": "OCTET_STRING",
	       "data": [
	           {
	               "type": "SEQUENCE",
	               "data": [
	                   {
	                       "type": "[1]",
	                       "data": [
	                           {
	                               "type": "OCTET_STRING",
	                               "data": {
	                                   "type": "Buffer",
	                                   "data": [92, 219, 157, 144, 115, 64, 69, 91, 99, 115, 230, 117, 43, 115, 252, 54, 132, 83, 96, 34, 21, 250, 234, 187, 124, 22, 95, 11, 173, 172, 7, 204]
	                               }
	                           }
	                       ]
	                   }
	               ]
	           }
	       ]
	   }
	]
	*/
    public byte[] getExtension(X509Certificate attestationCert) {
        byte[] extensionValue = attestationCert.getExtensionValue(KEY_DESCRIPTION_OID);
        byte[] extracted;
        try {
            Asn1OctetString extensionEnvelope = new Asn1OctetString();
            extensionEnvelope.decode(extensionValue);
            byte[] extensionEnvelopeValue = extensionEnvelope.getValue();
            Asn1Container container = (Asn1Container) Asn1Parser.parse(ByteBuffer.wrap(extensionEnvelopeValue));
            Asn1ParseResult firstElement = container.getChildren().get(0);
            Asn1OctetString octetString = new Asn1OctetString();
            octetString.decode(firstElement);
            extracted = octetString.getValue();
            return extracted;
        } catch (IOException | RuntimeException e) {
            throw errorResponseFactory.badRequestException(AttestationErrorResponseType.APPLE_ERROR, "Failed to extract nonce from Apple anonymous attestation statement.");
        }
    }
}
