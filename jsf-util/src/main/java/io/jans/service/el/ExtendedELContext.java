/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.el;

import javax.el.ELContext;

/**
 * @author Yuriy Movchan Date: 05/22/2017
 */
public abstract class ExtendedELContext extends ELContext {

    public abstract ConstantResolver getConstantResolver();

}
