/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.document.store.conf;

import javax.xml.bind.annotation.XmlEnum;

/**
 * @author Yuriy Movchan on 04/10/2020
 */
@XmlEnum(String.class)
public enum DocumentStoreType {
    LOCAL, JCA, WEB_DAV
}
