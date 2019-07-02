import java.util.*;
import java.awt.Graphics;

public class Game {
    public static enum GameState {
        WAITING_FOR_SOURCE, WAITING_FOR_DESTINATION, ANIMATING, ON_HOLD, GAME_OVER;
    }

    // static variables
    public static final String s_capture_delim = "=";
    public static final String s_moves_delim = ":";
    public static final String s_move_path_delim = "-";
    private static final int m_max_kings_moves_without_capture = 10;

    // instance variables
    private Player[] m_players; 
    private int m_board_size, m_pieces_per_player, m_curr_player_index, m_winner_index, m_player_source, 
                m_player_destination, m_kings_moves_without_capture;
    private int[][] m_board;
    private boolean m_capture_occured;
    private String m_selected_move;
    private GameState m_game_state;
    private BoardCell m_invalid_cell;
    private Set<BoardCell> m_painted_cells, m_animation_cells;
    private Set<Integer> m_destinations;
    private Map<Integer,BoardCell> m_board_cells;
    private Map<Integer,Set<String>> m_movable_pieces;
    private Map<Integer,Set<Integer>> m_pos_on_jump_path, m_pos_on_move_path;

    // constructor
    public Game(int board_size, Player[] players)   {
        m_players = players;
        clearPlayersPieces();   // clear player pieces from previous game if any

        // create array representation of game board and create player pieces
        m_board_size = board_size;
        m_board = new int[m_board_size][m_board_size];        
        m_pieces_per_player = (m_board_size/2) * (m_board_size - 2)/2;   
        int p1_row, p1_col, p2_row, p2_col, player_POV_index, p2_global_index;
        int p1_label = m_players[0].m_int_label;
        int p2_label = m_players[1].m_int_label;
        for (int i=0; i<(2*m_pieces_per_player); i=i+2) {
            p1_row = i/m_board_size;
            p1_col = (p1_row%2 == 0) ? (i%m_board_size) : (i%m_board_size) + 1;
            player_POV_index = p1_row*m_board_size + p1_col;
            m_board[p1_row][p1_col] = p1_label;
            m_players[0].addPiece(new Piece(player_POV_index, m_players[0]));

            p2_global_index = reflectPosition(player_POV_index, m_board_size);
            p2_row = p2_global_index/m_board_size;
            p2_col = p2_global_index%m_board_size;
            m_board[p2_row][p2_col] = p2_label;
            m_players[1].addPiece(new Piece(player_POV_index, m_players[1]));
        }

        // create board cell objects
        m_board_cells = new HashMap<Integer,BoardCell>();
        for (int i=0; i<(m_board_size*m_board_size); i=i+2) {
            int row = i/m_board_size;
            int col = (row%2 == 0) ? (i%m_board_size) : (i%m_board_size) + 1;
            int index = row*m_board_size + col;
            m_board_cells.put(index, new BoardCell(index));
        }

        // initialize some helper sets/maps
        m_painted_cells = new HashSet<BoardCell>();
        m_animation_cells = new HashSet<BoardCell>();
        m_destinations = new HashSet<Integer>();
        m_pos_on_jump_path = new HashMap<Integer,Set<Integer>>();
        m_pos_on_move_path = new HashMap<Integer,Set<Integer>>();

        // identify moveable pieces for first player
        getMovablePieces(m_players[m_curr_player_index % 2]);
    }

    // return game state
    public GameState getGameState() {
        return m_game_state;
    }

    // get index of winning player
    public int getWinnerIndex() {
        return m_winner_index;
    }

    // convert position in player POV to global position
    public static int reflectPosition(int pos, int board_size) {
        return (board_size*board_size) - pos - 1;
    }

    // convert position in player POV to global position
    public static int reflectPosition(int pos) {
        return reflectPosition(pos, Checkers.getGameBoardSize());
    }

    // get opponent piece label in internal representation of game board
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

