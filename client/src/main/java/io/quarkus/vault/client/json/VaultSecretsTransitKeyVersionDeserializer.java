package io.quarkus.vault.client.json;

import static java.time.ZoneOffset.UTC;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;

import io.quarkus.vault.client.api.secrets.transit.VaultSecretsTransitKeyVersion;

public class VaultSecretsTransitKeyVersionDeserializer extends DelegatingDeserializer {

    public VaultSecretsTransitKeyVersionDeserializer(JsonDeserializer<?> defaultDeserializer) {
        super(defaultDeserializer);
    }

    @Override
    protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee) {
        return new VaultSecretsTransitKeyVersionDeserializer(newDelegatee);
    }

    @Override
    public VaultSecretsTransitKeyVersion deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken().isNumeric()) {
            var creationTime = OffsetDateTime.ofInstant(Instant.ofEpochSecond(p.readValueAs(Long.class)), UTC);
            return new VaultSecretsTransitKeyVersion()
                    .setCreationTime(creationTime);
        } else {
            return (VaultSecretsTransitKeyVersion) super.deserialize(p, ctxt);
        }
    }

}
