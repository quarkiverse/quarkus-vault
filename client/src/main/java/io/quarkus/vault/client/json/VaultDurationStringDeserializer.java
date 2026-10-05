package io.quarkus.vault.client.json;

import java.time.Duration;
import java.util.Locale;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

public class VaultDurationStringDeserializer extends StdDeserializer<Duration> {

    public VaultDurationStringDeserializer() {
        super(Duration.class);
    }

    @Override
    public Duration deserialize(JsonParser p, DeserializationContext ctxt) {
        switch (p.currentToken()) {
            case VALUE_STRING:
                return Duration.parse("PT" + p.getString().toUpperCase(Locale.ROOT));
            case VALUE_NUMBER_INT:
                return Duration.ofSeconds(p.getLongValue());
            default:
                throw ctxt.wrongTokenException(p, Duration.class, p.currentToken(), "Expected string or number");
        }
    }
}
