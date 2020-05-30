package tablut;

import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import static tablut.Move.ROOK_MOVES;
import static tablut.Piece.*;
import static tablut.Square.*;
import static tablut.Utils.error;

/** Suppress unchecked warnings.
 * @author Philipp
 * */
@SuppressWarnings("unchecked")

/** The state of a Tablut Game.
 *  @author Philipp
 */
class Board {

    /** The number of squares on a side of the board. */
    static final int SIZE = 9;

    /** The throne (or castle) square and its four surrounding squares.. */
    static final Square THRONE = sq(4, 4),
        NTHRONE = sq(4, 5),
        STHRONE = sq(4, 3),
        WTHRONE = sq(3, 4),
        ETHRONE = sq(5, 4);

    /** Squares that surround the throne. */
    static final SqList SURROUNDING_THRONE = new SqList();
    static {
        SURROUNDING_THRONE.add(NTHRONE);
        SURROUNDING_THRONE.add(STHRONE);
        SURROUNDING_THRONE.add(WTHRONE);
        SURROUNDING_THRONE.add(ETHRONE);
    }

    /** Initial positions of attackers. */
    static final Square[] INITIAL_ATTACKERS = {
        sq(0, 3), sq(0, 4), sq(0, 5), sq(1, 4),
        sq(8, 3), sq(8, 4), sq(8, 5), sq(7, 4),
        sq(3, 0), sq(4, 0), sq(5, 0), sq(4, 1),
        sq(3, 8), sq(4, 8), sq(5, 8), sq(4, 7)
    };

    /** Initial positions of defenders of the king. */
    static final Square[] INITIAL_DEFENDERS = {
        NTHRONE, ETHRONE, STHRONE, WTHRONE,
        sq(4, 6), sq(4, 2), sq(2, 4), sq(6, 4)
    };

    /** Initializes a game board with SIZE squares on a side in the
     *  initial position. */
    Board() {
        init();
    }

    /** Initializes a copy of MODEL. */
    Board(Board model) {
        copy(model);
    }

    /** Copies MODEL into me. */
    void copy(Board model) {
        if (model == this) {
            return;
        }
        init();
        _stateSetBlack = (HashSet<Square>) model._stateSetBlack.clone();
        _stateSetWhite = (HashSet<Square>) model._stateSetWhite.clone();
        _state.copy(model._state);
        _undoStack = (Stack<Board.State>) model._undoStack.clone();
        _undoSet = (HashSet<Board.State>) model._undoSet.clone();
        _turn = model._turn;
        _repeated = model._repeated;
        _winner = model._winner;
    }

    /** Clears the board to the initial position. */
    void init() {
        _stateSetBlack = new HashSet<Square>();
        _stateSetWhite = new HashSet<Square>();
        _state = new State();
        _undoStack = new Stack<State>();
        _undoSet = new HashSet<State>();

        for (Square att : INITIAL_ATTACKERS) {
            put(BLACK, att);
        }
        for (Square def : INITIAL_DEFENDERS) {
            put(WHITE, def);
        }
        put(KING, THRONE);
        _state.setKing(THRONE);
        record();
        _turn = BLACK;
        _repeated = false;
        _winner = null;
        _moveCount = 0;
        _moveLimit = Integer.MAX_VALUE;
    }

    /** Set the move limit to LIM.  It is an error if 2*LIM <= moveCount(). */
    void setMoveLimit(int lim) {
        if (2 * lim <= _moveCount) {
            throw error("Invalid move count");
        }
        _moveLimit = lim;
    }

    /** Return a Piece representing whose move it is (WHITE or BLACK). */
    Piece turn() {
        return _turn;
    }

    /** Return the winner in the current position, or null if there is no winner
     *  yet. */
    Piece winner() {
        return _winner;
    }

    /** Record current board by adding to undo stack. */
    private void record() {
        _undoStack.push(_state);
        _undoSet.add(_state);
        _state = new State(_undoStack.peek());
    }

    /** Returns true iff this is a win due to a repeated position. */
    boolean repeatedPosition() {
        return _repeated;
    }

    /** Record current position and set winner() next mover if the current
     *  position is a repeat. */
    private void checkRepeated() {
        if (_undoSet.contains(_state)) {
            _winner = _turn.opponent();
            _repeated = true;
        }
    }

    /** Return the number of moves since the initial position that have not been
     *  undone. */
    int moveCount() {
        return _moveCount;
    }

