package io.jans.agama.engine.serialize;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.minlog.Log;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.jans.agama.engine.service.ActionService;

import org.slf4j.Logger;

@ApplicationScoped
public class KryoSerializer {
    
    @Inject
    private Logger logger;

    @Inject
    private ActionService actionService;
    
    private ThreadLocal<Kryo> kryos;

    public Object deserialize(InputStream in) {

        logger.trace("Kryodeserializing");
        Input input = new Input(in);
        //If input is closed, the input's InputStream is closed
        return kryos.get().readClassAndObject(input);

    }

    public void serialize(Object data, OutputStream out) throws IOException {

        logger.trace("Kryoserializing");
        Output output = new Output(out);
        kryos.get().writeClassAndObject(output, data);
        output.flush();

    }

    @PostConstruct
    private void init() {
        
        Log.DEBUG();
        kryos = new ThreadLocal<Kryo>() {

            @Override
            protected Kryo initialValue() {
                Kryo kryo = new Kryo();
                kryo.setRegistrationRequired(false);
                kryo.setReferences(true);
                kryo.setClassLoader(actionService.getClassLoader());
                kryo.setOptimizedGenerics(false);
                return kryo;
            }
            
        };

    }
    
}
