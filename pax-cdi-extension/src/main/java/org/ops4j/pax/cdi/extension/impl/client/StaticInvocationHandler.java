/*
 * Copyright 2012 Harald Wellmann.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.cdi.extension.impl.client;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import javax.enterprise.inject.spi.InjectionPoint;

import org.ops4j.pax.cdi.extension.impl.compat.OsgiScopeUtils;
import org.ops4j.pax.cdi.extension.impl.compat.ServiceObjectsWrapper;
import org.ops4j.pax.cdi.extension.impl.util.InjectionPointOsgiUtils;
import org.ops4j.pax.cdi.spi.util.Exceptions;
import org.ops4j.pax.swissbox.core.ContextClassLoaderUtils;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;

/**
 * A static proxy invocation handler which always uses the same service reference obtained on
 * construction.
 *
 * @param <S>
 *            OSGi service type
 *
 * @author Harald Wellmann
 *
 */
public class StaticInvocationHandler<S> extends AbstractServiceInvocationHandler {

    private S service;
    private ServiceReference<S> serviceRef;
    private ServiceObjectsWrapper<S> serviceObjects;

    /**
     * Constructs a static invocation handler for the given OSGi service injection point.
     *
     * @param ip
     *            injection point
     */
    @SuppressWarnings("unchecked")
    public StaticInvocationHandler(InjectionPoint ip) {
        super(ip);
        this.serviceRef = InjectionPointOsgiUtils.getServiceReference(ip);
        this.serviceObjects = OsgiScopeUtils.createServiceObjectsWrapper(bundleContext,
            serviceRef);
        this.service = serviceObjects.getService();
    }

    @Override
    // CHECKSTYLE:SKIP
    public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {

        if (serviceRef != null) {
            return ContextClassLoaderUtils.doWithClassLoader(
                cdiContainer.getContextClassLoader(), new Callable<Object>() {

                    @Override
                    public Object call() throws Exception {
                        if (service != null) {
                            try {
                                return method.invoke(service, args);
                            }
                            catch (InvocationTargetException exc) {
                                throw Exceptions.unchecked(exc.getCause());
                            }
                        }
                        return null;
                    }
                });
        }
        throw new ServiceException("no service for injection point " + ip,
            ServiceException.UNREGISTERED);
    }

    @Override
    public void release() {
        serviceObjects.ungetService(service);
    }
}
