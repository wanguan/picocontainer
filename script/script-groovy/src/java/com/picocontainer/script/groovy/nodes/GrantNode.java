/*******************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.
 * ---------------------------------------------------------------------------
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 ******************************************************************************/
package com.picocontainer.script.groovy.nodes;

import java.security.Permission;
import java.util.Map;

import com.picocontainer.script.ScriptedPicoContainerMarkupException;

import com.picocontainer.classname.ClassPathElement;

/**
 * @author Paul Hammant
 */
@SuppressWarnings("serial")
public class GrantNode extends AbstractBuilderNode {

    public static final String NODE_NAME = "grant";

    public GrantNode() {
        super(NODE_NAME);
    }

    public Object createNewNode(final Object current, final Map<String, Object> attributes) {

        Permission permission = (Permission) attributes.remove("class");
        if (!(current instanceof ClassPathElement)) {
            throw new ScriptedPicoContainerMarkupException("Don't know how to create a 'grant' child of a '"
                    + current.getClass() + "' parent");
        }

        ClassPathElement cpe = (ClassPathElement) current;
        return cpe.grantPermission(permission);
    }

}
