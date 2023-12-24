package io.quarkus.vault.client.json;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.quarkus.vault.client.api.secrets.transit.VaultSecretsTransitKeyVersion;

public class VaultModule extends SimpleModule {

    public static final VaultModule INSTANCE = new VaultModule();

    public VaultModule() {
        super("VaultModule");
        setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc,
                    JsonDeserializer<?> deserializer) {
                if (VaultSecretsTransitKeyVersion.class.isAssignableFrom(beanDesc.getBeanClass())) {
                    return new VaultSecretsTransitKeyVersionDeserializer(deserializer);
                }
                return deserializer;
            }
        });
    }

}
