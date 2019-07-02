import java.util.*;
import java.awt.*;

public abstract class Player {

    public static enum PlayerType {
        HUMAN, AI;
    }
    
    // POSITIONS ARE IN PLAYER POV!!!
    private String m_name;                  // player name
    private int m_num_pieces, m_king_count; // number of player pieces, number of kings
    private Set<Piece> m_pieces, m_animating_captured_pieces;  // set of pieces
    public final int m_int_label;        // integer label for player in internal game board representation
    public final boolean m_reflect_pos;  // if piece positions are opposite game POV and need to be reflected
    public final Color m_piece_color, m_crown_color; // color of player piece and crown
    public final PlayerType m_type;      // player type

    // constructor
    public Player(String name, int board_label, boolean reflect, Color piece_color, 
                  Color crown_color, PlayerType type) {
        m_name = name;
        m_int_label = board_label;
        m_reflect_pos = reflect;
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
        if (piece.isKing()) {
            m_king_count--;
        }
        m_num_pieces--;
    }

    public void updateKingCount(Piece piece) {
        if (m_pieces.contains(piece)) {
            m_king_count++;
        }
    }

    // get array of positions of all pieces of the player
    public Set<Integer> getPiecePositions() {
        Set<Integer> positions = new HashSet<Integer>();
        for (Piece piece : m_pieces) {
            positions.add(piece.getPos());
        }
        return positions;
    }

    // get if player has a king piece
    public boolean hasKing() {
        return (m_king_count > 0);
    }

    // get the piece at a given position
    public Piece pieceAtPos(int pos) {
        for (Piece piece : m_pieces) {
            if (pos == piece.getPos()) {
                return piece;
            }
        }
        return null;
    }

    // clear all player pieces
    public void clearPieces() {
        m_num_pieces = 0;
        m_king_count = 0;
        m_pieces = new HashSet<Piece>();
        m_animating_captured_pieces = new HashSet<Piece>();
    }

    // check if any of the player's pieces is moving/being animated
    public boolean isAnimating() {
        for (Piece piece : m_pieces) {
            if (piece.isMoving()) return true;
        }
        for (Piece piece : m_animating_captured_pieces) {
            if (piece.isMoving()) return true;
        }
        return false;
    }

    // draw all of the player's pieces
    public void draw(Graphics g, boolean has_focus) {
        // draw all of player's piece
        for (Piece piece : m_pieces) {
            piece.draw(g, has_focus);
        }

        // for pieces that have been captured, draw the ones still animating and 
        // coalate the ones that are done moving
        Set<Piece> to_remove = new HashSet<Piece>();
        for (Piece piece : m_animating_captured_pieces) {
            if (piece.isMoving()) {
                piece.draw(g, has_focus);
            } else {
                to_remove.add(piece);
            }
        }

        // remove pieces that have been captured and are done animating
        for (Piece piece : to_remove) {
            m_animating_captured_pieces.remove(piece);
        }
    }
}