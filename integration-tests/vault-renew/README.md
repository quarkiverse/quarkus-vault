# Reproducer for issue #328 — `quarkus.vault.renew-grace-period` ignored with AppRole authentication

https://github.com/quarkiverse/quarkus-vault/issues/328

## The problem reported by @Visserr2

With approle authentication, the login token is only extended (renew-self) when a Vault access
happens within **30 seconds** of the token's expiry, even when `quarkus.vault.renew-grace-period`
is set to a larger value (the reporter used 1m, and 35m for long-running jobs). If no Vault call
lands inside that 30s window, the token expires; leases attached to it (e.g. dynamic database
credentials) are revoked by Vault, and a long-running job later fails with errors such as:

> The server principal "v-approle-..." is not able to access the database under the current security context.

The reporter worked around it by patching `VaultCachingTokenProvider.DEFAULT_RENEW_GRACE_PERIOD`
in their application.

## Root cause

In `runtime/src/main/java/io/quarkus/vault/runtime/client/VaultClientProducer.java`
(`configureAuthentication`), the configured grace period is propagated to the client for
KUBERNETES and USERPASS, but **not for APPROLE**:

```java
case KUBERNETES:
    ...
    .caching(config.renewGracePeriod())   // <-- propagated
    ...
case APPROLE:
    var appRoleOptions = VaultAppRoleAuthOptions.builder()
            .mountPath(appRoleConfig.authMountPath())
            .roleId(appRoleConfig.roleId().orElseThrow());
    ...                                   // <-- .caching(config.renewGracePeriod()) is never called
    builder.appRole(appRoleOptions.build());
    ...
case USERPASS:
    ...
    userPassOptions.caching(config.renewGracePeriod());   // <-- propagated
```

`VaultAppRoleAuthOptions.Builder` therefore keeps its default,
`VaultCachingTokenProvider.DEFAULT_RENEW_GRACE_PERIOD = Duration.ofSeconds(30)`, and the
`quarkus.vault.renew-grace-period` value is silently ignored for approle — exactly what the
issue reporter observed (their logs show `v-approle-...` credentials).

## The reproducer

The application (`RenewTestResource`) exposes two endpoints: `/renew/db` reads a table through the
datasource, which is configured with **Vault dynamic database credentials**
(`quarkus.datasource.credentials-provider`), and `/renew/kv` reads a KV secret (an arbitrary Vault
access giving the client the opportunity to extend the login token). Each `@QuarkusIntegrationTest`
runs the packaged application in its own process.

Two scenarios, each run once with approle and once with userpass — the auth method is the only
difference:

### 1. Pinpoint scenario (`*RenewGracePeriodITCase`, in-process Vault stub)

The stub issues 60s-TTL tokens and records every login and renew-self call;
`quarkus.vault.renew-grace-period=40s`. The test logs in, waits 25s, makes a second Vault access —
the token then expires in ~35s, i.e. **inside** the configured 40s grace period — and asserts that
the token was extended.

| Test | Auth | Result |
|------|------|--------|
| `AppRoleRenewGracePeriodITCase` | approle | **FAILS** — no renew-self is sent (35s > hard-coded 30s) |
| `UserPassRenewGracePeriodITCase` | userpass | PASSES — token extended as configured |

### 2. End-to-end scenario (`*DynamicCredentialsITCase`, real Vault + PostgreSQL in containers)

Vault (dev mode) and postgres run in containers on a shared docker network. The database secrets
engine issues dynamic credentials (lease TTL 10m) whose lease is a **child of the login token**
(TTL 90s); `quarkus.vault.renew-grace-period=60s`. Timeline:

- t0: `/renew/db` — login, dynamic credentials generated, connection opened and pooled
- t0+35s: `/renew/kv` — the token expires in ~55s, inside the configured 60s grace period, so it
  must be extended here
- t0+105s: `/renew/db` again

| Test | Auth | Result |
|------|------|--------|
| `AppRoleDynamicCredentialsITCase` | approle | **FAILS** — the token was not extended at t0+35s, expired at t0+90s, Vault revoked the credentials lease with it and ran the revocation statements against postgres; the DB access gets "permission denied for table demo" |
| `UserPassDynamicCredentialsITCase` | userpass | PASSES — token extended at t0+35s, lease untouched, DB access succeeds |

The approle failure is the real-Vault/real-DB materialization of the reporter's
"The server principal ... is not able to access the database under the current security context".

Note: the database role uses the same revocation statements as the Vault postgres plugin defaults,
minus the `DROP ROLE`, so that lease revocation deterministically succeeds (and strips the role of
its privileges) even while the role still has open connections.

## Running it

From the repository root:

```
mvn install -DskipTests -pl deployment -am
mvn verify -pl integration-tests/vault-renew
```

Expected result on current `main`: `Tests run: 4, Failures: 2` — both approle tests fail, both
userpass tests pass. The FINE logs also show the behavior from the issue: for approle the second
access logs only `using cached token`, while for userpass it is followed by `extended login token`.

## Fix

Add the missing call in the APPROLE branch of `VaultClientProducer.configureAuthentication`:

```java
appRoleOptions.caching(config.renewGracePeriod());
```

(after which all four tests pass).
