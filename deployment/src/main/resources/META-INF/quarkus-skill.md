### Secret configuration

```properties
%prod.quarkus.vault.url=https://vault.example.com:8200
%prod.quarkus.vault.authentication.kubernetes.role=my-role
quarkus.vault.secret-config-kv-path=myapps/vault-quickstart/config
```

```java
@ConfigProperty(name = "a-private-key")
String privateKey;
```

- In dev and test mode, Dev Services starts a Vault container and configures the connection automatically, as long as `quarkus.vault.url` is not set. Put `quarkus.vault.url` and authentication under a profile such as `%prod.` so they don't disable Dev Services.
- Keys stored under a configured path are mounted as config properties.
- `quarkus.vault.secret-config-kv-path` takes a comma separated list of paths. If two paths contain the same property, the last path wins.
- Prefixed form: `quarkus.vault.secret-config-kv-path.myprefix=config` makes `foo` from `secret/config` available as `myprefix.foo`.
- Named KV engines can fetch secrets from different mount paths. Their properties are under `quarkus.vault.kv-secret-engine.<alias>`.
- Only scalar types are supported for secret values.
- The config source has ordinal 270, which can be changed with `quarkus.vault.config-ordinal`. It is higher than file system or jar ordinals and lower than environment variables.
- Values from all config paths (`secret-config-kv-path`, its prefixed form, and the same properties under `kv-secret-engine.<alias>`) are cached together for `quarkus.vault.secret-config-cache-period` (default 10m), and are not fetched from Vault again until that period expires.
- After the cache period, Vault is read again on the next config lookup. If the refresh fails with an I/O error, for example because Vault is unreachable, the application keeps serving the last known values for another cache period. Other errors, such as a 403 after a policy is revoked or a 404, are thrown to the caller.
- A path that does not exist still fails the application at startup.

See: https://docs.quarkiverse.io/quarkus-vault/dev/index.html

### KV secret engine

- By default Quarkus assumes a KV engine version 2 mounted at `secret/`. Otherwise use `quarkus.vault.kv-secret-engine-version` and `quarkus.vault.kv-secret-engine-mount-path`.
- For a v2 engine, the URL is built as `<url>/v1/<mount>/data/...`. With `readSecret("foo/bar")` the request goes to `https://localhost:8200/v1/secret/data/foo/bar`.
- CLI commands do not specify `data` in the path. Policies apply to the real path, for example `secret/data/myapps/vault-quickstart/*` for v2.
- Programmatic access: inject `VaultKVSecretEngine` when the path is only known at runtime.

```java
@Inject
VaultKVSecretEngine kvSecretEngine;

kvSecretEngine.readSecret("myapps/vault-quickstart/" + vaultPath).toString();
```

See: https://docs.quarkiverse.io/quarkus-vault/dev/index.html

### Authentication

- Working with Vault is a two step process: log in to get a client token, then use that token within the limits of its policies.
- Supported methods: Token, Userpass, GitHub, AppRole, Kubernetes and AWS IAM. Vault Agent Auto-Auth can replace application authentication.
- Token: `quarkus.vault.authentication.client-token`.
- Userpass: `quarkus.vault.authentication.userpass.username` and `.password`.
- GitHub: a personal access token with the `read:org` scope, set with `quarkus.vault.authentication.github.token`.
- AppRole: a role id and a secret id, `quarkus.vault.authentication.app-role.role-id` and `.secret-id`.
- Kubernetes: the pod service account JWT is used. Set `quarkus.vault.authentication.kubernetes.role`.
- AWS IAM: signs a `sts:GetCallerIdentity` request. It needs the optional AWS SDK dependency and `quarkus.vault.authentication.aws-iam.role`.
- Response wrapping: wrapping tokens are short lived and can be unwrapped only once. Wrapped properties include `client-token-wrapping-token`, `app-role.secret-id-wrapping-token`, `userpass.password-wrapping-token` and `github.token-wrapping-token`.
- Vault Agent Auto-Auth: point `quarkus.vault.url` at the agent listener and set `quarkus.vault.authentication.none=true`. This property is exclusive with all other authentication settings.

