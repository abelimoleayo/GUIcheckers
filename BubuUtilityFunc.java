import java.util.*;

public class BubuUtilityFunc {

    public static String getUserChoice(String[] options, String[] abbrvs) {
        /*
            Write a program to prompt a user to pick from a set of options.
            ===============================================================
            Input description:
            ------------------
                - options: An array containing the names of all the options.
                -  abbrvs: An array containing the expected input corresponding to each option.
                Both arrays ('options' and 'abbrvs') will have the same length

            Example:  options = {"Apple", "Banana", "Cat"},    abbrvs = {"A", "B", "C"}
            --------                
                Given the example inputs above, your program should print the following prompt:

                            Apple [A] or Banana [B] or Cat [C]: 

                and should return "A" if the user enters "A" or "a";
                and should return "B" if the user enters "B" or "b";
                and should return "C" if the user enters "C" or "c";

                If the user enters anything besides the allowed inputs, your program should 
                print an error message and re-prompt for a choice as follows:

                            Please select A B or C
                            Apple [A] or Banana [B] or Cat [C]: 

                This should continue until the user enters a valid input.

            Note:
            -----
                - The example above has 3 options to choose from, but your program should 
                  be able to handle any number of options.
                - When promping, put a space after the colon.
                - To get the length of an array arr1, do:
                                    arr1.length      (Note that there's no () after .length)
                - To compare two strings str1 and str2 without worrying about upper or lower
                  cases, do:
                                    str1.equalsIgnoreCase(str2)
                - To remove leading and trailing whitespaces from a string str1, do:
                                    str1.trim()
                    E.g.     if str1 = "   bubu "
                           then str1.trim() = "bubu"
                - To get a substring of length N from a string str1 starting from index i, do:
                                    str1.substring(i,N)
                    E.g.     if str1 = "Bulouere"
                           then str1.substring(0,4) = "Bulo"
                                str1.substring(1,3) = "ulo"
                                str1.substring(3,5) = "ouere"
                    Note that the index starts at 0 not 1, so in "Bulouere", "B" is in index 0
        */

        return "";     // REMOVE "" and replace with your appropriate return value

    }


    public static void main(String[] args) {
        /*
            TESTS: DO NOT MODIFY. MODIFY THE FUNCTION ABOVE INSTEAD    
        */
        String[] test_options = new String[] {"Imoleayo", "Loves", "Bulouere", "So much"};
        String[] test_abbrvs = new String[] {"I", "L", "B", "SM"};

        try {
            String user_choice = getUserChoice(test_options, test_abbrvs);
            if (Arrays.asList(test_abbrvs).contains(user_choice)) {
                System.out.println("Works");
            } else {
                System.out.println(":( Fails");
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.out.println("There's a bug in your code Bubu!!!");
        }
    }

}
