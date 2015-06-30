/*****************************************************************************
 * Copyright (C) PicoContainer Organization. All rights reserved.            *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by Mike Rimov                                               *
 *****************************************************************************/
package com.picocontainer.gems.containers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentFactory;
import com.picocontainer.ComponentMonitor;
import com.picocontainer.DefaultPicoContainer;
import com.picocontainer.LifecycleStrategy;
import com.picocontainer.MutablePicoContainer;
import com.picocontainer.Parameter;
import com.picocontainer.PicoCompositionException;
import com.picocontainer.PicoContainer;
import com.picocontainer.adapters.InstanceAdapter;
import com.picocontainer.behaviors.Storing;

/**
 * Normal PicoContainers are meant to be created, started, stopped, disposed and
 * garbage collected. The goal of this container is to reduce the number of
 * registration calls (and therefore objects created) in areas where performance
 * is key (for example, this might be used in NanoContainer request containers).
 * <p>
 * It accomplishes its goal in two ways: <br/> (1) Once a container is disposed
 * of, start() may be called again, allowing for recycling of the container. (This is default
 * behavior with a picocontainer)
 * </p>
 * <p>
 * (2) All instance and adapter registrations will be unregistered when stop is called. (For example,
 * HttpServletRequest would be removed), and all component adapter instance values
 * are flushed.
 * </p>
 * <h4>Container Storage</h4>
 * <p>It is still up to the builder of this container to decide where to store its reference.  Since
 * it is reusable, it needs to be stored someplace that doesn't easily expire.  Probably the most
 * common storage location would be ThreadLocal storage.  HttpSession might be another valid
 * storage location.</p>
 * @author Michael Rimov
 * @todo Convert to composition.
 */
@SuppressWarnings("serial")
public class ReusablePicoContainer extends DefaultPicoContainer {

    /**
     * A list of all addComponent(key, instance) calls that were made.  These will be removed from the container
     * each time the container is stopped.
     */
	private final List<ComponentAdapter<?>> instanceRegistrations = new ArrayList<ComponentAdapter<?>>();

	/**
	 * A list of all addFlushableAdapter() calls that were made.  These instances will be removed from
	 * the container each time it is stopped.
	 */
    private final List<ComponentAdapter<?>> adapterRegistrations = new ArrayList<ComponentAdapter<?>>();

	private final Map<ComponentAdapter<?>, Storing.Stored<?>> storedReferences = new HashMap<ComponentAdapter<?>, Storing.Stored<?>>();

	/**
	 * Default constructor.
	 */
	public ReusablePicoContainer() {
		super();
	}

	public ReusablePicoContainer(final ComponentFactory componentFactory,
			final LifecycleStrategy lifecycle, final PicoContainer parent,
			final ComponentMonitor monitor) {
		super(parent, lifecycle, monitor, componentFactory);
	}

	public ReusablePicoContainer(final ComponentFactory componentFactory,
			final LifecycleStrategy lifecycle, final PicoContainer parent) {
		super(parent, lifecycle, componentFactory);
	}

	public ReusablePicoContainer(final ComponentFactory componentFactory,
			final PicoContainer parent) {
		super(parent, componentFactory);
	}

	public ReusablePicoContainer(final ComponentFactory componentFactory) {
		super(componentFactory);
	}

	public ReusablePicoContainer(final ComponentMonitor monitor,
			final LifecycleStrategy lifecycle, final PicoContainer parent) {
		super(parent, lifecycle, monitor);
	}

	public ReusablePicoContainer(final ComponentMonitor monitor, final PicoContainer parent) {
		super(parent, monitor);
	}

	public ReusablePicoContainer(final ComponentMonitor monitor) {
		super(monitor);
	}

	public ReusablePicoContainer(final LifecycleStrategy lifecycle,
			final PicoContainer parent) {
		super(parent, lifecycle);
	}

	public ReusablePicoContainer(final PicoContainer parent) {
		super(parent);
	}

	@Override
	public MutablePicoContainer addComponent(final Object key,
			final Object implOrInstance,
			final Parameter... parameters) throws PicoCompositionException {

		if (key == null) {
			throw new NullPointerException("key");
		}

		if (implOrInstance == null) {
			throw new NullPointerException("implOrInstance");
		}

		super.addComponent(key,
				implOrInstance,
				parameters);

		if (! (implOrInstance instanceof Class)) {
			instanceRegistrations.add(super.getComponentAdapter(key));
		} else {
			addStoredReference(key);
		}

		return this;
	}

