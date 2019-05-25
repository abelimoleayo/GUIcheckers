import java.util.*;
import java.awt.*;        

public class BoardCell {

    // possible cell states defining color of board cell
    public static enum CellState {
        DEFAULT, VALID_SOURCE, INVALID_SELECTION, SELECTED_SOURCE, 
        ON_PATH, ON_ANIMATION_PATH, DESTINATION, KING_DESTINATION;
    }

    private static Map<CellState,Color> s_color_map = new HashMap<CellState,Color>() {
        {
            put(CellState.DEFAULT, Color.LIGHT_GRAY);
            put(CellState.VALID_SOURCE, Color.ORANGE);
            put(CellState.INVALID_SELECTION, Color.RED);
            put(CellState.SELECTED_SOURCE, Color.GREEN);
            put(CellState.ON_PATH, Color.BLUE);
            put(CellState.ON_ANIMATION_PATH, Color.GREEN);
            put(CellState.DESTINATION, Color.GREEN);
            put(CellState.KING_DESTINATION, Color.GREEN);
        }
    };
    private final int[] m_coords;
    private final int m_global_pos;
    private final int m_cell_width;
    private CellState m_cell_state;
    private Player m_state_setter;

    public BoardCell(int global_pos) {
        m_global_pos = global_pos;
        m_coords = Checkers.getTopLeftCoordFromPos(m_global_pos);
        m_cell_width = Checkers.getBoardCellWidth();
        m_cell_state = CellState.DEFAULT;
    }

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
        g.setColor(s_color_map.get(m_cell_state));
        g.fillRect(m_coords[0], m_coords[1], m_cell_width, m_cell_width);
        if (m_cell_state == CellState.KING_DESTINATION) {
            UtilityFuncs.drawKingIcon(g, m_coords[0]+m_cell_width/2, m_coords[1]+m_cell_width/2,
                                      Checkers.getBoardPieceWidth(), m_state_setter.m_crown_color);
        }
    }
    
}
