import java.util.*;
import java.awt.Graphics;
//import BoardCell.CellState;     

public class Game {
    private static enum GameState {
        WAITING_FOR_SOURCE, WAITING_FOR_DESTINATION, ANIMATING, GAME_OVER;
    }

    public static final String s_capture_delim = "=";
    public static final String s_moves_delim = ":";
    public static final String s_move_path_delim = "-";
    private Player[] m_players;
    private int m_board_size, m_pieces_per_player;
    private int[][] m_board;
    private Map<Integer,BoardCell> m_board_cells;
    private int m_curr_player_index;
    private GameState m_game_state;
    private Map<Integer,Set<String>> m_movable_pieces;
    private BoardCell m_invalid_cell;
    private Set<BoardCell> m_painted_cells;
    private Set<Integer> m_destinations;
    private Integer m_player_source, m_player_destination;
    private String m_selected_move;
    private Map<Integer,Set<Integer>> m_pos_on_path;

    // add boolean array of paramters as third argument
    public Game(int board_size, Player[] players)   {
        m_players = players;

        m_board_size = board_size;
        m_board = new int[m_board_size][m_board_size];
        
        m_pieces_per_player = (m_board_size/2) * (m_board_size - 2)/2;
        
        int p1_row, p1_col, p2_row, p2_col, player_POV_index, p2_global_index;
        int p1_label = m_players[0].m_int_label;
        int p2_label = m_players[1].m_int_label;
        int cell_width = Checkers.getBoardCellWidth();
        m_board_cells = new HashMap<Integer,BoardCell>();
        for (int i=0; i<(2*m_pieces_per_player); i=i+2) {
            p1_row = i/m_board_size;
            p1_col = (p1_row%2 == 0) ? (i%m_board_size) : (i%m_board_size) + 1;
            player_POV_index = p1_row*m_board_size + p1_col;
            m_board_cells.put(player_POV_index, new BoardCell(player_POV_index));
            m_board[p1_row][p1_col] = p1_label;
            m_players[0].addPiece(new Piece(player_POV_index, m_players[0]));

            p2_global_index = reflectPosition(player_POV_index, m_board_size);
            p2_row = p2_global_index/m_board_size;
            p2_col = p2_global_index%m_board_size;
            m_board_cells.put(p2_global_index, new BoardCell(p2_global_index));
            m_board[p2_row][p2_col] = p2_label;
            m_players[1].addPiece(new Piece(player_POV_index, m_players[1]));
        }

        m_painted_cells = new HashSet<BoardCell>();
        m_destinations = new HashSet<Integer>();
        m_pos_on_path = new HashMap<Integer,Set<Integer>>();

        // call function to highlight moveable pieces
        getMovablePieces(m_players[m_curr_player_index % 2]);
        m_game_state = GameState.WAITING_FOR_SOURCE;
    }

    // convert position in player POV to global position
    public static int reflectPosition(int pos, int board_size) {
        return (board_size*board_size) - pos - 1;
    }

    public static int reflectPosition(int pos) {
        return reflectPosition(pos, Checkers.getGameBoardSize());
    }

    private static int opponentLabel(Player player, Player[] players_array) {
        Integer opponent_label = null;
        for (Player p : players_array) {
            if (p.m_int_label != player.m_int_label) {
                opponent_label = p.m_int_label;
                break;
            }
        }
        return (int) opponent_label;
    }

    private List<Integer> computeCapturePos(int start, int end, Player player) {
        List<Integer> captures = new ArrayList<Integer>();
        int start_row = start/m_board_size;
        int start_col = start%m_board_size;
        int end_row = end/m_board_size;
        int end_col = end%m_board_size;
        int d_row = (end_row > start_row) ? 1 : -1; 
        int d_col = (end_col > start_col) ? 1 : -1;
        int curr_row = start_row;
        int curr_col = start_col;
        int curr_pos, curr_board_pos;

        while (curr_row != end_row) {
            curr_row += d_row;
            curr_col += d_col; 
            curr_board_pos = curr_pos = curr_row*m_board_size + curr_col;
            if (player.m_reflect_pos) curr_board_pos = reflectPosition(curr_pos, m_board_size);
            if (m_board[curr_board_pos/m_board_size]
                       [curr_board_pos%m_board_size] == opponentLabel(player, m_players)) {
                captures.add(curr_pos);
            }
        }
        return captures;
    }