    // given the start and stop index of a jump, compute the index of the captured piece
    private static int computeCapturePos(int start, int end, int[][] board, int board_size, 
                                         boolean reflect_pos, int player_label, int opponent_label) {
        int start_row = start/board_size;
        int start_col = start%board_size;
        int end_row = end/board_size;
        int end_col = end%board_size;
        int d_row = (end_row > start_row) ? 1 : -1; 
        int d_col = (end_col > start_col) ? 1 : -1;
        int curr_row = start_row;
        int curr_col = start_col;
        int curr_pos = 0, curr_board_pos;
        while (curr_row != end_row) {
            curr_row += d_row;
            curr_col += d_col; 
            curr_board_pos = curr_pos = curr_row*board_size + curr_col;
            if (reflect_pos) curr_board_pos = reflectPosition(curr_pos, board_size);
            if (board[curr_board_pos/board_size][curr_board_pos%board_size] == opponent_label) {
                break;
            }
        }
        return curr_pos;
    }

    // identify set of landing positions one jump away from a given position
    private static Set<Integer> getJumpChildren(int pos, boolean isKing, Integer parent, 
                                                Set<Integer> ancestors, Set<Integer> past_captures, 
                                                int[][] board, int board_size, boolean reflect_pos, 
                                                int opponent_label) {
        int row = pos/board_size;
        int col = pos%board_size;
        int toward_parent_row_diff = 0;
        int toward_parent_col_diff = 0;
        if (parent != null) {
            toward_parent_row_diff = (((int) parent/board_size) > row) ? 1 : -1;
            toward_parent_col_diff = (((int) parent%board_size) > col) ? 1 : -1;
        }
        int[] row_diffs = new int[]{1, -1};
        int[] col_diffs = new int[]{1, -1};
        int capture_row, capture_col, child_row, child_col;
        int capture_pos, capture_board_pos, child_pos, child_board_pos;
        boolean road_block, piece_captured;

        Set<Integer> children = new HashSet<Integer>();
        for (int r_diff : row_diffs) {
            for (int c_diff: col_diffs) {
                // ignore if current capture direction is towards parent
                if (r_diff == toward_parent_row_diff && c_diff == toward_parent_col_diff) {
                    continue;
                }
                capture_row = row;
                capture_col = col;
                road_block = false;
                piece_captured = false; 
                do {  // for regular pieces, loop once; for kings, loop iuntil road_block
                    capture_row += r_diff;
                    capture_col += c_diff;
                    capture_pos = capture_row*board_size + capture_col;
                    capture_board_pos = reflect_pos ? reflectPosition(capture_pos, board_size) : capture_pos;
                    if (!piece_captured) { // no piece captured, check if capturable piece exists here
                        // check if capturable piece position is within bounds
                        if ((capture_row > 0) && (capture_row < (board_size - 1)) && 
                                    (capture_col > 0) && (capture_col < (board_size - 1)) &&
                                                                !past_captures.contains(capture_pos)) {
                            child_row = capture_row + r_diff;
                            child_col = capture_col + c_diff;
                            child_pos = child_row*board_size + child_col;
                            child_board_pos = reflect_pos ? reflectPosition(child_pos, board_size) 
                                                          : child_pos;
                            // check if landing cell just after capture piece is free
                            if (((board[capture_board_pos/board_size]
                                        [capture_board_pos%board_size] == opponent_label) || 
                                 (board[capture_board_pos/board_size]
                                        [capture_board_pos%board_size] == opponent_label*opponent_label)) && 
                                    ((board[child_board_pos/board_size]
                                             [child_board_pos%board_size] == 0) || 
                                      ancestors.contains(child_pos))) {
                                children.add(child_pos);
                                piece_captured = true;
                            } else if (board[capture_board_pos/board_size]
                                              [capture_board_pos%board_size] != 0) {
                                road_block = true;
                            }
                        } else {
                            road_block = true;
                        }
                    } else { // piece already captured, add every free cell in direction to children list
                        if ((capture_row >= 0) && (capture_row < board_size) && 
                                    (capture_col >= 0) && (capture_col < board_size)) {
                            if (board[capture_board_pos/board_size]
                                       [capture_board_pos%board_size] == 0) {
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

    // (for possible king jumps) in each jump direction, if at least one child with children exists, 
    // remove all childless children in that direction
    private static Set<Integer> removeBarrenChildrenInEachDirection(int board_size, 
                                                                    Set<Integer> all_children,
                                                                    Map<Integer,String> children_paths,
                                                                    Integer parent) {
        Set<Integer> valid_children = new HashSet<Integer>();

        int parent_row = parent/board_size;
        int parent_col = parent%board_size;
        int[] delta_row = {-1, 1};
        int[] delta_col = {-1, 1};
        for (int d_row : delta_row) {
            for (int d_col : delta_col) {
                // identify all children in current direction
                Set<Integer> children_in_direction = new HashSet<Integer>();
                for (Integer child : all_children) {
                    int child_row = child/board_size;
                    int child_col = child%board_size;
                    int child_parent_d_row = (child_row > parent_row) ? 1 : -1;
                    int child_parent_d_col = (child_col > parent_col) ? 1 : -1;
                    if ((d_row == child_parent_d_row) && (d_col == child_parent_d_col)) {
                        children_in_direction.add(child);
                    }
                }
                // identify which children have children/captures
                Set<Integer> children_in_direction_with_captures = new HashSet<Integer>();
                for (Integer child : children_in_direction) {
                    if (children_paths.get(child).contains(s_capture_delim)) {
                        children_in_direction_with_captures.add(child);
                    }
                }
                // keep only children with captures if at least one exists
                if (children_in_direction_with_captures.size() > 0) {
                    children_in_direction = children_in_direction_with_captures;
                }
                valid_children.addAll(children_in_direction);
            }
        }

        return valid_children;
    }

    // compute possible jumps from a given position for a given player
    private static String computePossibleJumpPaths(int pos, boolean is_king, Integer parent, 
                                                   Set<Integer> ancestors, Set<Integer> past_captures, 
                                                   boolean reflect_pos, int[][] board, int board_size,
                                                   int player_label, int opponent_label) {
        // set of landing positions one jump away from current position
        Set<Integer> jump_children = getJumpChildren(pos, is_king, parent, ancestors, past_captures, board,
                                                     board_size, reflect_pos, opponent_label);
        // base-case (no children)
        if (jump_children.size() == 0) {
            if (parent == null) {
                return "";
            } else {
                return Integer.toString(pos);
            }
        } else {
            // recursively identify children's jump paths
            ancestors.add(parent);
            Map<Integer,String> children_paths = new HashMap<Integer,String>();
            Map<Integer,Integer> children_captures = new HashMap<Integer,Integer>();
            for (Integer child : jump_children) {
                Set<Integer> childs_copy_of_past_captures = new HashSet<Integer>(past_captures);
                int capture = computeCapturePos(pos, (int)child, board, board_size, reflect_pos, 
                                                player_label, opponent_label);
                childs_copy_of_past_captures.add(capture);
                children_captures.put(child, capture);
                children_paths.put(child, computePossibleJumpPaths((int)child, is_king, new Integer(pos), 
                                                                   ancestors, childs_copy_of_past_captures, 
                                                                   reflect_pos, board, board_size, 
                                                                   player_label, opponent_label));
            }
            // remove bogus children in each direction
            if (is_king) {
                jump_children = removeBarrenChildrenInEachDirection(board_size, jump_children, 
                                                                    children_paths, pos);
            }
            // combine children's jump paths into one delimited string and return
            String paths = "";
            String[] child_paths_list;
            for (Integer child : jump_children) {
                child_paths_list = children_paths.get(child).split(s_moves_delim);
                for (String child_path : child_paths_list) {
                    paths += pos;
                    int capture = children_captures.get(child);
                    paths += s_capture_delim + capture;
                    paths += s_move_path_delim + child_path + s_moves_delim;
                }
            }
            return paths.substring(0, paths.length()-1);
        }
    }

    // identify all possible non-jump moves from a given position for a given player
    private static String computePossibleMovePaths(int pos, boolean is_king, int[][] board, 
                                                   int board_size, boolean reflect_pos) {
        boolean node_added;
        int row = pos/board_size;
        int col = pos%board_size;
        int[] row_diffs = is_king ? new int[]{1, -1} : new int[]{1};
        int[] col_diffs = {1, -1};  
        int new_row, new_col, dest_pos, dest_board_pos;

        String paths = "";
        String curr_path;
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
                    if ((new_row >= 0) && (new_row < board_size) && (new_col >= 0) && 
                                                            (new_col < board_size)) {       
                        dest_pos = new_row*board_size + new_col;
                        dest_board_pos = reflect_pos ? reflectPosition(dest_pos, board_size) : dest_pos;
                        if (board[dest_board_pos/board_size]
                                   [dest_board_pos%board_size] == 0) {
                            curr_path += dest_pos;
                            paths += curr_path + s_moves_delim;
                            curr_path += s_move_path_delim;
                            node_added = true;
                        }
                    }
                } while (is_king && node_added);
            }
        }

        if (paths.isEmpty()) return paths;
        return paths.substring(0, paths.length()-1);
    }

    // identify moveable (jump or no jump) pieces for a given player
    private void getMovablePieces(Player player) {
        int opponent_label = opponentLabel(player, m_players);
        m_movable_pieces = getMovablePieces(player, player.m_int_label, opponent_label, m_board, 
                                            m_board_size, player.m_reflect_pos, player.getPiecePositions());

        if (m_movable_pieces.size() == 0) {  // current player can't jump or move: game over
            m_winner_index = (m_curr_player_index + 1) % 2;
            m_game_state = GameState.GAME_OVER;
        } else { // current player can jump or move
            if (player.m_type == Player.PlayerType.AI) { // AI player: call handler to process move
                AIMoveHandler(player, opponent_label);
            } else { // human player: highlight moveable cells and set game state
                for (Integer pos : m_movable_pieces.keySet()) {
                    int global_pos = player.m_reflect_pos ? reflectPosition(pos, m_board_size) : pos;
                    m_board_cells.get(global_pos).setCellState(BoardCell.CellState.VALID_SOURCE, player);
                }
                m_game_state = GameState.WAITING_FOR_SOURCE;
            }
        }
    } 

    public static Map<Integer,Set<String>> getMovablePieces(Player player, int player_label, 
                                                            int opponent_label, int[][] board, 
                                                            int board_size, boolean reflect_pos, 
                                                            Set<Integer> piece_positions) {
        Map<Integer,Set<String>> jump_positions = new HashMap<Integer,Set<String>>();
        Map<Integer,Set<String>> move_positions = new HashMap<Integer,Set<String>>();
        String jump_paths, move_paths;
        boolean jump_found = false; 

        if (piece_positions == null) {
            piece_positions = new HashSet<Integer>();
            for (int i=0; i<board_size; i++) {
                for (int j=0; j<board_size; j++) {
                    if ((board[i][j] == player_label) || 
                        (board[i][j] == player_label * player_label)) {
                        int pos = i*board_size + j;
                        pos = reflect_pos ? reflectPosition(pos, board_size) : pos;
                        piece_positions.add(pos);
                    }
                }
            }
        }

        for (int pos : piece_positions) {
            boolean is_king;
            if (player != null) {
                is_king = player.pieceAtPos(pos).isKing();
            } else {
                int board_pos = reflect_pos ? reflectPosition(pos, board_size) : pos;
                is_king = (board[board_pos/board_size][board_pos%board_size] == player_label*player_label);
            }

            // check for possible jump from current possition, is found, set jump_found
            jump_paths = computePossibleJumpPaths(pos, is_king, null, new HashSet<Integer>(), 
                                                  new HashSet<Integer>(), reflect_pos, board, board_size, 
                                                  player_label, opponent_label);
            if ((jump_paths != null) && (!jump_paths.isEmpty())) {
                jump_positions.put(pos, new HashSet<String>());
                for (String path : jump_paths.split(s_moves_delim)) {
                    jump_positions.get(pos).add(path);
                }
                jump_found = true;
            }
            // if no possible jump found so far, check for possible move from current position
            if (!jump_found) {
                move_paths = computePossibleMovePaths(pos, is_king, board, board_size, reflect_pos);
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

    // handle move of AI player
    private void AIMoveHandler(Player player, int opponent_label) {
        int[] move = ((AIPlayer) player).getMove(m_movable_pieces, m_board, m_board_size, opponent_label);
        for (String path : m_movable_pieces.get(move[0])) {
            String[] split_path = path.split(s_move_path_delim);
            if (move[1] == Integer.parseInt(split_path[split_path.length - 1])) {
                updateBoard(player, move[0] + s_moves_delim + path);
                m_game_state = GameState.ANIMATING;
                break;
            }
        }
    }

    // update game board after a player's move
    private void updateBoard(Player player, String move) {
        m_capture_occured = move.contains(s_capture_delim);
        String[] move_sections = move.split(s_moves_delim);
        int source_pos = Integer.parseInt(move_sections[0]);
        String[] move_path = move_sections[1].split(s_move_path_delim);
        int final_pos = Integer.parseInt(move_path[move_path.length - 1]);

        // cache opponent
        Player opponent = null;
        for (Player p : m_players) {
            if (p.m_int_label != player.m_int_label) {
                opponent = p;
                break;
            }
        }

        // remove captured pieces and update board if move was a jump
        List<Integer> stops = new ArrayList<Integer>();
        if (m_capture_occured) {
            int capture_pos, capture_board_pos, capture_pos_opponent_POV;
            for (int i=0; i<move_path.length-1; i++) {
                String[] captor_capture = move_path[i].split(s_capture_delim);
                if (i>0) stops.add(Integer.parseInt(captor_capture[0]));
                capture_board_pos = capture_pos = Integer.parseInt(captor_capture[1]);
                capture_pos_opponent_POV = reflectPosition(capture_pos, m_board_size);
                if (player.m_reflect_pos) { 
                    capture_board_pos = capture_pos_opponent_POV;
                }
                m_board[capture_board_pos/m_board_size][capture_board_pos%m_board_size] = 0;
                opponent.removePiece(opponent.pieceAtPos(capture_pos_opponent_POV), i);
            }
        }

        // move player piece and update board
        Piece piece = player.pieceAtPos(source_pos);
        stops.add(final_pos);
        piece.setPos(stops);
        if (!piece.isKing() && (final_pos/m_board_size == m_board_size - 1)) {
            piece.makeKing();
        }
        if (player.m_reflect_pos) {
            source_pos = reflectPosition(source_pos, m_board_size);
            final_pos = reflectPosition(final_pos, m_board_size);
        }
        m_board[source_pos/m_board_size][source_pos%m_board_size] = 0;
        m_board[final_pos/m_board_size][final_pos%m_board_size] = player.m_int_label;
    }

    // clear all of a players' pieces (for new game)
    private void clearPlayersPieces() {
        for (int i=0; i<m_players.length; i++) {
            m_players[i].clearPieces();
        }
    }

    // draw the game (board cells and players' pieces)
    public void draw(Graphics g, boolean has_focus) {
        // check if animation is done
        if (m_game_state == GameState.ANIMATING) {
            animationDoneHandler();
        } 
        // draw all board cells
        for (BoardCell cell : m_board_cells.values()) {
            cell.draw(g);
        }
        // draw both players (draw all their pieces)
        m_players[(m_curr_player_index + 1)%2].draw(g, has_focus);
        m_players[m_curr_player_index%2].draw(g, has_focus);
    }

    // process player's choice for the piece to move (the source)
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
                    m_pos_on_jump_path.put(stop_pos, new HashSet<Integer>());
                    for (int i=0; i<stops.length-2; i=i+2) {
                        int start = Integer.parseInt(stops[i]);
                        int end = Integer.parseInt(stops[i+2]);
                        if (i == stops.length-3) end = Integer.parseInt(stops[i+1]);
                        int start_row = start/m_board_size, start_col = start%m_board_size;
                        int end_row = end/m_board_size, end_col = end%m_board_size;
                        int d_row = (end_row > start_row) ? 1 : -1;
                        int d_col = (end_col > start_col) ? 1 : -1;
                        int row = start_row, col = start_col;
                        m_pos_on_jump_path.get(stop_pos).add(start);
                        int curr_pos, curr_global_pos;
                        BoardCell curr_board_cell;
                        do {
                            row += d_row;
                            col += d_col;   
                            curr_pos = row*m_board_size + col;
                            m_pos_on_jump_path.get(stop_pos).add(curr_pos);
                            curr_global_pos = player.m_reflect_pos ? 
                                                    reflectPosition(curr_pos, m_board_size) : curr_pos;
                            curr_board_cell = m_board_cells.get(curr_global_pos);
                            curr_board_cell.setCellState(BoardCell.CellState.ON_JUMP_PATH, player);
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

                m_pos_on_move_path.put(stop_pos, new HashSet<Integer>());
                int start_pos = is_jump ? Integer.parseInt(stops[stops.length - 2].split(s_capture_delim)[1])
                                        : pos;
                if (!is_jump) m_pos_on_move_path.get(stop_pos).add(start_pos);
                int start_row = start_pos/m_board_size, start_col = start_pos%m_board_size;
                int end_row = stop_pos/m_board_size, end_col = stop_pos%m_board_size;
                int d_row = (end_row > start_row) ? 1 : -1;
                int d_col = (end_col > start_col) ? 1 : -1;
                int row = start_row, col = start_col;
                do {
                    row += d_row;
                    col += d_col;
                    int curr_pos = row*m_board_size + col;
                    m_pos_on_move_path.get(stop_pos).add(curr_pos);
                } while (row != end_row || col != end_col);
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

    // refresh instance variables used in processing player's choice for destination of selected piece
    private void refreshDestinationHandlerInstanceVariables() {
        m_painted_cells = new HashSet<BoardCell>();
        m_destinations = new HashSet<Integer>();
        m_pos_on_jump_path = new HashMap<Integer,Set<Integer>>();
        m_pos_on_move_path = new HashMap<Integer,Set<Integer>>();
    }

    // process player's choice for the destination of currently selected piece 
    private void waitingForDestinationHandler(int pos, int global_pos, Player player) {
        if (m_destinations.contains(pos)) { // if player's choice is a valid destination
            // figure out the path from piece location to the selected destination
            for (String path : m_movable_pieces.get(m_player_source)) {
                boolean is_jump = path.contains(s_capture_delim);
                String[] split_path = path.split(s_move_path_delim);
                if (pos == Integer.parseInt(split_path[split_path.length - 1])) {
                    m_player_destination = pos;
                    // reset state of valid source cells that arent the current one
                    for (Integer source_pos : m_movable_pieces.keySet()) {
                        if (source_pos != m_player_source) {
                            int global_source_pos = player.m_reflect_pos ? 
                                                    reflectPosition(source_pos, m_board_size) : source_pos;
                            m_board_cells.get(global_source_pos).setCellState(BoardCell.CellState.DEFAULT,
                                                                              player);
                        }
                    }
                    // reset all cells not on chosen path and set the state of chosen path cells
                    for (BoardCell cell : m_painted_cells) {
                        int cell_global_pos = cell.getGlobalPos();
                        int cell_pos = player.m_reflect_pos ? 
                                            reflectPosition(cell_global_pos, m_board_size) : cell_global_pos;
                        cell.setCellState(BoardCell.CellState.DEFAULT, player);
                        if (m_pos_on_move_path.get(pos).contains(cell_pos)) {
                            cell.setCellState(BoardCell.CellState.ON_ANIMATION_PATH, player);
                            m_animation_cells.add(cell);
                        }
                        if (is_jump) {
                            if (m_pos_on_jump_path.get(pos).contains(cell_pos)) { 
                                cell.setCellState(BoardCell.CellState.ON_ANIMATION_PATH, player);
                                m_animation_cells.add(cell);
                            } 
                        } 
                    }                    
                    refreshDestinationHandlerInstanceVariables();
                    m_selected_move = m_player_source + s_moves_delim + path;
                    // update board 
                    updateBoard(player, m_selected_move);
                    m_game_state = GameState.ANIMATING;
                    break;
                }
            }
        } else if (m_movable_pieces.keySet().contains(pos)) { // if player's choice is another movable piece
            for (BoardCell cell : m_painted_cells) {
                int cell_global_pos = cell.getGlobalPos();
                int cell_pos = player.m_reflect_pos ? reflectPosition(cell_global_pos, m_board_size) : 
                                                      cell_global_pos;
                if (!m_movable_pieces.keySet().contains(cell_pos)) {
                    cell.setCellState(BoardCell.CellState.DEFAULT, player);
                }
            }
            refreshDestinationHandlerInstanceVariables();
            waitingForSourceHandler(pos, global_pos, player);
        } else { // if player's choice is invalid
            if (player.pieceAtPos(pos) != null && !m_painted_cells.contains(m_board_cells.get(pos))) {
                if (m_invalid_cell != null) m_invalid_cell.setCellState(BoardCell.CellState.DEFAULT, player);
                m_invalid_cell = m_board_cells.get(global_pos);
                m_invalid_cell.setCellState(BoardCell.CellState.INVALID_SELECTION, player);

                int global_source_pos = player.m_reflect_pos ? reflectPosition(m_player_source, m_board_size)
                                                             : m_player_source;
                m_board_cells.get(global_source_pos).setCellState(BoardCell.CellState.VALID_SOURCE, player);

                // Refresh, set game state to waiting for source
                for (BoardCell cell : m_painted_cells) {
                    int cell_global_pos = cell.getGlobalPos();
                    int cell_pos = player.m_reflect_pos ? reflectPosition(cell_global_pos, m_board_size) : 
                                                          cell_global_pos;
                    if (!m_movable_pieces.keySet().contains(cell_pos)) {
                        cell.setCellState(BoardCell.CellState.DEFAULT, player);
                    }
                }
                refreshDestinationHandlerInstanceVariables();
                m_game_state = GameState.WAITING_FOR_SOURCE;
            }
        }
    }

    // check if animation is done, reset state of board cells on the completed animation path, update
    // current player index and call function to highlight movable pieces of next player
    private void animationDoneHandler() {
        if (!m_players[0].isAnimating() && !m_players[1].isAnimating()) { 
            for (BoardCell cell : m_animation_cells) {
                cell.setCellState(BoardCell.CellState.DEFAULT, m_players[m_curr_player_index % 2]);
            }
            m_animation_cells = new HashSet<BoardCell>();
            checkForKingDraw();
            m_curr_player_index++;
            getMovablePieces(m_players[m_curr_player_index % 2]);
        }
    }

    // process mouse click. only consider clicks if player selection is being expected
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
        }
    }

    private void checkForKingDraw() {
        if (m_capture_occured) {
            m_kings_moves_without_capture = 0;
            return;
        }
        if (m_players[0].hasKing() && m_players[1].hasKing()) {
            m_kings_moves_without_capture++;
            if (m_kings_moves_without_capture >= m_max_kings_moves_without_capture) {
                GameState old_game_state = m_game_state;
                m_game_state = GameState.ON_HOLD;
                Checkers.promptForDraw();
                m_game_state = old_game_state;
                m_kings_moves_without_capture = 0;  // reset count after prompt for draw
            }
        }
    }
}
