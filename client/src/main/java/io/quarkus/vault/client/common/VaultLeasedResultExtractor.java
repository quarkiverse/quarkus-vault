package io.quarkus.vault.client.common;

import java.util.Optional;
import java.util.logging.Logger;

import io.quarkus.vault.client.api.common.VaultLeasedResult;

public class VaultLeasedResultExtractor<T extends VaultLeasedResult<?, ?>> implements VaultResultExtractor<T> {

    private static final Logger log = Logger.getLogger(VaultLeasedResultExtractor.class.getName());

    private final Class<T> resultClass;

    public VaultLeasedResultExtractor(Class<T> resultClass) {
        this.resultClass = resultClass;
    }

    public static <T extends VaultLeasedResult<?, ?>> VaultLeasedResultExtractor<T> of(Class<T> resultClass) {
        return new VaultLeasedResultExtractor<>(resultClass);
    }

    @Override
    public Optional<T> extract(VaultResponse<T> response) {
        var extracted = VaultJSONResultExtractor.extract(response, resultClass);
        extracted.ifPresent(result -> {
            if (result.getWarnings() != null) {
                for (var warning : result.getWarnings()) {
                    log.warning(warning);
                }
            }
        });
        return extracted;
    }

}
