/*******************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.
 * ---------------------------------------------------------------------------
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file. 
 ******************************************************************************/
package com.picocontainer.web.axis;

import org.apache.axis.MessageContext;
import org.apache.axis.providers.java.MsgProvider;
import org.apache.axis.utils.cache.ClassCache;
import com.picocontainer.web.PicoServletFilter;

/**
 * Axis provider for message-style services that uses the
 * PicoServletContainerFilter to instantiate service classes and resolve their
 * dependencies.
 * 
 * @author <a href="mailto:evan@bottch.com">Evan Bottcher</a>
 */
@SuppressWarnings("serial")
public class PicoMsgProvider extends MsgProvider {

	private PicoHook picoHook = new PicoHook();
	
    protected Object makeNewServiceObject(MessageContext msgContext, String clsName) throws Exception {

        ClassLoader cl = msgContext.getClassLoader();
        ClassCache cache = msgContext.getAxisEngine().getClassCache();
        Class<?> svcClass = cache.lookup(clsName, cl).getJavaClass();

        return picoHook.getRequestComponentForThread(svcClass);
    }
    
    
    private static class PicoHook extends PicoServletFilter {
    	
    }

}
