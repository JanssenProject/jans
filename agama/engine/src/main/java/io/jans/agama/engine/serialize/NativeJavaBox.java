package io.jans.agama.engine.serialize;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

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
        logger.trace("{} in the output stream", rawClsName);

        out.writeUTF(rawClsName);
        out.writeObject(raw.getParentScope());

        logger.trace("Underlying object is an instance of {}", unwrapped.getClass().getName());
        ObjectSerializer serializer = ContinuationSerializer.getObjectSerializer();
        if (serializer == null) {
            out.writeObject(unwrapped);   
        } else {
            serializer.serialize(unwrapped, out);
        }
        
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        
        logger.trace("Reading NativeJavaBox");
        Class<?> rawCls = classFromName(in.readUTF());

        logger.trace("{} in the input stream", rawCls.getName());
        Scriptable parentScope = (Scriptable) in.readObject();
        
        ObjectSerializer serializer = ContinuationSerializer.getObjectSerializer();
        unwrapped = serializer == null ? in.readObject() : serializer.deserialize(in);
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
    
    private static Class<?> classFromName(String qname) throws ClassNotFoundException {
        return Class.forName(qname, false, ContinuationSerializer.getClassLoader());        
    }

    public NativeJavaObject getRaw() {
        logger.trace("Returning raw instance");
        return raw;
    }
    
}
