/*
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.
 * --------------------------------------------------------------------------
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.picocontainer.logging.store.factories;

import static com.picocontainer.logging.loggers.ConsoleLogger.LEVEL_INFO;

import java.util.Map;

import com.picocontainer.logging.store.LoggerStore;
import com.picocontainer.logging.store.stores.ConsoleLoggerStore;

/**
 * This is a basic factory for ConsoleLoggerStore.
 * 
 * @author Peter Donald
 */
public class ConsoleLoggerStoreFactory extends AbstractLoggerStoreFactory {

    protected LoggerStore doCreateLoggerStore(final Map<String, Object> config) {
        return new ConsoleLoggerStore(LEVEL_INFO);
    }
}
