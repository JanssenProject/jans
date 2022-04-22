/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.el;

import jakarta.el.ExpressionFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * @author Yuriy Movchan Date: 05/22/2017
 */
@ApplicationScoped
public class ExpressionFactoryProducer {

    @Produces
    @ApplicationScoped
    public ExpressionFactory createExpressionFactory() {
        return ExpressionFactory.newInstance();
    }

}