    // private static void printBoard(int[][] board, int board_size) {
    //     //System.out.println(" ");
    //     for (int i=board_size-1; i>=0; i--) {
    //         for (int j=0; j<board_size; j++) {
    //             System.out.print(board[i][j] + "\t");
    //         }
    //         System.out.println(" ");
    //     }
    // }

    private Set<Integer> getJumpChildren(int pos, boolean isKing, Integer parent, 
                                         Set<Integer> ancestors, Set<Integer> victims, 
                                         Player player, int opponent_label) {
        // add AND USE variable for _can jump backwards_
        int row = pos/m_board_size;
        int col = pos%m_board_size;
        int toward_parent_row_diff = 0;
        int toward_parent_col_diff = 0;
        if (parent != null) {
            toward_parent_row_diff = (((int) parent/m_board_size) > row) ? 1 : -1;
            toward_parent_col_diff = (((int) parent%m_board_size) > col) ? 1 : -1;
        }
        int[] row_diffs = new int[]{1, -1};
        int[] col_diffs = new int[]{1, -1};
        int capture_row, capture_col, child_row, child_col;
        int capture_pos, capture_board_pos, child_pos, child_board_pos;
        boolean road_block, piece_captured;

        Set<Integer> children = new HashSet<Integer>();
        for (int r_diff : row_diffs) {
            for (int c_diff: col_diffs) {
                if (r_diff == toward_parent_row_diff && c_diff == toward_parent_col_diff) {
                    continue;
                }
                capture_row = row;
                capture_col = col;
                road_block = false;
                piece_captured = false; 
                do {
                    capture_row += r_diff;
                    capture_col += c_diff;
                    capture_board_pos = capture_pos = capture_row*m_board_size + capture_col;
                    if (player.m_reflect_pos) {
                        capture_board_pos = reflectPosition(capture_pos, m_board_size);
                    }
                    if (!piece_captured) {
                        if ((capture_row > 0) && (capture_row < (m_board_size - 1)) && 
                                    (capture_col > 0) && (capture_col < (m_board_size - 1)) &&
                                                                !victims.contains(capture_pos)) {
                            child_row = capture_row + r_diff;
                            child_col = capture_col + c_diff;
                            child_board_pos = child_pos = child_row*m_board_size + child_col;
                            if (player.m_reflect_pos) {
                                child_board_pos = reflectPosition(child_pos, m_board_size);
                            }
                            if ((m_board[capture_board_pos/m_board_size]
                                        [capture_board_pos%m_board_size] == opponent_label) && 
                                    ((m_board[child_board_pos/m_board_size]
                                             [child_board_pos%m_board_size] == 0) || 
                                      ancestors.contains(child_pos))) {
                                //System.out.println("cp-gpa: "+child_pos + " " + parent);
                                children.add(child_pos);
                                piece_captured = true;
                            } else if (m_board[capture_board_pos/m_board_size]
                                              [capture_board_pos%m_board_size] != 0) {
                                road_block = true;
                            }
                        } else {
                            road_block = true;
                        }
                    } else {
                        if ((capture_row >= 0) && (capture_row < m_board_size) && 
                                    (capture_col >= 0) && (capture_col < m_board_size)) {
                            if (m_board[capture_board_pos/m_board_size]
                                       [capture_board_pos%m_board_size] == 0) {
                                children.add(capture_pos);
                                continue;
                            }
                        }
                        road_block = true;                        
                    } 
                } while (isKing && !road_block);
            }
        }

        return children;
    }

