// Generated from AuthnFlow.g4 by ANTLR 4.9.2
package io.jans.agama.antlr;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link AuthnFlowParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface AuthnFlowVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#flow}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlow(AuthnFlowParser.FlowContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#header}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHeader(AuthnFlowParser.HeaderContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#qname}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQname(AuthnFlowParser.QnameContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#base}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBase(AuthnFlowParser.BaseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#inputs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInputs(AuthnFlowParser.InputsContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#short_var}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShort_var(AuthnFlowParser.Short_varContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(AuthnFlowParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#preassign}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPreassign(AuthnFlowParser.PreassignContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#preassign_catch}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPreassign_catch(AuthnFlowParser.Preassign_catchContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#variable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable(AuthnFlowParser.VariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#flow_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlow_call(AuthnFlowParser.Flow_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#overrides}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOverrides(AuthnFlowParser.OverridesContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#action_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAction_call(AuthnFlowParser.Action_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#rrf_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRrf_call(AuthnFlowParser.Rrf_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#log}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLog(AuthnFlowParser.LogContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#static_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatic_call(AuthnFlowParser.Static_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#oo_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOo_call(AuthnFlowParser.Oo_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#argument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgument(AuthnFlowParser.ArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#simple_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimple_expr(AuthnFlowParser.Simple_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(AuthnFlowParser.LiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(AuthnFlowParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#array_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_expr(AuthnFlowParser.Array_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#object_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_expr(AuthnFlowParser.Object_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#assignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment(AuthnFlowParser.AssignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#keypair}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKeypair(AuthnFlowParser.KeypairContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#rfac}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRfac(AuthnFlowParser.RfacContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#finish}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFinish(AuthnFlowParser.FinishContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#choice}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitChoice(AuthnFlowParser.ChoiceContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOption(AuthnFlowParser.OptionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#ifelse}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfelse(AuthnFlowParser.IfelseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#caseof}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCaseof(AuthnFlowParser.CaseofContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#boolean_op_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBoolean_op_expr(AuthnFlowParser.Boolean_op_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#boolean_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBoolean_expr(AuthnFlowParser.Boolean_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#elseblock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElseblock(AuthnFlowParser.ElseblockContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#loop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoop(AuthnFlowParser.LoopContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#loopy}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoopy(AuthnFlowParser.LoopyContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#quit_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuit_stmt(AuthnFlowParser.Quit_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#statusr_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatusr_block(AuthnFlowParser.Statusr_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#statusr_allow}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatusr_allow(AuthnFlowParser.Statusr_allowContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#statusr_reply}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatusr_reply(AuthnFlowParser.Statusr_replyContext ctx);
	/**
	 * Visit a parse tree produced by {@link AuthnFlowParser#statusr_until}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatusr_until(AuthnFlowParser.Statusr_untilContext ctx);
}