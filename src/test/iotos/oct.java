package test.iotos;

import java.util.ArrayList;
import java.util.List;

public class oct {
    public static void main(String[] args){

        List<Integer> choose = octIntegers(3,8);


        for(int i=0 ; i<choose.size() ; i++){
            System.out.println(choose.get(i));
        }



    }

    private static List<Integer> octIntegers(int totalLength, int number){
        String o = Integer.toOctalString(number);

        List<Integer> select = new ArrayList<>();

        for(int i=0 ; i<totalLength-o.length() ; i++){
            select.add(0);
        }
        for(int i=0 ; i<o.length() ; i++){
            select.add(Integer.parseInt(Character.toString(o.charAt(i))));
        }

        return select;
    }
}
