import java.awt.*;

public class Piece {
    private static int s_ID_tracker = 0;
    private final int m_ID, m_piece_width, m_cell_width;
    private final Player m_owner;
    private int m_position, m_global_pos;
    private double m_curr_x, m_curr_y;
	private boolean m_isKing, m_isMoving;

	public Piece(int pos, Player owner) {
        m_ID = s_ID_tracker++;
        m_piece_width = Checkers.getBoardPieceWidth();
        m_cell_width = Checkers.getBoardCellWidth();
        m_owner = owner;
        m_position = pos;
        m_global_pos = m_owner.m_reflect_pos ? Game.reflectPosition(m_position) : m_position;
        m_isKing = false;
        m_isMoving = false;
        m_isCaptured = false;
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

    public void isCaptured() {
        m_isCaptured = true;
    }

    // set position of piece on board
    public void setPos(int position) {
        m_isMoving = true;
        m_
        m_position = position;
        m_global_pos = m_owner.m_reflect_pos ? Game.reflectPosition(m_position) : m_position;
    }

    public void draw(Graphics g) {
        // consider animation! when piece isn't on a cell

        /* if (ismoving) {
                update % of trip made
                draw

        */
        int[] coords = Checkers.getTopLeftCoordFromPos(m_global_pos);  
        g.setColor(m_owner.m_piece_color);
        g.fillOval(coords[0] + m_cell_width/2 - m_piece_width/2,
                   coords[1] + m_cell_width/2 - m_piece_width/2, m_piece_width, m_piece_width);
        if (m_isKing) {
            UtilityFuncs.drawKingIcon(g, coords[0] + m_cell_width/2, coords[1] + m_cell_width/2, 
                                      m_piece_width, m_owner.m_crown_color);
        }
    }
    
}
