public class Piece {

    private static int s_ID_tracker = 0;
    private int m_ID;
	private int m_position;
	private boolean m_isKing;

	public Piece(int pos) {
        m_ID = s_ID_tracker++;
		m_position = pos;
        m_isKing = false;
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

    // set position of piece on board
    public void setPos(int position) {
        m_position = position;
    }
    
}
