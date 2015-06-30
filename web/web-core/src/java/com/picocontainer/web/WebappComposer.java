/*******************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.
 * --------------------------------------------------------------------------
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 ******************************************************************************/
package com.picocontainer.web;

import com.picocontainer.MutablePicoContainer;

import javax.servlet.ServletContext;

/**
 * Allows to compose containers for different webapp scopes. The composer is
 * used by the
 * {@link com.picocontainer.web.PicoServletContainerListener PicoServletContainerListener}
 * after the webapp context is initialised. Users can either implement their
 * composer and register components for each scope directly or load them from a
 * picocontainer script, using the
 * {@link com.picocontainer.web.script.ScriptedWebappComposer ScriptedWebappComposer}.
 * 
 * @author Paul Hammant
 * @author Mauro Talevi
 */
public interface WebappComposer {

    void composeApplication(MutablePicoContainer container, ServletContext servletContext);

    void composeSession(MutablePicoContainer container);

    void composeRequest(MutablePicoContainer container);

}
