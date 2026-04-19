public class Token {
    public final TokenType type;
    public final String value;
    public final int position;
    public final int index;
    public final int line;

    public Token(TokenType type, String value, int position, int index, int line) {
        this.type = type;
        this.value = value;
        this.position = position;
        this.index = index;
        this.line = line;
    }
}
