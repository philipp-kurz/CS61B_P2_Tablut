package tablut;

import java.util.List;

import static tablut.Piece.BLACK;
import static tablut.Piece.WHITE;
import static tablut.Utils.error;

/** A Player that automatically generates moves.
 *  @author Philipp
 */
class AI extends Player {

    /** A position-score magnitude indicating a win (for white if positive,
     *  black if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 20;
    /** A position-score magnitude indicating a forced win in a subsequent
     *  move.  This differs from WINNING_VALUE to avoid putting off wins. */
    private static final int WILL_WIN_VALUE = Integer.MAX_VALUE - 40;

    /** A position-score magnitude for each white piece. */
    private static final int WHITE_PIECE_VALUE = 300;

    /** A position-score magnitude for each black piece. */
    private static final int BLACK_PIECE_VALUE = 250;

    /** White close-to-win-value. */
    private static final int WHITE_CLOSE_TO_WIN = 10000;

    /** Black close-to-win-value. */
    private static final int BLACK_CLOSE_TO_WIN = 5000;

    /** Random score to add to static score. */
    private static final int RANDOM_ADDED_SCORE = 5;

    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;

    /** Magnitude by which moves further in the future are reduced. */
    private static final int SUB_FUTURE_MOVE = -1000;

    /** Piece count to increase search depth. */
    private static final int NUM_PCS_TO_INC = 20;

    /** Piece count to increase search depth. */
    private static final int CRIT_NUM_OF_PCS = 3;

    /** Piece count to increase search depth. */
    private static final int MAX_DEPTH_FOR_CRIT_NUM = 6;


    /** A new AI with no piece or controller (intended to produce
     *  a template). */
    AI() {
        this(null, null);
    }

    /** A new AI playing PIECE under control of CONTROLLER. */
    AI(Piece piece, Controller controller) {
        super(piece, controller);
    }

    @Override
    Player create(Piece piece, Controller controller) {
        return new AI(piece, controller);
    }

    @Override
    String myMove() throws InterruptedException {
        Move res = null;
        if (_random) {
            List<Move> moves = board().legalMoves(_myPiece);
            int selected = _controller.randInt(moves.size());
            res = moves.get(selected);
        } else {
            res = findMove();
        }
        _controller.reportMove(res);
        return res.toString();
    }

    @Override
    boolean isManual() {
        return false;
    }

    /** Return a move for me from the current position, assuming there
     *  is a move. */
    private Move findMove() {
        Board b = new Board(board());
        int sense = _myPiece == WHITE ? 1 : -1;
        _sense = sense;
        _lastFoundMove = null;
        findMove(b, 0, true, sense, -INFTY, INFTY);
        return _lastFoundMove;
    }

    /** The move found by the last call to one of the ...FindMove methods
     *  below. */
    private Move _lastFoundMove;

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _lastFoundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _lastMoveFound.
     *  Maximizing for white, minimizing for black */
    private int findMove(Board board, int depth, boolean saveMove,
                         int sense, int alpha, int beta) {
        if (depth == maxDepth(board)) {
            return staticScore(board);
        }
        int score = -sense * INFTY;
        Piece side = sense > 0 ? WHITE : BLACK;
        List<Move> moves = board.legalMoves(side);
        for (Move move : moves) {
            Board b = new Board(board);
            b.makeMove(move);
            int tmpScore = 0;
            Piece winner = b.winner();
            if (winner == null) {
                tmpScore = _sense * SUB_FUTURE_MOVE * depth + findMove(b,
                        depth + 1, false, -sense,
                        alpha, beta);
            } else if (winner == WHITE) {
                tmpScore = WINNING_VALUE;
            } else if (winner == BLACK) {
                tmpScore = -WINNING_VALUE;
            } else {
                throw error("Invalid winner.");
            }

            if (side == WHITE) {
                if (tmpScore > score) {
                    score = tmpScore;
                    if (saveMove) {
                        _lastFoundMove = move;
                    }
                }
                alpha = Math.max(alpha, score);
            } else {
                if (tmpScore < score) {
                    score = tmpScore;
                    if (saveMove) {
                        _lastFoundMove = move;
                    }
                }
                beta = Math.min(beta, score);
            }
            if (alpha >= beta) {
                break;
            }
        }
        return score;
    }

    /** Return a heuristically determined maximum search depth
     *  based on characteristics of BOARD. */
    private static int maxDepth(Board board) {
        if (!_incDepth) {
            int pieceCount = 0;
            for (Square sq : Square.SQUARE_LIST) {
                if (board.get(sq) != Piece.EMPTY) {
                    pieceCount++;
                }
            }

            if (pieceCount < NUM_PCS_TO_INC) {
                _incDepth = true;
                _maxDepth++;
            }
        }
        return _maxDepth;
    }

    /** Return a heuristic value for BOARD. */
    private int staticScore(Board board) {
        int score = 0;
        score += board.pieceLocations(WHITE).size() * WHITE_PIECE_VALUE;
        score -= board.pieceLocations(BLACK).size() * BLACK_PIECE_VALUE;

        Square kingPos = board.kingPosition();

        board.setTurn(WHITE);
        List<Move> moves = board.legalMoves(WHITE);
        int winningMoves = 0;
        for (Move m : moves) {
            if (m.from() == kingPos && m.to().isEdge()) {
                winningMoves += 1;
            }
        }
        if (winningMoves >= 2) {
            return WILL_WIN_VALUE;
        } else if (winningMoves == 1) {
            score += WHITE_CLOSE_TO_WIN;
        }


        int scoreAdd = 0;
        for (int i = 0; i < 4; i++) {
            if (board.get(kingPos.rookMove(i, 1)) == BLACK
                    && kingPos != Board.THRONE) {
                if (scoreAdd == 0) {
                    scoreAdd = 1;
                } else {
                    scoreAdd *= 3;
                }
            }
        }
        score -= scoreAdd * BLACK_CLOSE_TO_WIN;

        score += _controller.randInt(RANDOM_ADDED_SCORE);
        score -= _controller.randInt(RANDOM_ADDED_SCORE);

        return score;
    }

    /** Determines whether moves are selected by random. */
    private boolean _random = false;
    /** Boolean that determines whether search depth has to be increased. */
    private static boolean _incDepth = false;
    /** Current maximum search depth. */
    private static int _maxDepth = 2;
    /** Stores current overall sense of AI. */
    private int _sense;
}
