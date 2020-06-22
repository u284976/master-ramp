package test.iotos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.List;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.Graphs;
import org.graphstream.graph.implementations.MultiNode;

import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.pathDescriptors.PathDescriptor;

public class bfs {
    private Graph topologyGraph;


    public bfs(Graph topologyGraph){
        this.topologyGraph = Graphs.clone(topologyGraph);
    }

    public PathDescriptor selectPath(int sourceNodeId, int destNodeId){
        
        // System.out.println("========BFS========");
        // System.out.println("find " + sourceNodeId + " to " + destNodeId);
        // System.out.println("========BFS========");

        boolean found = false;
        List<MultiNode> queue = new ArrayList<>();
        List<MultiNode> randomQueue = new ArrayList<>();
        Map<String,Boolean> visit = new HashMap<>();
        Map<String,String> parent = new HashMap<>();

        Random rand = new Random();

        queue.add(topologyGraph.getNode(Integer.toString(sourceNodeId)));
        
        MultiNode node = queue.get(0);
        visit.put(node.getId(), true);
        for(Edge e : node.getEdgeSet()){
            MultiNode neighborNode = e.getOpposite(node);

            int r = rand.nextInt(2);
            // origin block
            if(r == 0){

                if(visit.get(neighborNode.getId()) == null){
                    visit.put(neighborNode.getId(), false);
                    parent.put(neighborNode.getId(), node.getId());
                    queue.add(neighborNode);
                }else{
                    // == true
                    continue;
                }

            }else{
                // let neighbor be random access
                randomQueue.add(neighborNode);
            }
        }
        for(MultiNode neighborNode : randomQueue){
            if(visit.get(neighborNode.getId()) == null){
                visit.put(neighborNode.getId(), false);
                parent.put(neighborNode.getId(), node.getId());
                queue.add(neighborNode);
            }else{
                continue;
            }
        }
        randomQueue.clear();
        queue.remove(0);

        if(visit.containsKey(Integer.toString(destNodeId))){
            List<String> path = new ArrayList<>();
            List<Integer> pathNodeIds = new ArrayList<>();

            MultiNode destNode = topologyGraph.getNode(Integer.toString(destNodeId));
            path.add((String)node.getEdgeBetween(destNode).getAttribute("address_" + Integer.toString(destNodeId)));
            pathNodeIds.add(destNodeId);
            
            return new PathDescriptor(path.toArray(new String[0]), pathNodeIds);
        }

        while (!queue.isEmpty()) {
            node = queue.get(0);
            visit.put(node.getId(), true);

            for(Edge e : node.getEdgeSet()){
                MultiNode neighborNode = e.getOpposite(node);
    
                if(Integer.parseInt(neighborNode.getId()) == destNodeId){
                    found = true;
                }

                int r = rand.nextInt(2);
                // origin block
                if(r == 0 || found){
    
                    if(visit.get(neighborNode.getId()) == null){
                        visit.put(neighborNode.getId(), false);
                        parent.put(neighborNode.getId(), node.getId());
                        queue.add(neighborNode);
                    }else{
                        continue;
                    }
    
                }else{
                    // let neighbor be random access
                    randomQueue.add(neighborNode);
                }

                if(found){
                    break;
                }
            }

            if(found){
                randomQueue.clear();
                queue.clear();
                break;
            }

            for(MultiNode neighborNode : randomQueue){
                if(visit.get(neighborNode.getId()) == null){
                    visit.put(neighborNode.getId(), false);
                    parent.put(neighborNode.getId(), node.getId());
                    queue.add(neighborNode);
                }else{
                    continue;
                }
            }
            randomQueue.clear();
            queue.remove(0);
        }


        List<String> path = new ArrayList<>();
        List<Integer> pathNodeIDs = new ArrayList<>();
        MultiNode currentNode = topologyGraph.getNode(Integer.toString(destNodeId));

        String parentNodeID = parent.get(Integer.toString(destNodeId));
        MultiNode parentNode = topologyGraph.getNode(parentNodeID);
        
        while(parentNode != null){
            Edge edge = currentNode.getEdgeBetween(parentNodeID);
            
            path.add(0,(String)edge.getAttribute("address_" + currentNode.getId()));
            pathNodeIDs.add(0,Integer.parseInt(currentNode.getId()));

            currentNode = parentNode;
            parentNodeID = parent.get(parentNodeID);
            parentNode = topologyGraph.getNode(parentNodeID);
        }


        topologyGraph.clear();
        return new PathDescriptor(path.toArray(new String[0]), pathNodeIDs);
    }
}