package org.xdi.service.security;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.util.AnnotationLiteral;

/**
 * Security extension
 * 
 * @author Yuriy Movchan Date: 05/22/2017
 */
public class SecurityExtension implements Extension, Serializable {
	
	private static final long serialVersionUID = 8135226493110893567L;

    public <X> void processAnnotatedType(@Observes ProcessAnnotatedType<X> pat) {
		// Wrap this to override the annotations of the class
		final AnnotatedType<X> at = pat.getAnnotatedType();
		
		// Collect Secure annotation from the type
        List<Secure> typeSecureAnnotations = new ArrayList<Secure>();
        for (Annotation annotation : pat.getAnnotatedType().getAnnotations()) {
            collectAnnotations(Secure.class, annotation, typeSecureAnnotations);
        }

        // Collect the Secure annotations from the methods
    	Map<Method, InterceptSecure> interceptSecureForMethods = new HashMap<Method, InterceptSecure>();
        for (AnnotatedMethod<?> method : pat.getAnnotatedType().getMethods()) {
            final List<Secure> methodAnnotations = new ArrayList<Secure>(typeSecureAnnotations);

            collectAnnotations(Secure.class, method, methodAnnotations);

            // Store in the map if we find Secure annotations
            if (methodAnnotations.size() > 0) {
                InterceptSecure is = new InterceptSecureImpl(methodAnnotations.toArray(new Secure[methodAnnotations.size()]));
                
                // Add InterceptSecure annotation
                method.getAnnotations().add(is);

                interceptSecureForMethods.put(method.getJavaMember(), is);
            }
        }

        AnnotatedType<X> wrapped = new AnnotatedType<X>() {
        	@Override
			public Type getBaseType() {
				return at.getBaseType();
			}

			@Override
			public Set<Type> getTypeClosure() {
				return at.getTypeClosure();
			}

			@Override
			public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
				return at.getAnnotation(annotationType);
			}

			@Override
			public <T extends Annotation> Set<T> getAnnotations(Class<T> annotationType) {
				return (Set<T>) at.getAnnotations();
			}

			@Override
			public Set<Annotation> getAnnotations() {
				return at.getAnnotations();
			}

			@Override
			public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
				if (InterceptSecure.class.equals(annotationType)) {
					return true;
				}

				return at.isAnnotationPresent(annotationType);
			}

			@Override
			public Class<X> getJavaClass() {
				return at.getJavaClass();
			}

			@Override
			public Set<AnnotatedConstructor<X>> getConstructors() {
				return at.getConstructors();
			}

			@Override
			public Set<AnnotatedMethod<? super X>> getMethods() {
				return at.getMethods();
			}

			@Override
			public Set<AnnotatedField<? super X>> getFields() {
				return at.getFields();
			}
		};
		
		if (interceptSecureForMethods.size() > 0) {
			pat.setAnnotatedType(wrapped);
		}
    }

    private <T> void collectAnnotations(Class<T> annotationType, AnnotatedMethod method, List<T> values) {
        for (Annotation annotation : method.getAnnotations()) {
            collectAnnotations(annotationType, annotation, values);
        }
    }

    private <T> void collectAnnotations(Class<T> annotationType, Annotation annotation, List<T> values) {
        if (annotationType.isAssignableFrom(annotation.annotationType())) {
            values.add((T) annotation);
        }
    }

    private static class InterceptSecureImpl extends AnnotationLiteral<InterceptSecure> implements InterceptSecure {

    	private final Secure[] values;

        InterceptSecureImpl(Secure[] values) {
            this.values = values;
        }

        @Override
        public Secure[] value() {
            return values;
        }
    }

}