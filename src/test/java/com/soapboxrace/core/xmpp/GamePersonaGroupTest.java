package com.soapboxrace.core.xmpp;

import org.junit.Test;
import javax.xml.bind.JAXBContext;
import java.io.StringReader;
import java.util.Arrays;
import static org.junit.Assert.*;

public class GamePersonaGroupTest {
    private GamePersonaGroup parse(String xml) throws Exception {
        return (GamePersonaGroup) JAXBContext.newInstance(GamePersonaGroup.class)
            .createUnmarshaller().unmarshal(new StringReader(xml));
    }
    @Test public void parsesPluginMembers() throws Exception {
        assertEquals(Arrays.asList(10L, 20L), parse("<gamePersonaGroup><member>10</member><member>20</member></gamePersonaGroup>").getMembers());
    }
    @Test public void missingGroupIsEmpty() throws Exception {
        assertTrue(parse("<gamePersonaGroup/>").getMembers().isEmpty());
    }
}
