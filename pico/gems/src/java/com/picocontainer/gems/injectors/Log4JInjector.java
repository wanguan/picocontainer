/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 *****************************************************************************/
package com.picocontainer.gems.injectors;

import java.lang.reflect.Type;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.picocontainer.PicoCompositionException;
import com.picocontainer.PicoContainer;
import com.picocontainer.injectors.FactoryInjector;
import com.picocontainer.injectors.InjectInto;

/**
 * This will Inject a Log4J Logger for the injectee's class name
 */
public class Log4JInjector extends FactoryInjector<Logger> {

    @Override
	public Logger getComponentInstance(final PicoContainer container, final Type into) throws PicoCompositionException {
        return LogManager.getLogger((((InjectInto) into).getIntoClass()));
    }

}