    private String computePossibleJumpPaths(int pos, boolean isKing, Integer parent, 
                                            Set<Integer> ancestors, Set<Integer> victims, 
                                            Player player, int opponent_label) {
        //System.out.println("pos: " + pos);
        Set<Integer> jump_children = getJumpChildren(pos, isKing, parent, ancestors, victims, 
                                                     player, opponent_label);
        //System.out.println("Children: " + jump_children);
        if (jump_children.size() == 0) { // leaf
            if (parent == null) {
                return "";
            } else {
                return Integer.toString(pos);
            }
        } else {
            ancestors.add(parent);
            Map<Integer,String> children_paths = new HashMap<Integer,String>();
            Map<Integer,List<Integer>> children_captures = new HashMap<Integer,List<Integer>>();
            for (Integer child : jump_children) {
                Set<Integer> childs_vicitim_copy = new HashSet<Integer>(victims);
                List<Integer> captures = computeCapturePos(pos, (int)child, player);
                Collections.addAll(childs_vicitim_copy, captures.toArray(new Integer[0]));
                //System.out.println("chld-parn: "+child+ " " + pos);
                children_captures.put(child, captures);
                children_paths.put(child, computePossibleJumpPaths((int)child, isKing, 
                                                                  new Integer(pos), ancestors, 
                                                                  childs_vicitim_copy, player, 
                                                                  opponent_label));
            }
            Set<Integer> jump_children_with_captures = new HashSet<Integer>();
            for (Integer child : jump_children) {
                if (children_paths.get(child).contains(s_capture_delim)) {
                    jump_children_with_captures.add(child);
                }
            }
            if (jump_children_with_captures.size() > 0) {
                jump_children = jump_children_with_captures;
            }

            String paths = "";
            String[] child_paths_list;
            for (Integer child : jump_children) {
                child_paths_list = children_paths.get(child).split(s_moves_delim);
                for (String child_path : child_paths_list) {
                    paths += pos;
                    List<Integer> captures = children_captures.get(child);
                    for (int i=0; i<captures.size(); i++) {
                        paths += s_capture_delim + captures.get(i);
                    }
                    paths += s_move_path_delim + child_path + s_moves_delim;
                }
            }
            return paths.substring(0, paths.length()-1);
        }
    }

    private String computePossibleMovePaths(int pos, Player player) {
        // TODO: check 2 corners (4 if can move in any direction) 
        boolean isKing = player.pieceAtPos(pos).isKing();        
        boolean node_added;
        int row = pos/m_board_size;
        int col = pos%m_board_size;
        int[] row_diffs = isKing ? new int[]{1, -1} : new int[]{1};
        int[] col_diffs = {1, -1};  
        int new_row, new_col, dest_pos, dest_board_pos;

        String paths = "";
        String curr_path;
        // - intrapath, : interpath
        for (int r_diff : row_diffs) {
            for (int c_diff : col_diffs) {
                node_added = false;
                curr_path = "";
                new_row = row;
                new_col = col;
                do {
                    node_added = false;
                    new_row += r_diff;
                    new_col += c_diff;
                    if ((new_row >= 0) && (new_row < m_board_size) && (new_col >= 0) && 
                                                            (new_col < m_board_size)) {       
                        dest_board_pos = dest_pos = new_row*m_board_size + new_col;
                        if (player.m_reflect_pos) dest_board_pos = reflectPosition(dest_pos, 
                                                                                   m_board_size);
                        if (m_board[dest_board_pos/m_board_size]
                                   [dest_board_pos%m_board_size] == 0) {
                            curr_path += dest_pos;
                            paths += curr_path + s_moves_delim;
                            curr_path += s_move_path_delim;
                            node_added = true;
                        }
                    }
                } while (isKing && node_added);
            }
        }

        if (paths.isEmpty()) return paths;
        //System.out.println(paths);
        return paths.substring(0, paths.length()-1);
    }

