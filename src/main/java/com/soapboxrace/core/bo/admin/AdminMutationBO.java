package com.soapboxrace.core.bo.admin;

import com.google.gson.Gson;
import com.soapboxrace.core.bo.BanBO;
import com.soapboxrace.core.jpa.PersonaEntity;
import com.soapboxrace.core.jpa.UserEntity;
import javax.ejb.*;
import javax.persistence.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Stateless
public class AdminMutationBO {
    @PersistenceContext private EntityManager em;
    @EJB private BanBO bans;
    private final Gson json = new Gson();

    // Called while TokenSessionBO holds its WRITE lock. Commit before releasing the login gate.
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String apply(Long id, AdminCommand command) {
        command.validate();
        Number existing = (Number) em.createNativeQuery("SELECT COUNT(*) FROM admin_audit WHERE request_id=?1")
            .setParameter(1, command.requestId).getSingleResult();
        if (existing.longValue() != 0) throw new AdminPanelException(409);
        UserEntity user = em.find(UserEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (user == null) throw new AdminPanelException(404);
        Map<String, Object> before = new LinkedHashMap<>();
        Map<String, Object> after = new LinkedHashMap<>();
        switch (command.action) {
            case "account":
                if (command.email != null) {
                    Long duplicates = em.createQuery("SELECT COUNT(u) FROM UserEntity u WHERE u.email=:email AND u.id<>:id", Long.class)
                        .setParameter("email", command.email).setParameter("id", id).getSingleResult();
                    if (duplicates > 0) throw new AdminPanelException(409);
                    before.put("email", user.getEmail()); user.setEmail(command.email); after.put("email", user.getEmail());
                }
                if (command.isLocked != null) { before.put("isLocked", user.isLocked()); user.setLocked(command.isLocked); after.put("isLocked", user.isLocked()); }
                if (command.isAdmin != null) { before.put("isAdmin", user.isAdmin()); user.setAdmin(command.isAdmin); after.put("isAdmin", user.isAdmin()); }
                if (command.premium != null) { before.put("premium", user.isPremium()); user.setPremium(command.premium); after.put("premium", user.isPremium()); }
                break;
            case "password":
                user.setPassword(legacyHash(command.password));
                after.put("passwordChanged", true); // Never store passwords or password hashes in audit.
                break;
            case "persona":
                PersonaEntity persona = em.find(PersonaEntity.class, command.personaId, LockModeType.PESSIMISTIC_WRITE);
                if (persona == null || !persona.getUser().getId().equals(id)) throw new AdminPanelException(404);
                before.put("personaId", persona.getPersonaId()); after.put("personaId", persona.getPersonaId());
                if (command.cash != null) { before.put("cash", persona.getCash()); persona.setCash(command.cash); after.put("cash", persona.getCash()); }
                if (command.boost != null) { before.put("boost", persona.getBoost()); persona.setBoost(command.boost); after.put("boost", persona.getBoost()); }
                if (command.motto != null) { before.put("motto", persona.getMotto()); persona.setMotto(command.motto); after.put("motto", persona.getMotto()); }
                break;
            case "ban":
                if (bans.isBanned(user)) throw new AdminPanelException(409);
                bans.banUser(user, null, command.reason, command.endsAt == null ? null : LocalDateTime.parse(command.endsAt));
                before.put("banned", false); after.put("banned", true); after.put("endsAt", command.endsAt);
                break;
            case "unban":
                if (!bans.isBanned(user)) throw new AdminPanelException(409);
                bans.unbanUser(user); before.put("banned", true); after.put("banned", false);
                break;
            default: throw new IllegalArgumentException();
        }
        em.flush();
        em.createNativeQuery("INSERT INTO admin_audit (request_id,user_id,action,reason,before_data,after_data,created_at) VALUES (?1,?2,?3,?4,?5,?6,UTC_TIMESTAMP())")
            .setParameter(1, command.requestId).setParameter(2, id).setParameter(3, command.action).setParameter(4, command.reason.trim())
            .setParameter(5, json.toJson(before)).setParameter(6, json.toJson(after)).executeUpdate();
        return "{\"ok\":true}";
    }
    public static String legacyHash(String password) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(password.getBytes(StandardCharsets.US_ASCII));
            StringBuilder out = new StringBuilder();
            for (byte value : digest) out.append(String.format("%02x", value & 0xff));
            return out.toString();
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
