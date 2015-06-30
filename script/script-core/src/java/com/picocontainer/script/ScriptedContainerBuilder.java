/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 *****************************************************************************/
package com.picocontainer.script;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import com.picocontainer.PicoContainer;

/**
 * Abstract class for script-based container builders
 *
 * @author Aslak Helles&oslash;y
 * @author Obie Fernandez
 * @author Mauro Talevi
 */
public abstract class ScriptedContainerBuilder extends AbstractContainerBuilder {

    private final Reader scriptReader;
    private final URL scriptURL;
    private final ClassLoader classLoader;


    public ScriptedContainerBuilder(final Reader script, final ClassLoader classLoader) {
        super();
    	this.scriptReader = script;
        if (script == null) {
            throw new NullPointerException("script");
        }
        this.scriptURL = null;
        this.classLoader = classLoader;
        if (classLoader == null) {
            throw new NullPointerException("classLoader");
        }
    }

    public ScriptedContainerBuilder(final URL script, final ClassLoader classLoader) {
        super();
    	this.scriptReader = null;
        this.scriptURL = script;
        if (script == null) {
            throw new NullPointerException("script");
        }
        this.classLoader = classLoader;
        if (classLoader == null) {
            throw new NullPointerException("classLoader");
        }
    }

    @Override
    protected final PicoContainer createContainer(final PicoContainer parentContainer, final Object assemblyScope) {
        try {
            return createContainerFromScript(parentContainer, assemblyScope);
        } finally {
            try {
                Reader reader = getScriptReader();
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                // do nothing. we've given it our best try, now get on with it
            }
        }
    }

    protected final ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Retrieves the script input stream, however, you should use {@link #getScriptReader() getScriptReader()}
     * instead to prevent any encoding problems.
     * @return InputStream containing the data for the script.
     * @throws IOException
     */
    @SuppressWarnings("synthetic-access")
    protected final InputStream getScriptInputStream() throws IOException{
        if (scriptReader != null) {
            return new InputStream() {
                @Override
                public int read() throws IOException {
                    return scriptReader.read();
                }
            };
        }
        return scriptURL.openStream();
    }

    /**
     * Use this method to get a text-reader for the script's input stream.
     * @return a java.io.Reader instance.  (You should close it yourself
     * once finished.)
     * @throws IOException
     */
    protected final Reader getScriptReader() throws IOException{
        if (scriptReader != null) {
            return scriptReader;
        }
        return new InputStreamReader(scriptURL.openStream());
    }

    protected abstract PicoContainer createContainerFromScript(PicoContainer parentContainer, Object assemblyScope);

}