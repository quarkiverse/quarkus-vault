package io.quarkus.vault.runtime.client.authmethod;

import io.quarkus.vault.runtime.VaultConfigHolder;
import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.VaultInternalBase;
import io.quarkus.vault.runtime.client.dto.auth.VaultAwsIamAuth;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class VaultInternalAwsIamAuthMethod extends VaultInternalBase {

  @Inject
  private VaultConfigHolder vaultConfigHolder;

  @Override
  protected String opNamePrefix() {
    return super.opNamePrefix() + " [AUTH (aws iam)]";
  }

  public Uni<VaultAwsIamAuth> login(final VaultClient vaultClient) {
    return null;
  }
}
