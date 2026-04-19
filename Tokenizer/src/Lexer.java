import java.util.*;

public class Lexer {

    private String input;
    private int pos;
    private int line;
    private int col;
    private int length;
    private final static Set<String> KEYWORDS = new HashSet<>();

    static {
        KEYWORDS.addAll(Arrays.asList(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue",
            "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto",
            "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "package",
            "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch",
            "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while"
        ));
    }

    private Lexer(String input) {
        this.input = input;
        this.pos = 0;
        this.line = 1;
        this.col = 1;
        this.length = input.length();
    }

    public static List<Token> lex(String input) {
        return new Lexer(input).tokenize();
    }

    private List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        int tokenIdx = 0;

        while (pos < length) {
            char c = peek();

            if (Character.isWhitespace(c)) {
                scanWhitespace();
                continue;
            }

            int startPos = pos;
            int startL   = line;
            int startC   = col;

            if (c == '/' && peek(1) == '/') {
                tokens.add(scanLineComment(startPos, startL, tokenIdx++));
                continue;
            }
            if (c == '/' && peek(1) == '*') {
                tokens.add(scanBlockComment(startPos, startL, tokenIdx++));
                continue;
            }

            if (c == '"') {
                tokens.add(scanString(startPos, startL, tokenIdx++));
                continue;
            }
            if (c == '\'') {
                tokens.add(scanChar(startPos, startL, tokenIdx++));
                continue;
            }

            if (Character.isDigit(c) || (c == '.' && Character.isDigit(peek(1)))) {
                tokens.add(scanNumber(startPos, startL, tokenIdx++));
                continue;
            }

            if (isIdentifierStart(c)) {
                tokens.add(scanIdentifierOrKeyword(startPos, startL, tokenIdx++));
                continue;
            }

            if (isOperatorChar(c)) {
                tokens.add(scanOperator(startPos, startL, tokenIdx++));
                continue;
            }

            if (isSeparator(c)) {
                tokens.add(new Token(TokenType.SEPARATOR, String.valueOf(consume()), startPos, tokenIdx++, startL));
                continue;
            }

            tokens.add(new Token(TokenType.UNKNOWN, String.valueOf(consume()), startPos, tokenIdx++, startL));
        }

        return tokens;
    }

    private char peek() {
        return pos < length ? input.charAt(pos) : '\0';
    }

    private char peek(int n) {
        return (pos + n) < length ? input.charAt(pos + n) : '\0';
    }

    private char consume() {
        char c = peek();
        pos++;
        if (c == '\n') {
            line++;
            col = 1;
        } else {
            col++;
        }
        return c;
    }

    private void scanWhitespace() {
        while (pos < length && Character.isWhitespace(peek())) {
            consume();
        }
    }

    private Token scanLineComment(int startPos, int startL, int idx) {
        StringBuilder sb = new StringBuilder();
        while (pos < length && peek() != '\n') {
            sb.append(consume());
        }
        return new Token(TokenType.COMMENT, sb.toString(), startPos, idx, startL);
    }

    private Token scanBlockComment(int startPos, int startL, int idx) {
        StringBuilder sb = new StringBuilder();
        sb.append(consume());
        sb.append(consume()); 
        while (pos < length) {
            if (peek() == '*' && peek(1) == '/') {
                sb.append(consume());
                sb.append(consume());
                break;
            }
            sb.append(consume());
        }
        return new Token(TokenType.COMMENT, sb.toString(), startPos, idx, startL);
    }

    private Token scanString(int startPos, int startL, int idx) {
        StringBuilder sb = new StringBuilder();
        char quote = consume();
        sb.append(quote);
        while (pos < length && peek() != quote) {
            if (peek() == '\\') {
                sb.append(consume());
            }
            sb.append(consume());
        }
        if (peek() == quote)
            sb.append(consume());
        return new Token(TokenType.STRING_LITERAL, sb.toString(), startPos, idx, startL);
    }

    private Token scanChar(int startPos, int startL, int idx) {
        StringBuilder sb = new StringBuilder();
        char quote = consume();
        sb.append(quote);
        while (pos < length && peek() != quote) {
            if (peek() == '\\') {
                sb.append(consume());
            }
            sb.append(consume());
        }
        if (peek() == quote)
            sb.append(consume());
        return new Token(TokenType.CHAR_LITERAL, sb.toString(), startPos, idx, startL);
    }

    private Token scanNumber(int startPos, int startL, int idx) {
        StringBuilder sb = new StringBuilder();
        boolean isFloat = false;

        if (peek() == '0' && (peek(1) == 'x' || peek(1) == 'X')) {
            sb.append(consume());
            sb.append(consume());
            while (isHexDigit(peek()))
                sb.append(consume());
            if (peek() == 'l' || peek() == 'L')
                sb.append(consume());
            return new Token(TokenType.INTEGER_LITERAL, sb.toString(), startPos, idx, startL);
        }

        while (Character.isDigit(peek()))
            sb.append(consume());

        if (peek() == '.') {
            isFloat = true;
            sb.append(consume());
            while (Character.isDigit(peek()))
                sb.append(consume());
        }

        if (peek() == 'e' || peek() == 'E') {
            isFloat = true;
            sb.append(consume());
            if (peek() == '+' || peek() == '-')
                sb.append(consume());
            while (Character.isDigit(peek()))
                sb.append(consume());
        }

        char last = peek();
        if ("fFdD".indexOf(last) != -1 && last != '\0') {
            isFloat = true;
            sb.append(consume());
        } else if ("lL".indexOf(last) != -1 && last != '\0') {
            sb.append(consume());
        }

        return new Token(isFloat ? TokenType.FLOAT_LITERAL : TokenType.INTEGER_LITERAL, sb.toString(), startPos,
                idx, startL);
    }

    private Token scanIdentifierOrKeyword(int startPos, int startL, int idx) {
        StringBuilder sb = new StringBuilder();
        while (isIdentifierPart(peek())) {
            sb.append(consume());
        }
        String val = sb.toString();
        
        if (val.equals("true") || val.equals("false")) {
            return new Token(TokenType.BOOLEAN_LITERAL, val, startPos, idx, startL);
        }
        if (val.equals("null")) {
            return new Token(TokenType.NULL_LITERAL, val, startPos, idx, startL);
        }
        if (KEYWORDS.contains(val)) {
            return new Token(TokenType.KEYWORD, val, startPos, idx, startL);
        }

        return new Token(TokenType.IDENTIFIER, val, startPos, idx, startL);
    }

    private Token scanOperator(int startPos, int startL, int idx) {
        StringBuilder sb = new StringBuilder();
        String[] multi = { ">>>=", ">>>", "<<=", ">>=", "==", "!=", "<=", ">=", "&&", "||", "++", "--", "<<", ">>",
                "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "->", "::" };
        for (String m : multi) {
            boolean match = true;
            for (int i = 0; i < m.length(); i++) {
                if (peek(i) != m.charAt(i)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                for (int i = 0; i < m.length(); i++)
                    sb.append(consume());
                return new Token(TokenType.OPERATOR, sb.toString(), startPos, idx, startL);
            }
        }
        sb.append(consume());
        return new Token(TokenType.OPERATOR, sb.toString(), startPos, idx, startL);
    }

    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
    }

    private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    private boolean isHexDigit(char c) {
        return Character.isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private boolean isOperatorChar(char c) {
        return "+-*/%&|^~!<>=:?".indexOf(c) != -1;
    }

    private boolean isSeparator(char c) {
        return "{}()[];,.".indexOf(c) != -1;
    }
}
