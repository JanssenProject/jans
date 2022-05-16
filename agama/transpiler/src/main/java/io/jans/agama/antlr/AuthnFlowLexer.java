// Generated from AuthnFlow.g4 by ANTLR 4.9.2
package io.jans.agama.antlr;

  import com.yuvalshavit.antlr4.DenterHelper;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class AuthnFlowLexer extends Lexer {
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
		WS=52;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "NL", 
			"DIGIT", "CH", "ALNUM", "SPACES", "COMMA", "COMMENT", "FLOWSTART", "BASE", 
			"FLOWINPUTS", "LOG", "FLOWCALL", "ACTIONCALL", "RRFCALL", "STATUS_CHK", 
			"OPEN", "CLOSE", "OVERRIDE", "WHEN", "OTHERWISE", "REPEAT", "ITERATE", 
			"MATCH", "QUIT", "FINISH", "RFAC", "IS", "NOT", "AND", "OR", "SECS", 
			"TO", "MAXTIMES", "USE", "EQ", "MINUS", "NUL", "BOOL", "STRING", "UINT", 
			"SINT", "DECIMAL", "ALPHANUM", "QNAME", "EVALNUM", "DOTEXPR", "DOTIDXEXPR", 
			"SPCOMMA", "WS"
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
			"DOTIDXEXPR", "SPCOMMA", "WS"
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


	  private final DenterHelper denter = DenterHelper.builder()
	    .nl(NL)
	    .indent(AuthnFlowParser.INDENT)
	    .dedent(AuthnFlowParser.DEDENT)
	    .pullToken(AuthnFlowLexer.super::nextToken);

	  @Override
	  public Token nextToken() {
	    return denter.nextToken();
	  }


	public AuthnFlowLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "AuthnFlow.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\66\u01dd\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t"+
		" \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t"+
		"+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64"+
		"\t\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\3\2\3\2\3\3\3\3"+
		"\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\3\n\5\n\u0087\n\n\3\n"+
		"\3\n\7\n\u008b\n\n\f\n\16\n\u008e\13\n\3\13\3\13\3\f\3\f\3\r\3\r\3\r\3"+
		"\r\7\r\u0098\n\r\f\r\16\r\u009b\13\r\3\16\6\16\u009e\n\16\r\16\16\16\u009f"+
		"\3\17\3\17\3\20\3\20\3\20\3\20\7\20\u00a8\n\20\f\20\16\20\u00ab\13\20"+
		"\3\20\3\20\3\21\3\21\3\21\3\21\3\21\3\22\3\22\3\22\3\22\3\22\3\22\3\22"+
		"\3\22\3\22\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\24\3\24\3\24\3\24\3\25"+
		"\3\25\3\25\3\25\3\25\3\25\3\25\3\25\3\26\3\26\3\26\3\26\3\26\3\27\3\27"+
		"\3\27\3\27\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30"+
		"\3\30\3\30\3\30\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\32\3\32"+
		"\3\32\3\32\3\32\3\32\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33"+
		"\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\34\3\34\3\34\3\34\3\34"+
		"\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\36\3\36\3\36\3\36"+
		"\3\36\3\36\3\36\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37"+
		"\3\37\3\37\3 \3 \3 \3 \3 \3 \3!\3!\3!\3!\3!\3\"\3\"\3\"\3\"\3\"\3\"\3"+
		"\"\3#\3#\3#\3#\3#\3$\3$\3$\3%\3%\3%\3%\3&\3&\3&\3&\3\'\3\'\3\'\3(\3(\3"+
		"(\3(\3(\3(\3(\3(\3)\3)\3)\3*\3*\3*\3*\3*\3*\3*\3*\3*\3*\3+\3+\3+\3+\3"+
		"+\3+\3,\3,\3-\3-\3.\3.\3.\3.\3.\3/\3/\3/\3/\3/\3/\3/\3/\3/\5/\u017f\n"+
		"/\3\60\3\60\7\60\u0183\n\60\f\60\16\60\u0186\13\60\3\60\3\60\3\61\6\61"+
		"\u018b\n\61\r\61\16\61\u018c\3\62\3\62\3\62\3\63\3\63\5\63\u0194\n\63"+
		"\3\63\3\63\3\63\3\64\3\64\3\65\3\65\3\65\7\65\u019e\n\65\f\65\16\65\u01a1"+
		"\13\65\3\66\3\66\3\66\5\66\u01a6\n\66\3\66\5\66\u01a9\n\66\3\67\3\67\7"+
		"\67\u01ad\n\67\f\67\16\67\u01b0\13\67\38\38\38\58\u01b5\n8\38\38\58\u01b9"+
		"\n8\38\58\u01bc\n8\38\38\78\u01c0\n8\f8\168\u01c3\138\68\u01c5\n8\r8\16"+
		"8\u01c6\39\59\u01ca\n9\39\79\u01cd\n9\f9\169\u01d0\139\39\39\59\u01d4"+
		"\n9\39\79\u01d7\n9\f9\169\u01da\139\3:\3:\2\2;\3\3\5\4\7\5\t\6\13\7\r"+
		"\b\17\t\21\n\23\13\25\2\27\2\31\2\33\2\35\2\37\f!\r#\16%\17\'\20)\21+"+
		"\22-\23/\24\61\25\63\26\65\27\67\309\31;\32=\33?\34A\35C\36E\37G I!K\""+
		"M#O$Q%S&U\'W(Y)[*]+_,a-c.e/g\60i\61k\62m\63o\64q\65s\66\3\2\7\4\2\13\13"+
		"\"\"\3\2\62;\4\2C\\c|\4\2\f\f\17\17\7\2\13\13\"#%\u0080\u0082\u008e\u00a2"+
		"\1\2\u01ef\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2"+
		"\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\37\3\2\2\2\2!"+
		"\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3"+
		"\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2"+
		"\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E"+
		"\3\2\2\2\2G\3\2\2\2\2I\3\2\2\2\2K\3\2\2\2\2M\3\2\2\2\2O\3\2\2\2\2Q\3\2"+
		"\2\2\2S\3\2\2\2\2U\3\2\2\2\2W\3\2\2\2\2Y\3\2\2\2\2[\3\2\2\2\2]\3\2\2\2"+
		"\2_\3\2\2\2\2a\3\2\2\2\2c\3\2\2\2\2e\3\2\2\2\2g\3\2\2\2\2i\3\2\2\2\2k"+
		"\3\2\2\2\2m\3\2\2\2\2o\3\2\2\2\2q\3\2\2\2\2s\3\2\2\2\3u\3\2\2\2\5w\3\2"+
		"\2\2\7y\3\2\2\2\t{\3\2\2\2\13}\3\2\2\2\r\177\3\2\2\2\17\u0081\3\2\2\2"+
		"\21\u0083\3\2\2\2\23\u0086\3\2\2\2\25\u008f\3\2\2\2\27\u0091\3\2\2\2\31"+
		"\u0093\3\2\2\2\33\u009d\3\2\2\2\35\u00a1\3\2\2\2\37\u00a3\3\2\2\2!\u00ae"+
		"\3\2\2\2#\u00b3\3\2\2\2%\u00bc\3\2\2\2\'\u00c3\3\2\2\2)\u00c7\3\2\2\2"+
		"+\u00cf\3\2\2\2-\u00d4\3\2\2\2/\u00d8\3\2\2\2\61\u00e7\3\2\2\2\63\u00f0"+
		"\3\2\2\2\65\u00f6\3\2\2\2\67\u0109\3\2\2\29\u010e\3\2\2\2;\u0118\3\2\2"+
		"\2=\u011f\3\2\2\2?\u012c\3\2\2\2A\u0132\3\2\2\2C\u0137\3\2\2\2E\u013e"+
		"\3\2\2\2G\u0143\3\2\2\2I\u0146\3\2\2\2K\u014a\3\2\2\2M\u014e\3\2\2\2O"+
		"\u0151\3\2\2\2Q\u0159\3\2\2\2S\u015c\3\2\2\2U\u0166\3\2\2\2W\u016c\3\2"+
		"\2\2Y\u016e\3\2\2\2[\u0170\3\2\2\2]\u017e\3\2\2\2_\u0180\3\2\2\2a\u018a"+
		"\3\2\2\2c\u018e\3\2\2\2e\u0193\3\2\2\2g\u0198\3\2\2\2i\u019a\3\2\2\2k"+
		"\u01a2\3\2\2\2m\u01aa\3\2\2\2o\u01b1\3\2\2\2q\u01c9\3\2\2\2s\u01db\3\2"+
		"\2\2uv\7~\2\2v\4\3\2\2\2wx\7&\2\2x\6\3\2\2\2yz\7%\2\2z\b\3\2\2\2{|\7]"+
		"\2\2|\n\3\2\2\2}~\7_\2\2~\f\3\2\2\2\177\u0080\7}\2\2\u0080\16\3\2\2\2"+
		"\u0081\u0082\7\177\2\2\u0082\20\3\2\2\2\u0083\u0084\7<\2\2\u0084\22\3"+
		"\2\2\2\u0085\u0087\7\17\2\2\u0086\u0085\3\2\2\2\u0086\u0087\3\2\2\2\u0087"+
		"\u0088\3\2\2\2\u0088\u008c\7\f\2\2\u0089\u008b\t\2\2\2\u008a\u0089\3\2"+
		"\2\2\u008b\u008e\3\2\2\2\u008c\u008a\3\2\2\2\u008c\u008d\3\2\2\2\u008d"+
		"\24\3\2\2\2\u008e\u008c\3\2\2\2\u008f\u0090\t\3\2\2\u0090\26\3\2\2\2\u0091"+
		"\u0092\t\4\2\2\u0092\30\3\2\2\2\u0093\u0099\5\27\f\2\u0094\u0098\7a\2"+
		"\2\u0095\u0098\5\27\f\2\u0096\u0098\5\25\13\2\u0097\u0094\3\2\2\2\u0097"+
		"\u0095\3\2\2\2\u0097\u0096\3\2\2\2\u0098\u009b\3\2\2\2\u0099\u0097\3\2"+
		"\2\2\u0099\u009a\3\2\2\2\u009a\32\3\2\2\2\u009b\u0099\3\2\2\2\u009c\u009e"+
		"\t\2\2\2\u009d\u009c\3\2\2\2\u009e\u009f\3\2\2\2\u009f\u009d\3\2\2\2\u009f"+
		"\u00a0\3\2\2\2\u00a0\34\3\2\2\2\u00a1\u00a2\7.\2\2\u00a2\36\3\2\2\2\u00a3"+
		"\u00a4\7\61\2\2\u00a4\u00a5\7\61\2\2\u00a5\u00a9\3\2\2\2\u00a6\u00a8\n"+
		"\5\2\2\u00a7\u00a6\3\2\2\2\u00a8\u00ab\3\2\2\2\u00a9\u00a7\3\2\2\2\u00a9"+
		"\u00aa\3\2\2\2\u00aa\u00ac\3\2\2\2\u00ab\u00a9\3\2\2\2\u00ac\u00ad\b\20"+
		"\2\2\u00ad \3\2\2\2\u00ae\u00af\7H\2\2\u00af\u00b0\7n\2\2\u00b0\u00b1"+
		"\7q\2\2\u00b1\u00b2\7y\2\2\u00b2\"\3\2\2\2\u00b3\u00b4\7D\2\2\u00b4\u00b5"+
		"\7c\2\2\u00b5\u00b6\7u\2\2\u00b6\u00b7\7g\2\2\u00b7\u00b8\7r\2\2\u00b8"+
		"\u00b9\7c\2\2\u00b9\u00ba\7v\2\2\u00ba\u00bb\7j\2\2\u00bb$\3\2\2\2\u00bc"+
		"\u00bd\7K\2\2\u00bd\u00be\7p\2\2\u00be\u00bf\7r\2\2\u00bf\u00c0\7w\2\2"+
		"\u00c0\u00c1\7v\2\2\u00c1\u00c2\7u\2\2\u00c2&\3\2\2\2\u00c3\u00c4\7N\2"+
		"\2\u00c4\u00c5\7q\2\2\u00c5\u00c6\7i\2\2\u00c6(\3\2\2\2\u00c7\u00c8\7"+
		"V\2\2\u00c8\u00c9\7t\2\2\u00c9\u00ca\7k\2\2\u00ca\u00cb\7i\2\2\u00cb\u00cc"+
		"\7i\2\2\u00cc\u00cd\7g\2\2\u00cd\u00ce\7t\2\2\u00ce*\3\2\2\2\u00cf\u00d0"+
		"\7E\2\2\u00d0\u00d1\7c\2\2\u00d1\u00d2\7n\2\2\u00d2\u00d3\7n\2\2\u00d3"+
		",\3\2\2\2\u00d4\u00d5\7T\2\2\u00d5\u00d6\7T\2\2\u00d6\u00d7\7H\2\2\u00d7"+
		".\3\2\2\2\u00d8\u00d9\7U\2\2\u00d9\u00da\7v\2\2\u00da\u00db\7c\2\2\u00db"+
		"\u00dc\7v\2\2\u00dc\u00dd\7w\2\2\u00dd\u00de\7u\2\2\u00de\u00df\7\"\2"+
		"\2\u00df\u00e0\7e\2\2\u00e0\u00e1\7j\2\2\u00e1\u00e2\7g\2\2\u00e2\u00e3"+
		"\7e\2\2\u00e3\u00e4\7m\2\2\u00e4\u00e5\7g\2\2\u00e5\u00e6\7t\2\2\u00e6"+
		"\60\3\2\2\2\u00e7\u00e8\7Q\2\2\u00e8\u00e9\7r\2\2\u00e9\u00ea\7g\2\2\u00ea"+
		"\u00eb\7p\2\2\u00eb\u00ec\7\"\2\2\u00ec\u00ed\7h\2\2\u00ed\u00ee\7q\2"+
		"\2\u00ee\u00ef\7t\2\2\u00ef\62\3\2\2\2\u00f0\u00f1\7E\2\2\u00f1\u00f2"+
		"\7n\2\2\u00f2\u00f3\7q\2\2\u00f3\u00f4\7u\2\2\u00f4\u00f5\7g\2\2\u00f5"+
		"\64\3\2\2\2\u00f6\u00f7\7Q\2\2\u00f7\u00f8\7x\2\2\u00f8\u00f9\7g\2\2\u00f9"+
		"\u00fa\7t\2\2\u00fa\u00fb\7t\2\2\u00fb\u00fc\7k\2\2\u00fc\u00fd\7f\2\2"+
		"\u00fd\u00fe\7g\2\2\u00fe\u00ff\7\"\2\2\u00ff\u0100\7v\2\2\u0100\u0101"+
		"\7g\2\2\u0101\u0102\7o\2\2\u0102\u0103\7r\2\2\u0103\u0104\7n\2\2\u0104"+
		"\u0105\7c\2\2\u0105\u0106\7v\2\2\u0106\u0107\7g\2\2\u0107\u0108\7u\2\2"+
		"\u0108\66\3\2\2\2\u0109\u010a\7Y\2\2\u010a\u010b\7j\2\2\u010b\u010c\7"+
		"g\2\2\u010c\u010d\7p\2\2\u010d8\3\2\2\2\u010e\u010f\7Q\2\2\u010f\u0110"+
		"\7v\2\2\u0110\u0111\7j\2\2\u0111\u0112\7g\2\2\u0112\u0113\7t\2\2\u0113"+
		"\u0114\7y\2\2\u0114\u0115\7k\2\2\u0115\u0116\7u\2\2\u0116\u0117\7g\2\2"+
		"\u0117:\3\2\2\2\u0118\u0119\7T\2\2\u0119\u011a\7g\2\2\u011a\u011b\7r\2"+
		"\2\u011b\u011c\7g\2\2\u011c\u011d\7c\2\2\u011d\u011e\7v\2\2\u011e<\3\2"+
		"\2\2\u011f\u0120\7K\2\2\u0120\u0121\7v\2\2\u0121\u0122\7g\2\2\u0122\u0123"+
		"\7t\2\2\u0123\u0124\7c\2\2\u0124\u0125\7v\2\2\u0125\u0126\7g\2\2\u0126"+
		"\u0127\7\"\2\2\u0127\u0128\7q\2\2\u0128\u0129\7x\2\2\u0129\u012a\7g\2"+
		"\2\u012a\u012b\7t\2\2\u012b>\3\2\2\2\u012c\u012d\7O\2\2\u012d\u012e\7"+
		"c\2\2\u012e\u012f\7v\2\2\u012f\u0130\7e\2\2\u0130\u0131\7j\2\2\u0131@"+
		"\3\2\2\2\u0132\u0133\7S\2\2\u0133\u0134\7w\2\2\u0134\u0135\7k\2\2\u0135"+
		"\u0136\7v\2\2\u0136B\3\2\2\2\u0137\u0138\7H\2\2\u0138\u0139\7k\2\2\u0139"+
		"\u013a\7p\2\2\u013a\u013b\7k\2\2\u013b\u013c\7u\2\2\u013c\u013d\7j\2\2"+
		"\u013dD\3\2\2\2\u013e\u013f\7T\2\2\u013f\u0140\7H\2\2\u0140\u0141\7C\2"+
		"\2\u0141\u0142\7E\2\2\u0142F\3\2\2\2\u0143\u0144\7k\2\2\u0144\u0145\7"+
		"u\2\2\u0145H\3\2\2\2\u0146\u0147\7p\2\2\u0147\u0148\7q\2\2\u0148\u0149"+
		"\7v\2\2\u0149J\3\2\2\2\u014a\u014b\7c\2\2\u014b\u014c\7p\2\2\u014c\u014d"+
		"\7f\2\2\u014dL\3\2\2\2\u014e\u014f\7q\2\2\u014f\u0150\7t\2\2\u0150N\3"+
		"\2\2\2\u0151\u0152\7u\2\2\u0152\u0153\7g\2\2\u0153\u0154\7e\2\2\u0154"+
		"\u0155\7q\2\2\u0155\u0156\7p\2\2\u0156\u0157\7f\2\2\u0157\u0158\7u\2\2"+
		"\u0158P\3\2\2\2\u0159\u015a\7v\2\2\u015a\u015b\7q\2\2\u015bR\3\2\2\2\u015c"+
		"\u015d\7v\2\2\u015d\u015e\7k\2\2\u015e\u015f\7o\2\2\u015f\u0160\7g\2\2"+
		"\u0160\u0161\7u\2\2\u0161\u0162\7\"\2\2\u0162\u0163\7o\2\2\u0163\u0164"+
		"\7c\2\2\u0164\u0165\7z\2\2\u0165T\3\2\2\2\u0166\u0167\7w\2\2\u0167\u0168"+
		"\7u\2\2\u0168\u0169\7k\2\2\u0169\u016a\7p\2\2\u016a\u016b\7i\2\2\u016b"+
		"V\3\2\2\2\u016c\u016d\7?\2\2\u016dX\3\2\2\2\u016e\u016f\7/\2\2\u016fZ"+
		"\3\2\2\2\u0170\u0171\7p\2\2\u0171\u0172\7w\2\2\u0172\u0173\7n\2\2\u0173"+
		"\u0174\7n\2\2\u0174\\\3\2\2\2\u0175\u0176\7h\2\2\u0176\u0177\7c\2\2\u0177"+
		"\u0178\7n\2\2\u0178\u0179\7u\2\2\u0179\u017f\7g\2\2\u017a\u017b\7v\2\2"+
		"\u017b\u017c\7t\2\2\u017c\u017d\7w\2\2\u017d\u017f\7g\2\2\u017e\u0175"+
		"\3\2\2\2\u017e\u017a\3\2\2\2\u017f^\3\2\2\2\u0180\u0184\7$\2\2\u0181\u0183"+
		"\t\6\2\2\u0182\u0181\3\2\2\2\u0183\u0186\3\2\2\2\u0184\u0182\3\2\2\2\u0184"+
		"\u0185\3\2\2\2\u0185\u0187\3\2\2\2\u0186\u0184\3\2\2\2\u0187\u0188\7$"+
		"\2\2\u0188`\3\2\2\2\u0189\u018b\5\25\13\2\u018a\u0189\3\2\2\2\u018b\u018c"+
		"\3\2\2\2\u018c\u018a\3\2\2\2\u018c\u018d\3\2\2\2\u018db\3\2\2\2\u018e"+
		"\u018f\5Y-\2\u018f\u0190\5a\61\2\u0190d\3\2\2\2\u0191\u0194\5c\62\2\u0192"+
		"\u0194\5a\61\2\u0193\u0191\3\2\2\2\u0193\u0192\3\2\2\2\u0194\u0195\3\2"+
		"\2\2\u0195\u0196\7\60\2\2\u0196\u0197\5a\61\2\u0197f\3\2\2\2\u0198\u0199"+
		"\5\31\r\2\u0199h\3\2\2\2\u019a\u019f\5\31\r\2\u019b\u019c\7\60\2\2\u019c"+
		"\u019e\5\31\r\2\u019d\u019b\3\2\2\2\u019e\u01a1\3\2\2\2\u019f\u019d\3"+
		"\2\2\2\u019f\u01a0\3\2\2\2\u01a0j\3\2\2\2\u01a1\u019f\3\2\2\2\u01a2\u01a8"+
		"\7\60\2\2\u01a3\u01a9\5_\60\2\u01a4\u01a6\7&\2\2\u01a5\u01a4\3\2\2\2\u01a5"+
		"\u01a6\3\2\2\2\u01a6\u01a7\3\2\2\2\u01a7\u01a9\5\31\r\2\u01a8\u01a3\3"+
		"\2\2\2\u01a8\u01a5\3\2\2\2\u01a9l\3\2\2\2\u01aa\u01ae\5\31\r\2\u01ab\u01ad"+
		"\5k\66\2\u01ac\u01ab\3\2\2\2\u01ad\u01b0\3\2\2\2\u01ae\u01ac\3\2\2\2\u01ae"+
		"\u01af\3\2\2\2\u01afn\3\2\2\2\u01b0\u01ae\3\2\2\2\u01b1\u01c4\5m\67\2"+
		"\u01b2\u01b4\7]\2\2\u01b3\u01b5\5\33\16\2\u01b4\u01b3\3\2\2\2\u01b4\u01b5"+
		"\3\2\2\2\u01b5\u01b8\3\2\2\2\u01b6\u01b9\5a\61\2\u01b7\u01b9\5\31\r\2"+
		"\u01b8\u01b6\3\2\2\2\u01b8\u01b7\3\2\2\2\u01b9\u01bb\3\2\2\2\u01ba\u01bc"+
		"\5\33\16\2\u01bb\u01ba\3\2\2\2\u01bb\u01bc\3\2\2\2\u01bc\u01bd\3\2\2\2"+
		"\u01bd\u01c1\7_\2\2\u01be\u01c0\5k\66\2\u01bf\u01be\3\2\2\2\u01c0\u01c3"+
		"\3\2\2\2\u01c1\u01bf\3\2\2\2\u01c1\u01c2\3\2\2\2\u01c2\u01c5\3\2\2\2\u01c3"+
		"\u01c1\3\2\2\2\u01c4\u01b2\3\2\2\2\u01c5\u01c6\3\2\2\2\u01c6\u01c4\3\2"+
		"\2\2\u01c6\u01c7\3\2\2\2\u01c7p\3\2\2\2\u01c8\u01ca\5\33\16\2\u01c9\u01c8"+
		"\3\2\2\2\u01c9\u01ca\3\2\2\2\u01ca\u01ce\3\2\2\2\u01cb\u01cd\5\23\n\2"+
		"\u01cc\u01cb\3\2\2\2\u01cd\u01d0\3\2\2\2\u01ce\u01cc\3\2\2\2\u01ce\u01cf"+
		"\3\2\2\2\u01cf\u01d1\3\2\2\2\u01d0\u01ce\3\2\2\2\u01d1\u01d3\5\35\17\2"+
		"\u01d2\u01d4\5\33\16\2\u01d3\u01d2\3\2\2\2\u01d3\u01d4\3\2\2\2\u01d4\u01d8"+
		"\3\2\2\2\u01d5\u01d7\5\23\n\2\u01d6\u01d5\3\2\2\2\u01d7\u01da\3\2\2\2"+
		"\u01d8\u01d6\3\2\2\2\u01d8\u01d9\3\2\2\2\u01d9r\3\2\2\2\u01da\u01d8\3"+
		"\2\2\2\u01db\u01dc\5\33\16\2\u01dct\3\2\2\2\32\2\u0086\u008c\u0097\u0099"+
		"\u009f\u00a9\u017e\u0184\u018c\u0193\u019f\u01a5\u01a8\u01ae\u01b4\u01b8"+
		"\u01bb\u01c1\u01c6\u01c9\u01ce\u01d3\u01d8\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}