    private void getMovablePieces(Player player) {
        Map<Integer,Set<String>> jump_positions = new HashMap<Integer,Set<String>>();
        Map<Integer,Set<String>> move_positions = new HashMap<Integer,Set<String>>();
        String jump_paths, move_paths;
        boolean jump_found = false;

        for (int pos : player.getPiecePositions()) {
            //System.out.println("Pos: " + pos);
            // check for jump, is so, set jumpFOund
            jump_paths = computePossibleJumpPaths(pos, player.pieceAtPos(pos).isKing(), null, 
                                                  new HashSet<Integer>(), new HashSet<Integer>(), 
                                                  player, opponentLabel(player, m_players));
            //System.out.println("Jump paths: " + jump_paths);
            if ((jump_paths != null) && (!jump_paths.isEmpty())) {
                jump_positions.put(pos, new HashSet<String>());
                for (String path : jump_paths.split(s_moves_delim)) {
                    jump_positions.get(pos).add(path);
                }
                jump_found = true;
            }
            // check for moves if no jump found yet
            if (!jump_found) {
                move_paths = computePossibleMovePaths(pos, player);
                if ((move_paths != null) && (!move_paths.isEmpty())) {
                    move_positions.put(pos, new HashSet<String>());
                    for (String path : move_paths.split(s_moves_delim)) {
                        move_positions.get(pos).add(path);
                    }
                }
            }
        }

        if (jump_found) {
            m_movable_pieces = jump_positions;
        } else {
            m_movable_pieces = move_positions;
        }

        if (m_movable_pieces.size() == 0) {
            clearPlayersPieces();
            // TODO: winner is update curr_player_index;
            m_game_state = GameState.GAME_OVER;
        }

        for (Integer pos : m_movable_pieces.keySet()) {
            int global_pos = player.m_reflect_pos ? reflectPosition(pos, m_board_size) : pos;
            m_board_cells.get(global_pos).setCellState(BoardCell.CellState.VALID_SOURCE, player);
        }
    } 

    // private String getPlayerMove(Player player, Map<Integer,Set<String>> possible_moves) {
    //     boolean source_selected = false;
    //     boolean move_complete = false;
    //     int player_choice;
    //     int player_source = 0;
    //     String chosen_path = "";

    //     // TODO: use possible_moves.keySet() to highlight background of all possible source
    //     while (!move_complete) {
    //         if (!source_selected) {
    //             System.out.print("Select source: ");
    //             player_choice = player.getMove(possible_moves, m_board, source_selected);
    //             if (possible_moves.containsKey(player_choice)) {
    //                 source_selected = true;
    //                 player_source = player_choice;
    //                 // TODO: highlight selected souce here
    //             } else {
    //                 System.out.println("Invalid source");
    //             }
    //         } else {
    //             System.out.print("Source selected, now select move: ");
    //             player_choice = player.getMove(possible_moves, m_board, source_selected);
    //             for (String path : possible_moves.get(player_source)) {
    //                 String[] split_path = path.split(s_move_path_delim);
    //                 if (player_choice == Integer.parseInt(split_path[split_path.length - 1])) {
    //                     chosen_path = path;
    //                     move_complete = true;
    //                     break;
    //                 }
    //             }
    //             if (!move_complete) {
    //                 if (possible_moves.containsKey(player_choice)) {
    //                     player_source = player_choice;
    //                     // TODO remove previous source highlight, highlight new selected souce here
    //                 } else {
    //                     System.out.println("Invalid move.");
    //                 }
    //             }
    //         }
    //     }

    //     return player_source + s_moves_delim + chosen_path;
    // }

    private void updateBoard(Player player, String move) {
        /* Thankful for this rule: https://www.itsyourturn.com/t_helptopic2130.html#helpitem1329*/
        boolean is_jump = move.contains(s_capture_delim);
        String[] move_sections = move.split(s_moves_delim);
        int source_pos = Integer.parseInt(move_sections[0]);
        String[] move_path = move_sections[1].split(s_move_path_delim);
        int final_pos = Integer.parseInt(move_path[move_path.length - 1]);

        // move player piece and update board
        Piece piece = player.pieceAtPos(source_pos);
        piece.setPos(final_pos);
        if (!piece.isKing() && (final_pos/m_board_size == m_board_size - 1)) {
            piece.makeKing();
        }
        if (player.m_reflect_pos) {
            source_pos = reflectPosition(source_pos, m_board_size);
            final_pos = reflectPosition(final_pos, m_board_size);
        }
        m_board[source_pos/m_board_size][source_pos%m_board_size] = 0;
        m_board[final_pos/m_board_size][final_pos%m_board_size] = player.m_int_label;

        // remove captured pieces and update board if move was a jump
        if (is_jump) {
            for (int i=0; i<move_path.length-1; i++) {
                String[] captures = move_path[i].split(s_capture_delim);
                for (int j=1; j<captures.length; j++) {
                    int capture_pos, capture_board_pos, capture_pos_opponent_POV;
                    capture_board_pos = capture_pos = Integer.parseInt(captures[j]);
                    capture_pos_opponent_POV = reflectPosition(capture_pos, m_board_size);
                    if (player.m_reflect_pos) { 
                        capture_board_pos = capture_pos_opponent_POV;
                    }
                    m_board[capture_board_pos/m_board_size][capture_board_pos%m_board_size] = 0;
                    for (Player p : m_players) {
                        if (p.m_int_label != player.m_int_label) {
                            p.removePiece(p.pieceAtPos(capture_pos_opponent_POV));
                        }
                    }
                }
            }
        }
    }

