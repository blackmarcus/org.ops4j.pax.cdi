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
package org.ops4j.pax.cdi.tck.porting.owb;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.FunctionMapper;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.ResourceBundleELResolver;
import javax.el.VariableMapper;
import javax.enterprise.inject.spi.BeanManager;

import org.jboss.cdi.tck.spi.EL;

public class ELImpl implements EL {

    // private static final ExpressionFactory EXPRESSION_FACTORY = new WrappedExpressionFactory(new
    // ExpressionFactoryImpl());

    public ELImpl() {
    }

    public static ELResolver getELResolver() {
        CompositeELResolver composite = new CompositeELResolver();
        composite.add(new BeanELResolver());
        composite.add(new ArrayELResolver());
        composite.add(new MapELResolver());
        composite.add(new ListELResolver());
        composite.add(new ResourceBundleELResolver());
        composite.add(new OwbTckElResolver());

        return composite;
    }

    public static class ELContextImpl extends ELContext {

        @Override
        public ELResolver getELResolver() {
            return ELImpl.getELResolver();
        }

        @Override
        public FunctionMapper getFunctionMapper() {
            return null; // new FunctionMapperImpl();
        }

        @Override
        public VariableMapper getVariableMapper() {
            return null; // new VariableMapperImpl();
        }

    }

    @SuppressWarnings("unchecked")
    public <T> T evaluateMethodExpression(String expression, Class<T> expectedType,
        Class<?>[] expectedParamTypes, Object[] expectedParams) {
        //ELContext context = createELContext();
        Object object = null; // EXPRESSION_FACTORY.createMethodExpression(context, expression,
                              // expectedType, expectedParamTypes).invoke(context, expectedParams);

        return (T) object;
    }

    @SuppressWarnings("unchecked")
    public <T> T evaluateValueExpression(String expression, Class<T> expectedType) {
        //ELContext context = createELContext();
        Object object = null; // EXPRESSION_FACTORY.createValueExpression(context, expression,
                              // expectedType).getValue(context);

        return (T) object;
    }

    public ELContext createELContext(BeanManager beanManager) {
        ELContext context = new ELContextImpl();

        return context;
    }

    @Override
    public <T> T evaluateValueExpression(BeanManager beanManager, String expression,
        Class<T> expectedType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T evaluateMethodExpression(BeanManager beanManager, String expression,
        Class<T> expectedType, Class<?>[] expectedParamTypes, Object[] expectedParams) {
        // TODO Auto-generated method stub
        return null;
    }

}
