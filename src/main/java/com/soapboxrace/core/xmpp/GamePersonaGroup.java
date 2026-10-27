package com.soapboxrace.core.xmpp;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/** XML contract of the Soapbox Openfire REST plugin's persona group endpoint. */
@XmlRootElement(name = "gamePersonaGroup")
public class GamePersonaGroup {
    private List<Long> members = new ArrayList<>();

    @XmlElement(name = "member")
    public List<Long> getMembers() {
        return members;
    }

    public void setMembers(List<Long> members) {
        this.members = members == null ? new ArrayList<>() : members;
    }
}
