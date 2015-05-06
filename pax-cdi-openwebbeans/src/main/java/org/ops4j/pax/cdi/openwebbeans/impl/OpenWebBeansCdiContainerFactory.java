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
package org.ops4j.pax.cdi.openwebbeans.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.webbeans.config.WebBeansContext;
import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.ops4j.pax.cdi.spi.CdiContainerListener;
import org.ops4j.pax.cdi.spi.CdiContainerType;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * {@link CdiContainerFactory} implementation based on Apache OpenWebBeans.
 *
 * @author Harald Wellmann
 *
 */
@Component
public class OpenWebBeansCdiContainerFactory implements CdiContainerFactory {

    private Map<Long, CdiContainer> containers = new HashMap<Long, CdiContainer>();
    private List<CdiContainerListener> listeners = new ArrayList<CdiContainerListener>();
    private ComponentContext componentContext;

    public OpenWebBeansCdiContainerFactory() {
    }

    /**
     * Called by the service component runtime when this component gets activated.
     *
     * @param cc
     *            component context
     */
    @Activate
    public void activate(ComponentContext cc) {
        this.componentContext = cc;
    }

    @Override
    public String getProviderName() {
        return WebBeansContext.class.getName();
    }

    @Override
    public CdiContainer createContainer(Bundle bundle, Collection<Bundle> extensions,
        CdiContainerType containerType) {
        Bundle ownBundle = componentContext.getBundleContext().getBundle();
        OpenWebBeansCdiContainer container = new OpenWebBeansCdiContainer(containerType, ownBundle,
            bundle, extensions);
        containers.put(bundle.getBundleId(), container);
        for (CdiContainerListener listener : listeners) {
            listener.postCreate(container);
        }
        return container;
    }

    @Override
    public CdiContainer getContainer(Bundle bundle) {
        return containers.get(bundle.getBundleId());
    }

    @Override
    public Collection<CdiContainer> getContainers() {
        return Collections.unmodifiableCollection(containers.values());
    }

    @Override
    public void removeContainer(Bundle bundle) {
        CdiContainer container = containers.remove(bundle.getBundleId());
        for (CdiContainerListener listener : listeners) {
            listener.preDestroy(container);
        }
    }

    @Override
    public void addListener(CdiContainerListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(CdiContainerListener listener) {
        listeners.remove(listener);
    }
}
