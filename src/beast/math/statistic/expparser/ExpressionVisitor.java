// Generated from Expression.g4 by ANTLR 4.2
package beast.math.statistic.expparser;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link ExpressionParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface ExpressionVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#UnaryOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryOp(@NotNull ExpressionParser.UnaryOpContext ctx);

	/**
	 * Visit a parse tree produced by {@link ExpressionParser#Bracketed}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBracketed(@NotNull ExpressionParser.BracketedContext ctx);

	/**
	 * Visit a parse tree produced by {@link ExpressionParser#Variable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable(@NotNull ExpressionParser.VariableContext ctx);

	/**
	 * Visit a parse tree produced by {@link ExpressionParser#Number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumber(@NotNull ExpressionParser.NumberContext ctx);

	/**
	 * Visit a parse tree produced by {@link ExpressionParser#AddSub}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddSub(@NotNull ExpressionParser.AddSubContext ctx);

	/**
	 * Visit a parse tree produced by {@link ExpressionParser#ELSEWHERE1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitELSEWHERE1(@NotNull ExpressionParser.ELSEWHERE1Context ctx);

	/**
	 * Visit a parse tree produced by {@link ExpressionParser#Negation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNegation(@NotNull ExpressionParser.NegationContext ctx);

	/**
	 * Visit a parse tree produced by {@link ExpressionParser#ELSEWHERE2}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitELSEWHERE2(@NotNull ExpressionParser.ELSEWHERE2Context ctx);

	/**
	 * Visit a parse tree produced by {@link ExpressionParser#MulDiv}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMulDiv(@NotNull ExpressionParser.MulDivContext ctx);
}