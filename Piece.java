import java.util.*;
import java.awt.Graphics;

public class Piece {
    // static variables
    private static int s_ID_tracker = 0;

    // instance variables
    private final int m_ID, m_piece_width, m_cell_width;
    private final Player m_owner;
    private int m_position, m_capture_index, m_jump_stop_index, m_DeltaX, m_DeltaY, 
                m_animation_step, m_animation_steps_per_stop;
    private int[] m_global_coords, m_last_stop_coords, m_prior_coords;
    private int[][] m_stop_coords;
	private boolean m_to_make_king, m_isKing, m_isMoving, m_isCaptured;

	public Piece(int pos, Player owner) {
        m_ID = s_ID_tracker++;
        m_piece_width = Checkers.getBoardPieceWidth();
        m_cell_width = Checkers.getBoardCellWidth();
        m_owner = owner;
        m_position = pos;
        m_global_coords = posToCenterCoords(pos);
        m_prior_coords = m_global_coords;
        m_isKing = false;
        m_isMoving = false;
        m_isCaptured = false;
        m_animation_steps_per_stop = Checkers.s_animation_steps;
	}

    // override comparison method to allow for checking if piece is contained in a set/collection
    @Override 
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass()!= this.getClass()) {
            return false;
        }

        Piece piece = (Piece) obj;
        return (this.m_ID == piece.getID());
    }

    @Override
    public int hashCode() {
        return m_ID;
    }

    public int getID() {
        return m_ID;
    }

    public Player getOwner() {
        return m_owner;
    }

    public boolean isKing() {
    	return m_isKing;
    }

    public void makeKing() {
        m_to_make_king = true;
    }

	public int getPos() {	
		return m_position;
	}

    public boolean isMoving() {
        return m_isMoving;
    }

    public void isCaptured(int capture_index) {
        m_isMoving = true;
        m_isCaptured = true;
        m_capture_index = capture_index; // index of phase of captor's move where this piece is captured
    }

    // convert board position to x,y coordinates
    private int[] posToCenterCoords(int pos) {
        int global_pos = m_owner.m_reflect_pos ? Game.reflectPosition(pos) : pos;
        int[] top_left = Checkers.getTopLeftCoordFromPos(global_pos);
        return new int[] {top_left[0] + m_cell_width/2, top_left[1] + m_cell_width/2};
    }

    // initiate movement of piece from current position to new position
    public void setPos(List<Integer> stops) {
        m_isMoving = true;

        // cache current position before motion begins
        m_last_stop_coords = m_global_coords;     

        // compue x,y coordinates of all the stops in the move
        int stop_length = stops.size();
        m_position = stops.get(stop_length - 1);
        m_stop_coords = new int[stop_length][2];
        for (int i=0; i<stop_length; i++) {
            m_stop_coords[i] = posToCenterCoords(stops.get(i));
        }

        // update coordinates to coordinates of destination
        m_global_coords = m_stop_coords[stop_length-1]; 

        // initialize vector components between consecutive stops in piece's motion
        m_DeltaX = m_stop_coords[0][0] - m_last_stop_coords[0];
        m_DeltaY = m_stop_coords[0][1] - m_last_stop_coords[1];
    }

    // compute the x,y coordinate of the center of the piece
    private int[] computeCenterCoordinate() {
        // default return value to the non-moving coordinates of the piece
        int[] center_coords = m_global_coords;

        // if piece is moving, use number of frames since motion started to compute current coordinates
        if (m_isMoving) {                         
            m_animation_step++; // increase frame count
            if (m_isCaptured) { // piece is being captured
                // make piece disappear based on it's index in the series of captures
                if (m_animation_step == (int)(m_animation_steps_per_stop*(m_capture_index + 0.5))) {
                    m_isMoving = false;
                }
            } else {            // piece is moving to new cell or jumping
                // a phase of motion is complete (for jumps, a phase correspond to a jump over one piece)
                if (m_animation_step == m_animation_steps_per_stop) {
                    // update coordinate of piece to next stop in piece's motion
                    center_coords = m_stop_coords[m_jump_stop_index];                    
                    m_animation_step = 0; // reset frame count

                    // if current stop is the final stop/phase of motion
                    if (m_jump_stop_index >= m_stop_coords.length - 1) {
                        // motion complete, make piece king if it should be king
                        if (!m_isKing && m_to_make_king) {
                            m_isKing = true;
                        }
                        m_isMoving = false;
                        m_jump_stop_index = 0;
                    } else {  // restart new step in jump motion
                        m_last_stop_coords = center_coords;
                        m_jump_stop_index++;
                        m_DeltaX = m_stop_coords[m_jump_stop_index][0] - m_last_stop_coords[0];
                        m_DeltaY = m_stop_coords[m_jump_stop_index][1] - m_last_stop_coords[1];
                    }
                } else {  // a phase of motion is ongoing, compute position 
                    int dx = (int) (((double) (m_DeltaX * m_animation_step))/m_animation_steps_per_stop);
                    int dy = (int) (((double) (m_DeltaY * m_animation_step))/m_animation_steps_per_stop);
                    int newX = m_last_stop_coords[0] + dx;
                    int newY = m_last_stop_coords[1] + dy;
                    center_coords = new int[] {newX, newY};
                }
            }
        }

        return center_coords;       
    }

    public void draw(Graphics g, boolean has_focus) {
        // compute current coordinates of piece
        int[] coords = has_focus ? computeCenterCoordinate() : m_prior_coords;
        m_prior_coords = coords; 

        // draw the piece  (and crown if piece is a king)        
        g.setColor(m_owner.m_piece_color);
        g.fillOval(coords[0] - m_piece_width/2,
                   coords[1] - m_piece_width/2, m_piece_width, m_piece_width);
        if (m_isKing) {
            UtilityFuncs.drawKingIcon(g, coords[0], coords[1], 
                                      m_piece_width, m_owner.m_crown_color);
        }
    }
    
}
