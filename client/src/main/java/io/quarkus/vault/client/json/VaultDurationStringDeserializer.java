package io.quarkus.vault.client.json;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class VaultDurationStringDeserializer extends StdDeserializer<Duration> {

    public VaultDurationStringDeserializer() {
        super(Duration.class);
    }

    @Override
    public Duration deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        switch (p.currentToken()) {
            case VALUE_STRING:
                return Duration.parse("PT" + p.getText().toUpperCase(Locale.ROOT));
            case VALUE_NUMBER_INT:
                return Duration.ofSeconds(p.getLongValue());
            default:
                throw ctxt.wrongTokenException(p, Duration.class, p.currentToken(), "Expected string or number");
        }
    }
}
