package com.picocontainer.jetty;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

public class DependencyInjectionTestFilter implements Filter {

    private final Integer integer;
    private String foo;

    public DependencyInjectionTestFilter(final Integer integer) {
        this.integer = integer;
    }

    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String servletPath = req.getServletPath();
        if (servletPath.equals("/foo2")) {
            request.setAttribute("foo2", " Filtered!(int= " + integer + (foo != null? " " + foo : "" ) + ")");

        }
        chain.doFilter(request, response);
    }

    public void init(final FilterConfig filterConfig) throws ServletException {
        String initParameter = filterConfig.getInitParameter("foo");
        if (initParameter!= null) {
            foo = initParameter;
        }
    }

    public void destroy() {
    }

    // used when handling this filter directly rather than letting Jetty instantiate it.
    public void setFoo(final String foo) {
        this.foo = foo;
    }
}

