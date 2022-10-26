package io.jans.agama.engine.serialize;

import io.jans.agama.engine.service.ActionService;
import io.jans.agama.engine.service.ManagedBeanService;
import io.jans.agama.model.serialize.Type;
import io.jans.util.Pair;
import io.jans.service.cdi.util.CdiUtil;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Set;

import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.NativeJavaList;
import org.mozilla.javascript.NativeJavaMap;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NativeJavaBox implements Serializable {
    
    private static final long serialVersionUID = 3843792598994958978L;        
    private static final Logger logger = LoggerFactory.getLogger(NativeJavaBox.class);

    private static final ManagedBeanService MBSRV = CdiUtil.bean(ManagedBeanService.class);
    private static final SerializerFactory SERFACT = CdiUtil.bean(SerializerFactory.class);
    private static final ActionService ACTSRV = CdiUtil.bean(ActionService.class);
    
    private NativeJavaObject raw;
    private Object unwrapped;
    
    public NativeJavaBox(NativeJavaObject raw) {
        
        this.raw = raw;        
        unwrapped = raw.unwrap();
        
        if (NativeJavaObject.class.isInstance(unwrapped)) {
            throw new UnsupportedOperationException("Unexpected NativeJavaObject inside a NativeJavaObject");
        }
            
        logger.trace("NativeJavaBox created");

    }
    
    private void writeObject(ObjectOutputStream out) throws IOException {

        String rawClsName = raw.getClass().getName();
        logger.trace("{} to the output stream", rawClsName);

        out.writeUTF(rawClsName);
        out.writeObject(raw.getParentScope());
        logger.trace("Underlying object is an instance of {}", unwrapped.getClass().getName());

        Pair<Class<?>, Set<Annotation>> metadata = MBSRV.getBeanMetadata(unwrapped);
        Class<?> cdiBeanClass = metadata.getFirst();

        boolean cdiBean = cdiBeanClass != null; 
        out.writeBoolean(cdiBean);

        if (cdiBean) {
            String realClassName = cdiBeanClass.getName();
            Set<Annotation> qualies = metadata.getSecond();
            logger.trace("Managed bean class {}", realClassName);

            //store class name and qualifiers only, not the bean itself
            out.writeUTF(realClassName);
            out.writeObject(qualies);   //kryo fails deserializing Annotations :(
        } else {

            //The object serializer instance may change at runtime. It has to be looked up every time 
            ObjectSerializer serializer = SERFACT.get();
            boolean useJavaOnlySerialization = serializer == null;

            out.writeObject(useJavaOnlySerialization ? null : serializer.getType());
            //unwrapped is not a managed object
            if (useJavaOnlySerialization) {
                out.writeObject(unwrapped);
            } else {
                serializer.serialize(unwrapped, out);
            }
        }

    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {

        Class<?> rawCls = ACTSRV.classFromName(in.readUTF());

        logger.trace("{} in the input stream", rawCls.getName());
        Scriptable parentScope = (Scriptable) in.readObject();

        boolean cdiBean = in.readBoolean();
        if (cdiBean) {

            String realClassName = in.readUTF(); 
            Set<Annotation> qualies = (Set<Annotation>) in.readObject();

            logger.trace("Managed bean class {}", realClassName);
            unwrapped = ManagedBeanService.instance(ACTSRV.classFromName(realClassName), qualies);
        } else {

            Type type = (Type) in.readObject();
            ObjectSerializer serializer = SERFACT.get(type);
            unwrapped = serializer == null ? in.readObject() : serializer.deserialize(in);
        }

        logger.trace("Underlying object is an instance of {}", unwrapped.getClass().getName());
        
        if (rawCls.equals(NativeJavaObject.class)) {
            raw = new NativeJavaObject(parentScope, unwrapped, unwrapped.getClass());
            
        } else if (rawCls.equals(NativeJavaClass.class)) {
            raw = new NativeJavaClass(parentScope, (Class<?>) unwrapped);
            
        } else if (rawCls.equals(NativeJavaList.class)) {
            raw = new NativeJavaList(parentScope, unwrapped);
            
        } else if (rawCls.equals(NativeJavaArray.class)) {
            raw = NativeJavaArray.wrap(parentScope, unwrapped);
            
        } else if (rawCls.equals(NativeJavaMap.class)) {
            raw = new NativeJavaMap(parentScope, unwrapped);
            
        }

    }

    public NativeJavaObject getRaw() {
        logger.trace("Returning raw instance");
        return raw;
    }
    
}