    /** Return location of the king. */
    Square kingPosition() {
        return _state.getKing();
    }

    /** Return the contents the square at S. */
    final Piece get(Square s) {
        return get(s.col(), s.row());
    }

    /** Return the contents of the square at (COL, ROW), where
     *  0 <= COL, ROW <= 9. */
    final Piece get(int col, int row) {
        return _state.get(col, row);
    }

    /** Return the contents of the square at COL ROW. */
    final Piece get(char col, char row) {
        return get(col - 'a', row - '1');
    }

    /** Set square S to P. */
    final void put(Piece p, Square s) {
        _state.set(p, s.col(), s.row());
        switch (p) {
        case EMPTY:
            _stateSetWhite.remove(s);
            _stateSetBlack.remove(s);
            break;
        case KING:
            _state.setKing(s);
            _stateSetWhite.add(s);
            break;
        case WHITE:
            _stateSetWhite.add(s);
            break;
        case BLACK:
            _stateSetBlack.add(s);
            break;
        default:
            throw error("Invalid piece!");
        }
    }

    /** Set square S to P and record for undoing. */
    final void revPut(Piece p, Square s) {
        put(p, s);
        record();
    }

    /** Set square COL ROW to P. */
    final void put(Piece p, char col, char row) {
        put(p, sq(col - 'a', row - '1'));
    }

    /** Return true iff FROM - TO is an unblocked rook move on the current
     *  board.  For this to be true, FROM-TO must be a rook move and the
     *  squares along it, other than FROM, must be empty. */
    boolean isUnblockedMove(Square from, Square to) {
        int dir = from.direction(to);
        Square sq = from;
        while (sq != to) {
            sq = sq.rookMove(dir, 1);
            if (_state.get(sq.col(), sq.row()) != EMPTY) {
                return false;
            }
        }
        return true;
    }

    /** Return true iff FROM is a valid starting square for a move. */
    boolean isLegal(Square from) {
        return get(from).side() == _turn;
    }

    /** Return true iff FROM-TO is a valid move.
     *
     * static Move mv(Square from, Square to) already checks if move is
     * generally possible in terms of positions.
     * Only have to check if path is free.
     *
     * */
    boolean isLegal(Square from, Square to) {
        return _winner == null
                && isLegal(from)
                && isUnblockedMove(from, to)
                && (to != THRONE || get(from) == KING);
    }

    /** Return true iff MOVE is a legal move in the current
     *  position. */
    boolean isLegal(Move move) {
        return move != null && isLegal(move.from(), move.to());
    }

    /** Move FROM-TO, assuming this is a legal move. */
    void makeMove(Square from, Square to) {
        if (!isLegal(from, to)) {
            System.out.println("Error");
        }
        assert isLegal(from, to);

        put(get(from), to);
        put(EMPTY, from);
        checkRepeated();
        _turn = _turn.opponent();
        _moveCount += 1;
        SqList capturePartners = tryCapture(to);
        for (Square partner : capturePartners) {
            capture(to, partner);
        }
        record();

        if (_moveCount / 2 >= _moveLimit) {
            _winner = _turn.opponent();
        } else if (get(to) == KING && to.isEdge()) {
            _winner = WHITE;
        } else if (_state._kingPosition == null) {
            _winner = BLACK;
        } else if (!hasMove(WHITE)) {
            _winner = BLACK;
        } else if (!hasMove(BLACK)) {
            _winner = WHITE;
        }
    }


    /** Move according to MOVE, assuming it is a legal move. */
    void makeMove(Move move) {
        makeMove(move.from(), move.to());
    }

    /** Returns list of partner squares which pieces in between can
     * be captured by moving to SQ. */
    private SqList tryCapture(Square sq) {
        SqList sl = new SqList();
        for (int i = 0; i < 4; i += 1) {
            Piece side = get(sq).side();
            Square sq2 = sq.rookMove(i, 2);
            boolean add = false;
            if (sq2 != null) {
                Square between = sq.between(sq2);
                if (get(between).side() == side.opponent()) {
                    if (get(between) != KING && isAllied(side, sq2)) {
                        add = true;
                    } else if (get(between) == KING) {
                        if (THRONE != between
                                && !SURROUNDING_THRONE.contains(between)
                                && isAllied(side, sq2)) {
                            add = true;
                        } else {
                            int count = 0;
                            for (int j = 0; j < 4; j += 1) {
                                if (isAllied(side, between.rookMove(j, 1))) {
                                    count += 1;
                                }
                            }
                            if (count > 3) {
                                add = true;
                            }
                        }
                    }
                }
            }
            if (add) {
                sl.add(sq2);
            }
        }
        return sl;
    }

