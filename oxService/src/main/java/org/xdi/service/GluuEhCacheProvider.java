package org.xdi.service;

import static org.jboss.seam.ScopeType.APPLICATION;

import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.cache.EhCacheProvider;

/**
 * Created by eugeniuparvan on 12/28/16.
 * 
 * @author eugeniuparvan
 * @author Yuriy Movchan
 */
@Name("gluuCacheProvider")
@Scope(APPLICATION)
@AutoCreate
@Startup
public class GluuEhCacheProvider extends EhCacheProvider{
}