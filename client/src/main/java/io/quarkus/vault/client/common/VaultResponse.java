package io.quarkus.vault.client.common;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

public class VaultResponse<T> {

    private final VaultRequest<T> request;

    private final int statusCode;

    private final List<Entry<String, String>> headers;

    private final byte[] body;

    public VaultResponse(VaultRequest<T> request, int statusCode, List<Entry<String, String>> headers, byte[] body) {
        this.request = request;
        this.statusCode = statusCode;
        this.headers = headers != null ? headers : List.of();
        this.body = body;
    }

    public VaultRequest<T> getRequest() {
        return request;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public List<Entry<String, String>> getHeaders() {
        return headers;
    }

    public Optional<byte[]> getBody() {
        if (body == null || body.length == 0) {
            return Optional.empty();
        }
        return Optional.of(body);
    }

    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }

    public boolean isStatusCodeExpected() {
        var expected = request.getExpectedStatusCodes();
        return expected.isEmpty() || expected.contains(statusCode);
    }

    public boolean isUpgradedResponse() {
        return statusCode == 200 && request.getExpectedStatusCodes().equals(List.of(204));
    }

    public T getResult() {
        return request.getResultExtractor().extract(this)
                .orElse(null);
    }

    public Optional<String> getBodyAsString() {
        return Optional.ofNullable(body).map(String::new);
    }
}
