package com.picocontainer.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;

import org.junit.Test;
import com.picocontainer.script.testmodel.FredImpl;
import com.picocontainer.script.testmodel.ThingThatTakesParamsInConstructor;
import com.picocontainer.script.testmodel.Wilma;
import com.picocontainer.script.testmodel.WilmaImpl;
import com.picocontainer.script.xml.XMLContainerBuilder;

import com.picocontainer.DefaultPicoContainer;
import com.picocontainer.MutablePicoContainer;
import com.picocontainer.behaviors.Caching;
import com.picocontainer.behaviors.Storing;
import com.picocontainer.parameters.ComponentParameter;

/**
 * Test case to prove that the DefaultContainerRecorder can be replaced by use of Storing behaviours.
 *
 * @author Konstantin Pribluda
 * @author Aslak Helles&oslash;y
 * @author Mauro Talevi
 */
public class StoringContainerTestCase {

    @Test public void testInvocationsCanBeRecordedAndReplayedOnADifferentContainerInstance() throws Exception {

        // This test case is not testing Storing. Its just testing that a Caching parent does so.
        DefaultPicoContainer parent = new DefaultPicoContainer(new Caching());
        parent.addComponent("fruit", "apple");
        parent.addComponent("int", 239);
        parent.addComponent("thing",
                ThingThatTakesParamsInConstructor.class,
                ComponentParameter.DEFAULT,
                ComponentParameter.DEFAULT);

        Storing storing1 = new Storing();
        DefaultPicoContainer child1 = new DefaultPicoContainer(parent, storing1);
        assertEquals("store should be empty", 0, storing1.getCacheSize());
        Object a1 = child1.getComponent("fruit");
        assertEquals("store should still be empty: its not used", 0, storing1.getCacheSize());
        ThingThatTakesParamsInConstructor a2 = (ThingThatTakesParamsInConstructor) child1.getComponent("thing");
        assertEquals("apple", a1);
        assertEquals("apple239", a2.getValue());

        // test that we can replay once more
        Storing storing2 = new Storing();
        DefaultPicoContainer child2 = new DefaultPicoContainer(parent, storing2);
        assertEquals("store should be empty", 0, storing2.getCacheSize());
        Object b1 = child2.getComponent("fruit");
        assertEquals("store should still be empty: its not used", 0, storing2.getCacheSize());
        ThingThatTakesParamsInConstructor b2 = (ThingThatTakesParamsInConstructor) child2.getComponent("thing");
        assertEquals("apple", b1);
        assertEquals("apple239", b2.getValue());

        assertSame("cache of 'recording' parent container should be caching", a1,b1);
        assertSame("cache of 'recording' parent container should be caching", a2,b2);
    }

    @Test public void testRecorderWorksAfterSerialization() throws IOException, ClassNotFoundException, IllegalAccessException, InvocationTargetException {
        DefaultPicoContainer recorded = new DefaultPicoContainer(new Caching());
        recorded.addComponent("fruit", "apple");
        DefaultPicoContainer replayed = new DefaultPicoContainer(recorded, new Storing());
        DefaultPicoContainer serializedReplayed = (DefaultPicoContainer) serializeAndDeserialize(replayed);
        assertEquals("apple", serializedReplayed.getComponent("fruit"));
    }

    private Object serializeAndDeserialize(final Object o) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);

        oos.writeObject(o);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));

        return ois.readObject();
    }


    @Test public void scriptedPopulationOfContainerHierarchy() {

        MutablePicoContainer parent = new DefaultPicoContainer(new Caching());

        // parent has nothing populated in it
        DefaultPicoContainer child = new DefaultPicoContainer(parent, new Storing());

        new XMLContainerBuilder(new StringReader(""
                + "<container>"
                + "  <component-implementation key='wilma' class='"+WilmaImpl.class.getName()+"'/>"
                + "</container>"
                ), Thread.currentThread().getContextClassLoader()).populateContainer(child);

        assertNull(child.getComponent("fred"));
        assertNotNull(child.getComponent("wilma"));

        DefaultPicoContainer grandchild = new DefaultPicoContainer(child, new Storing());

        new XMLContainerBuilder(new StringReader(
                  "<container>"
                + "  <component-implementation key='fred' class='"+FredImpl.class.getName()+"'>"
                + "     <parameter key='wilma'/>"
                + "  </component-implementation>"
                + "</container>"
                ), Thread.currentThread().getContextClassLoader()).populateContainer(grandchild);

        assertNotNull(grandchild.getComponent("fred"));
        assertNotNull(grandchild.getComponent("wilma"));

        FredImpl fred = (FredImpl)grandchild.getComponent("fred");
        Wilma wilma = (Wilma)grandchild.getComponent("wilma");

        assertSame(wilma, fred.wilma());
    }

}
