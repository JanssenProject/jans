package io.jans.service.document.store.conf;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author shekhar L. on 29/10/2022
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DBDocumentStoreConfiguration implements Serializable {

	private static final long serialVersionUID = 3380170170265842538L;

	@Override
	public String toString() {
		return "DBDocumentStoreConfiguration []";
	}

}