    private void clearPlayersPieces() {
        for (int i=0; i<m_players.length; i++) {
            m_players[i].clearPieces();
        }
    }

    public void draw(Graphics g) {
        for (BoardCell cell : m_board_cells.values()) {
            cell.draw(g);
        }
        for (Player p : m_players) {
            p.draw(g);
        }
    }

    private void waitingForSourceHandler(int pos, int global_pos, Player player) {
        BoardCell selected_cell = m_board_cells.get(global_pos);
        if (m_movable_pieces.keySet().contains(pos)) {
            // reset prior invalid selection if any
            if (m_invalid_cell != null) {
                m_invalid_cell.setCellState(BoardCell.CellState.DEFAULT, player);
                m_invalid_cell = null;
            }
            // reset all prior painted cells if any
            for (BoardCell cell : m_painted_cells) {
                cell.setCellState(BoardCell.CellState.DEFAULT, player);
                m_painted_cells.remove(cell);
            }
            // reset all valid sources in-case they were selected before
            for (Integer source_pos : m_movable_pieces.keySet()) {
                int global_source_pos = player.m_reflect_pos ? 
                                        reflectPosition(source_pos, m_board_size) : source_pos;
                m_board_cells.get(global_source_pos).setCellState(BoardCell.CellState.VALID_SOURCE,
                                                                  player);
            }
            // set state of selected cell
            selected_cell.setCellState(BoardCell.CellState.SELECTED_SOURCE, player);
            m_painted_cells.add(selected_cell);
            // get the moveable paths from source
            Set<String> moves = m_movable_pieces.get(pos);
            // set cell state of on_path cells, add them to m_painted cells
            boolean is_jump = moves.iterator().next().contains(s_capture_delim);
            if (is_jump) {
                for (String move : moves) {
                    String[] stops = move.split(s_capture_delim + "|" + s_move_path_delim);
                    int stop_pos = Integer.parseInt(stops[stops.length - 1]);
                    m_pos_on_path.put(stop_pos, new HashSet<Integer>());
                    m_pos_on_path.get(stop_pos).add(stop_pos);
                    for (int i=0; i<stops.length-2; i=i+2) {
                        int start = Integer.parseInt(stops[i]);
                        int end = Integer.parseInt(stops[i+2]);
                        if (i == stops.length-3) end = Integer.parseInt(stops[i+1]);
                        int start_row = start/m_board_size, start_col = start%m_board_size;
                        int end_row = end/m_board_size, end_col = end%m_board_size;
                        int d_row = (end_row > start_row) ? 1 : -1;
                        int d_col = (end_col > start_col) ? 1 : -1;
                        int row = start_row, col = start_col;
                        m_pos_on_path.get(stop_pos).add(start);
                        int curr_pos, curr_global_pos;
                        BoardCell curr_board_cell;
                        do {
                            row += d_row;
                            col += d_col;   
                            curr_pos = row*m_board_size + col;
                            m_pos_on_path.get(stop_pos).add(curr_pos);
                            curr_global_pos = player.m_reflect_pos ? 
                                                    reflectPosition(curr_pos, m_board_size) : curr_pos;
                            curr_board_cell = m_board_cells.get(curr_global_pos);
                            curr_board_cell.setCellState(BoardCell.CellState.ON_PATH, player);
                            m_painted_cells.add(curr_board_cell);
                        } while (row != end_row || col != end_col);
                    }
                }
            }
            // set cell state of possible landing cells
            for (String move : moves) {
                String[] stops = move.split(s_move_path_delim);
                int stop_pos = Integer.parseInt(stops[stops.length - 1]);
                int stop_global_pos = player.m_reflect_pos ? reflectPosition(stop_pos, m_board_size) : 
                                                             stop_pos;
                BoardCell stop_cell = m_board_cells.get(stop_global_pos);
                stop_cell.setCellState(BoardCell.CellState.DESTINATION, player);
                if ((stop_pos/m_board_size == m_board_size - 1) && (!player.pieceAtPos(pos).isKing())) {
                    stop_cell.setCellState(BoardCell.CellState.KING_DESTINATION, player);
                }
                m_painted_cells.add(stop_cell);
                m_destinations.add(stop_pos);
            }
            m_player_source = pos;
            m_game_state = GameState.WAITING_FOR_DESTINATION;
        } else {
            if (player.pieceAtPos(pos) != null) { // only highlight cell containing a piece
                if (m_invalid_cell != null) m_invalid_cell.setCellState(BoardCell.CellState.DEFAULT, player);
                m_invalid_cell = selected_cell;
                m_invalid_cell.setCellState(BoardCell.CellState.INVALID_SELECTION, player);
            }
        }  
    }

