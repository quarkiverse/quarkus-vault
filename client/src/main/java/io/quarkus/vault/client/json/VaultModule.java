package io.quarkus.vault.client.json;

import java.time.Duration;

import io.quarkus.vault.client.api.secrets.transit.VaultSecretsTransitKeyVersion;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.module.SimpleModule;

public class VaultModule extends SimpleModule {

    public static final VaultModule INSTANCE = new VaultModule();

    public VaultModule() {
        super("VaultModule");
        addSerializer(Duration.class, new VaultDurationStringSerializer());
        addDeserializer(Duration.class, new VaultDurationStringDeserializer());
        setDeserializerModifier(new ValueDeserializerModifier() {
            @Override
            public ValueDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription.Supplier beanDescRef,
                    ValueDeserializer<?> deserializer) {
                if (VaultSecretsTransitKeyVersion.class.isAssignableFrom(beanDescRef.getBeanClass())) {
                    return new VaultSecretsTransitKeyVersionDeserializer(deserializer);
                }
                return deserializer;
            }
        });
    }

}
