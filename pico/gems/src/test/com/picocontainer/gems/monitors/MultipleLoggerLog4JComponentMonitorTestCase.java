/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *****************************************************************************/
package com.picocontainer.gems.monitors;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import com.picocontainer.ComponentMonitor;

/**
 * @author Paul Hammant
 * @author Mauro Talevi
 */
public class MultipleLoggerLog4JComponentMonitorTestCase extends ComponentMonitorHelperTestCase {

    String logPrefixName = String.class.getName();

    @Override
	protected ComponentMonitor makeComponentMonitor() {
        return new Log4JComponentMonitor();
    }

    @Override
	protected Method getMethod() throws NoSuchMethodException {
        return String.class.getMethod("toString");
    }

    @Override
	protected Constructor<?> getConstructor() {
        return String.class.getConstructors()[0];
    }

    @Override
	protected String getLogPrefix() {
        return "[" + logPrefixName + "] ";
    }

    @Override
	public void testShouldTraceNoComponent() throws IOException {
        logPrefixName = ComponentMonitor.class.getName();
        super.testShouldTraceNoComponent();
    }
    

	@Override
	protected ComponentMonitor makeComponentMonitorWithDelegate(ComponentMonitor delegate) {
		return new Log4JComponentMonitor(delegate);
	}
}
