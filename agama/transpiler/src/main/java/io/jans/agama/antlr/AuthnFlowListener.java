// Generated from AuthnFlow.g4 by ANTLR 4.9.2
package io.jans.agama.antlr;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link AuthnFlowParser}.
 */
public interface AuthnFlowListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#flow}.
	 * @param ctx the parse tree
	 */
	void enterFlow(AuthnFlowParser.FlowContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#flow}.
	 * @param ctx the parse tree
	 */
	void exitFlow(AuthnFlowParser.FlowContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#header}.
	 * @param ctx the parse tree
	 */
	void enterHeader(AuthnFlowParser.HeaderContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#header}.
	 * @param ctx the parse tree
	 */
	void exitHeader(AuthnFlowParser.HeaderContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#qname}.
	 * @param ctx the parse tree
	 */
	void enterQname(AuthnFlowParser.QnameContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#qname}.
	 * @param ctx the parse tree
	 */
	void exitQname(AuthnFlowParser.QnameContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#base}.
	 * @param ctx the parse tree
	 */
	void enterBase(AuthnFlowParser.BaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#base}.
	 * @param ctx the parse tree
	 */
	void exitBase(AuthnFlowParser.BaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#inputs}.
	 * @param ctx the parse tree
	 */
	void enterInputs(AuthnFlowParser.InputsContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#inputs}.
	 * @param ctx the parse tree
	 */
	void exitInputs(AuthnFlowParser.InputsContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#short_var}.
	 * @param ctx the parse tree
	 */
	void enterShort_var(AuthnFlowParser.Short_varContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#short_var}.
	 * @param ctx the parse tree
	 */
	void exitShort_var(AuthnFlowParser.Short_varContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(AuthnFlowParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(AuthnFlowParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#preassign}.
	 * @param ctx the parse tree
	 */
	void enterPreassign(AuthnFlowParser.PreassignContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#preassign}.
	 * @param ctx the parse tree
	 */
	void exitPreassign(AuthnFlowParser.PreassignContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#preassign_catch}.
	 * @param ctx the parse tree
	 */
	void enterPreassign_catch(AuthnFlowParser.Preassign_catchContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#preassign_catch}.
	 * @param ctx the parse tree
	 */
	void exitPreassign_catch(AuthnFlowParser.Preassign_catchContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#variable}.
	 * @param ctx the parse tree
	 */
	void enterVariable(AuthnFlowParser.VariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#variable}.
	 * @param ctx the parse tree
	 */
	void exitVariable(AuthnFlowParser.VariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#flow_call}.
	 * @param ctx the parse tree
	 */
	void enterFlow_call(AuthnFlowParser.Flow_callContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#flow_call}.
	 * @param ctx the parse tree
	 */
	void exitFlow_call(AuthnFlowParser.Flow_callContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#overrides}.
	 * @param ctx the parse tree
	 */
	void enterOverrides(AuthnFlowParser.OverridesContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#overrides}.
	 * @param ctx the parse tree
	 */
	void exitOverrides(AuthnFlowParser.OverridesContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#action_call}.
	 * @param ctx the parse tree
	 */
	void enterAction_call(AuthnFlowParser.Action_callContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#action_call}.
	 * @param ctx the parse tree
	 */
	void exitAction_call(AuthnFlowParser.Action_callContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#rrf_call}.
	 * @param ctx the parse tree
	 */
	void enterRrf_call(AuthnFlowParser.Rrf_callContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#rrf_call}.
	 * @param ctx the parse tree
	 */
	void exitRrf_call(AuthnFlowParser.Rrf_callContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#log}.
	 * @param ctx the parse tree
	 */
	void enterLog(AuthnFlowParser.LogContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#log}.
	 * @param ctx the parse tree
	 */
	void exitLog(AuthnFlowParser.LogContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#static_call}.
	 * @param ctx the parse tree
	 */
	void enterStatic_call(AuthnFlowParser.Static_callContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#static_call}.
	 * @param ctx the parse tree
	 */
	void exitStatic_call(AuthnFlowParser.Static_callContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#oo_call}.
	 * @param ctx the parse tree
	 */
	void enterOo_call(AuthnFlowParser.Oo_callContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#oo_call}.
	 * @param ctx the parse tree
	 */
	void exitOo_call(AuthnFlowParser.Oo_callContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#argument}.
	 * @param ctx the parse tree
	 */
	void enterArgument(AuthnFlowParser.ArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#argument}.
	 * @param ctx the parse tree
	 */
	void exitArgument(AuthnFlowParser.ArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#simple_expr}.
	 * @param ctx the parse tree
	 */
	void enterSimple_expr(AuthnFlowParser.Simple_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#simple_expr}.
	 * @param ctx the parse tree
	 */
	void exitSimple_expr(AuthnFlowParser.Simple_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(AuthnFlowParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(AuthnFlowParser.LiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(AuthnFlowParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(AuthnFlowParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#array_expr}.
	 * @param ctx the parse tree
	 */
	void enterArray_expr(AuthnFlowParser.Array_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#array_expr}.
	 * @param ctx the parse tree
	 */
	void exitArray_expr(AuthnFlowParser.Array_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#object_expr}.
	 * @param ctx the parse tree
	 */
	void enterObject_expr(AuthnFlowParser.Object_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#object_expr}.
	 * @param ctx the parse tree
	 */
	void exitObject_expr(AuthnFlowParser.Object_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#assignment}.
	 * @param ctx the parse tree
	 */
	void enterAssignment(AuthnFlowParser.AssignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#assignment}.
	 * @param ctx the parse tree
	 */
	void exitAssignment(AuthnFlowParser.AssignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#keypair}.
	 * @param ctx the parse tree
	 */
	void enterKeypair(AuthnFlowParser.KeypairContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#keypair}.
	 * @param ctx the parse tree
	 */
	void exitKeypair(AuthnFlowParser.KeypairContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#rfac}.
	 * @param ctx the parse tree
	 */
	void enterRfac(AuthnFlowParser.RfacContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#rfac}.
	 * @param ctx the parse tree
	 */
	void exitRfac(AuthnFlowParser.RfacContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#finish}.
	 * @param ctx the parse tree
	 */
	void enterFinish(AuthnFlowParser.FinishContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#finish}.
	 * @param ctx the parse tree
	 */
	void exitFinish(AuthnFlowParser.FinishContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#choice}.
	 * @param ctx the parse tree
	 */
	void enterChoice(AuthnFlowParser.ChoiceContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#choice}.
	 * @param ctx the parse tree
	 */
	void exitChoice(AuthnFlowParser.ChoiceContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#option}.
	 * @param ctx the parse tree
	 */
	void enterOption(AuthnFlowParser.OptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#option}.
	 * @param ctx the parse tree
	 */
	void exitOption(AuthnFlowParser.OptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#ifelse}.
	 * @param ctx the parse tree
	 */
	void enterIfelse(AuthnFlowParser.IfelseContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#ifelse}.
	 * @param ctx the parse tree
	 */
	void exitIfelse(AuthnFlowParser.IfelseContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#caseof}.
	 * @param ctx the parse tree
	 */
	void enterCaseof(AuthnFlowParser.CaseofContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#caseof}.
	 * @param ctx the parse tree
	 */
	void exitCaseof(AuthnFlowParser.CaseofContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#boolean_op_expr}.
	 * @param ctx the parse tree
	 */
	void enterBoolean_op_expr(AuthnFlowParser.Boolean_op_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#boolean_op_expr}.
	 * @param ctx the parse tree
	 */
	void exitBoolean_op_expr(AuthnFlowParser.Boolean_op_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#boolean_expr}.
	 * @param ctx the parse tree
	 */
	void enterBoolean_expr(AuthnFlowParser.Boolean_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#boolean_expr}.
	 * @param ctx the parse tree
	 */
	void exitBoolean_expr(AuthnFlowParser.Boolean_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#elseblock}.
	 * @param ctx the parse tree
	 */
	void enterElseblock(AuthnFlowParser.ElseblockContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#elseblock}.
	 * @param ctx the parse tree
	 */
	void exitElseblock(AuthnFlowParser.ElseblockContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#loop}.
	 * @param ctx the parse tree
	 */
	void enterLoop(AuthnFlowParser.LoopContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#loop}.
	 * @param ctx the parse tree
	 */
	void exitLoop(AuthnFlowParser.LoopContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#loopy}.
	 * @param ctx the parse tree
	 */
	void enterLoopy(AuthnFlowParser.LoopyContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#loopy}.
	 * @param ctx the parse tree
	 */
	void exitLoopy(AuthnFlowParser.LoopyContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#quit_stmt}.
	 * @param ctx the parse tree
	 */
	void enterQuit_stmt(AuthnFlowParser.Quit_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#quit_stmt}.
	 * @param ctx the parse tree
	 */
	void exitQuit_stmt(AuthnFlowParser.Quit_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#statusr_block}.
	 * @param ctx the parse tree
	 */
	void enterStatusr_block(AuthnFlowParser.Statusr_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#statusr_block}.
	 * @param ctx the parse tree
	 */
	void exitStatusr_block(AuthnFlowParser.Statusr_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#statusr_allow}.
	 * @param ctx the parse tree
	 */
	void enterStatusr_allow(AuthnFlowParser.Statusr_allowContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#statusr_allow}.
	 * @param ctx the parse tree
	 */
	void exitStatusr_allow(AuthnFlowParser.Statusr_allowContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#statusr_reply}.
	 * @param ctx the parse tree
	 */
	void enterStatusr_reply(AuthnFlowParser.Statusr_replyContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#statusr_reply}.
	 * @param ctx the parse tree
	 */
	void exitStatusr_reply(AuthnFlowParser.Statusr_replyContext ctx);
	/**
	 * Enter a parse tree produced by {@link AuthnFlowParser#statusr_until}.
	 * @param ctx the parse tree
	 */
	void enterStatusr_until(AuthnFlowParser.Statusr_untilContext ctx);
	/**
	 * Exit a parse tree produced by {@link AuthnFlowParser#statusr_until}.
	 * @param ctx the parse tree
	 */
	void exitStatusr_until(AuthnFlowParser.Statusr_untilContext ctx);
}