package io.jans.agama.engine.service;

import io.jans.util.Pair;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ClassCastException;
import java.util.HashSet;
import java.util.Set;

import org.jboss.weld.proxy.WeldClientProxy;
import org.slf4j.Logger;

@ApplicationScoped
public class ManagedBeanService {
    
    @Inject
    private Logger logger;

    @Inject 
    private BeanManager beanManager;

    public static <T> T instance(Class<T> subtype, Set<Annotation> qualifiers) {
        return CDI.current().select(subtype, qualifiers.toArray(new Annotation[0])).get();
    }
    
    public Pair<Class<?>, Set<Annotation>> getBeanMetadata(Object obj) {

        Class<?> beanClass = null;
        Set<Annotation> qualifiers = null;
        try {
            WeldClientProxy proxy = WeldClientProxy.class.cast(obj);
            //it's a managed instance
            Bean bean = proxy.getMetadata().getBean();
            beanClass = bean.getBeanClass();
            //Make it mutable
            qualifiers = new HashSet<>(bean.getQualifiers());
        } catch (ClassCastException e) {
            if (obj.getClass().isAnnotationPresent(Singleton.class)) {
                //it's a managed instance. Beans with scope @Singleton don’t have a proxy object.
                //Clients hold a direct reference to the singleton instance
                beanClass = obj.getClass();
                qualifiers = getClassQualifiers(beanClass); 
            }
        }
        if (beanClass != null) {
            qualifiers.remove(new AnnotationLiteral<Any>() {});
            logger.debug("Qualifiers found in class {}: {}", beanClass.getSimpleName(), qualifiers);
        }
        return new Pair<>(beanClass, qualifiers);

    }
    
    private Set<Annotation> getClassQualifiers(Class<?> cls) {

        Set<Annotation> qualies = new HashSet<>();
        for (Annotation ann : cls.getAnnotations()) {
            Class<? extends Annotation> clazz = ann.annotationType();

            //A qualifier annotation is annotated with @Qualifier and @Retention(RUNTIME)
            if (clazz.isAnnotationPresent(Qualifier.class)) {
                Retention ret = clazz.getAnnotation(Retention.class);
                if (ret != null && ret.value().equals(RetentionPolicy.RUNTIME)) {
                    qualies.add(ann);
                }
            }
        }
        return qualies;

    }

    /*
    public static void main(String ...args) throws Exception {
        ManagedBeanService mbs = new ManagedBeanService();
        System.out.println(mbs.getClassQualifiers(....class));
    }
    */
    
}