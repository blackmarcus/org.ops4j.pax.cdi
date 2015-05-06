/*
 * Copyright 2014 Harald Wellmann.
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
package org.ops4j.pax.cdi.extension.impl.compat;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;

/**
 * Implements {@link ServiceObjectsWrapper} for OSGi 6.0 and higher. The implementation uses
 * {@link ServiceObjects} to correctly handle prototype scope, in addition to singleton and bundle
 * scope.
 *
 * @param <S>
 *            OSGi service type
 *
 * @author Harald Wellmann
 */
public class Osgi6ServiceObjectsWrapper<S> implements ServiceObjectsWrapper<S> {

    private ServiceObjects<S> serviceObjects;

    @Override
    public S getService() {
        return serviceObjects.getService();
    }

    @Override
    public void ungetService(S service) {
        serviceObjects.ungetService(service);
    }

    @Override
    public ServiceReference<S> getServiceReference() {
        return serviceObjects.getServiceReference();
    }

    @Override
    public void init(BundleContext bc, ServiceReference<S> serviceReference) {
        this.serviceObjects = bc.getServiceObjects(serviceReference);
    }
}