	@Override
	public  MutablePicoContainer addComponent(final Object implOrInstance)
			throws PicoCompositionException {
		if ((implOrInstance instanceof Class)) {
			super.addComponent(implOrInstance);
			addStoredReference(implOrInstance);
			return this;
		} else {
			return this.addComponent(implOrInstance.getClass(), implOrInstance);
		}
	}

	/**
	 * Precalculates all references to Stored behaviors.
	 * @param key the object key.
	 */
	private void addStoredReference(final Object key) {
		ComponentAdapter<?> ca = this.getComponentAdapter(key);
		Storing.Stored<?> stored =  ca.findAdapterOfType(Storing.Stored.class);
	    if (stored != null) {
	    	storedReferences.put(ca, stored);
	    }
    }

	@Override
	public synchronized void stop() {
		super.stop();
		flushInstances();
	}

    /**
     * Automatically called by {@link #stop() stop()}, this method clears all instantiated
     * instances and readies the picocontainer for reuse.
     */
    public void flushInstances() {
        //Remove all instance registrations.
		for (ComponentAdapter<?> eachAdapter : this.instanceRegistrations) {
			this.removeComponent(eachAdapter.getComponentKey());
		}
		instanceRegistrations.clear();

		for (ComponentAdapter<?> eachAdapter : this.adapterRegistrations) {
		    this.removeComponent(eachAdapter.getComponentKey());
		}
		adapterRegistrations.clear();

		//Flush all remaining objects.
		for (Storing.Stored<?> eachStoredBehavior : this.storedReferences.values()) {
			eachStoredBehavior.flush();
		}
    }

	@Override
    public MutablePicoContainer addAdapter(final ComponentAdapter<?> componentAdapter, final Properties properties) {
		super.addAdapter(componentAdapter, properties);
		if (componentAdapter.findAdapterOfType(InstanceAdapter.class) != null) {
			this.instanceRegistrations.add(componentAdapter);
		} else {
			this.addStoredReference(componentAdapter.getComponentKey());
		}

		return this;
    }

	/**
	 * Use this instead of addAdapter in cases where you have custom adapters that the container
	 * cannot figure out whether it should be flushed after each request or not. Anything
	 * added through this method will be flushed after each stop() of the container.
	 * @param componentAdapter
	 * @return <em>this</em> to allow for method chaining.
	 */
	public MutablePicoContainer addFlushableAdapter(final ComponentAdapter<?> componentAdapter) {
	    adapterRegistrations.add(componentAdapter);
	    addAdapter(componentAdapter);
	    return this;
	}

	@Override
    public MutablePicoContainer addAdapter(final ComponentAdapter<?> componentAdapter) {
		super.addAdapter(componentAdapter);
		if (componentAdapter.findAdapterOfType(InstanceAdapter.class) != null) {
			this.instanceRegistrations.add(componentAdapter);
		} else {
			this.addStoredReference(componentAdapter.getComponentKey());
		}

		return this;
    }

	private void removeLocalReferences(final ComponentAdapter<?> ca) {
		this.storedReferences.remove(ca);
	}

	@Override
    public <T> ComponentAdapter<T> removeComponent(final Object key) {
		ComponentAdapter< T > result= null;
        try {
            result = super.removeComponent(key);
        } catch (PicoCompositionException e) {
            //Help with debugging any lifecycle errors.
            throw new PicoCompositionException("There was an error removing component by key: '" + key + "'",e);
        }

        if (result != null) {
			removeLocalReferences(result);
		}

		return result;
    }

	@Override
    public <T> ComponentAdapter<T> removeComponentByInstance(final T componentInstance) {
		ComponentAdapter<T> result =  super.removeComponentByInstance(componentInstance);
		if (result != null) {
			removeLocalReferences(result);
		}

		return result;
    }

    @Override
	public MutablePicoContainer makeChildContainer() {
        ReusablePicoContainer pc = new ReusablePicoContainer(componentFactory, lifecycle, this);
        addChildContainer(pc);
        return pc;
    }

}
