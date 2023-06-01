package io.jans.model.custom.script.type.selectaccount;

import io.jans.model.custom.script.type.BaseExternalType;

/**
 * @author Yuriy Z
 */
public interface SelectAccountType extends BaseExternalType {

    String getSelectAccountPage(Object context);

    String getNextPage(Object context);

    String getAccountDisplayName(Object context);

    boolean onSelect(Object context);
}
