package io.quarkus.vault.client.common;

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
    public T extract(VaultResponse<T> response) {
        var result = VaultJSONResultExtractor.extract(response, resultClass);
        if (result != null && result.warnings != null) {
            for (var warning : result.warnings) {
                log.warning(warning);
            }
        }
        return result;
    }

}
