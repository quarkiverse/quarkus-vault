package io.quarkus.vault.client.test;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;

public class TickableInstanceSource implements InstantSource {

    private Instant instant;

    public TickableInstanceSource(Instant instant) {
        this.instant = instant;
    }

    public void tick(Duration duration) {
        instant = instant.plus(duration);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
