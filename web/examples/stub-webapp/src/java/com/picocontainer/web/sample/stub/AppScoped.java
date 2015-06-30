package com.picocontainer.web.sample.stub;

import java.io.Serializable;

@SuppressWarnings("serial")
public class AppScoped implements Serializable {

    private int counter;

    public AppScoped() {
    }

    public String getCounter() {
        return "<br/> &nbsp;&nbsp;&nbsp; AppScoped id:" + System.identityHashCode(this) + ", counter: "+ ++counter;
    }
}
