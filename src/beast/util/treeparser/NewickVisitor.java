// Generated from /home/tvaughan/code/beast_and_friends/beast2/src/beast/util/treeparser/Newick.g4 by ANTLR 4.5
package beast.util.treeparser;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link NewickParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface NewickVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link NewickParser#tree}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTree(@NotNull NewickParser.TreeContext ctx);
	/**
	 * Visit a parse tree produced by {@link NewickParser#node}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNode(@NotNull NewickParser.NodeContext ctx);
	/**
	 * Visit a parse tree produced by {@link NewickParser#post}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPost(@NotNull NewickParser.PostContext ctx);
	/**
	 * Visit a parse tree produced by {@link NewickParser#label}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabel(@NotNull NewickParser.LabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link NewickParser#meta}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMeta(@NotNull NewickParser.MetaContext ctx);
	/**
	 * Visit a parse tree produced by {@link NewickParser#attrib}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttrib(@NotNull NewickParser.AttribContext ctx);
	/**
	 * Visit a parse tree produced by {@link NewickParser#attribValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttribValue(@NotNull NewickParser.AttribValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link NewickParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumber(@NotNull NewickParser.NumberContext ctx);
	/**
	 * Visit a parse tree produced by {@link NewickParser#vector}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVector(@NotNull NewickParser.VectorContext ctx);
}