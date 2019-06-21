import java.util.*;
import java.awt.*;

public class AIPlayer extends Player {

    private final int m_minimax_depth = 3;

    // TODO: add class variable to store difficulty level

    // constructor
    public AIPlayer(String name, int board_label, boolean reflect, Color piece_color, 
                    Color crown_color) {
        super(name, board_label, reflect, piece_color, crown_color, Player.PlayerType.AI);
    }

    // constructor for when name isn't provided
    public AIPlayer(int board_label, boolean reflect, Color piece_color, Color crown_color) {
        super("Default_AI_name", board_label, reflect, piece_color, crown_color, Player.PlayerType.AI);
    }

    // get AI player's move from collection of possible moves
    public int[] getMove(Map<Integer,Set<String>> moveable_pieces_pos, int[][] game_board, int board_size,
                         int opponent_label) {

        // ACTUAL: AI logic (use difficulty level field)
        int best_move_value = Integer.MIN_VALUE;
        int[] best_move = new int[2];
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        for (Integer pos : moveable_pieces_pos.keySet()) {
            for (String move : moveable_pieces_pos.get(pos)) {    
                int[][] next_game_board = getBoardAfterMove(deepCopy(game_board, board_size), board_size,
                                                            pos, move, this.m_reflect_pos, this.m_int_label);
                int move_value = miniMax(next_game_board, board_size, m_minimax_depth, alpha, beta, false, 
                                         opponent_label, this.m_int_label, !this.m_reflect_pos);
                if (move_value > best_move_value) {
                    best_move_value = move_value;
                    String[] move_stops = move.split(Game.s_move_path_delim);
                    best_move = new int[]{pos, Integer.parseInt(move_stops[move_stops.length - 1])};
                }
                if (move_value > alpha) {
                    alpha = move_value;
                }
            }
        }

        return best_move;

    }

    private int[][] getBoardAfterMove(int[][] board, int board_size, int source_pos, String move, 
                                      boolean reflect_pos, int player_label) {
        boolean is_jump = move.contains(Game.s_capture_delim);
        String[] move_path = move.split(Game.s_move_path_delim);
        int final_pos = Integer.parseInt(move_path[move_path.length - 1]);

        if (is_jump) {
            for (int i=0; i<move_path.length-1; i++) {
                int capture_pos = Integer.parseInt(move_path[i].split(Game.s_capture_delim)[1]);
                if (reflect_pos) capture_pos = Game.reflectPosition(capture_pos, board_size); 
                board[capture_pos/board_size][capture_pos%board_size] = 0;
            }
        }

        int new_label = player_label;
        if (final_pos/board_size == board_size - 1) {
            new_label = player_label*player_label;
        }

        if (reflect_pos) {
            source_pos = Game.reflectPosition(source_pos, board_size);
            final_pos = Game.reflectPosition(final_pos, board_size);
        }

        board[source_pos/board_size][source_pos%board_size] = 0;
        board[final_pos/board_size][final_pos%board_size] = new_label;

        return board;
    }

    private int[][] deepCopy(int[][] board, int board_size) {
        int[][] new_board = new int[board_size][];
        for (int i=0; i<board_size; i++) {
            int[] sub_array = new int[board_size];
            System.arraycopy(board[i], 0, sub_array, 0, board_size);
            new_board[i] = sub_array;
        }
        return new_board;
    }

    private int miniMax(int[][] board, int board_size, int depth, int alpha, int beta, boolean maximizing,
                        int player_label, int opponent_label, boolean reflect_pos) {
        if (depth == 0) {
            return evalBoard(board, board_size, player_label, opponent_label, reflect_pos);
        }
        Map<Integer,Set<String>> moveable_pieces = Game.getMovablePieces(null, this.m_int_label, 
                                                                         opponent_label, board, board_size, 
                                                                         this.m_reflect_pos, null);
        if (moveable_pieces.size() == 0) {
            return evalBoard(board, board_size, player_label, opponent_label, reflect_pos);
        }

        if (maximizing) {
            int max_move_value = Integer.MIN_VALUE;
            for (Integer pos : moveable_pieces.keySet()) {
                for (String move : moveable_pieces.get(pos)) {
                    int[][] next_game_board = getBoardAfterMove(deepCopy(board, board_size), board_size, 
                                                                pos, move, reflect_pos, player_label);
                    int move_value = miniMax(next_game_board, board_size, depth-1, alpha, beta, !maximizing, 
                                             opponent_label, player_label, !reflect_pos);
                    if (move_value > max_move_value) {
                        max_move_value = move_value;
                    }
                    if (move_value > alpha) {
                        alpha = move_value;
                    }
                    if (beta <= alpha) {
                        break;
                    }
                }
            }
            return max_move_value;
        } else {
            int min_move_value = Integer.MAX_VALUE;
            for (Integer pos : moveable_pieces.keySet()) {
                for (String move : moveable_pieces.get(pos)) {
                    int[][] next_game_board = getBoardAfterMove(deepCopy(board, board_size), board_size, 
                                                                pos, move, reflect_pos, player_label);
                    int move_value = miniMax(next_game_board, board_size, depth-1, alpha, beta, !maximizing, 
                                             opponent_label, player_label, !reflect_pos);
                    if (move_value < min_move_value) {
                        min_move_value = move_value;
                    }
                    if (move_value < beta) {
                        beta = move_value;
                    }
                    if (beta <= alpha) {
                        break;
                    }
                }
            }
            return min_move_value;
        }
    }

    private int evalBoard(int[][] game_board, int board_size, int player_label, int opponent_label, 
                          boolean reflect_pos) {
        int value = 0;
        int backward_piece_value = 5;
        int forward_piece_value = 7;
        int king_value = 10;

        for (int row=0; row<board_size; row++) {
            for (int col=0; col<board_size; col++) {
                if ((row + col) % 2 == 0) {
                    if (game_board[row][col] == player_label*player_label) {
                        value += king_value;
                    } else if (game_board[row][col] == opponent_label*opponent_label) {
                        value -= king_value;
                    } else if (game_board[row][col] == player_label) {
                        if ((reflect_pos && (row < board_size/2)) ||
                            (!reflect_pos && (row > board_size/2))) {
                            value += forward_piece_value;
                        } else {
                            value += backward_piece_value;
                        }
                    } else if (game_board[row][col] == opponent_label) {
                        if ((reflect_pos && (row < board_size/2)) ||
                            (!reflect_pos && (row > board_size/2))) {
                            value -= backward_piece_value;
                        } else {
                            value -= forward_piece_value;
                        }
                    }
                }
            }
        }

        // // print-board
        // for (int i=board_size-1; i>=0; i--) {
        //     for (int j=0; j<board_size; j++) {
        //         System.out.print(game_board[i][j] + "\t");
        //     }
        //     System.out.println(" ");
        // }
        // System.out.println(player_label + ": " + value);

        return value;
    }
}
