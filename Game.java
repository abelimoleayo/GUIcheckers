import java.util.*;

public class Game {

    public static final String s_capture_delim = "=";
    public static final String s_moves_delim = ":";
    public static final String s_move_path_delim = "-";
    private Player[] m_players;
    private int m_board_size, m_pieces_per_player;
    private int[][] m_board;

    // add boolean array of paramters as third argument
    public Game(int board_size, Player[] players)   {
        m_players = players;

        m_board_size = board_size;
        m_board = new int[m_board_size][m_board_size];
        
        m_pieces_per_player = (m_board_size/2) * (m_board_size - 2)/2;
        
        int p1_row, p1_col, player_POV_index, p2_global_index;
        int p1_label = m_players[0].m_int_label;
        int p2_label = m_players[1].m_int_label;
        for (int i=0; i<(2*m_pieces_per_player); i=i+2) {
            p1_row = i/m_board_size;
            p1_col = (p1_row%2 == 0) ? (i%m_board_size) : (i%m_board_size) + 1;
            player_POV_index = p1_row*m_board_size + p1_col;
            
            m_board[p1_row][p1_col] = p1_label;
            m_players[0].addPiece(new Piece(player_POV_index));

            p2_global_index = reflectPosition(player_POV_index, m_board_size);
            m_board[p2_global_index/m_board_size][p2_global_index%m_board_size] = p2_label;
            m_players[1].addPiece(new Piece(player_POV_index));
        }

        // int p1_label = m_players[0].m_int_label;
        // int p2_label = m_players[1].m_int_label;
        // Piece p = new Piece(9); p.makeKing();
        // m_board[1][1] = p1_label; m_players[0].addPiece(p);
        // m_board[1][3] = p2_label; m_players[1].addPiece(new Piece(reflectPosition(11,8)));
        // m_board[1][5] = p2_label; m_players[1].addPiece(new Piece(reflectPosition(13,8)));
        // m_board[4][4] = p2_label; m_players[1].addPiece(new Piece(reflectPosition(36,8)));
        // m_board[4][6] = p2_label; m_players[1].addPiece(new Piece(reflectPosition(38,8)));


        printBoard(m_board, m_board_size);
    }

    // convert position in player POV to global position
    private static int reflectPosition(int pos, int board_size) {
        return (board_size*board_size) - pos - 1;
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

    private static void printBoard(int[][] board, int board_size) {
        //System.out.println(" ");
        for (int i=board_size-1; i>=0; i--) {
            for (int j=0; j<board_size; j++) {
                System.out.print(board[i][j] + "\t");
            }
            System.out.println(" ");
        }
    }

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

    private Map<Integer,Set<String>> getMovablePieces(Player player) {
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
            return jump_positions;
        } else {
            return move_positions;
        }
    } 

    private String getPlayerMove(Player player, Map<Integer,Set<String>> possible_moves) {
        boolean source_selected = false;
        boolean move_complete = false;
        int player_choice;
        int player_source = 0;
        String chosen_path = "";

        // TODO: use possible_moves.keySet() to highlight background of all possible source
        while (!move_complete) {
            if (!source_selected) {
                System.out.print("Select source: ");
                player_choice = player.getMove(possible_moves, m_board, source_selected);
                if (possible_moves.containsKey(player_choice)) {
                    source_selected = true;
                    player_source = player_choice;
                    // TODO: highlight selected souce here
                } else {
                    System.out.println("Invalid source");
                }
            } else {
                System.out.print("Source selected, now select move: ");
                player_choice = player.getMove(possible_moves, m_board, source_selected);
                for (String path : possible_moves.get(player_source)) {
                    String[] split_path = path.split(s_move_path_delim);
                    if (player_choice == Integer.parseInt(split_path[split_path.length - 1])) {
                        chosen_path = path;
                        move_complete = true;
                        break;
                    }
                }
                if (!move_complete) {
                    if (possible_moves.containsKey(player_choice)) {
                        player_source = player_choice;
                        // TODO: remove previous source highlight, highlight new selected souce here
                    } else {
                        System.out.println("Invalid move.");
                    }
                }
            }
        }

        return player_source + s_moves_delim + chosen_path;
    }

    private void updateBoard(Player player, String move) {
        /* Thankful for this rule: https://www.itsyourturn.com/t_helptopic2130.html#helpitem1329 */
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

    public int play() {
        // return 0 if p1 won, 
        // return 1 if p2 won
        // return -1 if game terminated or other weird thing happens
        
        int idx = 0;
        Player curr_player;
        Map<Integer,Set<String>> movable_pieces;
        String curr_player_move;
        while (true) {
            curr_player = m_players[idx++ % 2];
            movable_pieces = getMovablePieces(curr_player);
            if (movable_pieces.size() == 0) {
                // this player lost, next player is winner
                clearPlayersPieces();
                System.out.println("\n\n");
                printBoard(m_board, m_board_size);
                return idx%2;
            }
            System.out.println("Moveable pieces");
            for (Integer i : movable_pieces.keySet()) {
                System.out.print("\t" + i + ": ");
                for (String s : movable_pieces.get(i)) {
                    System.out.print(s + ", ");
                }
                System.out.println(" ");
            }
            // TODO: highlight all possible moveable spots

            curr_player_move = getPlayerMove(curr_player, movable_pieces);
            updateBoard(curr_player, curr_player_move);
            printBoard(m_board, m_board_size);

            //break;
            // if (idx == 4) {
            //     Random r = new Random();
            //     return UtilityFuncs.r.nextInt(3) - 1;
            // }
        }

        // compute pieces that can move (captures take priority) and highlight them
            // if none, player loses (return appropriate integer)
        // get user piece choice
        // highlight possible movements of user piece choice
        // get user destination choice
        // animate (if needed)
        // update m_board and player's piece position
        // use user's choice to update game (animation may or may not happen here)
    }
}
