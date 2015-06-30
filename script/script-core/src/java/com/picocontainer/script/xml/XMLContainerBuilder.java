/*******************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.
 * ---------------------------------------------------------------------------
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 ******************************************************************************/
package com.picocontainer.script.xml;

import static com.picocontainer.script.xml.AttributeUtils.EMPTY;
import static com.picocontainer.script.xml.AttributeUtils.isSet;
import static com.picocontainer.script.xml.AttributeUtils.notSet;
import static com.picocontainer.script.xml.XMLConstants.CLASS;
import static com.picocontainer.script.xml.XMLConstants.CLASSLOADER;
import static com.picocontainer.script.xml.XMLConstants.CLASSNAME;
import static com.picocontainer.script.xml.XMLConstants.CLASSPATH;
import static com.picocontainer.script.xml.XMLConstants.CLASS_NAME_KEY;
import static com.picocontainer.script.xml.XMLConstants.COMPONENT;
import static com.picocontainer.script.xml.XMLConstants.COMPONENT_ADAPTER;
import static com.picocontainer.script.xml.XMLConstants.COMPONENT_ADAPTER_FACTORY;
import static com.picocontainer.script.xml.XMLConstants.COMPONENT_FROM_JNDI;
import static com.picocontainer.script.xml.XMLConstants.COMPONENT_IMPLEMENTATION;
import static com.picocontainer.script.xml.XMLConstants.COMPONENT_INSTANCE;
import static com.picocontainer.script.xml.XMLConstants.COMPONENT_INSTANCE_FACTORY;
import static com.picocontainer.script.xml.XMLConstants.COMPONENT_KEY_TYPE;
import static com.picocontainer.script.xml.XMLConstants.COMPONENT_VALUE_TYPE;
import static com.picocontainer.script.xml.XMLConstants.CONTAINER;
import static com.picocontainer.script.xml.XMLConstants.CONTEXT;
import static com.picocontainer.script.xml.XMLConstants.EMPTY_COLLECTION;
import static com.picocontainer.script.xml.XMLConstants.FACTORY;
import static com.picocontainer.script.xml.XMLConstants.FILE;
import static com.picocontainer.script.xml.XMLConstants.JNDI_NAME;
import static com.picocontainer.script.xml.XMLConstants.KEY;
import static com.picocontainer.script.xml.XMLConstants.PARAMETER;
import static com.picocontainer.script.xml.XMLConstants.PARAMETER_ZERO;
import static com.picocontainer.script.xml.XMLConstants.URL;
import static com.picocontainer.script.xml.XMLConstants.VALUE;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.NamingException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.picocontainer.gems.jndi.JNDIObjectReference;
import com.picocontainer.gems.jndi.JNDIProvided;
import com.picocontainer.script.ScriptedBuilder;
import com.picocontainer.script.ScriptedContainerBuilder;
import com.picocontainer.script.ScriptedPicoContainerMarkupException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.googlecode.jtype.Generic;
import com.picocontainer.Behavior;
import com.picocontainer.Characteristics;
import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentFactory;
import com.picocontainer.ComponentMonitor;
import com.picocontainer.DefaultPicoContainer;
import com.picocontainer.Injector;
import com.picocontainer.LifecycleStrategy;
import com.picocontainer.MutablePicoContainer;
import com.picocontainer.Parameter;
import com.picocontainer.PicoClassNotFoundException;
import com.picocontainer.PicoCompositionException;
import com.picocontainer.PicoContainer;
import com.picocontainer.behaviors.Caching;
import com.picocontainer.classname.ClassLoadingPicoContainer;
import com.picocontainer.classname.ClassName;
import com.picocontainer.classname.ClassPathElement;
import com.picocontainer.classname.DefaultClassLoadingPicoContainer;
import com.picocontainer.injectors.AbstractInjectionType;
import com.picocontainer.injectors.ConstructorInjection;
import com.picocontainer.injectors.MultiArgMemberInjector;
import com.picocontainer.lifecycle.NullLifecycleStrategy;
import com.picocontainer.monitors.NullComponentMonitor;
import com.picocontainer.parameters.ComponentParameter;
import com.picocontainer.parameters.ConstantParameter;
import com.picocontainer.parameters.ConstructorParameters;
import com.picocontainer.parameters.FieldParameters;
import com.picocontainer.parameters.MethodParameters;

