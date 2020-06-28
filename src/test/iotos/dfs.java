package test.iotos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.Graphs;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.graph.implementations.MultiNode;

public class dfs {


    int[][] adj;
    int time = 0;
    int[] parent;
    /**
     * color : 
     *  0 (white) : not found yet
     *  1 (gray) : found but not finish
     *  2 (black) : finish
     */
    int[] color;
    int[] discovery;
    int[] finish;
    int[] low;

    public List<Integer> findArticulationPoint(Graph topoGraph){

        adj = new int[topoGraph.getNodeCount()][topoGraph.getNodeCount()];
        for(int i=0 ; i<adj.length ; i++){
            for(int j=0 ; j<adj[i].length ; j++){
                adj[i][j] = 0;
            }
        }

        for(Node node : topoGraph.getEachNode()){
            for(Edge edge : node.getEachEdge()){
                adj[Integer.parseInt(node.getId())-1][Integer.parseInt(edge.getOpposite(node).getId())-1] = 1;
            }
        }

        time = 0;
        parent = new int[adj.length];
        /**
         * color : 
         *  0 (white) : not found yet
         *  1 (gray) : found but not finish
         *  2 (black) : finish
         */
        color = new int[adj.length];
        discovery = new int[adj.length];
        finish = new int[adj.length];
        low = new int[adj.length];
        
        for(int i=0 ; i<color.length ; i++){
            color[i] = 0;
            discovery[i] = -1;
            finish[i] = -1;
        }

        
        /**
         * for each node start DFS
         * 
         * Start at i=0 --> nodeID=1 (Controller)
         * sourceCount to count root node child, if count >= 2 means root is articulation point
         */
        int sourceCount = 0;
        for(int i=0 ; i<color.length ; i++){
            if(color[i] == 0){
                sourceCount++;
                DFSvisit(i);
            }
        }
        
        // System.out.println("======================");
        // for(int i=0 ; i<color.length ; i++){
        //     System.out.print(color[i] + " ");
        // }
        // System.out.println();
        // for(int i=0 ; i<color.length ; i++){
        //     System.out.print(discovery[i] + " ");
        // }
        // System.out.println();
        // for(int i=0 ; i<color.length ; i++){
        //     System.out.print(finish[i] + " ");
        // }
        // System.out.println();
        // for(int i=0 ; i<color.length ; i++){
        //     System.out.print(parent[i] + " ");
        // }
        // System.out.println();
        // for(int i=0 ; i<color.length ; i++){
        //     System.out.print(low[i] + " ");
        // }

        List<Integer> result = new ArrayList<>();

        // if root have 2 or more child, root is articulation point
        if(sourceCount>=2){
            result.add(0);
        }
        // if other vertex v has any child u, let low(u) >= discovery(v) , v is articulation point
        for(int i=1 ; i<adj.length ; i++){
            for(int j=0 ; j<adj[i].length ; j++){

                if(adj[i][j] == 1){

                    // if child
                    if(discovery[i] < discovery[j]){

                        // if low(u) >= d(v)
                        if(low[j] >= discovery[i]){
                            result.add(i);
                            break;
                        }
                    }

                }
            }
        }

        // System.out.println();
        // for(int i=0 ; i<result.size() ; i++){
        //     System.out.println(result.get(i) +" ");
        // }

        List<Integer> ans = new ArrayList<>();
        for(int i=0 ; i<result.size() ; i++){
            ans.add(result.get(i)+1);
        }
        return ans;
    }

    private void DFSvisit(int vertex){
        color[vertex] = 1;
        discovery[vertex] = time;
        time++;

        for(int i=0 ; i < adj[vertex].length ; i++){

            // have edge
            if(adj[vertex][i] == 1){
                // and not find yet
                if(color[i] == 0){
                    parent[i] = vertex;
                    DFSvisit(i);
                }
            }
        }

        int d = discovery[vertex];
        int min_low_of_child = Integer.MAX_VALUE;
        int min_d_backEdge = Integer.MAX_VALUE;

        for(int i=0 ; i < adj[vertex].length ; i++){
            // find child or back edge
            if(adj[vertex][i] == 1 && i != parent[vertex]){

                // back edge
                if(finish[i] == -1){
                    if(discovery[i] < min_d_backEdge){
                        min_d_backEdge = discovery[i];
                    }
                }else{
                    // child
                    if(low[i] < min_low_of_child){
                        min_low_of_child = low[i];
                    }
                }
            }
        }

        low[vertex] = Math.min(Math.min(d, min_d_backEdge), min_low_of_child);
        color[vertex] = 2;
        finish[vertex] = time;
        time++;
    }


    /**
     * test data
     * 
     */

    // public static void main(String[] args){
    //     Graph topo = new MultiGraph("topology");
    //     topo.addNode("1");
    //     topo.addNode("2");
    //     topo.addNode("3");
    //     topo.addNode("4");
    //     topo.addNode("5");
    //     topo.addNode("6");
    //     topo.addNode("7");
    //     topo.addNode("8");
    //     topo.addNode("9");
    //     topo.addNode("10");
    //     topo.addEdge("12", "1", "2");
    //     topo.addEdge("23", "2", "3");
    //     topo.addEdge("24", "2", "4");
    //     topo.addEdge("35", "3", "5");
    //     topo.addEdge("45", "4", "5");
    //     topo.addEdge("46", "4", "6");

    //     topo.addEdge("67", "6", "7");
    //     topo.addEdge("68", "6", "8");
    //     topo.addEdge("78", "7", "8");
    //     topo.addEdge("89", "8", "9");
    //     topo.addEdge("810", "8", "10");

    //     List<Integer> ap = findArticulationPoint(topo);

    //     for(int i=0 ; i<ap.size() ; i++){
    //         System.out.println(ap.get(i));
    //     }
        
    // }
}