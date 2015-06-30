/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 *****************************************************************************/

package com.picocontainer.jetty;

/**
 */
@SuppressWarnings("serial")
public class JettyServerLifecycleException extends RuntimeException {
    public JettyServerLifecycleException(final String string, final Throwable throwable) {
        super(string, throwable);
    }
}
