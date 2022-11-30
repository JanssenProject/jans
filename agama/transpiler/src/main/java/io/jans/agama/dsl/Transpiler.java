package io.jans.agama.dsl;

import freemarker.ext.dom.NodeModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import io.jans.agama.antlr.AuthnFlowLexer;
import io.jans.agama.antlr.AuthnFlowParser;
import io.jans.agama.dsl.error.SyntaxException;
import io.jans.agama.dsl.error.RecognitionErrorListener;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.sf.saxon.dom.NodeOverNodeInfo;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.sapling.SaplingDocument;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Transpiler {
    
    public static final String UTIL_SCRIPT_NAME = "util.js";    
    public static final String UTIL_SCRIPT_CONTENTS;
    
    private static final String FTL_LOCATION = "JSGenerator.ftl";

    private static final ClassLoader CLS_LOADER = Transpiler.class.getClassLoader();
    private static final Configuration FM_CONFIG;

    private final Logger logger = LoggerFactory.getLogger(Transpiler.class);

    private String flowId;
    private String fanny;

    private Processor processor;
    private XPathCompiler xpathCompiler;
    private Template jsGenerator;

    static {

        try (
            Reader r = new InputStreamReader(CLS_LOADER.getResourceAsStream(UTIL_SCRIPT_NAME), UTF_8);
            StringWriter sw = new StringWriter()) {
            
            r.transferTo(sw);
            UTIL_SCRIPT_CONTENTS = sw.toString();
        } catch (IOException e) {
            throw new RuntimeException("Unable to read utility script", e);
        }

        FM_CONFIG = new Configuration(Configuration.VERSION_2_3_31);
        FM_CONFIG.setClassLoaderForTemplateLoading(CLS_LOADER, "/");
        FM_CONFIG.setDefaultEncoding(UTF_8.toString());
        FM_CONFIG.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        FM_CONFIG.setLogTemplateExceptions(false);
        FM_CONFIG.setWrapUncheckedExceptions(true);
        FM_CONFIG.setFallbackOnNullLoopVariable(false);

    }
    
    public Transpiler(String flowQName) throws TranspilerException {

        if (flowQName == null)
            throw new TranspilerException("Qualified name cannot be null", new NullPointerException());
        
        this.flowId = flowQName;
        fanny = "_" + flowQName.replaceAll("\\.", "_");    //Generates a valid JS function name

        processor = new Processor(false);
        xpathCompiler = processor.newXPathCompiler();
        xpathCompiler.setCaching(true);

        try {
            jsGenerator = FM_CONFIG.getTemplate(FTL_LOCATION);
        } catch (Exception e) {
            throw new TranspilerException("Template loading failed", e);
        }

    }
    
    private String getFanny() {
        return fanny;
    }
    
    private AuthnFlowParser.FlowContext getFlowContext(String DSLCode)
            throws SyntaxException, TranspilerException {

        InputStream is = new ByteArrayInputStream(DSLCode.getBytes(UTF_8));
        CharStream input = null;

        try {
            logger.debug("Creating ANTLR CharStream from DSL code");
            //fromStream method closes the input stream upon returning
            input = CharStreams.fromStream(is);
        } catch (IOException ioe) {
            throw new TranspilerException(ioe.getMessage(), ioe);
        }

        AuthnFlowLexer lexer = new AuthnFlowLexer(input);
        RecognitionErrorListener lexerErrListener = new RecognitionErrorListener();
        lexer.addErrorListener(lexerErrListener);
        logger.debug("Lexer for grammar '{}' initialized", lexer.getGrammarFileName());
        
        logger.debug("Creating parser");
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        AuthnFlowParser parser = new AuthnFlowParser(tokens);
        RecognitionErrorListener parserErrListener = new RecognitionErrorListener();
        parser.addErrorListener(parserErrListener);

        try {
            AuthnFlowParser.FlowContext flowContext = parser.flow();
            SyntaxException syntaxException = Stream.of(lexerErrListener, parserErrListener)
                    .map(RecognitionErrorListener::getError).filter(Objects::nonNull)
                    .findFirst().orElse(null);

            if (syntaxException != null) {
                throw syntaxException;
            } else if (!lexer._hitEOF) {
                throw new SyntaxException("Unable to process the input code thoroughly",
                        lexer.getText(), lexer.getLine(), lexer.getCharPositionInLine());                
            }
            return flowContext;

        } catch (RecognitionException re) {
            Token offender = re.getOffendingToken();
            throw new SyntaxException(re.getMessage(), offender.getText(),
                    offender.getLine(), offender.getCharPositionInLine());
        }

    }

    public XdmNode asXML(String DSLCode) throws SyntaxException, TranspilerException, SaxonApiException {

        AuthnFlowParser.FlowContext flowContext = getFlowContext(DSLCode);
        validateName(flowContext);
        logger.debug("Traversing parse tree");

        //Generate XML representation
        SaplingDocument document = Visitor.document(flowContext, AuthnFlowParser.RULE_flow, fanny);
        applyValidations(document);
        return document.toXdmNode(processor);

    }

    public List<String> getInputs(XdmNode node) throws SaxonApiException {
        
        return xpathCompiler.evaluate(Visitor.INPUTS_XPATH_EXPR, node)
                    .stream().map(XdmItem::getStringValue).collect(Collectors.toList());

    }
    
    public Integer getTimeout(XdmNode node) throws SaxonApiException {
        
        return Optional.ofNullable(
            xpathCompiler.evaluateSingle(Visitor.TIMEOUT_XPATH_EXPR, node))
                .map(XdmItem::getStringValue).map(Integer::valueOf).orElse(null);

    }
    
    public String generateJS(XdmNode node) throws TranspilerException  {

        try {
            StringWriter sw = new StringWriter();
            NodeModel model = NodeModel.wrap(NodeOverNodeInfo.wrap(node.getUnderlyingNode()));

            jsGenerator.process(model, sw);
            return sw.toString();
        } catch (IOException | TemplateException e) {
            throw new TranspilerException("Transformation failed", e);
        }

    }

    private void applyValidations(SaplingDocument doc) throws TranspilerException {

        try {
            XdmNode node = doc.toXdmNode(processor);
            
            checkAutoInvocations(node);
            checkInputsUniqueness(node);
        } catch (SaxonApiException se) {
            throw new TranspilerException("Validation failed", se);
        }

    }
    
    private void validateName(AuthnFlowParser.FlowContext flowContext) throws TranspilerException {
        
        String qname = flowContext.header().qname().getText();
        if (!flowId.equals(qname))
            throw new TranspilerException("Qualified name mismatch: " + flowId + " vs. " + qname);

    }

    private void checkAutoInvocations(XdmNode node) throws TranspilerException, SaxonApiException {
    
        Set<String> invocations = xpathCompiler.evaluate(Visitor.FLOWCALL_XPATH_EXPR, node)
                .stream().map(XdmItem::getStringValue).collect(Collectors.toSet());

        if (invocations.contains(flowId))
            throw new TranspilerException("A flow must not trigger an instance of itself");            

    }

    private void checkInputsUniqueness(XdmNode node) throws TranspilerException, SaxonApiException {
        
        List<String> inputs = getInputs(node);
        Set<String> inputsSet = inputs.stream().collect(Collectors.toSet());
        String configVar = Optional.ofNullable(
            xpathCompiler.evaluateSingle(Visitor.CONFIG_XPATH_EXPR, node))
                .map(XdmItem::getStringValue).orElse(null);
         
        if (inputsSet.size() < inputs.size())
            throw new TranspilerException("One or more input variable names are duplicated");
        
        if (configVar != null && inputsSet.contains(configVar))
            throw new TranspilerException("Configuration variable '" + configVar + 
                    "' cannot be used as an input variable");

    }

    private void logXml(XdmNode node) {
        logger.debug("\n{}", node.toString());
        //System.out.println("\n" + node.toString());
    }
    
    private void generateFromXml(String fileName, OutputStream out) throws Exception {
        NodeModel model = NodeModel.parse(Paths.get(fileName).toFile());
        jsGenerator.process(model, new OutputStreamWriter(out, UTF_8));       
    }
    
    /**
     * Transpiles the input source code to code runnable by Agama flow engine in the
     * form of a Javascript function
     * @param flowQname Qualified name of the input flow
     * @param source Source code of input flow (written using Agama DSL)
     * @return A TranspilerResult object holding the details of the generated function      
     * @throws SyntaxException When the input source has syntactic errors, details are contained
     * in the exception thrown
     * @throws TranspilerException When other kind of processing error occurred.
     */
    public static TranspilationResult transpile(String flowQname, String source)
            throws TranspilerException, SyntaxException {

        Transpiler tr = new Transpiler(flowQname);
        try {
            XdmNode doc = tr.asXML(source);
            
            TranspilationResult result = new TranspilationResult();
            result.setFuncName(tr.getFanny());
            result.setCode(tr.generateJS(doc));
            result.setInputs(tr.getInputs(doc));
            result.setTimeout(tr.getTimeout(doc));

            return result;
        } catch (SaxonApiException e) {
            throw new TranspilerException(e.getMessage(), e);
        }

    }

    public static void runSyntaxCheck(String flowQname, String source)
            throws SyntaxException, TranspilerException {

        Transpiler tr = new Transpiler(flowQname);
        tr.validateName(tr.getFlowContext(source));
    }

    public static void runSyntaxCheck(String source) throws SyntaxException, TranspilerException {
        Transpiler tr = new Transpiler("");
        tr.getFlowContext(source);
    }

    public static void main(String... args) throws Exception {

        int len = args.length;
        
        if (len != 2) {
            System.err.println("Expecting 2 params: input file path and flow ID");
            return;
        }

        Transpiler tr = new Transpiler(args[1]);
        String dslCode = new String(Files.readAllBytes(Paths.get(args[0])), UTF_8);
        
        XdmNode doc = tr.asXML(dslCode);
        tr.logXml(doc);
        System.out.println("\nInputs: " + tr.getInputs(doc));
        System.out.println("\nTimeout: " + tr.getTimeout(doc));
        System.out.println("\n" + tr.generateJS(doc));

        //tr.generateFromXml("Sample.xml", System.out);
    }

}
