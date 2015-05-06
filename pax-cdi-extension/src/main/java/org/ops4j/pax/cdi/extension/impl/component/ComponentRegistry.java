/*
 * Copyright 2013 Harald Wellmann
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

package org.ops4j.pax.cdi.extension.impl.component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A registry for all service components, i.e. managed beans qualified as
 * {@code @OsgiServiceProvider}. The registry maps each such bean type to a component descriptor
 * which tracks the service dependencies of the given component.
 *
 * @author Harald Wellmann
 *
 */
@Typed()
public class ComponentRegistry {

    private static Logger log = LoggerFactory.getLogger(ComponentRegistry.class);

    private Map<Bean<?>, ComponentDescriptor<?>> descriptors = new HashMap<>();

    private BundleContext bundleContext;

    private Set<InjectionPoint> nonComponentDependencies = new HashSet<>();


    /**
     * Adds a component bean type to the registry, creating an empty descriptor for it.
     *
     * @param component
     */
    public <S> void addComponent(Bean<S> component) {
        descriptors.put(component, new ComponentDescriptor<S>(component));
    }

    /**
     * Adds a service dependency for a bean type.
     * <p>
     * Precondition: The bean type was added to the registry by {@link #addComponent(Bean)}.
     *
     * @param component
     *            component bean
     * @param ip
     *            injection point of the given bean, qualified as {@code OsgiService}
     */
    public <S> void addDependency(Bean<S> component, InjectionPoint ip) {
        log.debug("adding dependency {} -> {}", component, ip);
        ComponentDescriptor<?> descriptor = descriptors.get(component);
        descriptor.addDependency(ip);
    }

    /**
     * Returns all component beans.
     *
     * @return set of beans
     */
    public Set<Bean<?>> getComponents() {
        return descriptors.keySet();
    }

    /**
     * Returns the component descriptor for the given bean type.
     *
     * @param component
     *            service component bean
     * @return component descriptor, or null if the bean is not a service component
     */
    public ComponentDescriptor<?> getDescriptor(Bean<?> component) {
        return descriptors.get(component);
    }

    /**
     * Gets the bundle context.
     *
     * @return the bundle context
     */
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    /**
     * Sets the bundle context.
     *
     * @param bundleContext
     *            the bundle context to set
     */
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * Adds a dependency for an injection point which is not a service component.
     *
     * @param ip
     *            injection point
     */
    public void addNonComponentDependency(InjectionPoint ip) {
        nonComponentDependencies.add(ip);
    }

    /**
     * Returns the set of beans dependencies which are not OSGi service components.
     *
     * @return the non-component dependencies
     */
    public Set<InjectionPoint> getNonComponentDependencies() {
        return nonComponentDependencies;
    }

    /**
     * Checks if the given bean is an OSGi service component.
     *
     * @param bean
     *            CDI bean
     * @return true if bean is a service component.
     */
    public boolean isComponent(Bean<?> bean) {
        return descriptors.containsKey(bean);
    }
}
