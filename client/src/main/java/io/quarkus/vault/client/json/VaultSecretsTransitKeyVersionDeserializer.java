package io.quarkus.vault.client.json;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.OffsetDateTime;

import io.quarkus.vault.client.api.secrets.transit.VaultSecretsTransitKeyVersion;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.DelegatingDeserializer;

public class VaultSecretsTransitKeyVersionDeserializer extends DelegatingDeserializer {

    public VaultSecretsTransitKeyVersionDeserializer(ValueDeserializer<?> defaultDeserializer) {
        super(defaultDeserializer);
    }

    @Override
    protected ValueDeserializer<?> newDelegatingInstance(ValueDeserializer<?> newDelegatee) {
        return new VaultSecretsTransitKeyVersionDeserializer(newDelegatee);
    }

    @Override
    public VaultSecretsTransitKeyVersion deserialize(JsonParser p, DeserializationContext ctxt) {
        if (p.currentToken().isNumeric()) {
            var creationTime = OffsetDateTime.ofInstant(Instant.ofEpochSecond(p.readValueAs(Long.class)), UTC);
            return new VaultSecretsTransitKeyVersion()
                    .setCreationTime(creationTime);
        } else {
            return (VaultSecretsTransitKeyVersion) super.deserialize(p, ctxt);
        }
    }

}
