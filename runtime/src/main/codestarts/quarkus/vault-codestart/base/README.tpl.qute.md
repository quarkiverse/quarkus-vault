{#include readme-header /}

#### Dev Services Requirements

In development and test modes (`quarkus:dev` and `@QuarkusTest`), Quarkus automatically provisions a HashiCorp Vault container using **Vault Dev Services**.
- A container runtime such as **Docker** or **Podman** is required.
- The secret is initialized automatically upon container startup via `quarkus.vault.devservices.init-commands`:
  ```properties
  quarkus.vault.devservices.init-commands=kv put secret/myapps/vault-codestart greeting=hello-from-vault
  quarkus.vault.secret-config-kv-path=myapps/vault-codestart
  ```

#### Production Considerations

In production mode:
- Dev Services is disabled and `init-commands` has no effect.
- The `quarkus.vault.secret-config-kv-path` property expects the configured KV path (`secret/myapps/vault-codestart`) to already exist on your target Vault server.
- Vault connection parameters (such as `quarkus.vault.url` and authentication credentials) must be configured for your production environment.

#### Endpoints

- `GET /vault/config` - Returns the secret read via `@ConfigProperty(name = "greeting")`
- `GET /vault/engine` - Returns the secret read programmatically via `VaultKVSecretEngine.readSecret("myapps/vault-codestart")`

