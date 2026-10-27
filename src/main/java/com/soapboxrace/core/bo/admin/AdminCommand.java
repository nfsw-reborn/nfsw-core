package com.soapboxrace.core.bo.admin;

/** Explicit allowlist; arbitrary columns/SQL and client-supplied hashes are never accepted. */
public class AdminCommand {
    public String action;
    public String reason;
    public String requestId;
    public Boolean isLocked;
    public Boolean isAdmin;
    public Boolean premium;
    public String email;
    public String password;
    public Long personaId;
    public Double cash;
    public Double boost;
    public String motto;
    public String endsAt;

    public void validate() {
        if (reason == null || reason.trim().length() < 3 || reason.length() > 500 || requestId == null ||
            !requestId.matches("[a-fA-F0-9-]{36}")) throw new IllegalArgumentException();
        if (action == null) throw new IllegalArgumentException();
        switch (action) {
            case "account":
                if (isLocked == null && isAdmin == null && premium == null && email == null) throw new IllegalArgumentException();
                if (email != null && (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") || email.length() > 254)) throw new IllegalArgumentException();
                break;
            case "password":
                // The game launcher uses SHA-1; keep its input within the launcher's ASCII contract.
                if (password == null || !password.matches("[ -~]{12,64}")) throw new IllegalArgumentException();
                break;
            case "persona":
                if (personaId == null || personaId <= 0 || (cash == null && boost == null && motto == null)) throw new IllegalArgumentException();
                if (cash != null && (!Double.isFinite(cash) || cash < 0 || cash > 2000000000 || cash != Math.floor(cash))) throw new IllegalArgumentException();
                if (boost != null && (!Double.isFinite(boost) || boost < 0 || boost > 2000000000 || boost != Math.floor(boost))) throw new IllegalArgumentException();
                if (motto != null && motto.length() > 255) throw new IllegalArgumentException();
                break;
            case "ban":
                if (endsAt != null && !java.time.LocalDateTime.parse(endsAt).isAfter(java.time.LocalDateTime.now())) throw new IllegalArgumentException();
                break;
            case "unban": break;
            default: throw new IllegalArgumentException();
        }
    }
}
