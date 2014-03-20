// Generated from Expression.g4 by ANTLR 4.2
package beast.math.statistic.expparser;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class ExpressionLexer extends Lexer {
	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__3=1, T__2=2, T__1=3, T__0=4, ADD=5, SUB=6, MUL=7, DIV=8, EXP=9, LOG=10, 
		NNINT=11, NNFLOAT=12, VARNAME=13, WHITESPACE=14;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] tokenNames = {
		"<INVALID>",
		"']'", "')'", "'['", "'('", "'+'", "'-'", "'*'", "'/'", "'exp'", "'log'", 
		"NNINT", "NNFLOAT", "VARNAME", "WHITESPACE"
	};
	public static final String[] ruleNames = {
		"T__3", "T__2", "T__1", "T__0", "ADD", "SUB", "MUL", "DIV", "EXP", "LOG", 
		"NNINT", "NNFLOAT", "D", "NZD", "VARNAME", "WHITESPACE"
	};


	public ExpressionLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "Expression.g4"; }

	@Override
	public String[] getTokenNames() { return tokenNames; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\20j\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\3\2\3\2\3"+
		"\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\3\n\3\n\3\n\3\n"+
		"\3\13\3\13\3\13\3\13\3\f\3\f\3\f\7\f?\n\f\f\f\16\fB\13\f\5\fD\n\f\3\r"+
		"\3\r\3\r\7\rI\n\r\f\r\16\rL\13\r\3\r\3\r\5\rP\n\r\3\r\6\rS\n\r\r\r\16"+
		"\rT\5\rW\n\r\3\16\3\16\3\17\3\17\3\20\3\20\7\20_\n\20\f\20\16\20b\13\20"+
		"\3\21\6\21e\n\21\r\21\16\21f\3\21\3\21\2\2\22\3\3\5\4\7\5\t\6\13\7\r\b"+
		"\17\t\21\n\23\13\25\f\27\r\31\16\33\2\35\2\37\17!\20\3\2\b\4\2GGgg\3\2"+
		"\62;\3\2\63;\5\2C\\aac|\7\2//\62;C\\aac|\5\2\13\f\17\17\"\"o\2\3\3\2\2"+
		"\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3"+
		"\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2"+
		"\2\2\37\3\2\2\2\2!\3\2\2\2\3#\3\2\2\2\5%\3\2\2\2\7\'\3\2\2\2\t)\3\2\2"+
		"\2\13+\3\2\2\2\r-\3\2\2\2\17/\3\2\2\2\21\61\3\2\2\2\23\63\3\2\2\2\25\67"+
		"\3\2\2\2\27C\3\2\2\2\31E\3\2\2\2\33X\3\2\2\2\35Z\3\2\2\2\37\\\3\2\2\2"+
		"!d\3\2\2\2#$\7_\2\2$\4\3\2\2\2%&\7+\2\2&\6\3\2\2\2\'(\7]\2\2(\b\3\2\2"+
		"\2)*\7*\2\2*\n\3\2\2\2+,\7-\2\2,\f\3\2\2\2-.\7/\2\2.\16\3\2\2\2/\60\7"+
		",\2\2\60\20\3\2\2\2\61\62\7\61\2\2\62\22\3\2\2\2\63\64\7g\2\2\64\65\7"+
		"z\2\2\65\66\7r\2\2\66\24\3\2\2\2\678\7n\2\289\7q\2\29:\7i\2\2:\26\3\2"+
		"\2\2;D\7\62\2\2<@\5\35\17\2=?\5\33\16\2>=\3\2\2\2?B\3\2\2\2@>\3\2\2\2"+
		"@A\3\2\2\2AD\3\2\2\2B@\3\2\2\2C;\3\2\2\2C<\3\2\2\2D\30\3\2\2\2EF\5\27"+
		"\f\2FJ\7\60\2\2GI\5\33\16\2HG\3\2\2\2IL\3\2\2\2JH\3\2\2\2JK\3\2\2\2KV"+
		"\3\2\2\2LJ\3\2\2\2MO\t\2\2\2NP\7/\2\2ON\3\2\2\2OP\3\2\2\2PR\3\2\2\2QS"+
		"\5\33\16\2RQ\3\2\2\2ST\3\2\2\2TR\3\2\2\2TU\3\2\2\2UW\3\2\2\2VM\3\2\2\2"+
		"VW\3\2\2\2W\32\3\2\2\2XY\t\3\2\2Y\34\3\2\2\2Z[\t\4\2\2[\36\3\2\2\2\\`"+
		"\t\5\2\2]_\t\6\2\2^]\3\2\2\2_b\3\2\2\2`^\3\2\2\2`a\3\2\2\2a \3\2\2\2b"+
		"`\3\2\2\2ce\t\7\2\2dc\3\2\2\2ef\3\2\2\2fd\3\2\2\2fg\3\2\2\2gh\3\2\2\2"+
		"hi\b\21\2\2i\"\3\2\2\2\13\2@CJOTV`f\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}