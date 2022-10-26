package io.jans.agama.engine.script;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.jans.agama.model.EngineConfig;
import io.jans.util.Pair;
import io.jans.service.cdi.util.CdiUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtils {

    private static final Logger LOG = LoggerFactory.getLogger(LogUtils.class);
    //MUST be a single character string
    private static final String PLACEHOLDER = "%";

    private static int maxIterableItems = 1;
    
    private enum LogLevel {
        ERROR, WARN, INFO, DEBUG, TRACE;
        
        String getValue() {
            return toString().toLowerCase();
        }

    }
    
    /**
     * rest has at least 1 element
     * @param rest 
     */
    public static void log(Object ...rest) {

        LogLevel level;
        int dummyArgs = 0;
        String sfirst;
        int nargs = rest.length - 1;
        
        maxIterableItems = CdiUtil.bean(EngineConfig.class).getMaxItemsLoggedInCollections();

        Object first = rest[0];
        if (first != null && first instanceof String) {
            Pair<LogLevel, String> p = getLogLevel(first.toString());
            level = p.getFirst();

            if (ignoreLogStatement(level)) return;
            
            Pair<String, Integer> q = getFormatString(p.getSecond(), nargs);
            sfirst = q.getFirst();
            dummyArgs = q.getSecond();
            
        } else {
            level = LogLevel.INFO;
            
            if (ignoreLogStatement(level)) return;
            
            sfirst = asString(first) + getFormatString("", nargs).getFirst();
        }

        Object[] args = new String[nargs + dummyArgs];
        for (int i = 0; i < nargs; i++) {
            args[i] = asString(rest[i + 1]);
        }
        Arrays.fill(args, nargs, args.length, "");
        String result = String.format(sfirst, args);

        switch (level) {
            case ERROR:
                LOG.error(result);
                break;
            case WARN:
                LOG.warn(result);
                break;
            case INFO:
                LOG.info(result);
                break;
            case DEBUG:
                LOG.debug(result);
                break;
            case TRACE:
                LOG.trace(result);
                break;
        }
        
    }
    
    private static boolean ignoreLogStatement(LogLevel logLevel) {
        
        switch (logLevel) {
            case TRACE: return !LOG.isTraceEnabled();
            case DEBUG: return !LOG.isDebugEnabled();
            case INFO: return !LOG.isInfoEnabled();
            case WARN: return !LOG.isWarnEnabled();
            case ERROR: return !LOG.isErrorEnabled();
        }
        return false;
        
    }
    
    private static Pair<LogLevel, String> getLogLevel(String first) {

        LogLevel level = null;
        String newFirst = null;

        String suffix = " ";
        if (first.startsWith("@")) {
            level = Stream.of(LogLevel.values()).filter(
                    l -> {
                        String lev = l.getValue();
                        return first.startsWith("@" + lev.substring(0, 1) + suffix) 
                            || first.startsWith("@" + lev + suffix);
                    }
            ).findFirst().orElse(null);

            if (level != null) {
                int levLen = first.substring(2).startsWith(suffix) ? 1 : level.getValue().length();
                newFirst = first.substring(1 + levLen + suffix.length());
            }
        }
        
        if (level == null) {
            newFirst = first;
            level = LogLevel.INFO;
        }
        return new Pair<>(level, newFirst);
                
    }

    private static Pair<String, Integer> getFormatString(String str, int nargs) {
    
        Integer dummyArgs = 0;
        String tmp = str.replace(PLACEHOLDER, "%s");
        int existingPlaceHolders = tmp.length() - str.length();
        
        if (existingPlaceHolders > 0) {
            int excess = existingPlaceHolders - nargs; 
            if (excess < 0) {
                tmp += " %s".repeat(-excess);
            } else {
                dummyArgs = excess;
            }
        } else {
            tmp = str + " %s".repeat(nargs);
        }
        return new Pair<>(tmp, dummyArgs);
        
    }
    
    private static String subListAsString(List<?> list, int originalSize) {
        
        StringBuilder sb = new StringBuilder("[");
        list.forEach(item -> sb.append(asString(item)).append(", "));

        if (originalSize > maxIterableItems) {
            sb.append("...").append(originalSize - maxIterableItems).append(" more");
        } else {
            sb.deleteCharAt(sb.length() - 1);
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.append("]").toString();
        
    }

    private static String asString(Object obj) {
        
        if (obj == null) return "null";
        Class<?> objCls = obj.getClass();
        
        //JS-native numeric values always come as doubles; make them look like integers if that's the case
        if (objCls.equals(Double.class)) {

            Double d = (Double) obj;
            if (Math.floor(d) == d && d >= 1.0*Long.MIN_VALUE && d <= 1.0*Long.MAX_VALUE) {
                return Long.toString(d.longValue());
            }

        } else if (objCls.isArray()) {
            
            List<Object> list = new ArrayList<>();
            int len = Array.getLength(obj);

            for (int i = 0; i < Math.min(len, maxIterableItems); i++) {
                list.add(Array.get(obj, i));
            }
            return subListAsString(list, len);
            
        } else if (Collection.class.isInstance(obj)) {

            Collection<?> col = (Collection<?>) obj;
            Iterator iterator = col.iterator();
            
            List<Object> list = new ArrayList<>();
            int len = col.size();
            
            for (int i = 0; i < Math.min(len, maxIterableItems); i++) {
                list.add(iterator.next());
            }            
            return subListAsString(list, len);  
            
        } else if (Map.class.isInstance(obj)) {
            
            Map map = (Map) obj;
            List<AbstractMap.SimpleImmutableEntry> entries = new ArrayList<>();
            int i = 0;

            for (Object key : map.keySet()) {                
                entries.add(new AbstractMap.SimpleImmutableEntry(key, map.get(key)));
                if (++i == maxIterableItems) break;
            }
            return subListAsString(entries, map.size());
            
        } else if (Map.Entry.class.isInstance(obj)) {

            Map.Entry e = (Map.Entry) obj;
            return String.format("(%s: %s)", asString(e.getKey()), asString(e.getValue()));
            
        } else if (Throwable.class.isInstance(obj)) {

            Throwable t = (Throwable) obj;
            try(
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw)) {
                
                t.printStackTrace(pw);
                return sw.toString();
            } catch(IOException e) {
                //can be ignored
            }
        }
        return obj.toString();

    }
    
}
