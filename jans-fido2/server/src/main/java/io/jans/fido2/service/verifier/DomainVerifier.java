/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

/*
 * Copyright (c) 2018 Mastercard
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package io.jans.fido2.service.verifier;

import java.net.MalformedURLException;
import java.net.URL;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.jans.fido2.exception.Fido2RpRuntimeException;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

@ApplicationScoped
public class DomainVerifier {

    @Inject
    private Logger log;
    
    @Inject
    private CommonVerifiers commonVerifiers;

    public boolean verifyDomain(String domain, JsonNode clientDataNode) {
    	String clientDataOrigin = commonVerifiers.verifyThatFieldString(clientDataNode, "origin");
        // a hack, there is a problem when we are sending https://blah as rp.id
        // which is sent to us from the browser in let rpid = window.location.origin;
        // so instead we are using
        // let rpid = document.domain;
        // but then clientDataOrigin is https://

        log.debug("Domains comparison {} {}", domain, clientDataOrigin);
        //Notes wrt #1114:
        // "The RP ID of a public key credential determines its scope. I.e., it determines the set of origins on which the
        //  public key credential may be exercised"
        // See https://www.w3.org/TR/webauthn/#scope, https://html.spec.whatwg.org/multipage/origin.html, and
        // https://url.spec.whatwg.org/#hosts-(domains-and-ip-addresses)

        // It is assumed variable domain contains RP ID, and clientDataOrigin is the relying party origin as sent by user agent
        // We check here if RP ID is a registrable domain suffix of the clientDataOrigin's effective domain or if it is
        // equal to clientDataOrigin's effective domain

        String effectiveDomain;
        try {
            effectiveDomain = new URL(clientDataOrigin).getHost();
        } catch (MalformedURLException e) {
            //clientDataOrigin does not conform to tuple origin syntax! assuming it contains the 4th tuple element, ie. domain
            effectiveDomain = clientDataOrigin;
        }

        if (!domain.equals(effectiveDomain)) {
            //Check registrable domain suffix rule
            if (!effectiveDomain.endsWith("." + domain)) {
                throw new Fido2RpRuntimeException("Domains don't match");
            }
        }

        return true;
    }

}
