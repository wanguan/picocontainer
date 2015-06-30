/*******************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.
 * --------------------------------------------------------------------------
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 ******************************************************************************/
package com.picocontainer.web;

import java.io.Serializable;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.picocontainer.MutablePicoContainer;
import com.picocontainer.PicoCompositionException;
import com.picocontainer.PicoContainer;
import com.picocontainer.containers.EmptyPicoContainer;
import com.picocontainer.web.providers.AbstractScopedContainerBuilder;
import com.picocontainer.web.providers.PicoServletParameterProcessor;

/**
 * Servlet listener class that hooks into the underlying servlet container and
 * instantiates, assembles, starts, stores and disposes the appropriate pico
 * containers when applications/sessions start/stop.
 * <p>
 * To use, simply add as a listener to the web.xml the listener-class
 * 
 * <pre>
 * &lt;listener&gt;
 *  &lt;listener-class&gt;com.picocontainer.web.PicoServletContainerListener&lt;/listener-class&gt;
 * &lt;/listener&gt; 
 * </pre>
 * 
 * </p>
 * <p>
 * The listener also requires a the class name of the
 * {@link com.picocontainer.web.WebappComposer} as a context-param in web.xml:
 * 
 * <pre>
 *  &lt;context-param&gt;
 *   &lt;param-name&gt;webapp-composer-class&lt;/param-name&gt;
 *   &lt;param-value&gt;com.company.MyWebappComposer&lt;/param-value&gt;
 *  &lt;/context-param&gt;
 * </pre>
 * 
 * The composer will be used to compose the components for the different webapp
 * scopes after the context has been initialised.
 * </p>
 * 
 * @author Joe Walnes
 * @author Aslak Helles&oslash;y
 * @author Philipp Meier
 * @author Paul Hammant
 * @author Mauro Talevi
 * @author Konstantin Pribluda
 * @author Michael Rimov
 */
@SuppressWarnings("serial")
public class PicoServletContainerListener implements ServletContextListener, HttpSessionListener, Serializable {

    public static final String WEBAPP_COMPOSER_CLASS = "webapp-composer-class";

    /**
     * Use {@link com.picocontainer.web.ContextParameters#STATELESS_WEBAPP instead}
     */
    @Deprecated    
    public static final String STATELESS_WEBAPP = "stateless-webapp";

    public static final String PRINT_SESSION_SIZE = "print-session-size";

    private boolean isStateless;
    /**
     * Default constructor used in webapp containers
     */
    public PicoServletContainerListener() {
    }

    public void contextInitialized(final ServletContextEvent event) {
    	
    	//System.setSecurityManager(new ProfilingSecurityManager());
    	
        ServletContext context = event.getServletContext();
        //isStateless = Boolean.parseBoolean(context.getInitParameter(STATELESS_WEBAPP));        
        ScopedContainers scopedContainers = makeScopedContainers(context);

        compose(loadComposer(context), context, scopedContainers);
        start(scopedContainers.getApplicationContainer());
        context.setAttribute(ScopedContainers.class.getName(), scopedContainers);
    }

    /**
     * Overide this method if you need a more specialized container tree.
     * Here is the default block of code for this -
     *
     *   DefaultPicoContainer appCtnr = new DefaultPicoContainer(new Guarding().wrap(new Caching()), makeLifecycleStrategy(), makeParentContainer(), makeAppComponentMonitor());
     *   DefaultPicoContainer sessCtnr;
     *   PicoContainer parentOfRequestContainer;
     *   ThreadLocalLifecycleState sessionState;
     *   Storing sessStoring;
     *   if (stateless) {
     *       sessionState = null;
     *       sessStoring = null;
     *       sessCtnr = null;
     *       parentOfRequestContainer = appCtnr;
     *   } else {
     *       sessionState = new ThreadLocalLifecycleState();
     *       sessStoring = new Storing();
     *       sessCtnr = new DefaultPicoContainer(new Guarding().wrap(sessStoring), makeLifecycleStrategy(), appCtnr, makeSessionComponentMonitor());
     *       sessCtnr.setLifecycleState(sessionState);
     *       parentOfRequestContainer = sessCtnr;
     *   }
     *   Storing reqStoring = new Storing();
     *   DefaultPicoContainer reqCtnr = new DefaultPicoContainer(new Guarding().wrap(addRequestBehaviors(reqStoring)), makeLifecycleStrategy(), sessCtnr, makeRequestComponentMonitor());
     *   ThreadLocalLifecycleState requestState = new ThreadLocalLifecycleState();
     *   reqCtnr.setLifecycleState(requestState);
     *   return new ScopedContainers(appCtnr, sessCtnr, reqCtnr, sessStoring, reqStoring, sessionState, requestState);
     *
     * @param stateless
     * @return
     */
    protected ScopedContainers makeScopedContainers(ServletContext servletContext) {
/*        DefaultPicoContainer appCtnr = new DefaultPicoContainer(makeParentContainer(), makeLifecycleStrategy(), makeAppComponentMonitor(), new Guarding().wrap(new Caching()));
        DefaultPicoContainer sessCtnr;
        PicoContainer parentOfRequestContainer;
        ThreadLocalLifecycleState sessionState;
        Storing sessStoring;
        if (stateless) {
            sessionState = null;
            sessStoring = null;
            sessCtnr = null;
            parentOfRequestContainer = appCtnr;
        } else {
            sessionState = new ThreadLocalLifecycleState();
            sessStoring = new Storing();
            sessCtnr = new DefaultPicoContainer(appCtnr, makeLifecycleStrategy(), makeSessionComponentMonitor(), new Guarding().wrap(sessStoring));
            sessCtnr.setLifecycleState(sessionState);
            parentOfRequestContainer = sessCtnr;
        }
        Storing reqStoring = new Storing();
        DefaultPicoContainer reqCtnr = new DefaultPicoContainer(parentOfRequestContainer, makeLifecycleStrategy(), makeRequestComponentMonitor(), new Guarding().wrap(addRequestBehaviors(reqStoring)));
        ThreadLocalLifecycleState requestState = new ThreadLocalLifecycleState();
        reqCtnr.setLifecycleState(requestState);
        return new ScopedContainers(appCtnr, sessCtnr, reqCtnr, sessStoring, reqStoring, sessionState, requestState);
        */
		PicoServletParameterProcessor paramProcessor = new PicoServletParameterProcessor();
		AbstractScopedContainerBuilder containerBuilder = paramProcessor.processContextParameters(servletContext);
		ScopedContainers containers =  containerBuilder.makeScopedContainers(paramProcessor.isStateless());
    	
    	return containers;
    }

