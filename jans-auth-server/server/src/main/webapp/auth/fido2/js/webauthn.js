// Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
//
// Copyright (c) 2020, Janssen Project

const authAbortController = new AbortController();

(function(root, factory) {
  if (typeof define === 'function' && define.amd) {
    define(['base64url'], factory);
  } else if (typeof module === 'object' && module.exports) {
    module.exports = factory(require('base64url'));
  } else {
    root.webauthn = factory(root.base64url);
  }
})(this, function(base64url) {

  function extend(obj, more) {
    return Object.assign({}, obj, more);
  }

  /**
   * Create a WebAuthn credential.
   *
   * @param request: object - A PublicKeyCredentialCreationOptions object, except
   *   where binary values are base64url encoded strings instead of byte arrays
   *
   * @return a PublicKeyCredentialCreationOptions suitable for passing as the
   *   `publicKey` parameter to `navigator.credentials.create()`
   */
  function decodePublicKeyCredentialCreationOptions(request) {
	console.log("Request : "+request);
	console.log("JSON.stringify request: "+ JSON.stringify(request));
	
    const excludeCredentials = request.excludeCredentials.map(credential => extend(
      credential, {
      id: base64url.toByteArray(credential.id),
    }));

	// Default authenticatorSelection if not specified
	const defaultAuthenticatorSelection = {
		authenticatorAttachment: 'platform', // or 'cross-platform' 
		userVerification: 'preferred',      // 'required' or 'preferred'
		requireResidentKey: false           // set to true if you require a resident key
	};

	// Use provided authenticatorSelection if available, otherwise use default
	const authenticatorSelection = request.authenticatorSelection || defaultAuthenticatorSelection;


    const publicKeyCredentialCreationOptions = extend(
      request, {
      attestation: 'direct',
      user: extend(
        request.user, {
        id: base64url.toByteArray(request.user.id),
      }),
      challenge: base64url.toByteArray(request.challenge),
      excludeCredentials,
	  authenticatorSelection,
    });

    return publicKeyCredentialCreationOptions;
  }

  /**
   * Create a WebAuthn credential.
   *
   * @param request: object - A PublicKeyCredentialCreationOptions object, except
   *   where binary values are base64url encoded strings instead of byte arrays
   *
   * @return the Promise returned by `navigator.credentials.create`
   */
  function createCredential(request) {
    return navigator.credentials.create({
      publicKey: decodePublicKeyCredentialCreationOptions(request),
    });
  }

  /**
   * Perform a WebAuthn assertion.
   *
   * @param request: object - A PublicKeyCredentialRequestOptions object,
   *   except where binary values are base64url encoded strings instead of byte
   *   arrays
   *
   * @return a PublicKeyCredentialRequestOptions suitable for passing as the
   *   `publicKey` parameter to `navigator.credentials.get()`
   */
  function decodePublicKeyCredentialRequestOptions(request) {
    const allowCredentials = request.allowCredentials && request.allowCredentials.map(credential => extend(
      credential, {
      id: base64url.toByteArray(credential.id),
    }));

	// Default authenticatorSelection for assertion requests
	const defaultAuthenticatorSelection = {
		authenticatorAttachment: 'platform', // or 'cross-platform' based on your requirement
		userVerification: 'preferred',      // 'required' or 'preferred'
	};
	// Use provided authenticatorSelection if available, otherwise use default
	const authenticatorSelection = request.authenticatorSelection || defaultAuthenticatorSelection;


    const publicKeyCredentialRequestOptions = extend(
      request, {
      allowCredentials,
      challenge: base64url.toByteArray(request.challenge),
	  authenticatorSelection, 
    });

    return publicKeyCredentialRequestOptions;
  }

  /**
   * Perform a WebAuthn assertion.
   *
   * @param request: object - A PublicKeyCredentialRequestOptions object,
   *   except where binary values are base64url encoded strings instead of byte
   *   arrays
   *
   * @return the Promise returned by `navigator.credentials.get`
   */
  function getAssertion(request) {
    console.log('Get assertion', request);
    return navigator.credentials.get({
      publicKey: decodePublicKeyCredentialRequestOptions(request),
    });
  }

 function getAssertionConditional(request) {
	
	const authAbortSignal = authAbortController.signal;
			
    console.log('Get assertion conditional', request);
    return navigator.credentials.get({
       publicKey: decodePublicKeyCredentialRequestOptions(request),
	   mediation: "conditional",
	   signal : authAbortSignal,
    });
  }

  /** Turn a PublicKeyCredential object into a plain object with base64url encoded binary values */
  function responseToObject(response) {

    let clientExtensionResults = {};

    try {
      clientExtensionResults = response.getClientExtensionResults();
    } catch (e) {
      console.error('getClientExtensionResults failed', e);
    }
	console.log("Response : "+response);
	console.log("JSON.stringify: "+ JSON.stringify(response));
	
    if (response.response.attestationObject) {
      return {
        type: response.type,
        id: response.id,
		rawId: base64url.fromByteArray(response.rawId),
        response: {
          attestationObject: base64url.fromByteArray(response.response.attestationObject),
		  authenticatorData: base64url.fromByteArray(response.response.getAuthenticatorData()),
          clientDataJSON: base64url.fromByteArray(response.response.clientDataJSON),
		  publicKey : base64url.fromByteArray(response.response.getPublicKey()),
		  publicKeyAlgorithm : response.response.getPublicKeyAlgorithm(),
		  transports : response.response.getTransports(),
        },
        clientExtensionResults,
		authenticatorAttachment : response.authenticatorAttachment,
		
      };
    } else {
      return {
        type: response.type,
        id: response.id,
        rawId: base64url.fromByteArray(response.rawId),
        response: {
          authenticatorData: base64url.fromByteArray(response.response.authenticatorData),
          clientDataJSON: base64url.fromByteArray(response.response.clientDataJSON),
          signature: base64url.fromByteArray(response.response.signature),
          userHandle: response.response.userHandle && base64url.fromByteArray(response.response.userHandle),
        },
        clientExtensionResults,
		authenticatorAttachment : response.authenticatorAttachment,
      };
    }
  }

  return {
    decodePublicKeyCredentialCreationOptions,
    decodePublicKeyCredentialRequestOptions,
    createCredential,
    getAssertion,
	getAssertionConditional,
    responseToObject,
  };

});
