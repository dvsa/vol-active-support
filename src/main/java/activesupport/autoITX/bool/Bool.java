package activesupport.autoITX.bool;

import activesupport.number.Int;

public class Bool{
    public static boolean random(){
        return (Int.random(0, 1) == 1) ? true : false;
    }
}