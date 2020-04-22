package org.gluu.service.el;

import javax.el.ELContext;

/**
 * @author Yuriy Movchan Date: 05/22/2017
 */
public abstract class ExtendedELContext extends ELContext {

    public abstract ConstantResolver getConstantResolver();

}
