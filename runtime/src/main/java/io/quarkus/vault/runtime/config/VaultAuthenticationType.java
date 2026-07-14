package io.quarkus.vault.runtime.config;

public enum VaultAuthenticationType {

    /**
     * Kubernetes vault authentication
     * <p>
     * When running the application into kubernetes, it is possible to benefit from the vault/kubernetes integration
     * for authentication. Once the kubernetes authentication has been enabled into vault, create a vault role
     * associating one or more vault policies, with one or more service accounts and one or more namespaces.
     * When selecting the kubernetes authentication type, specify the vault authentication role to use.
     * <p>
     * see https://www.vaultproject.io/api/auth/kubernetes/index.html
     */
    KUBERNETES,

    /**
     * Username & password vault authentication
     * <p>
     * Vault supports authentication through username and password. Before using it, userpass auth needs to be enabled
     * in vault, and a user needs to be created with his password. This type of authentication is easy to use,
     * and useful in development environments. When using in production, some care will have to be taken to keep
     * some confidentiality on the password.
     * <p>
     * https://www.vaultproject.io/api/auth/userpass/index.html
     */
    USERPASS,

    /**
     * Role & secret vault authentication using AppRole method
     * <p>
     * <p>
     * https://www.vaultproject.io/api/auth/approle/index.html
     */
    APPROLE,

    /**
     * GitHub token vault authentication
     * <p>
     * Vault supports authentication using GitHub personal access tokens. Before using it, the github auth
     * method needs to be enabled in vault, and configured with the GitHub organization users belong to.
     * <p>
     * https://developer.hashicorp.com/vault/api-docs/auth/github
     */
    GITHUB,

    /**
     * AWS IAM vault authentication
     * <p>
     * The aws auth method provides an automated mechanism to retrieve a Vault token for IAM principals.
     * A special AWS request signed with AWS IAM credentials ({@code sts:GetCallerIdentity}) is used for
     * authentication, so no Vault-specific secret needs to be provisioned to the client: the IAM
     * credentials already available to AWS instances, Lambda functions, etc. are used to authenticate.
     * <p>
     * see https://developer.hashicorp.com/vault/api-docs/auth/aws
     */
    AWS_IAM,

    /**
     * No authentication
     * <p>
     * Requests are sent to Vault without any client token. This is useful when the configured Vault url points
     * to a Vault Agent configured with Auto-Auth, which authenticates on behalf of the application and injects
     * the Vault token into proxied requests.
     * <p>
     * https://developer.hashicorp.com/vault/docs/agent-and-proxy/autoauth
     */
    NONE

}
