// Generated from /Users/vaughant/code/beast_and_friends/beast2/src/beast/util/treeparser/NewickLexer.g4 by ANTLR 4.7
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
	static { RuntimeMetaData.checkVersion("4.7", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		SEMI=1, COMMA=2, OPENP=3, CLOSEP=4, COLON=5, FLOAT_SCI=6, FLOAT=7, INT=8, 
		OPENA=9, WHITESPACE=10, STRING=11, EQ=12, ACOMMA=13, OPENV=14, CLOSEV=15, 
		AFLOAT_SCI=16, AFLOAT=17, AINT=18, AWHITESPACE=19, ASTRING=20, CLOSEA=21, 
		ATTRIBWS=22;
	public static final int
		ATTRIB_MODE=1;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE", "ATTRIB_MODE"
	};

	public static final String[] ruleNames = {
		"SEMI", "COMMA", "OPENP", "CLOSEP", "COLON", "FLOAT_SCI", "FLOAT", "INT", 
		"NNINT", "NZD", "D", "OPENA", "WHITESPACE", "STRING", "EQ", "ACOMMA", 
		"OPENV", "CLOSEV", "AFLOAT_SCI", "AFLOAT", "AINT", "AWHITESPACE", "ASTRING", 
		"CLOSEA", "ATTRIBWS"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "';'", null, "'('", "')'", "':'", null, null, null, "'[&'", null, 
		null, "'='", null, "'{'", "'}'", null, null, null, null, null, "']'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "SEMI", "COMMA", "OPENP", "CLOSEP", "COLON", "FLOAT_SCI", "FLOAT", 
		"INT", "OPENA", "WHITESPACE", "STRING", "EQ", "ACOMMA", "OPENV", "CLOSEV", 
		"AFLOAT_SCI", "AFLOAT", "AINT", "AWHITESPACE", "ASTRING", "CLOSEA", "ATTRIBWS"
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
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\30\u0118\b\1\b\1"+
		"\4\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t"+
		"\n\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4"+
		"\22\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4"+
		"\31\t\31\4\32\t\32\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\5\7B\n"+
		"\7\3\7\5\7E\n\7\3\7\3\7\6\7I\n\7\r\7\16\7J\3\7\3\7\3\7\6\7P\n\7\r\7\16"+
		"\7Q\5\7T\n\7\5\7V\n\7\3\7\3\7\5\7Z\n\7\3\7\6\7]\n\7\r\7\16\7^\3\b\5\b"+
		"b\n\b\3\b\5\be\n\b\3\b\3\b\6\bi\n\b\r\b\16\bj\3\b\3\b\3\b\7\bp\n\b\f\b"+
		"\16\bs\13\b\5\bu\n\b\3\t\5\tx\n\t\3\t\3\t\3\n\3\n\3\n\7\n\177\n\n\f\n"+
		"\16\n\u0082\13\n\5\n\u0084\n\n\3\13\3\13\3\f\3\f\3\r\3\r\3\r\3\r\3\r\3"+
		"\16\6\16\u0090\n\16\r\16\16\16\u0091\3\16\3\16\3\17\6\17\u0097\n\17\r"+
		"\17\16\17\u0098\3\17\3\17\7\17\u009d\n\17\f\17\16\17\u00a0\13\17\3\17"+
		"\3\17\3\17\7\17\u00a5\n\17\f\17\16\17\u00a8\13\17\3\17\5\17\u00ab\n\17"+
		"\3\20\3\20\3\21\3\21\3\22\3\22\3\23\3\23\3\24\5\24\u00b6\n\24\3\24\5\24"+
		"\u00b9\n\24\3\24\3\24\6\24\u00bd\n\24\r\24\16\24\u00be\3\24\3\24\3\24"+
		"\6\24\u00c4\n\24\r\24\16\24\u00c5\5\24\u00c8\n\24\5\24\u00ca\n\24\3\24"+
		"\3\24\5\24\u00ce\n\24\3\24\6\24\u00d1\n\24\r\24\16\24\u00d2\3\25\5\25"+
		"\u00d6\n\25\3\25\5\25\u00d9\n\25\3\25\3\25\6\25\u00dd\n\25\r\25\16\25"+
		"\u00de\3\25\3\25\3\25\7\25\u00e4\n\25\f\25\16\25\u00e7\13\25\5\25\u00e9"+
		"\n\25\3\26\5\26\u00ec\n\26\3\26\3\26\3\27\6\27\u00f1\n\27\r\27\16\27\u00f2"+
		"\3\27\3\27\3\30\6\30\u00f8\n\30\r\30\16\30\u00f9\3\30\3\30\7\30\u00fe"+
		"\n\30\f\30\16\30\u0101\13\30\3\30\3\30\3\30\7\30\u0106\n\30\f\30\16\30"+
		"\u0109\13\30\3\30\5\30\u010c\n\30\3\31\3\31\3\31\3\31\3\32\6\32\u0113"+
		"\n\32\r\32\16\32\u0114\3\32\3\32\6\u009e\u00a6\u00ff\u0107\2\33\4\3\6"+
		"\4\b\5\n\6\f\7\16\b\20\t\22\n\24\2\26\2\30\2\32\13\34\f\36\r \16\"\17"+
		"$\20&\21(\22*\23,\24.\25\60\26\62\27\64\30\4\2\3\t\4\2GGgg\4\2--//\3\2"+
		"\63;\3\2\62;\5\2\13\f\17\17\"\"\n\2%%\'(,-/;C\\aac|~~\n\2%%\'(,-/<C\\"+
		"aac|~~\2\u013e\2\4\3\2\2\2\2\6\3\2\2\2\2\b\3\2\2\2\2\n\3\2\2\2\2\f\3\2"+
		"\2\2\2\16\3\2\2\2\2\20\3\2\2\2\2\22\3\2\2\2\2\32\3\2\2\2\2\34\3\2\2\2"+
		"\2\36\3\2\2\2\3 \3\2\2\2\3\"\3\2\2\2\3$\3\2\2\2\3&\3\2\2\2\3(\3\2\2\2"+
		"\3*\3\2\2\2\3,\3\2\2\2\3.\3\2\2\2\3\60\3\2\2\2\3\62\3\2\2\2\3\64\3\2\2"+
		"\2\4\66\3\2\2\2\68\3\2\2\2\b:\3\2\2\2\n<\3\2\2\2\f>\3\2\2\2\16A\3\2\2"+
		"\2\20a\3\2\2\2\22w\3\2\2\2\24\u0083\3\2\2\2\26\u0085\3\2\2\2\30\u0087"+
		"\3\2\2\2\32\u0089\3\2\2\2\34\u008f\3\2\2\2\36\u00aa\3\2\2\2 \u00ac\3\2"+
		"\2\2\"\u00ae\3\2\2\2$\u00b0\3\2\2\2&\u00b2\3\2\2\2(\u00b5\3\2\2\2*\u00d5"+
		"\3\2\2\2,\u00eb\3\2\2\2.\u00f0\3\2\2\2\60\u010b\3\2\2\2\62\u010d\3\2\2"+
		"\2\64\u0112\3\2\2\2\66\67\7=\2\2\67\5\3\2\2\289\7.\2\29\7\3\2\2\2:;\7"+
		"*\2\2;\t\3\2\2\2<=\7+\2\2=\13\3\2\2\2>?\7<\2\2?\r\3\2\2\2@B\7/\2\2A@\3"+
		"\2\2\2AB\3\2\2\2BU\3\2\2\2CE\5\24\n\2DC\3\2\2\2DE\3\2\2\2EF\3\2\2\2FH"+
		"\7\60\2\2GI\5\30\f\2HG\3\2\2\2IJ\3\2\2\2JH\3\2\2\2JK\3\2\2\2KV\3\2\2\2"+
		"LS\5\24\n\2MO\7\60\2\2NP\5\30\f\2ON\3\2\2\2PQ\3\2\2\2QO\3\2\2\2QR\3\2"+
		"\2\2RT\3\2\2\2SM\3\2\2\2ST\3\2\2\2TV\3\2\2\2UD\3\2\2\2UL\3\2\2\2VW\3\2"+
		"\2\2WY\t\2\2\2XZ\t\3\2\2YX\3\2\2\2YZ\3\2\2\2Z\\\3\2\2\2[]\5\30\f\2\\["+
		"\3\2\2\2]^\3\2\2\2^\\\3\2\2\2^_\3\2\2\2_\17\3\2\2\2`b\7/\2\2a`\3\2\2\2"+
		"ab\3\2\2\2bt\3\2\2\2ce\5\24\n\2dc\3\2\2\2de\3\2\2\2ef\3\2\2\2fh\7\60\2"+
		"\2gi\5\30\f\2hg\3\2\2\2ij\3\2\2\2jh\3\2\2\2jk\3\2\2\2ku\3\2\2\2lm\5\24"+
		"\n\2mq\7\60\2\2np\5\30\f\2on\3\2\2\2ps\3\2\2\2qo\3\2\2\2qr\3\2\2\2ru\3"+
		"\2\2\2sq\3\2\2\2td\3\2\2\2tl\3\2\2\2u\21\3\2\2\2vx\7/\2\2wv\3\2\2\2wx"+
		"\3\2\2\2xy\3\2\2\2yz\5\24\n\2z\23\3\2\2\2{\u0084\7\62\2\2|\u0080\5\26"+
		"\13\2}\177\5\30\f\2~}\3\2\2\2\177\u0082\3\2\2\2\u0080~\3\2\2\2\u0080\u0081"+
		"\3\2\2\2\u0081\u0084\3\2\2\2\u0082\u0080\3\2\2\2\u0083{\3\2\2\2\u0083"+
		"|\3\2\2\2\u0084\25\3\2\2\2\u0085\u0086\t\4\2\2\u0086\27\3\2\2\2\u0087"+
		"\u0088\t\5\2\2\u0088\31\3\2\2\2\u0089\u008a\7]\2\2\u008a\u008b\7(\2\2"+
		"\u008b\u008c\3\2\2\2\u008c\u008d\b\r\2\2\u008d\33\3\2\2\2\u008e\u0090"+
		"\t\6\2\2\u008f\u008e\3\2\2\2\u0090\u0091\3\2\2\2\u0091\u008f\3\2\2\2\u0091"+
		"\u0092\3\2\2\2\u0092\u0093\3\2\2\2\u0093\u0094\b\16\3\2\u0094\35\3\2\2"+
		"\2\u0095\u0097\t\7\2\2\u0096\u0095\3\2\2\2\u0097\u0098\3\2\2\2\u0098\u0096"+
		"\3\2\2\2\u0098\u0099\3\2\2\2\u0099\u00ab\3\2\2\2\u009a\u009e\7$\2\2\u009b"+
		"\u009d\13\2\2\2\u009c\u009b\3\2\2\2\u009d\u00a0\3\2\2\2\u009e\u009f\3"+
		"\2\2\2\u009e\u009c\3\2\2\2\u009f\u00a1\3\2\2\2\u00a0\u009e\3\2\2\2\u00a1"+
		"\u00ab\7$\2\2\u00a2\u00a6\7)\2\2\u00a3\u00a5\13\2\2\2\u00a4\u00a3\3\2"+
		"\2\2\u00a5\u00a8\3\2\2\2\u00a6\u00a7\3\2\2\2\u00a6\u00a4\3\2\2\2\u00a7"+
		"\u00a9\3\2\2\2\u00a8\u00a6\3\2\2\2\u00a9\u00ab\7)\2\2\u00aa\u0096\3\2"+
		"\2\2\u00aa\u009a\3\2\2\2\u00aa\u00a2\3\2\2\2\u00ab\37\3\2\2\2\u00ac\u00ad"+
		"\7?\2\2\u00ad!\3\2\2\2\u00ae\u00af\7.\2\2\u00af#\3\2\2\2\u00b0\u00b1\7"+
		"}\2\2\u00b1%\3\2\2\2\u00b2\u00b3\7\177\2\2\u00b3\'\3\2\2\2\u00b4\u00b6"+
		"\7/\2\2\u00b5\u00b4\3\2\2\2\u00b5\u00b6\3\2\2\2\u00b6\u00c9\3\2\2\2\u00b7"+
		"\u00b9\5\24\n\2\u00b8\u00b7\3\2\2\2\u00b8\u00b9\3\2\2\2\u00b9\u00ba\3"+
		"\2\2\2\u00ba\u00bc\7\60\2\2\u00bb\u00bd\5\30\f\2\u00bc\u00bb\3\2\2\2\u00bd"+
		"\u00be\3\2\2\2\u00be\u00bc\3\2\2\2\u00be\u00bf\3\2\2\2\u00bf\u00ca\3\2"+
		"\2\2\u00c0\u00c7\5\24\n\2\u00c1\u00c3\7\60\2\2\u00c2\u00c4\5\30\f\2\u00c3"+
		"\u00c2\3\2\2\2\u00c4\u00c5\3\2\2\2\u00c5\u00c3\3\2\2\2\u00c5\u00c6\3\2"+
		"\2\2\u00c6\u00c8\3\2\2\2\u00c7\u00c1\3\2\2\2\u00c7\u00c8\3\2\2\2\u00c8"+
		"\u00ca\3\2\2\2\u00c9\u00b8\3\2\2\2\u00c9\u00c0\3\2\2\2\u00ca\u00cb\3\2"+
		"\2\2\u00cb\u00cd\t\2\2\2\u00cc\u00ce\7/\2\2\u00cd\u00cc\3\2\2\2\u00cd"+
		"\u00ce\3\2\2\2\u00ce\u00d0\3\2\2\2\u00cf\u00d1\5\30\f\2\u00d0\u00cf\3"+
		"\2\2\2\u00d1\u00d2\3\2\2\2\u00d2\u00d0\3\2\2\2\u00d2\u00d3\3\2\2\2\u00d3"+
		")\3\2\2\2\u00d4\u00d6\7/\2\2\u00d5\u00d4\3\2\2\2\u00d5\u00d6\3\2\2\2\u00d6"+
		"\u00e8\3\2\2\2\u00d7\u00d9\5\24\n\2\u00d8\u00d7\3\2\2\2\u00d8\u00d9\3"+
		"\2\2\2\u00d9\u00da\3\2\2\2\u00da\u00dc\7\60\2\2\u00db\u00dd\5\30\f\2\u00dc"+
		"\u00db\3\2\2\2\u00dd\u00de\3\2\2\2\u00de\u00dc\3\2\2\2\u00de\u00df\3\2"+
		"\2\2\u00df\u00e9\3\2\2\2\u00e0\u00e1\5\24\n\2\u00e1\u00e5\7\60\2\2\u00e2"+
		"\u00e4\5\30\f\2\u00e3\u00e2\3\2\2\2\u00e4\u00e7\3\2\2\2\u00e5\u00e3\3"+
		"\2\2\2\u00e5\u00e6\3\2\2\2\u00e6\u00e9\3\2\2\2\u00e7\u00e5\3\2\2\2\u00e8"+
		"\u00d8\3\2\2\2\u00e8\u00e0\3\2\2\2\u00e9+\3\2\2\2\u00ea\u00ec\7/\2\2\u00eb"+
		"\u00ea\3\2\2\2\u00eb\u00ec\3\2\2\2\u00ec\u00ed\3\2\2\2\u00ed\u00ee\5\24"+
		"\n\2\u00ee-\3\2\2\2\u00ef\u00f1\t\6\2\2\u00f0\u00ef\3\2\2\2\u00f1\u00f2"+
		"\3\2\2\2\u00f2\u00f0\3\2\2\2\u00f2\u00f3\3\2\2\2\u00f3\u00f4\3\2\2\2\u00f4"+
		"\u00f5\b\27\3\2\u00f5/\3\2\2\2\u00f6\u00f8\t\b\2\2\u00f7\u00f6\3\2\2\2"+
		"\u00f8\u00f9\3\2\2\2\u00f9\u00f7\3\2\2\2\u00f9\u00fa\3\2\2\2\u00fa\u010c"+
		"\3\2\2\2\u00fb\u00ff\7$\2\2\u00fc\u00fe\13\2\2\2\u00fd\u00fc\3\2\2\2\u00fe"+
		"\u0101\3\2\2\2\u00ff\u0100\3\2\2\2\u00ff\u00fd\3\2\2\2\u0100\u0102\3\2"+
		"\2\2\u0101\u00ff\3\2\2\2\u0102\u010c\7$\2\2\u0103\u0107\7)\2\2\u0104\u0106"+
		"\13\2\2\2\u0105\u0104\3\2\2\2\u0106\u0109\3\2\2\2\u0107\u0108\3\2\2\2"+
		"\u0107\u0105\3\2\2\2\u0108\u010a\3\2\2\2\u0109\u0107\3\2\2\2\u010a\u010c"+
		"\7)\2\2\u010b\u00f7\3\2\2\2\u010b\u00fb\3\2\2\2\u010b\u0103\3\2\2\2\u010c"+
		"\61\3\2\2\2\u010d\u010e\7_\2\2\u010e\u010f\3\2\2\2\u010f\u0110\b\31\4"+
		"\2\u0110\63\3\2\2\2\u0111\u0113\t\6\2\2\u0112\u0111\3\2\2\2\u0113\u0114"+
		"\3\2\2\2\u0114\u0112\3\2\2\2\u0114\u0115\3\2\2\2\u0115\u0116\3\2\2\2\u0116"+
		"\u0117\b\32\3\2\u0117\65\3\2\2\2-\2\3ADJQSUY^adjqtw\u0080\u0083\u0091"+
		"\u0098\u009e\u00a6\u00aa\u00b5\u00b8\u00be\u00c5\u00c7\u00c9\u00cd\u00d2"+
		"\u00d5\u00d8\u00de\u00e5\u00e8\u00eb\u00f2\u00f9\u00ff\u0107\u010b\u0114"+
		"\5\4\3\2\b\2\2\4\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}