/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.el;

import javax.el.ExpressionFactory;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

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
