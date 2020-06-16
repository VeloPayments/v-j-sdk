package com.velopayments.blockchain.sdk.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.velopayments.blockchain.crypt.EncryptionKeyPair;
import com.velopayments.blockchain.crypt.Key;
import com.velopayments.blockchain.crypt.SigningKeyPair;
import lombok.*;

import java.util.Objects;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
/**
 * Value object to represent the keys that a blockchain entity may need
 */
public class EntityKeyConfig implements EntityKeys {

    /**
     * Artifact Id of the entity within the blockchain
     */
    @NonNull
    private UUID entityId;

    /**
     * DisplayName of the entity
     */
    @NonNull
    private String entityName;

    @NonNull
    private EntityKeyConfigContentType contentType;

    @NonNull
    @JsonProperty("signingKey")
    private SigningKeyPair signingKeyPair;

    @NonNull
    @JsonProperty("encryptionKey")
    private EncryptionKeyPair encryptionKeyPair;

    private Key secretKey;

    private String passphraseProtectedCertificateBase64;

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof EntityKeyConfig)) return false;
        final EntityKeyConfig other = (EntityKeyConfig) o;
        if (!other.canEqual(this)) return false;
        final Object this$entityId = this.getEntityId();
        final Object other$entityId = other.getEntityId();
        if (!Objects.equals(this$entityId, other$entityId)) return false;
        final Object this$entityName = this.getEntityName();
        final Object other$entityName = other.getEntityName();
        if (!Objects.equals(this$entityName, other$entityName)) return false;
        final Object this$contentType = this.getContentType();
        final Object other$contentType = other.getContentType();
        if (!Objects.equals(this$contentType, other$contentType)) return false;
        final Object this$signingKeyPair = this.getSigningKeyPair();
        final Object other$signingKeyPair = other.getSigningKeyPair();
        if (!Objects.equals(this$signingKeyPair, other$signingKeyPair)) return false;
        final Object this$encryptionKeyPair = this.getEncryptionKeyPair();
        final Object other$encryptionKeyPair = other.getEncryptionKeyPair();
        if (!Objects.equals(this$encryptionKeyPair, other$encryptionKeyPair)) return false;
        final Object this$secretKey = this.getSecretKey();
        final Object other$secretKey = other.getSecretKey();
        if (!Objects.equals(this$secretKey, other$secretKey)) return false;
        final Object this$passphraseProtectedCertificateBase64 = this.getPassphraseProtectedCertificateBase64();
        final Object other$passphraseProtectedCertificateBase64 = other.getPassphraseProtectedCertificateBase64();
        return Objects.equals(this$passphraseProtectedCertificateBase64, other$passphraseProtectedCertificateBase64);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof EntityKeyConfig;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $entityId = this.getEntityId();
        result = result * PRIME + ($entityId == null ? 43 : $entityId.hashCode());
        final Object $entityName = this.getEntityName();
        result = result * PRIME + ($entityName == null ? 43 : $entityName.hashCode());
        final Object $contentType = this.getContentType();
        result = result * PRIME + ($contentType == null ? 43 : $contentType.hashCode());
        final Object $signingKeyPair = this.getSigningKeyPair();
        result = result * PRIME + ($signingKeyPair == null ? 43 : $signingKeyPair.hashCode());
        final Object $encryptionKeyPair = this.getEncryptionKeyPair();
        result = result * PRIME + ($encryptionKeyPair == null ? 43 : $encryptionKeyPair.hashCode());
        final Object $secretKey = this.getSecretKey();
        result = result * PRIME + ($secretKey == null ? 43 : $secretKey.hashCode());
        final Object $passphraseProtectedCertificateBase64 = this.getPassphraseProtectedCertificateBase64();
        result = result * PRIME + ($passphraseProtectedCertificateBase64 == null ? 43 : $passphraseProtectedCertificateBase64.hashCode());
        return result;
    }
}
