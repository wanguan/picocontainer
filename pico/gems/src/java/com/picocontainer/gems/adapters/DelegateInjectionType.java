/**
 *
 */
package com.picocontainer.gems.adapters;

import java.util.Properties;

import com.picocontainer.gems.util.DelegateMethod;

import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentMonitor;
import com.picocontainer.LifecycleStrategy;
import com.picocontainer.PicoCompositionException;
import com.picocontainer.injectors.AbstractInjectionType;
import com.picocontainer.parameters.ConstructorParameters;
import com.picocontainer.parameters.FieldParameters;
import com.picocontainer.parameters.MethodParameters;

/**
 * Mirrored InjectionType for handling delegate methods.
 * @author Michael Rimov
 */
@SuppressWarnings("serial")
public class DelegateInjectionType extends AbstractInjectionType {


	/**
	 * DelegateMethod instance key.
	 */
    private static final String DELEGATE = "delegateInstance";


    /**
     * Delegate target instance key.
     */
    private static final String INSTANCE = "targetInstance";

	/**
	 * Default constructor.
	 */
	public DelegateInjectionType() {
		super();
	}


    /**
	 * {@inheritDoc}
	 *
	 */
	public <T> ComponentAdapter<T> createComponentAdapter(
			final ComponentMonitor monitor,
			final LifecycleStrategy lifecycle,
			final Properties componentProps, final Object key,
			final Class<T> impl, final ConstructorParameters constructorParams, final FieldParameters[] fieldParams, final MethodParameters[] methodParams)
			throws PicoCompositionException {

		DelegateMethod<?, T> method =  cast(componentProps.remove(DELEGATE));

		//TODO: what to do since if there is no method, the delegate adapter won't work.
		if (method == null) {
			throw new IllegalArgumentException("Component properties must have a "
					+"com.picocontainer.gems.util.DelegateMethod object stored as delegateInstance");
		}

		Object instance = componentProps.remove(INSTANCE);
		if (instance == null) {
			throw new IllegalArgumentException("Property 'instance' must exist.");
		}



		return new DelegateMethodAdapter<T>(key, monitor, instance, method);
	}


	/**
	 * Takes care of generic warnings.
	 * @param <T>
	 * @param source
	 * @return an appropriately cast object.
	 */
	@SuppressWarnings("unchecked")
	private <T> T cast(final Object source) {
		return (T) source;
	}

	/**
	 * Use this static factory method as a way of creating all the necessary properties that are required by the adapter.
	 * <p>Example:</p>
	 * <pre>
	 * 		DelegateAdapterFactory factory = new DelegateAdapterFactory();
	 * 		HttpServletRequest request = .....;
	 *
	 *      //When object is instantiated will lazily call:   request.getSession(false);
	 * 		Properties props = createDelegateProperties(request, &quot;getSession&quot;, false);
	 *
	 * 		DelegateMethodAdapter adapter = createComponentAdapter(new ConsoleComponentMonitor(), new DefaultLifecycleStrategy(),
	 * 				 props, HttpSession.class, HttpSession.class);
	 * </pre>
	 * @param targetObject the object to be operated on.
	 * @param methodName the name of the method to invoke.
	 * @param parameters the parameters to supply upon invocation. (Also used to find matching argument).
	 * @return the appropriate properties that can be used with createComponentAdapter().
	 */
	public static Properties createDelegateProprties(final Object targetObject, final String methodName, final Object... parameters) {
		Properties props = new Properties();
		props.put(INSTANCE, targetObject);
		props.put(DELEGATE, createDelegate(targetObject.getClass(), methodName, parameters));

		return props;
	}

	/**
	 * Generic-friendly instantiation.  If you have control of your own code, you can also just use the DelegateMethod constructors.
	 * @param <INSTANCE>
	 * @param <RETURN_TYPE>
	 * @param targetType the type of object being instantiated.
	 * @param methodName the method name to invoke when called.
	 * @param parameters the method paramters to use.
	 * @return DelegateMethod instance.
	 */
	public static <INSTANCE,RETURN_TYPE> DelegateMethod<INSTANCE, RETURN_TYPE> createDelegate(final Class<INSTANCE> targetType, final String methodName, final Object... parameters) {
		return new DelegateMethod<INSTANCE,RETURN_TYPE>(targetType, methodName, parameters);
	}
}
