package de.dhbw.mh.redeggs;

import com.sun.tools.jconsole.JConsoleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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

	private char expect(char expectedChar) throws RedeggsParseException {
		if (!this.check(expectedChar)) throw new RedeggsParseException("[EXPECT] Expected " + expectedChar + " but got " + this.peek(), this.cursor);
		return this.consume();
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
		char token = this.peek();
		return (token == '_') || (token >= 'a' && token <= 'z') || (token >= 'A' && token <= 'Z') || (token >= '0' && token <= '9');
	}

	private RegularEggspression regex() throws RedeggsParseException {
		RegularEggspression concat = this.concat();
		return this.union(concat);
	}

	private RegularEggspression union(RegularEggspression left) throws RedeggsParseException {
		if (this.check('|')) {
			this.consume();
			RegularEggspression concat = this.concat();
			RegularEggspression suffix = this.suffix(concat);
			return this.union(new RegularEggspression.Alternation(left, suffix));
		} else if (this.isTheEnd() || this.check(')')) {
			return left; // epsilon
		}
		throw new RedeggsParseException("[UNION] Expected '|', epsilon or ')'", this.cursor);
	}

	private RegularEggspression concat() throws RedeggsParseException {
		RegularEggspression kleene = this.kleene();
		return suffix(kleene);
	}

	private RegularEggspression suffix(RegularEggspression left) throws RedeggsParseException {
		if (this.isLiteral() || this.check('(') || this.check('[')) {
			RegularEggspression right = this.kleene();
			return suffix(new RegularEggspression.Concatenation(left, right));
		} else if (this.check(')') || this.check('|') || this.isTheEnd()) {
			return left; // epsilon
		}
		throw new RedeggsParseException("[SUFFIX] Expected LIT, '(', '[', ')', '|' or epsilon", this.cursor);
	}

	private RegularEggspression kleene() throws RedeggsParseException {
		RegularEggspression base = this.base();
		return this.star(base);
	}

	private RegularEggspression star(RegularEggspression base) throws RedeggsParseException {
		if (this.check('*')) {
			this.consume();
			return new RegularEggspression.Star(base);
		} else if (this.isTheEnd() || this.isLiteral() || this.check('(') || this.check(')') || this.check('[') || this.check('|')) {
			return base;
		}
		throw new RedeggsParseException("[STAR] Expected *, LIT, '(', ')', '[', '|' or epsilon but got " + base.toString(), this.cursor);
	}

	private RegularEggspression base() throws RedeggsParseException {
		if (this.isLiteral()) {
			char literal = this.consume();
			// VirtualSymbol literal = this.symbolFactory.newSymbol()
			// 		.include(single('_'), range('a', 'z'), range('A', 'Z'))
			// 		.andNothingElse();
			VirtualSymbol symbol = this.symbolFactory.newSymbol().include(single(literal)).andNothingElse();
			return new RegularEggspression.Literal(symbol);
		} else if (this.check('(')) {
			this.consume();
			RegularEggspression expression = this.regex();
			this.expect(')');
			return expression;
		} else if (this.check('[')) {
			this.consume();

			boolean negationIncluded = this.negation();
			SymbolFactory.Builder builder = this.symbolFactory.newSymbol();

			List<CodePointRange> inhalt = this.inhalt();
			List<CodePointRange> range = this.range();

			CodePointRange[] ranges = Stream.concat(inhalt.stream(), range.stream()).toArray(CodePointRange[]::new);

			if (this.negation()) {
				builder.exclude(ranges);
			} else {
				builder.include(ranges);
			}

			this.expect(']');
			return new RegularEggspression.Literal(builder.andNothingElse());
		}

		throw new RedeggsParseException("[BASE] Expected LIT, '(' or '[' but got " + this.peek(), this.cursor);
	}

	private boolean negation() throws RedeggsParseException {
		if (this.check('^')) {
			this.consume();
			return true;
		} else if (this.isLiteral()) {
			return false;
		}
		throw new RedeggsParseException("[NEGATION] Expected '^' or LIT", this.cursor);
	}


	private List<CodePointRange> inhalt() throws RedeggsParseException {
		char literal = this.consumeLiteral();
		return this.rest(literal);
	}

	private List<CodePointRange> range() throws  RedeggsParseException {
		if (this.isLiteral()) {
			List<CodePointRange> inhaltRange = this.inhalt();
			List<CodePointRange> range = this.range();

			return Stream.concat(inhaltRange.stream(), range.stream()).toList();
		} else if (this.check(']')) {
			return new ArrayList<>();
		}

		throw new RedeggsParseException("[RANGE] Expected LIT or ']'", this.cursor);
	}

	private List<CodePointRange> rest(char left) throws RedeggsParseException {
		if (this.check('-')) {
			this.consume();
			char right = this.consumeLiteral();

			List<CodePointRange> range = new ArrayList<>();
			range.add(CodePointRange.range(left, right));

			return range;
		} else if (this.isLiteral() || this.check(']')) {
			List<CodePointRange> range = new ArrayList<>();
			range.add(single(left));
			return range;
		}

		throw new RedeggsParseException("[REST] Expected -, LIT or ']'", this.cursor);
	}

	private char consumeLiteral() throws RedeggsParseException {
		if (this.isLiteral()) {
			return this.consume();
		}

		throw new RedeggsParseException("[LIT] Expected Literal", this.cursor);
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
		this.input = regex;
		this.cursor = 0;

		RegularEggspression expr = null;

		while (!this.isTheEnd()) {
			expr = this.regex();
		}

		return expr;
	}
}
