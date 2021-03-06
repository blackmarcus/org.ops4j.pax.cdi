/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.cdi.extension;

import org.easymock.EasyMock;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.cdi.api.Component;
import org.ops4j.pax.cdi.api.Config;
import org.ops4j.pax.cdi.api.Dynamic;
import org.ops4j.pax.cdi.api.Global;
import org.ops4j.pax.cdi.api.Greedy;
import org.ops4j.pax.cdi.api.PrototypeScoped;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class MandatoryConfigDependentTest extends AbstractTest {

    @Test(timeout = 10000)
    public void test() throws Exception {
        startConfigAdmin();

        synchronized (Hello.instance) {
            CdiContainer container = EasyMock.createMock(CdiContainer.class);
            CdiContainerFactory factory = EasyMock.createMock(CdiContainerFactory.class);
            EasyMock.expect(factory.getContainer(framework)).andReturn(container).anyTimes();
            container.pause();
            EasyMock.expectLastCall().atLeastOnce();
            container.resume();
            EasyMock.expectLastCall().atLeastOnce();
            container.stop();
            EasyMock.expectLastCall().anyTimes();
            container.start(EasyMock.anyObject());
            EasyMock.expectLastCall().anyTimes();
            EasyMock.replay(container, factory);

            register(CdiContainerFactory.class, factory);

            WeldContainer cdi = createCdi(Hello.class, ConfigHolder.class, ProcessorImpl.class);

            // create configuration
            System.err.println("Setting configuration");
            getConfiguration(MyConfig.class).update(dictionary("message", "hello world"));

            Hello hello = cdi.select(Hello.class).iterator().next();

            Assert.assertEquals("Message: hello world", hello.sayHelloWorld());

            System.err.println("Updating configuration");
            getConfiguration(MyConfig.class).update(dictionary("message", "hello world2"));
            Thread.sleep(500);
            Assert.assertEquals("Message: hello world2", hello.sayHelloWorld());

            // delete configuration
            getConfiguration(MyConfig.class).delete();
        }
    }

    @interface MyConfig {

        String message() default "hello";

    }

    @Component @PrototypeScoped
    public static class ConfigHolder {

        @Inject @Config
        MyConfig config;

        public String message() {
            System.err.println("Getting message from: " + this + ": " + config.message());
            return config.message();
        }

        @PostConstruct
        public void init() {
            System.err.println("Activating InnerImpl from: " + this);
        }

        @PreDestroy
        public void destroy() {
            System.err.println("Deactivating InnerImpl from: " + this);
        }

    }

    @Component @Global
    public static class ProcessorImpl {

        @Inject @Component
        ConfigHolder inner;

        public String message() {
            return inner.message();
        }

        @PostConstruct
        public void init() {
            System.err.println("Activating ProcessorImpl");
        }

        @PreDestroy
        public void destroy() {
            System.err.println("Deactivating ProcessorImpl");
        }
    }

    public static class Hello {

        static final AtomicInteger created = new AtomicInteger();
        static final AtomicInteger destroyed = new AtomicInteger();
        static final AtomicReference<Hello> instance = new AtomicReference<>();

        @Inject @Any
        ProcessorImpl processor;

        @PostConstruct
        public void init() {
            created.incrementAndGet();
            instance.set(this);
            System.err.println("Creating Hello instance");
            synchronized (instance) {
                instance.notifyAll();
            }
        }

        @PreDestroy
        public void destroy() {
            destroyed.incrementAndGet();
            instance.set(null);
            System.err.println("Destroying Hello instance");
            synchronized (instance) {
                instance.notifyAll();
            }
        }

        public String sayHelloWorld() {
            return "Message: " + processor.message();
        }
    }

}
