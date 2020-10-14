/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

// Generated from ScimFilter.g4 by ANTLR 4.5.3
package io.jans.scim.service.antlr.scimFilter.antlr4;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.RuntimeMetaData;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.VocabularyImpl;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class ScimFilterLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.5.3", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, WHITESPACE=16, 
		ALPHA=17, NUMBER=18, BOOLEAN=19, NULL=20, NAMECHAR=21, URI=22, ATTRNAME=23, 
		SUBATTR=24, CHAR=25, STRING=26;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
		"T__9", "T__10", "T__11", "T__12", "T__13", "T__14", "DIGIT", "LOWERCASE", 
		"UPPERCASE", "HEXDIG", "NOQUOTEORBKSLSH", "BKSLSH", "WHITESPACE", "ALPHA", 
		"NUMBER", "BOOLEAN", "NULL", "NAMECHAR", "URI", "ATTRNAME", "SUBATTR", 
		"CHAR", "STRING"
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


	public ScimFilterLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "ScimFilter.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\34\u00da\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t"+
		" \4!\t!\3\2\3\2\3\2\3\3\3\3\3\3\3\4\3\4\3\4\3\5\3\5\3\5\3\6\3\6\3\6\3"+
		"\7\3\7\3\7\3\b\3\b\3\b\3\t\3\t\3\t\3\n\3\n\3\n\3\13\3\13\3\13\3\f\3\f"+
		"\3\f\3\f\3\r\3\r\3\16\3\16\3\17\3\17\3\17\3\17\3\20\3\20\3\20\3\21\3\21"+
		"\3\22\3\22\3\23\3\23\3\24\3\24\3\25\3\25\3\26\3\26\3\27\6\27~\n\27\r\27"+
		"\16\27\177\3\27\3\27\3\30\3\30\5\30\u0086\n\30\3\31\5\31\u0089\n\31\3"+
		"\31\6\31\u008c\n\31\r\31\16\31\u008d\3\31\3\31\6\31\u0092\n\31\r\31\16"+
		"\31\u0093\5\31\u0096\n\31\3\32\3\32\3\32\3\32\3\32\3\32\3\32\3\32\3\32"+
		"\5\32\u00a1\n\32\3\33\3\33\3\33\3\33\3\33\3\34\3\34\3\34\5\34\u00ab\n"+
		"\34\3\35\3\35\3\35\7\35\u00b0\n\35\f\35\16\35\u00b3\13\35\3\35\3\35\3"+
		"\35\3\36\5\36\u00b9\n\36\3\36\3\36\7\36\u00bd\n\36\f\36\16\36\u00c0\13"+
		"\36\3\37\3\37\3\37\3 \3 \3 \3 \3 \3 \3 \3 \3 \3 \3 \5 \u00d0\n \3!\3!"+
		"\7!\u00d4\n!\f!\16!\u00d7\13!\3!\3!\2\2\"\3\3\5\4\7\5\t\6\13\7\r\b\17"+
		"\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\2#\2%\2\'\2)\2+\2-\22"+
		"/\23\61\24\63\25\65\26\67\279\30;\31=\32?\33A\34\3\2\f\3\2\62;\3\2c|\3"+
		"\2C\\\5\2\62;CHch\5\2\"#%]_\1\4\2\13\13\"\"\4\2\60\60^^\4\2//aa\4\2\60"+
		"\60<<\n\2$$\61\61^^ddhhppttvv\u00e3\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2"+
		"\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23"+
		"\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2"+
		"\2\2\2\37\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65"+
		"\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3"+
		"\2\2\2\3C\3\2\2\2\5F\3\2\2\2\7I\3\2\2\2\tL\3\2\2\2\13O\3\2\2\2\rR\3\2"+
		"\2\2\17U\3\2\2\2\21X\3\2\2\2\23[\3\2\2\2\25^\3\2\2\2\27a\3\2\2\2\31e\3"+
		"\2\2\2\33g\3\2\2\2\35i\3\2\2\2\37m\3\2\2\2!p\3\2\2\2#r\3\2\2\2%t\3\2\2"+
		"\2\'v\3\2\2\2)x\3\2\2\2+z\3\2\2\2-}\3\2\2\2/\u0085\3\2\2\2\61\u0088\3"+
		"\2\2\2\63\u00a0\3\2\2\2\65\u00a2\3\2\2\2\67\u00aa\3\2\2\29\u00ac\3\2\2"+
		"\2;\u00b8\3\2\2\2=\u00c1\3\2\2\2?\u00cf\3\2\2\2A\u00d1\3\2\2\2CD\7g\2"+
		"\2DE\7s\2\2E\4\3\2\2\2FG\7p\2\2GH\7g\2\2H\6\3\2\2\2IJ\7e\2\2JK\7q\2\2"+
		"K\b\3\2\2\2LM\7u\2\2MN\7y\2\2N\n\3\2\2\2OP\7g\2\2PQ\7y\2\2Q\f\3\2\2\2"+
		"RS\7i\2\2ST\7v\2\2T\16\3\2\2\2UV\7n\2\2VW\7v\2\2W\20\3\2\2\2XY\7i\2\2"+
		"YZ\7g\2\2Z\22\3\2\2\2[\\\7n\2\2\\]\7g\2\2]\24\3\2\2\2^_\7r\2\2_`\7t\2"+
		"\2`\26\3\2\2\2ab\7p\2\2bc\7q\2\2cd\7v\2\2d\30\3\2\2\2ef\7*\2\2f\32\3\2"+
		"\2\2gh\7+\2\2h\34\3\2\2\2ij\7c\2\2jk\7p\2\2kl\7f\2\2l\36\3\2\2\2mn\7q"+
		"\2\2no\7t\2\2o \3\2\2\2pq\t\2\2\2q\"\3\2\2\2rs\t\3\2\2s$\3\2\2\2tu\t\4"+
		"\2\2u&\3\2\2\2vw\t\5\2\2w(\3\2\2\2xy\t\6\2\2y*\3\2\2\2z{\7^\2\2{,\3\2"+
		"\2\2|~\t\7\2\2}|\3\2\2\2~\177\3\2\2\2\177}\3\2\2\2\177\u0080\3\2\2\2\u0080"+
		"\u0081\3\2\2\2\u0081\u0082\b\27\2\2\u0082.\3\2\2\2\u0083\u0086\5#\22\2"+
		"\u0084\u0086\5%\23\2\u0085\u0083\3\2\2\2\u0085\u0084\3\2\2\2\u0086\60"+
		"\3\2\2\2\u0087\u0089\7/\2\2\u0088\u0087\3\2\2\2\u0088\u0089\3\2\2\2\u0089"+
		"\u008b\3\2\2\2\u008a\u008c\5!\21\2\u008b\u008a\3\2\2\2\u008c\u008d\3\2"+
		"\2\2\u008d\u008b\3\2\2\2\u008d\u008e\3\2\2\2\u008e\u0095\3\2\2\2\u008f"+
		"\u0091\t\b\2\2\u0090\u0092\5!\21\2\u0091\u0090\3\2\2\2\u0092\u0093\3\2"+
		"\2\2\u0093\u0091\3\2\2\2\u0093\u0094\3\2\2\2\u0094\u0096\3\2\2\2\u0095"+
		"\u008f\3\2\2\2\u0095\u0096\3\2\2\2\u0096\62\3\2\2\2\u0097\u0098\7h\2\2"+
		"\u0098\u0099\7c\2\2\u0099\u009a\7n\2\2\u009a\u009b\7u\2\2\u009b\u00a1"+
		"\7g\2\2\u009c\u009d\7v\2\2\u009d\u009e\7t\2\2\u009e\u009f\7w\2\2\u009f"+
		"\u00a1\7g\2\2\u00a0\u0097\3\2\2\2\u00a0\u009c\3\2\2\2\u00a1\64\3\2\2\2"+
		"\u00a2\u00a3\7p\2\2\u00a3\u00a4\7w\2\2\u00a4\u00a5\7n\2\2\u00a5\u00a6"+
		"\7n\2\2\u00a6\66\3\2\2\2\u00a7\u00ab\t\t\2\2\u00a8\u00ab\5!\21\2\u00a9"+
		"\u00ab\5/\30\2\u00aa\u00a7\3\2\2\2\u00aa\u00a8\3\2\2\2\u00aa\u00a9\3\2"+
		"\2\2\u00ab8\3\2\2\2\u00ac\u00b1\5/\30\2\u00ad\u00b0\5\67\34\2\u00ae\u00b0"+
		"\t\n\2\2\u00af\u00ad\3\2\2\2\u00af\u00ae\3\2\2\2\u00b0\u00b3\3\2\2\2\u00b1"+
		"\u00af\3\2\2\2\u00b1\u00b2\3\2\2\2\u00b2\u00b4\3\2\2\2\u00b3\u00b1\3\2"+
		"\2\2\u00b4\u00b5\5/\30\2\u00b5\u00b6\7<\2\2\u00b6:\3\2\2\2\u00b7\u00b9"+
		"\59\35\2\u00b8\u00b7\3\2\2\2\u00b8\u00b9\3\2\2\2\u00b9\u00ba\3\2\2\2\u00ba"+
		"\u00be\5/\30\2\u00bb\u00bd\5\67\34\2\u00bc\u00bb\3\2\2\2\u00bd\u00c0\3"+
		"\2\2\2\u00be\u00bc\3\2\2\2\u00be\u00bf\3\2\2\2\u00bf<\3\2\2\2\u00c0\u00be"+
		"\3\2\2\2\u00c1\u00c2\7\60\2\2\u00c2\u00c3\5;\36\2\u00c3>\3\2\2\2\u00c4"+
		"\u00d0\5)\25\2\u00c5\u00c6\5+\26\2\u00c6\u00c7\7w\2\2\u00c7\u00c8\5\'"+
		"\24\2\u00c8\u00c9\5\'\24\2\u00c9\u00ca\5\'\24\2\u00ca\u00cb\5\'\24\2\u00cb"+
		"\u00d0\3\2\2\2\u00cc\u00cd\5+\26\2\u00cd\u00ce\t\13\2\2\u00ce\u00d0\3"+
		"\2\2\2\u00cf\u00c4\3\2\2\2\u00cf\u00c5\3\2\2\2\u00cf\u00cc\3\2\2\2\u00d0"+
		"@\3\2\2\2\u00d1\u00d5\7$\2\2\u00d2\u00d4\5? \2\u00d3\u00d2\3\2\2\2\u00d4"+
		"\u00d7\3\2\2\2\u00d5\u00d3\3\2\2\2\u00d5\u00d6\3\2\2\2\u00d6\u00d8\3\2"+
		"\2\2\u00d7\u00d5\3\2\2\2\u00d8\u00d9\7$\2\2\u00d9B\3\2\2\2\21\2\177\u0085"+
		"\u0088\u008d\u0093\u0095\u00a0\u00aa\u00af\u00b1\u00b8\u00be\u00cf\u00d5"+
		"\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}