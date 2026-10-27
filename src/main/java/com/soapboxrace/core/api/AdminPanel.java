package com.soapboxrace.core.api;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.soapboxrace.core.bo.TokenSessionBO;
import com.soapboxrace.core.bo.admin.AdminCommand;
import com.soapboxrace.core.bo.admin.AdminPanelException;
import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;

/** Internal bridge for the separate admin service. Disabled without a server-side key. */
@Path("AdminPanel")
@Produces(MediaType.APPLICATION_JSON)
public class AdminPanel {
    @EJB private TokenSessionBO sessions;
    private static final Gson JSON = new Gson();

    public static boolean authorized(String expected, String header) {
        return expected != null && expected.length() >= 32 && header != null &&
            MessageDigest.isEqual(("Bearer " + expected).getBytes(StandardCharsets.UTF_8), header.getBytes(StandardCharsets.UTF_8));
    }
    private boolean authorize(String header) {
        return authorized(System.getenv("NFSW_ADMIN_API_KEY"), header);
    }
    @GET @Path("sessions")
    public Response sessions(@HeaderParam("Authorization") String authorization) {
        if (!authorize(authorization)) return Response.status(404).build();
        return Response.ok(JSON.toJson(sessions.adminSessionSummary())).build();
    }
    @POST @Path("users/{id}/commands") @Consumes(MediaType.APPLICATION_JSON)
    public Response command(@HeaderParam("Authorization") String authorization, @PathParam("id") Long id, String body) {
        if (!authorize(authorization)) return Response.status(404).build();
        try {
            if (body == null || body.length() > 8192) throw new IllegalArgumentException("Invalid command");
            AdminCommand command = JSON.fromJson(body, AdminCommand.class);
            if (command == null) throw new IllegalArgumentException("Invalid command");
            command.validate();
            return Response.ok(sessions.runAdminCommand(id, command)).build();
        } catch (AdminPanelException e) {
            return Response.status(e.getStatus()).entity("{\"message\":\"Operation rejected\"}").build();
        } catch (IllegalArgumentException | JsonParseException e) {
            return Response.status(400).entity(JSON.toJson(Collections.singletonMap("message", "Invalid command"))).build();
        } catch (RuntimeException e) {
            // The legacy global mapper exposes XML stack traces. Keep the admin boundary opaque.
            return Response.status(500).entity("{\"message\":\"Admin operation failed\"}").build();
        }
    }
}
