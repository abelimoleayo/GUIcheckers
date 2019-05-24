import java.util.*;
import java.awt.Graphics;

public class Piece {
    private static int s_ID_tracker = 0;
    private final int m_ID, m_piece_width, m_cell_width;
    private final Player m_owner;
    private int m_position, m_capture_index, m_jump_stop_index;
    private int m_DeltaX, m_DeltaY, m_animation_step, m_total_animation_steps;
    private int[] m_global_coords, m_last_stop_coords;
    private int[][] m_stop_coords;
    private double m_curr_x, m_curr_y;
	private boolean m_isKing, m_isMoving, m_isCaptured;

	public Piece(int pos, Player owner) {
        m_ID = s_ID_tracker++;
        m_piece_width = Checkers.getBoardPieceWidth();
        m_cell_width = Checkers.getBoardCellWidth();
        m_owner = owner;
        m_position = pos;
        m_global_coords = posToCenterCoords(pos);
        m_isKing = false;
        m_isMoving = false;
        m_isCaptured = false;
        m_total_animation_steps = Checkers.s_animation_steps;
	}

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

    // canMoveBackward: only king
    public boolean isKing() {
    	return m_isKing;
    }

    public void makeKing() {
        m_isKing = true;
    }

    // get position of piece on board	
	public int getPos() {	
		return m_position;
	}

    public boolean isMoving() {
        return m_isMoving;
    }

    public void isCaptured(int capture_index) {
        m_isMoving = true;
        m_isCaptured = true;
        m_capture_index = capture_index;
    }

    private int[] posToCenterCoords(int pos) {
        int global_pos = m_owner.m_reflect_pos ? Game.reflectPosition(pos) : pos;
        int[] top_left = Checkers.getTopLeftCoordFromPos(global_pos);
        return new int[] {top_left[0] + m_cell_width/2, top_left[1] + m_cell_width/2};
    }

    // set position of piece on board
    public void setPos(List<Integer> stops) {
        m_isMoving = true;
        m_last_stop_coords = m_global_coords;

        int stop_length = stops.size();
        m_position = stops.get(stop_length - 1);
        m_stop_coords = new int[stop_length][2];
        for (int i=0; i<stop_length; i++) {
            m_stop_coords[i] = posToCenterCoords(stops.get(i));
        }
        m_global_coords = m_stop_coords[stop_length-1];

        m_DeltaX = m_stop_coords[0][0] - m_last_stop_coords[0];
        m_DeltaY = m_stop_coords[0][1] - m_last_stop_coords[1];
    }

    private int[] updatePos() {
        int[] center_coords = m_global_coords;
        if (m_isMoving) {
            m_animation_step++;
            if (m_isCaptured) {
                if (m_animation_step == (int)(m_total_animation_steps*(m_capture_index + 0.5))) {
                    m_isMoving = false;
                }
            } else {
                // jumpinf
                if (m_animation_step == m_total_animation_steps) {
                    center_coords = m_stop_coords[m_jump_stop_index];
                    m_animation_step = 0;
                    if (m_jump_stop_index >= m_stop_coords.length - 1) {
                        m_isMoving = false;
                        m_jump_stop_index = 0;
                    } else {
                        m_last_stop_coords = center_coords;
                        m_jump_stop_index++;
                        m_DeltaX = m_stop_coords[m_jump_stop_index][0] - m_last_stop_coords[0];
                        m_DeltaY = m_stop_coords[m_jump_stop_index][1] - m_last_stop_coords[1];
                    }
                } else {
                    int dx = (int) (((double) (m_DeltaX * m_animation_step))/m_total_animation_steps);
                    int dy = (int) (((double) (m_DeltaY * m_animation_step))/m_total_animation_steps);
                    int newX = m_last_stop_coords[0] + dx;
                    int newY = m_last_stop_coords[1] + dy;
                    center_coords = new int[] {newX, newY};
                }
            }
        }

        return center_coords;       
    }

    public void draw(Graphics g) {
        int[] coords = updatePos();         
        g.setColor(m_owner.m_piece_color);
        g.fillOval(coords[0] - m_piece_width/2,
                   coords[1] - m_piece_width/2, m_piece_width, m_piece_width);
        if (m_isKing) {
            UtilityFuncs.drawKingIcon(g, coords[0], coords[1], 
                                      m_piece_width, m_owner.m_crown_color);
        }
    }
    
}