/**
 * This class builds up a hierarchy of PicoContainers from an XML configuration file.
 *
 * @author Paul Hammant
 * @author Aslak Helles&oslash;y
 * @author Jeppe Cramon
 * @author Mauro Talevi
 */
public class XMLContainerBuilder extends ScriptedContainerBuilder {

    private final static String DEFAULT_COMPONENT_INSTANCE_FACTORY = BeanComponentInstanceFactory.class.getName();

    private Element rootElement;

    /**
     * The XMLComponentInstanceFactory globally defined for the container.
     * It may be overridden at node level.
     */
    private XMLComponentInstanceFactory componentInstanceFactory;

    public XMLContainerBuilder(final Reader script, final ClassLoader classLoader) {
        super(script, classLoader);
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            parse(documentBuilder, new InputSource(script));
        } catch (ParserConfigurationException e) {
            throw new ScriptedPicoContainerMarkupException(e);
        }
    }

    public XMLContainerBuilder(final URL script, final ClassLoader classLoader) {
        super(script, classLoader);
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            documentBuilder.setEntityResolver(new EntityResolver() {
                public InputSource resolveEntity(final String publicId, final String systemId) throws IOException {
                    URL url = new URL(script, systemId);
                    return new InputSource(url.openStream());
                }
            });
            parse(documentBuilder, new InputSource(script.toString()));
        } catch (ParserConfigurationException e) {
            throw new ScriptedPicoContainerMarkupException(e);
        }
    }

    private void parse(final DocumentBuilder documentBuilder, final InputSource inputSource) {
        try {
            rootElement = documentBuilder.parse(inputSource).getDocumentElement();
        } catch (SAXException e) {
            throw new ScriptedPicoContainerMarkupException(e);
        } catch (IOException e) {
            throw new ScriptedPicoContainerMarkupException(e);
        }
    }

    @Override
	protected PicoContainer createContainerFromScript(final PicoContainer parentContainer, final Object assemblyScope) {
        try {
            // create ComponentInstanceFactory for the container
            componentInstanceFactory = createComponentInstanceFactory(rootElement.getAttribute(COMPONENT_INSTANCE_FACTORY));
            MutablePicoContainer childContainer = createMutablePicoContainer(
                     parentContainer, new ContainerOptions(rootElement));
            populateContainer(childContainer);
            return childContainer;
        } catch (PicoClassNotFoundException e) {
            throw new ScriptedPicoContainerMarkupException("Class not found:" + e.getMessage(), e);
        }
    }

    private MutablePicoContainer createMutablePicoContainer(final PicoContainer parentContainer, final ContainerOptions containerOptions) throws PicoCompositionException {
    	boolean caching = containerOptions.isCaching();
    	boolean inherit = containerOptions.isInheritParentBehaviors();
    	String monitorName = containerOptions.getMonitorName();
    	String componentFactoryName = containerOptions.getComponentFactoryName();

    	if (inherit) {
    		if (!(parentContainer instanceof MutablePicoContainer)) {
    			throw new PicoCompositionException("For behavior inheritance to be used, the parent picocontainer must be of type MutablePicoContainer");
    		}

    		MutablePicoContainer parentPico = (MutablePicoContainer)parentContainer;
    		return parentPico.makeChildContainer();
    	}

    	ScriptedBuilder builder = new ScriptedBuilder(parentContainer);
        if (caching) {
			builder.withCaching();
		}
        return builder
            .withClassLoader(getClassLoader())
            .withLifecycle()
            .withComponentFactory(componentFactoryName)
            .withMonitor(monitorName)
            .build();

    }

    public void populateContainer(final MutablePicoContainer container) {
        try {
            String parentClass = rootElement.getAttribute("parentclassloader");
            ClassLoader classLoader = getClassLoader();
            if (parentClass != null && !EMPTY.equals(parentClass)) {
                classLoader = classLoader.loadClass(parentClass).getClassLoader();
            }
            ClassLoadingPicoContainer scriptedContainer = new DefaultClassLoadingPicoContainer(classLoader, container);
            ClassLoadingPicoContainer classLoadingPicoContainer = new DefaultClassLoadingPicoContainer(getClassLoader());
            addComponentsAndChildContainers(scriptedContainer, rootElement, classLoadingPicoContainer);
        } catch (ClassNotFoundException e) {
            throw new ScriptedPicoContainerMarkupException("Class not found: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new ScriptedPicoContainerMarkupException(e);
        } catch (SAXException e) {
            throw new ScriptedPicoContainerMarkupException(e);
        } catch (NamingException e) {
            throw new ScriptedPicoContainerMarkupException(e);
        }
    }

    private void addComponentsAndChildContainers(final ClassLoadingPicoContainer parentContainer, final Element containerElement, final ClassLoadingPicoContainer knownComponentAdapterFactories) throws ClassNotFoundException, IOException, SAXException, NamingException {

        ClassLoadingPicoContainer metaContainer = new DefaultClassLoadingPicoContainer(getClassLoader(),
                new CompFactoryWrappingInjectionType(), knownComponentAdapterFactories);
        NodeList children = containerElement.getChildNodes();
        // register classpath first, regardless of order in the document.
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                Element childElement = (Element) children.item(i);
                String name = childElement.getNodeName();
                if (CLASSPATH.equals(name)) {
                    addClasspath(parentContainer, childElement);
                }
            }
        }
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                Element childElement = (Element) children.item(i);
                String name = childElement.getNodeName();
                if (CONTAINER.equals(name)) {
                    MutablePicoContainer childContainer = parentContainer.makeChildContainer();
                    ClassLoadingPicoContainer childPicoContainer = new DefaultClassLoadingPicoContainer(parentContainer.getComponentClassLoader(), childContainer);
                    addComponentsAndChildContainers(childPicoContainer, childElement, metaContainer);
                } else if (COMPONENT_IMPLEMENTATION.equals(name)
                        || COMPONENT.equals(name)) {
                    addComponent(parentContainer, childElement, new Properties[0]);
                } else if (COMPONENT_INSTANCE.equals(name)) {
                    registerComponentInstance(parentContainer, childElement);
                } else if (COMPONENT_FROM_JNDI.equals(name)) {
                    registerComponentFromJndi(parentContainer, childElement);
                } else if (COMPONENT_ADAPTER.equals(name)) {
                    addComponentAdapter(parentContainer, childElement, metaContainer);
                } else if (COMPONENT_ADAPTER_FACTORY.equals(name)) {
                    addComponentFactory(childElement, metaContainer);
                } else if (CLASSLOADER.equals(name)) {
                    addClassLoader(parentContainer, childElement, metaContainer);
                } else if (!CLASSPATH.equals(name)) {
                    throw new ScriptedPicoContainerMarkupException("Unsupported element:" + name);
                }
            }
        }
    }


    private void addComponentFactory(final Element element, final ClassLoadingPicoContainer metaContainer) throws MalformedURLException, ClassNotFoundException {
        if (notSet(element.getAttribute(KEY))) {
            throw new ScriptedPicoContainerMarkupException("'" + KEY + "' attribute not specified for " + element.getNodeName());
        }
        Element node = (Element)element.cloneNode(false);
        NodeList children = element.getChildNodes();
        String key = null;
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                Element childElement = (Element) children.item(i);
                String name = childElement.getNodeName();
                if (COMPONENT_ADAPTER_FACTORY.equals(name)) {
                    if (!"".equals(childElement.getAttribute(KEY))) {
                        throw new ScriptedPicoContainerMarkupException("'" + KEY + "' attribute must not be specified for nested " + element.getNodeName());
                    }
                    childElement = (Element)childElement.cloneNode(true);
                    key = "ContrivedKey:" + String.valueOf(System.identityHashCode(childElement));
                    childElement.setAttribute(KEY, key);
                    addComponentFactory(childElement, metaContainer);
                    // replace nested CAF with a ComponentParameter using an internally generated key
                    //Element parameter = node.getOwnerDocument().createElement(PARAMETER);
                    //parameter.setAttribute(KEY, key);
                    //node.appendChild(parameter);
                } else if (PARAMETER.equals(name)) {
                    node.appendChild(childElement.cloneNode(true));
                }
            }
        }
        // handle CAF now as standard component in the metaContainer
        if (key != null) {
            addComponent(metaContainer, node, new ForCaf(key));
        } else {
            addComponent(metaContainer, node, new ForCaf[0]);
        }
    }

    @SuppressWarnings("serial")
    public class ForCaf extends Properties {

        public ForCaf(final String key) {
            super.put("ForCAF", key);
        }
    }

    private void addClassLoader(final ClassLoadingPicoContainer parentContainer, final Element childElement, final ClassLoadingPicoContainer metaContainer) throws IOException, SAXException, ClassNotFoundException, NamingException {
        String parentClass = childElement.getAttribute("parentclassloader");
        ClassLoader parentClassLoader = parentContainer.getComponentClassLoader();
        if (parentClass != null && !EMPTY.equals(parentClass)) {
            parentClassLoader = parentClassLoader.loadClass(parentClass).getClassLoader();
        }
        ClassLoadingPicoContainer scripted = new DefaultClassLoadingPicoContainer(parentClassLoader, parentContainer);
        addComponentsAndChildContainers(scripted, childElement, metaContainer);
    }

    private void addClasspath(final ClassLoadingPicoContainer container, final Element classpathElement) throws IOException, ClassNotFoundException {
        NodeList children = classpathElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                Element childElement = (Element) children.item(i);

                String fileName = childElement.getAttribute(FILE);
                String urlSpec = childElement.getAttribute(URL);
                URL url;
                if (urlSpec != null && !EMPTY.equals(urlSpec)) {
                    url = new URL(urlSpec);
                } else {
                    File file = new File(fileName);
                    if (!file.exists()) {
                        throw new IOException(file.getAbsolutePath() + " doesn't exist");
                    }

                    url = file.toURI().toURL();
                }
                ClassPathElement cpe = container.addClassLoaderURL(url);
                addPermissions(cpe, childElement);
            }
        }
    }

    private void addPermissions(final ClassPathElement classPathElement, final Element classPathXmlElement) throws ClassNotFoundException {
        NodeList children = classPathXmlElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                Element childElement = (Element) children.item(i);

                String permissionClassName = childElement.getAttribute(CLASSNAME);
                String action = childElement.getAttribute(CONTEXT);
                String value = childElement.getAttribute(VALUE);
                MutablePicoContainer mpc = new DefaultPicoContainer();
                mpc.addComponent(Permission.class, Class.forName(permissionClassName), new ConstantParameter(action), new ConstantParameter(value));

                Permission permission = mpc.getComponent(Permission.class);
                classPathElement.grantPermission(permission);
            }
        }

    }

    private void addComponent(final ClassLoadingPicoContainer container, final Element element, final Properties... props) throws ClassNotFoundException, MalformedURLException {
        String className = element.getAttribute(CLASS);
        if (notSet(className)) {
            throw new ScriptedPicoContainerMarkupException("'" + CLASS + "' attribute not specified for " + element.getNodeName());
        }

        Parameter[] parameters = createChildParameters(container, element);
        Class<?> clazz = container.getComponentClassLoader().loadClass(className);
        Object key = element.getAttribute(KEY);
        if (notSet(key)) {
            String classKey = element.getAttribute(CLASS_NAME_KEY);
            if (isSet(classKey)) {
                key = getClassLoader().loadClass(classKey);
            } else {
                key = clazz;
            }
        }
        if (parameters == null) {
            container.addComponent(key, clazz);
        } else {
            container.as(props).addComponent(key, clazz, parameters);
        }
    }



    private Parameter[] createChildParameters(final ClassLoadingPicoContainer container, final Element element) throws ClassNotFoundException, MalformedURLException {
        List<Parameter> parametersList = new ArrayList<Parameter>();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                Element childElement = (Element) children.item(i);
                if (PARAMETER.equals(childElement.getNodeName())) {
                    parametersList.add(createParameter(container, childElement));
                }

                if (PARAMETER_ZERO.equals(childElement.getNodeName())) {
                	//Check:  We can't check everything here since we aren't schema validating
                	//But it will at least catch some goofs.
                	if (!parametersList.isEmpty()) {
                		throw new PicoCompositionException("Cannot mix other parameters with '" + PARAMETER_ZERO +"' nodes.");
                	}

                	return Parameter.ZERO;
                }
            }
        }

        Parameter[] parameters = null;
        if (!parametersList.isEmpty()) {
            parameters = parametersList.toArray(new Parameter[parametersList.size()]);
        }
        return parameters;
    }

    /**
     * Build the com.picocontainer.Parameter from the <code>parameter</code> element. This could
     * create either a ComponentParameter or ConstantParameter instance,
     * depending on the values of the element's attributes. This is somewhat
     * complex because there are five constructors for ComponentParameter and one for
     * ConstantParameter. These are:
     *
     * <a href="http://www.picocontainer.org/picocontainer/latest/picocontainer/apidocs/org/picocontainer/defaults/ComponentParameter.html">ComponentParameter Javadocs</a>:
     *
     * <code>ComponentParameter() - Expect any scalar paramter of the appropriate type or an Array.
     *       ComponentParameter(boolean emptyCollection) - Expect any scalar paramter of the appropriate type or an Array.
     *       ComponentParameter(Class componentValueType, boolean emptyCollection) - Expect any scalar paramter of the appropriate type or the collecting type Array,Collectionor Map.
     *       ComponentParameter(Class keyType, Class componentValueType, boolean emptyCollection) - Expect any scalar paramter of the appropriate type or the collecting type Array,Collectionor Map.
     *       ComponentParameter(Object key) - Expect a parameter matching a component of a specific key.</code>
     *
     * and
     *
     * <a href="http://www.picocontainer.org/picocontainer/latest/picocontainer/apidocs/org/picocontainer/defaults/ConstantParameter.html">ConstantParameter Javadocs</a>:
     *
     * <code>ConstantParameter(Object value)</code>
     *
     * The rules for this are, in order:
     *
     * 1) If the <code>key</code> attribute is not null/empty, the fifth constructor will be used.
     * 2) If the <code>keyType</code> attribute is not null/empty, the fourth constructor will be used.
     *    In this case, both the <code>componentValueType</code> and <code>emptyCollection</code> attributes must be non-null/empty or an exception will be thrown.
     * 3) If the <code>componentValueType</code> attribute is not null/empty, the third constructor will be used.
     *    In this case, the <code>emptyCollection</code> attribute must be non-null/empty.
     * 4) If the <code>emptyCollection</code> attribute is not null/empty, the second constructor will be used.
     * 5) If there is no child element of the parameter, the first constructor will be used.
     * 6) Otherwise, the return value will be a ConstantParameter with the return from the createInstance value.
     * @param element
     * @param pico
     * @return
     * @throws ClassNotFoundException
     * @throws MalformedURLException
     */
    private Parameter createParameter(final PicoContainer pico, final Element element) throws ClassNotFoundException, MalformedURLException {
        final Parameter parameter;
        String key = element.getAttribute(KEY);
        String emptyCollectionString = element.getAttribute(EMPTY_COLLECTION);
        String componentValueTypeString = element.getAttribute(COMPONENT_VALUE_TYPE);
        String keyTypeString = element.getAttribute(COMPONENT_KEY_TYPE);

        // key not null/empty takes precidence
        if (key != null && !EMPTY.equals(key)) {
            parameter = new ComponentParameter(key);
        } else if (keyTypeString != null && !EMPTY.equals(keyTypeString)) {
            if (emptyCollectionString == null || componentValueTypeString == null ||
                    EMPTY.equals(emptyCollectionString) || EMPTY.equals(componentValueTypeString)) {

                throw new ScriptedPicoContainerMarkupException("The keyType attribute was specified (" +
                        keyTypeString + ") but one or both of the emptyCollection (" +
                        emptyCollectionString + ") or componentValueType (" + componentValueTypeString +
                        ") was empty or null.");
            }

            Class<?> keyType = getClassLoader().loadClass(keyTypeString);
            Class<?> componentValueType = getClassLoader().loadClass(componentValueTypeString);

            boolean emptyCollection = Boolean.valueOf(emptyCollectionString);

            parameter = new ComponentParameter(keyType, Generic.get(componentValueType), emptyCollection);
        } else if (componentValueTypeString != null && !EMPTY.equals(componentValueTypeString)) {
            if (emptyCollectionString == null || EMPTY.equals(emptyCollectionString)) {

                throw new ScriptedPicoContainerMarkupException("The componentValueType attribute was specified (" +
                        componentValueTypeString + ") but the emptyCollection (" +
                        emptyCollectionString + ") was empty or null.");
            }

            Class<?> componentValueType = getClassLoader().loadClass(componentValueTypeString);

            boolean emptyCollection = Boolean.valueOf(emptyCollectionString);

            parameter = new ComponentParameter(Generic.get(componentValueType), emptyCollection);
        } else if (emptyCollectionString != null && !EMPTY.equals(emptyCollectionString)) {
            boolean emptyCollection = Boolean.valueOf(emptyCollectionString);

            parameter = new ComponentParameter(emptyCollection);
        }
        else if (getFirstChildElement(element, false) == null) {
            parameter = new ComponentParameter();
        } else {
            Object instance = createInstance(pico, element);
            parameter = new ConstantParameter(instance);
        }
        return parameter;
    }


    private void registerComponentFromJndi(final ClassLoadingPicoContainer container, final Element element) throws ClassNotFoundException, PicoCompositionException, MalformedURLException, NamingException {
        String key = element.getAttribute(KEY);
        String classKey = element.getAttribute(CLASS);
        String jndiName = element.getAttribute(JNDI_NAME);
        if (notSet(key)) {
            // TODO
        }
        container.addAdapter(new JNDIProvided(key, new JNDIObjectReference(jndiName), getClassLoader().loadClass(classKey)));
    }


    private void registerComponentInstance(final ClassLoadingPicoContainer container, final Element element) throws ClassNotFoundException, PicoCompositionException, MalformedURLException {
        Object instance = createInstance(container, element);
        String key = element.getAttribute(KEY);
        String classKey = element.getAttribute(CLASS_NAME_KEY);
        if (notSet(key)) {
            if (!notSet(classKey)) {
                container.addComponent(getClassLoader().loadClass(classKey), instance);
            } else {
                container.addComponent(instance);
            }
        } else {
            container.addComponent(key, instance);
        }
    }

    private Object createInstance(final PicoContainer pico, final Element element) throws MalformedURLException {
        XMLComponentInstanceFactory factory = createComponentInstanceFactory(element.getAttribute(FACTORY));
        Element instanceElement = getFirstChildElement(element, true);
        return factory.makeInstance(pico, instanceElement, getClassLoader());
    }

    private Element getFirstChildElement(final Element parent, final boolean fail) {
        NodeList children = parent.getChildNodes();
        Element child = null;
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                child = (Element) children.item(i);
                break;
            }
        }
        if (child == null && fail) {
            throw new ScriptedPicoContainerMarkupException(parent.getNodeName() + " needs a child element");
        }
        return child;
    }

    private XMLComponentInstanceFactory createComponentInstanceFactory(String factoryClass) {
        if (notSet(factoryClass)) {
            // no factory has been specified for the node
            // return globally defined factory for the container - if there is one
            if (componentInstanceFactory != null) {
                return componentInstanceFactory;
            }
            factoryClass = DEFAULT_COMPONENT_INSTANCE_FACTORY;
        }

        // using a PicoContainer is overkill here.
        try {
            return (XMLComponentInstanceFactory)getClassLoader().loadClass(factoryClass).newInstance();
        } catch (InstantiationException e) {
            throw new PicoCompositionException(e);
        } catch (IllegalAccessException e) {
            throw new PicoCompositionException(e);
        } catch (ClassNotFoundException e) {
            throw new PicoClassNotFoundException(factoryClass, e);
        }
    }

    private void addComponentAdapter(final ClassLoadingPicoContainer container, final Element element, final ClassLoadingPicoContainer metaContainer) throws ClassNotFoundException, PicoCompositionException, MalformedURLException {
        String className = element.getAttribute(CLASS);
        if (notSet(className)) {
            throw new ScriptedPicoContainerMarkupException("'" + CLASS + "' attribute not specified for " + element.getNodeName());
        }
        Class<?> implementationClass = getClassLoader().loadClass(className);
        Object key = element.getAttribute(KEY);
        String classKey = element.getAttribute(CLASS_NAME_KEY);
        if (notSet(key)) {
            if (!notSet(classKey)) {
                key = getClassLoader().loadClass(classKey);
            } else {
                key = implementationClass;
            }
        }
        Parameter[] parameters = createChildParameters(container, element);
        ComponentFactory componentFactory = createComponentFactory(element.getAttribute(FACTORY), metaContainer);

        container.as(Characteristics.NONE).addAdapter(componentFactory.createComponentAdapter(new NullComponentMonitor(), new NullLifecycleStrategy(), new Properties(), key, implementationClass, new ConstructorParameters(parameters), null, null));
    }

    private ComponentFactory createComponentFactory(final String factoryName, final ClassLoadingPicoContainer metaContainer) throws PicoCompositionException {
        if (notSet(factoryName)) {
            return new Caching().wrap(new ConstructorInjection());
        }
        final Serializable key;
        if (metaContainer.getComponentAdapter(factoryName) != null) {
            key = factoryName;
        } else {
            metaContainer.addComponent(ComponentFactory.class, new ClassName(factoryName));
            key = ComponentFactory.class;
        }
        return (ComponentFactory) metaContainer.getComponent(key);
    }


    @SuppressWarnings({"serial","synthetic-access"})
    public static class CompFactoryWrappingInjectionType extends AbstractInjectionType {

        ConstructorInjection constructorInjection = new ConstructorInjection();

        @SuppressWarnings("unchecked")
		public <T> ComponentAdapter<T> createComponentAdapter(final ComponentMonitor monitor, final LifecycleStrategy lifecycle, final Properties props, final Object key, final Class<T> impl, final ConstructorParameters constructorParams, final FieldParameters[] fieldParams, final MethodParameters[] methodParams)
                throws PicoCompositionException {

            ComponentAdapter<T> adapter = constructorInjection.createComponentAdapter(monitor, lifecycle, props, key, impl, constructorParams, fieldParams, methodParams);
            String otherKey = props.getProperty("ForCAF");
            if (otherKey != null && !otherKey.equals("")) {
                props.remove("ForCAF");
                return new MySingleMemberInjector(key, impl, monitor, false, true, otherKey, (Injector) adapter);
            }
            return adapter;
        }
    }

    /**
     * @todo Revisit params used for this injector (if any)
     */
    @SuppressWarnings("serial")
    private static class MySingleMemberInjector extends MultiArgMemberInjector {
        private final String otherKey;
        private final Injector injector;

        @SuppressWarnings("unchecked")
		private MySingleMemberInjector(final Object key, final Class impl,
                                       final ComponentMonitor monitor,
                                       final boolean useNames, final boolean requireUseOfAllParameters, final String otherKey, final Injector injector) {
            super(key, impl, null, monitor, useNames, requireUseOfAllParameters);
            this.otherKey = otherKey;
            this.injector = injector;
        }

        @Override
        public Object getComponentInstance(final PicoContainer container, final Type into) throws PicoCompositionException {
            Behavior bf = (Behavior) injector.getComponentInstance(container, into);
            bf.wrap((ComponentFactory) container.getComponent(otherKey));
            return bf;
        }
    }
}
