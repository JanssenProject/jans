package io.jans.agama.dsl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VarsTransformer {
    
    private static final String EVAL_SYMBOL = "$";
    private static final Logger logger = LoggerFactory.getLogger(VarsTransformer.class);
    private static final Set<String> JS_KEYWORDS;
    
    //See util.js
    private static final String STRING_INDEX_FUNC = "_sc";
    private static final String INT_INDEX_FUNC = "_ic";
    
    static {
        //The following cannot be used as variable names in DSL code
        String[] javascriptKeywords = new String[] {
            // Based on https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Lexical_grammar#keywords
            "break", "case", "catch", "class", "const", "continue", 
            "debugger", "default", "delete", "do", "else", "export", "extends", 
            "finally", "for", "function", "if", "import", "in", "instanceof", "new", 
            "return", "super", "switch", "this", "throw", "try", "typeof", 
            "var", "void", "while", "with", "yield",
            "enum", "await", "let",

            // Prevent arbitrary access to Java classes from script code, see
            // http://web.archive.org/web/20210304081342/https://developer.mozilla.org/en-US/docs/Scripting_Java
            "Packages",
            
            // See https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects
            // function names not included because they cannot be invoked from DSL code
            // Control abstraction objects (except Promise), Reflection, Internationalization, 
            // and WebAssembly not yet supported in Rhino
            "Infinity", "NaN", "undefined", "globalThis",
            "Object", "Function", "Boolean", "Symbol",
            "Error", "AggregateError", "EvalError", "InternalError", "RangeError",
            "ReferenceError", "SyntaxError", "TypeError", "URIError",
            "Number", "BigInt", "Math", "Date", "String", "RegExp",
            "Array", "Int8Array", "Uint8Array", "Uint8ClampedArray", "Int16Array", "Uint16Array",
            "Int32Array", "Uint32Array", "Float32Array", "Float64Array", "BigInt64Array", "BigUint64Array",
            "Map", "Set", "WeakMap", "WeakSet",
            "ArrayBuffer", "SharedArrayBuffer", "Atomics", "DataView", "JSON",
            "Promise", "arguments"
        };
        
        JS_KEYWORDS = new HashSet(Arrays.asList(javascriptKeywords));

    }

    public static String correctedVariable(String variable) {

        int dotIndex = variable.indexOf(".");
        int rbIndex = variable.indexOf("[");
        int index;
        
        if (dotIndex != -1) {
            if (rbIndex != -1) {
                index = Math.min(dotIndex, rbIndex);
            } else {
                index = dotIndex;
            }
        } else {
            index = rbIndex;
        }
        
        String identifier =  index == -1 ? variable : variable.substring(0, index);
        if (JS_KEYWORDS.contains(identifier)) {
            logger.trace("Renaming variable {}", variable);
            identifier = "_" + identifier;
        }
        return index == -1 ? identifier : identifier + variable.substring(index);
        
    }

    public static String convertToBracketNotation(String str) {
        return processDotEvalAccessors(processStringAccessors(processExistingBrackets(str)));
    }
    
    private static String processDotEvalAccessors(String str) {
        
        String subs;
        StringBuilder sb = new StringBuilder();
        int dot, brack, symlen = EVAL_SYMBOL.length();
        int i, j = 0;
        
        i = str.indexOf("." + EVAL_SYMBOL);
        if (i == -1) return str;
        
        do {
            sb.append(str.substring(j, i));
            
            dot = str.indexOf(".", i + 1 + symlen);
            brack = str.indexOf("[", i + 1 + symlen);
            if (dot == -1) {
                if (brack == -1) {
                    j = str.length();
                } else {
                    j = brack;
                }
            } else {
                if (brack == -1) {
                    j = dot;
                } else {
                    j = Math.min(dot, brack);
                }
            }

            subs = str.substring(i + 1 + symlen, j);
            sb.append(String.format("[%s(%s, \"%s\")]", STRING_INDEX_FUNC, subs, subs));
            i = str.indexOf("." + EVAL_SYMBOL, j);
        } while (i != -1);
        
        sb.append(str.substring(j));
        subs = sb.toString();
        logger.trace("processDotEvalAccessors: {} converted to {}", str, subs);
        return subs;
        
    }
    
    private static String processStringAccessors(String str) {
        
        String subs;
        StringBuilder sb = new StringBuilder();
        int i, j = -1;
        
        i = str.indexOf(".\"");
        if (i == -1) return str;

        do {
            sb.append(str.substring(j + 1, i));
            j = str.indexOf("\"", i + 2);
            //j cannot be -1
            subs = str.substring(i + 1, j + 1);
            //null passed for 2nd param since it is guaranteed subs is a string
            sb.append(String.format("[%s(%s, null)]", STRING_INDEX_FUNC, subs));
            i = str.indexOf(".\"", j + 1);
        } while (i != -1);
        
        sb.append(str.substring(j + 1));
        subs = sb.toString();
        logger.trace("processStringAccessors: {} converted to {}", str, subs);
        return subs;
        
    }
    
    private static String processExistingBrackets(String str) {
        
        String subs;
        StringBuilder sb = new StringBuilder();
        int i, j = 0;
        
        i = str.indexOf("[");
        if (i == -1) return str;
        
        do {
            sb.append(str.substring(j, i + 1));
            j = str.indexOf("]", i + 1);
            //j cannot be -1
            subs = str.substring(i + 1, j);
            sb.append(String.format("%s(%s, \"%s\")", INT_INDEX_FUNC, subs, subs));
            i = str.indexOf("[", j + 1);
        } while (i != -1);
        
        sb.append(str.substring(j));
        subs = sb.toString();
        logger.trace("processExistingBrackets: {} converted to {}", str, subs);
        return subs;

    }
    
}
