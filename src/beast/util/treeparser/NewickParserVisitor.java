// Generated from NewickParser.g4 by ANTLR 4.7
package beast.util.treeparser;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link NewickParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface NewickParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link NewickParser#tree}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTree(NewickParser.TreeContext ctx);
	/**
	 * Visit a parse tree produced by {@link NewickParser#node}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNode(NewickParser.NodeContext ctx);
	/**
	 * Visit a parse tree produced by {@link NewickParser#post}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPost(NewickParser.PostContext ctx);
	/**
	 * Visit a parse tree produced by {@link NewickParser#label}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabel(NewickParser.LabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link NewickParser#meta}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMeta(NewickParser.MetaContext ctx);
	/**
	 * Visit a parse tree produced by {@link NewickParser#attrib}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttrib(NewickParser.AttribContext ctx);
	/**
	 * Visit a parse tree produced by {@link NewickParser#attribValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttribValue(NewickParser.AttribValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link NewickParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumber(NewickParser.NumberContext ctx);
	/**
	 * Visit a parse tree produced by {@link NewickParser#attribNumber}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttribNumber(NewickParser.AttribNumberContext ctx);
	/**
	 * Visit a parse tree produced by {@link NewickParser#vector}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVector(NewickParser.VectorContext ctx);
}