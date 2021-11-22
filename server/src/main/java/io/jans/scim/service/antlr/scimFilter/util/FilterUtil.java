/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service.antlr.scimFilter.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.jans.scim.model.scim2.AttributeDefinition.Type;
import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.util.IntrospectUtil;
import io.jans.scim.model.scim2.util.ScimResourceUtil;
import io.jans.scim.service.antlr.scimFilter.antlr4.ScimFilterParser;
import io.jans.scim.service.antlr.scimFilter.enums.CompValueType;
import io.jans.scim.service.antlr.scimFilter.enums.ScimOperator;
import io.jans.util.Pair;

import java.util.Map;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilterUtil {
    
    //the shortest filter expression between square brackets is 6 characters long: ref pr
    //(dollar sign in $ref is dropped by ReferenceURIInterceptor)
    private static final int MINLEN_VALFILTER = 6;
    //the shortest subattribute name length is 3: ref
    private static final Pattern VALFILTER_PATTERN = Pattern.compile("\\(*+(not\\()?+\\(*+\\w{3,}+\\s++(eq|ne|co|sw|ew|gt|lt|ge|le|pr).*");
    private static final Pattern WORDCHAR_PATTERN = Pattern.compile("\\w");

    private static Logger log = LogManager.getLogger(FilterUtil.class);

    public static CompValueType getCompValueType(ScimFilterParser.CompvalueContext compValueCtx){

        CompValueType valueType=null;

        if (compValueCtx.BOOLEAN()!=null)
            valueType=CompValueType.BOOLEAN;
        else
        if (compValueCtx.NUMBER()!=null)
            valueType=CompValueType.NUMBER;
        else
        if (compValueCtx.NULL()!=null)
            valueType=CompValueType.NULL;
        else
        if (compValueCtx.STRING()!=null)
            valueType = CompValueType.STRING;

        return valueType;

    }

    private static String checkTypeConsistency(String attribute, Type attrType, CompValueType type){

        boolean pass;
        String error=null;

        if (Type.STRING.equals(attrType) || Type.REFERENCE.equals(attrType) || Type.DATETIME.equals(attrType))
            pass=type.equals(CompValueType.STRING);   //Here STRING refers to ScimFilter.g4 rules
        else
        if (Type.INTEGER.equals(attrType) || Type.DECIMAL.equals(attrType))
            pass=type.equals(CompValueType.NUMBER);   //Here NUMBER refers to ScimFilter.g4 rule
        else
        if (Type.BOOLEAN.equals(attrType))
            pass=type.equals(CompValueType.BOOLEAN);   //Here BOOLEAN refers to ScimFilter.g4 rule
        else {
            error=String.format("Filtering over attributes of type '%s' is not supported", attrType);
            log.error("checkTypeConsistency. {}", error);
            return error;
        }

        if (!pass) {
            error=getTypeInconsistencyError(attribute, attrType.toString(), type.toString());
            log.error("checkTypeConsistency. {}", error);
        }

        return error;

    }

    public static String checkFilterConsistency(String attribute, Type attrType, CompValueType type, ScimOperator operator){

        String error=null;

        if (type.equals(CompValueType.NULL)) {
            if (!operator.equals(ScimOperator.EQUAL) && !operator.equals(ScimOperator.NOT_EQUAL)) {
                //Only makes sense if operator is eq or neq
                error=String.format("Cannot use null in a filter with operator '%s'", operator.getValue());
                log.error("checkFilterConsistency. {}", error);
            }
        }
        else
            error = checkTypeConsistency(attribute, attrType, type);

        return error;

    }

    public static void logOperatorInconsistency(String operator, String type, String attrname){
        log.error(getOperatorInconsistencyError(operator, type, attrname));
    }

    public static String getOperatorInconsistencyError(String operator, String type, String attrname){
        return String.format("Operator '%s' is not consistent with type '%s' of attribute %s", operator, type, attrname);
    }

    private static String getTypeInconsistencyError(String attrname, String type1, String type2){
        return String.format("Attribute %s is of type '%s' but compare value supplied is of type '%s'", attrname, type1, type2);
    }

    public static String preprocess(String filther, Class<? extends BaseScimResource> clazz) throws Exception{

        filther = filther.replaceAll(ScimResourceUtil.getDefaultSchemaUrn(clazz) + ":", "");
        filther = removeBrackets(filther, clazz);
        log.debug("Filter transformed to: {}", filther);

        return filther;

    }

    private static int startIndexParentAttr(String str) {

        int i = str.length();
        for (; i > 0 && WORDCHAR_PATTERN.matcher(str.substring(i - 1, i)).matches(); i--);
        return i;

    }

    private static String applyPrefix(String parent, String innerExpr, Class<? extends BaseScimResource> clazz){

        SortedSet<String> validPaths= IntrospectUtil.allAttrs.get(clazz);
        //Find all those starting with parent
        for (String path : validPaths.tailSet(parent)){
            if (path.startsWith(parent)){
                String subAttr=path.substring(parent.length());
                if (subAttr.startsWith(".")){
                    subAttr=subAttr.substring(1);
                    //add parent prefix to all occurrences of subAttr in the expression
                    innerExpr=innerExpr.replaceAll(subAttr, parent + "." + subAttr);
                }
            }
            else
                break;
        }
        return innerExpr;
    }

    /**
     * Translates a filter string such as emails[type eq "work" and value co "@example.com"] or ims[type eq "xmpp" and value co "@foo.com"]
     * into (emails.type eq "work" and emails.value co "@example.com") or (ims.type eq "xmpp" and ims.value co "@foo.com").
     * This is a best-effort approximate algorithm, it's not 100% accurate
     * @param filther
     * @param clazz
     * @return
     * @throws Exception
     */
    private static String removeBrackets(String filther, Class<? extends BaseScimResource> clazz) throws Exception {

        int offset = 0;
        int open = filther.indexOf("[");
        StringBuilder sb=new StringBuilder();

        while (open != -1 && open + MINLEN_VALFILTER + 1 < filther.length()) {
            int close = -1;
            int k = startIndexParentAttr(filther.substring(offset, open)) + offset;

            if (k < open) {
                close = filther.indexOf("]", open + MINLEN_VALFILTER);
                
                while (close != -1) {
                    String str = filther.substring(open + 1, close);
                    Matcher m = VALFILTER_PATTERN.matcher(str);

                    if (m.matches()) {
                        sb.append(filther.substring(offset, k)).append("(");
                        sb.append(applyPrefix(filther.substring(k, open), str, clazz));
                        sb.append(")");
                        offset = close + 1;

                        break;
                    } else {
                        close = filther.indexOf("]", close + 2);
                    }
                }
                
            }
            if (close == -1) {
                k = Math.min(filther.length(), open + 2);
                sb.append(filther.substring(offset, k));
                offset = k;
            }
            open = filther.indexOf("[", offset);
            
        }
        //"Paste" the remaining unprocessed part
        sb.append(filther.substring(offset));        
        return sb.toString();

    }

    public static Pair<String, Boolean> getLdapAttributeOfResourceAttribute(String path, Class<? extends BaseScimResource> resourceClass){

        log.trace("getLdapAttributeOfResourceAttribute. path={}", path);
        boolean inner=false;
        Map<String, String> refs=IntrospectUtil.storeRefs.get(resourceClass);
        String ldapAttribute=null;

        while (ldapAttribute==null && path.length()>0){
            ldapAttribute=refs.get(path);

            if (ldapAttribute==null) {
                int i = path.indexOf(".");
                inner = i >= 0;
                i = (i == -1) ? 0 : i;
                path = path.substring(0, i);
            }
        }
        return new Pair<>(ldapAttribute, inner);

    }

}
