// Generated from AuthnFlow.g4 by ANTLR 4.9.2
package io.jans.agama.antlr;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class AuthnFlowParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.9.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, NL=9, 
		COMMENT=10, FLOWSTART=11, BASE=12, FLOWINPUTS=13, LOG=14, FLOWCALL=15, 
		ACTIONCALL=16, RRFCALL=17, STATUS_CHK=18, OPEN=19, CLOSE=20, OVERRIDE=21, 
		WHEN=22, OTHERWISE=23, REPEAT=24, ITERATE=25, MATCH=26, QUIT=27, FINISH=28, 
		RFAC=29, IS=30, NOT=31, AND=32, OR=33, SECS=34, TO=35, MAXTIMES=36, USE=37, 
		EQ=38, MINUS=39, NUL=40, BOOL=41, STRING=42, UINT=43, SINT=44, DECIMAL=45, 
		ALPHANUM=46, QNAME=47, EVALNUM=48, DOTEXPR=49, DOTIDXEXPR=50, SPCOMMA=51, 
		WS=52, INDENT=53, DEDENT=54;
	public static final int
		RULE_flow = 0, RULE_header = 1, RULE_qname = 2, RULE_base = 3, RULE_inputs = 4, 
		RULE_short_var = 5, RULE_statement = 6, RULE_preassign = 7, RULE_preassign_catch = 8, 
		RULE_variable = 9, RULE_flow_call = 10, RULE_overrides = 11, RULE_action_call = 12, 
		RULE_rrf_call = 13, RULE_log = 14, RULE_static_call = 15, RULE_oo_call = 16, 
		RULE_argument = 17, RULE_simple_expr = 18, RULE_literal = 19, RULE_expression = 20, 
		RULE_array_expr = 21, RULE_object_expr = 22, RULE_assignment = 23, RULE_keypair = 24, 
		RULE_rfac = 25, RULE_finish = 26, RULE_choice = 27, RULE_option = 28, 
		RULE_ifelse = 29, RULE_caseof = 30, RULE_bool_expr = 31, RULE_boolean_op_expr = 32, 
		RULE_boolean_expr = 33, RULE_elseblock = 34, RULE_loop = 35, RULE_loopy = 36, 
		RULE_quit_stmt = 37, RULE_stchk_block = 38, RULE_stchk_open = 39, RULE_stchk_close = 40;
	private static String[] makeRuleNames() {
		return new String[] {
			"flow", "header", "qname", "base", "inputs", "short_var", "statement", 
			"preassign", "preassign_catch", "variable", "flow_call", "overrides", 
			"action_call", "rrf_call", "log", "static_call", "oo_call", "argument", 
			"simple_expr", "literal", "expression", "array_expr", "object_expr", 
			"assignment", "keypair", "rfac", "finish", "choice", "option", "ifelse", 
			"caseof", "bool_expr", "boolean_op_expr", "boolean_expr", "elseblock", 
			"loop", "loopy", "quit_stmt", "stchk_block", "stchk_open", "stchk_close"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'|'", "'$'", "'#'", "'['", "']'", "'{'", "'}'", "':'", null, null, 
			"'Flow'", "'Basepath'", "'Inputs'", "'Log'", "'Trigger'", "'Call'", "'RRF'", 
			"'Status checker'", "'Open for'", "'Close'", "'Override templates'", 
			"'When'", "'Otherwise'", "'Repeat'", "'Iterate over'", "'Match'", "'Quit'", 
			"'Finish'", "'RFAC'", "'is'", "'not'", "'and'", "'or'", "'seconds'", 
			"'to'", "'times max'", "'using'", "'='", "'-'", "'null'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, "NL", "COMMENT", 
			"FLOWSTART", "BASE", "FLOWINPUTS", "LOG", "FLOWCALL", "ACTIONCALL", "RRFCALL", 
			"STATUS_CHK", "OPEN", "CLOSE", "OVERRIDE", "WHEN", "OTHERWISE", "REPEAT", 
			"ITERATE", "MATCH", "QUIT", "FINISH", "RFAC", "IS", "NOT", "AND", "OR", 
			"SECS", "TO", "MAXTIMES", "USE", "EQ", "MINUS", "NUL", "BOOL", "STRING", 
			"UINT", "SINT", "DECIMAL", "ALPHANUM", "QNAME", "EVALNUM", "DOTEXPR", 
			"DOTIDXEXPR", "SPCOMMA", "WS", "INDENT", "DEDENT"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "AuthnFlow.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public AuthnFlowParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class FlowContext extends ParserRuleContext {
		public HeaderContext header() {
			return getRuleContext(HeaderContext.class,0);
		}
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public FlowContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_flow; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterFlow(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitFlow(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitFlow(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FlowContext flow() throws RecognitionException {
		FlowContext _localctx = new FlowContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_flow);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(82);
			header();
			setState(84); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(83);
				statement();
				}
				}
				setState(86); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << LOG) | (1L << FLOWCALL) | (1L << ACTIONCALL) | (1L << RRFCALL) | (1L << WHEN) | (1L << REPEAT) | (1L << ITERATE) | (1L << MATCH) | (1L << FINISH) | (1L << RFAC) | (1L << ALPHANUM) | (1L << QNAME) | (1L << DOTEXPR) | (1L << DOTIDXEXPR) | (1L << WS))) != 0) );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class HeaderContext extends ParserRuleContext {
		public TerminalNode FLOWSTART() { return getToken(AuthnFlowParser.FLOWSTART, 0); }
		public List<TerminalNode> WS() { return getTokens(AuthnFlowParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(AuthnFlowParser.WS, i);
		}
		public QnameContext qname() {
			return getRuleContext(QnameContext.class,0);
		}
		public TerminalNode INDENT() { return getToken(AuthnFlowParser.INDENT, 0); }
		public BaseContext base() {
			return getRuleContext(BaseContext.class,0);
		}
		public TerminalNode DEDENT() { return getToken(AuthnFlowParser.DEDENT, 0); }
		public InputsContext inputs() {
			return getRuleContext(InputsContext.class,0);
		}
		public List<TerminalNode> NL() { return getTokens(AuthnFlowParser.NL); }
		public TerminalNode NL(int i) {
			return getToken(AuthnFlowParser.NL, i);
		}
		public HeaderContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_header; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterHeader(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitHeader(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitHeader(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HeaderContext header() throws RecognitionException {
		HeaderContext _localctx = new HeaderContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_header);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(88);
			match(FLOWSTART);
			setState(89);
			match(WS);
			setState(90);
			qname();
			setState(92);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(91);
				match(WS);
				}
			}

			setState(94);
			match(INDENT);
			setState(95);
			base();
			setState(97);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==FLOWINPUTS) {
				{
				setState(96);
				inputs();
				}
			}

			setState(99);
			match(DEDENT);
			setState(103);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==NL) {
				{
				{
				setState(100);
				match(NL);
				}
				}
				setState(105);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class QnameContext extends ParserRuleContext {
		public TerminalNode ALPHANUM() { return getToken(AuthnFlowParser.ALPHANUM, 0); }
		public TerminalNode QNAME() { return getToken(AuthnFlowParser.QNAME, 0); }
		public QnameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qname; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterQname(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitQname(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitQname(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QnameContext qname() throws RecognitionException {
		QnameContext _localctx = new QnameContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_qname);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(106);
			_la = _input.LA(1);
			if ( !(_la==ALPHANUM || _la==QNAME) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BaseContext extends ParserRuleContext {
		public TerminalNode BASE() { return getToken(AuthnFlowParser.BASE, 0); }
		public List<TerminalNode> WS() { return getTokens(AuthnFlowParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(AuthnFlowParser.WS, i);
		}
		public TerminalNode STRING() { return getToken(AuthnFlowParser.STRING, 0); }
		public TerminalNode NL() { return getToken(AuthnFlowParser.NL, 0); }
		public BaseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_base; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterBase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitBase(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitBase(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BaseContext base() throws RecognitionException {
		BaseContext _localctx = new BaseContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_base);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(108);
			match(BASE);
			setState(109);
			match(WS);
			setState(110);
			match(STRING);
			setState(112);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(111);
				match(WS);
				}
			}

			setState(114);
			match(NL);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InputsContext extends ParserRuleContext {
		public TerminalNode FLOWINPUTS() { return getToken(AuthnFlowParser.FLOWINPUTS, 0); }
		public TerminalNode NL() { return getToken(AuthnFlowParser.NL, 0); }
		public List<TerminalNode> WS() { return getTokens(AuthnFlowParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(AuthnFlowParser.WS, i);
		}
		public List<Short_varContext> short_var() {
			return getRuleContexts(Short_varContext.class);
		}
		public Short_varContext short_var(int i) {
			return getRuleContext(Short_varContext.class,i);
		}
		public InputsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inputs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterInputs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitInputs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitInputs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InputsContext inputs() throws RecognitionException {
		InputsContext _localctx = new InputsContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_inputs);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(116);
			match(FLOWINPUTS);
			setState(119); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(117);
					match(WS);
					setState(118);
					short_var();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(121); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			setState(124);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(123);
				match(WS);
				}
			}

			setState(126);
			match(NL);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Short_varContext extends ParserRuleContext {
		public TerminalNode ALPHANUM() { return getToken(AuthnFlowParser.ALPHANUM, 0); }
		public Short_varContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_short_var; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterShort_var(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitShort_var(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitShort_var(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Short_varContext short_var() throws RecognitionException {
		Short_varContext _localctx = new Short_varContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_short_var);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(128);
			match(ALPHANUM);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class StatementContext extends ParserRuleContext {
		public Flow_callContext flow_call() {
			return getRuleContext(Flow_callContext.class,0);
		}
		public Action_callContext action_call() {
			return getRuleContext(Action_callContext.class,0);
		}
		public Rrf_callContext rrf_call() {
			return getRuleContext(Rrf_callContext.class,0);
		}
		public AssignmentContext assignment() {
			return getRuleContext(AssignmentContext.class,0);
		}
		public LogContext log() {
			return getRuleContext(LogContext.class,0);
		}
		public RfacContext rfac() {
			return getRuleContext(RfacContext.class,0);
		}
		public FinishContext finish() {
			return getRuleContext(FinishContext.class,0);
		}
		public IfelseContext ifelse() {
			return getRuleContext(IfelseContext.class,0);
		}
		public ChoiceContext choice() {
			return getRuleContext(ChoiceContext.class,0);
		}
		public LoopContext loop() {
			return getRuleContext(LoopContext.class,0);
		}
		public LoopyContext loopy() {
			return getRuleContext(LoopyContext.class,0);
		}
		public StatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StatementContext statement() throws RecognitionException {
		StatementContext _localctx = new StatementContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_statement);
		try {
			setState(141);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(130);
				flow_call();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(131);
				action_call();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(132);
				rrf_call();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(133);
				assignment();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(134);
				log();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(135);
				rfac();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(136);
				finish();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(137);
				ifelse();
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(138);
				choice();
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(139);
				loop();
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(140);
				loopy();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PreassignContext extends ParserRuleContext {
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public TerminalNode EQ() { return getToken(AuthnFlowParser.EQ, 0); }
		public List<TerminalNode> WS() { return getTokens(AuthnFlowParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(AuthnFlowParser.WS, i);
		}
		public PreassignContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_preassign; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterPreassign(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitPreassign(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitPreassign(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PreassignContext preassign() throws RecognitionException {
		PreassignContext _localctx = new PreassignContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_preassign);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(143);
			variable();
			setState(145);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(144);
				match(WS);
				}
			}

			setState(147);
			match(EQ);
			setState(149);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(148);
				match(WS);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Preassign_catchContext extends ParserRuleContext {
		public Short_varContext short_var() {
			return getRuleContext(Short_varContext.class,0);
		}
		public TerminalNode EQ() { return getToken(AuthnFlowParser.EQ, 0); }
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public List<TerminalNode> WS() { return getTokens(AuthnFlowParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(AuthnFlowParser.WS, i);
		}
		public Preassign_catchContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_preassign_catch; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterPreassign_catch(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitPreassign_catch(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitPreassign_catch(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Preassign_catchContext preassign_catch() throws RecognitionException {
		Preassign_catchContext _localctx = new Preassign_catchContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_preassign_catch);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(152);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ALPHANUM) | (1L << QNAME) | (1L << DOTEXPR) | (1L << DOTIDXEXPR))) != 0)) {
				{
				setState(151);
				variable();
				}
			}

			setState(155);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(154);
				match(WS);
				}
			}

			setState(157);
			match(T__0);
			setState(159);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(158);
				match(WS);
				}
			}

			setState(161);
			short_var();
			setState(163);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(162);
				match(WS);
				}
			}

			setState(165);
			match(EQ);
			setState(167);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(166);
				match(WS);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VariableContext extends ParserRuleContext {
		public Short_varContext short_var() {
			return getRuleContext(Short_varContext.class,0);
		}
		public TerminalNode QNAME() { return getToken(AuthnFlowParser.QNAME, 0); }
		public TerminalNode DOTEXPR() { return getToken(AuthnFlowParser.DOTEXPR, 0); }
		public TerminalNode DOTIDXEXPR() { return getToken(AuthnFlowParser.DOTIDXEXPR, 0); }
		public VariableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterVariable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitVariable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitVariable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableContext variable() throws RecognitionException {
		VariableContext _localctx = new VariableContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_variable);
		try {
			setState(173);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ALPHANUM:
				enterOuterAlt(_localctx, 1);
				{
				setState(169);
				short_var();
				}
				break;
			case QNAME:
				enterOuterAlt(_localctx, 2);
				{
				setState(170);
				match(QNAME);
				}
				break;
			case DOTEXPR:
				enterOuterAlt(_localctx, 3);
				{
				setState(171);
				match(DOTEXPR);
				}
				break;
			case DOTIDXEXPR:
				enterOuterAlt(_localctx, 4);
				{
				setState(172);
				match(DOTIDXEXPR);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Flow_callContext extends ParserRuleContext {
		public TerminalNode FLOWCALL() { return getToken(AuthnFlowParser.FLOWCALL, 0); }
		public List<TerminalNode> WS() { return getTokens(AuthnFlowParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(AuthnFlowParser.WS, i);
		}
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public QnameContext qname() {
			return getRuleContext(QnameContext.class,0);
		}
		public OverridesContext overrides() {
			return getRuleContext(OverridesContext.class,0);
		}
		public TerminalNode NL() { return getToken(AuthnFlowParser.NL, 0); }
		public PreassignContext preassign() {
			return getRuleContext(PreassignContext.class,0);
		}
		public List<ArgumentContext> argument() {
			return getRuleContexts(ArgumentContext.class);
		}
		public ArgumentContext argument(int i) {
			return getRuleContext(ArgumentContext.class,i);
		}
		public Flow_callContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_flow_call; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterFlow_call(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitFlow_call(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitFlow_call(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Flow_callContext flow_call() throws RecognitionException {
		Flow_callContext _localctx = new Flow_callContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_flow_call);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(176);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ALPHANUM) | (1L << QNAME) | (1L << DOTEXPR) | (1L << DOTIDXEXPR))) != 0)) {
				{
				setState(175);
				preassign();
				}
			}

			setState(178);
			match(FLOWCALL);
			setState(179);
			match(WS);
			setState(183);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__1:
				{
				setState(180);
				match(T__1);
				setState(181);
				variable();
				}
				break;
			case ALPHANUM:
			case QNAME:
				{
				setState(182);
				qname();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(188);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,18,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(185);
					argument();
					}
					} 
				}
				setState(190);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,18,_ctx);
			}
			setState(192);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(191);
				match(WS);
				}
			}

			setState(196);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case INDENT:
				{
				setState(194);
				overrides();
				}
				break;
			case NL:
				{
				setState(195);
				match(NL);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OverridesContext extends ParserRuleContext {
		public TerminalNode INDENT() { return getToken(AuthnFlowParser.INDENT, 0); }
		public TerminalNode OVERRIDE() { return getToken(AuthnFlowParser.OVERRIDE, 0); }
		public List<TerminalNode> WS() { return getTokens(AuthnFlowParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(AuthnFlowParser.WS, i);
		}
		public List<TerminalNode> STRING() { return getTokens(AuthnFlowParser.STRING); }
		public TerminalNode STRING(int i) {
			return getToken(AuthnFlowParser.STRING, i);
		}
		public TerminalNode NL() { return getToken(AuthnFlowParser.NL, 0); }
		public TerminalNode DEDENT() { return getToken(AuthnFlowParser.DEDENT, 0); }
		public OverridesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_overrides; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterOverrides(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitOverrides(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitOverrides(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OverridesContext overrides() throws RecognitionException {
		OverridesContext _localctx = new OverridesContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_overrides);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(198);
			match(INDENT);
			setState(199);
			match(OVERRIDE);
			setState(200);
			match(WS);
			setState(201);
			match(STRING);
			setState(206);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,21,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(202);
					match(WS);
					setState(203);
					match(STRING);
					}
					} 
				}
				setState(208);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,21,_ctx);
			}
			setState(210);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(209);
				match(WS);
				}
			}

			setState(212);
			match(NL);
			setState(213);
			match(DEDENT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Action_callContext extends ParserRuleContext {
		public TerminalNode ACTIONCALL() { return getToken(AuthnFlowParser.ACTIONCALL, 0); }
		public List<TerminalNode> WS() { return getTokens(AuthnFlowParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(AuthnFlowParser.WS, i);
		}
		public TerminalNode NL() { return getToken(AuthnFlowParser.NL, 0); }
		public Static_callContext static_call() {
			return getRuleContext(Static_callContext.class,0);
		}
		public Oo_callContext oo_call() {
			return getRuleContext(Oo_callContext.class,0);
		}
		public PreassignContext preassign() {
			return getRuleContext(PreassignContext.class,0);
		}
		public Preassign_catchContext preassign_catch() {
			return getRuleContext(Preassign_catchContext.class,0);
		}
		public Action_callContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_action_call; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterAction_call(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitAction_call(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitAction_call(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Action_callContext action_call() throws RecognitionException {
		Action_callContext _localctx = new Action_callContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_action_call);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(217);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,23,_ctx) ) {
			case 1:
				{
				setState(215);
				preassign();
				}
				break;
			case 2:
				{
				setState(216);
				preassign_catch();
				}
				break;
			}
			setState(219);
			match(ACTIONCALL);
			setState(220);
			match(WS);
			setState(223);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
			case 1:
				{
				setState(221);
				static_call();
				}
				break;
			case 2:
				{
				setState(222);
				oo_call();
				}
				break;
			}
			setState(226);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(225);
				match(WS);
				}
			}

			setState(228);
			match(NL);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Rrf_callContext extends ParserRuleContext {
		public TerminalNode RRFCALL() { return getToken(AuthnFlowParser.RRFCALL, 0); }
		public List<TerminalNode> WS() { return getTokens(AuthnFlowParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(AuthnFlowParser.WS, i);
		}
		public TerminalNode STRING() { return getToken(AuthnFlowParser.STRING, 0); }
		public Stchk_blockContext stchk_block() {
			return getRuleContext(Stchk_blockContext.class,0);
		}
		public TerminalNode NL() { return getToken(AuthnFlowParser.NL, 0); }
		public PreassignContext preassign() {
			return getRuleContext(PreassignContext.class,0);
		}
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public TerminalNode BOOL() { return getToken(AuthnFlowParser.BOOL, 0); }
		public Rrf_callContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rrf_call; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterRrf_call(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitRrf_call(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitRrf_call(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Rrf_callContext rrf_call() throws RecognitionException {
		Rrf_callContext _localctx = new Rrf_callContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_rrf_call);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(231);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ALPHANUM) | (1L << QNAME) | (1L << DOTEXPR) | (1L << DOTIDXEXPR))) != 0)) {
				{
				setState(230);
				preassign();
				}
			}

			setState(233);
			match(RRFCALL);
			setState(234);
			match(WS);
			setState(235);
			match(STRING);
			setState(238);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
			case 1:
				{
				setState(236);
				match(WS);
				setState(237);
				variable();
				}
				break;
			}
			setState(242);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,28,_ctx) ) {
			case 1:
				{
				setState(240);
				match(WS);
				setState(241);
				match(BOOL);
				}
				break;
			}
			setState(245);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(244);
				match(WS);
				}
			}

			setState(249);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case INDENT:
				{
				setState(247);
				stchk_block();
				}
				break;
			case NL:
				{
				setState(248);
				match(NL);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LogContext extends ParserRuleContext {
		public TerminalNode LOG() { return getToken(AuthnFlowParser.LOG, 0); }
		public TerminalNode NL() { return getToken(AuthnFlowParser.NL, 0); }
		public List<ArgumentContext> argument() {
			return getRuleContexts(ArgumentContext.class);
		}
		public ArgumentContext argument(int i) {
			return getRuleContext(ArgumentContext.class,i);
		}
		public TerminalNode WS() { return getToken(AuthnFlowParser.WS, 0); }
		public LogContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_log; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterLog(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitLog(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitLog(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LogContext log() throws RecognitionException {
		LogContext _localctx = new LogContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_log);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(251);
			match(LOG);
			setState(253); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(252);
					argument();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(255); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,31,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			setState(258);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(257);
				match(WS);
				}
			}

			setState(260);
			match(NL);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Static_callContext extends ParserRuleContext {
		public QnameContext qname() {
			return getRuleContext(QnameContext.class,0);
		}
		public TerminalNode ALPHANUM() { return getToken(AuthnFlowParser.ALPHANUM, 0); }
		public List<ArgumentContext> argument() {
			return getRuleContexts(ArgumentContext.class);
		}
		public ArgumentContext argument(int i) {
			return getRuleContext(ArgumentContext.class,i);
		}
		public Static_callContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_static_call; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterStatic_call(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitStatic_call(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitStatic_call(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Static_callContext static_call() throws RecognitionException {
		Static_callContext _localctx = new Static_callContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_static_call);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(262);
			qname();
			setState(263);
			match(T__2);
			setState(264);
			match(ALPHANUM);
			setState(268);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,33,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(265);
					argument();
					}
					} 
				}
				setState(270);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,33,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Oo_callContext extends ParserRuleContext {
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public TerminalNode WS() { return getToken(AuthnFlowParser.WS, 0); }
		public TerminalNode ALPHANUM() { return getToken(AuthnFlowParser.ALPHANUM, 0); }
		public List<ArgumentContext> argument() {
			return getRuleContexts(ArgumentContext.class);
		}
		public ArgumentContext argument(int i) {
			return getRuleContext(ArgumentContext.class,i);
		}
		public Oo_callContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_oo_call; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterOo_call(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitOo_call(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitOo_call(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Oo_callContext oo_call() throws RecognitionException {
		Oo_callContext _localctx = new Oo_callContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_oo_call);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(271);
			variable();
			setState(272);
			match(WS);
			setState(273);
			match(ALPHANUM);
			setState(277);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,34,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(274);
					argument();
					}
					} 
				}
				setState(279);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,34,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArgumentContext extends ParserRuleContext {
		public TerminalNode WS() { return getToken(AuthnFlowParser.WS, 0); }
		public Simple_exprContext simple_expr() {
			return getRuleContext(Simple_exprContext.class,0);
		}
		public ArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argument; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterArgument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitArgument(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitArgument(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgumentContext argument() throws RecognitionException {
		ArgumentContext _localctx = new ArgumentContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_argument);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(280);
			match(WS);
			setState(281);
			simple_expr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Simple_exprContext extends ParserRuleContext {
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public TerminalNode MINUS() { return getToken(AuthnFlowParser.MINUS, 0); }
		public Simple_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_simple_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterSimple_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitSimple_expr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitSimple_expr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Simple_exprContext simple_expr() throws RecognitionException {
		Simple_exprContext _localctx = new Simple_exprContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_simple_expr);
		try {
			setState(287);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NUL:
			case BOOL:
			case STRING:
			case UINT:
			case SINT:
			case DECIMAL:
				enterOuterAlt(_localctx, 1);
				{
				setState(283);
				literal();
				}
				break;
			case ALPHANUM:
			case QNAME:
			case DOTEXPR:
			case DOTIDXEXPR:
				enterOuterAlt(_localctx, 2);
				{
				setState(284);
				variable();
				}
				break;
			case MINUS:
				enterOuterAlt(_localctx, 3);
				{
				{
				setState(285);
				match(MINUS);
				setState(286);
				variable();
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LiteralContext extends ParserRuleContext {
		public TerminalNode BOOL() { return getToken(AuthnFlowParser.BOOL, 0); }
		public TerminalNode STRING() { return getToken(AuthnFlowParser.STRING, 0); }
		public TerminalNode UINT() { return getToken(AuthnFlowParser.UINT, 0); }
		public TerminalNode SINT() { return getToken(AuthnFlowParser.SINT, 0); }
		public TerminalNode DECIMAL() { return getToken(AuthnFlowParser.DECIMAL, 0); }
		public TerminalNode NUL() { return getToken(AuthnFlowParser.NUL, 0); }
		public LiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LiteralContext literal() throws RecognitionException {
		LiteralContext _localctx = new LiteralContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(289);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << NUL) | (1L << BOOL) | (1L << STRING) | (1L << UINT) | (1L << SINT) | (1L << DECIMAL))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExpressionContext extends ParserRuleContext {
		public Object_exprContext object_expr() {
			return getRuleContext(Object_exprContext.class,0);
		}
		public Array_exprContext array_expr() {
			return getRuleContext(Array_exprContext.class,0);
		}
		public Bool_exprContext bool_expr() {
			return getRuleContext(Bool_exprContext.class,0);
		}
		public Simple_exprContext simple_expr() {
			return getRuleContext(Simple_exprContext.class,0);
		}
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionContext expression() throws RecognitionException {
		ExpressionContext _localctx = new ExpressionContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_expression);
		try {
			setState(295);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,36,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(291);
				object_expr();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(292);
				array_expr();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(293);
				bool_expr();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(294);
				simple_expr();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Array_exprContext extends ParserRuleContext {
		public List<TerminalNode> WS() { return getTokens(AuthnFlowParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(AuthnFlowParser.WS, i);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<TerminalNode> SPCOMMA() { return getTokens(AuthnFlowParser.SPCOMMA); }
		public TerminalNode SPCOMMA(int i) {
			return getToken(AuthnFlowParser.SPCOMMA, i);
		}
		public Array_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_array_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterArray_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitArray_expr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitArray_expr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Array_exprContext array_expr() throws RecognitionException {
		Array_exprContext _localctx = new Array_exprContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_array_expr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(297);
			match(T__3);
			setState(299);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,37,_ctx) ) {
			case 1:
				{
				setState(298);
				match(WS);
				}
				break;
			}
			setState(304);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__3) | (1L << T__5) | (1L << MINUS) | (1L << NUL) | (1L << BOOL) | (1L << STRING) | (1L << UINT) | (1L << SINT) | (1L << DECIMAL) | (1L << ALPHANUM) | (1L << QNAME) | (1L << DOTEXPR) | (1L << DOTIDXEXPR))) != 0)) {
				{
				{
				setState(301);
				expression();
				}
				}
				setState(306);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(311);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==SPCOMMA) {
				{
				{
				setState(307);
				match(SPCOMMA);
				setState(308);
				expression();
				}
				}
				setState(313);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(315);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(314);
				match(WS);
				}
			}

			setState(317);
			match(T__4);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Object_exprContext extends ParserRuleContext {
		public List<TerminalNode> WS() { return getTokens(AuthnFlowParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(AuthnFlowParser.WS, i);
		}
		public List<KeypairContext> keypair() {
			return getRuleContexts(KeypairContext.class);
		}
		public KeypairContext keypair(int i) {
			return getRuleContext(KeypairContext.class,i);
		}
		public List<TerminalNode> SPCOMMA() { return getTokens(AuthnFlowParser.SPCOMMA); }
		public TerminalNode SPCOMMA(int i) {
			return getToken(AuthnFlowParser.SPCOMMA, i);
		}
		public Object_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_object_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterObject_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitObject_expr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitObject_expr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Object_exprContext object_expr() throws RecognitionException {
		Object_exprContext _localctx = new Object_exprContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_object_expr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(319);
			match(T__5);
			setState(321);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,41,_ctx) ) {
			case 1:
				{
				setState(320);
				match(WS);
				}
				break;
			}
			setState(326);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ALPHANUM) {
				{
				{
				setState(323);
				keypair();
				}
				}
				setState(328);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(333);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==SPCOMMA) {
				{
				{
				setState(329);
				match(SPCOMMA);
				setState(330);
				keypair();
				}
				}
				setState(335);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(337);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(336);
				match(WS);
				}
			}

			setState(339);
			match(T__6);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AssignmentContext extends ParserRuleContext {
		public PreassignContext preassign() {
			return getRuleContext(PreassignContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode NL() { return getToken(AuthnFlowParser.NL, 0); }
		public TerminalNode WS() { return getToken(AuthnFlowParser.WS, 0); }
		public AssignmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assignment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterAssignment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitAssignment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitAssignment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AssignmentContext assignment() throws RecognitionException {
		AssignmentContext _localctx = new AssignmentContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_assignment);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(341);
			preassign();
			setState(342);
			expression();
			setState(344);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(343);
				match(WS);
				}
			}

			setState(346);
			match(NL);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class KeypairContext extends ParserRuleContext {
		public TerminalNode ALPHANUM() { return getToken(AuthnFlowParser.ALPHANUM, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public List<TerminalNode> WS() { return getTokens(AuthnFlowParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(AuthnFlowParser.WS, i);
		}
		public KeypairContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_keypair; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterKeypair(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitKeypair(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitKeypair(this);
			else return visitor.visitChildren(this);
		}
	}

	public final KeypairContext keypair() throws RecognitionException {
		KeypairContext _localctx = new KeypairContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_keypair);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(348);
			match(ALPHANUM);
			setState(350);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(349);
				match(WS);
				}
			}

			setState(352);
			match(T__7);
			setState(354);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(353);
				match(WS);
				}
			}

			setState(356);
			expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RfacContext extends ParserRuleContext {
		public TerminalNode RFAC() { return getToken(AuthnFlowParser.RFAC, 0); }
		public TerminalNode WS() { return getToken(AuthnFlowParser.WS, 0); }
		public TerminalNode NL() { return getToken(AuthnFlowParser.NL, 0); }
		public TerminalNode STRING() { return getToken(AuthnFlowParser.STRING, 0); }
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public PreassignContext preassign() {
			return getRuleContext(PreassignContext.class,0);
		}
		public RfacContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rfac; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterRfac(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitRfac(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitRfac(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RfacContext rfac() throws RecognitionException {
		RfacContext _localctx = new RfacContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_rfac);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(359);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ALPHANUM) | (1L << QNAME) | (1L << DOTEXPR) | (1L << DOTIDXEXPR))) != 0)) {
				{
				setState(358);
				preassign();
				}
			}

			setState(361);
			match(RFAC);
			setState(362);
			match(WS);
			setState(365);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STRING:
				{
				setState(363);
				match(STRING);
				}
				break;
			case ALPHANUM:
			case QNAME:
			case DOTEXPR:
			case DOTIDXEXPR:
				{
				setState(364);
				variable();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(367);
			match(NL);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FinishContext extends ParserRuleContext {
		public TerminalNode FINISH() { return getToken(AuthnFlowParser.FINISH, 0); }
		public List<TerminalNode> WS() { return getTokens(AuthnFlowParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(AuthnFlowParser.WS, i);
		}
		public TerminalNode NL() { return getToken(AuthnFlowParser.NL, 0); }
		public TerminalNode BOOL() { return getToken(AuthnFlowParser.BOOL, 0); }
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public FinishContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_finish; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterFinish(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitFinish(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitFinish(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FinishContext finish() throws RecognitionException {
		FinishContext _localctx = new FinishContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_finish);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(369);
			match(FINISH);
			setState(370);
			match(WS);
			setState(373);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case BOOL:
				{
				setState(371);
				match(BOOL);
				}
				break;
			case ALPHANUM:
			case QNAME:
			case DOTEXPR:
			case DOTIDXEXPR:
				{
				setState(372);
				variable();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(376);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(375);
				match(WS);
				}
			}

			setState(378);
			match(NL);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ChoiceContext extends ParserRuleContext {
		public TerminalNode MATCH() { return getToken(AuthnFlowParser.MATCH, 0); }
		public List<TerminalNode> WS() { return getTokens(AuthnFlowParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(AuthnFlowParser.WS, i);
		}
		public Simple_exprContext simple_expr() {
			return getRuleContext(Simple_exprContext.class,0);
		}
		public TerminalNode TO() { return getToken(AuthnFlowParser.TO, 0); }
		public TerminalNode INDENT() { return getToken(AuthnFlowParser.INDENT, 0); }
		public TerminalNode DEDENT() { return getToken(AuthnFlowParser.DEDENT, 0); }
		public List<OptionContext> option() {
			return getRuleContexts(OptionContext.class);
		}
		public OptionContext option(int i) {
			return getRuleContext(OptionContext.class,i);
		}
		public ElseblockContext elseblock() {
			return getRuleContext(ElseblockContext.class,0);
		}
		public ChoiceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_choice; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterChoice(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitChoice(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitChoice(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ChoiceContext choice() throws RecognitionException {
		ChoiceContext _localctx = new ChoiceContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_choice);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(380);
			match(MATCH);
			setState(381);
			match(WS);
			setState(382);
			simple_expr();
			setState(383);
			match(WS);
			setState(384);
			match(TO);
			setState(386);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(385);
				match(WS);
				}
			}

			setState(388);
			match(INDENT);
			setState(390); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(389);
				option();
				}
				}
				setState(392); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << MINUS) | (1L << NUL) | (1L << BOOL) | (1L << STRING) | (1L << UINT) | (1L << SINT) | (1L << DECIMAL) | (1L << ALPHANUM) | (1L << QNAME) | (1L << DOTEXPR) | (1L << DOTIDXEXPR))) != 0) );
			setState(394);
			match(DEDENT);
			setState(396);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OTHERWISE) {
				{
				setState(395);
				elseblock();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OptionContext extends ParserRuleContext {
		public Simple_exprContext simple_expr() {
			return getRuleContext(Simple_exprContext.class,0);
		}
		public TerminalNode INDENT() { return getToken(AuthnFlowParser.INDENT, 0); }
		public TerminalNode DEDENT() { return getToken(AuthnFlowParser.DEDENT, 0); }
		public TerminalNode WS() { return getToken(AuthnFlowParser.WS, 0); }
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public OptionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_option; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterOption(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitOption(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitOption(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OptionContext option() throws RecognitionException {
		OptionContext _localctx = new OptionContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_option);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(398);
			simple_expr();
			setState(400);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(399);
				match(WS);
				}
			}

			setState(402);
			match(INDENT);
			setState(404); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(403);
				statement();
				}
				}
				setState(406); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << LOG) | (1L << FLOWCALL) | (1L << ACTIONCALL) | (1L << RRFCALL) | (1L << WHEN) | (1L << REPEAT) | (1L << ITERATE) | (1L << MATCH) | (1L << FINISH) | (1L << RFAC) | (1L << ALPHANUM) | (1L << QNAME) | (1L << DOTEXPR) | (1L << DOTIDXEXPR) | (1L << WS))) != 0) );
			setState(408);
			match(DEDENT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IfelseContext extends ParserRuleContext {
		public CaseofContext caseof() {
			return getRuleContext(CaseofContext.class,0);
		}
		public TerminalNode INDENT() { return getToken(AuthnFlowParser.INDENT, 0); }
		public TerminalNode DEDENT() { return getToken(AuthnFlowParser.DEDENT, 0); }
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public ElseblockContext elseblock() {
			return getRuleContext(ElseblockContext.class,0);
		}
		public IfelseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ifelse; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterIfelse(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitIfelse(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitIfelse(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IfelseContext ifelse() throws RecognitionException {
		IfelseContext _localctx = new IfelseContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_ifelse);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(410);
			caseof();
			setState(411);
			match(INDENT);
			setState(413); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(412);
				statement();
				}
				}
				setState(415); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << LOG) | (1L << FLOWCALL) | (1L << ACTIONCALL) | (1L << RRFCALL) | (1L << WHEN) | (1L << REPEAT) | (1L << ITERATE) | (1L << MATCH) | (1L << FINISH) | (1L << RFAC) | (1L << ALPHANUM) | (1L << QNAME) | (1L << DOTEXPR) | (1L << DOTIDXEXPR) | (1L << WS))) != 0) );
			setState(417);
			match(DEDENT);
			setState(419);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OTHERWISE) {
				{
				setState(418);
				elseblock();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CaseofContext extends ParserRuleContext {
		public TerminalNode WHEN() { return getToken(AuthnFlowParser.WHEN, 0); }
		public TerminalNode WS() { return getToken(AuthnFlowParser.WS, 0); }
		public Bool_exprContext bool_expr() {
			return getRuleContext(Bool_exprContext.class,0);
		}
		public CaseofContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_caseof; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterCaseof(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitCaseof(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitCaseof(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CaseofContext caseof() throws RecognitionException {
		CaseofContext _localctx = new CaseofContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_caseof);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(421);
			match(WHEN);
			setState(422);
			match(WS);
			setState(423);
			bool_expr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Bool_exprContext extends ParserRuleContext {
		public Boolean_exprContext boolean_expr() {
			return getRuleContext(Boolean_exprContext.class,0);
		}
		public List<Boolean_op_exprContext> boolean_op_expr() {
			return getRuleContexts(Boolean_op_exprContext.class);
		}
		public Boolean_op_exprContext boolean_op_expr(int i) {
			return getRuleContext(Boolean_op_exprContext.class,i);
		}
		public Bool_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_bool_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterBool_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitBool_expr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitBool_expr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Bool_exprContext bool_expr() throws RecognitionException {
		Bool_exprContext _localctx = new Bool_exprContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_bool_expr);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(425);
			boolean_expr();
			setState(429);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,59,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(426);
					boolean_op_expr();
					}
					} 
				}
				setState(431);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,59,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Boolean_op_exprContext extends ParserRuleContext {
		public Boolean_exprContext boolean_expr() {
			return getRuleContext(Boolean_exprContext.class,0);
		}
		public TerminalNode AND() { return getToken(AuthnFlowParser.AND, 0); }
		public TerminalNode OR() { return getToken(AuthnFlowParser.OR, 0); }
		public List<TerminalNode> NL() { return getTokens(AuthnFlowParser.NL); }
		public TerminalNode NL(int i) {
			return getToken(AuthnFlowParser.NL, i);
		}
		public TerminalNode WS() { return getToken(AuthnFlowParser.WS, 0); }
		public Boolean_op_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_boolean_op_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterBoolean_op_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitBoolean_op_expr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitBoolean_op_expr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Boolean_op_exprContext boolean_op_expr() throws RecognitionException {
		Boolean_op_exprContext _localctx = new Boolean_op_exprContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_boolean_op_expr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(435);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==NL) {
				{
				{
				setState(432);
				match(NL);
				}
				}
				setState(437);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(438);
			_la = _input.LA(1);
			if ( !(_la==AND || _la==OR) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(440);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(439);
				match(WS);
				}
			}

			setState(445);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==NL) {
				{
				{
				setState(442);
				match(NL);
				}
				}
				setState(447);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(448);
			boolean_expr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Boolean_exprContext extends ParserRuleContext {
		public List<Simple_exprContext> simple_expr() {
			return getRuleContexts(Simple_exprContext.class);
		}
		public Simple_exprContext simple_expr(int i) {
			return getRuleContext(Simple_exprContext.class,i);
		}
		public List<TerminalNode> WS() { return getTokens(AuthnFlowParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(AuthnFlowParser.WS, i);
		}
		public TerminalNode IS() { return getToken(AuthnFlowParser.IS, 0); }
		public TerminalNode NOT() { return getToken(AuthnFlowParser.NOT, 0); }
		public Boolean_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_boolean_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterBoolean_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitBoolean_expr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitBoolean_expr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Boolean_exprContext boolean_expr() throws RecognitionException {
		Boolean_exprContext _localctx = new Boolean_exprContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_boolean_expr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(450);
			simple_expr();
			setState(451);
			match(WS);
			setState(452);
			match(IS);
			setState(453);
			match(WS);
			setState(456);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==NOT) {
				{
				setState(454);
				match(NOT);
				setState(455);
				match(WS);
				}
			}

			setState(458);
			simple_expr();
			setState(460);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,64,_ctx) ) {
			case 1:
				{
				setState(459);
				match(WS);
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ElseblockContext extends ParserRuleContext {
		public TerminalNode OTHERWISE() { return getToken(AuthnFlowParser.OTHERWISE, 0); }
		public TerminalNode INDENT() { return getToken(AuthnFlowParser.INDENT, 0); }
		public TerminalNode DEDENT() { return getToken(AuthnFlowParser.DEDENT, 0); }
		public TerminalNode WS() { return getToken(AuthnFlowParser.WS, 0); }
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public ElseblockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_elseblock; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterElseblock(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitElseblock(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitElseblock(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ElseblockContext elseblock() throws RecognitionException {
		ElseblockContext _localctx = new ElseblockContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_elseblock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(462);
			match(OTHERWISE);
			setState(464);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(463);
				match(WS);
				}
			}

			setState(466);
			match(INDENT);
			setState(468); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(467);
				statement();
				}
				}
				setState(470); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << LOG) | (1L << FLOWCALL) | (1L << ACTIONCALL) | (1L << RRFCALL) | (1L << WHEN) | (1L << REPEAT) | (1L << ITERATE) | (1L << MATCH) | (1L << FINISH) | (1L << RFAC) | (1L << ALPHANUM) | (1L << QNAME) | (1L << DOTEXPR) | (1L << DOTIDXEXPR) | (1L << WS))) != 0) );
			setState(472);
			match(DEDENT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LoopContext extends ParserRuleContext {
		public TerminalNode ITERATE() { return getToken(AuthnFlowParser.ITERATE, 0); }
		public List<TerminalNode> WS() { return getTokens(AuthnFlowParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(AuthnFlowParser.WS, i);
		}
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public TerminalNode USE() { return getToken(AuthnFlowParser.USE, 0); }
		public Short_varContext short_var() {
			return getRuleContext(Short_varContext.class,0);
		}
		public TerminalNode INDENT() { return getToken(AuthnFlowParser.INDENT, 0); }
		public TerminalNode DEDENT() { return getToken(AuthnFlowParser.DEDENT, 0); }
		public PreassignContext preassign() {
			return getRuleContext(PreassignContext.class,0);
		}
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public Quit_stmtContext quit_stmt() {
			return getRuleContext(Quit_stmtContext.class,0);
		}
		public LoopContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_loop; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterLoop(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitLoop(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitLoop(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LoopContext loop() throws RecognitionException {
		LoopContext _localctx = new LoopContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_loop);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(475);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ALPHANUM) | (1L << QNAME) | (1L << DOTEXPR) | (1L << DOTIDXEXPR))) != 0)) {
				{
				setState(474);
				preassign();
				}
			}

			setState(477);
			match(ITERATE);
			setState(478);
			match(WS);
			setState(479);
			variable();
			setState(480);
			match(WS);
			setState(481);
			match(USE);
			setState(482);
			match(WS);
			setState(483);
			short_var();
			setState(484);
			match(INDENT);
			setState(486); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(485);
				statement();
				}
				}
				setState(488); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << LOG) | (1L << FLOWCALL) | (1L << ACTIONCALL) | (1L << RRFCALL) | (1L << WHEN) | (1L << REPEAT) | (1L << ITERATE) | (1L << MATCH) | (1L << FINISH) | (1L << RFAC) | (1L << ALPHANUM) | (1L << QNAME) | (1L << DOTEXPR) | (1L << DOTIDXEXPR) | (1L << WS))) != 0) );
			setState(491);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==QUIT) {
				{
				setState(490);
				quit_stmt();
				}
			}

			setState(493);
			match(DEDENT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LoopyContext extends ParserRuleContext {
		public TerminalNode REPEAT() { return getToken(AuthnFlowParser.REPEAT, 0); }
		public List<TerminalNode> WS() { return getTokens(AuthnFlowParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(AuthnFlowParser.WS, i);
		}
		public TerminalNode MAXTIMES() { return getToken(AuthnFlowParser.MAXTIMES, 0); }
		public TerminalNode INDENT() { return getToken(AuthnFlowParser.INDENT, 0); }
		public TerminalNode DEDENT() { return getToken(AuthnFlowParser.DEDENT, 0); }
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public TerminalNode UINT() { return getToken(AuthnFlowParser.UINT, 0); }
		public PreassignContext preassign() {
			return getRuleContext(PreassignContext.class,0);
		}
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public Quit_stmtContext quit_stmt() {
			return getRuleContext(Quit_stmtContext.class,0);
		}
		public LoopyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_loopy; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterLoopy(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitLoopy(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitLoopy(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LoopyContext loopy() throws RecognitionException {
		LoopyContext _localctx = new LoopyContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_loopy);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(496);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ALPHANUM) | (1L << QNAME) | (1L << DOTEXPR) | (1L << DOTIDXEXPR))) != 0)) {
				{
				setState(495);
				preassign();
				}
			}

			setState(498);
			match(REPEAT);
			setState(499);
			match(WS);
			setState(502);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ALPHANUM:
			case QNAME:
			case DOTEXPR:
			case DOTIDXEXPR:
				{
				setState(500);
				variable();
				}
				break;
			case UINT:
				{
				setState(501);
				match(UINT);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(504);
			match(WS);
			setState(505);
			match(MAXTIMES);
			setState(507);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(506);
				match(WS);
				}
			}

			setState(509);
			match(INDENT);
			setState(511); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(510);
				statement();
				}
				}
				setState(513); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << LOG) | (1L << FLOWCALL) | (1L << ACTIONCALL) | (1L << RRFCALL) | (1L << WHEN) | (1L << REPEAT) | (1L << ITERATE) | (1L << MATCH) | (1L << FINISH) | (1L << RFAC) | (1L << ALPHANUM) | (1L << QNAME) | (1L << DOTEXPR) | (1L << DOTIDXEXPR) | (1L << WS))) != 0) );
			setState(516);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==QUIT) {
				{
				setState(515);
				quit_stmt();
				}
			}

			setState(518);
			match(DEDENT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Quit_stmtContext extends ParserRuleContext {
		public TerminalNode QUIT() { return getToken(AuthnFlowParser.QUIT, 0); }
		public TerminalNode WS() { return getToken(AuthnFlowParser.WS, 0); }
		public CaseofContext caseof() {
			return getRuleContext(CaseofContext.class,0);
		}
		public TerminalNode NL() { return getToken(AuthnFlowParser.NL, 0); }
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public Quit_stmtContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_quit_stmt; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterQuit_stmt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitQuit_stmt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitQuit_stmt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Quit_stmtContext quit_stmt() throws RecognitionException {
		Quit_stmtContext _localctx = new Quit_stmtContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_quit_stmt);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(520);
			match(QUIT);
			setState(521);
			match(WS);
			setState(522);
			caseof();
			setState(523);
			match(NL);
			setState(527);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << LOG) | (1L << FLOWCALL) | (1L << ACTIONCALL) | (1L << RRFCALL) | (1L << WHEN) | (1L << REPEAT) | (1L << ITERATE) | (1L << MATCH) | (1L << FINISH) | (1L << RFAC) | (1L << ALPHANUM) | (1L << QNAME) | (1L << DOTEXPR) | (1L << DOTIDXEXPR) | (1L << WS))) != 0)) {
				{
				{
				setState(524);
				statement();
				}
				}
				setState(529);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Stchk_blockContext extends ParserRuleContext {
		public List<TerminalNode> INDENT() { return getTokens(AuthnFlowParser.INDENT); }
		public TerminalNode INDENT(int i) {
			return getToken(AuthnFlowParser.INDENT, i);
		}
		public TerminalNode STATUS_CHK() { return getToken(AuthnFlowParser.STATUS_CHK, 0); }
		public Stchk_openContext stchk_open() {
			return getRuleContext(Stchk_openContext.class,0);
		}
		public Stchk_closeContext stchk_close() {
			return getRuleContext(Stchk_closeContext.class,0);
		}
		public List<TerminalNode> DEDENT() { return getTokens(AuthnFlowParser.DEDENT); }
		public TerminalNode DEDENT(int i) {
			return getToken(AuthnFlowParser.DEDENT, i);
		}
		public TerminalNode WS() { return getToken(AuthnFlowParser.WS, 0); }
		public Action_callContext action_call() {
			return getRuleContext(Action_callContext.class,0);
		}
		public Stchk_blockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stchk_block; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterStchk_block(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitStchk_block(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitStchk_block(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Stchk_blockContext stchk_block() throws RecognitionException {
		Stchk_blockContext _localctx = new Stchk_blockContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_stchk_block);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(530);
			match(INDENT);
			setState(531);
			match(STATUS_CHK);
			setState(533);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(532);
				match(WS);
				}
			}

			setState(535);
			match(INDENT);
			setState(536);
			stchk_open();
			setState(538);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << ACTIONCALL) | (1L << ALPHANUM) | (1L << QNAME) | (1L << DOTEXPR) | (1L << DOTIDXEXPR) | (1L << WS))) != 0)) {
				{
				setState(537);
				action_call();
				}
			}

			setState(540);
			stchk_close();
			setState(541);
			match(DEDENT);
			setState(542);
			match(DEDENT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Stchk_openContext extends ParserRuleContext {
		public TerminalNode OPEN() { return getToken(AuthnFlowParser.OPEN, 0); }
		public List<TerminalNode> WS() { return getTokens(AuthnFlowParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(AuthnFlowParser.WS, i);
		}
		public TerminalNode SECS() { return getToken(AuthnFlowParser.SECS, 0); }
		public TerminalNode NL() { return getToken(AuthnFlowParser.NL, 0); }
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public TerminalNode UINT() { return getToken(AuthnFlowParser.UINT, 0); }
		public Stchk_openContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stchk_open; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterStchk_open(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitStchk_open(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitStchk_open(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Stchk_openContext stchk_open() throws RecognitionException {
		Stchk_openContext _localctx = new Stchk_openContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_stchk_open);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(544);
			match(OPEN);
			setState(545);
			match(WS);
			setState(548);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ALPHANUM:
			case QNAME:
			case DOTEXPR:
			case DOTIDXEXPR:
				{
				setState(546);
				variable();
				}
				break;
			case UINT:
				{
				setState(547);
				match(UINT);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(550);
			match(WS);
			setState(551);
			match(SECS);
			setState(553);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(552);
				match(WS);
				}
			}

			setState(555);
			match(NL);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Stchk_closeContext extends ParserRuleContext {
		public TerminalNode CLOSE() { return getToken(AuthnFlowParser.CLOSE, 0); }
		public List<TerminalNode> WS() { return getTokens(AuthnFlowParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(AuthnFlowParser.WS, i);
		}
		public CaseofContext caseof() {
			return getRuleContext(CaseofContext.class,0);
		}
		public TerminalNode NL() { return getToken(AuthnFlowParser.NL, 0); }
		public Stchk_closeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stchk_close; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).enterStchk_close(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AuthnFlowListener ) ((AuthnFlowListener)listener).exitStchk_close(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AuthnFlowVisitor ) return ((AuthnFlowVisitor<? extends T>)visitor).visitStchk_close(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Stchk_closeContext stchk_close() throws RecognitionException {
		Stchk_closeContext _localctx = new Stchk_closeContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_stchk_close);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(557);
			match(CLOSE);
			setState(558);
			match(WS);
			setState(559);
			caseof();
			setState(561);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WS) {
				{
				setState(560);
				match(WS);
				}
			}

			setState(563);
			match(NL);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\38\u0238\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\3\2\3\2"+
		"\6\2W\n\2\r\2\16\2X\3\3\3\3\3\3\3\3\5\3_\n\3\3\3\3\3\3\3\5\3d\n\3\3\3"+
		"\3\3\7\3h\n\3\f\3\16\3k\13\3\3\4\3\4\3\5\3\5\3\5\3\5\5\5s\n\5\3\5\3\5"+
		"\3\6\3\6\3\6\6\6z\n\6\r\6\16\6{\3\6\5\6\177\n\6\3\6\3\6\3\7\3\7\3\b\3"+
		"\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\5\b\u0090\n\b\3\t\3\t\5\t\u0094"+
		"\n\t\3\t\3\t\5\t\u0098\n\t\3\n\5\n\u009b\n\n\3\n\5\n\u009e\n\n\3\n\3\n"+
		"\5\n\u00a2\n\n\3\n\3\n\5\n\u00a6\n\n\3\n\3\n\5\n\u00aa\n\n\3\13\3\13\3"+
		"\13\3\13\5\13\u00b0\n\13\3\f\5\f\u00b3\n\f\3\f\3\f\3\f\3\f\3\f\5\f\u00ba"+
		"\n\f\3\f\7\f\u00bd\n\f\f\f\16\f\u00c0\13\f\3\f\5\f\u00c3\n\f\3\f\3\f\5"+
		"\f\u00c7\n\f\3\r\3\r\3\r\3\r\3\r\3\r\7\r\u00cf\n\r\f\r\16\r\u00d2\13\r"+
		"\3\r\5\r\u00d5\n\r\3\r\3\r\3\r\3\16\3\16\5\16\u00dc\n\16\3\16\3\16\3\16"+
		"\3\16\5\16\u00e2\n\16\3\16\5\16\u00e5\n\16\3\16\3\16\3\17\5\17\u00ea\n"+
		"\17\3\17\3\17\3\17\3\17\3\17\5\17\u00f1\n\17\3\17\3\17\5\17\u00f5\n\17"+
		"\3\17\5\17\u00f8\n\17\3\17\3\17\5\17\u00fc\n\17\3\20\3\20\6\20\u0100\n"+
		"\20\r\20\16\20\u0101\3\20\5\20\u0105\n\20\3\20\3\20\3\21\3\21\3\21\3\21"+
		"\7\21\u010d\n\21\f\21\16\21\u0110\13\21\3\22\3\22\3\22\3\22\7\22\u0116"+
		"\n\22\f\22\16\22\u0119\13\22\3\23\3\23\3\23\3\24\3\24\3\24\3\24\5\24\u0122"+
		"\n\24\3\25\3\25\3\26\3\26\3\26\3\26\5\26\u012a\n\26\3\27\3\27\5\27\u012e"+
		"\n\27\3\27\7\27\u0131\n\27\f\27\16\27\u0134\13\27\3\27\3\27\7\27\u0138"+
		"\n\27\f\27\16\27\u013b\13\27\3\27\5\27\u013e\n\27\3\27\3\27\3\30\3\30"+
		"\5\30\u0144\n\30\3\30\7\30\u0147\n\30\f\30\16\30\u014a\13\30\3\30\3\30"+
		"\7\30\u014e\n\30\f\30\16\30\u0151\13\30\3\30\5\30\u0154\n\30\3\30\3\30"+
		"\3\31\3\31\3\31\5\31\u015b\n\31\3\31\3\31\3\32\3\32\5\32\u0161\n\32\3"+
		"\32\3\32\5\32\u0165\n\32\3\32\3\32\3\33\5\33\u016a\n\33\3\33\3\33\3\33"+
		"\3\33\5\33\u0170\n\33\3\33\3\33\3\34\3\34\3\34\3\34\5\34\u0178\n\34\3"+
		"\34\5\34\u017b\n\34\3\34\3\34\3\35\3\35\3\35\3\35\3\35\3\35\5\35\u0185"+
		"\n\35\3\35\3\35\6\35\u0189\n\35\r\35\16\35\u018a\3\35\3\35\5\35\u018f"+
		"\n\35\3\36\3\36\5\36\u0193\n\36\3\36\3\36\6\36\u0197\n\36\r\36\16\36\u0198"+
		"\3\36\3\36\3\37\3\37\3\37\6\37\u01a0\n\37\r\37\16\37\u01a1\3\37\3\37\5"+
		"\37\u01a6\n\37\3 \3 \3 \3 \3!\3!\7!\u01ae\n!\f!\16!\u01b1\13!\3\"\7\""+
		"\u01b4\n\"\f\"\16\"\u01b7\13\"\3\"\3\"\5\"\u01bb\n\"\3\"\7\"\u01be\n\""+
		"\f\"\16\"\u01c1\13\"\3\"\3\"\3#\3#\3#\3#\3#\3#\5#\u01cb\n#\3#\3#\5#\u01cf"+
		"\n#\3$\3$\5$\u01d3\n$\3$\3$\6$\u01d7\n$\r$\16$\u01d8\3$\3$\3%\5%\u01de"+
		"\n%\3%\3%\3%\3%\3%\3%\3%\3%\3%\6%\u01e9\n%\r%\16%\u01ea\3%\5%\u01ee\n"+
		"%\3%\3%\3&\5&\u01f3\n&\3&\3&\3&\3&\5&\u01f9\n&\3&\3&\3&\5&\u01fe\n&\3"+
		"&\3&\6&\u0202\n&\r&\16&\u0203\3&\5&\u0207\n&\3&\3&\3\'\3\'\3\'\3\'\3\'"+
		"\7\'\u0210\n\'\f\'\16\'\u0213\13\'\3(\3(\3(\5(\u0218\n(\3(\3(\3(\5(\u021d"+
		"\n(\3(\3(\3(\3(\3)\3)\3)\3)\5)\u0227\n)\3)\3)\3)\5)\u022c\n)\3)\3)\3*"+
		"\3*\3*\3*\5*\u0234\n*\3*\3*\3*\2\2+\2\4\6\b\n\f\16\20\22\24\26\30\32\34"+
		"\36 \"$&(*,.\60\62\64\668:<>@BDFHJLNPR\2\5\3\2\60\61\3\2*/\3\2\"#\2\u026e"+
		"\2T\3\2\2\2\4Z\3\2\2\2\6l\3\2\2\2\bn\3\2\2\2\nv\3\2\2\2\f\u0082\3\2\2"+
		"\2\16\u008f\3\2\2\2\20\u0091\3\2\2\2\22\u009a\3\2\2\2\24\u00af\3\2\2\2"+
		"\26\u00b2\3\2\2\2\30\u00c8\3\2\2\2\32\u00db\3\2\2\2\34\u00e9\3\2\2\2\36"+
		"\u00fd\3\2\2\2 \u0108\3\2\2\2\"\u0111\3\2\2\2$\u011a\3\2\2\2&\u0121\3"+
		"\2\2\2(\u0123\3\2\2\2*\u0129\3\2\2\2,\u012b\3\2\2\2.\u0141\3\2\2\2\60"+
		"\u0157\3\2\2\2\62\u015e\3\2\2\2\64\u0169\3\2\2\2\66\u0173\3\2\2\28\u017e"+
		"\3\2\2\2:\u0190\3\2\2\2<\u019c\3\2\2\2>\u01a7\3\2\2\2@\u01ab\3\2\2\2B"+
		"\u01b5\3\2\2\2D\u01c4\3\2\2\2F\u01d0\3\2\2\2H\u01dd\3\2\2\2J\u01f2\3\2"+
		"\2\2L\u020a\3\2\2\2N\u0214\3\2\2\2P\u0222\3\2\2\2R\u022f\3\2\2\2TV\5\4"+
		"\3\2UW\5\16\b\2VU\3\2\2\2WX\3\2\2\2XV\3\2\2\2XY\3\2\2\2Y\3\3\2\2\2Z[\7"+
		"\r\2\2[\\\7\66\2\2\\^\5\6\4\2]_\7\66\2\2^]\3\2\2\2^_\3\2\2\2_`\3\2\2\2"+
		"`a\7\67\2\2ac\5\b\5\2bd\5\n\6\2cb\3\2\2\2cd\3\2\2\2de\3\2\2\2ei\78\2\2"+
		"fh\7\13\2\2gf\3\2\2\2hk\3\2\2\2ig\3\2\2\2ij\3\2\2\2j\5\3\2\2\2ki\3\2\2"+
		"\2lm\t\2\2\2m\7\3\2\2\2no\7\16\2\2op\7\66\2\2pr\7,\2\2qs\7\66\2\2rq\3"+
		"\2\2\2rs\3\2\2\2st\3\2\2\2tu\7\13\2\2u\t\3\2\2\2vy\7\17\2\2wx\7\66\2\2"+
		"xz\5\f\7\2yw\3\2\2\2z{\3\2\2\2{y\3\2\2\2{|\3\2\2\2|~\3\2\2\2}\177\7\66"+
		"\2\2~}\3\2\2\2~\177\3\2\2\2\177\u0080\3\2\2\2\u0080\u0081\7\13\2\2\u0081"+
		"\13\3\2\2\2\u0082\u0083\7\60\2\2\u0083\r\3\2\2\2\u0084\u0090\5\26\f\2"+
		"\u0085\u0090\5\32\16\2\u0086\u0090\5\34\17\2\u0087\u0090\5\60\31\2\u0088"+
		"\u0090\5\36\20\2\u0089\u0090\5\64\33\2\u008a\u0090\5\66\34\2\u008b\u0090"+
		"\5<\37\2\u008c\u0090\58\35\2\u008d\u0090\5H%\2\u008e\u0090\5J&\2\u008f"+
		"\u0084\3\2\2\2\u008f\u0085\3\2\2\2\u008f\u0086\3\2\2\2\u008f\u0087\3\2"+
		"\2\2\u008f\u0088\3\2\2\2\u008f\u0089\3\2\2\2\u008f\u008a\3\2\2\2\u008f"+
		"\u008b\3\2\2\2\u008f\u008c\3\2\2\2\u008f\u008d\3\2\2\2\u008f\u008e\3\2"+
		"\2\2\u0090\17\3\2\2\2\u0091\u0093\5\24\13\2\u0092\u0094\7\66\2\2\u0093"+
		"\u0092\3\2\2\2\u0093\u0094\3\2\2\2\u0094\u0095\3\2\2\2\u0095\u0097\7("+
		"\2\2\u0096\u0098\7\66\2\2\u0097\u0096\3\2\2\2\u0097\u0098\3\2\2\2\u0098"+
		"\21\3\2\2\2\u0099\u009b\5\24\13\2\u009a\u0099\3\2\2\2\u009a\u009b\3\2"+
		"\2\2\u009b\u009d\3\2\2\2\u009c\u009e\7\66\2\2\u009d\u009c\3\2\2\2\u009d"+
		"\u009e\3\2\2\2\u009e\u009f\3\2\2\2\u009f\u00a1\7\3\2\2\u00a0\u00a2\7\66"+
		"\2\2\u00a1\u00a0\3\2\2\2\u00a1\u00a2\3\2\2\2\u00a2\u00a3\3\2\2\2\u00a3"+
		"\u00a5\5\f\7\2\u00a4\u00a6\7\66\2\2\u00a5\u00a4\3\2\2\2\u00a5\u00a6\3"+
		"\2\2\2\u00a6\u00a7\3\2\2\2\u00a7\u00a9\7(\2\2\u00a8\u00aa\7\66\2\2\u00a9"+
		"\u00a8\3\2\2\2\u00a9\u00aa\3\2\2\2\u00aa\23\3\2\2\2\u00ab\u00b0\5\f\7"+
		"\2\u00ac\u00b0\7\61\2\2\u00ad\u00b0\7\63\2\2\u00ae\u00b0\7\64\2\2\u00af"+
		"\u00ab\3\2\2\2\u00af\u00ac\3\2\2\2\u00af\u00ad\3\2\2\2\u00af\u00ae\3\2"+
		"\2\2\u00b0\25\3\2\2\2\u00b1\u00b3\5\20\t\2\u00b2\u00b1\3\2\2\2\u00b2\u00b3"+
		"\3\2\2\2\u00b3\u00b4\3\2\2\2\u00b4\u00b5\7\21\2\2\u00b5\u00b9\7\66\2\2"+
		"\u00b6\u00b7\7\4\2\2\u00b7\u00ba\5\24\13\2\u00b8\u00ba\5\6\4\2\u00b9\u00b6"+
		"\3\2\2\2\u00b9\u00b8\3\2\2\2\u00ba\u00be\3\2\2\2\u00bb\u00bd\5$\23\2\u00bc"+
		"\u00bb\3\2\2\2\u00bd\u00c0\3\2\2\2\u00be\u00bc\3\2\2\2\u00be\u00bf\3\2"+
		"\2\2\u00bf\u00c2\3\2\2\2\u00c0\u00be\3\2\2\2\u00c1\u00c3\7\66\2\2\u00c2"+
		"\u00c1\3\2\2\2\u00c2\u00c3\3\2\2\2\u00c3\u00c6\3\2\2\2\u00c4\u00c7\5\30"+
		"\r\2\u00c5\u00c7\7\13\2\2\u00c6\u00c4\3\2\2\2\u00c6\u00c5\3\2\2\2\u00c7"+
		"\27\3\2\2\2\u00c8\u00c9\7\67\2\2\u00c9\u00ca\7\27\2\2\u00ca\u00cb\7\66"+
		"\2\2\u00cb\u00d0\7,\2\2\u00cc\u00cd\7\66\2\2\u00cd\u00cf\7,\2\2\u00ce"+
		"\u00cc\3\2\2\2\u00cf\u00d2\3\2\2\2\u00d0\u00ce\3\2\2\2\u00d0\u00d1\3\2"+
		"\2\2\u00d1\u00d4\3\2\2\2\u00d2\u00d0\3\2\2\2\u00d3\u00d5\7\66\2\2\u00d4"+
		"\u00d3\3\2\2\2\u00d4\u00d5\3\2\2\2\u00d5\u00d6\3\2\2\2\u00d6\u00d7\7\13"+
		"\2\2\u00d7\u00d8\78\2\2\u00d8\31\3\2\2\2\u00d9\u00dc\5\20\t\2\u00da\u00dc"+
		"\5\22\n\2\u00db\u00d9\3\2\2\2\u00db\u00da\3\2\2\2\u00db\u00dc\3\2\2\2"+
		"\u00dc\u00dd\3\2\2\2\u00dd\u00de\7\22\2\2\u00de\u00e1\7\66\2\2\u00df\u00e2"+
		"\5 \21\2\u00e0\u00e2\5\"\22\2\u00e1\u00df\3\2\2\2\u00e1\u00e0\3\2\2\2"+
		"\u00e2\u00e4\3\2\2\2\u00e3\u00e5\7\66\2\2\u00e4\u00e3\3\2\2\2\u00e4\u00e5"+
		"\3\2\2\2\u00e5\u00e6\3\2\2\2\u00e6\u00e7\7\13\2\2\u00e7\33\3\2\2\2\u00e8"+
		"\u00ea\5\20\t\2\u00e9\u00e8\3\2\2\2\u00e9\u00ea\3\2\2\2\u00ea\u00eb\3"+
		"\2\2\2\u00eb\u00ec\7\23\2\2\u00ec\u00ed\7\66\2\2\u00ed\u00f0\7,\2\2\u00ee"+
		"\u00ef\7\66\2\2\u00ef\u00f1\5\24\13\2\u00f0\u00ee\3\2\2\2\u00f0\u00f1"+
		"\3\2\2\2\u00f1\u00f4\3\2\2\2\u00f2\u00f3\7\66\2\2\u00f3\u00f5\7+\2\2\u00f4"+
		"\u00f2\3\2\2\2\u00f4\u00f5\3\2\2\2\u00f5\u00f7\3\2\2\2\u00f6\u00f8\7\66"+
		"\2\2\u00f7\u00f6\3\2\2\2\u00f7\u00f8\3\2\2\2\u00f8\u00fb\3\2\2\2\u00f9"+
		"\u00fc\5N(\2\u00fa\u00fc\7\13\2\2\u00fb\u00f9\3\2\2\2\u00fb\u00fa\3\2"+
		"\2\2\u00fc\35\3\2\2\2\u00fd\u00ff\7\20\2\2\u00fe\u0100\5$\23\2\u00ff\u00fe"+
		"\3\2\2\2\u0100\u0101\3\2\2\2\u0101\u00ff\3\2\2\2\u0101\u0102\3\2\2\2\u0102"+
		"\u0104\3\2\2\2\u0103\u0105\7\66\2\2\u0104\u0103\3\2\2\2\u0104\u0105\3"+
		"\2\2\2\u0105\u0106\3\2\2\2\u0106\u0107\7\13\2\2\u0107\37\3\2\2\2\u0108"+
		"\u0109\5\6\4\2\u0109\u010a\7\5\2\2\u010a\u010e\7\60\2\2\u010b\u010d\5"+
		"$\23\2\u010c\u010b\3\2\2\2\u010d\u0110\3\2\2\2\u010e\u010c\3\2\2\2\u010e"+
		"\u010f\3\2\2\2\u010f!\3\2\2\2\u0110\u010e\3\2\2\2\u0111\u0112\5\24\13"+
		"\2\u0112\u0113\7\66\2\2\u0113\u0117\7\60\2\2\u0114\u0116\5$\23\2\u0115"+
		"\u0114\3\2\2\2\u0116\u0119\3\2\2\2\u0117\u0115\3\2\2\2\u0117\u0118\3\2"+
		"\2\2\u0118#\3\2\2\2\u0119\u0117\3\2\2\2\u011a\u011b\7\66\2\2\u011b\u011c"+
		"\5&\24\2\u011c%\3\2\2\2\u011d\u0122\5(\25\2\u011e\u0122\5\24\13\2\u011f"+
		"\u0120\7)\2\2\u0120\u0122\5\24\13\2\u0121\u011d\3\2\2\2\u0121\u011e\3"+
		"\2\2\2\u0121\u011f\3\2\2\2\u0122\'\3\2\2\2\u0123\u0124\t\3\2\2\u0124)"+
		"\3\2\2\2\u0125\u012a\5.\30\2\u0126\u012a\5,\27\2\u0127\u012a\5@!\2\u0128"+
		"\u012a\5&\24\2\u0129\u0125\3\2\2\2\u0129\u0126\3\2\2\2\u0129\u0127\3\2"+
		"\2\2\u0129\u0128\3\2\2\2\u012a+\3\2\2\2\u012b\u012d\7\6\2\2\u012c\u012e"+
		"\7\66\2\2\u012d\u012c\3\2\2\2\u012d\u012e\3\2\2\2\u012e\u0132\3\2\2\2"+
		"\u012f\u0131\5*\26\2\u0130\u012f\3\2\2\2\u0131\u0134\3\2\2\2\u0132\u0130"+
		"\3\2\2\2\u0132\u0133\3\2\2\2\u0133\u0139\3\2\2\2\u0134\u0132\3\2\2\2\u0135"+
		"\u0136\7\65\2\2\u0136\u0138\5*\26\2\u0137\u0135\3\2\2\2\u0138\u013b\3"+
		"\2\2\2\u0139\u0137\3\2\2\2\u0139\u013a\3\2\2\2\u013a\u013d\3\2\2\2\u013b"+
		"\u0139\3\2\2\2\u013c\u013e\7\66\2\2\u013d\u013c\3\2\2\2\u013d\u013e\3"+
		"\2\2\2\u013e\u013f\3\2\2\2\u013f\u0140\7\7\2\2\u0140-\3\2\2\2\u0141\u0143"+
		"\7\b\2\2\u0142\u0144\7\66\2\2\u0143\u0142\3\2\2\2\u0143\u0144\3\2\2\2"+
		"\u0144\u0148\3\2\2\2\u0145\u0147\5\62\32\2\u0146\u0145\3\2\2\2\u0147\u014a"+
		"\3\2\2\2\u0148\u0146\3\2\2\2\u0148\u0149\3\2\2\2\u0149\u014f\3\2\2\2\u014a"+
		"\u0148\3\2\2\2\u014b\u014c\7\65\2\2\u014c\u014e\5\62\32\2\u014d\u014b"+
		"\3\2\2\2\u014e\u0151\3\2\2\2\u014f\u014d\3\2\2\2\u014f\u0150\3\2\2\2\u0150"+
		"\u0153\3\2\2\2\u0151\u014f\3\2\2\2\u0152\u0154\7\66\2\2\u0153\u0152\3"+
		"\2\2\2\u0153\u0154\3\2\2\2\u0154\u0155\3\2\2\2\u0155\u0156\7\t\2\2\u0156"+
		"/\3\2\2\2\u0157\u0158\5\20\t\2\u0158\u015a\5*\26\2\u0159\u015b\7\66\2"+
		"\2\u015a\u0159\3\2\2\2\u015a\u015b\3\2\2\2\u015b\u015c\3\2\2\2\u015c\u015d"+
		"\7\13\2\2\u015d\61\3\2\2\2\u015e\u0160\7\60\2\2\u015f\u0161\7\66\2\2\u0160"+
		"\u015f\3\2\2\2\u0160\u0161\3\2\2\2\u0161\u0162\3\2\2\2\u0162\u0164\7\n"+
		"\2\2\u0163\u0165\7\66\2\2\u0164\u0163\3\2\2\2\u0164\u0165\3\2\2\2\u0165"+
		"\u0166\3\2\2\2\u0166\u0167\5*\26\2\u0167\63\3\2\2\2\u0168\u016a\5\20\t"+
		"\2\u0169\u0168\3\2\2\2\u0169\u016a\3\2\2\2\u016a\u016b\3\2\2\2\u016b\u016c"+
		"\7\37\2\2\u016c\u016f\7\66\2\2\u016d\u0170\7,\2\2\u016e\u0170\5\24\13"+
		"\2\u016f\u016d\3\2\2\2\u016f\u016e\3\2\2\2\u0170\u0171\3\2\2\2\u0171\u0172"+
		"\7\13\2\2\u0172\65\3\2\2\2\u0173\u0174\7\36\2\2\u0174\u0177\7\66\2\2\u0175"+
		"\u0178\7+\2\2\u0176\u0178\5\24\13\2\u0177\u0175\3\2\2\2\u0177\u0176\3"+
		"\2\2\2\u0178\u017a\3\2\2\2\u0179\u017b\7\66\2\2\u017a\u0179\3\2\2\2\u017a"+
		"\u017b\3\2\2\2\u017b\u017c\3\2\2\2\u017c\u017d\7\13\2\2\u017d\67\3\2\2"+
		"\2\u017e\u017f\7\34\2\2\u017f\u0180\7\66\2\2\u0180\u0181\5&\24\2\u0181"+
		"\u0182\7\66\2\2\u0182\u0184\7%\2\2\u0183\u0185\7\66\2\2\u0184\u0183\3"+
		"\2\2\2\u0184\u0185\3\2\2\2\u0185\u0186\3\2\2\2\u0186\u0188\7\67\2\2\u0187"+
		"\u0189\5:\36\2\u0188\u0187\3\2\2\2\u0189\u018a\3\2\2\2\u018a\u0188\3\2"+
		"\2\2\u018a\u018b\3\2\2\2\u018b\u018c\3\2\2\2\u018c\u018e\78\2\2\u018d"+
		"\u018f\5F$\2\u018e\u018d\3\2\2\2\u018e\u018f\3\2\2\2\u018f9\3\2\2\2\u0190"+
		"\u0192\5&\24\2\u0191\u0193\7\66\2\2\u0192\u0191\3\2\2\2\u0192\u0193\3"+
		"\2\2\2\u0193\u0194\3\2\2\2\u0194\u0196\7\67\2\2\u0195\u0197\5\16\b\2\u0196"+
		"\u0195\3\2\2\2\u0197\u0198\3\2\2\2\u0198\u0196\3\2\2\2\u0198\u0199\3\2"+
		"\2\2\u0199\u019a\3\2\2\2\u019a\u019b\78\2\2\u019b;\3\2\2\2\u019c\u019d"+
		"\5> \2\u019d\u019f\7\67\2\2\u019e\u01a0\5\16\b\2\u019f\u019e\3\2\2\2\u01a0"+
		"\u01a1\3\2\2\2\u01a1\u019f\3\2\2\2\u01a1\u01a2\3\2\2\2\u01a2\u01a3\3\2"+
		"\2\2\u01a3\u01a5\78\2\2\u01a4\u01a6\5F$\2\u01a5\u01a4\3\2\2\2\u01a5\u01a6"+
		"\3\2\2\2\u01a6=\3\2\2\2\u01a7\u01a8\7\30\2\2\u01a8\u01a9\7\66\2\2\u01a9"+
		"\u01aa\5@!\2\u01aa?\3\2\2\2\u01ab\u01af\5D#\2\u01ac\u01ae\5B\"\2\u01ad"+
		"\u01ac\3\2\2\2\u01ae\u01b1\3\2\2\2\u01af\u01ad\3\2\2\2\u01af\u01b0\3\2"+
		"\2\2\u01b0A\3\2\2\2\u01b1\u01af\3\2\2\2\u01b2\u01b4\7\13\2\2\u01b3\u01b2"+
		"\3\2\2\2\u01b4\u01b7\3\2\2\2\u01b5\u01b3\3\2\2\2\u01b5\u01b6\3\2\2\2\u01b6"+
		"\u01b8\3\2\2\2\u01b7\u01b5\3\2\2\2\u01b8\u01ba\t\4\2\2\u01b9\u01bb\7\66"+
		"\2\2\u01ba\u01b9\3\2\2\2\u01ba\u01bb\3\2\2\2\u01bb\u01bf\3\2\2\2\u01bc"+
		"\u01be\7\13\2\2\u01bd\u01bc\3\2\2\2\u01be\u01c1\3\2\2\2\u01bf\u01bd\3"+
		"\2\2\2\u01bf\u01c0\3\2\2\2\u01c0\u01c2\3\2\2\2\u01c1\u01bf\3\2\2\2\u01c2"+
		"\u01c3\5D#\2\u01c3C\3\2\2\2\u01c4\u01c5\5&\24\2\u01c5\u01c6\7\66\2\2\u01c6"+
		"\u01c7\7 \2\2\u01c7\u01ca\7\66\2\2\u01c8\u01c9\7!\2\2\u01c9\u01cb\7\66"+
		"\2\2\u01ca\u01c8\3\2\2\2\u01ca\u01cb\3\2\2\2\u01cb\u01cc\3\2\2\2\u01cc"+
		"\u01ce\5&\24\2\u01cd\u01cf\7\66\2\2\u01ce\u01cd\3\2\2\2\u01ce\u01cf\3"+
		"\2\2\2\u01cfE\3\2\2\2\u01d0\u01d2\7\31\2\2\u01d1\u01d3\7\66\2\2\u01d2"+
		"\u01d1\3\2\2\2\u01d2\u01d3\3\2\2\2\u01d3\u01d4\3\2\2\2\u01d4\u01d6\7\67"+
		"\2\2\u01d5\u01d7\5\16\b\2\u01d6\u01d5\3\2\2\2\u01d7\u01d8\3\2\2\2\u01d8"+
		"\u01d6\3\2\2\2\u01d8\u01d9\3\2\2\2\u01d9\u01da\3\2\2\2\u01da\u01db\78"+
		"\2\2\u01dbG\3\2\2\2\u01dc\u01de\5\20\t\2\u01dd\u01dc\3\2\2\2\u01dd\u01de"+
		"\3\2\2\2\u01de\u01df\3\2\2\2\u01df\u01e0\7\33\2\2\u01e0\u01e1\7\66\2\2"+
		"\u01e1\u01e2\5\24\13\2\u01e2\u01e3\7\66\2\2\u01e3\u01e4\7\'\2\2\u01e4"+
		"\u01e5\7\66\2\2\u01e5\u01e6\5\f\7\2\u01e6\u01e8\7\67\2\2\u01e7\u01e9\5"+
		"\16\b\2\u01e8\u01e7\3\2\2\2\u01e9\u01ea\3\2\2\2\u01ea\u01e8\3\2\2\2\u01ea"+
		"\u01eb\3\2\2\2\u01eb\u01ed\3\2\2\2\u01ec\u01ee\5L\'\2\u01ed\u01ec\3\2"+
		"\2\2\u01ed\u01ee\3\2\2\2\u01ee\u01ef\3\2\2\2\u01ef\u01f0\78\2\2\u01f0"+
		"I\3\2\2\2\u01f1\u01f3\5\20\t\2\u01f2\u01f1\3\2\2\2\u01f2\u01f3\3\2\2\2"+
		"\u01f3\u01f4\3\2\2\2\u01f4\u01f5\7\32\2\2\u01f5\u01f8\7\66\2\2\u01f6\u01f9"+
		"\5\24\13\2\u01f7\u01f9\7-\2\2\u01f8\u01f6\3\2\2\2\u01f8\u01f7\3\2\2\2"+
		"\u01f9\u01fa\3\2\2\2\u01fa\u01fb\7\66\2\2\u01fb\u01fd\7&\2\2\u01fc\u01fe"+
		"\7\66\2\2\u01fd\u01fc\3\2\2\2\u01fd\u01fe\3\2\2\2\u01fe\u01ff\3\2\2\2"+
		"\u01ff\u0201\7\67\2\2\u0200\u0202\5\16\b\2\u0201\u0200\3\2\2\2\u0202\u0203"+
		"\3\2\2\2\u0203\u0201\3\2\2\2\u0203\u0204\3\2\2\2\u0204\u0206\3\2\2\2\u0205"+
		"\u0207\5L\'\2\u0206\u0205\3\2\2\2\u0206\u0207\3\2\2\2\u0207\u0208\3\2"+
		"\2\2\u0208\u0209\78\2\2\u0209K\3\2\2\2\u020a\u020b\7\35\2\2\u020b\u020c"+
		"\7\66\2\2\u020c\u020d\5> \2\u020d\u0211\7\13\2\2\u020e\u0210\5\16\b\2"+
		"\u020f\u020e\3\2\2\2\u0210\u0213\3\2\2\2\u0211\u020f\3\2\2\2\u0211\u0212"+
		"\3\2\2\2\u0212M\3\2\2\2\u0213\u0211\3\2\2\2\u0214\u0215\7\67\2\2\u0215"+
		"\u0217\7\24\2\2\u0216\u0218\7\66\2\2\u0217\u0216\3\2\2\2\u0217\u0218\3"+
		"\2\2\2\u0218\u0219\3\2\2\2\u0219\u021a\7\67\2\2\u021a\u021c\5P)\2\u021b"+
		"\u021d\5\32\16\2\u021c\u021b\3\2\2\2\u021c\u021d\3\2\2\2\u021d\u021e\3"+
		"\2\2\2\u021e\u021f\5R*\2\u021f\u0220\78\2\2\u0220\u0221\78\2\2\u0221O"+
		"\3\2\2\2\u0222\u0223\7\25\2\2\u0223\u0226\7\66\2\2\u0224\u0227\5\24\13"+
		"\2\u0225\u0227\7-\2\2\u0226\u0224\3\2\2\2\u0226\u0225\3\2\2\2\u0227\u0228"+
		"\3\2\2\2\u0228\u0229\7\66\2\2\u0229\u022b\7$\2\2\u022a\u022c\7\66\2\2"+
		"\u022b\u022a\3\2\2\2\u022b\u022c\3\2\2\2\u022c\u022d\3\2\2\2\u022d\u022e"+
		"\7\13\2\2\u022eQ\3\2\2\2\u022f\u0230\7\26\2\2\u0230\u0231\7\66\2\2\u0231"+
		"\u0233\5> \2\u0232\u0234\7\66\2\2\u0233\u0232\3\2\2\2\u0233\u0234\3\2"+
		"\2\2\u0234\u0235\3\2\2\2\u0235\u0236\7\13\2\2\u0236S\3\2\2\2SX^cir{~\u008f"+
		"\u0093\u0097\u009a\u009d\u00a1\u00a5\u00a9\u00af\u00b2\u00b9\u00be\u00c2"+
		"\u00c6\u00d0\u00d4\u00db\u00e1\u00e4\u00e9\u00f0\u00f4\u00f7\u00fb\u0101"+
		"\u0104\u010e\u0117\u0121\u0129\u012d\u0132\u0139\u013d\u0143\u0148\u014f"+
		"\u0153\u015a\u0160\u0164\u0169\u016f\u0177\u017a\u0184\u018a\u018e\u0192"+
		"\u0198\u01a1\u01a5\u01af\u01b5\u01ba\u01bf\u01ca\u01ce\u01d2\u01d8\u01dd"+
		"\u01ea\u01ed\u01f2\u01f8\u01fd\u0203\u0206\u0211\u0217\u021c\u0226\u022b"+
		"\u0233";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}