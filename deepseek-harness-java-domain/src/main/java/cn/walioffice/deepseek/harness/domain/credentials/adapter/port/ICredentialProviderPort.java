package cn.walioffice.deepseek.harness.domain.credentials.adapter.port;

import cn.walioffice.deepseek.harness.domain.credentials.model.valobj.CredentialInfoVO;
import cn.walioffice.deepseek.harness.domain.credentials.model.valobj.CredentialRef;
import cn.walioffice.deepseek.harness.domain.credentials.model.valobj.ResolvedCredentialVO;
import java.util.Optional;

/**
 * Port implemented by credential providers over their source layers.
 *
 * <p>One seam-wide rule binds all operations: an empty stored value is absent
 * everywhere — {@link #resolve} skips it, {@link #describe} reports it
 * unconfigured — so a blank never masquerades as a configured secret.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ICredentialProviderPort {

    /**
     * Resolve one reference to its current value. Resolution is per call:
     * consumers re-resolve at each operation and must not cache across
     * operations.
     *
     * @param ref the reference to resolve
     * @return the value and its source, or empty while unconfigured
     */
    Optional<ResolvedCredentialVO> resolve(CredentialRef ref);

    /**
     * Describe one reference for configuration surfaces without exposing the value.
     *
     * @param ref the reference to describe
     * @return configured state, supplying source, and writability
     */
    CredentialInfoVO describe(CredentialRef ref);

    /**
     * Durably store one value in the provider-managed writable source. Rejects
     * while a read-only source shadows the reference, and rejects an empty
     * value (use {@link #unset}).
     *
     * @param ref   the reference to store
     * @param value the non-empty secret value
     */
    void set(CredentialRef ref, String value);

    /**
     * Remove one reference from the provider-managed writable source; removing
     * an absent reference is a no-op. Rejects while a read-only source shadows
     * the reference.
     *
     * @param ref the reference to remove
     */
    void unset(CredentialRef ref);
}
