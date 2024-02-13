package io.quarkus.vault.runtime;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.vault.runtime.config.CredentialsProviderConfig;
import io.quarkus.vault.runtime.config.VaultAppRoleAuthenticationConfig;
import io.quarkus.vault.runtime.config.VaultAuthenticationConfig;
import io.quarkus.vault.runtime.config.VaultEnterpriseConfig;
import io.quarkus.vault.runtime.config.VaultKubernetesAuthenticationConfig;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;
import io.quarkus.vault.runtime.config.VaultTlsConfig;
import io.quarkus.vault.runtime.config.VaultTransitConfig;
import io.quarkus.vault.runtime.config.VaultUserpassAuthenticationConfig;
import io.quarkus.vault.test.VaultTestLifecycleManager;

@DisabledOnOs(OS.WINDOWS) // https://github.com/quarkusio/quarkus/issues/3796
@QuarkusTestResource(VaultTestLifecycleManager.class)
public class VaultJDKClientITCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-vault-jdk-client.properties", "application.properties"));

    @Test
    void appRole() throws IOException {

        VaultRuntimeConfig config = new VaultRuntimeConfigImpl() {
            @Override
            public VaultAuthenticationConfig authentication() {
                return new VaultAuthenticationConfigImpl() {
                    @Override
                    public VaultAppRoleAuthenticationConfig appRole() {
                        return new VaultAppRoleAuthenticationConfigImpl() {
                            @Override
                            public String authMountPath() {
                                return DEFAULT_APPROLE_AUTH_MOUNT_PATH;
                            }

                            @Override
                            public Optional<String> roleId() {
                                return Optional.of(ConfigProviderResolver.instance().getConfig().getValue("vault-test.role-id",
                                        String.class));
                            }

                            @Override
                            public Optional<String> secretId() {
                                return Optional.of(ConfigProviderResolver.instance().getConfig()
                                        .getValue("vault-test.secret-id", String.class));
                            }
                        };
                    }
                };
            }
        };

        JDKVaultClient vaultClient = new JDKVaultClient(config);
        var token = vaultClient.loginAppRole();
        Assertions.assertNotNull(token);

        Map<String, Object> configSecrets = vaultClient.readSecret("config");
        Map<String, Object> configJsonSecrets = vaultClient.readSecret("config-json");
        Assertions.assertEquals("{password=bar}", new TreeMap<>(configSecrets).toString());
        Assertions.assertEquals("{fooList=[foo1, foo2, foo3], fooMap={key1=value1, key2=value2}, isEnabled=true, nullFoo=null}",
                new TreeMap<>(configJsonSecrets).toString());
    }

    @Test
    void userpass() throws IOException {
        VaultRuntimeConfig config = new VaultRuntimeConfigImpl() {
            @Override
            public VaultAuthenticationConfig authentication() {
                return new VaultAuthenticationConfigImpl() {
                    @Override
                    public VaultUserpassAuthenticationConfig userpass() {
                        return new VaultUserpassAuthenticationConfigImpl() {
                            @Override
                            public Optional<String> username() {
                                return Optional.of("bob");
                            }

                            @Override
                            public Optional<String> password() {
                                return Optional.of("sinclair");
                            }
                        };
                    }
                };
            }
        };

        JDKVaultClient vaultClient = new JDKVaultClient(config);
        var token = vaultClient.loginUserPass();
        Assertions.assertNotNull(token);
    }

    private static class VaultRuntimeConfigImpl implements VaultRuntimeConfig {
        @Override
        public int configOrdinal() {
            return 0;
        }

        @Override
        public Optional<URL> url() {
            try {
                return Optional.of(new URL("https://localhost:8200"));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public VaultEnterpriseConfig enterprise() {
            return null;
        }

        @Override
        public VaultAuthenticationConfig authentication() {
            return null;
        }

        @Override
        public Duration renewGracePeriod() {
            return null;
        }

        @Override
        public Duration secretConfigCachePeriod() {
            return null;
        }

        @Override
        public Optional<List<String>> secretConfigKvPath() {
            return Optional.empty();
        }

        @Override
        public Map<String, KvPathConfig> secretConfigKvPathPrefix() {
            return null;
        }

        @Override
        public int mpConfigInitialAttempts() {
            return 0;
        }

        @Override
        public LogConfidentialityLevel logConfidentialityLevel() {
            return null;
        }

        @Override
        public int kvSecretEngineVersion() {
            return Integer.parseInt(KV_SECRET_ENGINE_VERSION_V2);
        }

        @Override
        public String kvSecretEngineMountPath() {
            return DEFAULT_KV_SECRET_ENGINE_MOUNT_PATH;
        }

        @Override
        public VaultTlsConfig tls() {
            return new VaultTlsConfig() {
                @Override
                public Optional<Boolean> skipVerify() {
                    return Optional.empty();
                }

                @Override
                public Optional<String> caCert() {
                    return Optional.of("src/test/resources/vault-tls.crt");
                }

                @Override
                public boolean useKubernetesCaCert() {
                    return false;
                }
            };
        }

        @Override
        public Duration connectTimeout() {
            return Duration.ofSeconds(5);
        }

        @Override
        public Duration readTimeout() {
            return Duration.ofSeconds(5);
        }

        @Override
        public Optional<List<String>> nonProxyHosts() {
            return Optional.empty();
        }

        @Override
        public Optional<String> proxyHost() {
            return Optional.empty();
        }

        @Override
        public Integer proxyPort() {
            return null;
        }

        @Override
        public Map<String, CredentialsProviderConfig> credentialsProvider() {
            return null;
        }

        @Override
        public VaultTransitConfig transit() {
            return null;
        }

        @Override
        public Map<String, String> devServices() {
            return null;
        }

        @Override
        public Map<String, String> health() {
            return null;
        }
    }

    static class VaultAuthenticationConfigImpl implements VaultAuthenticationConfig {

        @Override
        public Optional<String> clientToken() {
            return Optional.empty();
        }

        @Override
        public Optional<String> clientTokenWrappingToken() {
            return Optional.empty();
        }

        @Override
        public VaultAppRoleAuthenticationConfig appRole() {
            return null;
        }

        @Override
        public VaultUserpassAuthenticationConfig userpass() {
            return null;
        }

        @Override
        public VaultKubernetesAuthenticationConfig kubernetes() {
            return null;
        }
    }

    static class VaultUserpassAuthenticationConfigImpl implements VaultUserpassAuthenticationConfig {

        @Override
        public Optional<String> username() {
            return Optional.empty();
        }

        @Override
        public Optional<String> password() {
            return Optional.empty();
        }

        @Override
        public Optional<String> passwordWrappingToken() {
            return Optional.empty();
        }
    }

    static class VaultAppRoleAuthenticationConfigImpl implements VaultAppRoleAuthenticationConfig {

        @Override
        public Optional<String> roleId() {
            return Optional.empty();
        }

        @Override
        public Optional<String> secretId() {
            return Optional.empty();
        }

        @Override
        public Optional<String> secretIdWrappingToken() {
            return Optional.empty();
        }

        @Override
        public String authMountPath() {
            return null;
        }
    }

}
