/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *****************************************************************************/

package com.picocontainer.gems.constraints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import com.picocontainer.testmodel.AlternativeTouchable;
import com.picocontainer.testmodel.DecoratedTouchable;
import com.picocontainer.testmodel.DependsOnArray;
import com.picocontainer.testmodel.DependsOnList;
import com.picocontainer.testmodel.DependsOnTouchable;
import com.picocontainer.testmodel.DependsOnTwoComponents;
import com.picocontainer.testmodel.SimpleTouchable;
import com.picocontainer.testmodel.Touchable;

import com.picocontainer.DefaultPicoContainer;
import com.picocontainer.MutablePicoContainer;
import com.picocontainer.behaviors.Caching;
import com.picocontainer.injectors.AbstractInjector;
import com.picocontainer.parameters.ComponentParameter;

/**
 * Integration tests using Constraints.
 *
 * @author Nick Sieger
 */
public class ConstraintIntegrationTestCase {

    MutablePicoContainer container;

    @Before
    public void setUp() throws Exception {
        container = new DefaultPicoContainer(new Caching());
        container.addComponent(SimpleTouchable.class);
        container.addComponent(DependsOnTouchable.class);
        container.addComponent(DependsOnTwoComponents.class);
        container.addComponent(ArrayList.class, new ArrayList());
        container.addComponent(Object[].class, new Object[0]);
    }


    @Test public void testAmbiguouTouchableDependency() {
        container.addComponent(AlternativeTouchable.class);
        container.addComponent(DecoratedTouchable.class);

        try {
            container.getComponent(DecoratedTouchable.class);
            fail("AmbiguousComponentResolutionException expected");
        } catch (AbstractInjector.AmbiguousComponentResolutionException acre) {
            // success
        }
    }

    @Test public void testTouchableDependencyWithComponentKeyParameter() {
        container.addComponent(AlternativeTouchable.class);
        container.addComponent(DecoratedTouchable.class,
                                                  DecoratedTouchable.class,
                                                  new ComponentParameter(SimpleTouchable.class));

        Touchable t = container.getComponent(DecoratedTouchable.class);
        assertNotNull(t);
    }

    @Test public void testTouchableDependencyInjectedViaConstraint() {
        container.addComponent(AlternativeTouchable.class);
        container.addComponent(DecoratedTouchable.class,
                                                  DecoratedTouchable.class,
                                                  new Not(new IsType(SimpleTouchable.class)));
        Touchable t = container.getComponent(DecoratedTouchable.class);
        assertNotNull(t);
    }

    @Test public void testComponentDependsOnCollectionOfEverythingElse() {
        container.addComponent(DependsOnList.class,
                                                  DependsOnList.class,
                                                  new CollectionConstraint(Anything.ANYTHING));
        DependsOnList dol = container.getComponent(DependsOnList.class);
        assertNotNull(dol);
        List dependencies = dol.getDependencies();
        assertEquals(5, dependencies.size());
    }

    @Test public void testComponentDependsOnCollectionOfTouchables() {
        container.addComponent(AlternativeTouchable.class);
        container.addComponent(DecoratedTouchable.class,
                                                  DecoratedTouchable.class,
                                                  new Not(new IsType(SimpleTouchable.class)));
        container.addComponent(DependsOnList.class,
                                                  DependsOnList.class,
                                                  new CollectionConstraint(new IsType(Touchable.class)));
        DependsOnList dol = container.getComponent(DependsOnList.class);
        assertNotNull(dol);
        List dependencies = dol.getDependencies();
        assertEquals(3, dependencies.size());
    }

    @Test public void testComponentDependsOnCollectionOfSpecificTouchables() {
        container.addComponent(AlternativeTouchable.class);
        container.addComponent(DecoratedTouchable.class,
                                                  DecoratedTouchable.class,
                                                  new Not(new IsType(SimpleTouchable.class)));
        container.addComponent(DependsOnList.class,
                                                  DependsOnList.class,
                                                  new CollectionConstraint(new Or(new IsType(AlternativeTouchable.class),
                                                                                  new IsType(DecoratedTouchable.class))));

        DependsOnList dol = container.getComponent(DependsOnList.class);
        AlternativeTouchable at = container.getComponent(AlternativeTouchable.class);
        DecoratedTouchable dt = container.getComponent(DecoratedTouchable.class);
        assertNotNull(dol);
        List dependencies = dol.getDependencies();
        assertEquals(2, dependencies.size());
        assertTrue(dependencies.contains(at));
        assertTrue(dependencies.contains(dt));
    }

    @Test public void testComponentDependsOnArrayOfEverythingElse() {
        container.addComponent(DependsOnArray.class,
                                                  DependsOnArray.class,
                                                  new CollectionConstraint(Anything.ANYTHING));
        DependsOnArray doa = container.getComponent(DependsOnArray.class);
        assertNotNull(doa);
        Object[] dependencies = doa.getDependencies();
        assertEquals(5, dependencies.length);
    }

}
