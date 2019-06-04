import java.util.*;
import java.awt.*;        

public class BoardCell {

    // possible cell states defining color of board cell
    public static enum CellState {
        DEFAULT,            // default cell state
        VALID_SOURCE,       // piece on this cell is a valid selection
        INVALID_SELECTION,  // piece on this cell is an invalid selection
        SELECTED_SOURCE,    // user has selected this piece as the piece to move
        ON_JUMP_PATH,       // this cell is on a possible jump-path/capture-path of the selected source piece 
        ON_ANIMATION_PATH,  // this cell is on THE path of the selected move
        DESTINATION,        // this cell is a possible destination cell for the moving piece
        KING_DESTINATION;   // this cell is a possible destination cell that will crown the moving piece
    }

    // color definition for each cell state
    private static Map<CellState,Color> s_color_map = new HashMap<CellState,Color>() {
        {
            put(CellState.DEFAULT, new Color(130,81,43));            // dark brown
            put(CellState.VALID_SOURCE, new Color(255,140,0));       // orange-ish
            put(CellState.INVALID_SELECTION, Color.RED);             // red
            put(CellState.SELECTED_SOURCE, new Color(116,194,0));    // green
            put(CellState.ON_JUMP_PATH, new Color(46,130,255));      // blue
            put(CellState.ON_ANIMATION_PATH, new Color(116,194,0));  // green
            put(CellState.DESTINATION, new Color(116,194,0));        // green
            put(CellState.KING_DESTINATION, new Color(116,194,0));   // green
        }
    };

    // instance variables
    private final int m_global_pos, m_cell_width;  // position of cell and width of the cell
    private final int[] m_coords;                  // x,y coordinate of top-left corner of cell
    private CellState m_cell_state;                // cell state
    private Player m_state_setter;                 // player whose action caused cell state to change      

    // constructor
    public BoardCell(int global_pos) {
        m_global_pos = global_pos;
        m_coords = Checkers.getTopLeftCoordFromPos(m_global_pos);
        m_cell_width = Checkers.getBoardCellWidth();
        m_cell_state = CellState.DEFAULT;
    }

    // override methods for comparison (needed when checking if a set of boardcells contains a cell)
    @Override 
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        BoardCell cell = (BoardCell) obj;
        return (this.m_global_pos == cell.getGlobalPos());
    }

    @Override
    public int hashCode() {
        return m_global_pos;
    }

    public int getGlobalPos() {
        return m_global_pos;
    }

    public void setCellState(CellState cell_state, Player player) {
        m_cell_state = cell_state;
        m_state_setter = player;
    }

    public void draw(Graphics g) {
        // paint the cell with the color corresponding to it's state
        g.setColor(s_color_map.get(m_cell_state));
        g.fillRect(m_coords[0], m_coords[1], m_cell_width, m_cell_width);

        // draw crown icon on the cell if the piece becomes a king by arriving on it
        if (m_cell_state == CellState.KING_DESTINATION) {
            UtilityFuncs.drawKingIcon(g, m_coords[0]+m_cell_width/2, m_coords[1]+m_cell_width/2,
                                      Checkers.getBoardPieceWidth(), m_state_setter.m_crown_color);
        }
    }
    
}
