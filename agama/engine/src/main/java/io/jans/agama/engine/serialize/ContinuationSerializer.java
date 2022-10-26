package io.jans.agama.engine.serialize;

import io.jans.agama.engine.service.ActionService;
import io.jans.util.Pair;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;

import org.mozilla.javascript.NativeContinuation;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;

@ApplicationScoped
public class ContinuationSerializer {

    @Inject
    private ActionService actionService;

    public byte[] save(Scriptable scope, NativeContinuation continuation) throws IOException {
        
        class CustomObjectOutputStream extends ObjectOutputStream {
        
            CustomObjectOutputStream(OutputStream out) throws IOException {
                super(out);
                enableReplaceObject(true);
            }

            @Override
            protected Object replaceObject​(Object obj) throws IOException {

                if (NativeJavaObject.class.isInstance(obj)) {
                    return new NativeJavaBox((NativeJavaObject) obj);
                }
                return super.replaceObject(obj);

            }
            
        }
        
        try (   ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream sos = new CustomObjectOutputStream(baos)) {

            //Pair is not java-serializable, use a 2-length array
            sos.writeObject(new Object[] { scope, continuation });
            return baos.toByteArray();
        }
        
    }

    public Pair<Scriptable, NativeContinuation> restore(byte[] data) throws IOException {

        class CustomObjectInputStream extends ObjectInputStream {

            public CustomObjectInputStream(InputStream in) throws IOException {
                super(in);
                enableResolveObject(true);
            }        

            @Override
            public Class<?> resolveClass​(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                return actionService.classFromName(desc.getName());
            }

            @Override
            protected Object resolveObject​(Object obj) throws IOException {

                if (obj != null && obj.getClass().equals(NativeJavaBox.class)) {
                    return ((NativeJavaBox) obj).getRaw();
                }
                return super.resolveObject(obj);

            }

        }
        
        try (   ByteArrayInputStream bais = new ByteArrayInputStream(data);
                ObjectInputStream sis = new CustomObjectInputStream(bais)) {
            
            Object[] arr = (Object[]) sis.readObject();            
            return new Pair<>((Scriptable) arr[0], (NativeContinuation) arr[1]);
            
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
        
    }

}
