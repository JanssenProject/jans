package io.jans.agama.dsl;

import io.jans.agama.antlr.AuthnFlowParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.saxon.sapling.SaplingDocument;
import net.sf.saxon.sapling.SaplingElement;
import net.sf.saxon.sapling.SaplingNode;
import net.sf.saxon.sapling.Saplings;

import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Visitor {
    
    public static final String FLOWCALL_XPATH_EXPR = "//flow_call/qname/text()";
    public static final String INPUTS_XPATH_EXPR = "/flow/header/inputs/short_var/text()";
    public static final String CONFIG_XPATH_EXPR = "/flow/header/configs/short_var/text()";
    public static final String TIMEOUT_XPATH_EXPR = "/flow/header/timeout/UINT/text()";

    private static final Logger logger = LoggerFactory.getLogger(Visitor.class);
    private static final Set<Integer> INCLUDE_SYMBOLS;
    private static final Set<Integer> RULES_AS_TEXT;
    
    static {
        Integer[] asText = new Integer[] {
            AuthnFlowParser.RULE_qname, AuthnFlowParser.RULE_short_var, AuthnFlowParser.RULE_variable,
            AuthnFlowParser.RULE_expression, AuthnFlowParser.RULE_object_expr,
            AuthnFlowParser.RULE_array_expr, AuthnFlowParser.RULE_simple_expr
        };
        
        RULES_AS_TEXT = new HashSet(Arrays.asList(asText));
        
        //Symbols that may be found as leaves in the tree traversal and must not be skipped
        Integer[] includeSymbols = new Integer[] {
            AuthnFlowParser.NOT, AuthnFlowParser.AND, AuthnFlowParser.OR, AuthnFlowParser.MINUS,
            AuthnFlowParser.NUL, AuthnFlowParser.BOOL, AuthnFlowParser.STRING,
            AuthnFlowParser.UINT, AuthnFlowParser.SINT, AuthnFlowParser.DECIMAL,
            AuthnFlowParser.ALPHANUM, AuthnFlowParser.DOTEXPR, AuthnFlowParser.DOTIDXEXPR
        };
        
        INCLUDE_SYMBOLS = new HashSet(Arrays.asList(includeSymbols));
    }
    
    public static SaplingDocument document(ParseTree tree, int ruleIndex, String treeId) {
        return Saplings.doc().withChild(visitElement(tree, ruleIndex).withAttr("id", treeId));
    }
 
    private static String getRuleName(int ruleIndex) {
        return AuthnFlowParser.ruleNames[ruleIndex];
    }
    
    private static SaplingElement visitElement(ParseTree tree, int ruleIndex) {

        List<SaplingElement> childElements = new ArrayList<>();
        int nchildren = tree.getChildCount();

        for (int i = 0; i < nchildren; i++) {
            ParseTree child = tree.getChild(i);
            SaplingElement elem = null;
            
            if (child instanceof RuleContext) {

                RuleContext ruleCtx = (RuleContext) child;
                int ind = ruleCtx.getRuleIndex();
                
                if (RULES_AS_TEXT.contains(ind)) {
                    elem = Saplings.elem(getRuleName(ind)).withText(makeTextOf(child));
                } else {
                    elem = visitElement(child, ind);
                }

            } else if (child instanceof TerminalNode) {
                Token token = ((TerminalNode) child).getSymbol();
                int type = token.getType();

                if (INCLUDE_SYMBOLS.contains(type)) {
                    //logger.debug("{} {}", token.getType(), token.getText());
                    String name = AuthnFlowParser.VOCABULARY.getSymbolicName(type);
                    elem = Saplings.elem(name).withText(token.getText());
                }
            }
            
            if (elem != null) {
                childElements.add(elem);                
            }
        }

        return Saplings.elem(getRuleName(ruleIndex))
                .withChild(childElements.toArray(new SaplingNode[0]));

    }

    private static String makeTextOf(ParseTree tree) {
        
        String text = null;
        if (tree instanceof RuleContext) {

            RuleContext ruleCtx = (RuleContext) tree;
            int ind = ruleCtx.getRuleIndex();
            boolean isShortVar = AuthnFlowParser.RULE_short_var == ind;

            if (!isShortVar) {
                text = "";
                for (int i = 0; i < tree.getChildCount(); i++) {
                    text += makeTextOf(tree.getChild(i));
                }
            } else {
                text = VarsTransformer.correctedVariable(ruleCtx.getText());
            }

            if (AuthnFlowParser.RULE_variable == ind) {
                text = VarsTransformer.correctedVariable(text);
            }

        } else if (tree instanceof TerminalNode) {
            Token token = ((TerminalNode) tree).getSymbol();
            text = token.getText();
            int type = token.getType();
            
            if (type == AuthnFlowParser.DOTEXPR || type == AuthnFlowParser.DOTIDXEXPR) {
                text = VarsTransformer.convertToBracketNotation(text);
            }
        }
        return text;  
                     
    }

}
