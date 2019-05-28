import java.awt.*;        
import java.awt.event.*;
import javax.swing.*;

public class Checkers extends JPanel {

    private static enum GameMode {
        SINGLE_PLAYER, MULTI_PLAYER;
    }
    private static GameMode s_game_mode; 
    private static Player[] s_players = new Player[2];
    private static int s_game_index, s_curr_winner_index, s_board_cell_width, s_board_piece_width, 
                       s_game_board_size;
    private static int[] s_games_won = {0, 0};
    private static boolean s_game_mode_selected, s_waiting;
    private static Game s_game;
    private static Timer s_timer;
    private static JFrame s_window;
    private static final Color s_background_color = new Color(214,179,134);
    private static final Color s_player1_color = new Color(252,204,145);
    private static final Color s_player2_color = Color.BLACK;
    private static final Color s_player1_crown_color = Color.CYAN;
    private static final Color s_player2_crown_color = Color.YELLOW;
    private static final int s_window_size = 720;
    public static final int s_animation_steps = 15;    

    private Checkers() {
        ActionListener action = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                repaint();
            }
        };
        s_timer = new Timer( 30, action );

        addMouseListener( new MouseAdapter() {
            public void mousePressed(MouseEvent evt) {
                processMousePress(evt);
            }
        } );

        addFocusListener( new FocusListener() {
            public void focusGained(FocusEvent evt) {
                s_timer.start();
                repaint();
            }
            public void focusLost(FocusEvent evt) {
                s_timer.stop();
                repaint();
            }
        } );
    }

    private void processMousePress(MouseEvent evt) {
        if (s_game_mode_selected) {
            int row = evt.getY()/s_board_cell_width;
            int col = evt.getX()/s_board_cell_width;
            if (row >= s_game_board_size) row = s_game_board_size - 1; //bottom edge of window
            if (col >= s_game_board_size) col = s_game_board_size - 1; //right edge of window
            if (row % 2 == col % 2) return; // user clicks white grid
            row = s_game_board_size - row - 1;
            s_game.processMousePress(row, col);
        }
    }

    public void paintComponent(Graphics g) {
        if (s_waiting) {
            g.setColor(s_background_color);
            g.fillRect(0, 0, s_window_size, s_window_size);
            if (s_game != null) s_game.draw(g);
            return;
        }
        if (s_game_mode_selected) {
            if (s_game.getGameState() == Game.GameState.GAME_OVER) {
                updateScoresAndPrintOutcome();
            }
            g.setColor(s_background_color);
            g.fillRect(0, 0, s_window_size, s_window_size);
            s_game.draw(g);
        } else {
            setGameModeAndCreatePlayers();
            s_game = new Game(s_game_board_size, s_players);
            s_game_index++;
            s_timer.start();
        }
    }

    public static int getBoardCellWidth() {
        return s_board_cell_width;
    }

    public static int getBoardPieceWidth() {
        return s_board_piece_width;
    }

    public static int getGameBoardSize() {
        return s_game_board_size;
    }

    public static int[] getTopLeftCoordFromPos(int global_pos) {
        int row = global_pos/s_game_board_size;
        int col = global_pos%s_game_board_size;        
        int[] coords = {col*s_board_cell_width, (s_game_board_size - row - 1)*s_board_cell_width};
        return coords;
    }

    private static void setGameModeAndCreatePlayers() {
        s_waiting = true;
        String[] choices = {"8x8 American", "10x10 International", "12x12 Canadian"};
        String user_choice = (String) JOptionPane.showInputDialog(s_window, "Select board size",
                                                                  "Game Settings", 
                                                                  JOptionPane.QUESTION_MESSAGE, 
                                                                  null, choices, choices[0]); 
        switch (user_choice) {
            case "8x8 American": 
                s_game_board_size = 8;
                break;
            case "10x10 International": 
                s_game_board_size = 10;
                break;
            case "12x12 Canadian": 
                s_game_board_size = 12;
                break;
        }
        s_board_cell_width = s_window_size/s_game_board_size;
        s_board_piece_width = (int) (0.8*s_board_cell_width);
        choices = new String[] {"Single player", "Multi player"};
        user_choice = (String) JOptionPane.showInputDialog(s_window, "Select game mode",
                                                           "Game Settings", 
                                                           JOptionPane.QUESTION_MESSAGE, null, 
                                                           choices, choices[0]);
        switch (user_choice.substring(0,1)) {
            case "S": 
                s_game_mode = GameMode.SINGLE_PLAYER;
                break;
            case "M": 
                s_game_mode = GameMode.MULTI_PLAYER;
                break;
        }
        createPlayers();
        s_game_mode_selected = true;
        s_waiting = false;
    }

    private static void createPlayers() {
        switch (s_game_mode) {
            case SINGLE_PLAYER:
                JTextField player_name_field = new JTextField();
                Object[] message = {"Enter player name", player_name_field};
                int option = JOptionPane.showConfirmDialog(s_window, message, "Enter player name", 
                                                           JOptionPane.OK_CANCEL_OPTION);
                String player_name = (player_name_field.getText().length() == 0) ? "Human" :
                                                                                player_name_field.getText();
                if (option == JOptionPane.OK_OPTION) {
                    s_players[0] = new HumanPlayer(player_name, 1, false, s_player1_color, 
                                                   s_player1_crown_color);
                    s_players[1] = new AIPlayer("AI", 2, true, s_player2_color, s_player2_crown_color);
                }               
                break;
            case MULTI_PLAYER: 
                JTextField player1_name_field = new JTextField();
                JTextField player2_name_field = new JTextField();
                Object[] message1 = {"Enter player 1 name", player1_name_field, 
                                     "Enter player 2 name", player2_name_field};
                int option1 = JOptionPane.showConfirmDialog(s_window, message1, "Enter player name", 
                                                           JOptionPane.OK_CANCEL_OPTION);
                String player1_name = (player1_name_field.getText().length() == 0) ? "Player 1" :
                                                                            player1_name_field.getText();
                String player2_name = (player2_name_field.getText().length() == 0) ? "Player 2" :
                                                                                player2_name_field.getText();
                if (option1 == JOptionPane.OK_OPTION) {
                    s_players[0] = new HumanPlayer(player1_name, 1, false, s_player1_color, 
                                                   s_player1_crown_color);
                    s_players[1] = new HumanPlayer(player2_name, 2, true, s_player2_color, 
                                                   s_player2_crown_color);
                }               
                break;
        }
    }

    private static void printOutro() {
        String outro = "Game over!\n\n";
        if (s_games_won[0] == s_games_won[1]) {
            outro += "Game ends in a tie!";
        } else {
            outro += s_players[(s_games_won[0] > s_games_won[1]) ? 0 : 1].getName()  
                     + " won the most games!\n\n";
        }
        outro += "Final score: " + s_players[0].getName() + " [" 
                                 + s_games_won[0] + "] vs. [" + s_games_won[1] + "] " 
                                 + s_players[1].getName();
        JOptionPane.showMessageDialog(s_window, outro, "Game Over", JOptionPane.INFORMATION_MESSAGE);
        s_window.dispatchEvent(new WindowEvent(s_window, WindowEvent.WINDOW_CLOSING));
    }

    private static void updateScoresAndPrintOutcome() {
        s_waiting = true;
        s_curr_winner_index = s_game.getWinnerIndex();
        s_games_won[s_curr_winner_index] += 1;
        String message = s_players[s_curr_winner_index].getName() + " won game #" + s_game_index + "\n\n";
        message += "Current Score: " + s_players[0].getName() + " [" + s_games_won[0]
                                 + "] vs. [" + s_games_won[1] + "] " + s_players[1].getName() + "\n\n";
        message += "Play new game?";
        Object[] options = {"Yes", "No"};
        int choice = JOptionPane.showOptionDialog(s_window, message, "New Game?", JOptionPane.YES_NO_OPTION,
                                                JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
        if (choice == JOptionPane.YES_OPTION) {
            s_game = new Game(s_game_board_size, s_players);
            s_game_index++;
            s_waiting = false;
        } else {
            printOutro();
        }
    }

    public static void main(String[] args) {
        s_window = new JFrame("GUI Checkers");
        s_window.setContentPane(new Checkers());
        s_window.getContentPane().setPreferredSize(new Dimension(s_window_size, s_window_size));
        s_window.pack();
        s_window.setLocation(100,100);
        s_window.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        s_window.setResizable(false);
        s_window.setVisible(true);
    }
}