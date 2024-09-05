package io.jans.agama.engine.serialize;

import io.jans.agama.model.EngineConfig;
import io.jans.agama.model.serialize.Type;
import io.jans.util.StringHelper; 

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;

import org.slf4j.Logger;

import static io.jans.agama.model.serialize.Type.*;

@ApplicationScoped
public class SerializationUtil {
    
    @Inject
    private KryoSerializer criolina;
    
    @Inject
    private EngineConfig econfig;
    
    @Inject
    private Logger logger;
    
    public void write(Object obj, ObjectOutputStream out) throws IOException {
        
        Type type = typeFor(obj);
        logger.trace("Serialization strategy chosen was {}", type); 
        boolean useKryo = type.equals(KRYO);
        out.writeBoolean(useKryo);

        if (useKryo) {
            criolina.serialize(obj, out);
        } else {
            out.writeObject(obj);
        }

    }
    
    public Object read(ObjectInputStream in) throws IOException, ClassNotFoundException {
        return in.readBoolean() ? criolina.deserialize(in) : in.readObject();
    }

    // For a given object, it determines the serializer to use given the following hints:
    // - If the object's class is in the default package (unnamed package), use kryo
    // - If the object is an exception use java unless the class name appears in the kryo rules of agama config
    // - Use kryo or java based on the largest package match found in the rules. When in a tie, use kryo
    // The below are examples of package matches against hypothetical class ab.ur.ri.do
    // a) ab.ur.ri
    // b) ab
    // Here, match (a) is better (larger). The below are NOT matches against the mentioned class:
    // a) ab.ur.ri.do.s
    // b) ab.ur.ri.dor
    private Type typeFor(Object budget) {
        
        Map<String, List<String>> rules = econfig.getSerializeRules();
        if (rules == null) return KRYO;
        
        String clsName = budget.getClass().getName();
        List<String> jules = aList(rules, JAVA);
        List<String> kules = aList(rules, KRYO);        

        int kryoScore = score(clsName, kules);
        if (kryoScore == -1) return KRYO;

        if (Exception.class.isInstance(budget)) {
            //Use Java for serializing exceptions except when the full classname is found in the kryo rules 
            return kryoScore == 0 ? KRYO : JAVA;
        } else {
            int jScore = score(clsName, jules);          
            return kryoScore <= jScore ? KRYO : JAVA;
        }

    }
    
    private static int score(String clsName, List<String> prefixes) {
        
        int parts = dotCount(clsName);
        if (parts == 0) return -1;     //this class is in the default package!
        
        parts++;
        int sc = 0;     //holds the largest match against a package
        
        for (String pack : prefixes) {            
            if (StringHelper.isNotEmptyString(pack)) {
                int packageParts = dotCount(pack) + 1;
                
                if (parts > packageParts) {
                    if (clsName.startsWith(pack + ".") && sc < packageParts)
                        sc = packageParts;
                } else if (parts == packageParts) {    //clsName does not belong to package pack
                    if (pack.equals(clsName)) return 0;     //perfect match (pack is actually a classname)
                }
            }
        }
        return parts - sc;

    }
    
    private static int dotCount(String str) {
        
        int s = -1, i = -1;
        do {
            i++;
            s++;
            i = str.indexOf('.', i);
        } while (i != -1);
        return s;
        
    }
    
    private static List<String> aList(Map<String, List<String>> map, Type type) {
        return map.getOrDefault(type.toString(), Collections.emptyList());
    }

}
