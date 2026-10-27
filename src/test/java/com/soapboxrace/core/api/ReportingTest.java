package com.soapboxrace.core.api;

import com.soapboxrace.core.bo.HardwareInfoBO;
import com.soapboxrace.core.bo.RequestSessionInfo;
import com.soapboxrace.core.dao.UserDAO;
import com.soapboxrace.core.jpa.HardwareInfoEntity;
import com.soapboxrace.core.jpa.UserEntity;
import com.soapboxrace.jaxb.http.HardwareInfo;
import com.soapboxrace.jaxb.util.JAXBUtility;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class ReportingTest {
    private void set(Reporting target, String name, Object value) throws Exception {
        Field field = Reporting.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
    private void acceptsVendor(String vendor) throws Exception {
        Reporting reporting = new Reporting();
        UserEntity user = new UserEntity();
        boolean[] saved = {false};
        set(reporting, "hardwareInfoBO", new HardwareInfoBO() {
            @Override public HardwareInfoEntity save(HardwareInfo info) {
                assertEquals(vendor, info.getCpuid0());
                HardwareInfoEntity result = new HardwareInfoEntity();
                result.setHardwareHash("test-hardware-hash");
                return result;
            }
        });
        set(reporting, "requestSessionInfo", new RequestSessionInfo() {
            @Override public UserEntity getUser() { return user; }
        });
        set(reporting, "userDAO", new UserDAO() {
            @Override public void update(UserEntity value) {
                assertSame(user, value);
                saved[0] = true;
            }
        });
        HardwareInfo info = new HardwareInfo();
        info.setCpuid0(vendor);
        String xml = JAXBUtility.marshal(info);
        assertEquals("", reporting.sendHardwareInfo(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
        assertTrue(saved[0]);
        assertEquals("test-hardware-hash", user.getGameHardwareHash());
    }
    @Test public void acceptsWindowsArmEmulation() throws Exception { acceptsVendor("MicrosoftXTA"); }
    @Test public void preservesIntelTelemetry() throws Exception { acceptsVendor("GenuineIntel"); }
    @Test public void preservesAmdTelemetry() throws Exception { acceptsVendor("AuthenticAMD"); }
    @Test public void acceptsMissingOptionalVendor() throws Exception { acceptsVendor(null); }
}
