package io.quarkus.vault.runtime;

import static io.quarkus.vault.client.logging.LogConfidentialityLevel.LOW;
import static io.quarkus.vault.client.logging.LogConfidentialityLevel.MEDIUM;

import io.quarkus.vault.client.logging.LogConfidentialityLevel;

public class VaultDynamicCredentials extends LeaseBase {

    public String username;
    public String password;

    public VaultDynamicCredentials(LeaseBase lease, String username, String password) {
        super(lease);
        this.username = username;
        this.password = password;
    }

    public String getConfidentialInfo(LogConfidentialityLevel level) {
        return "{" + super.getConfidentialInfo(level) + ", username: " + level.maskWithTolerance(username, MEDIUM)
                + ", password:"
                + level.maskWithTolerance(password, LOW) + "}";
    }

}
