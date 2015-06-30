package com.picocontainer.web.sample.stub;

import java.io.Serializable;

@SuppressWarnings("serial")
public class RequestScoped implements Serializable {

    private SessionScoped sessionScopeComp;
    private int counter;

    public RequestScoped(SessionScoped sessionScopeComp) {
        this.sessionScopeComp = sessionScopeComp;
    }

    public String getCounterAndDependantsCounters() {
        return sessionScopeComp.getCounterAndDependantsCounters() + "<br/> &nbsp;&nbsp;&nbsp; RequestScoped id: " + System.identityHashCode(this) + ", counter: " + ++counter;
    }

}