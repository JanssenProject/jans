/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

// Generated from ScimFilter.g4 by ANTLR 4.5.3
package io.jans.scim.service.antlr.scimFilter.antlr4;

import java.util.List;

import org.antlr.v4.runtime.FailedPredicateException;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.RuntimeMetaData;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.VocabularyImpl;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class ScimFilterParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.5.3", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, WHITESPACE=16, 
		ALPHA=17, NUMBER=18, BOOLEAN=19, NULL=20, NAMECHAR=21, URI=22, ATTRNAME=23, 
		SUBATTR=24, CHAR=25, STRING=26;
	public static final int
		RULE_attrpath = 0, RULE_compareop = 1, RULE_compvalue = 2, RULE_attrexp = 3, 
		RULE_filter = 4;
	public static final String[] ruleNames = {
		"attrpath", "compareop", "compvalue", "attrexp", "filter"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'eq'", "'ne'", "'co'", "'sw'", "'ew'", "'gt'", "'lt'", "'ge'", 
		"'le'", "'pr'", "'not'", "'('", "')'", "'and'", "'or'", null, null, null, 
		null, "'null'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, "WHITESPACE", "ALPHA", "NUMBER", "BOOLEAN", "NULL", 
		"NAMECHAR", "URI", "ATTRNAME", "SUBATTR", "CHAR", "STRING"
	};
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
	public String getGrammarFileName() { return "ScimFilter.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public ScimFilterParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class AttrpathContext extends ParserRuleContext {
		public TerminalNode ATTRNAME() { return getToken(ScimFilterParser.ATTRNAME, 0); }
		public TerminalNode SUBATTR() { return getToken(ScimFilterParser.SUBATTR, 0); }
		public AttrpathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attrpath; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ScimFilterListener ) ((ScimFilterListener)listener).enterAttrpath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ScimFilterListener ) ((ScimFilterListener)listener).exitAttrpath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ScimFilterVisitor ) return ((ScimFilterVisitor<? extends T>)visitor).visitAttrpath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AttrpathContext attrpath() throws RecognitionException {
		AttrpathContext _localctx = new AttrpathContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_attrpath);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(10);
			match(ATTRNAME);
			setState(12);
			_la = _input.LA(1);
			if (_la==SUBATTR) {
				{
				setState(11);
				match(SUBATTR);
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

	public static class CompareopContext extends ParserRuleContext {
		public CompareopContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_compareop; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ScimFilterListener ) ((ScimFilterListener)listener).enterCompareop(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ScimFilterListener ) ((ScimFilterListener)listener).exitCompareop(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ScimFilterVisitor ) return ((ScimFilterVisitor<? extends T>)visitor).visitCompareop(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CompareopContext compareop() throws RecognitionException {
		CompareopContext _localctx = new CompareopContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_compareop);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(14);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8))) != 0)) ) {
			_errHandler.recoverInline(this);
			} else {
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

	public static class CompvalueContext extends ParserRuleContext {
		public TerminalNode BOOLEAN() { return getToken(ScimFilterParser.BOOLEAN, 0); }
		public TerminalNode NUMBER() { return getToken(ScimFilterParser.NUMBER, 0); }
		public TerminalNode STRING() { return getToken(ScimFilterParser.STRING, 0); }
		public TerminalNode NULL() { return getToken(ScimFilterParser.NULL, 0); }
		public CompvalueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_compvalue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ScimFilterListener ) ((ScimFilterListener)listener).enterCompvalue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ScimFilterListener ) ((ScimFilterListener)listener).exitCompvalue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ScimFilterVisitor ) return ((ScimFilterVisitor<? extends T>)visitor).visitCompvalue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CompvalueContext compvalue() throws RecognitionException {
		CompvalueContext _localctx = new CompvalueContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_compvalue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(16);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << NUMBER) | (1L << BOOLEAN) | (1L << NULL) | (1L << STRING))) != 0)) ) {
			_errHandler.recoverInline(this);
			} else {
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

	public static class AttrexpContext extends ParserRuleContext {
		public AttrpathContext attrpath() {
			return getRuleContext(AttrpathContext.class,0);
		}
		public CompareopContext compareop() {
			return getRuleContext(CompareopContext.class,0);
		}
		public CompvalueContext compvalue() {
			return getRuleContext(CompvalueContext.class,0);
		}
		public AttrexpContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attrexp; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ScimFilterListener ) ((ScimFilterListener)listener).enterAttrexp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ScimFilterListener ) ((ScimFilterListener)listener).exitAttrexp(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ScimFilterVisitor ) return ((ScimFilterVisitor<? extends T>)visitor).visitAttrexp(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AttrexpContext attrexp() throws RecognitionException {
		AttrexpContext _localctx = new AttrexpContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_attrexp);
		try {
			setState(25);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(18);
				attrpath();
				setState(19);
				match(T__9);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(21);
				attrpath();
				setState(22);
				compareop();
				setState(23);
				compvalue();
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

	public static class FilterContext extends ParserRuleContext {
		public FilterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filter; }
	 
		public FilterContext() { }
		public void copyFrom(FilterContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class SimpleExprContext extends FilterContext {
		public AttrexpContext attrexp() {
			return getRuleContext(AttrexpContext.class,0);
		}
		public SimpleExprContext(FilterContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ScimFilterListener ) ((ScimFilterListener)listener).enterSimpleExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ScimFilterListener ) ((ScimFilterListener)listener).exitSimpleExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ScimFilterVisitor ) return ((ScimFilterVisitor<? extends T>)visitor).visitSimpleExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AndFilterContext extends FilterContext {
		public List<FilterContext> filter() {
			return getRuleContexts(FilterContext.class);
		}
		public FilterContext filter(int i) {
			return getRuleContext(FilterContext.class,i);
		}
		public AndFilterContext(FilterContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ScimFilterListener ) ((ScimFilterListener)listener).enterAndFilter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ScimFilterListener ) ((ScimFilterListener)listener).exitAndFilter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ScimFilterVisitor ) return ((ScimFilterVisitor<? extends T>)visitor).visitAndFilter(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class NegatedFilterContext extends FilterContext {
		public FilterContext filter() {
			return getRuleContext(FilterContext.class,0);
		}
		public NegatedFilterContext(FilterContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ScimFilterListener ) ((ScimFilterListener)listener).enterNegatedFilter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ScimFilterListener ) ((ScimFilterListener)listener).exitNegatedFilter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ScimFilterVisitor ) return ((ScimFilterVisitor<? extends T>)visitor).visitNegatedFilter(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class OrFilterContext extends FilterContext {
		public List<FilterContext> filter() {
			return getRuleContexts(FilterContext.class);
		}
		public FilterContext filter(int i) {
			return getRuleContext(FilterContext.class,i);
		}
		public OrFilterContext(FilterContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ScimFilterListener ) ((ScimFilterListener)listener).enterOrFilter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ScimFilterListener ) ((ScimFilterListener)listener).exitOrFilter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ScimFilterVisitor ) return ((ScimFilterVisitor<? extends T>)visitor).visitOrFilter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FilterContext filter() throws RecognitionException {
		return filter(0);
	}

	private FilterContext filter(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		FilterContext _localctx = new FilterContext(_ctx, _parentState);
		FilterContext _prevctx = _localctx;
		int _startState = 8;
		enterRecursionRule(_localctx, 8, RULE_filter, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(36);
			switch (_input.LA(1)) {
			case T__10:
			case T__11:
				{
				_localctx = new NegatedFilterContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(29);
				_la = _input.LA(1);
				if (_la==T__10) {
					{
					setState(28);
					match(T__10);
					}
				}

				setState(31);
				match(T__11);
				setState(32);
				filter(0);
				setState(33);
				match(T__12);
				}
				break;
			case ATTRNAME:
				{
				_localctx = new SimpleExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(35);
				attrexp();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(46);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(44);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,4,_ctx) ) {
					case 1:
						{
						_localctx = new AndFilterContext(new FilterContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_filter);
						setState(38);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(39);
						match(T__13);
						setState(40);
						filter(4);
						}
						break;
					case 2:
						{
						_localctx = new OrFilterContext(new FilterContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_filter);
						setState(41);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(42);
						match(T__14);
						setState(43);
						filter(3);
						}
						break;
					}
					} 
				}
				setState(48);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 4:
			return filter_sempred((FilterContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean filter_sempred(FilterContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 3);
		case 1:
			return precpred(_ctx, 2);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3\34\64\4\2\t\2\4\3"+
		"\t\3\4\4\t\4\4\5\t\5\4\6\t\6\3\2\3\2\5\2\17\n\2\3\3\3\3\3\4\3\4\3\5\3"+
		"\5\3\5\3\5\3\5\3\5\3\5\5\5\34\n\5\3\6\3\6\5\6 \n\6\3\6\3\6\3\6\3\6\3\6"+
		"\5\6\'\n\6\3\6\3\6\3\6\3\6\3\6\3\6\7\6/\n\6\f\6\16\6\62\13\6\3\6\2\3\n"+
		"\7\2\4\6\b\n\2\4\3\2\3\13\4\2\24\26\34\34\64\2\f\3\2\2\2\4\20\3\2\2\2"+
		"\6\22\3\2\2\2\b\33\3\2\2\2\n&\3\2\2\2\f\16\7\31\2\2\r\17\7\32\2\2\16\r"+
		"\3\2\2\2\16\17\3\2\2\2\17\3\3\2\2\2\20\21\t\2\2\2\21\5\3\2\2\2\22\23\t"+
		"\3\2\2\23\7\3\2\2\2\24\25\5\2\2\2\25\26\7\f\2\2\26\34\3\2\2\2\27\30\5"+
		"\2\2\2\30\31\5\4\3\2\31\32\5\6\4\2\32\34\3\2\2\2\33\24\3\2\2\2\33\27\3"+
		"\2\2\2\34\t\3\2\2\2\35\37\b\6\1\2\36 \7\r\2\2\37\36\3\2\2\2\37 \3\2\2"+
		"\2 !\3\2\2\2!\"\7\16\2\2\"#\5\n\6\2#$\7\17\2\2$\'\3\2\2\2%\'\5\b\5\2&"+
		"\35\3\2\2\2&%\3\2\2\2\'\60\3\2\2\2()\f\5\2\2)*\7\20\2\2*/\5\n\6\6+,\f"+
		"\4\2\2,-\7\21\2\2-/\5\n\6\5.(\3\2\2\2.+\3\2\2\2/\62\3\2\2\2\60.\3\2\2"+
		"\2\60\61\3\2\2\2\61\13\3\2\2\2\62\60\3\2\2\2\b\16\33\37&.\60";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}