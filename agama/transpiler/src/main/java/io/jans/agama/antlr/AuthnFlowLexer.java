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
		ACTIONCALL=16, RRFCALL=17, STATUS_REQ=18, ALLOW=19, REPLY=20, UNTIL=21, 
		OVERRIDE=22, WHEN=23, OTHERWISE=24, REPEAT=25, ITERATE=26, MATCH=27, QUIT=28, 
		FINISH=29, RFAC=30, IS=31, NOT=32, AND=33, OR=34, SECS=35, TO=36, MAXTIMES=37, 
		USE=38, EQ=39, MINUS=40, NUL=41, BOOL=42, STRING=43, UINT=44, SINT=45, 
		DECIMAL=46, ALPHANUM=47, QNAME=48, EVALNUM=49, DOTEXPR=50, DOTIDXEXPR=51, 
		SPCOMMA=52, WS=53;
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
			"FLOWINPUTS", "LOG", "FLOWCALL", "ACTIONCALL", "RRFCALL", "STATUS_REQ", 
			"ALLOW", "REPLY", "UNTIL", "OVERRIDE", "WHEN", "OTHERWISE", "REPEAT", 
			"ITERATE", "MATCH", "QUIT", "FINISH", "RFAC", "IS", "NOT", "AND", "OR", 
			"SECS", "TO", "MAXTIMES", "USE", "EQ", "MINUS", "NUL", "BOOL", "STRING", 
			"UINT", "SINT", "DECIMAL", "ALPHANUM", "QNAME", "EVALNUM", "DOTEXPR", 
			"DOTIDXEXPR", "SPCOMMA", "WS"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'|'", "'$'", "'#'", "'['", "']'", "'{'", "'}'", "':'", null, null, 
			"'Flow'", "'Basepath'", "'Inputs'", "'Log'", "'Trigger'", "'Call'", "'RRF'", 
			"'Status requests'", "'Allow for'", "'Reply'", "'Until'", "'Override templates'", 
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
			"STATUS_REQ", "ALLOW", "REPLY", "UNTIL", "OVERRIDE", "WHEN", "OTHERWISE", 
			"REPEAT", "ITERATE", "MATCH", "QUIT", "FINISH", "RFAC", "IS", "NOT", 
			"AND", "OR", "SECS", "TO", "MAXTIMES", "USE", "EQ", "MINUS", "NUL", "BOOL", 
			"STRING", "UINT", "SINT", "DECIMAL", "ALPHANUM", "QNAME", "EVALNUM", 
			"DOTEXPR", "DOTIDXEXPR", "SPCOMMA", "WS"
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\67\u01e7\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t"+
		" \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t"+
		"+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64"+
		"\t\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\3\2\3\2\3"+
		"\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\3\n\5\n\u0089\n"+
		"\n\3\n\3\n\7\n\u008d\n\n\f\n\16\n\u0090\13\n\3\13\3\13\3\f\3\f\3\r\3\r"+
		"\3\r\3\r\7\r\u009a\n\r\f\r\16\r\u009d\13\r\3\16\6\16\u00a0\n\16\r\16\16"+
		"\16\u00a1\3\17\3\17\3\20\3\20\3\20\3\20\7\20\u00aa\n\20\f\20\16\20\u00ad"+
		"\13\20\3\20\3\20\3\21\3\21\3\21\3\21\3\21\3\22\3\22\3\22\3\22\3\22\3\22"+
		"\3\22\3\22\3\22\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\24\3\24\3\24\3\24"+
		"\3\25\3\25\3\25\3\25\3\25\3\25\3\25\3\25\3\26\3\26\3\26\3\26\3\26\3\27"+
		"\3\27\3\27\3\27\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30"+
		"\3\30\3\30\3\30\3\30\3\30\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31"+
		"\3\31\3\32\3\32\3\32\3\32\3\32\3\32\3\33\3\33\3\33\3\33\3\33\3\33\3\34"+
		"\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34"+
		"\3\34\3\34\3\34\3\34\3\35\3\35\3\35\3\35\3\35\3\36\3\36\3\36\3\36\3\36"+
		"\3\36\3\36\3\36\3\36\3\36\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3 \3 \3 "+
		"\3 \3 \3 \3 \3 \3 \3 \3 \3 \3 \3!\3!\3!\3!\3!\3!\3\"\3\"\3\"\3\"\3\"\3"+
		"#\3#\3#\3#\3#\3#\3#\3$\3$\3$\3$\3$\3%\3%\3%\3&\3&\3&\3&\3\'\3\'\3\'\3"+
		"\'\3(\3(\3(\3)\3)\3)\3)\3)\3)\3)\3)\3*\3*\3*\3+\3+\3+\3+\3+\3+\3+\3+\3"+
		"+\3+\3,\3,\3,\3,\3,\3,\3-\3-\3.\3.\3/\3/\3/\3/\3/\3\60\3\60\3\60\3\60"+
		"\3\60\3\60\3\60\3\60\3\60\5\60\u0189\n\60\3\61\3\61\7\61\u018d\n\61\f"+
		"\61\16\61\u0190\13\61\3\61\3\61\3\62\6\62\u0195\n\62\r\62\16\62\u0196"+
		"\3\63\3\63\3\63\3\64\3\64\5\64\u019e\n\64\3\64\3\64\3\64\3\65\3\65\3\66"+
		"\3\66\3\66\7\66\u01a8\n\66\f\66\16\66\u01ab\13\66\3\67\3\67\3\67\5\67"+
		"\u01b0\n\67\3\67\5\67\u01b3\n\67\38\38\78\u01b7\n8\f8\168\u01ba\138\3"+
		"9\39\39\59\u01bf\n9\39\39\59\u01c3\n9\39\59\u01c6\n9\39\39\79\u01ca\n"+
		"9\f9\169\u01cd\139\69\u01cf\n9\r9\169\u01d0\3:\5:\u01d4\n:\3:\7:\u01d7"+
		"\n:\f:\16:\u01da\13:\3:\3:\5:\u01de\n:\3:\7:\u01e1\n:\f:\16:\u01e4\13"+
		":\3;\3;\2\2<\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\2\27\2\31\2\33"+
		"\2\35\2\37\f!\r#\16%\17\'\20)\21+\22-\23/\24\61\25\63\26\65\27\67\309"+
		"\31;\32=\33?\34A\35C\36E\37G I!K\"M#O$Q%S&U\'W(Y)[*]+_,a-c.e/g\60i\61"+
		"k\62m\63o\64q\65s\66u\67\3\2\7\4\2\13\13\"\"\3\2\62;\4\2C\\c|\4\2\f\f"+
		"\17\17\7\2\13\13\"#%\u0080\u0082\u008e\u00a2\1\2\u01f9\2\3\3\2\2\2\2\5"+
		"\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2"+
		"\2\21\3\2\2\2\2\23\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2"+
		"\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2"+
		"\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2"+
		"\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2\2\2I\3\2\2\2"+
		"\2K\3\2\2\2\2M\3\2\2\2\2O\3\2\2\2\2Q\3\2\2\2\2S\3\2\2\2\2U\3\2\2\2\2W"+
		"\3\2\2\2\2Y\3\2\2\2\2[\3\2\2\2\2]\3\2\2\2\2_\3\2\2\2\2a\3\2\2\2\2c\3\2"+
		"\2\2\2e\3\2\2\2\2g\3\2\2\2\2i\3\2\2\2\2k\3\2\2\2\2m\3\2\2\2\2o\3\2\2\2"+
		"\2q\3\2\2\2\2s\3\2\2\2\2u\3\2\2\2\3w\3\2\2\2\5y\3\2\2\2\7{\3\2\2\2\t}"+
		"\3\2\2\2\13\177\3\2\2\2\r\u0081\3\2\2\2\17\u0083\3\2\2\2\21\u0085\3\2"+
		"\2\2\23\u0088\3\2\2\2\25\u0091\3\2\2\2\27\u0093\3\2\2\2\31\u0095\3\2\2"+
		"\2\33\u009f\3\2\2\2\35\u00a3\3\2\2\2\37\u00a5\3\2\2\2!\u00b0\3\2\2\2#"+
		"\u00b5\3\2\2\2%\u00be\3\2\2\2\'\u00c5\3\2\2\2)\u00c9\3\2\2\2+\u00d1\3"+
		"\2\2\2-\u00d6\3\2\2\2/\u00da\3\2\2\2\61\u00ea\3\2\2\2\63\u00f4\3\2\2\2"+
		"\65\u00fa\3\2\2\2\67\u0100\3\2\2\29\u0113\3\2\2\2;\u0118\3\2\2\2=\u0122"+
		"\3\2\2\2?\u0129\3\2\2\2A\u0136\3\2\2\2C\u013c\3\2\2\2E\u0141\3\2\2\2G"+
		"\u0148\3\2\2\2I\u014d\3\2\2\2K\u0150\3\2\2\2M\u0154\3\2\2\2O\u0158\3\2"+
		"\2\2Q\u015b\3\2\2\2S\u0163\3\2\2\2U\u0166\3\2\2\2W\u0170\3\2\2\2Y\u0176"+
		"\3\2\2\2[\u0178\3\2\2\2]\u017a\3\2\2\2_\u0188\3\2\2\2a\u018a\3\2\2\2c"+
		"\u0194\3\2\2\2e\u0198\3\2\2\2g\u019d\3\2\2\2i\u01a2\3\2\2\2k\u01a4\3\2"+
		"\2\2m\u01ac\3\2\2\2o\u01b4\3\2\2\2q\u01bb\3\2\2\2s\u01d3\3\2\2\2u\u01e5"+
		"\3\2\2\2wx\7~\2\2x\4\3\2\2\2yz\7&\2\2z\6\3\2\2\2{|\7%\2\2|\b\3\2\2\2}"+
		"~\7]\2\2~\n\3\2\2\2\177\u0080\7_\2\2\u0080\f\3\2\2\2\u0081\u0082\7}\2"+
		"\2\u0082\16\3\2\2\2\u0083\u0084\7\177\2\2\u0084\20\3\2\2\2\u0085\u0086"+
		"\7<\2\2\u0086\22\3\2\2\2\u0087\u0089\7\17\2\2\u0088\u0087\3\2\2\2\u0088"+
		"\u0089\3\2\2\2\u0089\u008a\3\2\2\2\u008a\u008e\7\f\2\2\u008b\u008d\t\2"+
		"\2\2\u008c\u008b\3\2\2\2\u008d\u0090\3\2\2\2\u008e\u008c\3\2\2\2\u008e"+
		"\u008f\3\2\2\2\u008f\24\3\2\2\2\u0090\u008e\3\2\2\2\u0091\u0092\t\3\2"+
		"\2\u0092\26\3\2\2\2\u0093\u0094\t\4\2\2\u0094\30\3\2\2\2\u0095\u009b\5"+
		"\27\f\2\u0096\u009a\7a\2\2\u0097\u009a\5\27\f\2\u0098\u009a\5\25\13\2"+
		"\u0099\u0096\3\2\2\2\u0099\u0097\3\2\2\2\u0099\u0098\3\2\2\2\u009a\u009d"+
		"\3\2\2\2\u009b\u0099\3\2\2\2\u009b\u009c\3\2\2\2\u009c\32\3\2\2\2\u009d"+
		"\u009b\3\2\2\2\u009e\u00a0\t\2\2\2\u009f\u009e\3\2\2\2\u00a0\u00a1\3\2"+
		"\2\2\u00a1\u009f\3\2\2\2\u00a1\u00a2\3\2\2\2\u00a2\34\3\2\2\2\u00a3\u00a4"+
		"\7.\2\2\u00a4\36\3\2\2\2\u00a5\u00a6\7\61\2\2\u00a6\u00a7\7\61\2\2\u00a7"+
		"\u00ab\3\2\2\2\u00a8\u00aa\n\5\2\2\u00a9\u00a8\3\2\2\2\u00aa\u00ad\3\2"+
		"\2\2\u00ab\u00a9\3\2\2\2\u00ab\u00ac\3\2\2\2\u00ac\u00ae\3\2\2\2\u00ad"+
		"\u00ab\3\2\2\2\u00ae\u00af\b\20\2\2\u00af \3\2\2\2\u00b0\u00b1\7H\2\2"+
		"\u00b1\u00b2\7n\2\2\u00b2\u00b3\7q\2\2\u00b3\u00b4\7y\2\2\u00b4\"\3\2"+
		"\2\2\u00b5\u00b6\7D\2\2\u00b6\u00b7\7c\2\2\u00b7\u00b8\7u\2\2\u00b8\u00b9"+
		"\7g\2\2\u00b9\u00ba\7r\2\2\u00ba\u00bb\7c\2\2\u00bb\u00bc\7v\2\2\u00bc"+
		"\u00bd\7j\2\2\u00bd$\3\2\2\2\u00be\u00bf\7K\2\2\u00bf\u00c0\7p\2\2\u00c0"+
		"\u00c1\7r\2\2\u00c1\u00c2\7w\2\2\u00c2\u00c3\7v\2\2\u00c3\u00c4\7u\2\2"+
		"\u00c4&\3\2\2\2\u00c5\u00c6\7N\2\2\u00c6\u00c7\7q\2\2\u00c7\u00c8\7i\2"+
		"\2\u00c8(\3\2\2\2\u00c9\u00ca\7V\2\2\u00ca\u00cb\7t\2\2\u00cb\u00cc\7"+
		"k\2\2\u00cc\u00cd\7i\2\2\u00cd\u00ce\7i\2\2\u00ce\u00cf\7g\2\2\u00cf\u00d0"+
		"\7t\2\2\u00d0*\3\2\2\2\u00d1\u00d2\7E\2\2\u00d2\u00d3\7c\2\2\u00d3\u00d4"+
		"\7n\2\2\u00d4\u00d5\7n\2\2\u00d5,\3\2\2\2\u00d6\u00d7\7T\2\2\u00d7\u00d8"+
		"\7T\2\2\u00d8\u00d9\7H\2\2\u00d9.\3\2\2\2\u00da\u00db\7U\2\2\u00db\u00dc"+
		"\7v\2\2\u00dc\u00dd\7c\2\2\u00dd\u00de\7v\2\2\u00de\u00df\7w\2\2\u00df"+
		"\u00e0\7u\2\2\u00e0\u00e1\7\"\2\2\u00e1\u00e2\7t\2\2\u00e2\u00e3\7g\2"+
		"\2\u00e3\u00e4\7s\2\2\u00e4\u00e5\7w\2\2\u00e5\u00e6\7g\2\2\u00e6\u00e7"+
		"\7u\2\2\u00e7\u00e8\7v\2\2\u00e8\u00e9\7u\2\2\u00e9\60\3\2\2\2\u00ea\u00eb"+
		"\7C\2\2\u00eb\u00ec\7n\2\2\u00ec\u00ed\7n\2\2\u00ed\u00ee\7q\2\2\u00ee"+
		"\u00ef\7y\2\2\u00ef\u00f0\7\"\2\2\u00f0\u00f1\7h\2\2\u00f1\u00f2\7q\2"+
		"\2\u00f2\u00f3\7t\2\2\u00f3\62\3\2\2\2\u00f4\u00f5\7T\2\2\u00f5\u00f6"+
		"\7g\2\2\u00f6\u00f7\7r\2\2\u00f7\u00f8\7n\2\2\u00f8\u00f9\7{\2\2\u00f9"+
		"\64\3\2\2\2\u00fa\u00fb\7W\2\2\u00fb\u00fc\7p\2\2\u00fc\u00fd\7v\2\2\u00fd"+
		"\u00fe\7k\2\2\u00fe\u00ff\7n\2\2\u00ff\66\3\2\2\2\u0100\u0101\7Q\2\2\u0101"+
		"\u0102\7x\2\2\u0102\u0103\7g\2\2\u0103\u0104\7t\2\2\u0104\u0105\7t\2\2"+
		"\u0105\u0106\7k\2\2\u0106\u0107\7f\2\2\u0107\u0108\7g\2\2\u0108\u0109"+
		"\7\"\2\2\u0109\u010a\7v\2\2\u010a\u010b\7g\2\2\u010b\u010c\7o\2\2\u010c"+
		"\u010d\7r\2\2\u010d\u010e\7n\2\2\u010e\u010f\7c\2\2\u010f\u0110\7v\2\2"+
		"\u0110\u0111\7g\2\2\u0111\u0112\7u\2\2\u01128\3\2\2\2\u0113\u0114\7Y\2"+
		"\2\u0114\u0115\7j\2\2\u0115\u0116\7g\2\2\u0116\u0117\7p\2\2\u0117:\3\2"+
		"\2\2\u0118\u0119\7Q\2\2\u0119\u011a\7v\2\2\u011a\u011b\7j\2\2\u011b\u011c"+
		"\7g\2\2\u011c\u011d\7t\2\2\u011d\u011e\7y\2\2\u011e\u011f\7k\2\2\u011f"+
		"\u0120\7u\2\2\u0120\u0121\7g\2\2\u0121<\3\2\2\2\u0122\u0123\7T\2\2\u0123"+
		"\u0124\7g\2\2\u0124\u0125\7r\2\2\u0125\u0126\7g\2\2\u0126\u0127\7c\2\2"+
		"\u0127\u0128\7v\2\2\u0128>\3\2\2\2\u0129\u012a\7K\2\2\u012a\u012b\7v\2"+
		"\2\u012b\u012c\7g\2\2\u012c\u012d\7t\2\2\u012d\u012e\7c\2\2\u012e\u012f"+
		"\7v\2\2\u012f\u0130\7g\2\2\u0130\u0131\7\"\2\2\u0131\u0132\7q\2\2\u0132"+
		"\u0133\7x\2\2\u0133\u0134\7g\2\2\u0134\u0135\7t\2\2\u0135@\3\2\2\2\u0136"+
		"\u0137\7O\2\2\u0137\u0138\7c\2\2\u0138\u0139\7v\2\2\u0139\u013a\7e\2\2"+
		"\u013a\u013b\7j\2\2\u013bB\3\2\2\2\u013c\u013d\7S\2\2\u013d\u013e\7w\2"+
		"\2\u013e\u013f\7k\2\2\u013f\u0140\7v\2\2\u0140D\3\2\2\2\u0141\u0142\7"+
		"H\2\2\u0142\u0143\7k\2\2\u0143\u0144\7p\2\2\u0144\u0145\7k\2\2\u0145\u0146"+
		"\7u\2\2\u0146\u0147\7j\2\2\u0147F\3\2\2\2\u0148\u0149\7T\2\2\u0149\u014a"+
		"\7H\2\2\u014a\u014b\7C\2\2\u014b\u014c\7E\2\2\u014cH\3\2\2\2\u014d\u014e"+
		"\7k\2\2\u014e\u014f\7u\2\2\u014fJ\3\2\2\2\u0150\u0151\7p\2\2\u0151\u0152"+
		"\7q\2\2\u0152\u0153\7v\2\2\u0153L\3\2\2\2\u0154\u0155\7c\2\2\u0155\u0156"+
		"\7p\2\2\u0156\u0157\7f\2\2\u0157N\3\2\2\2\u0158\u0159\7q\2\2\u0159\u015a"+
		"\7t\2\2\u015aP\3\2\2\2\u015b\u015c\7u\2\2\u015c\u015d\7g\2\2\u015d\u015e"+
		"\7e\2\2\u015e\u015f\7q\2\2\u015f\u0160\7p\2\2\u0160\u0161\7f\2\2\u0161"+
		"\u0162\7u\2\2\u0162R\3\2\2\2\u0163\u0164\7v\2\2\u0164\u0165\7q\2\2\u0165"+
		"T\3\2\2\2\u0166\u0167\7v\2\2\u0167\u0168\7k\2\2\u0168\u0169\7o\2\2\u0169"+
		"\u016a\7g\2\2\u016a\u016b\7u\2\2\u016b\u016c\7\"\2\2\u016c\u016d\7o\2"+
		"\2\u016d\u016e\7c\2\2\u016e\u016f\7z\2\2\u016fV\3\2\2\2\u0170\u0171\7"+
		"w\2\2\u0171\u0172\7u\2\2\u0172\u0173\7k\2\2\u0173\u0174\7p\2\2\u0174\u0175"+
		"\7i\2\2\u0175X\3\2\2\2\u0176\u0177\7?\2\2\u0177Z\3\2\2\2\u0178\u0179\7"+
		"/\2\2\u0179\\\3\2\2\2\u017a\u017b\7p\2\2\u017b\u017c\7w\2\2\u017c\u017d"+
		"\7n\2\2\u017d\u017e\7n\2\2\u017e^\3\2\2\2\u017f\u0180\7h\2\2\u0180\u0181"+
		"\7c\2\2\u0181\u0182\7n\2\2\u0182\u0183\7u\2\2\u0183\u0189\7g\2\2\u0184"+
		"\u0185\7v\2\2\u0185\u0186\7t\2\2\u0186\u0187\7w\2\2\u0187\u0189\7g\2\2"+
		"\u0188\u017f\3\2\2\2\u0188\u0184\3\2\2\2\u0189`\3\2\2\2\u018a\u018e\7"+
		"$\2\2\u018b\u018d\t\6\2\2\u018c\u018b\3\2\2\2\u018d\u0190\3\2\2\2\u018e"+
		"\u018c\3\2\2\2\u018e\u018f\3\2\2\2\u018f\u0191\3\2\2\2\u0190\u018e\3\2"+
		"\2\2\u0191\u0192\7$\2\2\u0192b\3\2\2\2\u0193\u0195\5\25\13\2\u0194\u0193"+
		"\3\2\2\2\u0195\u0196\3\2\2\2\u0196\u0194\3\2\2\2\u0196\u0197\3\2\2\2\u0197"+
		"d\3\2\2\2\u0198\u0199\5[.\2\u0199\u019a\5c\62\2\u019af\3\2\2\2\u019b\u019e"+
		"\5e\63\2\u019c\u019e\5c\62\2\u019d\u019b\3\2\2\2\u019d\u019c\3\2\2\2\u019e"+
		"\u019f\3\2\2\2\u019f\u01a0\7\60\2\2\u01a0\u01a1\5c\62\2\u01a1h\3\2\2\2"+
		"\u01a2\u01a3\5\31\r\2\u01a3j\3\2\2\2\u01a4\u01a9\5\31\r\2\u01a5\u01a6"+
		"\7\60\2\2\u01a6\u01a8\5\31\r\2\u01a7\u01a5\3\2\2\2\u01a8\u01ab\3\2\2\2"+
		"\u01a9\u01a7\3\2\2\2\u01a9\u01aa\3\2\2\2\u01aal\3\2\2\2\u01ab\u01a9\3"+
		"\2\2\2\u01ac\u01b2\7\60\2\2\u01ad\u01b3\5a\61\2\u01ae\u01b0\7&\2\2\u01af"+
		"\u01ae\3\2\2\2\u01af\u01b0\3\2\2\2\u01b0\u01b1\3\2\2\2\u01b1\u01b3\5\31"+
		"\r\2\u01b2\u01ad\3\2\2\2\u01b2\u01af\3\2\2\2\u01b3n\3\2\2\2\u01b4\u01b8"+
		"\5\31\r\2\u01b5\u01b7\5m\67\2\u01b6\u01b5\3\2\2\2\u01b7\u01ba\3\2\2\2"+
		"\u01b8\u01b6\3\2\2\2\u01b8\u01b9\3\2\2\2\u01b9p\3\2\2\2\u01ba\u01b8\3"+
		"\2\2\2\u01bb\u01ce\5o8\2\u01bc\u01be\7]\2\2\u01bd\u01bf\5\33\16\2\u01be"+
		"\u01bd\3\2\2\2\u01be\u01bf\3\2\2\2\u01bf\u01c2\3\2\2\2\u01c0\u01c3\5c"+
		"\62\2\u01c1\u01c3\5\31\r\2\u01c2\u01c0\3\2\2\2\u01c2\u01c1\3\2\2\2\u01c3"+
		"\u01c5\3\2\2\2\u01c4\u01c6\5\33\16\2\u01c5\u01c4\3\2\2\2\u01c5\u01c6\3"+
		"\2\2\2\u01c6\u01c7\3\2\2\2\u01c7\u01cb\7_\2\2\u01c8\u01ca\5m\67\2\u01c9"+
		"\u01c8\3\2\2\2\u01ca\u01cd\3\2\2\2\u01cb\u01c9\3\2\2\2\u01cb\u01cc\3\2"+
		"\2\2\u01cc\u01cf\3\2\2\2\u01cd\u01cb\3\2\2\2\u01ce\u01bc\3\2\2\2\u01cf"+
		"\u01d0\3\2\2\2\u01d0\u01ce\3\2\2\2\u01d0\u01d1\3\2\2\2\u01d1r\3\2\2\2"+
		"\u01d2\u01d4\5\33\16\2\u01d3\u01d2\3\2\2\2\u01d3\u01d4\3\2\2\2\u01d4\u01d8"+
		"\3\2\2\2\u01d5\u01d7\5\23\n\2\u01d6\u01d5\3\2\2\2\u01d7\u01da\3\2\2\2"+
		"\u01d8\u01d6\3\2\2\2\u01d8\u01d9\3\2\2\2\u01d9\u01db\3\2\2\2\u01da\u01d8"+
		"\3\2\2\2\u01db\u01dd\5\35\17\2\u01dc\u01de\5\33\16\2\u01dd\u01dc\3\2\2"+
		"\2\u01dd\u01de\3\2\2\2\u01de\u01e2\3\2\2\2\u01df\u01e1\5\23\n\2\u01e0"+
		"\u01df\3\2\2\2\u01e1\u01e4\3\2\2\2\u01e2\u01e0\3\2\2\2\u01e2\u01e3\3\2"+
		"\2\2\u01e3t\3\2\2\2\u01e4\u01e2\3\2\2\2\u01e5\u01e6\5\33\16\2\u01e6v\3"+
		"\2\2\2\32\2\u0088\u008e\u0099\u009b\u00a1\u00ab\u0188\u018e\u0196\u019d"+
		"\u01a9\u01af\u01b2\u01b8\u01be\u01c2\u01c5\u01cb\u01d0\u01d3\u01d8\u01dd"+
		"\u01e2\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}