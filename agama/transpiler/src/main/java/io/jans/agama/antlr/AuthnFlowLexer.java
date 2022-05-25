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
		COMMENT=10, FLOWSTART=11, BASE=12, TIMEOUT=13, CONFIGS=14, FLOWINPUTS=15, 
		LOG=16, FLOWCALL=17, ACTIONCALL=18, RRFCALL=19, STATUS_CHK=20, OPEN=21, 
		CLOSE=22, OVERRIDE=23, WHEN=24, OTHERWISE=25, REPEAT=26, ITERATE=27, MATCH=28, 
		QUIT=29, FINISH=30, RFAC=31, IS=32, NOT=33, AND=34, OR=35, SECS=36, TO=37, 
		MAXTIMES=38, USE=39, EQ=40, MINUS=41, NUL=42, BOOL=43, STRING=44, UINT=45, 
		SINT=46, DECIMAL=47, ALPHANUM=48, QNAME=49, EVALNUM=50, DOTEXPR=51, DOTIDXEXPR=52, 
		SPCOMMA=53, WS=54;
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
			"TIMEOUT", "CONFIGS", "FLOWINPUTS", "LOG", "FLOWCALL", "ACTIONCALL", 
			"RRFCALL", "STATUS_CHK", "OPEN", "CLOSE", "OVERRIDE", "WHEN", "OTHERWISE", 
			"REPEAT", "ITERATE", "MATCH", "QUIT", "FINISH", "RFAC", "IS", "NOT", 
			"AND", "OR", "SECS", "TO", "MAXTIMES", "USE", "EQ", "MINUS", "NUL", "BOOL", 
			"STRING", "UINT", "SINT", "DECIMAL", "ALPHANUM", "QNAME", "EVALNUM", 
			"DOTEXPR", "DOTIDXEXPR", "SPCOMMA", "WS"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'|'", "'$'", "'#'", "'['", "']'", "'{'", "'}'", "':'", null, null, 
			"'Flow'", "'Basepath'", "'Timeout'", "'Configs'", "'Inputs'", "'Log'", 
			"'Trigger'", "'Call'", "'RRF'", "'Status checker'", "'Open for'", "'Close'", 
			"'Override templates'", "'When'", "'Otherwise'", "'Repeat'", "'Iterate over'", 
			"'Match'", "'Quit'", "'Finish'", "'RFAC'", "'is'", "'not'", "'and'", 
			"'or'", "'seconds'", "'to'", "'times max'", "'using'", "'='", "'-'", 
			"'null'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, "NL", "COMMENT", 
			"FLOWSTART", "BASE", "TIMEOUT", "CONFIGS", "FLOWINPUTS", "LOG", "FLOWCALL", 
			"ACTIONCALL", "RRFCALL", "STATUS_CHK", "OPEN", "CLOSE", "OVERRIDE", "WHEN", 
			"OTHERWISE", "REPEAT", "ITERATE", "MATCH", "QUIT", "FINISH", "RFAC", 
			"IS", "NOT", "AND", "OR", "SECS", "TO", "MAXTIMES", "USE", "EQ", "MINUS", 
			"NUL", "BOOL", "STRING", "UINT", "SINT", "DECIMAL", "ALPHANUM", "QNAME", 
			"EVALNUM", "DOTEXPR", "DOTIDXEXPR", "SPCOMMA", "WS"
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\28\u01f1\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64\t"+
		"\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\3\2\3"+
		"\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\3\n\5\n\u008b"+
		"\n\n\3\n\3\n\7\n\u008f\n\n\f\n\16\n\u0092\13\n\3\13\3\13\3\f\3\f\3\r\3"+
		"\r\3\r\3\r\7\r\u009c\n\r\f\r\16\r\u009f\13\r\3\16\6\16\u00a2\n\16\r\16"+
		"\16\16\u00a3\3\17\3\17\3\20\3\20\3\20\3\20\7\20\u00ac\n\20\f\20\16\20"+
		"\u00af\13\20\3\20\3\20\3\21\3\21\3\21\3\21\3\21\3\22\3\22\3\22\3\22\3"+
		"\22\3\22\3\22\3\22\3\22\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\24\3"+
		"\24\3\24\3\24\3\24\3\24\3\24\3\24\3\25\3\25\3\25\3\25\3\25\3\25\3\25\3"+
		"\26\3\26\3\26\3\26\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\30\3\30\3"+
		"\30\3\30\3\30\3\31\3\31\3\31\3\31\3\32\3\32\3\32\3\32\3\32\3\32\3\32\3"+
		"\32\3\32\3\32\3\32\3\32\3\32\3\32\3\32\3\33\3\33\3\33\3\33\3\33\3\33\3"+
		"\33\3\33\3\33\3\34\3\34\3\34\3\34\3\34\3\34\3\35\3\35\3\35\3\35\3\35\3"+
		"\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3"+
		"\36\3\36\3\36\3\36\3\36\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3"+
		"\37\3 \3 \3 \3 \3 \3 \3 \3!\3!\3!\3!\3!\3!\3!\3!\3!\3!\3!\3!\3!\3\"\3"+
		"\"\3\"\3\"\3\"\3\"\3#\3#\3#\3#\3#\3$\3$\3$\3$\3$\3$\3$\3%\3%\3%\3%\3%"+
		"\3&\3&\3&\3\'\3\'\3\'\3\'\3(\3(\3(\3(\3)\3)\3)\3*\3*\3*\3*\3*\3*\3*\3"+
		"*\3+\3+\3+\3,\3,\3,\3,\3,\3,\3,\3,\3,\3,\3-\3-\3-\3-\3-\3-\3.\3.\3/\3"+
		"/\3\60\3\60\3\60\3\60\3\60\3\61\3\61\3\61\3\61\3\61\3\61\3\61\3\61\3\61"+
		"\5\61\u0193\n\61\3\62\3\62\7\62\u0197\n\62\f\62\16\62\u019a\13\62\3\62"+
		"\3\62\3\63\6\63\u019f\n\63\r\63\16\63\u01a0\3\64\3\64\3\64\3\65\3\65\5"+
		"\65\u01a8\n\65\3\65\3\65\3\65\3\66\3\66\3\67\3\67\3\67\7\67\u01b2\n\67"+
		"\f\67\16\67\u01b5\13\67\38\38\38\58\u01ba\n8\38\58\u01bd\n8\39\39\79\u01c1"+
		"\n9\f9\169\u01c4\139\3:\3:\3:\5:\u01c9\n:\3:\3:\5:\u01cd\n:\3:\5:\u01d0"+
		"\n:\3:\3:\7:\u01d4\n:\f:\16:\u01d7\13:\6:\u01d9\n:\r:\16:\u01da\3;\5;"+
		"\u01de\n;\3;\7;\u01e1\n;\f;\16;\u01e4\13;\3;\3;\5;\u01e8\n;\3;\7;\u01eb"+
		"\n;\f;\16;\u01ee\13;\3<\3<\2\2=\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23"+
		"\13\25\2\27\2\31\2\33\2\35\2\37\f!\r#\16%\17\'\20)\21+\22-\23/\24\61\25"+
		"\63\26\65\27\67\309\31;\32=\33?\34A\35C\36E\37G I!K\"M#O$Q%S&U\'W(Y)["+
		"*]+_,a-c.e/g\60i\61k\62m\63o\64q\65s\66u\67w8\3\2\7\4\2\13\13\"\"\3\2"+
		"\62;\4\2C\\c|\4\2\f\f\17\17\7\2\13\13\"#%\u0080\u0082\u008e\u00a2\1\2"+
		"\u0203\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2"+
		"\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\37\3\2\2\2\2!\3\2"+
		"\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2"+
		"\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3"+
		"\2\2\2\2;\3\2\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2"+
		"\2\2G\3\2\2\2\2I\3\2\2\2\2K\3\2\2\2\2M\3\2\2\2\2O\3\2\2\2\2Q\3\2\2\2\2"+
		"S\3\2\2\2\2U\3\2\2\2\2W\3\2\2\2\2Y\3\2\2\2\2[\3\2\2\2\2]\3\2\2\2\2_\3"+
		"\2\2\2\2a\3\2\2\2\2c\3\2\2\2\2e\3\2\2\2\2g\3\2\2\2\2i\3\2\2\2\2k\3\2\2"+
		"\2\2m\3\2\2\2\2o\3\2\2\2\2q\3\2\2\2\2s\3\2\2\2\2u\3\2\2\2\2w\3\2\2\2\3"+
		"y\3\2\2\2\5{\3\2\2\2\7}\3\2\2\2\t\177\3\2\2\2\13\u0081\3\2\2\2\r\u0083"+
		"\3\2\2\2\17\u0085\3\2\2\2\21\u0087\3\2\2\2\23\u008a\3\2\2\2\25\u0093\3"+
		"\2\2\2\27\u0095\3\2\2\2\31\u0097\3\2\2\2\33\u00a1\3\2\2\2\35\u00a5\3\2"+
		"\2\2\37\u00a7\3\2\2\2!\u00b2\3\2\2\2#\u00b7\3\2\2\2%\u00c0\3\2\2\2\'\u00c8"+
		"\3\2\2\2)\u00d0\3\2\2\2+\u00d7\3\2\2\2-\u00db\3\2\2\2/\u00e3\3\2\2\2\61"+
		"\u00e8\3\2\2\2\63\u00ec\3\2\2\2\65\u00fb\3\2\2\2\67\u0104\3\2\2\29\u010a"+
		"\3\2\2\2;\u011d\3\2\2\2=\u0122\3\2\2\2?\u012c\3\2\2\2A\u0133\3\2\2\2C"+
		"\u0140\3\2\2\2E\u0146\3\2\2\2G\u014b\3\2\2\2I\u0152\3\2\2\2K\u0157\3\2"+
		"\2\2M\u015a\3\2\2\2O\u015e\3\2\2\2Q\u0162\3\2\2\2S\u0165\3\2\2\2U\u016d"+
		"\3\2\2\2W\u0170\3\2\2\2Y\u017a\3\2\2\2[\u0180\3\2\2\2]\u0182\3\2\2\2_"+
		"\u0184\3\2\2\2a\u0192\3\2\2\2c\u0194\3\2\2\2e\u019e\3\2\2\2g\u01a2\3\2"+
		"\2\2i\u01a7\3\2\2\2k\u01ac\3\2\2\2m\u01ae\3\2\2\2o\u01b6\3\2\2\2q\u01be"+
		"\3\2\2\2s\u01c5\3\2\2\2u\u01dd\3\2\2\2w\u01ef\3\2\2\2yz\7~\2\2z\4\3\2"+
		"\2\2{|\7&\2\2|\6\3\2\2\2}~\7%\2\2~\b\3\2\2\2\177\u0080\7]\2\2\u0080\n"+
		"\3\2\2\2\u0081\u0082\7_\2\2\u0082\f\3\2\2\2\u0083\u0084\7}\2\2\u0084\16"+
		"\3\2\2\2\u0085\u0086\7\177\2\2\u0086\20\3\2\2\2\u0087\u0088\7<\2\2\u0088"+
		"\22\3\2\2\2\u0089\u008b\7\17\2\2\u008a\u0089\3\2\2\2\u008a\u008b\3\2\2"+
		"\2\u008b\u008c\3\2\2\2\u008c\u0090\7\f\2\2\u008d\u008f\t\2\2\2\u008e\u008d"+
		"\3\2\2\2\u008f\u0092\3\2\2\2\u0090\u008e\3\2\2\2\u0090\u0091\3\2\2\2\u0091"+
		"\24\3\2\2\2\u0092\u0090\3\2\2\2\u0093\u0094\t\3\2\2\u0094\26\3\2\2\2\u0095"+
		"\u0096\t\4\2\2\u0096\30\3\2\2\2\u0097\u009d\5\27\f\2\u0098\u009c\7a\2"+
		"\2\u0099\u009c\5\27\f\2\u009a\u009c\5\25\13\2\u009b\u0098\3\2\2\2\u009b"+
		"\u0099\3\2\2\2\u009b\u009a\3\2\2\2\u009c\u009f\3\2\2\2\u009d\u009b\3\2"+
		"\2\2\u009d\u009e\3\2\2\2\u009e\32\3\2\2\2\u009f\u009d\3\2\2\2\u00a0\u00a2"+
		"\t\2\2\2\u00a1\u00a0\3\2\2\2\u00a2\u00a3\3\2\2\2\u00a3\u00a1\3\2\2\2\u00a3"+
		"\u00a4\3\2\2\2\u00a4\34\3\2\2\2\u00a5\u00a6\7.\2\2\u00a6\36\3\2\2\2\u00a7"+
		"\u00a8\7\61\2\2\u00a8\u00a9\7\61\2\2\u00a9\u00ad\3\2\2\2\u00aa\u00ac\n"+
		"\5\2\2\u00ab\u00aa\3\2\2\2\u00ac\u00af\3\2\2\2\u00ad\u00ab\3\2\2\2\u00ad"+
		"\u00ae\3\2\2\2\u00ae\u00b0\3\2\2\2\u00af\u00ad\3\2\2\2\u00b0\u00b1\b\20"+
		"\2\2\u00b1 \3\2\2\2\u00b2\u00b3\7H\2\2\u00b3\u00b4\7n\2\2\u00b4\u00b5"+
		"\7q\2\2\u00b5\u00b6\7y\2\2\u00b6\"\3\2\2\2\u00b7\u00b8\7D\2\2\u00b8\u00b9"+
		"\7c\2\2\u00b9\u00ba\7u\2\2\u00ba\u00bb\7g\2\2\u00bb\u00bc\7r\2\2\u00bc"+
		"\u00bd\7c\2\2\u00bd\u00be\7v\2\2\u00be\u00bf\7j\2\2\u00bf$\3\2\2\2\u00c0"+
		"\u00c1\7V\2\2\u00c1\u00c2\7k\2\2\u00c2\u00c3\7o\2\2\u00c3\u00c4\7g\2\2"+
		"\u00c4\u00c5\7q\2\2\u00c5\u00c6\7w\2\2\u00c6\u00c7\7v\2\2\u00c7&\3\2\2"+
		"\2\u00c8\u00c9\7E\2\2\u00c9\u00ca\7q\2\2\u00ca\u00cb\7p\2\2\u00cb\u00cc"+
		"\7h\2\2\u00cc\u00cd\7k\2\2\u00cd\u00ce\7i\2\2\u00ce\u00cf\7u\2\2\u00cf"+
		"(\3\2\2\2\u00d0\u00d1\7K\2\2\u00d1\u00d2\7p\2\2\u00d2\u00d3\7r\2\2\u00d3"+
		"\u00d4\7w\2\2\u00d4\u00d5\7v\2\2\u00d5\u00d6\7u\2\2\u00d6*\3\2\2\2\u00d7"+
		"\u00d8\7N\2\2\u00d8\u00d9\7q\2\2\u00d9\u00da\7i\2\2\u00da,\3\2\2\2\u00db"+
		"\u00dc\7V\2\2\u00dc\u00dd\7t\2\2\u00dd\u00de\7k\2\2\u00de\u00df\7i\2\2"+
		"\u00df\u00e0\7i\2\2\u00e0\u00e1\7g\2\2\u00e1\u00e2\7t\2\2\u00e2.\3\2\2"+
		"\2\u00e3\u00e4\7E\2\2\u00e4\u00e5\7c\2\2\u00e5\u00e6\7n\2\2\u00e6\u00e7"+
		"\7n\2\2\u00e7\60\3\2\2\2\u00e8\u00e9\7T\2\2\u00e9\u00ea\7T\2\2\u00ea\u00eb"+
		"\7H\2\2\u00eb\62\3\2\2\2\u00ec\u00ed\7U\2\2\u00ed\u00ee\7v\2\2\u00ee\u00ef"+
		"\7c\2\2\u00ef\u00f0\7v\2\2\u00f0\u00f1\7w\2\2\u00f1\u00f2\7u\2\2\u00f2"+
		"\u00f3\7\"\2\2\u00f3\u00f4\7e\2\2\u00f4\u00f5\7j\2\2\u00f5\u00f6\7g\2"+
		"\2\u00f6\u00f7\7e\2\2\u00f7\u00f8\7m\2\2\u00f8\u00f9\7g\2\2\u00f9\u00fa"+
		"\7t\2\2\u00fa\64\3\2\2\2\u00fb\u00fc\7Q\2\2\u00fc\u00fd\7r\2\2\u00fd\u00fe"+
		"\7g\2\2\u00fe\u00ff\7p\2\2\u00ff\u0100\7\"\2\2\u0100\u0101\7h\2\2\u0101"+
		"\u0102\7q\2\2\u0102\u0103\7t\2\2\u0103\66\3\2\2\2\u0104\u0105\7E\2\2\u0105"+
		"\u0106\7n\2\2\u0106\u0107\7q\2\2\u0107\u0108\7u\2\2\u0108\u0109\7g\2\2"+
		"\u01098\3\2\2\2\u010a\u010b\7Q\2\2\u010b\u010c\7x\2\2\u010c\u010d\7g\2"+
		"\2\u010d\u010e\7t\2\2\u010e\u010f\7t\2\2\u010f\u0110\7k\2\2\u0110\u0111"+
		"\7f\2\2\u0111\u0112\7g\2\2\u0112\u0113\7\"\2\2\u0113\u0114\7v\2\2\u0114"+
		"\u0115\7g\2\2\u0115\u0116\7o\2\2\u0116\u0117\7r\2\2\u0117\u0118\7n\2\2"+
		"\u0118\u0119\7c\2\2\u0119\u011a\7v\2\2\u011a\u011b\7g\2\2\u011b\u011c"+
		"\7u\2\2\u011c:\3\2\2\2\u011d\u011e\7Y\2\2\u011e\u011f\7j\2\2\u011f\u0120"+
		"\7g\2\2\u0120\u0121\7p\2\2\u0121<\3\2\2\2\u0122\u0123\7Q\2\2\u0123\u0124"+
		"\7v\2\2\u0124\u0125\7j\2\2\u0125\u0126\7g\2\2\u0126\u0127\7t\2\2\u0127"+
		"\u0128\7y\2\2\u0128\u0129\7k\2\2\u0129\u012a\7u\2\2\u012a\u012b\7g\2\2"+
		"\u012b>\3\2\2\2\u012c\u012d\7T\2\2\u012d\u012e\7g\2\2\u012e\u012f\7r\2"+
		"\2\u012f\u0130\7g\2\2\u0130\u0131\7c\2\2\u0131\u0132\7v\2\2\u0132@\3\2"+
		"\2\2\u0133\u0134\7K\2\2\u0134\u0135\7v\2\2\u0135\u0136\7g\2\2\u0136\u0137"+
		"\7t\2\2\u0137\u0138\7c\2\2\u0138\u0139\7v\2\2\u0139\u013a\7g\2\2\u013a"+
		"\u013b\7\"\2\2\u013b\u013c\7q\2\2\u013c\u013d\7x\2\2\u013d\u013e\7g\2"+
		"\2\u013e\u013f\7t\2\2\u013fB\3\2\2\2\u0140\u0141\7O\2\2\u0141\u0142\7"+
		"c\2\2\u0142\u0143\7v\2\2\u0143\u0144\7e\2\2\u0144\u0145\7j\2\2\u0145D"+
		"\3\2\2\2\u0146\u0147\7S\2\2\u0147\u0148\7w\2\2\u0148\u0149\7k\2\2\u0149"+
		"\u014a\7v\2\2\u014aF\3\2\2\2\u014b\u014c\7H\2\2\u014c\u014d\7k\2\2\u014d"+
		"\u014e\7p\2\2\u014e\u014f\7k\2\2\u014f\u0150\7u\2\2\u0150\u0151\7j\2\2"+
		"\u0151H\3\2\2\2\u0152\u0153\7T\2\2\u0153\u0154\7H\2\2\u0154\u0155\7C\2"+
		"\2\u0155\u0156\7E\2\2\u0156J\3\2\2\2\u0157\u0158\7k\2\2\u0158\u0159\7"+
		"u\2\2\u0159L\3\2\2\2\u015a\u015b\7p\2\2\u015b\u015c\7q\2\2\u015c\u015d"+
		"\7v\2\2\u015dN\3\2\2\2\u015e\u015f\7c\2\2\u015f\u0160\7p\2\2\u0160\u0161"+
		"\7f\2\2\u0161P\3\2\2\2\u0162\u0163\7q\2\2\u0163\u0164\7t\2\2\u0164R\3"+
		"\2\2\2\u0165\u0166\7u\2\2\u0166\u0167\7g\2\2\u0167\u0168\7e\2\2\u0168"+
		"\u0169\7q\2\2\u0169\u016a\7p\2\2\u016a\u016b\7f\2\2\u016b\u016c\7u\2\2"+
		"\u016cT\3\2\2\2\u016d\u016e\7v\2\2\u016e\u016f\7q\2\2\u016fV\3\2\2\2\u0170"+
		"\u0171\7v\2\2\u0171\u0172\7k\2\2\u0172\u0173\7o\2\2\u0173\u0174\7g\2\2"+
		"\u0174\u0175\7u\2\2\u0175\u0176\7\"\2\2\u0176\u0177\7o\2\2\u0177\u0178"+
		"\7c\2\2\u0178\u0179\7z\2\2\u0179X\3\2\2\2\u017a\u017b\7w\2\2\u017b\u017c"+
		"\7u\2\2\u017c\u017d\7k\2\2\u017d\u017e\7p\2\2\u017e\u017f\7i\2\2\u017f"+
		"Z\3\2\2\2\u0180\u0181\7?\2\2\u0181\\\3\2\2\2\u0182\u0183\7/\2\2\u0183"+
		"^\3\2\2\2\u0184\u0185\7p\2\2\u0185\u0186\7w\2\2\u0186\u0187\7n\2\2\u0187"+
		"\u0188\7n\2\2\u0188`\3\2\2\2\u0189\u018a\7h\2\2\u018a\u018b\7c\2\2\u018b"+
		"\u018c\7n\2\2\u018c\u018d\7u\2\2\u018d\u0193\7g\2\2\u018e\u018f\7v\2\2"+
		"\u018f\u0190\7t\2\2\u0190\u0191\7w\2\2\u0191\u0193\7g\2\2\u0192\u0189"+
		"\3\2\2\2\u0192\u018e\3\2\2\2\u0193b\3\2\2\2\u0194\u0198\7$\2\2\u0195\u0197"+
		"\t\6\2\2\u0196\u0195\3\2\2\2\u0197\u019a\3\2\2\2\u0198\u0196\3\2\2\2\u0198"+
		"\u0199\3\2\2\2\u0199\u019b\3\2\2\2\u019a\u0198\3\2\2\2\u019b\u019c\7$"+
		"\2\2\u019cd\3\2\2\2\u019d\u019f\5\25\13\2\u019e\u019d\3\2\2\2\u019f\u01a0"+
		"\3\2\2\2\u01a0\u019e\3\2\2\2\u01a0\u01a1\3\2\2\2\u01a1f\3\2\2\2\u01a2"+
		"\u01a3\5]/\2\u01a3\u01a4\5e\63\2\u01a4h\3\2\2\2\u01a5\u01a8\5g\64\2\u01a6"+
		"\u01a8\5e\63\2\u01a7\u01a5\3\2\2\2\u01a7\u01a6\3\2\2\2\u01a8\u01a9\3\2"+
		"\2\2\u01a9\u01aa\7\60\2\2\u01aa\u01ab\5e\63\2\u01abj\3\2\2\2\u01ac\u01ad"+
		"\5\31\r\2\u01adl\3\2\2\2\u01ae\u01b3\5\31\r\2\u01af\u01b0\7\60\2\2\u01b0"+
		"\u01b2\5\31\r\2\u01b1\u01af\3\2\2\2\u01b2\u01b5\3\2\2\2\u01b3\u01b1\3"+
		"\2\2\2\u01b3\u01b4\3\2\2\2\u01b4n\3\2\2\2\u01b5\u01b3\3\2\2\2\u01b6\u01bc"+
		"\7\60\2\2\u01b7\u01bd\5c\62\2\u01b8\u01ba\7&\2\2\u01b9\u01b8\3\2\2\2\u01b9"+
		"\u01ba\3\2\2\2\u01ba\u01bb\3\2\2\2\u01bb\u01bd\5\31\r\2\u01bc\u01b7\3"+
		"\2\2\2\u01bc\u01b9\3\2\2\2\u01bdp\3\2\2\2\u01be\u01c2\5\31\r\2\u01bf\u01c1"+
		"\5o8\2\u01c0\u01bf\3\2\2\2\u01c1\u01c4\3\2\2\2\u01c2\u01c0\3\2\2\2\u01c2"+
		"\u01c3\3\2\2\2\u01c3r\3\2\2\2\u01c4\u01c2\3\2\2\2\u01c5\u01d8\5q9\2\u01c6"+
		"\u01c8\7]\2\2\u01c7\u01c9\5\33\16\2\u01c8\u01c7\3\2\2\2\u01c8\u01c9\3"+
		"\2\2\2\u01c9\u01cc\3\2\2\2\u01ca\u01cd\5e\63\2\u01cb\u01cd\5\31\r\2\u01cc"+
		"\u01ca\3\2\2\2\u01cc\u01cb\3\2\2\2\u01cd\u01cf\3\2\2\2\u01ce\u01d0\5\33"+
		"\16\2\u01cf\u01ce\3\2\2\2\u01cf\u01d0\3\2\2\2\u01d0\u01d1\3\2\2\2\u01d1"+
		"\u01d5\7_\2\2\u01d2\u01d4\5o8\2\u01d3\u01d2\3\2\2\2\u01d4\u01d7\3\2\2"+
		"\2\u01d5\u01d3\3\2\2\2\u01d5\u01d6\3\2\2\2\u01d6\u01d9\3\2\2\2\u01d7\u01d5"+
		"\3\2\2\2\u01d8\u01c6\3\2\2\2\u01d9\u01da\3\2\2\2\u01da\u01d8\3\2\2\2\u01da"+
		"\u01db\3\2\2\2\u01dbt\3\2\2\2\u01dc\u01de\5\33\16\2\u01dd\u01dc\3\2\2"+
		"\2\u01dd\u01de\3\2\2\2\u01de\u01e2\3\2\2\2\u01df\u01e1\5\23\n\2\u01e0"+
		"\u01df\3\2\2\2\u01e1\u01e4\3\2\2\2\u01e2\u01e0\3\2\2\2\u01e2\u01e3\3\2"+
		"\2\2\u01e3\u01e5\3\2\2\2\u01e4\u01e2\3\2\2\2\u01e5\u01e7\5\35\17\2\u01e6"+
		"\u01e8\5\33\16\2\u01e7\u01e6\3\2\2\2\u01e7\u01e8\3\2\2\2\u01e8\u01ec\3"+
		"\2\2\2\u01e9\u01eb\5\23\n\2\u01ea\u01e9\3\2\2\2\u01eb\u01ee\3\2\2\2\u01ec"+
		"\u01ea\3\2\2\2\u01ec\u01ed\3\2\2\2\u01edv\3\2\2\2\u01ee\u01ec\3\2\2\2"+
		"\u01ef\u01f0\5\33\16\2\u01f0x\3\2\2\2\32\2\u008a\u0090\u009b\u009d\u00a3"+
		"\u00ad\u0192\u0198\u01a0\u01a7\u01b3\u01b9\u01bc\u01c2\u01c8\u01cc\u01cf"+
		"\u01d5\u01da\u01dd\u01e2\u01e7\u01ec\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}