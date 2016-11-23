// Generated from /home/tvaughan/code/beast_and_friends/beast2/src/beast/util/treeparser/NewickLexer.g4 by ANTLR 4.5.3
package beast.util.treeparser;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class NewickLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.5.3", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		SEMI=1, COMMA=2, OPENP=3, CLOSEP=4, COLON=5, FLOAT_SCI=6, FLOAT=7, INT=8, 
		OPENA=9, STRING=10, WHITESPACE=11, EQ=12, ACOMMA=13, OPENV=14, CLOSEV=15, 
		AFLOAT_SCI=16, AFLOAT=17, AINT=18, ASTRING=19, CLOSEA=20, ATTRIBWS=21;
	public static final int ATTRIB_MODE = 1;
	public static String[] modeNames = {
		"DEFAULT_MODE", "ATTRIB_MODE"
	};

	public static final String[] ruleNames = {
		"SEMI", "COMMA", "OPENP", "CLOSEP", "COLON", "FLOAT_SCI", "FLOAT", "INT", 
		"NNINT", "NZD", "D", "OPENA", "STRING", "WHITESPACE", "EQ", "ACOMMA", 
		"OPENV", "CLOSEV", "AFLOAT_SCI", "AFLOAT", "AINT", "ASTRING", "CLOSEA", 
		"ATTRIBWS"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "';'", null, "'('", "')'", "':'", null, null, null, "'[&'", null, 
		null, "'='", null, "'{'", "'}'", null, null, null, null, "']'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "SEMI", "COMMA", "OPENP", "CLOSEP", "COLON", "FLOAT_SCI", "FLOAT", 
		"INT", "OPENA", "STRING", "WHITESPACE", "EQ", "ACOMMA", "OPENV", "CLOSEV", 
		"AFLOAT_SCI", "AFLOAT", "AINT", "ASTRING", "CLOSEA", "ATTRIBWS"
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


	public NewickLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "NewickLexer.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\27\u010f\b\1\b\1"+
		"\4\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t"+
		"\n\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4"+
		"\22\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4"+
		"\31\t\31\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\5\7@\n\7\3\7\5\7"+
		"C\n\7\3\7\3\7\6\7G\n\7\r\7\16\7H\3\7\3\7\3\7\6\7N\n\7\r\7\16\7O\5\7R\n"+
		"\7\5\7T\n\7\3\7\3\7\5\7X\n\7\3\7\6\7[\n\7\r\7\16\7\\\3\b\5\b`\n\b\3\b"+
		"\5\bc\n\b\3\b\3\b\6\bg\n\b\r\b\16\bh\3\b\3\b\3\b\7\bn\n\b\f\b\16\bq\13"+
		"\b\5\bs\n\b\3\t\5\tv\n\t\3\t\3\t\3\n\3\n\3\n\7\n}\n\n\f\n\16\n\u0080\13"+
		"\n\5\n\u0082\n\n\3\13\3\13\3\f\3\f\3\r\3\r\3\r\3\r\3\r\3\16\6\16\u008e"+
		"\n\16\r\16\16\16\u008f\3\16\3\16\7\16\u0094\n\16\f\16\16\16\u0097\13\16"+
		"\3\16\3\16\3\16\7\16\u009c\n\16\f\16\16\16\u009f\13\16\3\16\5\16\u00a2"+
		"\n\16\3\17\6\17\u00a5\n\17\r\17\16\17\u00a6\3\17\3\17\3\20\3\20\3\21\3"+
		"\21\3\22\3\22\3\23\3\23\3\24\5\24\u00b4\n\24\3\24\5\24\u00b7\n\24\3\24"+
		"\3\24\6\24\u00bb\n\24\r\24\16\24\u00bc\3\24\3\24\3\24\6\24\u00c2\n\24"+
		"\r\24\16\24\u00c3\5\24\u00c6\n\24\5\24\u00c8\n\24\3\24\3\24\5\24\u00cc"+
		"\n\24\3\24\6\24\u00cf\n\24\r\24\16\24\u00d0\3\25\5\25\u00d4\n\25\3\25"+
		"\5\25\u00d7\n\25\3\25\3\25\6\25\u00db\n\25\r\25\16\25\u00dc\3\25\3\25"+
		"\3\25\7\25\u00e2\n\25\f\25\16\25\u00e5\13\25\5\25\u00e7\n\25\3\26\5\26"+
		"\u00ea\n\26\3\26\3\26\3\27\6\27\u00ef\n\27\r\27\16\27\u00f0\3\27\3\27"+
		"\7\27\u00f5\n\27\f\27\16\27\u00f8\13\27\3\27\3\27\3\27\7\27\u00fd\n\27"+
		"\f\27\16\27\u0100\13\27\3\27\5\27\u0103\n\27\3\30\3\30\3\30\3\30\3\31"+
		"\6\31\u010a\n\31\r\31\16\31\u010b\3\31\3\31\6\u0095\u009d\u00f6\u00fe"+
		"\2\32\4\3\6\4\b\5\n\6\f\7\16\b\20\t\22\n\24\2\26\2\30\2\32\13\34\f\36"+
		"\r \16\"\17$\20&\21(\22*\23,\24.\25\60\26\62\27\4\2\3\b\4\2GGgg\3\2\63"+
		";\3\2\62;\6\2*+..<=]_\5\2\13\f\17\17\"\"\7\2..??]_}}\177\177\u0134\2\4"+
		"\3\2\2\2\2\6\3\2\2\2\2\b\3\2\2\2\2\n\3\2\2\2\2\f\3\2\2\2\2\16\3\2\2\2"+
		"\2\20\3\2\2\2\2\22\3\2\2\2\2\32\3\2\2\2\2\34\3\2\2\2\2\36\3\2\2\2\3 \3"+
		"\2\2\2\3\"\3\2\2\2\3$\3\2\2\2\3&\3\2\2\2\3(\3\2\2\2\3*\3\2\2\2\3,\3\2"+
		"\2\2\3.\3\2\2\2\3\60\3\2\2\2\3\62\3\2\2\2\4\64\3\2\2\2\6\66\3\2\2\2\b"+
		"8\3\2\2\2\n:\3\2\2\2\f<\3\2\2\2\16?\3\2\2\2\20_\3\2\2\2\22u\3\2\2\2\24"+
		"\u0081\3\2\2\2\26\u0083\3\2\2\2\30\u0085\3\2\2\2\32\u0087\3\2\2\2\34\u00a1"+
		"\3\2\2\2\36\u00a4\3\2\2\2 \u00aa\3\2\2\2\"\u00ac\3\2\2\2$\u00ae\3\2\2"+
		"\2&\u00b0\3\2\2\2(\u00b3\3\2\2\2*\u00d3\3\2\2\2,\u00e9\3\2\2\2.\u0102"+
		"\3\2\2\2\60\u0104\3\2\2\2\62\u0109\3\2\2\2\64\65\7=\2\2\65\5\3\2\2\2\66"+
		"\67\7.\2\2\67\7\3\2\2\289\7*\2\29\t\3\2\2\2:;\7+\2\2;\13\3\2\2\2<=\7<"+
		"\2\2=\r\3\2\2\2>@\7/\2\2?>\3\2\2\2?@\3\2\2\2@S\3\2\2\2AC\5\24\n\2BA\3"+
		"\2\2\2BC\3\2\2\2CD\3\2\2\2DF\7\60\2\2EG\5\30\f\2FE\3\2\2\2GH\3\2\2\2H"+
		"F\3\2\2\2HI\3\2\2\2IT\3\2\2\2JQ\5\24\n\2KM\7\60\2\2LN\5\30\f\2ML\3\2\2"+
		"\2NO\3\2\2\2OM\3\2\2\2OP\3\2\2\2PR\3\2\2\2QK\3\2\2\2QR\3\2\2\2RT\3\2\2"+
		"\2SB\3\2\2\2SJ\3\2\2\2TU\3\2\2\2UW\t\2\2\2VX\7/\2\2WV\3\2\2\2WX\3\2\2"+
		"\2XZ\3\2\2\2Y[\5\30\f\2ZY\3\2\2\2[\\\3\2\2\2\\Z\3\2\2\2\\]\3\2\2\2]\17"+
		"\3\2\2\2^`\7/\2\2_^\3\2\2\2_`\3\2\2\2`r\3\2\2\2ac\5\24\n\2ba\3\2\2\2b"+
		"c\3\2\2\2cd\3\2\2\2df\7\60\2\2eg\5\30\f\2fe\3\2\2\2gh\3\2\2\2hf\3\2\2"+
		"\2hi\3\2\2\2is\3\2\2\2jk\5\24\n\2ko\7\60\2\2ln\5\30\f\2ml\3\2\2\2nq\3"+
		"\2\2\2om\3\2\2\2op\3\2\2\2ps\3\2\2\2qo\3\2\2\2rb\3\2\2\2rj\3\2\2\2s\21"+
		"\3\2\2\2tv\7/\2\2ut\3\2\2\2uv\3\2\2\2vw\3\2\2\2wx\5\24\n\2x\23\3\2\2\2"+
		"y\u0082\7\62\2\2z~\5\26\13\2{}\5\30\f\2|{\3\2\2\2}\u0080\3\2\2\2~|\3\2"+
		"\2\2~\177\3\2\2\2\177\u0082\3\2\2\2\u0080~\3\2\2\2\u0081y\3\2\2\2\u0081"+
		"z\3\2\2\2\u0082\25\3\2\2\2\u0083\u0084\t\3\2\2\u0084\27\3\2\2\2\u0085"+
		"\u0086\t\4\2\2\u0086\31\3\2\2\2\u0087\u0088\7]\2\2\u0088\u0089\7(\2\2"+
		"\u0089\u008a\3\2\2\2\u008a\u008b\b\r\2\2\u008b\33\3\2\2\2\u008c\u008e"+
		"\n\5\2\2\u008d\u008c\3\2\2\2\u008e\u008f\3\2\2\2\u008f\u008d\3\2\2\2\u008f"+
		"\u0090\3\2\2\2\u0090\u00a2\3\2\2\2\u0091\u0095\7$\2\2\u0092\u0094\13\2"+
		"\2\2\u0093\u0092\3\2\2\2\u0094\u0097\3\2\2\2\u0095\u0096\3\2\2\2\u0095"+
		"\u0093\3\2\2\2\u0096\u0098\3\2\2\2\u0097\u0095\3\2\2\2\u0098\u00a2\7$"+
		"\2\2\u0099\u009d\7)\2\2\u009a\u009c\13\2\2\2\u009b\u009a\3\2\2\2\u009c"+
		"\u009f\3\2\2\2\u009d\u009e\3\2\2\2\u009d\u009b\3\2\2\2\u009e\u00a0\3\2"+
		"\2\2\u009f\u009d\3\2\2\2\u00a0\u00a2\7)\2\2\u00a1\u008d\3\2\2\2\u00a1"+
		"\u0091\3\2\2\2\u00a1\u0099\3\2\2\2\u00a2\35\3\2\2\2\u00a3\u00a5\t\6\2"+
		"\2\u00a4\u00a3\3\2\2\2\u00a5\u00a6\3\2\2\2\u00a6\u00a4\3\2\2\2\u00a6\u00a7"+
		"\3\2\2\2\u00a7\u00a8\3\2\2\2\u00a8\u00a9\b\17\3\2\u00a9\37\3\2\2\2\u00aa"+
		"\u00ab\7?\2\2\u00ab!\3\2\2\2\u00ac\u00ad\7.\2\2\u00ad#\3\2\2\2\u00ae\u00af"+
		"\7}\2\2\u00af%\3\2\2\2\u00b0\u00b1\7\177\2\2\u00b1\'\3\2\2\2\u00b2\u00b4"+
		"\7/\2\2\u00b3\u00b2\3\2\2\2\u00b3\u00b4\3\2\2\2\u00b4\u00c7\3\2\2\2\u00b5"+
		"\u00b7\5\24\n\2\u00b6\u00b5\3\2\2\2\u00b6\u00b7\3\2\2\2\u00b7\u00b8\3"+
		"\2\2\2\u00b8\u00ba\7\60\2\2\u00b9\u00bb\5\30\f\2\u00ba\u00b9\3\2\2\2\u00bb"+
		"\u00bc\3\2\2\2\u00bc\u00ba\3\2\2\2\u00bc\u00bd\3\2\2\2\u00bd\u00c8\3\2"+
		"\2\2\u00be\u00c5\5\24\n\2\u00bf\u00c1\7\60\2\2\u00c0\u00c2\5\30\f\2\u00c1"+
		"\u00c0\3\2\2\2\u00c2\u00c3\3\2\2\2\u00c3\u00c1\3\2\2\2\u00c3\u00c4\3\2"+
		"\2\2\u00c4\u00c6\3\2\2\2\u00c5\u00bf\3\2\2\2\u00c5\u00c6\3\2\2\2\u00c6"+
		"\u00c8\3\2\2\2\u00c7\u00b6\3\2\2\2\u00c7\u00be\3\2\2\2\u00c8\u00c9\3\2"+
		"\2\2\u00c9\u00cb\t\2\2\2\u00ca\u00cc\7/\2\2\u00cb\u00ca\3\2\2\2\u00cb"+
		"\u00cc\3\2\2\2\u00cc\u00ce\3\2\2\2\u00cd\u00cf\5\30\f\2\u00ce\u00cd\3"+
		"\2\2\2\u00cf\u00d0\3\2\2\2\u00d0\u00ce\3\2\2\2\u00d0\u00d1\3\2\2\2\u00d1"+
		")\3\2\2\2\u00d2\u00d4\7/\2\2\u00d3\u00d2\3\2\2\2\u00d3\u00d4\3\2\2\2\u00d4"+
		"\u00e6\3\2\2\2\u00d5\u00d7\5\24\n\2\u00d6\u00d5\3\2\2\2\u00d6\u00d7\3"+
		"\2\2\2\u00d7\u00d8\3\2\2\2\u00d8\u00da\7\60\2\2\u00d9\u00db\5\30\f\2\u00da"+
		"\u00d9\3\2\2\2\u00db\u00dc\3\2\2\2\u00dc\u00da\3\2\2\2\u00dc\u00dd\3\2"+
		"\2\2\u00dd\u00e7\3\2\2\2\u00de\u00df\5\24\n\2\u00df\u00e3\7\60\2\2\u00e0"+
		"\u00e2\5\30\f\2\u00e1\u00e0\3\2\2\2\u00e2\u00e5\3\2\2\2\u00e3\u00e1\3"+
		"\2\2\2\u00e3\u00e4\3\2\2\2\u00e4\u00e7\3\2\2\2\u00e5\u00e3\3\2\2\2\u00e6"+
		"\u00d6\3\2\2\2\u00e6\u00de\3\2\2\2\u00e7+\3\2\2\2\u00e8\u00ea\7/\2\2\u00e9"+
		"\u00e8\3\2\2\2\u00e9\u00ea\3\2\2\2\u00ea\u00eb\3\2\2\2\u00eb\u00ec\5\24"+
		"\n\2\u00ec-\3\2\2\2\u00ed\u00ef\n\7\2\2\u00ee\u00ed\3\2\2\2\u00ef\u00f0"+
		"\3\2\2\2\u00f0\u00ee\3\2\2\2\u00f0\u00f1\3\2\2\2\u00f1\u0103\3\2\2\2\u00f2"+
		"\u00f6\7$\2\2\u00f3\u00f5\13\2\2\2\u00f4\u00f3\3\2\2\2\u00f5\u00f8\3\2"+
		"\2\2\u00f6\u00f7\3\2\2\2\u00f6\u00f4\3\2\2\2\u00f7\u00f9\3\2\2\2\u00f8"+
		"\u00f6\3\2\2\2\u00f9\u0103\7$\2\2\u00fa\u00fe\7)\2\2\u00fb\u00fd\13\2"+
		"\2\2\u00fc\u00fb\3\2\2\2\u00fd\u0100\3\2\2\2\u00fe\u00ff\3\2\2\2\u00fe"+
		"\u00fc\3\2\2\2\u00ff\u0101\3\2\2\2\u0100\u00fe\3\2\2\2\u0101\u0103\7)"+
		"\2\2\u0102\u00ee\3\2\2\2\u0102\u00f2\3\2\2\2\u0102\u00fa\3\2\2\2\u0103"+
		"/\3\2\2\2\u0104\u0105\7_\2\2\u0105\u0106\3\2\2\2\u0106\u0107\b\30\4\2"+
		"\u0107\61\3\2\2\2\u0108\u010a\t\6\2\2\u0109\u0108\3\2\2\2\u010a\u010b"+
		"\3\2\2\2\u010b\u0109\3\2\2\2\u010b\u010c\3\2\2\2\u010c\u010d\3\2\2\2\u010d"+
		"\u010e\b\31\3\2\u010e\63\3\2\2\2,\2\3?BHOQSW\\_bhoru~\u0081\u008f\u0095"+
		"\u009d\u00a1\u00a6\u00b3\u00b6\u00bc\u00c3\u00c5\u00c7\u00cb\u00d0\u00d3"+
		"\u00d6\u00dc\u00e3\u00e6\u00e9\u00f0\u00f6\u00fe\u0102\u010b\5\4\3\2\b"+
		"\2\2\4\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}