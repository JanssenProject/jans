/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

// Generated from ScimFilter.g4 by ANTLR 4.5.3
package io.jans.scim.service.antlr.scimFilter.antlr4;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link ScimFilterParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface ScimFilterVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link ScimFilterParser#attrpath}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttrpath(ScimFilterParser.AttrpathContext ctx);
	/**
	 * Visit a parse tree produced by {@link ScimFilterParser#compareop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompareop(ScimFilterParser.CompareopContext ctx);
	/**
	 * Visit a parse tree produced by {@link ScimFilterParser#compvalue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompvalue(ScimFilterParser.CompvalueContext ctx);
	/**
	 * Visit a parse tree produced by {@link ScimFilterParser#attrexp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttrexp(ScimFilterParser.AttrexpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code simpleExpr}
	 * labeled alternative in {@link ScimFilterParser#filter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleExpr(ScimFilterParser.SimpleExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code andFilter}
	 * labeled alternative in {@link ScimFilterParser#filter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAndFilter(ScimFilterParser.AndFilterContext ctx);
	/**
	 * Visit a parse tree produced by the {@code negatedFilter}
	 * labeled alternative in {@link ScimFilterParser#filter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNegatedFilter(ScimFilterParser.NegatedFilterContext ctx);
	/**
	 * Visit a parse tree produced by the {@code orFilter}
	 * labeled alternative in {@link ScimFilterParser#filter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrFilter(ScimFilterParser.OrFilterContext ctx);
}