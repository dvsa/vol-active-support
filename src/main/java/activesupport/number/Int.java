package activesupport.number;

import java.util.concurrent.ThreadLocalRandom;

public class Int {
    /**
     * Returns a number between 1 and the parameter max which is passed in as an argument.
     *
     * @param max an integer specifying the maximum value which is inclusive.
     * */
    public static int random(int max){
        return random(1, max);
    }

    /**
     * Returns a number between the min and max which are passed in as arguments.
     *
     * @param min an integer specifying the minimum value, which is inclusive.
     * @param max an integer specifying the maximum value, which is inclusive.
     * */
    public static int random(int min, int max){
        return max == 0 ? 0 : ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
