/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.model.base;

import java.io.Serializable;

import io.jans.orm.annotation.DataEntry;

/**
 * Dummy entry
 *
 * @author Yuriy Movchan Date: 07.13.2011
 */
@DataEntry
public class DummyEntry extends BaseEntry implements Serializable {

    private static final long serialVersionUID = -1111582184398161100L;

}
