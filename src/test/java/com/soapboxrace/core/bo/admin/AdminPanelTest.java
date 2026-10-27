package com.soapboxrace.core.bo.admin;

import com.soapboxrace.core.api.AdminPanel;
import com.soapboxrace.core.bo.TokenSessionBO;
import com.soapboxrace.core.jpa.TokenSessionEntity;
import com.soapboxrace.core.jpa.UserEntity;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;
import static org.junit.Assert.*;

public class AdminPanelTest {
    private AdminCommand command(String action) {
        AdminCommand c = new AdminCommand(); c.action=action; c.reason="Admin test"; c.requestId="12345678-1234-1234-1234-123456789012"; return c;
    }
    @Test public void bridgeIsDisabledWithoutStrongConfiguredKey() {
        assertFalse(AdminPanel.authorized(null, "Bearer key"));
        assertFalse(AdminPanel.authorized("", "Bearer "));
        String key="01234567890123456789012345678901";
        assertTrue(AdminPanel.authorized(key, "Bearer " + key));
        assertFalse(AdminPanel.authorized(key, "Bearer " + key + "x"));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsUnknownAction() { command("sql").validate(); }
    @Test(expected=IllegalArgumentException.class) public void requiresReason() { AdminCommand c=command("unban"); c.reason=""; c.validate(); }
    @Test(expected=IllegalArgumentException.class) public void rejectsNanBalance() { AdminCommand c=command("persona"); c.personaId=1L; c.cash=Double.NaN; c.validate(); }
    @Test(expected=IllegalArgumentException.class) public void rejectsNegativeBalance() { AdminCommand c=command("persona"); c.personaId=1L; c.cash=-1.0; c.validate(); }
    @Test(expected=IllegalArgumentException.class) public void rejectsNonAsciiGamePassword() { AdminCommand c=command("password"); c.password="haslozzzzzzzzż"; c.validate(); }
    @Test public void validatesAccountAndCurrencyCommands() {
        AdminCommand account=command("account"); account.email="admin@example.com"; account.validate();
        AdminCommand persona=command("persona"); persona.personaId=1L; persona.cash=123.0; persona.validate();
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", AdminMutationBO.legacyHash("abc"));
    }
    @SuppressWarnings("unchecked")
    @Test public void activeGameSessionBlocksEditsAndNeverLeaksToken() throws Exception {
        TokenSessionBO bo=new TokenSessionBO();
        Field keys=TokenSessionBO.class.getDeclaredField("sessionKeyToTokenMap"); keys.setAccessible(true);
        Field users=TokenSessionBO.class.getDeclaredField("userIdToSessionKeyMap"); users.setAccessible(true);
        UserEntity user=new UserEntity(); user.setId(7L);
        TokenSessionEntity session=new TokenSessionEntity(); session.setUserEntity(user); session.setExpirationDate(new Date(System.currentTimeMillis()+60000)); session.setActivePersonaId(100L);
        ((Map<String,TokenSessionEntity>)keys.get(bo)).put("secret-token",session);
        ((Map<Long,String>)users.get(bo)).put(7L,"secret-token");
        try { bo.runAdminCommand(7L,command("unban")); fail("must block online edits"); }
        catch (AdminPanelException e) { assertEquals(409,e.getStatus()); }
        assertEquals(1,bo.adminSessionSummary().size());
        assertFalse(bo.adminSessionSummary().get(0).containsValue("secret-token"));
        session.setExpirationDate(new Date(0)); assertTrue(bo.adminSessionSummary().isEmpty());
    }
}
