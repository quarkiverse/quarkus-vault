package io.quarkus.vault.runtime.client.dto.kv;

import java.util.Map;

import io.quarkus.vault.runtime.client.dto.AbstractVaultDTO;

/*

{
    "request_id":"47efb903-0582-8734-b627-2c52207221ca",
    "lease_id":"",
    "renewable":false,
    "lease_duration":604800,
    "data":{"password":{"foo":"bar"}},
    "wrap_info":null,
    "warnings":null,
    "auth":null
}

*/
public class VaultKvSecretJsonV1 extends AbstractVaultDTO<Map<String, Object>, Object> {

}
