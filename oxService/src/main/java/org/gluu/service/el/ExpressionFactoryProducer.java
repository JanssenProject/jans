package org.gluu.service.el;

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
