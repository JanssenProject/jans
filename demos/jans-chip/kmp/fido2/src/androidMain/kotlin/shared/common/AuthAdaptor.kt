package shared.common

import android.content.Context
import android.util.Base64
import android.util.Log
import android.util.Pair
import com.example.fido2.authAdaptor.AuthenticationProvider
import com.example.fido2.model.KtPublicKeyCredentialSource
import com.example.fido2.model.fido.assertion.option.AssertionOptionResponse
import com.example.fido2.model.fido.assertion.result.AssertionResultRequest
import com.example.fido2.model.fido.assertion.result.Response
import com.example.fido2.model.fido.attestation.option.AttestationOptionResponse
import com.example.fido2.model.fido.attestation.result.AttestationResultRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import io.jans.webauthn.Authenticator
import io.jans.webauthn.models.AttestationObject
import io.jans.webauthn.models.AuthenticatorGetAssertionOptions
import io.jans.webauthn.models.AuthenticatorGetAssertionResult
import io.jans.webauthn.models.AuthenticatorMakeCredentialOptions
import io.jans.webauthn.models.PublicKeyCredentialDescriptor
import io.jans.webauthn.models.PublicKeyCredentialSource
import io.jans.webauthn.models.RpEntity
import io.jans.webauthn.models.UserEntity
import io.jans.webauthn.util.CredentialSafe
import io.jans.webauthn.util.CredentialSelector
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.Signature

class AuthenticationAdaptor(context: Context): AuthenticationProvider {

    private var authAdaptor: AuthAdaptor? = null

    init {
        authAdaptor = AuthAdaptor(context)
    }

    override fun isCredentialsPresent(username: String): Boolean {
        return authAdaptor?.isCredentialsPresent(username) == true
    }

    override fun getAllCredentials(): List<KtPublicKeyCredentialSource>? {
        return authAdaptor?.getAllCredentials()
    }

    override fun deleteAllKeys() {
        authAdaptor?.deleteAllCredentials()
    }

    override suspend fun register(
        responseFromAPI: AttestationOptionResponse?,
        origin: String?
    ): AttestationResultRequest? {
        // Generate a new credential
        var publicKeyCredentialSource = authAdaptor?.getPublicKeyCredentialSource(responseFromAPI, origin)

        //Generate a signature object
        val signature = authAdaptor?.generateSignature(publicKeyCredentialSource)

        return authAdaptor?.register(responseFromAPI, origin, publicKeyCredentialSource)
    }

    override suspend fun authenticate(
        assertionOptionResponse: AssertionOptionResponse,
        origin: String?
    ): AssertionResultRequest? {
        val selectedPublicKeyCredentialSource = authAdaptor?.selectPublicKeyCredentialSource(
            LocalCredentialSelector(),
            assertionOptionResponse,
            origin
        )
        return authAdaptor?.authenticate(assertionOptionResponse, origin, selectedPublicKeyCredentialSource)
    }
}

class AuthAdaptor(context: Context) {

    private var authenticator: Authenticator? = null
    private val TAG: String = AuthAdaptor::class.java.name
    var obtainedContext: Context = context

    init {
        authenticator = Authenticator(context, false, false)
    }

    fun getAllCredentials(): List<KtPublicKeyCredentialSource>? {
        val credentialSafe: CredentialSafe? = authenticator?.credentialSafe
        return credentialSafe?.allCredentialSource?.map {
            KtPublicKeyCredentialSource(
                it.roomUid,
                it.id,
                it.keyPairAlias,
                it.rpId,
                it.userHandle,
                it.userDisplayName,
                it.otherUI,
                it.keyUseCounter
            )
        }
    }

    fun deleteAllCredentials() {
        val credentialSafe: CredentialSafe? = authenticator?.credentialSafe
        credentialSafe?.deleteAllCredentials()
    }

    fun isCredentialsPresent(username: String): Boolean {
        val allCredentials = getAllCredentials()
        allCredentials?.forEach { ele ->
            if(ele.userDisplayName == username) {
                return true
            }
        }
        return false
    }

