// Generated from /home/tvaughan/code/beast_and_friends/beast2/src/beast/util/treeparser/Newick.g4 by ANTLR 4.5.1
package beast.util.treeparser;
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
public class NewickLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.5.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, FLOAT=11, INT=12, STRING=13, WHITESPACE=14;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
		"T__9", "FLOAT", "INT", "NNINT", "NZD", "D", "STRING", "WHITESPACE"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "';'", "'('", "','", "')'", "':'", "'[&'", "']'", "'='", "'{'", 
		"'}'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, "FLOAT", 
		"INT", "STRING", "WHITESPACE"
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
	public String getGrammarFileName() { return "Newick.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\20\u0081\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\7\3\b\3\b\3\t"+
		"\3\t\3\n\3\n\3\13\3\13\3\f\5\f<\n\f\3\f\3\f\3\f\7\fA\n\f\f\f\16\fD\13"+
		"\f\3\f\3\f\5\fH\n\f\3\f\6\fK\n\f\r\f\16\fL\5\fO\n\f\3\r\5\rR\n\r\3\r\3"+
		"\r\3\16\3\16\3\16\7\16Y\n\16\f\16\16\16\\\13\16\5\16^\n\16\3\17\3\17\3"+
		"\20\3\20\3\21\6\21e\n\21\r\21\16\21f\3\21\3\21\7\21k\n\21\f\21\16\21n"+
		"\13\21\3\21\3\21\3\21\7\21s\n\21\f\21\16\21v\13\21\3\21\5\21y\n\21\3\22"+
		"\6\22|\n\22\r\22\16\22}\3\22\3\22\4lt\2\23\3\3\5\4\7\5\t\6\13\7\r\b\17"+
		"\t\21\n\23\13\25\f\27\r\31\16\33\2\35\2\37\2!\17#\20\3\2\7\4\2GGgg\3\2"+
		"\63;\3\2\62;\t\2\'(,-/;C\\aac|~~\5\2\13\f\17\17\"\"\u008b\2\3\3\2\2\2"+
		"\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2"+
		"\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2"+
		"\2!\3\2\2\2\2#\3\2\2\2\3%\3\2\2\2\5\'\3\2\2\2\7)\3\2\2\2\t+\3\2\2\2\13"+
		"-\3\2\2\2\r/\3\2\2\2\17\62\3\2\2\2\21\64\3\2\2\2\23\66\3\2\2\2\258\3\2"+
		"\2\2\27;\3\2\2\2\31Q\3\2\2\2\33]\3\2\2\2\35_\3\2\2\2\37a\3\2\2\2!x\3\2"+
		"\2\2#{\3\2\2\2%&\7=\2\2&\4\3\2\2\2\'(\7*\2\2(\6\3\2\2\2)*\7.\2\2*\b\3"+
		"\2\2\2+,\7+\2\2,\n\3\2\2\2-.\7<\2\2.\f\3\2\2\2/\60\7]\2\2\60\61\7(\2\2"+
		"\61\16\3\2\2\2\62\63\7_\2\2\63\20\3\2\2\2\64\65\7?\2\2\65\22\3\2\2\2\66"+
		"\67\7}\2\2\67\24\3\2\2\289\7\177\2\29\26\3\2\2\2:<\7/\2\2;:\3\2\2\2;<"+
		"\3\2\2\2<=\3\2\2\2=>\5\33\16\2>B\7\60\2\2?A\5\37\20\2@?\3\2\2\2AD\3\2"+
		"\2\2B@\3\2\2\2BC\3\2\2\2CN\3\2\2\2DB\3\2\2\2EG\t\2\2\2FH\7/\2\2GF\3\2"+
		"\2\2GH\3\2\2\2HJ\3\2\2\2IK\5\37\20\2JI\3\2\2\2KL\3\2\2\2LJ\3\2\2\2LM\3"+
		"\2\2\2MO\3\2\2\2NE\3\2\2\2NO\3\2\2\2O\30\3\2\2\2PR\7/\2\2QP\3\2\2\2QR"+
		"\3\2\2\2RS\3\2\2\2ST\5\33\16\2T\32\3\2\2\2U^\7\62\2\2VZ\5\35\17\2WY\5"+
		"\37\20\2XW\3\2\2\2Y\\\3\2\2\2ZX\3\2\2\2Z[\3\2\2\2[^\3\2\2\2\\Z\3\2\2\2"+
		"]U\3\2\2\2]V\3\2\2\2^\34\3\2\2\2_`\t\3\2\2`\36\3\2\2\2ab\t\4\2\2b \3\2"+
		"\2\2ce\t\5\2\2dc\3\2\2\2ef\3\2\2\2fd\3\2\2\2fg\3\2\2\2gy\3\2\2\2hl\7$"+
		"\2\2ik\13\2\2\2ji\3\2\2\2kn\3\2\2\2lm\3\2\2\2lj\3\2\2\2mo\3\2\2\2nl\3"+
		"\2\2\2oy\7$\2\2pt\7)\2\2qs\13\2\2\2rq\3\2\2\2sv\3\2\2\2tu\3\2\2\2tr\3"+
		"\2\2\2uw\3\2\2\2vt\3\2\2\2wy\7)\2\2xd\3\2\2\2xh\3\2\2\2xp\3\2\2\2y\"\3"+
		"\2\2\2z|\t\6\2\2{z\3\2\2\2|}\3\2\2\2}{\3\2\2\2}~\3\2\2\2~\177\3\2\2\2"+
		"\177\u0080\b\22\2\2\u0080$\3\2\2\2\20\2;BGLNQZ]fltx}\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}