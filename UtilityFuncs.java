import java.util.Scanner;
import java.util.Random;

public class UtilityFuncs {
    
    public static Random r = new Random();
    public static Scanner sc = new Scanner(System.in);

    public static String getUserChoice(String[] options, String[] abbrvs) {
        String user_choice;   
        String prompt = "";
        String wrong_input_mssg = "Please select ";

        // build strings for prompt and for error message
        for (int i=0; i<options.length; i++) {
            prompt += options[i] + " [" + abbrvs[i] + "] or ";
            wrong_input_mssg += abbrvs[i] + " or ";
        }
        prompt = prompt.substring(0, prompt.length()-4) + ": ";
        wrong_input_mssg = wrong_input_mssg.substring(0, wrong_input_mssg.length()-4) + ".";

        // loop until user provides valid input
        while (true) {  
            System.out.print(prompt);
            user_choice = sc.nextLine();
            for (String abbrv : abbrvs) {
                if (user_choice.trim().equalsIgnoreCase(abbrv)) {
                    return abbrv;
                }
            }         
            System.out.println(wrong_input_mssg);           
        }
    }
}
