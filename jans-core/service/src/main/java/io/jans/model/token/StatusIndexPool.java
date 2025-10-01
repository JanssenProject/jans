package io.jans.model.token;

import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;

/**
 * @author Yuriy Movchan
 * @version 1.0, 06/03/2024
 */
@DataEntry(sortBy = "jansNum")
@ObjectClass(value = "jansStatusIdxPool")
public class StatusIndexPool extends AbstractIndexPool {

	private static final long serialVersionUID = -2122431771066187529L;
}
