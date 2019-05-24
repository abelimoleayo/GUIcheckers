import java.util.*;
import java.awt.*;

public class HumanPlayer extends Player {

    private int m_selected_source;

    public HumanPlayer(String name, int board_label, boolean reflect, Color piece_color, 
                       Color crown_color) {
        super(name, board_label, reflect, piece_color, crown_color, Player.PlayerType.HUMAN);
    }
}
