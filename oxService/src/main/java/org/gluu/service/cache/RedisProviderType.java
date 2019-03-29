package org.gluu.service.cache;

import javax.xml.bind.annotation.XmlEnum;

/**
 * @author yuriyz
 */
@XmlEnum(String.class)
public enum RedisProviderType {
    STANDALONE, CLUSTER, SHARDED
}