    private fun generateAuthenticatorMakeCredentialOptions(responseFromAPI: AttestationOptionResponse?, origin: String?): AuthenticatorMakeCredentialOptions {
        val options = AuthenticatorMakeCredentialOptions()
        options.rpEntity = RpEntity()
        options.rpEntity.id = responseFromAPI?.rp?.id
        options.rpEntity.name = responseFromAPI?.rp?.name
        options.userEntity = UserEntity()
        options.userEntity.id =
            responseFromAPI?.user?.id?.toByteArray()
        options.userEntity.name = responseFromAPI?.user?.name
        options.userEntity.displayName = responseFromAPI?.user?.displayName
        options.clientDataHash = generateClientDataHash(
            responseFromAPI?.challenge,
            "webauthn.create",
            origin
        )
        options.requireResidentKey = false
        options.requireUserPresence = true
        options.requireUserVerification = false
        options.excludeCredentialDescriptorList =
            java.util.ArrayList<PublicKeyCredentialDescriptor>()
        val credTypesAndPubKeyAlgs: MutableList<Pair<String, Long>> = ArrayList()
        val pair = Pair("public-key", -7L)
        credTypesAndPubKeyAlgs.add(pair)
        options.credTypesAndPubKeyAlgs = credTypesAndPubKeyAlgs
        return options
    }
    suspend fun register(
        responseFromAPI: AttestationOptionResponse?,
        origin: String?,
        credentialSource: PublicKeyCredentialSource?
    ): AttestationResultRequest? {
        val credentialSafe: CredentialSafe? = authenticator?.credentialSafe
        val creds: List<PublicKeyCredentialSource>? = credentialSafe?.allCredentialSource
        creds?.forEach { ele ->
            Log.d(TAG, "ele.id $ele.id")
            Log.d(TAG,"ele.rpId $ele.rpId")
            Log.d(TAG, "ele.userDisplayName $ele.userDisplayName")
        }
        try {
            val options = generateAuthenticatorMakeCredentialOptions(responseFromAPI, origin)
            val attestationObject: AttestationObject? =
                authenticator?.makeCredential(options, credentialSource, obtainedContext, null)
            val attestationObjectBytes: ByteArray? = attestationObject?.asCBOR()
            Log.d(TAG + "attestationObjectBytes :", urlEncodeToString(attestationObjectBytes))
            Log.d(TAG, urlEncodeToString(attestationObject?.credentialId).replace("\n", ""))
            Log.d(TAG, urlEncodeToString(attestationObject?.credentialId))

            val clientDataJSON = generateClientDataJSON(
                responseFromAPI?.challenge,
                "webauthn.create",
                origin
            )
            val attestationResponse = clientDataJSON?.let {
                com.example.fido2.model.fido.attestation.result.AttestationResponse(
                    urlEncodeToString(attestationObjectBytes).replace("\n", ""),
                    it
                )
            }

            val attestationResultRequest = attestationResponse?.let {
                AttestationResultRequest(
                    urlEncodeToString(attestationObject?.credentialId).replace("\n", "")
                        .replace("=", ""),
                    "public-key",
                    it
                )
            }
            attestationResultRequest?.id =
                urlEncodeToString(attestationObject?.credentialId).replace("\n", "")
                    .replace("=", "")
            attestationResultRequest?.type = "public-key"

            if (attestationResponse != null) {
                attestationResultRequest?.response = attestationResponse
            }
            return attestationResultRequest
        } catch (e: Exception) {
            val attestationResultRequest = AttestationResultRequest(null, null, null)
            attestationResultRequest.isSuccessful = false
            attestationResultRequest.errorMessage =
                "Error in making credential by Authenticator : ${e.message}"
            e.printStackTrace()
            return attestationResultRequest
        }
    }

    suspend fun getPublicKeyCredentialSource(responseFromAPI: AttestationOptionResponse?, origin: String?): PublicKeyCredentialSource? {
        val options = generateAuthenticatorMakeCredentialOptions(responseFromAPI, origin)
        return authenticator?.getPublicKeyCredentialSource(options)
    }

    suspend fun generateSignature(credentialSource: PublicKeyCredentialSource? ): Signature? {
        return authenticator?.generateSignature(credentialSource)
    }

    private fun generateClientDataHash(
        challenge: String?,
        type: String?,
        origin: String?
    ): ByteArray? {
        // Convert clientDataJson to JSON string
        val objectMapper = ObjectMapper()
        val clientData = objectMapper.createObjectNode()
        clientData.put("type", type)
        clientData.put("challenge", challenge)
        clientData.put("origin", origin)
        objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
        val serializedClientData = objectMapper.writeValueAsString(clientData)

        // Calculate SHA-256 hash
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(serializedClientData.toByteArray(StandardCharsets.UTF_8))
    }

