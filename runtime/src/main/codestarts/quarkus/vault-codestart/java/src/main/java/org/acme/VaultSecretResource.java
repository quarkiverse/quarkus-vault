package org.acme;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.vault.VaultKVSecretEngine;

@Path("/vault")
public class VaultSecretResource {

    @ConfigProperty(name = "greeting")
    String greeting;

    @Inject
    VaultKVSecretEngine kvSecretEngine;

    @GET
    @Path("/config")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSecretFromConfig() {
        return greeting;
    }

    @GET
    @Path("/engine")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSecretFromEngine() {
        Map<String, String> secret = kvSecretEngine.readSecret("myapps/vault-codestart");
        return secret != null ? secret.get("greeting") : null;
    }
}

