package io.quarkus.vault.client;

import java.util.List;

public class VaultClientException extends VaultException {

    private String operationName;
    private String requestPath;
    private int status;
    private List<String> errors;
    private String body;

    public VaultClientException(String operationName, String requestPath, int status, String body) {
        this.operationName = operationName;
        this.requestPath = requestPath;
        this.status = status;
        this.body = body;
    }

    public VaultClientException(String operationName, String requestPath, int status, List<String> errors) {
        this.operationName = operationName;
        this.requestPath = requestPath;
        this.status = status;
        this.errors = errors;
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
}
