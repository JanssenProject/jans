package io.jans.scim.service.antlr.scimFilter.util;

import java.util.Map;
import java.util.SortedSet;
import java.util.regex.Pattern;

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

/**
 * @author Val Pecaoco
 * Updated by jgomer on 2017-12-12.
 */
public class FilterUtil {

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

    private static int startIndexParentAttr(String str){

        int i;
        for (i=str.length(); i>0 && Pattern.matches("\\w", str.substring(i-1, i)); i--);
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
     * Transalte a filter string such as emails[type eq "work" and value co "@example.com"] or ims[type eq "xmpp" and value co "@foo.com"]
     * into (emails.type eq "work" and emails.value co "@example.com") or (ims.type eq "xmpp" and ims.value co "@foo.com")
     * @param filther
     * @param clazz
     * @return
     * @throws Exception
     */
    private static String removeBrackets(String filther, Class<? extends BaseScimResource> clazz) throws Exception {

        //Remove brackets: [] and adjust by prefixing accordingly
        int j, offset =0;
        int i= filther.indexOf("[");
        StringBuilder sb=new StringBuilder();

        while (i!=-1) {
            //find closing bracket:
            j= filther.indexOf("]", i);
            if (j==-1)
                throw new Exception("Invalid filter: closing bracket ']' expected");

            String str= filther.substring(i+1, j);
            int k=startIndexParentAttr(filther.substring(offset, i)) + offset;
            if (k==i)
                throw new Exception("Invalid filter: no parent attrPath found before opening braket '['");

            sb.append(filther.substring(offset, k)).append("(");
            sb.append(applyPrefix(filther.substring(k,i), str, clazz));
            sb.append(")");

            offset=j+1;
            i= filther.indexOf("[", offset);
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
