package io.quarkus.vault.client.common;

import java.util.List;
import java.util.Map.Entry;

public class VaultResponse<T> {

    public final VaultRequest<T> request;

    public final int statusCode;

    public final List<Entry<String, String>> headers;

    public final byte[] body;

    public VaultResponse(VaultRequest<T> request, int statusCode, List<Entry<String, String>> headers, byte[] body) {
        this.request = request;
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
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
        return request.getResultExtractor().extract(this);
    }

}
