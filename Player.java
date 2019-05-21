import java.util.*;
import java.awt.*;

public abstract class Player {

    // POSITIONS ARE IN PLAYER POV!!!

    // TODO?: add static variable to check that constructor board_label doesn't already exist 

    private String m_name;
    private int m_num_pieces;
    private Set<Piece> m_pieces;
    public final int m_int_label; 
    public final boolean m_reflect_pos;
    public final Color m_piece_color;
    public final Color m_crown_color;

    public Player(String name, int board_label, boolean reflect, Color piece_color, 
                  Color crown_color) {
        m_name = name;
        m_int_label = board_label;
        m_reflect_pos = reflect;
        m_num_pieces = 0;
        m_piece_color = piece_color;
        m_crown_color = crown_color;
        m_pieces = new HashSet<Piece>();
    }

    public String getName() {
        return m_name;
    }

    public void addPiece(Piece piece) {
        m_pieces.add(piece);
        m_num_pieces++;
    }

    public void removePiece(Piece piece) {
        m_pieces.remove(piece);
        piece.isCaptured();
        m_num_pieces--;
    }

    public int[] getPiecePositions() {
        int[] positions = new int[m_num_pieces];
        int index = 0;
        for (Piece piece : m_pieces) {
            positions[index++] = piece.getPos(); 
        }
        return positions;
    }

    public Piece pieceAtPos(int pos) {
        for (Piece piece : m_pieces) {
            if (pos == piece.getPos()) {
                return piece;
            }
        }
        return null;
    }

    public void clearPieces() {
        m_num_pieces = 0;
        m_pieces = new HashSet<Piece>();
    }

    public boolean animating() {
        for (Piece piece : m_pieces) {
            if (piece.isMoving()) return true;
        }
        return false;
    }

    public void draw(Graphics g) {
        for (Piece p : m_pieces) {
            p.draw(g);
        }
    }

    public abstract int getMove(Map<Integer,Set<String>> moveable_pieces_pos, int[][] game_board, 
                                boolean source_selected);

}