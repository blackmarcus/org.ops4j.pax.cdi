/*
 * Copyright 2013 Guillaume Nodet, Harald Wellmann
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
package org.ops4j.pax.cdi.extender.impl;

import static org.ops4j.pax.cdi.api.Constants.EXTENDER_CAPABILITY;
import static org.ops4j.pax.cdi.spi.BeanBundles.findExtensions;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.CDIProvider;

import org.ops4j.pax.cdi.spi.CdiContainer;
import org.ops4j.pax.cdi.spi.CdiContainerFactory;
import org.ops4j.pax.cdi.spi.CdiContainerListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the PAX CDI extender capability. This extender depends on a {@link CDIProvider}, a
 * {@link CdiContainerFactory} and and optional CDI web adapter, represented by a
 * {@link CdiContainerListener} with the property {@code type} set to {@code web}.
 * <p>
 * The extender creates a CDI container for each bean bundle. For web beans bundles, the CDI
 * container is not created until the web adapter is available.
 *
 * @author Guillaume Nodet
 * @author Harald Wellmann
 *
 */
@Component(immediate = true, service = { })
public class CdiExtender implements BundleTrackerCustomizer<CdiContainer> {

    private static Logger log = LoggerFactory.getLogger(CdiExtender.class);

    private BundleContext context;
    private BundleTracker<CdiContainer> bundleWatcher;
    private CdiContainerFactory factory;

    private CdiContainerListener webAdapter;

    private CDIProvider cdiProvider;

    private Map<Long, Bundle> webBundles = new HashMap<>();

    /**
     * Called by the OSGi framework when this bundle is started. Registers the CDI provider, handles
     * web bundles and start a bundle tracker.
     *
     * @param ctx
     *            bundle context
     */
    @Activate
    public synchronized void activate(BundleContext ctx) {
        this.context = ctx;
        if (webAdapter != null) {
            handleWebBundles();
        }

        CDI.setCDIProvider(cdiProvider);

        log.info("starting CDI extender {}", context.getBundle().getSymbolicName());
        this.bundleWatcher = new BundleTracker<>(context, Bundle.ACTIVE, this);
        bundleWatcher.open();

    }

    /**
     * Called by the OSGi framework when this bundle is stopped. Unregisters the CDI provider and
     * stops the bundle tracker. Since all extended bundles depend on this bundle's extender
     * capability, they will be stopped automatically, unregistering any services created by this
     * extender.
     *
     * @param ctx
     *            bundle context
     */
    @Deactivate
    public synchronized void deactivate(BundleContext ctx) {
        BundleCdi.dispose();

        log.info("stopping CDI extender {}", ctx.getBundle().getSymbolicName());
        bundleWatcher.close();
    }

    @Override
    public synchronized CdiContainer addingBundle(final Bundle bundle, BundleEvent event) {
        boolean wired = false;
        List<BundleWire> wires = bundle.adapt(BundleWiring.class).getRequiredWires(
            EXTENDER_CAPABILITY);
        if (wires != null) {
            for (BundleWire wire : wires) {
                if (wire.getProviderWiring().getBundle().equals(context.getBundle())) {
                    wired = true;
                    break;
                }
            }
        }
        if (wired) {
            log.debug("found bean bundle: {}", bundle.getSymbolicName());
            return createContainer(bundle);
        }
        else {
            log.trace("not a bean bundle: {}", bundle.getSymbolicName());
            return null;
        }
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, CdiContainer object) {
        // We don't care about state changes
    }

    @Override
    public synchronized void removedBundle(Bundle bundle, BundleEvent event,
        CdiContainer container) {
        if (container != null) {
            synchronized (container) {
                container.stop();
            }
        }
        factory.removeContainer(bundle);
        if (webAdapter != null) {
            webAdapter.preDestroy(container);
        }
    }

    private CdiContainer createContainer(Bundle bundle) {
        // check if this is a web bundle
        Dictionary<String, String> headers = bundle.getHeaders();
        String contextPath = headers.get("Web-ContextPath");
        boolean isWebBundle = (contextPath != null);

        CdiContainer container = null;

        // Web bundles require a web adapter.
        if (isWebBundle) {
            // If the adapter is not available, just remember the bundle
            if (webAdapter == null) {
                log.debug("waiting for web adapter for {}", bundle);
                webBundles.put(bundle.getBundleId(), bundle);
            }
            // otherwise, create the CDI container, but don't start it until the servlet
            // context is available
            else {
                container = doCreateLazyContainer(bundle);
            }
        }
        // Standalone containers are started right now.
        else {
            container = doCreateContainer(bundle);
            container.start(new Object());
        }
        return container;
    }

    private CdiContainer doCreateContainer(Bundle bundle) {
        // Find extensions
        Set<Bundle> extensions = new HashSet<>();
        findExtensions(bundle, extensions);

        log.info("creating CDI container for bean bundle {} with extension bundles {}", bundle,
            extensions);
        return factory.createContainer(bundle, extensions);
    }

    private CdiContainer doCreateLazyContainer(Bundle bundle) {
        DelegatingCdiContainer container = new DelegatingCdiContainer(factory, bundle);
        if (webAdapter != null) {
            webAdapter.postCreate(container);
        }
        return container;
    }

    /**
     * Sets the optional web adapter dependency. When the adapter is set, web bundles will be
     * processed.
     *
     * @param listener
     *            web adapter
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public synchronized void setWebAdapter(CdiContainerListener listener) {
        log.debug("adding web adapter");
        this.webAdapter = listener;
        if (context != null) {
            handleWebBundles();
        }
    }

    private void handleWebBundles() {
        factory.addListener(webAdapter);
        for (Bundle bundle : webBundles.values()) {
            doCreateLazyContainer(bundle);
        }
        webBundles.clear();
    }

    /**
     * Unsets the optional web adapter dependency.
     *
     * @param listener
     *            web adapter
     */
    public synchronized void unsetWebAdapter(CdiContainerListener listener) {
        if (factory != null) {
            factory.removeListener(listener);
        }
        this.webAdapter = null;
    }

    /**
     * Sets the CDI container factory dependency.
     *
     * @param cdiContainerFactory
     *            CDI container factory
     */
    @Reference
    public synchronized void setCdiContainerFactory(CdiContainerFactory cdiContainerFactory) {
        this.factory = cdiContainerFactory;
    }

    /**
     * Set the CDI provider dependency.
     *
     * @param cdiProvider
     *            the cdiProvider to set
     */
    @Reference
    public synchronized void setCdiProvider(CDIProvider cdiProvider) {
        this.cdiProvider = cdiProvider;
    }
}
