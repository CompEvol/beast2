// Generated from /home/tvaughan/code/beast_and_friends/beast2/src/beast/util/treeparser/Newick.g4 by ANTLR 4.5.3
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
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, FLOAT_SCI=11, FLOAT=12, INT=13, STRING=14, WHITESPACE=15;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
		"T__9", "FLOAT_SCI", "FLOAT", "INT", "NNINT", "NZD", "D", "STRING", "WHITESPACE"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "';'", "'('", "','", "')'", "':'", "'[&'", "']'", "'='", "'{'", 
		"'}'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, "FLOAT_SCI", 
		"FLOAT", "INT", "STRING", "WHITESPACE"
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
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\21\u00a3\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\7\3"+
		"\b\3\b\3\t\3\t\3\n\3\n\3\13\3\13\3\f\5\f>\n\f\3\f\5\fA\n\f\3\f\3\f\6\f"+
		"E\n\f\r\f\16\fF\3\f\3\f\3\f\6\fL\n\f\r\f\16\fM\5\fP\n\f\5\fR\n\f\3\f\3"+
		"\f\5\fV\n\f\3\f\6\fY\n\f\r\f\16\fZ\3\r\5\r^\n\r\3\r\5\ra\n\r\3\r\3\r\6"+
		"\re\n\r\r\r\16\rf\3\r\3\r\3\r\7\rl\n\r\f\r\16\ro\13\r\5\rq\n\r\3\16\5"+
		"\16t\n\16\3\16\3\16\3\17\3\17\3\17\7\17{\n\17\f\17\16\17~\13\17\5\17\u0080"+
		"\n\17\3\20\3\20\3\21\3\21\3\22\6\22\u0087\n\22\r\22\16\22\u0088\3\22\3"+
		"\22\7\22\u008d\n\22\f\22\16\22\u0090\13\22\3\22\3\22\3\22\7\22\u0095\n"+
		"\22\f\22\16\22\u0098\13\22\3\22\5\22\u009b\n\22\3\23\6\23\u009e\n\23\r"+
		"\23\16\23\u009f\3\23\3\23\4\u008e\u0096\2\24\3\3\5\4\7\5\t\6\13\7\r\b"+
		"\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\2\37\2!\2#\20%\21\3\2\7\4\2"+
		"GGgg\3\2\63;\3\2\62;\n\2%%\'(,-/;C\\aac|~~\5\2\13\f\17\17\"\"\u00b5\2"+
		"\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2"+
		"\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2"+
		"\31\3\2\2\2\2\33\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\3\'\3\2\2\2\5)\3\2\2\2"+
		"\7+\3\2\2\2\t-\3\2\2\2\13/\3\2\2\2\r\61\3\2\2\2\17\64\3\2\2\2\21\66\3"+
		"\2\2\2\238\3\2\2\2\25:\3\2\2\2\27=\3\2\2\2\31]\3\2\2\2\33s\3\2\2\2\35"+
		"\177\3\2\2\2\37\u0081\3\2\2\2!\u0083\3\2\2\2#\u009a\3\2\2\2%\u009d\3\2"+
		"\2\2\'(\7=\2\2(\4\3\2\2\2)*\7*\2\2*\6\3\2\2\2+,\7.\2\2,\b\3\2\2\2-.\7"+
		"+\2\2.\n\3\2\2\2/\60\7<\2\2\60\f\3\2\2\2\61\62\7]\2\2\62\63\7(\2\2\63"+
		"\16\3\2\2\2\64\65\7_\2\2\65\20\3\2\2\2\66\67\7?\2\2\67\22\3\2\2\289\7"+
		"}\2\29\24\3\2\2\2:;\7\177\2\2;\26\3\2\2\2<>\7/\2\2=<\3\2\2\2=>\3\2\2\2"+
		">Q\3\2\2\2?A\5\35\17\2@?\3\2\2\2@A\3\2\2\2AB\3\2\2\2BD\7\60\2\2CE\5!\21"+
		"\2DC\3\2\2\2EF\3\2\2\2FD\3\2\2\2FG\3\2\2\2GR\3\2\2\2HO\5\35\17\2IK\7\60"+
		"\2\2JL\5!\21\2KJ\3\2\2\2LM\3\2\2\2MK\3\2\2\2MN\3\2\2\2NP\3\2\2\2OI\3\2"+
		"\2\2OP\3\2\2\2PR\3\2\2\2Q@\3\2\2\2QH\3\2\2\2RS\3\2\2\2SU\t\2\2\2TV\7/"+
		"\2\2UT\3\2\2\2UV\3\2\2\2VX\3\2\2\2WY\5!\21\2XW\3\2\2\2YZ\3\2\2\2ZX\3\2"+
		"\2\2Z[\3\2\2\2[\30\3\2\2\2\\^\7/\2\2]\\\3\2\2\2]^\3\2\2\2^p\3\2\2\2_a"+
		"\5\35\17\2`_\3\2\2\2`a\3\2\2\2ab\3\2\2\2bd\7\60\2\2ce\5!\21\2dc\3\2\2"+
		"\2ef\3\2\2\2fd\3\2\2\2fg\3\2\2\2gq\3\2\2\2hi\5\35\17\2im\7\60\2\2jl\5"+
		"!\21\2kj\3\2\2\2lo\3\2\2\2mk\3\2\2\2mn\3\2\2\2nq\3\2\2\2om\3\2\2\2p`\3"+
		"\2\2\2ph\3\2\2\2q\32\3\2\2\2rt\7/\2\2sr\3\2\2\2st\3\2\2\2tu\3\2\2\2uv"+
		"\5\35\17\2v\34\3\2\2\2w\u0080\7\62\2\2x|\5\37\20\2y{\5!\21\2zy\3\2\2\2"+
		"{~\3\2\2\2|z\3\2\2\2|}\3\2\2\2}\u0080\3\2\2\2~|\3\2\2\2\177w\3\2\2\2\177"+
		"x\3\2\2\2\u0080\36\3\2\2\2\u0081\u0082\t\3\2\2\u0082 \3\2\2\2\u0083\u0084"+
		"\t\4\2\2\u0084\"\3\2\2\2\u0085\u0087\t\5\2\2\u0086\u0085\3\2\2\2\u0087"+
		"\u0088\3\2\2\2\u0088\u0086\3\2\2\2\u0088\u0089\3\2\2\2\u0089\u009b\3\2"+
		"\2\2\u008a\u008e\7$\2\2\u008b\u008d\13\2\2\2\u008c\u008b\3\2\2\2\u008d"+
		"\u0090\3\2\2\2\u008e\u008f\3\2\2\2\u008e\u008c\3\2\2\2\u008f\u0091\3\2"+
		"\2\2\u0090\u008e\3\2\2\2\u0091\u009b\7$\2\2\u0092\u0096\7)\2\2\u0093\u0095"+
		"\13\2\2\2\u0094\u0093\3\2\2\2\u0095\u0098\3\2\2\2\u0096\u0097\3\2\2\2"+
		"\u0096\u0094\3\2\2\2\u0097\u0099\3\2\2\2\u0098\u0096\3\2\2\2\u0099\u009b"+
		"\7)\2\2\u009a\u0086\3\2\2\2\u009a\u008a\3\2\2\2\u009a\u0092\3\2\2\2\u009b"+
		"$\3\2\2\2\u009c\u009e\t\6\2\2\u009d\u009c\3\2\2\2\u009e\u009f\3\2\2\2"+
		"\u009f\u009d\3\2\2\2\u009f\u00a0\3\2\2\2\u00a0\u00a1\3\2\2\2\u00a1\u00a2"+
		"\b\23\2\2\u00a2&\3\2\2\2\30\2=@FMOQUZ]`fmps|\177\u0088\u008e\u0096\u009a"+
		"\u009f\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}