See: https://docs.quarkiverse.io/quarkus-vault/dev/vault-auth.html

### TLS

- Trusted certificates can come from the `javax.net.ssl.trustStore` system property, or from `quarkus.vault.tls.ca-cert` with a pem file.
- With Kubernetes authentication and no `ca-cert`, the Kubernetes CA bundle at `/var/run/secrets/kubernetes.io/serviceaccount/ca.crt` is used and overrides the truststore. To use a truststore with Kubernetes authentication, set `quarkus.vault.tls.use-kubernetes-ca-cert=false`, otherwise the TLS handshake fails.
- `quarkus.vault.tls.skip-verify` skips certificate validation. It is discouraged in production.

See: https://docs.quarkiverse.io/quarkus-vault/dev/index.html#_tls

### Datasource credentials

The guide lists three approaches: a KV property through the config source, a credentials provider, and dynamic database credentials.

- Config source: `quarkus.datasource.password = ${dbpassword}`. This works with any subsystem that needs a secret in configuration.
- Credentials provider: new connections fetch the password, so a changed password is handled without a restart.

```properties
quarkus.vault.credentials-provider.mydatabase.kv-path=myapps/vault-quickstart/db
quarkus.datasource.credentials-provider = mydatabase
```

- The password is read from key `password` by default (`kv-password-key`). If the secret also contains a `username` key (or the key set with `kv-username-key`), the provider returns the username as well, and `quarkus.datasource.username` can be dropped.
- Dynamic credentials: Vault creates and retires database accounts. Use `credentials-role` with `credentials-mount` set to `database`.
- Connections must be recycled before the lease TTL configured in Vault, with `quarkus.datasource.jdbc.max-lifetime` (the guide uses `30m` for a 1 hour TTL). Reactive datasources use `quarkus.datasource.reactive.max-lifetime`.
- With dynamic credentials the docs recommend `quarkus.hibernate-orm.schema-management.strategy=update`, because `drop-and-create` fails when another user owns the objects.
- `quarkus.vault.renew-grace-period` sets when leases are renewed or recreated, before they expire.
- Deprecated: `database-credentials-role` since 2.6, use `credentials-role` with mount `database`. `kv-key` since 4.9, use `kv-password-key`.

See: https://docs.quarkiverse.io/quarkus-vault/dev/vault-datasource.html

### Transit secret engine

- Transit provides encrypt, decrypt, rewrap, sign and verify signature operations. Keys stay in Vault.
- Keys are created with the Vault CLI, for example `vault write -f transit/keys/my-encryption-key`. Only the key name is required, and the key type defaults to `aes256-gcm96`, which can encrypt and decrypt but not sign.
- Signing needs a separate key with an asymmetric type, for example `vault write transit/keys/my-sign-key type=ecdsa-p256`.
- The example policy grants `update` on `transit/encrypt/my-encryption-key`, `transit/decrypt/my-encryption-key`, `transit/sign/my-sign-key` and `transit/verify/my-sign-key`.
- Default mount is `transit/`. Use `quarkus.vault.transit-secret-engine-mount-path` otherwise.

```java
@Inject
public VaultTransitSecretEngine transitSecretEngine;

transitSecretEngine.encrypt("my-encryption-key", clearData);
transitSecretEngine.decrypt("my-encryption-key", cipherText).asString();
String signature = transitSecretEngine.sign("my-sign-key", input);
transitSecretEngine.verifySignature("my-sign-key", signature, input);
```

- Batch methods are recommended for mass operations. Transit contexts allow key derivation. Rewrapping re-encrypts with the most recent key version after a rotation.

See: https://docs.quarkiverse.io/quarkus-vault/dev/vault-transit.html

### PKI secret engine

