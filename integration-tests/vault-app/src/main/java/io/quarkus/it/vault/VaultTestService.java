package io.quarkus.it.vault;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.logging.Logger;

import io.quarkus.vault.VaultAppRoleAuthService;
import io.quarkus.vault.VaultKVSecretEngine;
import io.quarkus.vault.VaultTransitSecretEngine;
import io.quarkus.vault.auth.VaultAppRoleAuthRole;
import io.quarkus.vault.auth.VaultAppRoleSecretId;
import io.quarkus.vault.auth.VaultAppRoleSecretIdAccessor;
import io.quarkus.vault.auth.VaultAppRoleSecretIdRequest;
import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.transit.ClearData;
import io.quarkus.vault.transit.KeyConfigRequestDetail;
import io.quarkus.vault.transit.KeyCreationRequestDetail;
import io.quarkus.vault.transit.SigningInput;
import io.quarkus.vault.transit.VaultTransitExportKeyType;

@ApplicationScoped
public class VaultTestService {

    private static final Logger log = Logger.getLogger(VaultTestService.class);

    private static final String KEY_NAME = "mykey";

    private static final String TEST_APP_ROLE = "MyRole";

    @Inject
    EntityManager entityManager;

    @ConfigProperty(name = "password")
    String someSecret;

    @Inject
    VaultKVSecretEngine kv;

    @Inject
    VaultTransitSecretEngine transit;

    @Inject
    VaultAppRoleAuthService appRoleAuthService;

