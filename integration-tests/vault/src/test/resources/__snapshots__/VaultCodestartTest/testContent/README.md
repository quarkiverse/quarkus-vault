# test-codestart

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/test-codestart-1.0.0-codestart-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- Vault ([guide](https://docs.quarkiverse.io/quarkus-vault/dev/index.html)): Store your credentials securely in HashiCorp Vault

## Provided Code

### HashiCorp Vault Dev Services Example

Read secrets from HashiCorp Vault Dev Services using @ConfigProperty and VaultKVSecretEngine.

[Related guide section...](https://docs.quarkiverse.io/quarkus-vault/dev/index.html)

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

