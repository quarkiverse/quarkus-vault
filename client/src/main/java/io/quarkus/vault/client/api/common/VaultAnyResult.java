package io.quarkus.vault.client.api.common;

import java.util.Map;

public class VaultAnyResult extends VaultLeasedResult<Map<String, Object>, VaultAuthResult<Object>> {

    // Common returned values
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";

}