    @Transactional
    public String test() {

        String expectedPassword = "bar";
        if (!expectedPassword.equals(someSecret)) {
            return "someSecret=" + someSecret + "; expected: " + expectedPassword;
        }
        String password = ConfigProviderResolver.instance().getConfig().getValue("password", String.class);
        if (!expectedPassword.equals(password)) {
            return "password=" + password + "; expected: " + expectedPassword;
        }

        // basic
        Map<String, String> secrets = kv.readSecret("foo");
        String expectedSecrets = "{secret=s\u20accr\u20act}";
        if (!expectedSecrets.equals(secrets.toString())) {
            return "/foo=" + secrets + "; expected: " + expectedSecrets;
        }

        // crud
        kv.writeSecret("crud", secrets);
        secrets = kv.readSecret("crud");
        if (!expectedSecrets.equals(secrets.toString())) {
            return "/crud=" + secrets + "; expected: " + expectedSecrets;
        }
        kv.deleteSecret("crud");
        try {
            secrets = kv.readSecret("crud");
            return "/crud=" + secrets + "; expected 404";
        } catch (VaultClientException e) {
            if (e.getStatus() != 404) {
                return "http response code=" + e.getStatus() + "; expected: 404";
            }
        }

        try {
            List gifts = entityManager.createQuery("select g from Gift g").getResultList();
            int count = gifts.size();
            log.info("found " + count + " gifts");
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter printWriter = new PrintWriter(sw);
            e.printStackTrace(printWriter);
            return sw.toString();
        }

        String coucou = "coucou";
        SigningInput input = new SigningInput(coucou);
        String keyName = "my-encryption-key";
        String ciphertext = transit.encrypt(keyName, coucou);
        ClearData decrypted = transit.decrypt(keyName, ciphertext);
        if (!coucou.equals(decrypted.asString())) {
            return "decrypted=" + password + "; expected: " + coucou;
        }

        String rewraped = transit.rewrap(keyName, ciphertext, null);
        decrypted = transit.decrypt(keyName, rewraped);
        if (!coucou.equals(decrypted.asString())) {
            return "decrypted=" + password + "; expected: " + coucou;
        }

        String signature = transit.sign("my-sign-key", input, null);
        if (!signature.startsWith("vault:v1:")) {
            return "invalid signature " + signature;
        }

        transit.verifySignature("my-sign-key", signature, input, null);

        keyAdminTest();

        // creates with defaults
        VaultAppRoleAuthRole newAppRoleWithDefaults = new VaultAppRoleAuthRole();
        appRoleAuthService.createOrUpdateAppRole(TEST_APP_ROLE, newAppRoleWithDefaults);

        // list app roles
        List<String> appRoles = appRoleAuthService.getAppRoles();
        if (appRoles == null || appRoles.size() != 2) {
            return "invalid approles number after creation " + appRoles == null ? "null" : appRoles.size() + ", expexted: 2";
        }

        // retrieve app role and checks defaults values have been set
        VaultAppRoleAuthRole appRoleAuthRole = appRoleAuthService.getAppRole(TEST_APP_ROLE);
        if (appRoleAuthRole == null) {
            return "failed to retrieve approle";
        }

        // testing defaults
        boolean different = false;
        different = different || appRoleAuthRole.bindSecretId == false;
        different = different || appRoleAuthRole.secretIdBoundCidrs != null;
        different = different || appRoleAuthRole.secretIdNumUses != 0;
        different = different || !appRoleAuthRole.secretIdTtl.equals("0");
        different = different || appRoleAuthRole.localSecretIds != false;
        different = different || appRoleAuthRole.tokenTtl != 0;
        different = different || appRoleAuthRole.tokenMaxTtl != 0;
        different = different || appRoleAuthRole.tokenPolicies == null;
        different = different || appRoleAuthRole.tokenPolicies.size() != 0;
        different = different || appRoleAuthRole.tokenBoundCidrs == null;
        different = different || appRoleAuthRole.tokenBoundCidrs.size() != 0;
        different = different || appRoleAuthRole.tokenExplicitMaxTtl != 0;
        different = different || appRoleAuthRole.tokenNoDefaultPolicy != false;
        different = different || appRoleAuthRole.tokenNumUses != 0;
        different = different || appRoleAuthRole.tokenPeriod != 0;
        different = different || !"default".equals(appRoleAuthRole.tokenType);
        if (different) {
            return "Approle not created with default: " + appRoleAuthRole;
        }

        // update approle
        VaultAppRoleAuthRole newAppRoleWithDefaultsOverriden = new VaultAppRoleAuthRole()
                .setBindSecretId(false)
                .setSecretIdBoundCidrs(Arrays.asList("192.168.1.1/24", "192.168.0.1/24"))
                .setSecretIdNumUses(5)
                .setSecretIdTtl("60m")
                .setTokenTtl(10)
                .setTokenMaxTtl(15)
                .setTokenPolicies(Arrays.asList("policy1", "policy2"))
                .setTokenBoundCidrs(Arrays.asList("192.168.0.0/24", "192.168.1.0/24"))
                .setTokenExplicitMaxTtl(20)
                .setTokenNoDefaultPolicy(true)
                .setTokenNumUses(1)
                .setTokenPeriod(30)
                .setTokenType("service");
        appRoleAuthService.createOrUpdateAppRole(TEST_APP_ROLE, newAppRoleWithDefaultsOverriden);

        // list app roles
        appRoles = appRoleAuthService.getAppRoles();
        if (appRoles == null || appRoles.size() != 2) {
            return "invalid approles number after update " + appRoles == null ? "null" : appRoles.size() + ", expexted: 2";
        }

        // retrieve app role and checks defaults values have been overriden
        appRoleAuthRole = appRoleAuthService.getAppRole(TEST_APP_ROLE);
        different = false;
        different = different || appRoleAuthRole.bindSecretId != false;
        different = different || appRoleAuthRole.secretIdBoundCidrs == null || appRoleAuthRole.secretIdBoundCidrs.size() != 2;
        different = different || appRoleAuthRole.secretIdNumUses != 5;
        different = different || !appRoleAuthRole.secretIdTtl.equals("3600");
        different = different || appRoleAuthRole.localSecretIds != false;
        different = different || appRoleAuthRole.tokenTtl != 10;
        different = different || appRoleAuthRole.tokenMaxTtl != 15;
        different = different || appRoleAuthRole.tokenPolicies == null || appRoleAuthRole.tokenPolicies.size() != 2;
        different = different || appRoleAuthRole.tokenBoundCidrs == null || appRoleAuthRole.tokenBoundCidrs.size() != 2;
        different = different || appRoleAuthRole.tokenExplicitMaxTtl != 20;
        different = different || appRoleAuthRole.tokenNoDefaultPolicy != true;
        different = different || appRoleAuthRole.tokenNumUses != 1;
        different = different || appRoleAuthRole.tokenPeriod != 30;
        different = different || !"service".equals(appRoleAuthRole.tokenType);
        if (different) {
            return "Approle not overriden: " + appRoleAuthRole;
        }

        // retrieve role ID
        String roleId = appRoleAuthService.getAppRoleRoleId(TEST_APP_ROLE);
        if (roleId == null) {
            return "approle role Id not correctly created";
        }

        // force role ID
        String uuid = UUID.randomUUID().toString();
        appRoleAuthService.setAppRoleRoleId(TEST_APP_ROLE, String.valueOf(uuid));
        String newRoleId = appRoleAuthService.getAppRoleRoleId(TEST_APP_ROLE);
        if (!uuid.equals(newRoleId)) {
            return "Bad role Id, expected:" + uuid + ", actual: " + newRoleId;
        }

        // creates secret ID
        // need to set bind secret ID flag
        appRoleAuthService
                .createOrUpdateAppRole(TEST_APP_ROLE, newAppRoleWithDefaultsOverriden.setBindSecretId(Boolean.TRUE));
        VaultAppRoleSecretId secretId = appRoleAuthService
                .createNewSecretId(TEST_APP_ROLE, new VaultAppRoleSecretIdRequest());
        if (secretId == null || secretId.secretId == null || secretId.secretIdAccessor == null) {
            return "secret ID creation failed, secretId: " + secretId + secretId == null ? ""
                    : (", secretId.secretId:" + secretId.secretId + ", secretId.secretIdAccessor:" + secretId.secretIdAccessor);
        }

        // verify that new secretAccesssorId apears in list
        List<String> secretIdAccessors = appRoleAuthService.getSecretIdAccessors(TEST_APP_ROLE);
        if (secretIdAccessors == null || !secretIdAccessors.contains(secretId.secretIdAccessor)) {
            return "new secretAccesssorId doesn't appear in list";
        }

        // try to retrieve secret id
        VaultAppRoleSecretIdAccessor secretIdRetrieved = appRoleAuthService
                .getSecretId(TEST_APP_ROLE, secretId.secretId);
        if (!secretIdRetrieved.secretIdAccessor.equals(secretIdAccessors.get(0))) {
            return "wrong secretIdAccessor, expected:" + secretIdAccessors.get(0) + ", actual:"
                    + secretIdRetrieved.secretIdAccessor;
        }

        // creates custom secret ID
        VaultAppRoleSecretId customSecretId = appRoleAuthService
                .createCustomSecretId(TEST_APP_ROLE, new VaultAppRoleSecretIdRequest().setSecretId("HelloWorld"));
        if (customSecretId == null || !"HelloWorld".equals(customSecretId.secretId)) {
            return "bad custom secretId, expected: HelloWorld, actual:" + customSecretId.secretId;
        }

        // verify that new secretAccesssorId apears in list
        secretIdAccessors = appRoleAuthService.getSecretIdAccessors(TEST_APP_ROLE);
        if (secretIdAccessors == null || !secretIdAccessors.contains(customSecretId.secretIdAccessor)) {
            return "new custom secretAccesssorId doesn't appear in list";
        }

        // delete secret id
        appRoleAuthService.deleteSecretId(TEST_APP_ROLE, secretId.secretId);
        VaultAppRoleSecretIdAccessor secretIdMissing = appRoleAuthService.getSecretId(TEST_APP_ROLE, secretId.secretId);
        if (secretIdMissing != null) {
            return "secretId should have been deleted";
        }

        return "OK";
    }

    protected void keyAdminTest() {

        transit.createKey(KEY_NAME, new KeyCreationRequestDetail().setExportable(true));
        transit.readKey(KEY_NAME);
        transit.listKeys();
        transit.exportKey(KEY_NAME, VaultTransitExportKeyType.encryption, null);
        transit.updateKeyConfiguration(KEY_NAME, new KeyConfigRequestDetail().setDeletionAllowed(true));
        transit.deleteKey(KEY_NAME);
    }

}
