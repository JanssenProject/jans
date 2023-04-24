package io.jans.cacherefresh.model;


import io.jans.orm.annotation.ObjectClass;

/**
 * Wrapper to add reired objectClass
 *
 * @author Yuriy Movchan Date: 05/24/2021
 */
@ObjectClass("jansPerson")
public class SimplePerson extends SimpleUser {

	private static final long serialVersionUID = -7741095209704297164L;

}
