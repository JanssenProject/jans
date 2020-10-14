/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

// Generated from ScimFilter.g4 by ANTLR 4.5.3
package io.jans.scim.service.antlr.scimFilter.antlr4;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ScimFilterParser}.
 */
public interface ScimFilterListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ScimFilterParser#attrpath}.
	 * @param ctx the parse tree
	 */
	void enterAttrpath(ScimFilterParser.AttrpathContext ctx);
	/**
	 * Exit a parse tree produced by {@link ScimFilterParser#attrpath}.
	 * @param ctx the parse tree
	 */
	void exitAttrpath(ScimFilterParser.AttrpathContext ctx);
	/**
	 * Enter a parse tree produced by {@link ScimFilterParser#compareop}.
	 * @param ctx the parse tree
	 */
	void enterCompareop(ScimFilterParser.CompareopContext ctx);
	/**
	 * Exit a parse tree produced by {@link ScimFilterParser#compareop}.
	 * @param ctx the parse tree
	 */
	void exitCompareop(ScimFilterParser.CompareopContext ctx);
	/**
	 * Enter a parse tree produced by {@link ScimFilterParser#compvalue}.
	 * @param ctx the parse tree
	 */
	void enterCompvalue(ScimFilterParser.CompvalueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ScimFilterParser#compvalue}.
	 * @param ctx the parse tree
	 */
	void exitCompvalue(ScimFilterParser.CompvalueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ScimFilterParser#attrexp}.
	 * @param ctx the parse tree
	 */
	void enterAttrexp(ScimFilterParser.AttrexpContext ctx);
	/**
	 * Exit a parse tree produced by {@link ScimFilterParser#attrexp}.
	 * @param ctx the parse tree
	 */
	void exitAttrexp(ScimFilterParser.AttrexpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code simpleExpr}
	 * labeled alternative in {@link ScimFilterParser#filter}.
	 * @param ctx the parse tree
	 */
	void enterSimpleExpr(ScimFilterParser.SimpleExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code simpleExpr}
	 * labeled alternative in {@link ScimFilterParser#filter}.
	 * @param ctx the parse tree
	 */
	void exitSimpleExpr(ScimFilterParser.SimpleExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code andFilter}
	 * labeled alternative in {@link ScimFilterParser#filter}.
	 * @param ctx the parse tree
	 */
	void enterAndFilter(ScimFilterParser.AndFilterContext ctx);
	/**
	 * Exit a parse tree produced by the {@code andFilter}
	 * labeled alternative in {@link ScimFilterParser#filter}.
	 * @param ctx the parse tree
	 */
	void exitAndFilter(ScimFilterParser.AndFilterContext ctx);
	/**
	 * Enter a parse tree produced by the {@code negatedFilter}
	 * labeled alternative in {@link ScimFilterParser#filter}.
	 * @param ctx the parse tree
	 */
	void enterNegatedFilter(ScimFilterParser.NegatedFilterContext ctx);
	/**
	 * Exit a parse tree produced by the {@code negatedFilter}
	 * labeled alternative in {@link ScimFilterParser#filter}.
	 * @param ctx the parse tree
	 */
	void exitNegatedFilter(ScimFilterParser.NegatedFilterContext ctx);
	/**
	 * Enter a parse tree produced by the {@code orFilter}
	 * labeled alternative in {@link ScimFilterParser#filter}.
	 * @param ctx the parse tree
	 */
	void enterOrFilter(ScimFilterParser.OrFilterContext ctx);
	/**
	 * Exit a parse tree produced by the {@code orFilter}
	 * labeled alternative in {@link ScimFilterParser#filter}.
	 * @param ctx the parse tree
	 */
	void exitOrFilter(ScimFilterParser.OrFilterContext ctx);
}