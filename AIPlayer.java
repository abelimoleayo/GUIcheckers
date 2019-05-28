import java.util.*;
import java.awt.*;

public class AIPlayer extends Player {

    private int m_selected_move;

    // TODO: add field to store difficulty level

    public AIPlayer(String name, int board_label, boolean reflect, Color piece_color, 
                    Color crown_color) {
        super(name, board_label, reflect, piece_color, crown_color, Player.PlayerType.AI);
    }

    public AIPlayer(int board_label, boolean reflect, Color piece_color, Color crown_color) {
        super("Default_AI_name", board_label, reflect, piece_color, crown_color, Player.PlayerType.AI);
    }

    public int getMove(Map<Integer,Set<String>> moveable_pieces_pos, int[][] game_board, 
                                                                 boolean source_selected) {
        int choice = 0;
        // TEMP: pick randomly
        if (!source_selected) {
            int rand_index = UtilityFuncs.r.nextInt(moveable_pieces_pos.keySet().size());
            int idx = 0;
            for (Integer key : moveable_pieces_pos.keySet()) {
                if (idx++ == rand_index) {
                    choice = key;
                    int rand_index_2 = UtilityFuncs.r.nextInt(moveable_pieces_pos.get(key).size());
                    int idx2 = 0;
                    for (String str : moveable_pieces_pos.get(key)) {
                        if (idx2++ == rand_index_2) {
                            String[] move_path = str.split(Game.s_move_path_delim);
                            m_selected_move = Integer.parseInt(move_path[move_path.length - 1]);
                            break;
                        }
                    }
                    break;
                }
            }
        } else {
            choice = this.m_selected_move; 
        }

        return choice;


        // ACTUAL: AI logic (use difficulty level field)
    }
}
