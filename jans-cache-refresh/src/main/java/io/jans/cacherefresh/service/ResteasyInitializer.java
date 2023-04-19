package io.jans.cacherefresh.service;

import java.util.HashSet;
import java.util.Set;

import io.jans.cacherefresh.api.impl.CacheRefreshResources;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
* Integration with Resteasy
*
* @author Yuriy Movchan
* @version 0.1, 03/21/2017
*/
@ApplicationPath("/restv1")
public class ResteasyInitializer extends Application {

   @Override
   public Set<Class<?>> getClasses() {
       HashSet<Class<?>> classes = new HashSet<>();
       classes.add(CacheRefreshResources.class);

       return classes;
   }

}
