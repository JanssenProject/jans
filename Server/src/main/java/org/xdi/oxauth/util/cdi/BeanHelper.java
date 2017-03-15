package org.xdi.oxauth.util.cdi;

import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for performing programmatic bean lookups.
 */

@Deprecated
public class BeanHelper {

	public final static Logger log = LoggerFactory.getLogger(BeanHelper.class);

	public static <T> T lookup(Class<T> clazz, BeanManager bm) {
		return lookup(clazz, bm, true);
	}

	public static <T> T lookup(Class<T> clazz, BeanManager bm, boolean optional) {
		Set<Bean<?>> beans = bm.getBeans(clazz);
		T instance = getContextualReference(bm, beans, clazz);
		if (!optional && instance == null) {
			throw new IllegalStateException(
					"CDI BeanManager cannot find an instance of requested type '" + clazz.getName() + "'");
		}
		return instance;
	}

	public static Object lookup(String name, BeanManager bm) {
		return lookup(name, bm, true);
	}

	public static Object lookup(String name, BeanManager bm, boolean optional) {
		Set<Bean<?>> beans = bm.getBeans(name);

		Object instance = getContextualReference(bm, beans, Object.class);
		if (!optional && instance == null) {
			throw new IllegalStateException("CDI BeanManager cannot find an instance of requested type '" + name + "'");
		}
		return instance;
	}

	public static <T> T lookup(Class<T> clazz) {
		return lookup(clazz, true);
	}

	public static <T> T lookup(Class<T> clazz, boolean optional) {
		BeanManager bm = CDI.current().getBeanManager();
		return lookup(clazz, bm, optional);
	}

	public static Object lookup(String name) {
		BeanManager bm = CDI.current().getBeanManager();
		return lookup(name, bm);
	}

	@SuppressWarnings("unchecked")
	private static <T> T getContextualReference(BeanManager bm, Set<Bean<?>> beans, Class<?> type) {
		if (beans == null || beans.size() == 0) {
			return null;
		}

		// If we would resolve to multiple beans then BeanManager#resolve would
		// throw an AmbiguousResolutionException
		Bean<?> bean = bm.resolve(beans);
		if (bean == null) {
			return null;
		} else {
			CreationalContext<?> creationalContext = bm.createCreationalContext(bean);

			// If we obtain a contextual reference to a @Dependent scope bean,
			// make sure it is released
			if (isDependentScoped(bean)) {
//				releaseOnContextClose(creationalContext, bean);
			}

			return (T) bm.getReference(bean, type, creationalContext);
		}
	}

	private static boolean isDependentScoped(Bean<?> bean) {
		return Dependent.class.equals(bean.getScope());
	}

//	private static void releaseOnContextClose(CreationalContext<?> creationalContext, Bean<?> bean) {
//		CommandContext commandContext = Context.getCommandContext();
//		if (commandContext != null) {
//			commandContext.registerCommandContextListener(new CreationalContextReleaseListener(creationalContext));
//
//		} else {
//			log.warn("Obtained instance of @Dependent scoped bean {}"
//					+ " outside of process engine command context. "
//					+ "Bean instance will not be destroyed. This is likely to create a memory leak. Please use a normal scope like @ApplicationScoped for this bean.", bean);
//
//		}
//	}

}
