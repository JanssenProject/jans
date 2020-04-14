package org.gluu.service.document.store.conf;

import javax.xml.bind.annotation.XmlEnum;

/**
 * @author Yuriy Movchan on 04/10/2020
 */
@XmlEnum(String.class)
public enum DocumentStoreType {
    LOCAL, JCA, WEB_DAV
}
