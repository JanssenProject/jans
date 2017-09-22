package org.xdi.service.cache;

import javax.xml.bind.annotation.XmlEnum;

/**
 * @author yuriyz
 */
@XmlEnum(String.class)
public enum RedisProviderType {
    STANDALONE,
    CLUSTER,
    SHARDED
}
