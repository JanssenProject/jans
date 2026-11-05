package io.jans.agama.engine.script;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import io.jans.agama.model.EngineConfig;
import io.jans.util.Pair;
import io.jans.service.cdi.util.CdiUtil;

import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptStackElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtils {

    private static final Logger LOG = LoggerFactory.getLogger(LogUtils.class);
    //MUST be a single character string
    private static final String PLACEHOLDER = "%";
    private static final String PREFIX_FORMAT = "|%s| ";

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
        logPrefixed(null, rest);
    }

    /**
     * rest has at least 1 element
     * @param rest 
     */
    public static void logPrefixed(String prefix, Object ...rest) {

        LogLevel level;
        int dummyArgs = 0;
        int offset = 0;
        String sfirst;
        int nargs = rest.length - 1;
        
        maxIterableItems = CdiUtil.bean(EngineConfig.class).getMaxItemsLoggedInCollections();

        Object first = rest[0];
        if (first != null && first instanceof String) {
            Pair<LogLevel, String> p = getLogLevel(first.toString());
            level = p.getFirst();

            if (ignoreLogStatement(level)) return;
            
            Pair<String, Integer> q = getFormatString(escapeLineBreaks(p.getSecond()), nargs);
            sfirst = q.getFirst();
            dummyArgs = q.getSecond();
            
        } else {
            level = LogLevel.INFO;
            
            if (ignoreLogStatement(level)) return;
            
            // passed as an argument, not as format text, so % sequences in the value are not interpreted
            sfirst = "%s" + getFormatString("", nargs).getFirst();
            offset = 1;
        }

        Object[] args = new String[offset + nargs + dummyArgs];
        if (offset == 1) {
            args[0] = logSafe(first);
        }
        for (int i = 0; i < nargs; i++) {
            Object arg = rest[i + 1];
            args[offset + i] = logSafe(arg);
        }
        Arrays.fill(args, offset + nargs, args.length, "");
        String pref = prefix == null ? "" : String.format(PREFIX_FORMAT, prefix);
        String result = pref + String.format(sfirst, args);

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
            default:
                break;
        }
        
    }
    
    private static String escapeLineBreaks(String str) {
        return str.replace("\r", "\\r").replace("\n", "\\n");
    }

    private static String logSafe(Object obj) {
        return obj instanceof Throwable ? throwableAsString((Throwable) obj) : escapeLineBreaks(asString(obj));
    }

    // Built from the throwable's structure so message text cannot forge frames or causes
    private static String throwableAsString(Throwable t) {

        StringBuilder sb = new StringBuilder();
        appendThrowable(sb, t, null, "", Collections.newSetFromMap(new IdentityHashMap<>()));
        return sb.toString();

    }

    private static void appendThrowable(StringBuilder sb, Throwable t, String caption, String indent, Set<Throwable> seen) {

        if (caption != null) {
            sb.append('\n').append(indent).append(caption);
        }
        if (!seen.add(t)) {
            sb.append("[CIRCULAR REFERENCE: ").append(t.getClass().getName()).append(']');
            return;
        }
        sb.append(t.getClass().getName());
        if (t.getMessage() != null) {
            sb.append(": ").append(escapeLineBreaks(t.getMessage()));
        }
        if (t instanceof RhinoException) {
            // script frames are only added by Rhino's printStackTrace override
            for (ScriptStackElement e : ((RhinoException) t).getScriptStack()) {
                sb.append('\n').append(indent).append("\tat ").append(escapeLineBreaks(e.toString()));
            }
        }
        for (StackTraceElement e : t.getStackTrace()) {
            sb.append('\n').append(indent).append("\tat ").append(escapeLineBreaks(e.toString()));
        }
        for (Throwable suppressed : t.getSuppressed()) {
            appendThrowable(sb, suppressed, "Suppressed: ", indent + "\t", seen);
        }
        if (t.getCause() != null) {
            appendThrowable(sb, t.getCause(), "Caused by: ", indent, seen);
        }

    }

    private static boolean ignoreLogStatement(LogLevel logLevel) {
        
        switch (logLevel) {
            case TRACE: return !LOG.isTraceEnabled();
            case DEBUG: return !LOG.isDebugEnabled();
            case INFO: return !LOG.isInfoEnabled();
            case WARN: return !LOG.isWarnEnabled();
            case ERROR: return !LOG.isErrorEnabled();
            default: break;
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
