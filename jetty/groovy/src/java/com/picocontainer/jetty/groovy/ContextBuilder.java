/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 *****************************************************************************/

package com.picocontainer.jetty.groovy;

import groovy.util.NodeBuilder;

import java.util.EnumSet;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.Servlet;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import com.picocontainer.jetty.PicoContext;
import com.picocontainer.jetty.groovy.adapters.NodeBuilderAdapter;
import com.picocontainer.jetty.groovy.adapters.WaffleAdapter;

import com.picocontainer.MutablePicoContainer;

public class ContextBuilder extends NodeBuilder {
    private final MutablePicoContainer parentContainer;
    private final PicoContext context;

    public ContextBuilder(final MutablePicoContainer parentContainer, final PicoContext context) {
        this.parentContainer = parentContainer;
        this.context = context;
    }

    @Override
	protected Object createNode(final Object name, final Map map) {
        if (name.equals("filter")) {
            return makeFilter(map);
        } else if (name.equals("servlet")) {
            return makeServlet(map);
        } else if (name.equals("initParam")) {
            makeInitParam(map);
            return null;
        } else if (name.equals("listener")) {
            return makeListener(map);
        } else if (name.equals("virtualHost")) {
            return addVirtualHost(map);
        } else if (name.equals("staticContent")) {
            setStaticContent(map);
            return null;
        } else if (name.equals("adapter")) {
            return makeAdapter(map);
        } else if (name.equals("waffleApp")) {
            return new WaffleAdapter(context, parentContainer, map).getNodeBuilder();
        }

        return null;
    }

    private void setStaticContent(final Map map) {

        if (map.containsKey("welcomePage")) {
            context.setStaticContext((String)map.remove("path"), (String)map.remove("welcomePage"));
        } else {
            context.setStaticContext((String)map.remove("path"));
        }

    }

    private Object makeAdapter(final Map map) {
        return new NodeBuilderAdapter((String)map.remove("nodeBuilder"),
                                      context,
                                      parentContainer,
                                      map).getNodeBuilder();
    }

    private Object makeListener(final Map map) {
        return context.addListener((Class)map.remove("class"));
    }

    private Object addVirtualHost(final Map map) {
        String virtualhost = (String) map.remove("name");
        context.addVirtualHost(virtualhost);
        return virtualhost;
    }


    private Object makeServlet(final Map map) {

        if (map.containsKey("class")) {
            ServletHolder servlet = context.addServletWithMapping((Class)map.remove("class"), (String)map
                .remove("path"));
            return new ServletHolderBuilder(servlet);
        } else {
            Servlet servlet = (Servlet)map.remove("instance");
            context.addServletWithMapping(servlet, (String)map.remove("path"));
            return servlet;
        }

    }

    private void makeInitParam(final Map map) {
        String name = (String) map.remove("name");
        String value = (String) map.remove("value");
        context.addInitParam(name, value);
    }

    private Object makeFilter(final Map map) {
        FilterHolder filter = context.addFilterWithMappings((Class)map.remove("class"), getPaths(map),
                                                           extractDispatchers(map));
        return new FilterHolderBuilder(filter);
    }

    private String[] getPaths(final Map map) {
        String[] paths = {};
        String mapping = (String) map.remove("path");
        if (mapping != null) {
            paths = new String[] { mapping };
        }
        String mappings = (String) map.remove("paths");
        if (mappings != null) {
            paths = mappings.replaceAll(" ", "").split(",");
        }
        return paths;
    }

    private EnumSet<DispatcherType> extractDispatchers(final Map map) {
        Object dispatchers = map.remove("dispatchers");
        if (dispatchers != null) {
            return (EnumSet<DispatcherType>)dispatchers;
        }
        // default value
        return PicoContext.DEFAULT_DISPATCH;
    }

}