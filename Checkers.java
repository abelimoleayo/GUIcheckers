enum GameMode {
    SINGLE_PLAYER, MULTI_PLAYER;
}

public class Checkers {

    private static GameMode s_game_mode; // game mode
    private static Player[] s_players = new Player[2];
    private static int s_game_index = 0;
    private static int[] s_games_won = {0, 0};
    private static int s_curr_winner_index, s_game_board_size;

    // print intro message 
    private static void printIntro() {
        System.out.println("\nWelcome to the best Checkers game in the world\n");
    }

    // get desired game mode: single or multi player 
    private static void setGameMode() {
        String board_size_str, user_choice;

        board_size_str = UtilityFuncs.getUserChoice(new String[] {"8x8 board", 
                                                                  "10x10 board", 
                                                                  "12x12 board"},
                                                    new String[] {"8", "10", "12"});
        s_game_board_size = Integer.parseInt(board_size_str);

        user_choice = UtilityFuncs.getUserChoice(new String[] {"Singleplayer", "Multiplayer"}, 
                                                 new String[] {"S", "M"});
        // convert input to GameMode enum and return 
        if (user_choice == "S") {
            s_game_mode = GameMode.SINGLE_PLAYER;
        } else {
            s_game_mode = GameMode.MULTI_PLAYER;
        }
    }

    private static void createPlayers() {
        String user_input;
        switch (s_game_mode) {
            case SINGLE_PLAYER:
                System.out.print("Enter player name: ");
                user_input = UtilityFuncs.sc.nextLine();
                s_players[0] = (user_input.length() > 0) ? 
                                            new HumanPlayer(user_input, 1, false) : 
                                            new HumanPlayer("Player", 1, false);    
                s_players[1] = new AIPlayer("AI", 2, true);
                break;
            case MULTI_PLAYER: 
                for (int i=1; i<3; i++) {
                    System.out.print("Enter player " + i + " name: ");
                    user_input = UtilityFuncs.sc.nextLine();
                    s_players[i-1] = (user_input.length() > 0) ? 
                                        new HumanPlayer(user_input, i, (i%2)==0) : 
                                        new HumanPlayer("Player " + i, i, (i%2)==0);
                }
                break;
        }
    }

    private static void playOneGame() {
        s_game_index++;
        System.out.println("\nGame #" + s_game_index + " ongoing...\n");
        Game game = new Game(s_game_board_size, s_players);
        s_curr_winner_index = game.play();
    }

    // TO FIX
    private static void printGamePlayErrorMessage() {
        System.out.println("Something went wrong during gameplay :(");
    }

    private static void updateScoresAndPrintOutcome() {
        s_games_won[s_curr_winner_index] += 1;
        System.out.println(s_players[s_curr_winner_index].getName() + " won game #" + s_game_index);
        System.out.println("Current Score: " + s_players[0].getName() + " [" + s_games_won[0]
                                 + "] vs. [" + s_games_won[1] + "] " + s_players[1].getName());
    }

    private static boolean userWantsNewGame() {
        return UtilityFuncs.getUserChoice(new String[] {"New game", "Exit"}, 
                                          new String[] {"N", "E"}).equals("N");
    }

    private static void printOutro() {
        String outro = "\nGame over! ";
        if (s_games_won[0] == s_games_won[1]) {
            outro += "Game ends in a tie!";
        } else {
            outro += s_players[(s_games_won[0] > s_games_won[1]) ? 0 : 1].getName()  
                     + " won the most games!";
        }
        System.out.println(outro + "\nFinal score: " + s_players[0].getName() + " [" 
                           + s_games_won[0] + "] vs. [" + s_games_won[1] + "] " 
                           + s_players[1].getName());
    }

    public static void main(String[] args) {
        printIntro();
        setGameMode();
        createPlayers();

        while (true) {
            // IS THERE NEED TO REFRESH PLAYERS HERE?
            playOneGame();
            if (s_curr_winner_index == -1) {
                printGamePlayErrorMessage();
                printOutro();
                break;
            } else {
                updateScoresAndPrintOutcome();
                if (!userWantsNewGame()) {
                    printOutro();
                    break;
                }
            }
        }
    }
}