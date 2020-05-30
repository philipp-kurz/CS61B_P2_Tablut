package tablut;

/** The contents of a cell on the board.
 *  @author P. N. Hilfinger
 */
enum Piece {


    /* EMPTY: empty square. WHITE, BLACK, and KING: pieces. */
    EMPTY("-", null, 0), WHITE("W", "White", 1), BLACK("B", "Black", 2),
    KING("K", "King", 3);


    /** A Piece whose board symbol is SYMBOL and that is called NAME in
     *  messages and has enum integer VALUE. */
    Piece(String symbol, String name, int value) {
        _symbol = symbol;
        _name = name;
        _value = value;
    }

    @Override
    public String toString() {
        return _symbol;
    }

    /** Return the class of PIECE.  This is either BLACK, WHITE, or
     *  EMPTY.  Usually the identify, except for the King, which is a DEFENGER.
     */
    Piece side() {
        return this == KING ? WHITE : this;
    }

    /** Return the Piece of opposing color, or null if this is not a
     *  player piece. */
    Piece opponent() {
        switch (this) {
        case WHITE: case KING:
            return BLACK;
        case BLACK:
            return WHITE;
        default:
            return null;
        }
    }

    /** Return my printed form for use in messages. */
    String toName() {
        return _name;
    }

    /** Return my enum integer value. */
    int value() {
        return _value;
    }

    /** The symbol used for the piece in textual board printouts. */
    private final String _symbol;
    /** The name in used in messages. */
    private final String _name;
    /** Integer value for different enum states. */
    private final int _value;
}
