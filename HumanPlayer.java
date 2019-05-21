import java.util.*;
import java.awt.*;

public class HumanPlayer extends Player {

    private int m_selected_source;

    public HumanPlayer(String name, int board_label, boolean reflect, Color piece_color, 
                       Color crown_color) {
        super(name, board_label, reflect, piece_color, crown_color);
    }
    
    @Override
    public int getMove(Map<Integer,Set<String>> moveable_pieces_pos, int[][] game_board, 
                                                                  boolean source_selected) {
        // TEMP print moveable options here
        String[] options;
        if (!source_selected) {
            Set<Integer> keys = moveable_pieces_pos.keySet();
            options = new String[keys.size()];
            int idx = 0;
            for (Integer key : keys) {
                options[idx++] = key.toString();
            }
            m_selected_source = 0;//Integer.parseInt(UtilityFuncs.getUserChoice(options, options));
            return m_selected_source;
        } else {
            Set<String> path_options = moveable_pieces_pos.get(m_selected_source);
            options = new String[path_options.size()];
            String[] str_path;
            int idx = 0;
            for (String str : path_options) {
                str_path = str.split(Game.s_move_path_delim);
                options[idx++] = str_path[str_path.length - 1];
            }

            return 0;//Integer.parseInt(UtilityFuncs.getUserChoice(options, options));
        }

        

        // ACTUAL: use graphics to get desired position

    }
}
