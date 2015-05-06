/*
 * Copyright 2014 Harald Wellmann
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

package org.ops4j.pax.cdi.extension.impl.context;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

/**
 * Specialized service factory builder for OSGi 6.0 This factory also handles protoytype scoped
 * beans.
 *
 * @author Harald Wellmann
 *
 */
public class Osgi6ServiceFactoryBuilder extends ServiceFactoryBuilder {

    /**
     * Constructs a service factory builder.
     *
     * @param beanManager
     *            bean manager of current bean bundle
     */
    public Osgi6ServiceFactoryBuilder(BeanManager beanManager) {
        super(beanManager);
    }

    @Override
    protected <S> Object buildPrototypeScopeServiceFactory(PrototypeScopeContext context,
        Bean<S> bean) {
        return new PrototypeScopeServiceFactory<S>(context, bean);
    }
}
