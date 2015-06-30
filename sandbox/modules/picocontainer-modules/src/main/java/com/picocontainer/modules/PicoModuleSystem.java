/*******************************************************************************
 * Copyright (C)  PicoContainer Committers. All rights reserved.
 * ---------------------------------------------------------------------------
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 ******************************************************************************/
package com.picocontainer.modules;

import com.picocontainer.script.util.MultiException;

import com.picocontainer.MutablePicoContainer;

public interface PicoModuleSystem {

	/**
	 * 
	 * @param parent
	 * @return <code>this</code> to allow for method chaining.
	 * @throws MultiException
	 */
	public PicoModuleSystem deploy(MutablePicoContainer parent) throws MultiException;

	/**
	 * 
	 * @return <code>this</code> to allow for method chaining.
	 * @throws MultiException
	 */
	public PicoModuleSystem deploy() throws MultiException, CircularDependencyException;
	
	/**
	 * Use getPico() after a call to deploy to retrieve the deployed PicoContainer.  The value
	 * will be the same used in {@link #deploy(MutablePicoContainer)} or an automatically
	 * created one if you used {@link #deploy()}
	 * @return constructed, deployed, and started PicoContainer.
	 */
	public MutablePicoContainer getPico() throws IllegalStateException;
	
	/**
	 * Stops (if started) and disposes the PicoContainer system.
	 * @throws MultiException
	 */
	public void undeploy() throws MultiException;

}
