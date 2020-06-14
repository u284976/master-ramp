package test.iotos;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.ApplicationRequirements;
import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.pathDescriptors.PathDescriptor;

public class iperfTest {
    /**
     * run on the node, which link is single hop to the "10.0.0.1" Network Interface Card
     */
    public static void main(String args[]) {
        // String cmd = "iperf -c 10.0.0.1";
        // try {
        //     Process p = Runtime.getRuntime().exec(cmd);
        //     InputStream inputStream = p.getInputStream();
        //     BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        //     String line;
        //     while((line = reader.readLine())!= null){
        //             System.out.println(line);
        //     }
        // } catch (Exception e) {
        //     //TODO: handle exception
        // }
        
        Map<Integer,PathDescriptor> test = new HashMap<>();
        String[] pathString = new String[2];
        pathString[0] = "10.0.0.1";
        pathString[1] = "10.0.0.2";
        List<Integer> pathNodeID = new ArrayList<>();
        pathNodeID.add(1);
        pathNodeID.add(2);
        PathDescriptor path = new PathDescriptor(pathString, pathNodeID);
        
        test.put(1, path);

        Map<Integer,PathDescriptor> test1 = new HashMap<>(test);
        
        iperfTest c = new iperfTest();
        c.change(test1);
        
        for(int i=0 ; i<2 ; i++){
            System.out.println(test.get(1).getPath()[i]);
        }
        
    }
    private void change(Map<Integer,PathDescriptor> flowPaths){
        String[] path = new String[2];
        path[0] = "10.0.0.11";
        path[1] = "10.0.0.22";
        flowPaths.get(1).setPath(path);
    }
}