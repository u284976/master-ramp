package test.iotos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.Graphs;
import org.graphstream.graph.implementations.MultiNode;

import android.R.bool;
import it.unibo.deis.lia.ramp.core.e2e.E2EComm;
import it.unibo.deis.lia.ramp.core.internode.Resolver;
import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.ApplicationRequirements;
import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.TrafficType;
import it.unibo.deis.lia.ramp.core.internode.sdn.controllerMessage.ControllerMessageResponse;
import it.unibo.deis.lia.ramp.core.internode.sdn.controllerMessage.MessageType;
import it.unibo.deis.lia.ramp.core.internode.sdn.controllerService.ControllerService;
import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.TopologyGraphSelector;
import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.pathDescriptors.PathDescriptor;
import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.pathSelectors.BreadthFirstFlowPathSelector;

/**
 * @author u284976
 */

public class GeneticAlgo implements TopologyGraphSelector{

    private static final double NODE_OVERLOAD = -1;
    private static final double LINK_OVERLOAD = -2;
    private static final double fitnessThreshold = 1;

    private static final int GenerateMAX = 5;
    
    private Graph topologyGraph;

    

    public GeneticAlgo(Graph topologyGraph){
        this.topologyGraph = topologyGraph;
    }

    @Override
    public PathDescriptor selectPath(int sourceNodeId, int destNodeId, ApplicationRequirements applicationRequirements, Map<Integer, PathDescriptor> activePaths){
        System.out.println("===========GeneticAlgo============");
        System.out.println("receive request by :" + sourceNodeId);
        System.out.println("active path:");
        for(int flowID : activePaths.keySet()){
            for(int nodeID : activePaths.get(flowID).getPathNodeIds()){
                System.out.print(nodeID + " ");
            }
            System.out.println();
        }
        System.out.println("===========GeneticAlgo============");

        
        // /**
        //  * check first, sourceNode can generate this number of throughput with neighbor
        //  */
        // double maxThroughput = 0;
        // for(Edge e : topologyGraph.getNode(Integer.toString(sourceNodeId)).getEachEdge()){
        //     if((double)e.getAttribute("throughput") > maxThroughput){
        //         maxThroughput = e.getAttribute("throughput");
        //     }
        // }
        // if(applicationRequirements.getPacketRate()*applicationRequirements.getPakcetLength() > maxThroughput){
        //     PathDescriptor path = null;
        //     return path;
        // }
        PathDescriptor tempPath;
        Graph checkGraph;
        tempPath = new bfs(topologyGraph).selectPath(sourceNodeId, destNodeId);

        checkGraph = Graphs.clone(topologyGraph);

        /**
         * 2020-06-15
         * Object is "call by reference" like ArrayList, HashMap, Integer
         * variable is "call by value" like String[], int,...
         * 
         * so when i clone pathDescriptor need "new Object" for ArrayList
         */
        Map<Integer,PathDescriptor> flowPaths = new HashMap<Integer,PathDescriptor>();
        for(int key : activePaths.keySet()){
            
            List<Integer> pathNodeIDs = new ArrayList<>();
            for(int i=0 ; i<activePaths.get(key).getPathNodeIds().size() ; i++){
                pathNodeIDs.add(activePaths.get(key).getPathNodeIds().get(i));
            }
            PathDescriptor path = new PathDescriptor(activePaths.get(key).getPath(), pathNodeIDs);
            flowPaths.put(key,path);
        }

        /**
         * GeneticAlgo need other flow requirement to calculate fitness value
         */
        Map<Integer,ApplicationRequirements> originAR = ControllerService.getInstance().getFlowApplicationRequirements();
        Map<Integer,ApplicationRequirements> flowAR = new HashMap<Integer,ApplicationRequirements>();
        for(int key : originAR.keySet()){
            ApplicationRequirements AR = new ApplicationRequirements(
                originAR.get(key).getTrafficType(),
                originAR.get(key).getPakcetLength(),
                originAR.get(key).getPacketRate(),
                originAR.get(key).getRequireDelay(),
                originAR.get(key).getRequireThroughput(),
                originAR.get(key).getDuration()
            );
            flowAR.put(key, AR);
        }
        /**
         * flowPath only store passing by nodeID and NetworkInterfaceCard
         * so needed additional request the node that proposed the flow request
         * which means flow Source
         */
        Map<Integer,Integer> originSource = new HashMap<>(ControllerService.getInstance().getflowSources());
        HashMap<Integer,Integer> flowSources = new HashMap<Integer,Integer>();
        for(int key : originSource.keySet()){
            Integer SourceID = originSource.get(key);
            flowSources.put(key, SourceID);
        }

        // flowID = 1 is mean default flow, it will not duplicate other existing FlowID
        flowPaths.put(1, tempPath);
        flowAR.put(1, applicationRequirements);
        flowSources.put(1, sourceNodeId);


        /**
         * 2020-06-19 : to be the same as the "iteration" part, move this step outside the formal method
         * 
         * combine flowSource and flowPath, becaue flowPath only store target node
         * ex certain flow about "S" to "D", through M1,M2 like below:
         *   S---->M1---->M2---->D
         * 
         * flowPath only store M1,M2,D these three nodeID and NetworkInterfaceCard
         * so needed add the node that proposed the flow request
         */
        for(int flowID : flowPaths.keySet()){
            PathDescriptor path = flowPaths.get(flowID);
            List<Integer> pathNodeID = path.getPathNodeIds();
            pathNodeID.add(0, flowSources.get(flowID));
            path.setPathNodeIds(pathNodeID);
            flowPaths.put(flowID, path);
        }

        formalMethod4(checkGraph, flowPaths, flowAR);

        boolean findOverLoad = false;
        Map<Integer,Double> flowDelays = new HashMap<>();
        Map<Integer,Double> flowThroughputs = new HashMap<>();
        for(int flowID : flowPaths.keySet()){
            /**
             * in the formal method, i store "calculated" delay and throughput at the last node of flow
             */
            String lastNodeID = Integer.toString(flowPaths.get(flowID).getDestinationNodeId());
            MultiNode lastNode = checkGraph.getNode(lastNodeID);
            if((double)lastNode.getAttribute(Integer.toString(flowID)+"delayOut") == NODE_OVERLOAD || (double)lastNode.getAttribute(Integer.toString(flowID)+"delayOut") == LINK_OVERLOAD){
                findOverLoad = true;
                break;
            }
            System.out.println("===========formal method output===========");
            System.out.println("source :" + flowSources.get(flowID) + ", delay = " + lastNode.getAttribute(Integer.toString(flowID) + "delayOut") + ", throughput = " + lastNode.getAttribute(Integer.toString(flowID) + "minThroughput"));
            System.out.println("===========formal method output===========");
            flowDelays.put(flowID, lastNode.getAttribute(Integer.toString(flowID) + "delayOut"));
            flowThroughputs.put(flowID, lastNode.getAttribute(Integer.toString(flowID) + "minThroughput"));
        }

        Map<Integer,Double> flowFits = null;
        boolean allFit = true;
        if(!findOverLoad){
            flowFits = calculateFitness(flowAR,flowDelays,flowThroughputs);

            for(Integer flowID : flowFits.keySet()){
                System.out.println("===========GeneticAlgo============");
                System.out.println("fitness value of flow " + flowID + " is :" + flowFits.get(flowID));
                System.out.println("require: delay = " + flowAR.get(flowID).getRequireDelay() + ", throughput = " + flowAR.get(flowID).getRequireThroughput());
                System.out.println("predict: delay = " + flowDelays.get(flowID) + ", throughput = " + flowThroughputs.get(flowID));
                System.out.println("===========GeneticAlgo============");
                if(flowFits.get(flowID) > fitnessThreshold){
                    allFit = false;
                }
            }

            if(allFit){
                // in formal method, will modify the flowPath (add source into the pathNodeID),
                // so neede remove it
                System.out.println("===========Genetic Output============");
                System.out.println("request by node:" + sourceNodeId);
                System.out.println("find path at first BFS path, don't need to change other path");
                for(int nodeID : tempPath.getPathNodeIds()){
                    System.out.print(nodeID+" ");
                }
                System.out.println();
                System.out.println("===========Genetic Output============");
                List<Integer> pathNodeID = tempPath.getPathNodeIds();
                pathNodeID.remove(0);

                checkGraph.clear();
                assert tempPath.getPath().length == tempPath.getPathNodeIds().size();
                return tempPath;
            }
        }
        

        

        /**
         * check LINK_OVERLOAD occur when this path add to the topo, and occur between articulation point
         */
        // find articulation point
        List<Integer> articulationPoints = new dfs().findArticulationPoint(topologyGraph);
        ControllerService.getInstance().setArticulationPoints(articulationPoints);
        System.out.println("===========dfs output============");
        System.out.println("articulation Point =");
        for(int i=0 ; i<articulationPoints.size() ; i++){
            System.out.print(articulationPoints.get(i)+" ");
        }
        System.out.println();
        System.out.println("===========dfs output============");
        
        // consider fixedness
        if(ControllerService.getInstance().getEnableFixedness()){
            // this path occur LINK_OVERLOAD
            if((double)checkGraph.getNode(Integer.toString(tempPath.getDestinationNodeId())).getAttribute("1minThroughput") == LINK_OVERLOAD){
                for(int i=0 ; i<tempPath.getPathNodeIds().size()-1 ; i++){
                    int nodeID = tempPath.getPathNodeIds().get(i);
                    int nextNodeID = tempPath.getPathNodeIds().get(i+1);

                    // include ap----ap
                    if(articulationPoints.contains(nodeID) && articulationPoints.contains(nextNodeID)){

                        MultiNode node = checkGraph.getNode(Integer.toString(nodeID));
                        MultiNode nextNode = checkGraph.getNode(Integer.toString(nextNodeID));
                        Edge edge = node.getEdgeBetween(nextNode);
                        
                        double lamda = edge.getAttribute("lamda");
                        double n = edge.getAttribute("n");
                        double throughput = edge.getAttribute("throughput");

                        // ap----ap occur overload
                        if(lamda*n > throughput){
                            // first time maitain fixedness
                            if(edge.getAttribute("fixedness") == null){
                                edge.addAttribute("fixedness", 100);
                            }
                            if((int)edge.getAttribute("fixedness") > 90){
                                System.out.println("===========Genetic Output============");
                                System.out.println("ERROR: Link overload occur between articulation point, let last path request delay");
                                System.out.println("===========Genetic Output============");
                                return null;
                            }
                        }
                    }
                }
            }
        }
        

        System.out.println();
        System.out.println("===========GeneticAlgo============");
        System.out.println("first path can't let all path fit, start Genetic");
        for(int nodeID : tempPath.getPathNodeIds()){
            System.out.print(nodeID+" ");
        }
        System.out.println();
        System.out.println("===========GeneticAlgo============");
        System.out.println();

        // TODO: step3
        /**
         * tempPath found by BFS can't let all flow fit self applicationRequirement
         * find another path for each flow
         * 
         * this logical need modify to more general, like "avoid remove edge that connect to articulation point"
         */

        /**
         * cut path, if path through articulation point(AP)
         * 
         * ex. A---B---C---D---E   C,D are AP
         * 
         * so follow up GeneticAlgo will only calculate A--->C 's path
         * 
         */
        Map<Integer,PathDescriptor> flowRearPath = new HashMap<>();
        Map<Integer,Integer> flowPriorityOnAp = new HashMap<>();
        for(int flowID : flowPaths.keySet()){
            String[] path = flowPaths.get(flowID).getPath();
            List<Integer> pathNodeIDs = flowPaths.get(flowID).getPathNodeIds();
            // ex find through     A---B---"C---D"---E
            //  path : B,C,D,E (address)
            //  pathNodeIDs : A,B,C,D,E
            for(int i=0 ; i<pathNodeIDs.size()-1 ; i++){
                
                if(articulationPoints.contains(pathNodeIDs.get(i)) && articulationPoints.contains(pathNodeIDs.get(i+1))){
                    List<String> frontPath = new ArrayList<>();
                    List<Integer> frontPathNodeIDs = new ArrayList<>();
                    List<String> rearPath = new ArrayList<>();
                    List<Integer> rearPathIDs = new ArrayList<>();
                    /**
                     * still remeber path.size+1 = pathNodeIDs
                     * so we protection this relation on frontPath and frontPathIDs
                     * 
                     * ex. 
                     * frontPath:           B,C  (address)
                     * frontPathNodeIDs     A,B,C
                     * rearPath             D,E  (address)
                     * rearPathIDs          D,E
                     */
                    frontPathNodeIDs.add(pathNodeIDs.get(0));
                    for(int j=0 ; j<i ; j++){
                        frontPath.add(path[j]);
                        frontPathNodeIDs.add(pathNodeIDs.get(j+1));
                    }
                    for(int j=i ; j<pathNodeIDs.size()-1 ; j++){
                        rearPath.add(path[j]);
                        rearPathIDs.add(pathNodeIDs.get(j+1));
                    }

                    PathDescriptor front = new PathDescriptor(frontPath.toArray(new String[0]), frontPathNodeIDs);
                    flowPaths.put(flowID, front);
                    
                    PathDescriptor rear = new PathDescriptor(rearPath.toArray(new String[0]), rearPathIDs);
                    flowRearPath.put(flowID, rear);

                    // give flowID priority with application Requirement
                    if(flowAR.get(flowID).getTrafficType().equals(TrafficType.VIDEO_STREAM)){
                        flowPriorityOnAp.put(flowID, 1);
                    }else if(flowAR.get(flowID).getTrafficType().equals(TrafficType.FILE_TRANSFER)){
                        flowPriorityOnAp.put(flowID, 2);
                    }else{
                        // if any in the future
                        flowPriorityOnAp.put(flowID, 3);
                    }
                    ControllerService.getInstance().setflowPriorityOnAP(flowPriorityOnAp);

                    break;
                }
            }
        }



        //  store all path
        Map<Integer,Map<Integer,PathDescriptor>> allPaths = new HashMap<>();
        checkGraph.clear();
        
        for(int flowID : flowPaths.keySet()){
            checkGraph = Graphs.clone(topologyGraph);
            Map<Integer,PathDescriptor> fps = new HashMap<>();

            // put first path as mother Gene
            fps.put(0, flowPaths.get(flowID));

            // find second path
            PathDescriptor path = flowPaths.get(flowID);
            
            /**
             * find second path by remove first node to second node edge
             * we assume all flow will not start at  articulation point
             */
            MultiNode firstNode = checkGraph.getNode(Integer.toString(path.getPathNodeIds().get(0)));
            MultiNode secondNode = checkGraph.getNode(Integer.toString(path.getPathNodeIds().get(1)));
            checkGraph.removeEdge(firstNode, secondNode);

            // PathDescriptor secondPath = new BreadthFirstFlowPathSelector(checkGraph).selectPath(path.getPathNodeIds().get(0), path.getDestinationNodeId(), null, null);
            PathDescriptor secondPath = new bfs(checkGraph).selectPath(path.getPathNodeIds().get(0), path.getDestinationNodeId());

            if(secondPath.getPath().length != 0){
                // add source to pathNodeID like previous do
                secondPath.getPathNodeIds().add(0,flowSources.get(flowID));
                // put second path as another mother Gene
                fps.put(1, secondPath);

                System.out.println();
                System.out.println("===========GeneticAlgo============");
                System.out.println("find second path to flowID " + flowID);
                if(secondPath!=null){
                    for(int nodeID : secondPath.getPathNodeIds()){
                        System.out.print(nodeID + " ");
                    }
                }
                System.out.println();
                System.out.println("===========GeneticAlgo============");
                System.out.println();
            }
            
            allPaths.put(flowID, fps);
            checkGraph.clear();
        }

        int[] flowIDArray = new int[allPaths.size()];
        Map<Integer,Integer> flowIDArrayIndex = new HashMap<>();
        int count = 0;
        for(int flowID : allPaths.keySet()){
            flowIDArray[count] = flowID;
            flowIDArrayIndex.put(flowID, count);
            count++;
        }

        System.out.println();
        System.out.println("===========flowIDArray============");
        for(int x=0 ; x<flowIDArray.length ; x++){
            System.out.println(flowIDArray[x]);
        }
        System.out.println("===========flowIDArray============");
        System.out.println();

        Map<Integer,List<Double>> bestFitValues = new HashMap<>();
        Map<Integer,List<Integer>> bestFitTurns = new HashMap<>();
        for(int flowID : allPaths.keySet()){
            List<Double> bestFitValue = new ArrayList<>();
            bestFitValue.add(Double.MAX_VALUE);
            bestFitValue.add(Double.MAX_VALUE);
            bestFitValues.put(flowID, bestFitValue);

            List<Integer> bestFitTurn = new ArrayList<>();
            bestFitTurn.add(0);
            bestFitTurn.add(0);
            bestFitTurns.put(flowID, bestFitTurn);
        }

        for(int Generation = 0 ; Generation < GenerateMAX ; Generation++){
            checkGraph = Graphs.clone(topologyGraph);

            System.out.println();
            System.out.println("===========display mother gene============");
            for(int flowID : allPaths.keySet()){
                System.out.println("flowID = " + flowID);
                for(int x : allPaths.get(flowID).keySet()){
                    if(allPaths.get(flowID).get(x) == null){
                        continue;
                    }
                    System.out.print(x + " = ");
                    for(int nodeID : allPaths.get(flowID).get(x).getPathNodeIds()){
                        System.out.print(nodeID + " ");
                    }
                    System.out.println();
                }
            }
            System.out.println("===========display mother gene============");
            System.out.println();

            crossout(allPaths);

            // System.out.println();
            // System.out.println("===========display after cross============");
            // for(int flowID : allPaths.keySet()){
            //     System.out.println("flowID = " + flowID);
            //     for(int x : allPaths.get(flowID).keySet()){
            //         if(allPaths.get(flowID).get(x) == null){
            //             continue;
            //         }
            //         System.out.print(x + " = ");
            //         for(int nodeID : allPaths.get(flowID).get(x).getPathNodeIds()){
            //             System.out.print(nodeID + " ");
            //         }
            //         System.out.println();
            //     }
            // }
            // System.out.println("===========display after cross============");
            // System.out.println();
    
            // mutation
            for(int flowID : allPaths.keySet()){
                PathDescriptor mPath = mutation(checkGraph, allPaths.get(flowID).get(0));
                allPaths.get(flowID).put(4, mPath);
                mPath = mutation(checkGraph, allPaths.get(flowID).get(1));
                allPaths.get(flowID).put(5, mPath);
                mPath = mutation(checkGraph, allPaths.get(flowID).get(2));
                allPaths.get(flowID).put(6, mPath);
                mPath = mutation(checkGraph, allPaths.get(flowID).get(3));
                allPaths.get(flowID).put(7, mPath);
            }

            // System.out.println();
            // System.out.println("===========display after mutation============");
            // for(int flowID : allPaths.keySet()){
            //     System.out.println("flowID = " + flowID);
            //     for(int x : allPaths.get(flowID).keySet()){
            //         if(allPaths.get(flowID).get(x) == null){
            //             continue;
            //         }
            //         System.out.print(x + " = ");
            //         for(int nodeID : allPaths.get(flowID).get(x).getPathNodeIds()){
            //             System.out.print(nodeID + " ");
            //         }
            //         System.out.println();
            //     }
            // }
            // System.out.println("===========display after mutation============");
            // System.out.println();

                
            System.out.println();
            System.out.println("===========display all path============");
            for(int flowID : allPaths.keySet()){
                System.out.println("flowID = " + flowID);
                for(int x : allPaths.get(flowID).keySet()){

                    PathDescriptor p = allPaths.get(flowID).get(x);
                    if(p == null){
                        continue;
                    }
                    // if p is equal previous(this turn) path
                    for(int y=0 ; y<x ;y++){
                        PathDescriptor q = allPaths.get(flowID).get(y);
                        if(q!=null){
                            if(p.getPath().equals(q.getPath())){
                                p = null;
                                continue;
                            }
                        }
                        
                    }

                    System.out.print(x + " = ");
                    for(int nodeID : allPaths.get(flowID).get(x).getPathNodeIds()){
                        System.out.print(nodeID + " ");
                    }
                    System.out.println();
                }
            }
            System.out.println("===========display all path============");
            System.out.println();
    
            checkGraph.clear();
            
    
            // check all flow path combination Fitness value
            double totalRound = Math.pow(8.0, allPaths.size());
    
            for(int i=0 ; i<totalRound ; i++){
                flowPaths.clear();

                List<Integer> select = octIntegers(allPaths.size(), i);
                for(int j=0 ; j<select.size() ; j++){
                    flowPaths.put(flowIDArray[j], allPaths.get(flowIDArray[j]).get(select.get(j)));
                }

                // if has null, skip this round
                boolean hasNull = false;
                for(int flowID : flowPaths.keySet()){
                    if(flowPaths.get(flowID) == null){
                        hasNull = true;
                        break;
                    }
                }
                if(hasNull){
                    continue;
                }

                // System.out.println("===========brute-force method============");
                // System.out.println("ture to " + i);
                // System.out.println("choose :");
                // for(int x =0 ; x<select.size() ; x ++){
                //     System.out.print(select.get(x) + " ");
                // }
                // System.out.println();
                // for(int flowID : flowPaths.keySet()){
                //     System.out.print("flowID " + flowID + " path : ");
                //     for(int nodeID : flowPaths.get(flowID).getPathNodeIds()){
                //         System.out.print(nodeID + " ");
                //     }
                //     System.out.println();
                // }
                // System.out.println("===========brute-force method============");
    
                flowDelays.clear();
                flowThroughputs.clear();
    
                checkGraph = Graphs.clone(topologyGraph);

                formalMethod4(checkGraph, flowPaths, flowAR);
    
                findOverLoad = false;
                for(int flowID : flowPaths.keySet()){
                    /**
                     * in the formal method, i store "calculated" delay and throughput at the last node of flow
                     */
                    String lastNodeID = Integer.toString(flowPaths.get(flowID).getDestinationNodeId());
                    MultiNode lastNode = checkGraph.getNode(lastNodeID);
                    if((double)lastNode.getAttribute(Integer.toString(flowID)+"delayOut") == NODE_OVERLOAD || (double)lastNode.getAttribute(Integer.toString(flowID)+"delayOut") == LINK_OVERLOAD){
                        findOverLoad = true;
                        break;
                    }
                    flowDelays.put(flowID, lastNode.getAttribute(Integer.toString(flowID) + "delayOut"));
                    flowThroughputs.put(flowID, lastNode.getAttribute(Integer.toString(flowID) + "minThroughput"));
                }
                if(findOverLoad){
                    continue;
                }
    
                if(flowFits!=null){
                    flowFits.clear();
                }
                flowFits = calculateFitness(flowAR,flowDelays,flowThroughputs);
                
    
                allFit = true;
                for(Integer flowID : flowFits.keySet()){
                    if(flowFits.get(flowID) > fitnessThreshold){
                        allFit = false;
                    }
                }
                if(allFit){

                    // recovery rearPath
                    for(int flowID : flowPaths.keySet()){

                        if(flowRearPath.get(flowID) != null){
                            List<String> totalPath = new ArrayList<>();
                            List<Integer> totalPathNodeIDs = new ArrayList<>();

                            for(int nodeID=0 ; nodeID<flowPaths.get(flowID).getPath().length ; nodeID++){
                                totalPath.add(flowPaths.get(flowID).getPath()[nodeID]);
                                totalPathNodeIDs.add(flowPaths.get(flowID).getPathNodeIds().get(nodeID));
                            }
                            totalPathNodeIDs.add(flowPaths.get(flowID).getPathNodeIds().get(flowPaths.get(flowID).getPath().length));

                            for(int nodeID=0 ; nodeID<flowRearPath.get(flowID).getPath().length ; nodeID++){
                                totalPath.add(flowRearPath.get(flowID).getPath()[nodeID]);
                                totalPathNodeIDs.add(flowRearPath.get(flowID).getPathNodeIds().get(nodeID));
                            }

                            PathDescriptor total = new PathDescriptor(totalPath.toArray(new String[0]), totalPathNodeIDs);
                            flowPaths.put(flowID, total);
                        }
                    }


                    
                    System.out.println("===========Genetic Output============");
                    System.out.println("find combination can let all path fit");
                    for(int flowID : flowPaths.keySet()){
                        System.out.print("flowID = " + flowID + " : ");

                        for(int nodeID : flowPaths.get(flowID).getPathNodeIds()){
                            System.out.print(nodeID + " ");
                        }
                        System.out.println();
                        System.out.println("require: delay = " + flowAR.get(flowID).getRequireDelay() + ", throughput = " + flowAR.get(flowID).getRequireThroughput());
                        System.out.println("predict: delay = " + flowDelays.get(flowID) + ", throughput = " + flowThroughputs.get(flowID));
                        System.out.println("fitness value is :" + flowFits.get(flowID));
                    }
                    System.out.println("===========Genetic Output============");

                    // in formal method, need flow Source so we add previoous
                    // here neede remove it
                    flowPaths.get(1).getPathNodeIds().remove(0);
    
                    handlePathChange(activePaths, flowPaths);
    
                    return flowPaths.get(1);
                }else{
                    // certain flow can't fit well   
                    // if it fitness value better than previous iteration
                    // store it value and iteration's index
                    // and try next combination
                    for(int flowID : flowPaths.keySet()){
                        if(flowFits.get(flowID) < bestFitValues.get(flowID).get(0)){
                            bestFitValues.get(flowID).add(0,flowFits.get(flowID));
                            bestFitValues.get(flowID).remove(2);
    
                            bestFitTurns.get(flowID).add(0,i);
                            bestFitTurns.get(flowID).remove(2);
    
                        }else if(flowFits.get(flowID) < bestFitValues.get(flowID).get(1)){
                            bestFitValues.get(flowID).add(1,flowFits.get(flowID));
                            bestFitValues.get(flowID).remove(2);
    
                            bestFitTurns.get(flowID).add(1,i);
                            bestFitTurns.get(flowID).remove(2);
                        }
                    }
                }
            }

            // replace Gene mothor
            Map<Integer, Map<Integer, PathDescriptor>> newAllPath = new HashMap<>();

            /**
             * choose bestfit turn , but may choose the same path
             */
            for(int flowID : allPaths.keySet()){

                int indexInArray = flowIDArrayIndex.get(flowID);

                List<Integer> bestTurnCombine = octIntegers(allPaths.size(), bestFitTurns.get(flowID).get(0));
                int recordPathIndex = bestTurnCombine.get(indexInArray);
                
                Map<Integer,PathDescriptor> fps = new HashMap<>();
                fps.put(0, allPaths.get(flowID).get(recordPathIndex));

                bestTurnCombine = octIntegers(allPaths.size(), bestFitTurns.get(flowID).get(1));
                recordPathIndex = bestTurnCombine.get(indexInArray);
                fps.put(1, allPaths.get(flowID).get(recordPathIndex));

                newAllPath.put(flowID, fps);
            }

            // choose shortest and second shortest
            // for(int flowID : allPaths.keySet()){
            //     int indexInArray = flowIDArrayIndex.get(flowID);
            // }
            allPaths.clear();
            allPaths = newAllPath;
        }

        


        // iteration fail
        // choose "last" path best turn
        flowPaths.clear();
        List<Integer> select = octIntegers(allPaths.size(), bestFitTurns.get(1).get(0));
        for(int j=0 ; j<select.size() ; j++){
            flowPaths.put(flowIDArray[j], allPaths.get(flowIDArray[j]).get(select.get(j)));
        }

        // recovery rearPath
        for(int flowID : flowPaths.keySet()){

            if(flowRearPath.get(flowID) != null){
                List<String> totalPath = new ArrayList<>();
                List<Integer> totalPathNodeIDs = new ArrayList<>();

                for(int nodeID=0 ; nodeID<flowPaths.get(flowID).getPath().length ; nodeID++){
                    totalPath.add(flowPaths.get(flowID).getPath()[nodeID]);
                    totalPathNodeIDs.add(flowPaths.get(flowID).getPathNodeIds().get(nodeID));
                }
                totalPathNodeIDs.add(flowPaths.get(flowID).getPathNodeIds().get(flowPaths.get(flowID).getPath().length));

                for(int nodeID=0 ; nodeID<flowRearPath.get(flowID).getPath().length ; nodeID++){
                    totalPath.add(flowRearPath.get(flowID).getPath()[nodeID]);
                    totalPathNodeIDs.add(flowRearPath.get(flowID).getPathNodeIds().get(nodeID));
                }

                PathDescriptor total = new PathDescriptor(totalPath.toArray(new String[0]), totalPathNodeIDs);
                flowPaths.put(flowID, total);
            }
        }


        System.out.println("===========Genetic Output============");
        System.out.println("request by node:" + sourceNodeId);
        System.out.println("can't find combination let all path fit, choose last generation best combination");
        for(int flowID : flowPaths.keySet()){
            System.out.print("flowID = " + flowID + " : ");

            for(int nodeID : flowPaths.get(flowID).getPathNodeIds()){
                System.out.print(nodeID + " ");
            }
            System.out.println();
        }
        System.out.println("===========Genetic Output============");

        handlePathChange(activePaths, flowPaths);

        flowPaths.get(1).getPathNodeIds().remove(0);
        return flowPaths.get(1);
    }

