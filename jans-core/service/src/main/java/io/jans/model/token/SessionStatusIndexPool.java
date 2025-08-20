package io.jans.model.token;

import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;

/**
 * @author Yuriy Z
 * @version 1.0, 06/03/2024
 */
@DataEntry(sortBy = "jansNum")
@ObjectClass(value = "jansSessionStatusIdxPool")
public class SessionStatusIndexPool extends AbstractIndexPool {

    private static final long serialVersionUID = -5522431771066187529L;
}
