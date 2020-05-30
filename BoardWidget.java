package tablut;

import ucb.gui2.Pad;

import java.util.concurrent.ArrayBlockingQueue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.event.MouseEvent;

import static tablut.Piece.*;
import static tablut.Square.sq;

/** A widget that displays a Tablut game.
 *  @author Philipp
 */
class BoardWidget extends Pad {

    /* Parameters controlling sizes, speeds, colors, and fonts. */

    /** Squares on each side of the board. */
    static final int SIZE = Board.SIZE;

    /** Colors of empty squares, pieces, grid lines, and boundaries. */
    static final Color
        SQUARE_COLOR = new Color(238, 207, 161),
        THRONE_COLOR = new Color(180, 255, 180),
        ADJACENT_THRONE_COLOR = new Color(200, 220, 200),
        CLICKED_SQUARE_COLOR = new Color(255, 255, 100),
        GRID_LINE_COLOR = Color.black,
        WHITE_COLOR = Color.white,
        BLACK_COLOR = Color.black,
        RIM_COLOR = Color.red;

    /** Margins. */
    static final int
        OFFSET = 2,
        MARGIN = 16;

    /** Side of single square and of board (in pixels). */
    static final int
        SQUARE_SIDE = 30,
        BOARD_SIDE = SQUARE_SIDE * SIZE + 2 * OFFSET + MARGIN;

    /** The font in which to render the "K" in the king. */
    static final Font KING_FONT = new Font("Serif", Font.BOLD, 18);
    /** The font for labeling rows and columns. */
    static final Font ROW_COL_FONT = new Font("SanSerif", Font.PLAIN, 10);

    /** Squares adjacent to the throne. */
    static final Square[] ADJACENT_THRONE = {
        Board.NTHRONE, Board.ETHRONE, Board.STHRONE, Board.WTHRONE
    };

    /** A graphical representation of a Tablut board that sends commands
     *  derived from mouse clicks to COMMANDS.  */
    BoardWidget(ArrayBlockingQueue<String> commands) {
        _commands = commands;
        setMouseHandler("click", this::mouseClicked);
        setPreferredSize(BOARD_SIDE, BOARD_SIDE);
        _acceptingMoves = false;
    }

    /** Draw the bare board G.  */
    private void drawGrid(Graphics2D g) {
        g.setColor(SQUARE_COLOR);
        g.fillRect(OFFSET, OFFSET, BOARD_SIDE, BOARD_SIDE);
        g.setColor(THRONE_COLOR);
        g.fillRect(cx(Board.THRONE), cy(Board.THRONE),
                   SQUARE_SIDE, SQUARE_SIDE);
        g.setColor(ADJACENT_THRONE_COLOR);
        for (Square sq : ADJACENT_THRONE) {
            g.fillRect(cx(sq), cy(sq),
                    SQUARE_SIDE, SQUARE_SIDE);
        }
        if (_clickedSquare != null) {
            g.setColor(CLICKED_SQUARE_COLOR);
            g.fillRect(cx(_clickedSquare), cy(_clickedSquare),
                    SQUARE_SIDE, SQUARE_SIDE);
        }

        g.setColor(GRID_LINE_COLOR);
        for (int k = 0; k <= SIZE; k += 1) {
            g.drawLine(cx(0), cy(k - 1), cx(SIZE), cy(k - 1));
            g.drawLine(cx(k), cy(-1), cx(k), cy(SIZE - 1));
        }
        for (Integer i = 9; i > 0; i -= 1) {
            g.drawString(i.toString(), OFFSET + 4,
                    OFFSET + (9 - i) * SQUARE_SIDE + SQUARE_SIDE / 2 + 4);
        }
        for (char i = 'a'; i <= 'i'; i += 1) {
            g.drawString(Character.toString(i), SQUARE_SIDE * (i - 'a')
                    + SQUARE_SIDE + 2, 9 * SQUARE_SIDE + SQUARE_SIDE / 2);
        }

    }

    @Override
    public synchronized void paintComponent(Graphics2D g) {
        drawGrid(g);
        Square.SQUARE_LIST.iterator().forEachRemaining(s -> drawPiece(g, s));
    }

    /** Draw the contents of S on G. */
    private void drawPiece(Graphics2D g, Square s) {
        Piece p = _board.get(s);
        if (p == EMPTY) {
            return;
        }
        Color c = p.side() == WHITE ? WHITE_COLOR : BLACK_COLOR;
        g.setColor(c);
        g.fillOval(cx(s) + 2, cy(s) + 2, SQUARE_SIDE - 4, SQUARE_SIDE - 4);
        g.setColor(RIM_COLOR);
        g.drawOval(cx(s) + 2, cy(s) + 2, SQUARE_SIDE - 4, SQUARE_SIDE - 4);

        if (p == KING) {
            g.setFont(KING_FONT);
            g.drawString("K", cx(s) + SQUARE_SIDE / 4,
                    cy(s) + SQUARE_SIDE * 2 / 3);
        }
    }

    /** Handle a click on S. */
    private void click(Square s) {
        if (_clickedSquare == null) {
            if (_board.isLegal(s)) {
                _clickedSquare = s;
            }
        } else {
            Move m = Move.mv(_clickedSquare, s);
            _clickedSquare = null;
            if (_board.isLegal(m)) {
                _commands.add(m.toString());
            }
        }
        repaint();
    }

    /** Handle mouse click event E. */
    private synchronized void mouseClicked(String unused, MouseEvent e) {
        int xpos = e.getX(), ypos = e.getY();
        int x = (xpos - OFFSET - MARGIN) / SQUARE_SIDE,
            y = (OFFSET - ypos) / SQUARE_SIDE + SIZE - 1;
        if (_acceptingMoves
            && x >= 0 && x < SIZE && y >= 0 && y < SIZE) {
            click(sq(x, y));
        }
    }

    /** Revise the displayed board according to BOARD. */
    synchronized void update(Board board) {
        _board.copy(board);
        repaint();
    }

    /** Turn on move collection iff COLLECTING, and clear any current
     *  partial selection.  When move collection is off, ignore clicks on
     *  the board. */
    void setMoveCollection(boolean collecting) {
        _acceptingMoves = collecting;
        repaint();
    }

    /** Return x-pixel coordinate of the left corners of column X
     *  relative to the upper-left corner of the board. */
    private int cx(int x) {
        return x * SQUARE_SIDE + OFFSET + MARGIN;
    }

    /** Return y-pixel coordinate of the upper corners of row Y
     *  relative to the upper-left corner of the board. */
    private int cy(int y) {
        return (SIZE - y - 1) * SQUARE_SIDE + OFFSET;
    }

    /** Return x-pixel coordinate of the left corner of S
     *  relative to the upper-left corner of the board. */
    private int cx(Square s) {
        return cx(s.col());
    }

    /** Return y-pixel coordinate of the upper corner of S
     *  relative to the upper-left corner of the board. */
    private int cy(Square s) {
        return cy(s.row());
    }

    /** Queue on which to post move commands (from mouse clicks). */
    private ArrayBlockingQueue<String> _commands;
    /** Board being displayed. */
    private final Board _board = new Board();

    /** True iff accepting moves from user. */
    private boolean _acceptingMoves;

    /** Square which has just been clicked on by user. */
    private Square _clickedSquare;

}
