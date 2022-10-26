/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service.antlr.scimFilter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.lang.StringUtils;
import io.jans.scim.model.exception.SCIMException;
import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.service.AttributeService;
import io.jans.scim.service.antlr.scimFilter.antlr4.ScimFilterBaseListener;
import io.jans.scim.service.antlr.scimFilter.antlr4.ScimFilterLexer;
import io.jans.scim.service.antlr.scimFilter.antlr4.ScimFilterParser;
import io.jans.scim.service.antlr.scimFilter.util.FilterUtil;
import io.jans.model.GluuAttribute;
import io.jans.orm.model.PersistenceConfiguration;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.service.PersistanceFactoryService;
import io.jans.orm.util.StringHelper;

import org.slf4j.Logger;

/**
 * @author Val Pecaoco
 * Re-engineered by jgomer on 2017-12-09.
 */
@ApplicationScoped
public class ScimFilterParserService {

    @Inject
    private Logger log;

	@Inject
	private PersistanceFactoryService persistenceFactoryService;

	@Inject
	private AttributeService attrService;

	@Inject
	PersistenceConfiguration persistenceConfiguration;

    private boolean ldapBackend;

    public boolean isLdapBackend() {
        return ldapBackend;
    }

    private ParseTree getParseTree(String filter, ScimFilterErrorListener errorListener){

        ANTLRInputStream input = new ANTLRInputStream(filter);
        ScimFilterLexer lexer = new ScimFilterLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        ScimFilterParser parser = new ScimFilterParser(tokens);
        parser.setTrimParseTree(true);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        return parser.filter();
    }

    public ParseTree getParseTree(String filter) throws Exception {

        ScimFilterErrorListener errorListener=new ScimFilterErrorListener();
        ParseTree tree=getParseTree(filter, errorListener);
        checkParsingErrors(errorListener);
        return tree;

    }

    private void checkParsingErrors(ScimFilterErrorListener errorListener) throws SCIMException {

        String outputErr=errorListener.getOutput();
        String symbolErr=errorListener.getSymbol();
        if (StringUtils.isNotEmpty(outputErr) || StringUtils.isNotEmpty(symbolErr))
            throw new SCIMException(String.format("Error parsing filter (symbol='%s'; message='%s')", symbolErr, outputErr));

    }

    private void walkTree(String filter, ScimFilterBaseListener listener) throws SCIMException {

        ScimFilterErrorListener errorListener=new ScimFilterErrorListener();
        ParseTree tree=getParseTree(filter, errorListener);
        checkParsingErrors(errorListener);
        ParseTreeWalker.DEFAULT.walk(listener, tree);

    }

    public Filter createFilter(String filter, Filter defaultFilter, Class<? extends BaseScimResource> clazz) throws SCIMException {

        try {
            Filter ldapFilter;

            if (StringUtils.isEmpty(filter))
                ldapFilter=defaultFilter;
            else {
            	List<GluuAttribute> allAttributes = attrService.getAllAttributes();
            	Map<String, GluuAttribute> allAttributesMap = buildAttributesMap(allAttributes);
                FilterListener filterListener = new FilterListener(clazz, allAttributesMap, ldapBackend);
                walkTree(FilterUtil.preprocess(filter, clazz), filterListener);
                ldapFilter = filterListener.getFilter();

                if (ldapFilter == null)
                    throw new Exception("An error occurred when building LDAP filter: " + filterListener.getError());
            }

            return ldapFilter;
        }
        catch (Exception e){
            throw new SCIMException(e.getMessage(), e);
        }

    }

    private Map<String, GluuAttribute> buildAttributesMap(List<GluuAttribute> attributes) {
    	Map<String, GluuAttribute> attributesMap = new HashMap<>();
    	for(GluuAttribute attribute : attributes ) {
    		attributesMap.put(StringHelper.toLowerCase(attribute.getName()), attribute);
    	}

    	return attributesMap;
	}

	public Boolean complexAttributeMatch(ParseTree parseTree, Map<String, Object> item, String parent, Class<? extends BaseScimResource> clazz) throws Exception {

        MatchFilterVisitor matchVisitor=new MatchFilterVisitor(item, parent, clazz);
        return matchVisitor.visit(parseTree);
    }

    @PostConstruct
    private void init() {
        ldapBackend = persistenceFactoryService.getPersistenceEntryManagerFactory(
        		persistenceConfiguration).getPersistenceType().equals("ldap");
    }

}