    protected PicoContainer makeParentContainer() {
        return new EmptyPicoContainer();
    }


    /**
     * Get the class to do compostition with - from a "webapp-composer-class" config param
     * from web.xml :
     *
     *   <context-param>
     *       <param-name>webapp-composer-class</param-name>
     *       <param-value>com.yourcompany.YourWebappComposer</param-value>
     *   </context-param>
     *
     * @param context
     * @return
     */
    protected WebappComposer loadComposer(ServletContext context) {
        String composerClassName = context.getInitParameter(WEBAPP_COMPOSER_CLASS);
        try {
            return (WebappComposer) Thread.currentThread().getContextClassLoader().loadClass(composerClassName)
                    .newInstance();
        } catch (Exception e) {
            throw new PicoCompositionException("Failed to load webapp composer class " + composerClassName
                    + ": ensure the context-param '" + WEBAPP_COMPOSER_CLASS + "' is configured in the web.xml.", e);
        }
    }

    protected void compose(WebappComposer composer, ServletContext context, ScopedContainers scopedContainers) {
        composer.composeApplication(scopedContainers.getApplicationContainer(), context);
        if (!isStateless) {
            composer.composeSession(scopedContainers.getSessionContainer());
        }
        composer.composeRequest(scopedContainers.getRequestContainer());
    }

    public void contextDestroyed(ServletContextEvent event) {
        ScopedContainers scopedContainers = getScopedContainers(event.getServletContext());
        if (scopedContainers != null && scopedContainers.getApplicationContainer() != null) {
	        stop(scopedContainers.getApplicationContainer());
	        dispose(scopedContainers.getApplicationContainer());
        }
        
        if (scopedContainers != null) {
            scopedContainers.dispose();        	
        }
        removeScopedContainersFromContext(event.getServletContext());
    }

    private void start(MutablePicoContainer container) {
        container.start();
    }

    private void dispose(MutablePicoContainer container) {
        container.dispose();
    }

    private void stop(MutablePicoContainer container) {
    	try {
	    	if (container.getLifecycleState().isStarted()) {
	    		container.stop();
	    	}
    	} catch (IllegalStateException ex) {
    		//swallow it, we still want dispose to try to go.
    	}
    }

    private ScopedContainers getScopedContainers(ServletContext context) {
        return (ScopedContainers) context.getAttribute(ScopedContainers.class.getName());
    }
    
    private void removeScopedContainersFromContext(ServletContext context) {
    	context.removeAttribute(ScopedContainers.class.getName());
    }

    public void sessionCreated(HttpSessionEvent event) {
        if (!isStateless) {
            HttpSession session = event.getSession();
            ScopedContainers scopedContainers = getScopedContainers(session.getServletContext());
            SessionStoreHolder ssh = new SessionStoreHolder(scopedContainers.getSessionStoring().resetCacheForThread(), scopedContainers.getSessionState().resetStateModelForThread());
            start(scopedContainers.getSessionContainer());
            session.setAttribute(SessionStoreHolder.class.getName(), ssh);
        }
    }

    public void sessionDestroyed(HttpSessionEvent event) {
        if (!isStateless) {
            HttpSession session = event.getSession();
            ScopedContainers scopedContainers = getScopedContainers(session.getServletContext());
            MutablePicoContainer sessionCtr = scopedContainers.getSessionContainer();
            SessionStoreHolder ssh = (SessionStoreHolder) session.getAttribute(SessionStoreHolder.class.getName());
            scopedContainers.getSessionStoring().putCacheForThread(ssh.getStoreWrapper());
            scopedContainers.getSessionState().putLifecycleStateModelForThread(ssh.getLifecycleState());
            try {
	            stop(sessionCtr);
	            dispose(sessionCtr);
            } finally {
            	//No matter what else happens, clear the memory.
	            scopedContainers.getSessionStoring().invalidateCacheForThread();
	            scopedContainers.getSessionState().invalidateStateModelForThread();
	            session.removeAttribute(SessionStoreHolder.class.getName());
            }
        }
    }

}
