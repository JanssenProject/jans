package org.xdi.service;

import org.jboss.seam.annotations.*;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.cache.EhCacheProvider;

import static org.jboss.seam.ScopeType.APPLICATION;
import static org.jboss.seam.annotations.Install.BUILT_IN;

/**
 * Created by eugeniuparvan on 12/28/16.
 */
@Name("org.jboss.seam.cache.cacheProvider")
@Scope(APPLICATION)
@BypassInterceptors
@Install(value = false, precedence = BUILT_IN, classDependencies="net.sf.ehcache.Cache")
@AutoCreate
@Startup
public class GluuEhCacheProvider extends EhCacheProvider{
}