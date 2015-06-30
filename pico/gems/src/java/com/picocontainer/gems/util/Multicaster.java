/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by Aslak Hellesoy & Joerg Schaible                                       *
 *****************************************************************************/
package com.picocontainer.gems.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import com.picocontainer.PicoContainer;
import com.thoughtworks.proxy.ProxyFactory;
import com.thoughtworks.proxy.toys.multicast.Multicasting;

/**
 * Factory for creating a multicaster object that multicasts calls to all
 * components in a PicoContainer instance.
 *
 * @author Aslak Helles&oslash;y
 * @author Chris Stevenson
 * @author Paul Hammant
 */
public class Multicaster {
    /**
     * Create a {@link Multicasting} proxy for the components of a {@link PicoContainer}.
     *
     * @param pico the container
     * @param callInInstantiationOrder <code>true</code> if the components will be called in instantiation order
     * @param proxyFactory the ProxyFactory to use
     * @return the Multicasting proxy
     */
    public static Object object(final PicoContainer pico, final boolean callInInstantiationOrder, final ProxyFactory proxyFactory) {
        List<Object> copy = new ArrayList<Object>(pico.getComponents());

        if (!callInInstantiationOrder) {
            // reverse the list
            Collections.reverse(copy);
        }
        return Multicasting.proxy(copy.toArray()).build(proxyFactory);
    }
}