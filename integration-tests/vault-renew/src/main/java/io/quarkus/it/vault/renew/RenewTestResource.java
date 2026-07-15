package io.quarkus.it.vault.renew;

import java.sql.SQLException;

import javax.sql.DataSource;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkus.vault.VaultKVSecretEngine;

@Path("/renew")
@Produces(MediaType.TEXT_PLAIN)
public class RenewTestResource {

    @Inject
    Instance<DataSource> dataSource;

    @Inject
    VaultKVSecretEngine kv;

    /**
     * Reads from the demo table, using a connection obtained with the Vault dynamic database
     * credentials. Returns "ok", or a 500 with the SQL error message.
     */
    @GET
    @Path("db")
    public String db() {
        try (var connection = dataSource.get().getConnection();
                var statement = connection.createStatement();
                var resultSet = statement.executeQuery("SELECT name FROM demo")) {
            resultSet.next();
            return resultSet.getString(1);
        } catch (SQLException e) {
            throw new WebApplicationException(
                    Response.serverError().type(MediaType.TEXT_PLAIN).entity("DB access failed: " + e).build());
        }
    }

    /**
     * Reads a KV secret: an arbitrary Vault access giving the client the opportunity to extend the
     * login token.
     */
    @GET
    @Path("kv")
    public String kv() {
        return kv.readSecret("foo").get("greeting");
    }
}
