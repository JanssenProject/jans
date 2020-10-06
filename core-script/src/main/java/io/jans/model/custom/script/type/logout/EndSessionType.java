package io.jans.model.custom.script.type.logout;

import io.jans.model.custom.script.type.BaseExternalType;

/**
 * @author Yuriy Zabrovarnyy
 */
public interface EndSessionType extends BaseExternalType {

    String getFrontchannelHtml(Object context);
}
