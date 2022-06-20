package beast.base.evolution.tree.treeparser;
// Generated from NewickParser.g4 by ANTLR 4.10
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
	static { RuntimeMetaData.checkVersion("4.10", RuntimeMetaData.VERSION); }

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
	private static String[] makeRuleNames() {
		return new String[] {
			"tree", "node", "post", "label", "meta", "attrib", "attribValue", "number", 
			"attribNumber", "vector"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "';'", null, "'('", "')'", "':'", null, null, null, "'[&'", null, 
			null, "'='", null, "'{'", "'}'", null, null, null, null, null, "']'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "SEMI", "COMMA", "OPENP", "CLOSEP", "COLON", "FLOAT_SCI", "FLOAT", 
			"INT", "OPENA", "WHITESPACE", "STRING", "EQ", "ACOMMA", "OPENV", "CLOSEV", 
			"AFLOAT_SCI", "AFLOAT", "AINT", "AWHITESPACE", "ASTRING", "CLOSEA", "ATTRIBWS"
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
		public TerminalNode SEMI() { return getToken(NewickParser.SEMI, 0); }
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
		public TerminalNode OPENP() { return getToken(NewickParser.OPENP, 0); }
		public List<NodeContext> node() {
			return getRuleContexts(NodeContext.class);
		}
		public NodeContext node(int i) {
			return getRuleContext(NodeContext.class,i);
		}
		public TerminalNode CLOSEP() { return getToken(NewickParser.CLOSEP, 0); }
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
		public TerminalNode COLON() { return getToken(NewickParser.COLON, 0); }
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
		public TerminalNode OPENA() { return getToken(NewickParser.OPENA, 0); }
		public List<AttribContext> attrib() {
			return getRuleContexts(AttribContext.class);
		}
		public AttribContext attrib(int i) {
			return getRuleContext(AttribContext.class,i);
		}
		public TerminalNode CLOSEA() { return getToken(NewickParser.CLOSEA, 0); }
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
		public TerminalNode EQ() { return getToken(NewickParser.EQ, 0); }
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
		public TerminalNode OPENV() { return getToken(NewickParser.OPENV, 0); }
		public List<AttribValueContext> attribValue() {
			return getRuleContexts(AttribValueContext.class);
		}
		public AttribValueContext attribValue(int i) {
			return getRuleContext(AttribValueContext.class,i);
		}
		public TerminalNode CLOSEV() { return getToken(NewickParser.CLOSEV, 0); }
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
		"\u0004\u0001\u0016^\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0001\u0000\u0001\u0000\u0003\u0000\u0017\b"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0005\u0001\u001f\b\u0001\n\u0001\f\u0001\"\t\u0001\u0001\u0001"+
		"\u0001\u0001\u0003\u0001&\b\u0001\u0001\u0001\u0001\u0001\u0001\u0002"+
		"\u0003\u0002+\b\u0002\u0001\u0002\u0003\u0002.\b\u0002\u0001\u0002\u0001"+
		"\u0002\u0003\u00022\b\u0002\u0001\u0002\u0003\u00025\b\u0002\u0001\u0003"+
		"\u0001\u0003\u0003\u00039\b\u0003\u0001\u0004\u0001\u0004\u0001\u0004"+
		"\u0001\u0004\u0005\u0004?\b\u0004\n\u0004\f\u0004B\t\u0004\u0001\u0004"+
		"\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0006"+
		"\u0001\u0006\u0001\u0006\u0003\u0006M\b\u0006\u0001\u0007\u0001\u0007"+
		"\u0001\b\u0001\b\u0001\t\u0001\t\u0001\t\u0001\t\u0005\tW\b\t\n\t\f\t"+
		"Z\t\t\u0001\t\u0001\t\u0001\t\u0000\u0000\n\u0000\u0002\u0004\u0006\b"+
		"\n\f\u000e\u0010\u0012\u0000\u0002\u0001\u0000\u0006\b\u0001\u0000\u0010"+
		"\u0012_\u0000\u0014\u0001\u0000\u0000\u0000\u0002%\u0001\u0000\u0000\u0000"+
		"\u0004*\u0001\u0000\u0000\u0000\u00068\u0001\u0000\u0000\u0000\b:\u0001"+
		"\u0000\u0000\u0000\nE\u0001\u0000\u0000\u0000\fL\u0001\u0000\u0000\u0000"+
		"\u000eN\u0001\u0000\u0000\u0000\u0010P\u0001\u0000\u0000\u0000\u0012R"+
		"\u0001\u0000\u0000\u0000\u0014\u0016\u0003\u0002\u0001\u0000\u0015\u0017"+
		"\u0005\u0001\u0000\u0000\u0016\u0015\u0001\u0000\u0000\u0000\u0016\u0017"+
		"\u0001\u0000\u0000\u0000\u0017\u0018\u0001\u0000\u0000\u0000\u0018\u0019"+
		"\u0005\u0000\u0000\u0001\u0019\u0001\u0001\u0000\u0000\u0000\u001a\u001b"+
		"\u0005\u0003\u0000\u0000\u001b \u0003\u0002\u0001\u0000\u001c\u001d\u0005"+
		"\u0002\u0000\u0000\u001d\u001f\u0003\u0002\u0001\u0000\u001e\u001c\u0001"+
		"\u0000\u0000\u0000\u001f\"\u0001\u0000\u0000\u0000 \u001e\u0001\u0000"+
		"\u0000\u0000 !\u0001\u0000\u0000\u0000!#\u0001\u0000\u0000\u0000\" \u0001"+
		"\u0000\u0000\u0000#$\u0005\u0004\u0000\u0000$&\u0001\u0000\u0000\u0000"+
		"%\u001a\u0001\u0000\u0000\u0000%&\u0001\u0000\u0000\u0000&\'\u0001\u0000"+
		"\u0000\u0000\'(\u0003\u0004\u0002\u0000(\u0003\u0001\u0000\u0000\u0000"+
		")+\u0003\u0006\u0003\u0000*)\u0001\u0000\u0000\u0000*+\u0001\u0000\u0000"+
		"\u0000+-\u0001\u0000\u0000\u0000,.\u0003\b\u0004\u0000-,\u0001\u0000\u0000"+
		"\u0000-.\u0001\u0000\u0000\u0000.4\u0001\u0000\u0000\u0000/1\u0005\u0005"+
		"\u0000\u000002\u0003\b\u0004\u000010\u0001\u0000\u0000\u000012\u0001\u0000"+
		"\u0000\u000023\u0001\u0000\u0000\u000035\u0003\u000e\u0007\u00004/\u0001"+
		"\u0000\u0000\u000045\u0001\u0000\u0000\u00005\u0005\u0001\u0000\u0000"+
		"\u000069\u0003\u000e\u0007\u000079\u0005\u000b\u0000\u000086\u0001\u0000"+
		"\u0000\u000087\u0001\u0000\u0000\u00009\u0007\u0001\u0000\u0000\u0000"+
		":;\u0005\t\u0000\u0000;@\u0003\n\u0005\u0000<=\u0005\r\u0000\u0000=?\u0003"+
		"\n\u0005\u0000><\u0001\u0000\u0000\u0000?B\u0001\u0000\u0000\u0000@>\u0001"+
		"\u0000\u0000\u0000@A\u0001\u0000\u0000\u0000AC\u0001\u0000\u0000\u0000"+
		"B@\u0001\u0000\u0000\u0000CD\u0005\u0015\u0000\u0000D\t\u0001\u0000\u0000"+
		"\u0000EF\u0005\u0014\u0000\u0000FG\u0005\f\u0000\u0000GH\u0003\f\u0006"+
		"\u0000H\u000b\u0001\u0000\u0000\u0000IM\u0003\u0010\b\u0000JM\u0005\u0014"+
		"\u0000\u0000KM\u0003\u0012\t\u0000LI\u0001\u0000\u0000\u0000LJ\u0001\u0000"+
		"\u0000\u0000LK\u0001\u0000\u0000\u0000M\r\u0001\u0000\u0000\u0000NO\u0007"+
		"\u0000\u0000\u0000O\u000f\u0001\u0000\u0000\u0000PQ\u0007\u0001\u0000"+
		"\u0000Q\u0011\u0001\u0000\u0000\u0000RS\u0005\u000e\u0000\u0000SX\u0003"+
		"\f\u0006\u0000TU\u0005\r\u0000\u0000UW\u0003\f\u0006\u0000VT\u0001\u0000"+
		"\u0000\u0000WZ\u0001\u0000\u0000\u0000XV\u0001\u0000\u0000\u0000XY\u0001"+
		"\u0000\u0000\u0000Y[\u0001\u0000\u0000\u0000ZX\u0001\u0000\u0000\u0000"+
		"[\\\u0005\u000f\u0000\u0000\\\u0013\u0001\u0000\u0000\u0000\u000b\u0016"+
		" %*-148@LX";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}