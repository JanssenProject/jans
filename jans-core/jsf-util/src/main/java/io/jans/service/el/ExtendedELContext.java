/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.el;

import jakarta.el.ELContext;

/**
 * @author Yuriy Movchan Date: 05/22/2017
 */
public abstract class ExtendedELContext extends ELContext {

    public abstract ConstantResolver getConstantResolver();

}
