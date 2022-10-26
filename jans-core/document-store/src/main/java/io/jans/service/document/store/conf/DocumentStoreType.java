/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.document.store.conf;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * @author Yuriy Movchan on 04/10/2020
 */
@XmlEnum(String.class)
public enum DocumentStoreType {
    LOCAL, JCA, WEB_DAV, DB
}
