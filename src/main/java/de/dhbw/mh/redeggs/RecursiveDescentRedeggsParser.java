package de.dhbw.mh.redeggs;

import com.sun.tools.jconsole.JConsoleContext;

import static de.dhbw.mh.redeggs.CodePointRange.range;
import static de.dhbw.mh.redeggs.CodePointRange.single;

/**
 * A parser for regular expressions using recursive descent parsing.
 * This class is responsible for converting a regular expression string into a
 * tree representation of a {@link RegularEggspression}.
 */
public class RecursiveDescentRedeggsParser {
	private String input;
	private int cursor;

	/**
	 * The symbol factory used to create symbols for the regular expression.
	 */
	protected final SymbolFactory symbolFactory;

	/**
	 * Constructs a new {@code RecursiveDescentRedeggsParser} with the specified
	 * symbol factory.
	 *
	 * @param symbolFactory the factory used to create symbols for parsing
	 */
	public RecursiveDescentRedeggsParser(SymbolFactory symbolFactory) {
		this.symbolFactory = symbolFactory;
	}

	private char peek() {
		return this.input.charAt(this.cursor);
	}

	private boolean check(char expectedChar) {
		return this.peek() == expectedChar;
	}

	private void expect(char expectedChar) throws RedeggsParseException {
		if (!this.check(expectedChar)) throw new RedeggsParseException("Expected " + expectedChar + " but got " + this.peek(), this.cursor);
	}

	private char consume() {
		char character = this.peek();
		this.cursor++;
		return character;
	}

	private boolean isTheEnd() {
		return this.cursor >= this.input.length();
	}

	private boolean isLiteral() {
		if (!this.isTheEnd()) return false;

		char token = this.peek();
		return (token == '_') || (token >= 'a' && token <= 'z') || (token >= 'A' && token <= 'Z') || (token >= '0' && token <= '9');
	}

	private RegularEggspression regex() throws RedeggsParseException {
		RegularEggspression concat = this.concat();
		return union(concat);
	}

	private RegularEggspression union(RegularEggspression left) throws RedeggsParseException {
		if (this.check('|')) {
			RegularEggspression right = this.concat();
			return union(new RegularEggspression.Alternation(left, right));
		}
		return left; // epsilon
	}

	private RegularEggspression concat() throws RedeggsParseException {
		RegularEggspression kleene = this.kleene();
		return suffix(kleene);
	}

	private RegularEggspression suffix(RegularEggspression left) throws RedeggsParseException {
		try {
			RegularEggspression right = this.kleene();
			return suffix(new RegularEggspression.Concatenation(left, right));
		} catch (RedeggsParseException exception) {
			return left; // epsilon
		}
	}

	private RegularEggspression kleene() throws RedeggsParseException {
		RegularEggspression base = this.base();
		return star(base);
	}


	private RegularEggspression star(RegularEggspression base) {
		if (this.check('*')) {
			return new RegularEggspression.Star(base);
		}
		return base; // epsilon
	}

	private RegularEggspression base() throws RedeggsParseException {
		if (isLiteral()) {
			char literal = this.consume();
			// VirtualSymbol literal = this.symbolFactory.newSymbol()
			// 		.include(single('_'), range('a', 'z'), range('A', 'Z'))
			// 		.andNothingElse();
			VirtualSymbol symbol = this.symbolFactory.newSymbol().include(single(literal)).andNothingElse();
			return new RegularEggspression.Literal(symbol);
		} else if (this.check('(')) {
			RegularEggspression expression = regex();
			this.expect(')');
			return expression;
		} else if (this.check('[')) {
			System.out.println("NOT IMPLEMENTED YET");
			this.expect(']');
		}

		throw new RedeggsParseException("Expected LIT, '(' or '[' but got nothing from the above.", this.cursor);
	}

	/**
	 * Parses a regular expression string into an abstract syntax tree (AST).
	 * 
	 * This class uses recursive descent parsing to convert a given regular
	 * expression into a tree structure that can be processed or compiled further.
	 * The AST nodes represent different components of the regex such as literals,
	 * operators, and groups.
	 *
	 * @param regex the regular expression to parse
	 * @return the {@link RegularEggspression} representation of the parsed regex
	 * @throws RedeggsParseException if the parsing fails or the regex is invalid
	 */
	public RegularEggspression parse(String regex) throws RedeggsParseException {
		// TODO: Implement the recursive descent parsing to convert `regex` into an AST.
		// This is a placeholder implementation to demonstrate how to create a symbol.

		this.input = regex;
		this.cursor = 0;

		return this.regex();
	}
}
