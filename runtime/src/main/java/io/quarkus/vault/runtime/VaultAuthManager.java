package io.quarkus.vault.runtime;

import static io.quarkus.vault.runtime.LogConfidentialityLevel.LOW;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.APPROLE;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.KUBERNETES;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.USERPASS;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.vault.VaultException;
import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.runtime.client.authmethod.VaultInternalAppRoleAuthMethod;
import io.quarkus.vault.runtime.client.authmethod.VaultInternalKubernetesAuthMethod;
import io.quarkus.vault.runtime.client.authmethod.VaultInternalTokenAuthMethod;
import io.quarkus.vault.runtime.client.authmethod.VaultInternalUserpassAuthMethod;
import io.quarkus.vault.runtime.client.backend.VaultInternalSystemBackend;
import io.quarkus.vault.runtime.client.dto.auth.AbstractVaultAuthAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleGenerateNewSecretID;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultTokenCreate;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV1;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2;
import io.quarkus.vault.runtime.config.VaultAuthenticationType;
import io.quarkus.vault.runtime.config.VaultBootstrapConfig;
import io.smallrye.mutiny.Uni;

/**
 * Handles authentication. Supports revocation and renewal.
 */
@Singleton
public class VaultAuthManager {

    private static final Logger log = Logger.getLogger(VaultAuthManager.class.getName());

    public static final String USERPASS_WRAPPING_TOKEN_PASSWORD_KEY = "password";

    private AtomicReference<VaultToken> loginCache = new AtomicReference<>(null);
    private Map<String, Uni<String>> unwrappingCache = new ConcurrentHashMap<>();
    private VaultConfigHolder vaultConfigHolder;
    private VaultInternalSystemBackend vaultInternalSystemBackend;
    private VaultInternalAppRoleAuthMethod vaultInternalAppRoleAuthMethod;
    private VaultInternalKubernetesAuthMethod vaultInternalKubernetesAuthMethod;
    private VaultInternalUserpassAuthMethod vaultInternalUserpassAuthMethod;
    private VaultInternalTokenAuthMethod vaultInternalTokenAuthMethod;

    VaultAuthManager(VaultConfigHolder vaultConfigHolder, VaultInternalSystemBackend vaultInternalSystemBackend,
            VaultInternalAppRoleAuthMethod vaultInternalAppRoleAuthMethod,
            VaultInternalKubernetesAuthMethod vaultInternalKubernetesAuthMethod,
            VaultInternalUserpassAuthMethod vaultInternalUserpassAuthMethod,
            VaultInternalTokenAuthMethod vaultInternalTokenAuthMethod) {
        this.vaultConfigHolder = vaultConfigHolder;
        this.vaultInternalSystemBackend = vaultInternalSystemBackend;
        this.vaultInternalAppRoleAuthMethod = vaultInternalAppRoleAuthMethod;
        this.vaultInternalKubernetesAuthMethod = vaultInternalKubernetesAuthMethod;
        this.vaultInternalUserpassAuthMethod = vaultInternalUserpassAuthMethod;
        this.vaultInternalTokenAuthMethod = vaultInternalTokenAuthMethod;
    }

    private VaultBootstrapConfig getConfig() {
        return vaultConfigHolder.getVaultBootstrapConfig();
    }

    public Uni<String> getClientToken() {
        return getConfig().authentication.isDirectClientToken() ? getDirectClientToken()
                : login().map(vaultToken -> vaultToken.clientToken);
    }

    private Uni<String> getDirectClientToken() {

        Optional<String> clientTokenOption = getConfig().authentication.clientToken;
        if (clientTokenOption.isPresent()) {
            return Uni.createFrom().item(clientTokenOption.get());
        }

        return unwrapWrappingTokenOnce("client token",
                getConfig().authentication.clientTokenWrappingToken.get(), unwrap -> unwrap.auth.clientToken,
                VaultTokenCreate.class);
    }

    private Uni<VaultToken> login() {
        return login(loginCache.get())
                .map(vaultToken -> {
                    loginCache.set(vaultToken);
                    return vaultToken;
                });
    }

    public Uni<VaultToken> login(VaultToken currentVaultToken) {
        return Uni.createFrom().item(Optional.ofNullable(currentVaultToken))
                // check clientToken is still valid
                .flatMap(this::validate)
                // extend clientToken if necessary
                .flatMap(vaultToken -> {
                    if (vaultToken.isPresent() && vaultToken.get().shouldExtend(getConfig().renewGracePeriod)) {
                        return extend(vaultToken.get().clientToken).map(Optional::of);
                    }
                    return Uni.createFrom().item(vaultToken);
                })
                // create new clientToken if necessary
                .flatMap(vaultToken -> {
                    if (vaultToken.isEmpty() || vaultToken.get().isExpired()
                            || vaultToken.get().expiresSoon(getConfig().renewGracePeriod)) {
                        return vaultLogin();
                    }
                    return Uni.createFrom().item(vaultToken.get());
                });
    }

    private Uni<Optional<VaultToken>> validate(Optional<VaultToken> vaultToken) {
        if (vaultToken.isEmpty()) {
            return Uni.createFrom().item(Optional.empty());
        }
        return vaultInternalTokenAuthMethod.lookupSelf(vaultToken.get().clientToken)
                .map(i -> vaultToken)
                .onFailure(VaultClientException.class).recoverWithUni(e -> {
                    if (((VaultClientException) e).getStatus() == 403) { // forbidden
                        log.debug("login token " + vaultToken.get().clientToken + " has become invalid");
                        return Uni.createFrom().item(Optional.empty());
                    } else {
                        return Uni.createFrom().failure(e);
                    }
                });
    }

