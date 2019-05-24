import java.util.*;
import java.awt.*;

public abstract class Player {

    public static enum PlayerType {
        HUMAN, AI;
    }

    // POSITIONS ARE IN PLAYER POV!!!
    private String m_name;
    private int m_num_pieces;
    private Set<Piece> m_pieces;
    private Set<Piece> m_animating_captured_pieces;
    public final int m_int_label; 
    public final boolean m_reflect_pos;
    public final Color m_piece_color;
    public final Color m_crown_color;
    public final PlayerType m_type;

    public Player(String name, int board_label, boolean reflect, Color piece_color, 
                  Color crown_color, PlayerType type) {
        m_name = name;
        m_int_label = board_label;
        m_reflect_pos = reflect;
        m_num_pieces = 0;
        m_piece_color = piece_color;
        m_crown_color = crown_color;
        m_pieces = new HashSet<Piece>();
        m_animating_captured_pieces = new HashSet<Piece>();
        m_type = type;
    }

    public String getName() {
        return m_name;
    }

    public void addPiece(Piece piece) {
        m_pieces.add(piece);
        m_num_pieces++;
    }

    public void removePiece(Piece piece, int capture_animation_index) {
        m_pieces.remove(piece);
        m_animating_captured_pieces.add(piece);
        piece.isCaptured(capture_animation_index);
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
        m_animating_captured_pieces = new HashSet<Piece>();
    }

    public boolean isAnimating() {
        for (Piece piece : m_pieces) {
            if (piece.isMoving()) return true;
        }
        for (Piece piece : m_animating_captured_pieces) {
            if (piece.isMoving()) return true;
        }
        return false;
    }

    public void draw(Graphics g) {
        for (Piece piece : m_pieces) {
            piece.draw(g);
        }
        Set<Piece> to_remove = new HashSet<Piece>();
        for (Piece piece : m_animating_captured_pieces) {
            if (piece.isMoving()) {
                piece.draw(g);
            } else {
                to_remove.add(piece);
            }
        }
        for (Piece piece : to_remove) {
            m_animating_captured_pieces.remove(piece);
        }
    }
}