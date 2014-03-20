// Generated from Expression.g4 by ANTLR 4.2
package beast.math.statistic.expparser;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ExpressionParser}.
 */
public interface ExpressionListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#UnaryOp}.
	 * @param ctx the parse tree
	 */
	void enterUnaryOp(@NotNull ExpressionParser.UnaryOpContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#UnaryOp}.
	 * @param ctx the parse tree
	 */
	void exitUnaryOp(@NotNull ExpressionParser.UnaryOpContext ctx);

	/**
	 * Enter a parse tree produced by {@link ExpressionParser#Bracketed}.
	 * @param ctx the parse tree
	 */
	void enterBracketed(@NotNull ExpressionParser.BracketedContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#Bracketed}.
	 * @param ctx the parse tree
	 */
	void exitBracketed(@NotNull ExpressionParser.BracketedContext ctx);

	/**
	 * Enter a parse tree produced by {@link ExpressionParser#Variable}.
	 * @param ctx the parse tree
	 */
	void enterVariable(@NotNull ExpressionParser.VariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#Variable}.
	 * @param ctx the parse tree
	 */
	void exitVariable(@NotNull ExpressionParser.VariableContext ctx);

	/**
	 * Enter a parse tree produced by {@link ExpressionParser#Number}.
	 * @param ctx the parse tree
	 */
	void enterNumber(@NotNull ExpressionParser.NumberContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#Number}.
	 * @param ctx the parse tree
	 */
	void exitNumber(@NotNull ExpressionParser.NumberContext ctx);

	/**
	 * Enter a parse tree produced by {@link ExpressionParser#AddSub}.
	 * @param ctx the parse tree
	 */
	void enterAddSub(@NotNull ExpressionParser.AddSubContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#AddSub}.
	 * @param ctx the parse tree
	 */
	void exitAddSub(@NotNull ExpressionParser.AddSubContext ctx);

	/**
	 * Enter a parse tree produced by {@link ExpressionParser#ELSEWHERE1}.
	 * @param ctx the parse tree
	 */
	void enterELSEWHERE1(@NotNull ExpressionParser.ELSEWHERE1Context ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#ELSEWHERE1}.
	 * @param ctx the parse tree
	 */
	void exitELSEWHERE1(@NotNull ExpressionParser.ELSEWHERE1Context ctx);

	/**
	 * Enter a parse tree produced by {@link ExpressionParser#Negation}.
	 * @param ctx the parse tree
	 */
	void enterNegation(@NotNull ExpressionParser.NegationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#Negation}.
	 * @param ctx the parse tree
	 */
	void exitNegation(@NotNull ExpressionParser.NegationContext ctx);

	/**
	 * Enter a parse tree produced by {@link ExpressionParser#ELSEWHERE2}.
	 * @param ctx the parse tree
	 */
	void enterELSEWHERE2(@NotNull ExpressionParser.ELSEWHERE2Context ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#ELSEWHERE2}.
	 * @param ctx the parse tree
	 */
	void exitELSEWHERE2(@NotNull ExpressionParser.ELSEWHERE2Context ctx);

	/**
	 * Enter a parse tree produced by {@link ExpressionParser#MulDiv}.
	 * @param ctx the parse tree
	 */
	void enterMulDiv(@NotNull ExpressionParser.MulDivContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#MulDiv}.
	 * @param ctx the parse tree
	 */
	void exitMulDiv(@NotNull ExpressionParser.MulDivContext ctx);
}