package io.quarkus.it.vault;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/vault")
public class VaultTestResource {

    @Inject
    VaultTestService vaultTestService;

    @GET
    @Produces(TEXT_PLAIN)
    public String test() {
        return vaultTestService.test();
    }

}
