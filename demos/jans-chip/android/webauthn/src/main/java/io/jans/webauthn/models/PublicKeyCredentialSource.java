package io.jans.webauthn.models;


import android.support.annotation.NonNull;
import android.util.Base64;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.security.SecureRandom;

@Entity(tableName = "credentials", indices = {@Index("rpId")})
public class PublicKeyCredentialSource {
    public static final String type = "public-key";

    @PrimaryKey(autoGenerate = true)
    public int roomUid;
    public byte[] id;
    public String keyPairAlias;
    public String rpId;
    public byte[] userHandle;
    public String userDisplayName;
    public String otherUI;
    public int keyUseCounter;

    @Ignore
    private static SecureRandom random;
    @Ignore
    private static final String KEYPAIR_PREFIX = "virgil-keypair-";

    /**
     * Construct a new PublicKeyCredentialSource. This is the canonical object that represents a
     * WebAuthn credential.
     *
     * @param rpId            The relying party ID.
     * @param userHandle      The unique ID used by the RP to identify the user.
     * @param userDisplayName A human-readable display name for the user.
     */
    public PublicKeyCredentialSource(@NonNull String rpId, byte[] userHandle, String userDisplayName) {
        ensureRandomInitialized();
        this.id = new byte[32];
        this.userDisplayName = userDisplayName;
        PublicKeyCredentialSource.random.nextBytes(this.id);

        this.rpId = rpId;
        this.keyPairAlias = KEYPAIR_PREFIX + Base64.encodeToString(id, Base64.NO_WRAP);
        this.userHandle = userHandle;
        this.keyUseCounter = 1;
    }

    /**
     * Ensure the SecureRandom singleton has been initialized.
     */
    private void ensureRandomInitialized() {
        if (PublicKeyCredentialSource.random == null) {
            PublicKeyCredentialSource.random = new SecureRandom();
        }
    }
}
