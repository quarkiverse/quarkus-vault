package io.quarkus.vault.client.json;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class VaultDurationStringSerializer extends StdSerializer<Duration> {

    public VaultDurationStringSerializer() {
        super(Duration.class);
    }

    @Override
    public void serialize(Duration value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        var fmt = value.toString();
        if (fmt.startsWith("PT")) {
            fmt = fmt.substring(2);
        }
        fmt = fmt.toLowerCase(Locale.ROOT);
        gen.writeString(fmt);
    }
}