    private void waitingForDestinationHandler(int pos, int global_pos, Player player) {
        if (m_destinations.contains(pos)) {
            for (String path : m_movable_pieces.get(m_player_source)) {
                String[] split_path = path.split(s_move_path_delim);
                if (pos == Integer.parseInt(split_path[split_path.length - 1])) {
                    m_player_destination = pos;
                    // reset source cells that arent the current one
                    for (Integer source_pos : m_movable_pieces.keySet()) {
                        if (source_pos != m_player_source) {
                            int global_source_pos = player.m_reflect_pos ? 
                                                    reflectPosition(source_pos, m_board_size) : source_pos;
                            m_board_cells.get(global_source_pos).setCellState(BoardCell.CellState.DEFAULT,
                                                                              player);
                        }
                    }
                    // reset state
                    //global_source_pos = player.m_reflect_pos ? 
                    //                    reflectPosition(m_player_source, m_board_size) : m_player_source;
                    //m_board_cells.get(global_source_pos).setCellState(BoardCell.CellState.ON_ANIMATION_PATH);
                    
                    // reset all cells not on chosen path and set_state of chosen path cells
                    for (BoardCell cell : m_painted_cells) {
                        int cell_global_pos = cell.getGlobalPos();
                        int cell_pos = player.m_reflect_pos ? 
                                            reflectPosition(cell_global_pos, m_board_size) : cell_global_pos;
                        if (m_pos_on_path.get(pos).contains(cell_pos)) {
                            cell.setCellState(BoardCell.CellState.ON_ANIMATION_PATH, player);
                        } else {
                            cell.setCellState(BoardCell.CellState.DEFAULT, player);
                        }
                    }                    
                    //m_player_source = null; // NOPE
                    m_painted_cells = new HashSet<BoardCell>();
                    m_destinations = new HashSet<Integer>();
                    m_pos_on_path = new HashMap<Integer,Set<Integer>>();
                    m_selected_move = m_player_source + s_moves_delim + path;
                    // update board 
                    updateBoard(player, m_selected_move);
                    m_game_state = GameState.ANIMATING;
                    break;
                }
            }
        } else if (m_movable_pieces.keySet().contains(pos)) {
            for (BoardCell cell : m_painted_cells) {
                int cell_global_pos = cell.getGlobalPos();
                int cell_pos = player.m_reflect_pos ? reflectPosition(cell_global_pos, m_board_size) : 
                                                      cell_global_pos;
                if (!m_movable_pieces.keySet().contains(cell_pos)) {
                    cell.setCellState(BoardCell.CellState.DEFAULT, player);
                }
            }
            m_painted_cells = new HashSet<BoardCell>();
            m_destinations = new HashSet<Integer>();
            m_pos_on_path = new HashMap<Integer,Set<Integer>>();
            waitingForSourceHandler(pos, global_pos, player);
        } else {
            // what happens if input is wrong cell
            if (player.pieceAtPos(pos) != null && !m_painted_cells.contains(m_board_cells.get(pos))) {
                if (m_invalid_cell != null) m_invalid_cell.setCellState(BoardCell.CellState.DEFAULT, player);
                m_invalid_cell = m_board_cells.get(global_pos);
                m_invalid_cell.setCellState(BoardCell.CellState.INVALID_SELECTION, player);
                // Referesh, set state to waiting for source
                for (BoardCell cell : m_painted_cells) {
                    int cell_global_pos = cell.getGlobalPos();
                    int cell_pos = player.m_reflect_pos ? reflectPosition(cell_global_pos, m_board_size) : 
                                                          cell_global_pos;
                    if (!m_movable_pieces.keySet().contains(cell_pos)) {
                        cell.setCellState(BoardCell.CellState.DEFAULT, player);
                    }
                }
                m_painted_cells = new HashSet<BoardCell>();
                m_destinations = new HashSet<Integer>();
                m_pos_on_path = new HashMap<Integer,Set<Integer>>();
                m_game_state = GameState.WAITING_FOR_SOURCE;
            }
        }
    }

