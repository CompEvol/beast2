// Generated from NewickParser.g4 by ANTLR 4.7
package beast.util.treeparser;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class NewickParser extends Parser {
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
		RULE_tree = 0, RULE_node = 1, RULE_post = 2, RULE_label = 3, RULE_meta = 4, 
		RULE_attrib = 5, RULE_attribValue = 6, RULE_number = 7, RULE_attribNumber = 8, 
		RULE_vector = 9;
	public static final String[] ruleNames = {
		"tree", "node", "post", "label", "meta", "attrib", "attribValue", "number", 
		"attribNumber", "vector"
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

	@Override
	public String getGrammarFileName() { return "NewickParser.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public NewickParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class TreeContext extends ParserRuleContext {
		public NodeContext node() {
			return getRuleContext(NodeContext.class,0);
		}
		public TerminalNode EOF() { return getToken(NewickParser.EOF, 0); }
		public TreeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tree; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NewickParserListener ) ((NewickParserListener)listener).enterTree(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NewickParserListener ) ((NewickParserListener)listener).exitTree(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NewickParserVisitor ) return ((NewickParserVisitor<? extends T>)visitor).visitTree(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TreeContext tree() throws RecognitionException {
		TreeContext _localctx = new TreeContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_tree);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(20);
			node();
			setState(22);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SEMI) {
				{
				setState(21);
				match(SEMI);
				}
			}

			setState(24);
			match(EOF);
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

	public static class NodeContext extends ParserRuleContext {
		public PostContext post() {
			return getRuleContext(PostContext.class,0);
		}
		public List<NodeContext> node() {
			return getRuleContexts(NodeContext.class);
		}
		public NodeContext node(int i) {
			return getRuleContext(NodeContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(NewickParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(NewickParser.COMMA, i);
		}
		public NodeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_node; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NewickParserListener ) ((NewickParserListener)listener).enterNode(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NewickParserListener ) ((NewickParserListener)listener).exitNode(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NewickParserVisitor ) return ((NewickParserVisitor<? extends T>)visitor).visitNode(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NodeContext node() throws RecognitionException {
		NodeContext _localctx = new NodeContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_node);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(37);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPENP) {
				{
				setState(26);
				match(OPENP);
				setState(27);
				node();
				setState(32);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(28);
					match(COMMA);
					setState(29);
					node();
					}
					}
					setState(34);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(35);
				match(CLOSEP);
				}
			}

			setState(39);
			post();
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

	public static class PostContext extends ParserRuleContext {
		public MetaContext nodeMeta;
		public MetaContext lengthMeta;
		public NumberContext length;
		public LabelContext label() {
			return getRuleContext(LabelContext.class,0);
		}
		public List<MetaContext> meta() {
			return getRuleContexts(MetaContext.class);
		}
		public MetaContext meta(int i) {
			return getRuleContext(MetaContext.class,i);
		}
		public NumberContext number() {
			return getRuleContext(NumberContext.class,0);
		}
		public PostContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_post; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NewickParserListener ) ((NewickParserListener)listener).enterPost(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NewickParserListener ) ((NewickParserListener)listener).exitPost(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NewickParserVisitor ) return ((NewickParserVisitor<? extends T>)visitor).visitPost(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PostContext post() throws RecognitionException {
		PostContext _localctx = new PostContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_post);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(42);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << FLOAT_SCI) | (1L << FLOAT) | (1L << INT) | (1L << STRING))) != 0)) {
				{
				setState(41);
				label();
				}
			}

			setState(45);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPENA) {
				{
				setState(44);
				((PostContext)_localctx).nodeMeta = meta();
				}
			}

			setState(52);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COLON) {
				{
				setState(47);
				match(COLON);
				setState(49);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OPENA) {
					{
					setState(48);
					((PostContext)_localctx).lengthMeta = meta();
					}
				}

				setState(51);
				((PostContext)_localctx).length = number();
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

	public static class LabelContext extends ParserRuleContext {
		public NumberContext number() {
			return getRuleContext(NumberContext.class,0);
		}
		public TerminalNode STRING() { return getToken(NewickParser.STRING, 0); }
		public LabelContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_label; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NewickParserListener ) ((NewickParserListener)listener).enterLabel(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NewickParserListener ) ((NewickParserListener)listener).exitLabel(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NewickParserVisitor ) return ((NewickParserVisitor<? extends T>)visitor).visitLabel(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LabelContext label() throws RecognitionException {
		LabelContext _localctx = new LabelContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_label);
		try {
			setState(56);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case FLOAT_SCI:
			case FLOAT:
			case INT:
				enterOuterAlt(_localctx, 1);
				{
				setState(54);
				number();
				}
				break;
			case STRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(55);
				match(STRING);
				}
				break;
			default:
				throw new NoViableAltException(this);
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

	public static class MetaContext extends ParserRuleContext {
		public List<AttribContext> attrib() {
			return getRuleContexts(AttribContext.class);
		}
		public AttribContext attrib(int i) {
			return getRuleContext(AttribContext.class,i);
		}
		public List<TerminalNode> ACOMMA() { return getTokens(NewickParser.ACOMMA); }
		public TerminalNode ACOMMA(int i) {
			return getToken(NewickParser.ACOMMA, i);
		}
		public MetaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_meta; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NewickParserListener ) ((NewickParserListener)listener).enterMeta(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NewickParserListener ) ((NewickParserListener)listener).exitMeta(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NewickParserVisitor ) return ((NewickParserVisitor<? extends T>)visitor).visitMeta(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MetaContext meta() throws RecognitionException {
		MetaContext _localctx = new MetaContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_meta);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(58);
			match(OPENA);
			setState(59);
			attrib();
			setState(64);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ACOMMA) {
				{
				{
				setState(60);
				match(ACOMMA);
				setState(61);
				attrib();
				}
				}
				setState(66);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(67);
			match(CLOSEA);
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

	public static class AttribContext extends ParserRuleContext {
		public Token attribKey;
		public AttribValueContext attribValue() {
			return getRuleContext(AttribValueContext.class,0);
		}
		public TerminalNode ASTRING() { return getToken(NewickParser.ASTRING, 0); }
		public AttribContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attrib; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NewickParserListener ) ((NewickParserListener)listener).enterAttrib(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NewickParserListener ) ((NewickParserListener)listener).exitAttrib(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NewickParserVisitor ) return ((NewickParserVisitor<? extends T>)visitor).visitAttrib(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AttribContext attrib() throws RecognitionException {
		AttribContext _localctx = new AttribContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_attrib);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(69);
			((AttribContext)_localctx).attribKey = match(ASTRING);
			setState(70);
			match(EQ);
			setState(71);
			attribValue();
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

	public static class AttribValueContext extends ParserRuleContext {
		public AttribNumberContext attribNumber() {
			return getRuleContext(AttribNumberContext.class,0);
		}
		public TerminalNode ASTRING() { return getToken(NewickParser.ASTRING, 0); }
		public VectorContext vector() {
			return getRuleContext(VectorContext.class,0);
		}
		public AttribValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attribValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NewickParserListener ) ((NewickParserListener)listener).enterAttribValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NewickParserListener ) ((NewickParserListener)listener).exitAttribValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NewickParserVisitor ) return ((NewickParserVisitor<? extends T>)visitor).visitAttribValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AttribValueContext attribValue() throws RecognitionException {
		AttribValueContext _localctx = new AttribValueContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_attribValue);
		try {
			setState(76);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case AFLOAT_SCI:
			case AFLOAT:
			case AINT:
				enterOuterAlt(_localctx, 1);
				{
				setState(73);
				attribNumber();
				}
				break;
			case ASTRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(74);
				match(ASTRING);
				}
				break;
			case OPENV:
				enterOuterAlt(_localctx, 3);
				{
				setState(75);
				vector();
				}
				break;
			default:
				throw new NoViableAltException(this);
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

	public static class NumberContext extends ParserRuleContext {
		public TerminalNode INT() { return getToken(NewickParser.INT, 0); }
		public TerminalNode FLOAT() { return getToken(NewickParser.FLOAT, 0); }
		public TerminalNode FLOAT_SCI() { return getToken(NewickParser.FLOAT_SCI, 0); }
		public NumberContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_number; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NewickParserListener ) ((NewickParserListener)listener).enterNumber(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NewickParserListener ) ((NewickParserListener)listener).exitNumber(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NewickParserVisitor ) return ((NewickParserVisitor<? extends T>)visitor).visitNumber(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NumberContext number() throws RecognitionException {
		NumberContext _localctx = new NumberContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_number);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(78);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << FLOAT_SCI) | (1L << FLOAT) | (1L << INT))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
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

	public static class AttribNumberContext extends ParserRuleContext {
		public TerminalNode AINT() { return getToken(NewickParser.AINT, 0); }
		public TerminalNode AFLOAT() { return getToken(NewickParser.AFLOAT, 0); }
		public TerminalNode AFLOAT_SCI() { return getToken(NewickParser.AFLOAT_SCI, 0); }
		public AttribNumberContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attribNumber; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NewickParserListener ) ((NewickParserListener)listener).enterAttribNumber(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NewickParserListener ) ((NewickParserListener)listener).exitAttribNumber(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NewickParserVisitor ) return ((NewickParserVisitor<? extends T>)visitor).visitAttribNumber(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AttribNumberContext attribNumber() throws RecognitionException {
		AttribNumberContext _localctx = new AttribNumberContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_attribNumber);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(80);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << AFLOAT_SCI) | (1L << AFLOAT) | (1L << AINT))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
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

	public static class VectorContext extends ParserRuleContext {
		public List<AttribValueContext> attribValue() {
			return getRuleContexts(AttribValueContext.class);
		}
		public AttribValueContext attribValue(int i) {
			return getRuleContext(AttribValueContext.class,i);
		}
		public List<TerminalNode> ACOMMA() { return getTokens(NewickParser.ACOMMA); }
		public TerminalNode ACOMMA(int i) {
			return getToken(NewickParser.ACOMMA, i);
		}
		public VectorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_vector; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NewickParserListener ) ((NewickParserListener)listener).enterVector(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NewickParserListener ) ((NewickParserListener)listener).exitVector(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NewickParserVisitor ) return ((NewickParserVisitor<? extends T>)visitor).visitVector(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VectorContext vector() throws RecognitionException {
		VectorContext _localctx = new VectorContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_vector);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(82);
			match(OPENV);
			setState(83);
			attribValue();
			setState(88);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ACOMMA) {
				{
				{
				setState(84);
				match(ACOMMA);
				setState(85);
				attribValue();
				}
				}
				setState(90);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(91);
			match(CLOSEV);
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

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\30`\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t\13\3"+
		"\2\3\2\5\2\31\n\2\3\2\3\2\3\3\3\3\3\3\3\3\7\3!\n\3\f\3\16\3$\13\3\3\3"+
		"\3\3\5\3(\n\3\3\3\3\3\3\4\5\4-\n\4\3\4\5\4\60\n\4\3\4\3\4\5\4\64\n\4\3"+
		"\4\5\4\67\n\4\3\5\3\5\5\5;\n\5\3\6\3\6\3\6\3\6\7\6A\n\6\f\6\16\6D\13\6"+
		"\3\6\3\6\3\7\3\7\3\7\3\7\3\b\3\b\3\b\5\bO\n\b\3\t\3\t\3\n\3\n\3\13\3\13"+
		"\3\13\3\13\7\13Y\n\13\f\13\16\13\\\13\13\3\13\3\13\3\13\2\2\f\2\4\6\b"+
		"\n\f\16\20\22\24\2\4\3\2\b\n\3\2\22\24\2a\2\26\3\2\2\2\4\'\3\2\2\2\6,"+
		"\3\2\2\2\b:\3\2\2\2\n<\3\2\2\2\fG\3\2\2\2\16N\3\2\2\2\20P\3\2\2\2\22R"+
		"\3\2\2\2\24T\3\2\2\2\26\30\5\4\3\2\27\31\7\3\2\2\30\27\3\2\2\2\30\31\3"+
		"\2\2\2\31\32\3\2\2\2\32\33\7\2\2\3\33\3\3\2\2\2\34\35\7\5\2\2\35\"\5\4"+
		"\3\2\36\37\7\4\2\2\37!\5\4\3\2 \36\3\2\2\2!$\3\2\2\2\" \3\2\2\2\"#\3\2"+
		"\2\2#%\3\2\2\2$\"\3\2\2\2%&\7\6\2\2&(\3\2\2\2\'\34\3\2\2\2\'(\3\2\2\2"+
		"()\3\2\2\2)*\5\6\4\2*\5\3\2\2\2+-\5\b\5\2,+\3\2\2\2,-\3\2\2\2-/\3\2\2"+
		"\2.\60\5\n\6\2/.\3\2\2\2/\60\3\2\2\2\60\66\3\2\2\2\61\63\7\7\2\2\62\64"+
		"\5\n\6\2\63\62\3\2\2\2\63\64\3\2\2\2\64\65\3\2\2\2\65\67\5\20\t\2\66\61"+
		"\3\2\2\2\66\67\3\2\2\2\67\7\3\2\2\28;\5\20\t\29;\7\r\2\2:8\3\2\2\2:9\3"+
		"\2\2\2;\t\3\2\2\2<=\7\13\2\2=B\5\f\7\2>?\7\17\2\2?A\5\f\7\2@>\3\2\2\2"+
		"AD\3\2\2\2B@\3\2\2\2BC\3\2\2\2CE\3\2\2\2DB\3\2\2\2EF\7\27\2\2F\13\3\2"+
		"\2\2GH\7\26\2\2HI\7\16\2\2IJ\5\16\b\2J\r\3\2\2\2KO\5\22\n\2LO\7\26\2\2"+
		"MO\5\24\13\2NK\3\2\2\2NL\3\2\2\2NM\3\2\2\2O\17\3\2\2\2PQ\t\2\2\2Q\21\3"+
		"\2\2\2RS\t\3\2\2S\23\3\2\2\2TU\7\20\2\2UZ\5\16\b\2VW\7\17\2\2WY\5\16\b"+
		"\2XV\3\2\2\2Y\\\3\2\2\2ZX\3\2\2\2Z[\3\2\2\2[]\3\2\2\2\\Z\3\2\2\2]^\7\21"+
		"\2\2^\25\3\2\2\2\r\30\"\',/\63\66:BNZ";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}