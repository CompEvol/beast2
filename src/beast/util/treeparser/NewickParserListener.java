// Generated from /home/tvaughan/code/beast_and_friends/beast2/src/beast/util/treeparser/NewickParser.g4 by ANTLR 4.5.3
package beast.util.treeparser;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link NewickParser}.
 */
public interface NewickParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link NewickParser#tree}.
	 * @param ctx the parse tree
	 */
	void enterTree(NewickParser.TreeContext ctx);
	/**
	 * Exit a parse tree produced by {@link NewickParser#tree}.
	 * @param ctx the parse tree
	 */
	void exitTree(NewickParser.TreeContext ctx);
	/**
	 * Enter a parse tree produced by {@link NewickParser#node}.
	 * @param ctx the parse tree
	 */
	void enterNode(NewickParser.NodeContext ctx);
	/**
	 * Exit a parse tree produced by {@link NewickParser#node}.
	 * @param ctx the parse tree
	 */
	void exitNode(NewickParser.NodeContext ctx);
	/**
	 * Enter a parse tree produced by {@link NewickParser#post}.
	 * @param ctx the parse tree
	 */
	void enterPost(NewickParser.PostContext ctx);
	/**
	 * Exit a parse tree produced by {@link NewickParser#post}.
	 * @param ctx the parse tree
	 */
	void exitPost(NewickParser.PostContext ctx);
	/**
	 * Enter a parse tree produced by {@link NewickParser#label}.
	 * @param ctx the parse tree
	 */
	void enterLabel(NewickParser.LabelContext ctx);
	/**
	 * Exit a parse tree produced by {@link NewickParser#label}.
	 * @param ctx the parse tree
	 */
	void exitLabel(NewickParser.LabelContext ctx);
	/**
	 * Enter a parse tree produced by {@link NewickParser#meta}.
	 * @param ctx the parse tree
	 */
	void enterMeta(NewickParser.MetaContext ctx);
	/**
	 * Exit a parse tree produced by {@link NewickParser#meta}.
	 * @param ctx the parse tree
	 */
	void exitMeta(NewickParser.MetaContext ctx);
	/**
	 * Enter a parse tree produced by {@link NewickParser#attrib}.
	 * @param ctx the parse tree
	 */
	void enterAttrib(NewickParser.AttribContext ctx);
	/**
	 * Exit a parse tree produced by {@link NewickParser#attrib}.
	 * @param ctx the parse tree
	 */
	void exitAttrib(NewickParser.AttribContext ctx);
	/**
	 * Enter a parse tree produced by {@link NewickParser#attribValue}.
	 * @param ctx the parse tree
	 */
	void enterAttribValue(NewickParser.AttribValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link NewickParser#attribValue}.
	 * @param ctx the parse tree
	 */
	void exitAttribValue(NewickParser.AttribValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link NewickParser#number}.
	 * @param ctx the parse tree
	 */
	void enterNumber(NewickParser.NumberContext ctx);
	/**
	 * Exit a parse tree produced by {@link NewickParser#number}.
	 * @param ctx the parse tree
	 */
	void exitNumber(NewickParser.NumberContext ctx);
	/**
	 * Enter a parse tree produced by {@link NewickParser#attribNumber}.
	 * @param ctx the parse tree
	 */
	void enterAttribNumber(NewickParser.AttribNumberContext ctx);
	/**
	 * Exit a parse tree produced by {@link NewickParser#attribNumber}.
	 * @param ctx the parse tree
	 */
	void exitAttribNumber(NewickParser.AttribNumberContext ctx);
	/**
	 * Enter a parse tree produced by {@link NewickParser#vector}.
	 * @param ctx the parse tree
	 */
	void enterVector(NewickParser.VectorContext ctx);
	/**
	 * Exit a parse tree produced by {@link NewickParser#vector}.
	 * @param ctx the parse tree
	 */
	void exitVector(NewickParser.VectorContext ctx);
}