    public void processMousePress(int row, int col) {
        Player curr_player = m_players[m_curr_player_index % 2];
        int global_pos = m_board_size*row + col;
        int pos = curr_player.m_reflect_pos ? reflectPosition(global_pos, m_board_size) : global_pos;
        switch (m_game_state) {
            case WAITING_FOR_SOURCE:
                waitingForSourceHandler(pos, global_pos, curr_player);
                break;
            case WAITING_FOR_DESTINATION:
                waitingForDestinationHandler(pos, global_pos, curr_player);
                break;
            // case ANIMATING: // MOVE TO DRAW
            //     if (!m_players[m_curr_player_index % 2].animating()) {
            //         // update curr_player_index and call set m_movable_pieces 
            //         m_game_state = GameState.WAITING_FOR_SOURCE;
            //     }
            //     break;
        }
    }

    // public int play() {
    //     // return 0 if p1 won, 
    //     // return 1 if p2 won
    //     // return -1 if game terminated or other weird thing happens
        
    //     Player curr_player  = m_players[m_curr_player_index % 2];
    //     //Map<Integer,Set<String>> movable_pieces = getMovablePieces(curr_player);
    //     int idx = 0;
    //     if (m_movable_pieces.size() == 0) {
    //         // this player lost, next player is winner
    //         clearPlayersPieces();
    //         System.out.println("\n\n");
    //         printBoard(m_board, m_board_size);
    //         return idx%2;
    //     }
    //     String curr_player_move;
    //     while (true) {
    //         curr_player = m_players[idx++ % 2];
    //         //movable_pieces = getMovablePieces(curr_player);
    //         if (m_movable_pieces.size() == 0) {
    //             // this player lost, next player is winner
    //             clearPlayersPieces();
    //             System.out.println("\n\n");
    //             printBoard(m_board, m_board_size);
    //             return idx%2;
    //         }
    //         // System.out.println("Moveable pieces");
    //         // for (Integer i : movable_pieces.keySet()) {
    //         //     System.out.print("\t" + i + ": ");
    //         //     for (String s : movable_pieces.get(i)) {
    //         //         System.out.print(s + ", ");
    //         //     }
    //         //     System.out.println(" ");
    //         // }
    //         //curr_player_move = getPlayerMove(curr_player, m_movable_pieces);
    //         //updateBoard(curr_player, curr_player_move);
    //         //printBoard(m_board, m_board_size);
    //     }

    //     // compute pieces that can move (captures take priority) and highlight them
    //         // if none, player loses (return appropriate integer)
    //     // get user piece choice
    //     // highlight possible movements of user piece choice
    //     // get user destination choice
    //     // animate (if needed)
    //     // update m_board and player's piece position
    //     // use user's choice to update game (animation may or may not happen here)
    // }
}
