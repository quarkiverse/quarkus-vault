package io.quarkus.vault.client;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.vault.client.common.VaultRequest;
import io.quarkus.vault.client.common.VaultResponse;

public class VaultClientException extends VaultException {

    private final String operationName;
    private final String requestPath;
    private final Integer status;
    private final List<String> errors;
    private final String body;

    public VaultClientException(VaultRequest<?> request, Integer status, List<String> errors, Throwable cause) {
        this(request.getOperation(), request.getUrl().toString(), status, errors, null, cause);
    }

    public VaultClientException(VaultRequest<?> request, Integer status, String body, Throwable cause) {
        this(request.getOperation(), request.getUrl().toString(), status, null, body, cause);
    }

    public VaultClientException(VaultResponse<?> response, List<String> errors, Throwable cause) {
        this(response.getRequest().getOperation(), response.getRequest().getUrl().toString(), response.getStatusCode(),
                errors, cause);
    }

    public VaultClientException(String operationName, String requestPath, Integer status, String body, Throwable cause) {
        this(operationName, requestPath, status, null, body, cause);
    }

    public VaultClientException(String operationName, String requestPath, Integer status, List<String> errors,
            Throwable cause) {
        this(operationName, requestPath, status, errors, null, cause);
    }

    private VaultClientException(String operationName, String requestPath, Integer status, List<String> errors,
            String body, Throwable cause) {
        super(formatMessage(operationName, requestPath, status, errors), cause);
        this.operationName = operationName;
        this.requestPath = requestPath;
        this.status = status;
        this.errors = errors;
        this.body = body;
    }

    public String getOperationName() {
        return operationName;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public Integer getStatus() {
        return status;
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getBody() {
        return body;
    }

    public boolean isPermissionDenied() {
        return status == 403;
    }

    public VaultClientException withError(String error) {
        ArrayList<String> errors = this.errors == null ? new ArrayList<>() : new ArrayList<>(this.errors);
        errors.add(0, error);
        return new VaultClientException(operationName, requestPath, status, errors, body, getCause());
    }

    static String formatMessage(String operationName, String requestPath, Integer status, List<String> errors) {
        StringBuilder sb = new StringBuilder();
        sb.append(operationName).append("' at path '").append(requestPath).append("'");
        if (status != null) {
            sb.append(" with status ").append(status);
        }
        if (errors != null && !errors.isEmpty()) {
            sb.append("\n").append("errors:").append("\n").append(String.join("\n", errors));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "VaultClientException{" +
                "operationName='" + operationName + '\'' +
                ", requestPath='" + requestPath + '\'' +
                ", status=" + status +
                (errors != null ? ", errors=" + errors : "") +
                (body != null ? ", body='" + body + '\'' : "") +
                '}';
    }

    public boolean hasErrorContaining(String message) {
        return errors != null && errors.stream().anyMatch(e -> e.contains(message));
    }
}
