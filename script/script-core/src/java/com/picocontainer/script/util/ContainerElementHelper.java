/*******************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.
 * ---------------------------------------------------------------------------
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 ******************************************************************************/
package com.picocontainer.script.util;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.picocontainer.ComponentFactory;
import com.picocontainer.DefaultPicoContainer;
import com.picocontainer.PicoContainer;
import com.picocontainer.behaviors.Caching;
import com.picocontainer.classname.ClassLoadingPicoContainer;
import com.picocontainer.classname.DefaultClassLoadingPicoContainer;
import com.picocontainer.containers.EmptyPicoContainer;

/**
 * Helper for ScriptedPicoContainer
 *
 * @author Paul Hammant
 */
public class ContainerElementHelper {

    public static ClassLoadingPicoContainer makeScriptedPicoContainer(ComponentFactory componentFactory,
            PicoContainer parent, final ClassLoader classLoader) {
        if (parent == null) {
            parent = new EmptyPicoContainer();
        }
        if (componentFactory == null) {
            componentFactory = new Caching();
        }
        return new DefaultClassLoadingPicoContainer(classLoader, new DefaultPicoContainer(parent, componentFactory));

    }

    public static void debug(final List<?> arg0, final Map<?,?> arg1) {
        System.out.println("-->debug " + arg0.size() + " " + arg1.size());
        for (int i = 0; i < arg0.size(); i++) {
            Object o = arg0.get(i);
            System.out.println("--> arg0[" + i + "] " + o);

        }
        Set<?> keys = arg1.keySet();
        int i = 0;
        for (Object o : keys) {
            System.out.println("--> arg1[" + i++ + "] " + o + ", " + arg1.get(o));

        }
    }

}
