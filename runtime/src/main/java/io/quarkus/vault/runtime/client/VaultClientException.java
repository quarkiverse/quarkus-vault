package io.quarkus.vault.runtime.client;

import io.quarkus.vault.VaultException;

public class VaultClientException extends VaultException {

    private final String operationName;
    private final String requestPath;
    private final int status;
    private final String body;

    public VaultClientException(String operationName, String requestPath, int status, String body) {
        this.operationName = operationName;
        this.requestPath = requestPath;
        this.status = status;
        this.body = body;
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

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "VaultClientException{" +
                "operationName='" + operationName + '\'' +
                ", requestPath='" + requestPath + '\'' +
                ", status=" + status +
                ", body='" + body + '\'' +
                '}';
    }
}
