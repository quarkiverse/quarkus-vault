package io.quarkus.vault.client;

import java.util.List;

public class VaultClientException extends VaultException {

    private final String operationName;
    private final String requestPath;
    private final int status;
    private final List<String> errors;
    private final String body;

    public VaultClientException(String operationName, String requestPath, int status, String body) {
        this.operationName = operationName;
        this.requestPath = requestPath;
        this.status = status;
        this.body = body;
        this.errors = List.of();
    }

    public VaultClientException(String operationName, String requestPath, int status, List<String> errors) {
        this.operationName = operationName;
        this.requestPath = requestPath;
        this.status = status;
        this.errors = errors;
        this.body = null;
    }

    public String getOperationName() {
        return operationName;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public int getStatus() {
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
