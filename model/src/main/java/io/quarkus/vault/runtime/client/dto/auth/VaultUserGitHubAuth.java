package io.quarkus.vault.runtime.client.dto.auth;

import io.quarkus.vault.runtime.client.dto.AbstractVaultDTO;

/*

{
  "request_id": "266bffba-8851-cfd2-dd2a-a230655c6f2b",
  "lease_id": "",
  "renewable": false,
  "lease_duration": 0,
  "data": null,
  "wrap_info": null,
  "warnings": null,
  "auth": {
    "client_token": "s.tmaYRmdXqKVF810aYOinWgMd",
    "accessor": "PAwVe79bWN0uoGCLrWdfYsIR",
    "policies": [
      "default",
      "mypolicy"
    ],
    "token_policies": [
      "default",
      "mypolicy"
    ],
    "metadata": {
      "username": "bob",
      "org": "obo"
    },
    "lease_duration": 43200,
    "renewable": true,
    "entity_id": "f0289e14-f9ac-a5e0-c1df-88d8d031bf38",
    "token_type": "service",
    "orphan": true
  }
}

*/
public class VaultUserGitHubAuth extends AbstractVaultDTO<Object, VaultUserGitHubAuthAuth> {

}