    private Uni<VaultToken> extend(String clientToken) {
        return vaultInternalTokenAuthMethod.renewSelf(clientToken, null)
                .map(renew -> {
                    VaultToken vaultToken = new VaultToken(renew.auth.clientToken, renew.auth.renewable,
                            renew.auth.leaseDurationSecs);
                    sanityCheck(vaultToken);
                    log.debug("extended login token: " + vaultToken.getConfidentialInfo(getConfig().logConfidentialityLevel));
                    return vaultToken;
                });
    }

    private Uni<VaultToken> vaultLogin() {
        return login(getConfig().getAuthenticationType())
                .map(vaultToken -> {
                    sanityCheck(vaultToken);
                    log.debug(
                            "created new login token: " + vaultToken.getConfidentialInfo(getConfig().logConfidentialityLevel));
                    return vaultToken;
                });
    }

    private Uni<VaultToken> login(VaultAuthenticationType type) {
        Uni<? extends AbstractVaultAuthAuth<?>> authRequest;
        if (type == KUBERNETES) {
            authRequest = loginKubernetes();
        } else if (type == USERPASS) {
            String username = getConfig().authentication.userpass.username.get();
            authRequest = getPassword()
                    .flatMap(password -> vaultInternalUserpassAuthMethod.login(username, password).map(r -> r.auth));
        } else if (type == APPROLE) {
            String roleId = getConfig().authentication.appRole.roleId.get();
            authRequest = getSecretId().flatMap(secretId -> vaultInternalAppRoleAuthMethod.login(roleId, secretId))
                    .map(r -> r.auth);
        } else {
            throw new UnsupportedOperationException("unknown authType " + getConfig().getAuthenticationType());
        }

        return authRequest.map(auth -> new VaultToken(auth.clientToken, auth.renewable, auth.leaseDurationSecs));
    }

    private Uni<String> getSecretId() {

        Optional<String> secretIdOption = getConfig().authentication.appRole.secretId;
        if (secretIdOption.isPresent()) {
            return Uni.createFrom().item(secretIdOption.get());
        }

        return unwrapWrappingTokenOnce("secret id",
                getConfig().authentication.appRole.secretIdWrappingToken.get(), unwrap -> unwrap.data.secretId,
                VaultAppRoleGenerateNewSecretID.class);
    }

    private Uni<String> getPassword() {

        Optional<String> passwordOption = getConfig().authentication.userpass.password;
        if (passwordOption.isPresent()) {
            return Uni.createFrom().item(passwordOption.get());
        }

        String wrappingToken = getConfig().authentication.userpass.passwordWrappingToken.get();
        if (getConfig().kvSecretEngineVersion == 1) {
            Function<VaultKvSecretV1, String> f = unwrap -> unwrap.data.get(USERPASS_WRAPPING_TOKEN_PASSWORD_KEY);
            return unwrapWrappingTokenOnce("password", wrappingToken, f, VaultKvSecretV1.class);
        } else {
            Function<VaultKvSecretV2, String> f = unwrap -> unwrap.data.data.get(USERPASS_WRAPPING_TOKEN_PASSWORD_KEY);
            return unwrapWrappingTokenOnce("password", wrappingToken, f, VaultKvSecretV2.class);
        }
    }

    private <T> Uni<String> unwrapWrappingTokenOnce(String type, String wrappingToken, Function<T, String> f, Class<T> clazz) {
        return unwrappingCache.computeIfAbsent(wrappingToken, (token) -> {
            return vaultInternalSystemBackend.unwrap(wrappingToken, clazz)
                    .map(unwrap -> {
                        String wrappedValue = f.apply(unwrap);

                        String displayValue = getConfig().logConfidentialityLevel.maskWithTolerance(wrappedValue, LOW);
                        log.debug("unwrapped " + type + ": " + displayValue);

                        return wrappedValue;
                    })
                    .onFailure(VaultClientException.class).transform(e -> {
                        if (((VaultClientException) e).getStatus() == 400) {
                            String message = "wrapping token is not valid or does not exist; " +
                                    "this means that the token has already expired " +
                                    "(if so you can increase the ttl on the wrapping token) or " +
                                    "has been consumed by somebody else " +
                                    "(potentially indicating that the wrapping token has been stolen)";
                            return new VaultException(message, e);
                        } else {
                            return e;
                        }
                    })
                    .memoize().indefinitely();
        });
    }

    private Uni<VaultKubernetesAuthAuth> loginKubernetes() {
        String jwt = new String(read(getConfig().authentication.kubernetes.jwtTokenPath), StandardCharsets.UTF_8);
        log.debug("authenticate with jwt at: " + getConfig().authentication.kubernetes.jwtTokenPath + " => "
                + getConfig().logConfidentialityLevel.maskWithTolerance(jwt, LOW));
        String role = getConfig().authentication.kubernetes.role.get();
        return vaultInternalKubernetesAuthMethod.login(role, jwt).map(r -> r.auth);
    }

    private byte[] read(String path) {
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sanityCheck(VaultToken vaultToken) {
        vaultToken.leaseDurationSanityCheck("auth", getConfig().renewGracePeriod);
    }

}