- The PKI engine generates dynamic X.509 certificates. Short TTLs reduce the need for CRLs, and ephemeral certificates can be created at startup.
- Setup uses the Vault CLI: enable `pki`, then generate a root CA, for example `vault write pki/root/generate/internal common_name=my-website.com ttl=8760h`. A root can also be generated with `VaultPKISecretEngine.generateRoot(GenerateRootOptions)`.
- A role defines what certificates may contain, for example `allowed_domains`, `allow_subdomains` and `max_ttl`. Roles can be created with `VaultPKISecretEngine.updateRole(String role, RoleOptions options)`.
- Issue with `generateCertificate(role, options)`, or sign a CSR with `signRequest(role, csr, options)`.
- Revoke with `revokeCertificate(serialNumber)`.

See: https://docs.quarkiverse.io/quarkus-vault/dev/vault-pki.html

### TOTP secret engine

- The extension provides `io.quarkus.vault.VaultTOTPSecretEngine` for the Vault TOTP engine.
- It offers `createKey`, `generateCode` and `validateCode`.

See: https://docs.quarkiverse.io/quarkus-vault/dev/index.html#totp

### Health check

- With `quarkus-smallrye-health`, a readiness check validates the connection to Vault. It is disabled by default and enabled with `quarkus.vault.health.enabled=true`.
- The endpoint reports ready only when Vault is initialized, unsealed and active.
- On an HA cluster reached through standby nodes, readiness reports DOWN and Kubernetes stops sending traffic to the application. Set `quarkus.vault.health.stand-by-ok=true` or `quarkus.vault.health.performance-stand-by-ok=true` to report standby or performance standby nodes as ready.

See: https://docs.quarkiverse.io/quarkus-vault/dev/index.html#_vault_health_check

### Dev Services

- Dev Services starts a Vault container in dev and test mode when Docker is running and `quarkus.vault.url` is not configured, and configures the connection automatically. The docs recommend defining real connection settings under the `%prod.` profile.
- Shared server: Dev Services discovers containers with the `quarkus-dev-service-vault` label. `service-name` (default `vault`) selects the server. Sharing is enabled by default in dev mode and disabled in test mode. `shared=false` disables it.
- Transit and PKI: `transit-enabled=true` mounts the transit engine at `transit/`, and `pki-enabled=true` mounts PKI at `pki/`. A started dev server mounts KV v2 at `secret/`.
- Init commands are passed to the `vault` executable in the container, so the `vault` prefix is omitted. They run in order, authenticated with the root token.

```properties
quarkus.vault.devservices.init-commands[0]=secrets enable -path=apps kv-v2
quarkus.vault.devservices.init-commands[1]=kv put apps/my-app foo=bar
```

- If a command fails, the remaining commands are skipped and an error is logged, but the container still starts. The error can be easy to miss.
- To troubleshoot, run a command against the running container: `docker exec -it <container-id> vault <command>`.

See: https://docs.quarkiverse.io/quarkus-vault/dev/index.html#dev-services

### Dev UI and Dev MCP

- The Vault card links to the Vault web UI of the configured or started server, and shows the server version.
- When Dev Services starts the container, the card shows the root token to log in to the Vault web UI, for example to check init command results. The token is not shown for a container that Dev Services only discovered.
- Without `quarkus.vault.url` and without Dev Services, the card states that no Vault is configured.
- The "Config sources" page lists, for each KV path, the property names it provides, the last fetch time and an error category such as `not found (404)` or `permission denied (403)`. Values are never shown.
- Dev MCP: config source metadata is not shared with MCP clients by default. Enable it with `%dev.quarkus.vault.dev-ui.expose-config-source-metadata-to-mcp=true`.
- The tool `quarkus-vault_getConfigSourcesForMcp` then returns KV paths, property names, fetch times and refresh errors. When disabled it returns a message explaining how to enable it.
- The setting is ignored outside dev mode.

See: https://docs.quarkiverse.io/quarkus-vault/dev/index.html
