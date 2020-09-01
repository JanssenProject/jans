/**
 * 
 */
package org.gluu.oxauthconfigapi.rest.ressource;

/**
 * @author Mougang T.Gasmyr
 *
 */
public class BaseResource {

	protected static final String READ_ACCESS = "oxauth-config-read";
	protected static final String WRITE_ACCESS = "oxauth-config-write";

//	public static <T> void checkResourceNotNull(T resource, String objectName) {
//		if (resource == null) {
//			throw new WebApplicationException(getResourceNotFoundError(objectName));
//		}
//	}
//
//	public static <T> void checkNotNull(String attribute, String attributeName) {
//		if (attribute == null) {
//			throw new WebApplicationException(getMissingAttributeError(attributeName));
//		}
//	}
//
//	public static <T> void checkNotEmpty(List<T> list, String attributeName) {
//		if (list == null || list.isEmpty()) {
//			throw new WebApplicationException(getMissingAttributeError(attributeName));
//		}
//	}

}
