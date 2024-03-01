package io.quarkus.vault.client.common;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VaultErrorResponse(List<String> errors) {
}
