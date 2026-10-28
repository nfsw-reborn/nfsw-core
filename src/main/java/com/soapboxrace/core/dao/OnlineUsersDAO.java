/*
 * This file is part of the Soapbox Race World core source code.
 * If you use any of this code for third-party purposes, please provide attribution.
 * Copyright (c) 2020.
 */

package com.soapboxrace.core.dao;

import com.soapboxrace.core.dao.util.LongKeyedDAO;
import com.soapboxrace.core.jpa.OnlineUsersEntity;

import javax.ejb.Stateless;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.List;

@Stateless
public class OnlineUsersDAO extends LongKeyedDAO<OnlineUsersEntity> {

    public OnlineUsersDAO() {
        super(OnlineUsersEntity.class);
    }

    public void saveSnapshot(OnlineUsersEntity snapshot) {
        // Startup and scheduled sampling can share the same second. Treat that
        // timestamp as one snapshot, including across application restarts.
        entityManager.createNativeQuery("INSERT INTO online_users (ID, numberOfOnline, numberOfRegistered) " +
                "VALUES (:time, :online, :registered) ON DUPLICATE KEY UPDATE " +
                "numberOfOnline=VALUES(numberOfOnline), numberOfRegistered=VALUES(numberOfRegistered)")
                .setParameter("time", snapshot.getTimeRecord())
                .setParameter("online", snapshot.getNumberOfOnline())
                .setParameter("registered", snapshot.getNumberOfRegistered())
                .executeUpdate();
    }

    public OnlineUsersEntity findByTime(Date time) {
        long timeLong = time.getTime() / 1000L;

        TypedQuery<OnlineUsersEntity> query = entityManager.createNamedQuery("OnlineUsersEntity.findByTime",
                OnlineUsersEntity.class);
        query.setParameter("time", (int) timeLong);

        List<OnlineUsersEntity> resultList = query.getResultList();
        return !resultList.isEmpty() ? resultList.get(0) : null;
    }

}
