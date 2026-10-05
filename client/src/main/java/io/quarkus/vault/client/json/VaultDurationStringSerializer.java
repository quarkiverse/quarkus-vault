package io.quarkus.vault.client.json;

import java.time.Duration;
import java.util.Locale;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class VaultDurationStringSerializer extends StdSerializer<Duration> {

    public VaultDurationStringSerializer() {
        super(Duration.class);
    }

    @Override
    public void serialize(Duration value, JsonGenerator gen, SerializationContext provider) {
        var fmt = value.toString();
        if (fmt.startsWith("PT")) {
            fmt = fmt.substring(2);
        }
        fmt = fmt.toLowerCase(Locale.ROOT);
        gen.writeString(fmt);
    }
}
