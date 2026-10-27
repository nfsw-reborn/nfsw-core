package com.soapboxrace.core.bo.admin;

import javax.ejb.ApplicationException;

/** Carries a safe status across EJB boundaries and rolls back the entire mutation. */
@ApplicationException(rollback = true)
public class AdminPanelException extends RuntimeException {
    private final int status;
    public AdminPanelException(int status) { super("Admin operation rejected"); this.status = status; }
    public int getStatus() { return status; }
}
