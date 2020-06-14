package test.iotos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.Graphs;
import org.graphstream.graph.implementations.MultiNode;

import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.ApplicationRequirements;
import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.TrafficType;
import it.unibo.deis.lia.ramp.core.internode.sdn.controllerService.ControllerService;
import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.TopologyGraphSelector;
import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.pathDescriptors.PathDescriptor;
import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.pathSelectors.BreadthFirstFlowPathSelector;

/**
 * @author u284976
 */

public class GeneticAlgo implements TopologyGraphSelector{
    
    
    private Graph topologyGraph;

    private double fitnessThreshold = 100;

    public GeneticAlgo(Graph topologyGraph){
        this.topologyGraph = topologyGraph;
    }

    @Override
    public PathDescriptor selectPath(int sourceNodeId, int destNodeId, ApplicationRequirements applicationRequirements, Map<Integer, PathDescriptor> activePaths){

        PathDescriptor tempPath = new BreadthFirstFlowPathSelector(topologyGraph).selectPath(sourceNodeId, destNodeId, null, null);
        Graph checkGraph = Graphs.clone(topologyGraph);

        Map<Integer,PathDescriptor> flowPaths = new HashMap<Integer,PathDescriptor>();
        for(int key : activePaths.keySet()){
            PathDescriptor path = new PathDescriptor(activePaths.get(key).getPath(), activePaths.get(key).getPathNodeIds());
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

        formalMethod(checkGraph, flowPaths, flowAR, flowSources, sourceNodeId);

        Map<Integer,Double> flowDelays = new HashMap<>();
        Map<Integer,Double> flowThroughputs = new HashMap<>();
        for(int flowID : flowPaths.keySet()){
            /**
             * in the formal method, i store "calculated" delay and throughput at the last node of flow
             */
            String lastNodeID = Integer.toString(flowPaths.get(flowID).getDestinationNodeId());
            MultiNode lastNode = checkGraph.getNode(lastNodeID);
            flowDelays.put(flowID, lastNode.getAttribute(Integer.toString(flowID) + "delayOut"));
            flowThroughputs.put(flowID, lastNode.getAttribute(Integer.toString(flowID) + "minThroughput"));
        }

        Map<Integer,Double> flowFits = calculateFitness(flowAR,flowDelays,flowThroughputs);

        // System.out.println("10.0.0.1 - 10.0.0.2:");
        // System.out.println("throughput = " + checkGraph.getNode("1").getEdgeBetween("2").getAttribute("throughput"));
        // System.out.println("10.0.0.2 - 10.0.0.3:");
        // System.out.println("throughput = " + checkGraph.getNode("2").getEdgeBetween("3").getAttribute("throughput"));
        

        boolean allFit = true;
        for(Integer flowID : flowFits.keySet()){
            System.out.println("===========GeneticAlgo============");
            System.out.println("Request by: "+ sourceNodeId);
            System.out.println("fitness value of " + flowID + " is :" + flowFits.get(flowID));
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
            List<Integer> pathNodeID = tempPath.getPathNodeIds();
            pathNodeID.remove(0);

            checkGraph.clear();
            assert tempPath.getPath().length == tempPath.getPathNodeIds().size();
            return tempPath;
        }
        // TODO: step :3
        

        PathDescriptor path = null;
        return path;
    }

    @Override
    public Map<Integer, PathDescriptor> getAllPathsFromSource(int sourceNodeId) {
        Map<Integer, PathDescriptor> paths = new BreadthFirstFlowPathSelector(topologyGraph).getAllPathsFromSource(sourceNodeId);
        return paths;
    }
    
    private void formalMethod(Graph tempGraph, Map<Integer,PathDescriptor> flowPaths,
                                Map<Integer,ApplicationRequirements> flowAR,
                                Map<Integer,Integer> flowSources,
                                int sourceNodeId){
        System.out.println("=============================");
        System.out.println("flowPaths = ");
        for(int flowID : flowPaths.keySet()){ 
            System.out.println(Integer.toString(flowID)+ ":");
            for(String s : flowPaths.get(flowID).getPath()){
                System.out.println(s);
            }
        }
        System.out.println("=============================");
        
        /**
         * add attribute in topo's node
         * 
         * cur : already arrive number of flow on this node, initial is 0
         * total : future total number of flow on this node
         * lamda : Possion process parameter means packet rate
         * n : Possion process parameter means packet length
         * 
         * 2020-06-05:
         * delete "cur" and "total"
         * because originally these two parameters were the rules set by paper author to run the formal method
         * but here is my own program, so i can ensure that "lamda" and "n" will complete setup
         * then go to the next step
         */
        for(Node node : tempGraph.getEachNode()){
            node.addAttribute("lamda", 0.0);
            node.addAttribute("n", 0.0);
        }

        /**
         * 2020-06-04
         * setup "lamda","n" attribute by "flowPath"
         * 
         * lamda = combine lamda    <---- see code or paper
         * n = combine n            <---- see code or paper
         * 
         * but flowPath only store target node
         * ex A certain flow about "S" to "D", through M1,M2 like below:
         *   S---->M1---->M2---->D
         * 
         * flowPath only store M1,M2,D these three nodeID and NetworkInterfaceCard
         * so needed add the node that proposed the flow request
         * 
         * 2020-06-05 modify to combine flowSource and flowPath First
         * ===========================================
         * requires attention!!!
         * path.getPathNodeIds() is List<Integer> include Source, ex S,M1,M2,D
         * path.getPath() is String[] "not" include Source, only M1,M2,D NetworkInterfaceCard address
         */
        for(int flowID : flowPaths.keySet()){
            PathDescriptor path = flowPaths.get(flowID);
            List<Integer> pathNodeID = path.getPathNodeIds();
            pathNodeID.add(0, flowSources.get(flowID));
            path.setPathNodeIds(pathNodeID);
            flowPaths.put(flowID, path);

            double flow_lamda = (double)flowAR.get(flowID).getPacketRate();
            double flow_n = (double)flowAR.get(flowID).getPakcetLength();
            for(int nodeID : pathNodeID){
                MultiNode node = tempGraph.getNode(Integer.toString(nodeID));
                double oldLamda = (double)node.getAttribute("lamda");
                double oldN = (double)node.getAttribute("n");

                double new_lamda = oldLamda + flow_lamda;
                double new_n = (flow_lamda*flow_n + oldLamda*oldN) / new_lamda;

                node.addAttribute("lamda", new_lamda);
                node.addAttribute("n", new_n);
            }
        }
        System.out.println("=============================");
        System.out.println("formal method : for each flow");
        

        // for each flowPath
        for(int flowID : flowPaths.keySet()){
            System.out.println("flowID = " + Integer.toString(flowID));

            PathDescriptor path = flowPaths.get(flowID);
            List<Integer> pathNodeIDs = path.getPathNodeIds();
            // On the way of flow, the smallest throughput
            double minThroughput;

            /**
             * calculate   "application Layer ---> Dispatch" delay
             * take node's max throughput with neighbor as "application Layer--->Dispatch" throughput
             */
            MultiNode sourceNode = tempGraph.getNode(Integer.toString(pathNodeIDs.get(0)));
            double maxThroughput = 0.0;
            for(Edge edge : sourceNode.getEdgeSet()){
                if((double)edge.getAttribute("throughput") > maxThroughput){
                    maxThroughput = (double)edge.getAttribute("throughput");
                }
            }
            minThroughput = maxThroughput;

            double sourceLamda = sourceNode.getAttribute("lamda");
            double sourceN = sourceNode.getAttribute("n");
            double capacity = maxThroughput - sourceLamda*sourceN;
            if(capacity > 0){
                String attributeDelayOut = Integer.toString(flowID) + "delayOut";
                double delay = sourceN / capacity;
                sourceNode.addAttribute(attributeDelayOut, delay);

                String attributeMinThroughput = Integer.toString(flowID) + "minThroughput";
                sourceNode.addAttribute(attributeMinThroughput, minThroughput);
            }else{
                // TODO: maitain this case, maybe node can't load so much
            }

            /**
             * Recursive calculate "node ---> neighbor_node" delay
             * until second last node
             */
            for(int i=0 ; i<pathNodeIDs.size()-1 ; i++){
                MultiNode lastNode = tempGraph.getNode(Integer.toString(pathNodeIDs.get(i)));
                MultiNode node = tempGraph.getNode(Integer.toString(pathNodeIDs.get(i+1)));

                /**
                 * Attention!!!
                 * path.getPathNodeIds() is List<Integer> include Source, ex. {S,M1,M2,D}
                 * path.getPath() is String[] "not" include Source, only M1,M2,D NetworkInterfaceCard address
                 * ex. {10.0.0.1, 10.0.0.2, 10.0.0.3}
                 */
                String nextAddr = path.getPath()[i];
                for(Edge edge : lastNode.getEdgeSetBetween(node)){
                    /**
                     * maybe have multiple link to one neighbor node
                     * so only select that target Network Interface Card address
                     */
                    if(edge.getAttribute("address_" + Integer.toString(pathNodeIDs.get(i+1))) == nextAddr){
                        double delayIn = lastNode.getAttribute(Integer.toString(flowID) + "delayOut");

                        double lamda = node.getAttribute("lamda");
                        double n = node.getAttribute("n");
                        double throughput = edge.getAttribute("throughput");
                        if(throughput < minThroughput){
                            minThroughput = throughput;
                        }

                        double cap = throughput - lamda*n;
                        if(cap > 0){
                            String attributeDelayOut = Integer.toString(flowID) + "delayOut";
                            double delay = (n / cap) + delayIn;
                            node.addAttribute(attributeDelayOut, delay);

                            String attributeMinThroughput = Integer.toString(flowID) + "minThroughput";
                            node.addAttribute(attributeMinThroughput, minThroughput);
                        }else{
                            // TODO: maitain this case, maybe node can't load so much
                        }
                        break;
                    }
                }
            }
        }
    }
    private Map<Integer,Double> calculateFitness(Map<Integer,ApplicationRequirements> flowAR,
                                    Map<Integer,Double> flowDelays,
                                    Map<Integer,Double> flowThroughputs){
        Map<Integer,Double> flowFits = new HashMap<>();
        for(int flowID : flowAR.keySet()){
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