    @Override
    public Map<Integer, PathDescriptor> getAllPathsFromSource(int sourceNodeId) {
        Map<Integer, PathDescriptor> paths = new BreadthFirstFlowPathSelector(topologyGraph).getAllPathsFromSource(sourceNodeId);
        return paths;
    }

    private List<Integer> octIntegers(int totalLength, int number){
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

    private void handlePathChange(Map<Integer,PathDescriptor> activePaths, Map<Integer,PathDescriptor> flowPaths){
        for(int flowID : activePaths.keySet()){

            // why not compare two map directly?
            // because have some variable different, ex : creation time, flowPaths's is empty
            if(activePaths.get(flowID).getPath().equals(flowPaths.get(flowID).getPath())){
                continue;
            }

            int noticeSourceID = activePaths.get(flowID).getPathNodeIds().get(0);
            String[] noticeSourceDest = Resolver.getInstance(false).resolveBlocking(noticeSourceID, 5 * 1000).get(0).getPath();
            MultiNode noticeNode = topologyGraph.getNode(Integer.toString(noticeSourceID));
            int noticeNodePort = noticeNode.getAttribute("port");

            // in formal method, need flow Source so we add previoous
            // here neede remove it
            flowPaths.get(flowID).getPathNodeIds().remove(0);

            List<PathDescriptor> newPaths = new ArrayList<>();
            newPaths.add(flowPaths.get(flowID));
            ControllerMessageResponse updateMessage = new ControllerMessageResponse(MessageType.FIX_PATH_PUSH_RESPONSE, flowID, newPaths);

            try {
                E2EComm.sendUnicast(noticeSourceDest, noticeSourceID, noticeNodePort, E2EComm.TCP, 0, E2EComm.serialize(updateMessage));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void crossout(Map<Integer,Map<Integer,PathDescriptor>> allPaths){
        for(int flowID : allPaths.keySet()){
            Map<Integer,PathDescriptor> fps = allPaths.get(flowID);
            PathDescriptor aPath = fps.get(0);
            PathDescriptor bPath = fps.get(1);

            // may occur when no second path exist
            if(bPath == null){
                continue;
            }

            List<Integer> commonGenes;
            commonGenes = findCommonGenes(aPath, bPath);

            // if no common genes skip this step
            if(commonGenes.size() == 0){
                fps.put(2, null);
                fps.put(3, null);
                continue;
            }

            Random rand = new Random(); 
    
            // Generate random integers in range 0 to commonGenes.size()-1 
            int choosenode = commonGenes.get(rand.nextInt(commonGenes.size())); 

            int seqIna = aPath.getPathNodeIds().indexOf(choosenode);
            int seqInb = bPath.getPathNodeIds().indexOf(choosenode);

            List<String> path = new ArrayList<>();
            List<Integer> pathNodeID = new ArrayList<>();

            /**
             * remeber List<Integer> pathNodeID 's index is one more than String[] path
             */
            pathNodeID.add(aPath.getPathNodeIds().get(0));
            for(int i=1 ; i<= seqIna ; i++){
                pathNodeID.add(aPath.getPathNodeIds().get(i));
                path.add(aPath.getPath()[i-1]);
            }
            for(int i=seqInb+1 ; i<=bPath.getPathNodeIds().size()-1 ; i++){
                pathNodeID.add(bPath.getPathNodeIds().get(i));
                path.add(bPath.getPath()[i-1]);
            }
            PathDescriptor cPath = new PathDescriptor(path.toArray(new String[0]), pathNodeID);
            fps.put(2, cPath);

            path = new ArrayList<>();
            pathNodeID = new ArrayList<>();

            pathNodeID.add(bPath.getPathNodeIds().get(0));
            for(int i=1 ; i<= seqInb ; i++){
                pathNodeID.add(bPath.getPathNodeIds().get(i));
                path.add(bPath.getPath()[i-1]);
            }
            for(int i=seqIna+1 ; i<=aPath.getPathNodeIds().size()-1 ; i++){
                pathNodeID.add(aPath.getPathNodeIds().get(i));
                path.add(aPath.getPath()[i-1]);
            }
            PathDescriptor dPath = new PathDescriptor(path.toArray(new String[0]), pathNodeID);
            fps.put(3, dPath);
        }
    }

    /**
     * this function will return Common node between 2 path without first and last node
     */
    private List<Integer> findCommonGenes(PathDescriptor a, PathDescriptor b){
        List<Integer> commonGenes = new ArrayList<>();
    
        for(int nodeID1 : a.getPathNodeIds()){
            for(int nodeID2 : b.getPathNodeIds()){
                if(nodeID1 == a.getPathNodeIds().get(0) || nodeID1 == a.getPathNodeIds().get(a.getPathNodeIds().size()-1)){
                    continue;
                }
                if(nodeID1 == nodeID2){
                    commonGenes.add(nodeID1);
                }
            }
        }

        return commonGenes;
    }

    private PathDescriptor mutation(Graph tempGraph, PathDescriptor path){

        if(path != null){
            int bottleneck = findBottleneck(tempGraph, path);
            // System.out.println();
            // System.out.println("===========mutation============");
            // System.out.println("source : " + path.getPathNodeIds().get(0));
            // System.out.println("bottleneck = " + bottleneck);
            // System.out.println("===========mutation============");
            // System.out.println();
    
            PathDescriptor mPath = null;
            if(bottleneck == -1){
                mPath = null;
            }else{
                MultiNode node = tempGraph.getNode(Integer.toString(path.getPathNodeIds().get(bottleneck)));
                MultiNode nextNode = tempGraph.getNode(Integer.toString(path.getPathNodeIds().get(bottleneck+1)));
                
                Edge edge = tempGraph.removeEdge(node, nextNode);
    
                int nodeID = Integer.parseInt(node.getId());
                int finalNodeID = path.getDestinationNodeId();
                // mPath = new BreadthFirstFlowPathSelector(tempGraph).selectPath(nodeID, finalNodeID, null, null);
                mPath = new bfs(tempGraph).selectPath(nodeID, finalNodeID);
                
                // recovery edge
                tempGraph.addEdge(edge.getId(), node, nextNode);
                for(String key : edge.getAttributeKeySet()){
                    node.getEdgeBetween(nextNode).addAttribute(key, (Object)edge.getAttribute(key));
                }
    
                if(mPath != null){
                    // add source
                    mPath.getPathNodeIds().add(0, nodeID);
    
                    // if mutation let the path has loop, drop it.
                    mPath = combineGene(path,mPath);
                }
            }
    
            return mPath;

        }else{
            return null;
        }
    }

    private int findBottleneck(Graph tempGraph, PathDescriptor path){

        // this path too short
        if(path.getPathNodeIds().size() <= 2){
            return -1;
        }

        int bottleneckSeq = -1;
        double minThroughput = Double.MAX_VALUE;
        for(int i=0 ; i<path.getPathNodeIds().size()-1 ; i++){
            MultiNode node = tempGraph.getNode(Integer.toString(path.getPathNodeIds().get(i)));
            MultiNode nextNode = tempGraph.getNode(Integer.toString(path.getPathNodeIds().get(i+1)));

            String nextAddr = path.getPath()[i];
            
            for(Edge edge : node.getEdgeSetBetween(nextNode)){
                if(edge.getAttribute("address_"+nextNode.getId()) == nextAddr){

                    double throughput = (double)edge.getAttribute("throughput");

                    if(throughput < minThroughput){
                        minThroughput = throughput;
                        bottleneckSeq = i;
                    }
                }
            }
        }
        return bottleneckSeq;
    }

    private PathDescriptor combineGene(PathDescriptor a, PathDescriptor b){

        PathDescriptor mPath = null;

        int mutationPoint = a.getPathNodeIds().indexOf(b.getPathNodeIds().get(0));

        boolean findLoop = false;
        for(int i=0 ; i<mutationPoint ; i++){
            if(b.getPathNodeIds().indexOf(a.getPathNodeIds().get(i)) != -1){
                findLoop = true;
                break;
            }
        }

        if(findLoop == true){
            return mPath;
        }else{
            List<String> path = new ArrayList<>();
            List<Integer> pathNodeID = new ArrayList<>();

            pathNodeID.add(a.getPathNodeIds().get(0));
            for(int i=0 ; i<mutationPoint ; i++){
                path.add(a.getPath()[i]);
                pathNodeID.add(a.getPathNodeIds().get(i+1));
            }

            b.getPathNodeIds().remove(0);
            for(int i=0 ; i<b.getPathNodeIds().size() ; i++){
                path.add(b.getPath()[i]);
                pathNodeID.add(b.getPathNodeIds().get(i));
            }

            mPath = new PathDescriptor(path.toArray(new String[0]), pathNodeID);
        }

        return mPath;
    }
    private void formalMethod4(Graph tempGraph, Map<Integer,PathDescriptor> flowPaths,Map<Integer,ApplicationRequirements> flowAR){

        // init edge parameter
        for(Edge edge : tempGraph.getEachEdge()){
            edge.addAttribute("lamda", 0.0);
            edge.addAttribute("n", 0.0);
        }
        // init node parameter
        for(Node node : tempGraph.getEachNode()){
            node.addAttribute("lamda", 0.0);
            node.addAttribute("n", 0.0);

            // let neighbor link max throughput as node throughput
            double maxThroughput = 0;
            for(Edge edge : node.getEdgeSet()){
                if((double)edge.getAttribute("throughput") > maxThroughput){
                    maxThroughput = (double)edge.getAttribute("throughput");
                }
            }
            node.addAttribute("maxThroughput", maxThroughput);
        }

        /**
         * setup "lamda","n" attribute by "flowPath" on node and edge
         * 
         * lamda = combine lamda    <---- see code or paper
         * n = combine n            <---- see code or paper
         * ============
         * requires attention!!!
         * path.getPathNodeIds() is List<Integer> include Source, ex S,M1,M2,D
         * path.getPath() is String[] "not" include Source, only M1,M2,D NetworkInterfaceCard address
         */
        for(int flowID : flowPaths.keySet()){
            PathDescriptor path = flowPaths.get(flowID);
            String[] pathAddress = path.getPath();
            List<Integer> pathNodeIDs = path.getPathNodeIds();

            double flow_lamda = (double)flowAR.get(flowID).getPacketRate();
            double flow_n = (double)flowAR.get(flowID).getPakcetLength();

            // set up node 
            for(int nodeID : pathNodeIDs){
                MultiNode node = tempGraph.getNode(Integer.toString(nodeID));
                double oldLamda = (double)node.getAttribute("lamda");
                double oldN = (double)node.getAttribute("n");

                double new_lamda = oldLamda + flow_lamda;
                double new_n = (flow_lamda*flow_n + oldLamda*oldN) / new_lamda;

                node.addAttribute("lamda", new_lamda);
                node.addAttribute("n", new_n);
            }

            // set up edge
            for(int i=0 ; i<pathNodeIDs.size()-1 ; i++){
                MultiNode node = tempGraph.getNode(Integer.toString(pathNodeIDs.get(i)));
                MultiNode nextNode = tempGraph.getNode(Integer.toString(pathNodeIDs.get(i+1)));
                String nextAddr = pathAddress[i];

                for(Edge edge : node.getEdgeSetBetween(nextNode)){
                    /**
                     * maybe have multiple link to one neighbor node
                     * so only select that target Network Interface Card address
                     */
                    if(edge.getAttribute("address_" + nextNode.getId()) == nextAddr){

                        // for maitain wireless characteristic, "in" and "out" are use same network interface card
                        String currentAddr = (String)edge.getAttribute("address_" + node.getId());
                        for(Edge sameNIC : node.getEdgeSet()){
                            if(sameNIC.getAttribute("address_" + node.getId()).equals(currentAddr)){
                                double oldLamda = (double)sameNIC.getAttribute("lamda");
                                double oldN = (double)sameNIC.getAttribute("n");
    
                                double new_lamda = oldLamda + flow_lamda;
                                double new_n = (flow_lamda*flow_n + oldLamda*oldN) / new_lamda;
    
                                sameNIC.addAttribute("lamda", new_lamda);
                                sameNIC.addAttribute("n", new_n);
                            }
                        }
                    }
                }
            }
        }

        // for each flow path
        for(int flowID : flowPaths.keySet()){
            PathDescriptor path = flowPaths.get(flowID);
            String[] pathAddress = path.getPath();
            List<Integer> pathNodeIDs = path.getPathNodeIds();

            // On the way of flow, the smallest throughput
            double minThroughput = Double.MAX_VALUE;
            double delayOut = 0;


            double flow_lamda = (double)flowAR.get(flowID).getPacketRate();
            double flow_n = (double)flowAR.get(flowID).getPakcetLength();
            for(int i=0 ; i<pathNodeIDs.size()-1 ; i++){

                /**
                 * calculate dispatch<--->network interface card
                 */
                String nodeID = Integer.toString(pathNodeIDs.get(i));
                String nextNodeID = Integer.toString(pathNodeIDs.get(i+1));
                MultiNode node = tempGraph.getNode(nodeID);
                MultiNode nextNode = tempGraph.getNode(nextNodeID);

                double node_throughput = (double)node.getAttribute("maxThroughput");
                
                double node_lamda = (double)node.getAttribute("lamda");
                double node_n = (double)node.getAttribute("n");

                double node_capacity = node_throughput - node_lamda*node_n;
                if(node_capacity > 0){

                    delayOut = delayOut + flow_n/node_capacity;

                    if(node_throughput < minThroughput){
                        minThroughput = node_throughput;
                    }

                }else{
                    System.out.println();
                    System.out.println("===========formalMethod============");
                    System.out.println("overload on node: " + nodeID);
                    System.out.println("===========formalMethod============");
                    System.out.println();

                    // continues for other flowPaths
                    // and let fitness value be a speicial value
                    delayOut = NODE_OVERLOAD;
                    minThroughput = NODE_OVERLOAD;
                    
                    break;
                }


                /**
                 * calculate link between node,nextNode
                 * 
                 * maybe have multiple link to nextNode
                 * so only select that target Network Interface Card address
                 */
                boolean failonLink = false;
                String nextAddr = pathAddress[i];
                for(Edge edge : node.getEdgeSetBetween(nextNode)){
                    if(edge.getAttribute("address_" + nextNode.getId()) == nextAddr){
                        
                        double link_throughput = (double)edge.getAttribute("throughput");

                        double link_lamda = (double)edge.getAttribute("lamda");
                        double link_n = (double)edge.getAttribute("n");

                        double link_capacity = link_throughput - link_lamda*link_n;
                        if(link_capacity > 0){

                            double link_delay = (double)edge.getAttribute("delay");
                            delayOut = delayOut + link_delay;

                            if(link_throughput < minThroughput){
                                minThroughput = link_throughput;
                            }
                        }else{
                            System.out.println();
                            System.out.println("===========formalMethod============");
                            System.out.println("overload on link between : " + node.getId() + " to " + nextNode.getId());
                            System.out.println("===========formalMethod============");
                            System.out.println();
                            // continues for other flowPaths
                            // and let fitness value be a speicial value
                            delayOut = LINK_OVERLOAD;
                            minThroughput = LINK_OVERLOAD;
                            
                            failonLink = true;
                        }

                        break;
                    }
                }
                if(failonLink){
                    break;
                }
            }

            int lastNodeID = path.getDestinationNodeId();
            MultiNode lastNode = tempGraph.getNode(Integer.toString(lastNodeID));
            lastNode.addAttribute(Integer.toString(flowID) + "delayOut", delayOut);
            lastNode.addAttribute(Integer.toString(flowID) + "minThroughput", minThroughput);
        }
    }

    private void formalMethod3(Graph tempGraph, Map<Integer,PathDescriptor> flowPaths,
    Map<Integer,ApplicationRequirements> flowAR){

        // init edge parameter
        for(Edge edge : tempGraph.getEachEdge()){
            edge.addAttribute("lamda", 0.0);
            edge.addAttribute("n", 0.0);
        }
        // init node parameter
        for(Node node : tempGraph.getEachNode()){
            node.addAttribute("lamda", 0.0);
            node.addAttribute("n", 0.0);

            // let neighbor link max throughput as node throughput
            double maxThroughput = 0;
            for(Edge edge : node.getEdgeSet()){
                if((double)edge.getAttribute("throughput") > maxThroughput){
                    maxThroughput = (double)edge.getAttribute("throughput");
                }
            }
            node.addAttribute("maxThroughput", maxThroughput);
        }

        /**
         * setup "lamda","n" attribute by "flowPath" on node and edge
         * 
         * lamda = combine lamda    <---- see code or paper
         * n = combine n            <---- see code or paper
         * ============
         * requires attention!!!
         * path.getPathNodeIds() is List<Integer> include Source, ex S,M1,M2,D
         * path.getPath() is String[] "not" include Source, only M1,M2,D NetworkInterfaceCard address
         */
        for(int flowID : flowPaths.keySet()){
            PathDescriptor path = flowPaths.get(flowID);
            String[] pathAddress = path.getPath();
            List<Integer> pathNodeIDs = path.getPathNodeIds();

            double flow_lamda = (double)flowAR.get(flowID).getPacketRate();
            double flow_n = (double)flowAR.get(flowID).getPakcetLength();

            // set up node 
            for(int nodeID : pathNodeIDs){
                MultiNode node = tempGraph.getNode(Integer.toString(nodeID));
                double oldLamda = (double)node.getAttribute("lamda");
                double oldN = (double)node.getAttribute("n");

                double new_lamda = oldLamda + flow_lamda;
                double new_n = (flow_lamda*flow_n + oldLamda*oldN) / new_lamda;

                node.addAttribute("lamda", new_lamda);
                node.addAttribute("n", new_n);
            }

            // set up edge
            for(int i=0 ; i<pathNodeIDs.size()-1 ; i++){
                MultiNode node = tempGraph.getNode(Integer.toString(pathNodeIDs.get(i)));
                MultiNode nextNode = tempGraph.getNode(Integer.toString(pathNodeIDs.get(i+1)));
                String nextAddr = pathAddress[i];

                for(Edge edge : node.getEdgeSetBetween(nextNode)){
                    /**
                     * maybe have multiple link to one neighbor node
                     * so only select that target Network Interface Card address
                     */
                    if(edge.getAttribute("address_" + nextNode.getId()) == nextAddr){
                        double oldLamda = (double)edge.getAttribute("lamda");
                        double oldN = (double)edge.getAttribute("n");

                        double new_lamda = oldLamda + flow_lamda;
                        double new_n = (flow_lamda*flow_n + oldLamda*oldN) / new_lamda;

                        edge.addAttribute("lamda", new_lamda);
                        edge.addAttribute("n", new_n);
                    }
                }
            }
        }

        // for each flow path
        for(int flowID : flowPaths.keySet()){
            PathDescriptor path = flowPaths.get(flowID);
            String[] pathAddress = path.getPath();
            List<Integer> pathNodeIDs = path.getPathNodeIds();

            // On the way of flow, the smallest throughput
            double minThroughput = Double.MAX_VALUE;
            double delayOut = 0;


            double flow_lamda = (double)flowAR.get(flowID).getPacketRate();
            double flow_n = (double)flowAR.get(flowID).getPakcetLength();
            for(int i=0 ; i<pathNodeIDs.size()-1 ; i++){

                /**
                 * calculate dispatch<--->network interface card
                 */
                String nodeID = Integer.toString(pathNodeIDs.get(i));
                String nextNodeID = Integer.toString(pathNodeIDs.get(i+1));
                MultiNode node = tempGraph.getNode(nodeID);
                MultiNode nextNode = tempGraph.getNode(nextNodeID);

                double node_throughput = (double)node.getAttribute("maxThroughput");
                
                double node_lamda = (double)node.getAttribute("lamda");
                double node_n = (double)node.getAttribute("n");

                double node_capacity = node_throughput - node_lamda*node_n;
                if(node_capacity > 0){

                    delayOut = delayOut + flow_n/node_capacity;

                    if(node_throughput < minThroughput){
                        minThroughput = node_throughput;
                    }

                }else{
                    System.out.println();
                    System.out.println("===========formalMethod============");
                    System.out.println("overload on node: " + nodeID);
                    System.out.println("===========formalMethod============");
                    System.out.println();

                    // continues for other flowPaths
                    // and let fitness value be a speicial value
                    delayOut = NODE_OVERLOAD;
                    minThroughput = NODE_OVERLOAD;
                    
                    break;
                }


                /**
                 * calculate link between node,nextNode
                 * 
                 * maybe have multiple link to nextNode
                 * so only select that target Network Interface Card address
                 */
                boolean failonLink = false;
                String nextAddr = pathAddress[i];
                for(Edge edge : node.getEdgeSetBetween(nextNode)){
                    if(edge.getAttribute("address_" + nextNode.getId()) == nextAddr){
                        
                        double link_throughput = (double)edge.getAttribute("throughput");

                        double link_lamda = (double)edge.getAttribute("lamda");
                        double link_n = (double)edge.getAttribute("n");

                        double link_capacity = link_throughput - link_lamda*link_n;
                        if(link_capacity > 0){

                            double link_delay = (double)edge.getAttribute("delay");
                            delayOut = delayOut + link_delay;

                            if(link_throughput < minThroughput){
                                minThroughput = link_throughput;
                            }
                        }else{
                            System.out.println();
                            System.out.println("===========formalMethod============");
                            System.out.println("overload on link between : " + node.getId() + " to " + nextNode.getId());
                            System.out.println("===========formalMethod============");
                            System.out.println();
                            // continues for other flowPaths
                            // and let fitness value be a speicial value
                            delayOut = LINK_OVERLOAD;
                            minThroughput = LINK_OVERLOAD;
                            
                            failonLink = true;
                        }

                        break;
                    }
                }
                if(failonLink){
                    break;
                }
            }

            int lastNodeID = path.getDestinationNodeId();
            MultiNode lastNode = tempGraph.getNode(Integer.toString(lastNodeID));
            lastNode.addAttribute(Integer.toString(flowID) + "delayOut", delayOut);
            lastNode.addAttribute(Integer.toString(flowID) + "minThroughput", minThroughput);
        }
    }    
    
    private Map<Integer,Double> calculateFitness(Map<Integer,ApplicationRequirements> flowAR,
                                    Map<Integer,Double> flowDelays,
                                    Map<Integer,Double> flowThroughputs){
        Map<Integer,Double> flowFits = new HashMap<>();
        for(int flowID : flowDelays.keySet()){
            TrafficType trafficType = flowAR.get(flowID).getTrafficType();
            /**
             * weight of parameter
             * a - for delay
             * b - for throughput
             */
            double a;
            double b;
            if(trafficType.equals(TrafficType.VIDEO_STREAM)){
                a = 1;
                b = 0;
            }else if(trafficType.equals(TrafficType.FILE_TRANSFER)){
                a = 0;
                b = 1;
            }else{
                a = 1;
                b = 1;
            }
            double AR_Delay = flowAR.get(flowID).getRequireDelay();
            double AR_Throughput = flowAR.get(flowID).getRequireThroughput();
            double measure_delay = flowDelays.get(flowID);
            double measure_throughput = flowThroughputs.get(flowID);

            /**
             * smaller fitness value is better
             * if value is negative, let value = 0
             */
            double Fitness = a * (measure_delay - AR_Delay) + b * (AR_Throughput - measure_throughput);
            if(Fitness < 0){
                Fitness = 0;
            }
            flowFits.put(flowID, Fitness);
        }

        return flowFits;
    }
}