    private fun urlEncodeToString(src: ByteArray?): String {
        return Base64.encodeToString(src, Base64.URL_SAFE)
    }

    private fun generateClientDataJSON(
        challenge: String?,
        type: String?,
        origin: String?
    ): String? {


        // Convert clientDataJson to JSON string
        val objectMapper = ObjectMapper()
        val clientData = objectMapper.createObjectNode()
        clientData.put("type", type)
        clientData.put("challenge", challenge)
        clientData.put("origin", origin)
        Log.d(
            TAG + "clientData.toString()",
            clientData.toString()
        )
        val clientDataJSON =
            urlEncodeToString(clientData.toString().toByteArray(StandardCharsets.UTF_8))
        Log.d(
            TAG + "clientDataJSON",
            clientDataJSON.replace("\n", "")
        )
        return clientDataJSON.replace("\n", "")
    }

    private fun decode(src: String?): ByteArray? {
        return Base64.decode(src, Base64.URL_SAFE)
    }

    private fun generateAuthenticatorGetAssertionOptions(assertionOptionResponse: AssertionOptionResponse?, origin: String?): AuthenticatorGetAssertionOptions {
        val options = AuthenticatorGetAssertionOptions()
        options.rpId = assertionOptionResponse?.rpId
        options.requireUserVerification = false
        options.requireUserPresence = true
        options.clientDataHash = generateClientDataHash(
            assertionOptionResponse?.challenge,
            "webauthn.get",
            origin
        )
        val allowCredentialDescriptorList: MutableList<PublicKeyCredentialDescriptor> =
            ArrayList<PublicKeyCredentialDescriptor>()
        assertionOptionResponse?.allowCredentials
            ?.forEach { cred ->
                Log.d(TAG, cred.id)
                val publicKeyCredentialDescriptor =
                    PublicKeyCredentialDescriptor(
                        cred.type,
                        decode(cred.id),
                        cred.transports
                    )
                allowCredentialDescriptorList.add(publicKeyCredentialDescriptor)
            }
        options.allowCredentialDescriptorList = allowCredentialDescriptorList
        return options
    }

    suspend fun authenticate(
        assertionOptionResponse: AssertionOptionResponse,
        origin: String?,
        selectedCredential: PublicKeyCredentialSource?
    ): AssertionResultRequest {
        try {

            val options: AuthenticatorGetAssertionOptions = generateAuthenticatorGetAssertionOptions(assertionOptionResponse, origin)
            val assertionObject: AuthenticatorGetAssertionResult? =
                authenticator?.getAssertion(options, selectedCredential, LocalCredentialSelector())

            val assertionResultRequest = AssertionResultRequest()
            assertionResultRequest.id =
                urlEncodeToString(assertionObject?.selectedCredentialId).replace(
                    "\n",
                    ""
                ).replace("=", "")

            assertionResultRequest.type = "public-key"
            assertionResultRequest.rawId =
                urlEncodeToString(assertionObject?.selectedCredentialId).replace(
                    "\n",
                    ""
                )

            val response: Response = Response()

            response.clientDataJSON = generateClientDataJSON(
                assertionOptionResponse.challenge,
                "webauthn.get",
                origin
            )

            response.authenticatorData =
                urlEncodeToString(assertionObject?.authenticatorData).replace(
                    "\n",
                    ""
                )
            response.signature = urlEncodeToString(assertionObject?.signature).replace("\n", "")
            assertionResultRequest.response = response
            return assertionResultRequest
        } catch (e: Exception) {
            val assertionResultRequest = AssertionResultRequest()
            assertionResultRequest.isSuccessful = false
            assertionResultRequest.errorMessage =
                "Error in making credential by Authenticator : ${e.message}"
            e.printStackTrace()
            return assertionResultRequest
        }
    }
    suspend fun selectPublicKeyCredentialSource(
        credentialSelector: CredentialSelector,
        assertionOptionResponse: AssertionOptionResponse?,
        origin: String?,
    ): PublicKeyCredentialSource? {
        val options: AuthenticatorGetAssertionOptions = generateAuthenticatorGetAssertionOptions(assertionOptionResponse, origin)
        return authenticator?.selectPublicKeyCredentialSource(credentialSelector, options)
    }
}