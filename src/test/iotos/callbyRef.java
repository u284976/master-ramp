package test.iotos;

import java.util.ArrayList;
import java.util.List;

import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.pathDescriptors.PathDescriptor;

public class callbyRef {




    public static void main(String[] args){



        List<String> p = new ArrayList<>();

        p.add("123");

        PathDescriptor path = new PathDescriptor(p.toArray(new String[0]));

        for(String s : path.getPath()){
            System.out.println(s);
        }

        p = new ArrayList<>();

        p.add("456");

        for(String s : path.getPath()){
            System.out.println(s);
        }


        /**
         * output :
         * 123
         * 123
         */
    }
}