    /** Returns true if square SQ is allied to side P. */
    private boolean isAllied(Piece p, Square sq) {
        Piece side = p.side();
        if (get(sq).side() == side) {
            return true;
        } else if (sq == THRONE && get(THRONE) == EMPTY) {
            return true;
        } else if (side == BLACK && sq == THRONE && get(THRONE) != EMPTY) {
            int count = 0;
            for (Square surr : SURROUNDING_THRONE) {
                if (get(surr) == BLACK) {
                    count += 1;
                }
            }
            if (count >= 3) {
                return true;
            }
        }
        return false;
    }

    /** Capture the piece between SQ0 and SQ2, assuming a piece just moved to
     *  SQ0 and the necessary conditions are satisfied. */
    private void capture(Square sq0, Square sq2) {
        Square between = sq0.between(sq2);
        if (get(between) == KING) {
            _state._kingPosition = null;
        }
        put(EMPTY, between);
    }

    /** Undo one move.  Has no effect on the initial board. */
    void undo() {
        if (_moveCount > 0) {
            undoPosition();
        }
    }

    /** Remove record of current position in the set of positions encountered,
     *  unless it is a repeated position or we are at the first move. */
    private void undoPosition() {
        _state = new State(_undoStack.pop());
        _undoSet.remove(_state);
        _state = new State(_undoStack.pop());
        _undoSet.remove(_state);
        _moveCount -= 1;
        _turn = _turn.opponent();
        _repeated = false;
        _winner = null;
        _stateSetWhite = new HashSet<Square>();
        _stateSetBlack = new HashSet<Square>();
        for (Square sq : SQUARE_LIST) {
            switch (get(sq).side()) {
            case WHITE:
                _stateSetWhite.add(sq);
                break;
            case BLACK:
                _stateSetBlack.add(sq);
                break;
            default:
            }
        }
        record();
    }

    /** Clear the undo stack and board-position counts. Does not modify the
     *  current position or win status. */
    void clearUndo() {
        while (_undoStack.size() > 1) {
            _undoStack.pop();
        }
        _undoSet.clear();
        _undoSet.add(_undoStack.peek());
        _moveCount = 0;
    }

    /** Return a new mutable list of all legal moves on the current board for
     *  SIDE (ignoring whose turn it is at the moment). */
    List<Move> legalMoves(Piece side) {
        Move.MoveList moves = new Move.MoveList();
        HashSet<Square> locs = pieceLocations(side);
        if (side == WHITE) {
            int i = kingPosition().index();
            for (int dir = 0; dir < 4; dir += 1) {
                for (Move m : ROOK_MOVES[i][dir]) {
                    if (get(m.to()) != EMPTY) {
                        break;
                    }
                    if (isLegal(m)) {
                        moves.add(m);
                    }
                }
            }
        }
        for (Square sq : locs) {
            if (get(sq) == KING) {
                continue;
            }
            int i = sq.index();
            for (int dir = 0; dir < 4; dir += 1) {
                for (Move m : ROOK_MOVES[i][dir]) {
                    if (get(m.to()) != EMPTY || m.to() == THRONE) {
                        break;
                    }
                    moves.add(m);
                }
            }
        }
        return moves;
    }

    /** Return true iff SIDE has a legal move. */
    boolean hasMove(Piece side) {
        return legalMoves(side).size() > 0;
    }

    /** Sets turn to SIDE. Useful for testing. */
    void setTurn(Piece side) {
        _turn = side.side();
    }

    @Override
    public String toString() {
        return toString(true);
    }

    /** Return a text representation of this Board.  If COORDINATES, then row
     *  and column designations are included along the left and bottom sides.
     */
    String toString(boolean coordinates) {
        Formatter out = new Formatter();
        for (int r = SIZE - 1; r >= 0; r -= 1) {
            if (coordinates) {
                out.format("%2d", r + 1);
            } else {
                out.format("  ");
            }
            for (int c = 0; c < SIZE; c += 1) {
                out.format(" %s", get(c, r));
            }
            out.format("%n");
        }
        if (coordinates) {
            out.format("  ");
            for (char c = 'a'; c <= 'i'; c += 1) {
                out.format(" %c", c);
            }
            out.format("%n");
        }
        return out.toString();
    }

    /** Return the locations of all pieces on SIDE. */
    HashSet<Square> pieceLocations(Piece side) {
        assert side != EMPTY;
        return side.side() == WHITE ? _stateSetWhite : _stateSetBlack;
    }

    /** Return the contents of _board in the order of SQUARE_LIST as a sequence
     *  of characters: the toString values of the current turn and Pieces. */
    String encodedBoard() {
        char[] result = new char[Square.SQUARE_LIST.size() + 1];
        result[0] = turn().toString().charAt(0);
        for (Square sq : SQUARE_LIST) {
            result[sq.index() + 1] = get(sq).toString().charAt(0);
        }
        return new String(result);
    }

    /** Piece whose turn it is (WHITE or BLACK). */
    private Piece _turn;
    /** Cached value of winner on this board, or null if it has not been
     *  computed. */
    private Piece _winner;
    /** Number of (still undone) moves since initial position. */
    private int _moveCount;
    /** True when current board is a repeated position (ending the game). */
    private boolean _repeated;

    /** The positions of pieces of a Tablut Game.
     *  @author Philipp
     */
    private class State {
        /** Pieces on Tablut board. */
        private Piece[][] _stateMatrix;
        /** Position of king. */
        private Square _kingPosition;

        /** Initialize state matrix with default board size. */
        State() {
            this(SIZE);
        }

        /** Initialize board with SIZE rows and columns. */
        State(int size) {
            _stateMatrix = new Piece[size][size];
            for (int i = 0; i < SIZE; i += 1) {
                for (int j = 0; j < SIZE; j += 1) {
                    _stateMatrix[i][j] = EMPTY;
                }
            }
        }

        /** Copy STATE into me. */
        void copy(State state) {
            for (int i = 0; i < _stateMatrix.length; i += 1) {
                for (int j = 0; j < _stateMatrix[0].length; j += 1) {
                    _stateMatrix[i][j] = state.get(i, j);
                }
            }
            _kingPosition = state._kingPosition;
        }

        @Override
        public String toString() {
            return toString(true);
        }

        /** Return a text representation of this Board. If COORDINATES, then row
         *  and column designations are included along the left and bottom
         *  sides.
         */
        String toString(boolean coordinates) {
            Formatter out = new Formatter();
            for (int r = SIZE - 1; r >= 0; r -= 1) {
                if (coordinates) {
                    out.format("%2d", r + 1);
                } else {
                    out.format("  ");
                }
                for (int c = 0; c < SIZE; c += 1) {
                    out.format(" %s", get(c, r));
                }
                out.format("%n");
            }
            if (coordinates) {
                out.format("  ");
                for (char c = 'a'; c <= 'i'; c += 1) {
                    out.format(" %c", c);
                }
                out.format("%n");
            }
            return out.toString();
        }

        /** Initialize by copying STATE into me. */
        State(State state) {
            this();
            copy(state);
        }

        /** Returns piece at COL and ROW. */
        Piece get(int col, int row) {
            return _stateMatrix[col][row];
        }

        /** Set piece at COL and ROW to P. */
        void set(Piece p, int col, int row) {
            _stateMatrix[col][row] = p;
        }

        /** Returns king position. */
        Square getKing() {
            return _kingPosition;
        }

        /** Set king position to SQ. */
        void setKing(Square sq) {
            _kingPosition = sq;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            for (int i = 0; i < 4; i += 1) {
                hash |= _stateMatrix[4][i].value() << 2 * i;
                hash |= _stateMatrix[i][4].value() << 2 * (i + 8);
            }
            for (int i = 5; i < 9; i += 1) {
                hash += _stateMatrix[4][i].value() << 2 * (i - 1);
                hash += _stateMatrix[i][4].value() << 2 * (i + 7);
            }
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            assert (obj instanceof State);
            State st = (State) obj;
            for (int i = 0; i < SIZE; i += 1) {
                for (int j = 0;  j < SIZE; j += 1) {
                    if (_stateMatrix[i][j] != st.get(i, j)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    /** Current state of board. */
    private State _state;
    /** Positions of all black pieces. */
    private HashSet<Square> _stateSetBlack;
    /** Positions of all white pieces. */
    private HashSet<Square> _stateSetWhite;
    /** Stack that holds last board states for undoing. */
    private Stack<State> _undoStack;
    /** Set that holds all old positions. */
    private HashSet<State> _undoSet;
    /** Stores the maximum number of moves. */
    private int _moveLimit;
}
