package it.unibo.deis.lia.ramp.core.internode.sdn.controllerClient;

import java.io.*;
import java.lang.reflect.Array;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import com.opencsv.CSVWriter;

import it.unibo.deis.lia.ramp.core.e2e.*;
import it.unibo.deis.lia.ramp.core.internode.sdn.advancedDataPlane.dataPlaneMessage.DataPlaneMessage;
import it.unibo.deis.lia.ramp.core.internode.sdn.advancedDataPlane.dataTypesManager.DataTypesManager;
import it.unibo.deis.lia.ramp.core.internode.sdn.advancedDataPlane.rulesManager.DataPlaneRulesManager;
import it.unibo.deis.lia.ramp.core.internode.sdn.controllerClient.controllerServiceDiscoveryPolicy.ControllerServiceDiscoveryPolicy;
import it.unibo.deis.lia.ramp.core.internode.sdn.controllerClient.controllerServiceDiscoveryPolicy.controllerServiceDiscoverer.ControllerServiceDiscoverer;
import it.unibo.deis.lia.ramp.core.internode.sdn.controllerClient.controllerServiceDiscoveryPolicy.controllerServiceDiscoverer.FirstAvailableControllerServiceDiscoverer;
import it.unibo.deis.lia.ramp.core.internode.sdn.controllerMessage.*;
import it.unibo.deis.lia.ramp.core.internode.sdn.controllerMessage.ControllerMessage;
import it.unibo.deis.lia.ramp.core.internode.sdn.controllerService.ControllerService;
import it.unibo.deis.lia.ramp.core.internode.sdn.osRoutingManager.OsRoutingManager;
import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.TopologyGraphSelector;
import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.graphUtils.GraphUtils;
import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.pathDescriptors.OsRoutingPathDescriptor;
import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.pathSelectors.BreadthFirstFlowPathSelector;
import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.pathSelectors.FewestIntersectionsFlowPathSelector;
import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.pathSelectors.MinimumNetworkLoadFlowPathSelector;
import it.unibo.deis.lia.ramp.core.internode.sdn.trafficEngineeringPolicy.TrafficEngineeringPolicy;
import it.unibo.deis.lia.ramp.core.internode.sdn.routingPolicy.RoutingPolicy;
import it.unibo.deis.lia.ramp.core.internode.sdn.dataPlaneForwarder.DataPlaneForwarder;
import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.pathDescriptors.PathDescriptor;
import it.unibo.deis.lia.ramp.core.internode.sdn.dataPlaneForwarder.trafficEngineeringForwarders.SinglePriorityForwarder;
import it.unibo.deis.lia.ramp.core.internode.sdn.dataPlaneForwarder.trafficEngineeringForwarders.MultipleFlowsSinglePriorityForwarder;
import it.unibo.deis.lia.ramp.core.internode.sdn.dataPlaneForwarder.trafficEngineeringForwarders.MultipleFlowsMultiplePrioritiesForwarder;
import it.unibo.deis.lia.ramp.core.internode.sdn.dataPlaneForwarder.routingForwarders.BestPathForwarder;
import it.unibo.deis.lia.ramp.core.internode.sdn.dataPlaneForwarder.routingForwarders.MulticastingForwarder;
import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.ApplicationRequirements;
import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.TrafficType;
import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.PathSelectionMetric;

import it.unibo.deis.lia.ramp.core.internode.*;
import it.unibo.deis.lia.ramp.service.management.ServiceDiscovery;
import it.unibo.deis.lia.ramp.service.management.ServiceManager;
import it.unibo.deis.lia.ramp.service.management.ServiceResponse;
import it.unibo.deis.lia.ramp.util.NetworkInterfaceStats;
import it.unibo.deis.lia.ramp.util.NodeStats;
import test.iotos.ClientMeasurer;
import test.iotos.ControllerMessageReady;
import test.iotos.GeneticPacketForward;
import test.iotos.messagetype.MeasureMessage;
import test.iotos.messagetype.timeDataType;

import org.graphstream.graph.Graph;

/**
 * @author Alessandro Dolci
 * @author Dmitrij David Padalino Montenero
 */
public class ControllerClient extends Thread implements ControllerClientInterface {

    /**
     * Control flow ID value, to be used for control communications between ControllerService and ControllerClients.
     */
    private static final int CONTROL_FLOW_ID = 0;

    /**
     * Default flow ID value, to be used by default type applications.
     */
    private static final int DEFAULT_FLOW_ID = 1;

    /**
     * Paths time-to-live value.
     */
    private static final int FLOW_PATHS_TTL = 60 * 1000;

    /**
     * ControllerClient instance.
     */
    private static ControllerClient controllerClient = null;

    /**
     * Socket used for the communication with the ControllerService.
     */
    private BoundReceiveSocket clientSocket = null;

    /**
     * Boolean value that reports if the ControllerClient is currently active.
     */
    private static boolean active;

    /**
     * This components sends periodically updates to the ControllerService
     * about the status of the current ControllerClient.
     */
    private UpdateManager updateManager;

    /**
     * At the moment this is just a base work to let the ControllerClient
     * to choose the ControllerService according to a specific strategy.
     * The only one currently implemented is FIRST_AVAILABLE {@link ControllerServiceDiscoveryPolicy}
     */
    private ControllerServiceDiscoveryPolicy controllerServiceDiscoveryPolicy;

    /**
     * ControllerService discover strategy.
     */
    private ControllerServiceDiscoverer controllerServiceDiscoverer;

    /**
     * This field keeps track the current TrafficEngineeringPolicy used
     * by all ControlAgent nodes. {@link TrafficEngineeringPolicy}
     */
    private TrafficEngineeringPolicy trafficEngineeringPolicy;

    /**
     * This component stores the DataPlaneForwarder object
     * according to the current TrafficEngineeringPolicy
     * imposed by the ControllerService.
     * flowDataPlaneForwarder and routingDataPlaneForwarder are orthogonal.
     */
    private DataPlaneForwarder flowDataPlaneForwarder;

    /**
     * This field keeps track the current routingPolicy used
     * by all ControlAgent nodes. {@link RoutingPolicy}
     */
    private RoutingPolicy routingPolicy;

    /**
     * This component stores the DataPlaneForwarder object
     * according to the current RoutingPolicy
     * imposed by the ControllerService.
     * routingDataPlaneForwarder and flowDataPlaneForwarder are orthogonal.
     */
    private DataPlaneForwarder routingDataPlaneForwarder;

    /**
     * This component maintains and manages the static routing
     * between ControllerClient nodes.
     */
    private OsRoutingManager osRoutingManager;

    /**
     * Data structure to hold paths imposed by the controller to the default flow
     * for the different destination nodes (destNodeId, path)
     * The key is the destinationNodeId
     * The value is a PathDescriptor
     */
    private Map<Integer, PathDescriptor> defaultFlowPaths;

    /**
     * Data structure to hold complete paths imposed by the controller for the existing flows (flowId, path)
     */
    private Map<Integer, PathDescriptor> flowPaths;

    /**
     * Data structure to hold start times of the existing flows (flowId, startTime)
     */
    private Map<Integer, Long> flowStartTimes;

    /**
     * Data structure to hold durations of the existing flows (flowId, duration)
     */
    private Map<Integer, Integer> flowDurations;

    /**
     * Data structure to hold priorities for the existing flows (flowId, priority)
     */
    private Map<Integer, Integer> flowPriorities;

    /**
     * Data structure to hold next hops for multicast forwarding associated to the existing flows (flowId, nextHops)
     */
    private Map<Integer, List<PathDescriptor>> flowMulticastNextHops;

    /**
     * Data structure to hold complete paths imposed by the controller for the existing os routing paths (routeId, path)
     */
    private Map<Integer, OsRoutingPathDescriptor> osRoutesPaths;

    /**
     * Data structure to hold start times of the existing os routing path (routeId, startTime)
     */
    private Map<Integer, Long> osRoutesStartTimes;

    /**
     * Data structure to hold durations of the existing os routing paths (routeId, duration)
     */
    private Map<Integer, Integer> osRoutesDurations;

    /**
     * Data structure to keep all the os routes available given a destination node id (destinationNodeId, List of route ids)
     */
    private Map<Integer, List<Integer>> routeIdsByDestination;

    /**
     * Data structure to keeps track if one of the nodes notified to the ControllerService has
     * specified any preferences in using the os level routing in place of the application level one
     * for a specific routeId.
     * <p>
     * True means that it should be used the os level routing
     */
    private Map<Integer, Boolean> osRoutesPriority;

    /**
     * This object will store the topology graph sent by the ControllerService when requested.
     * This graph could be used in case of fat ControllerClient, however the implementation of this ControllerClient
     * is a thin one so for the moment this functionality is used only for test purposes.
     */
    private Graph topologyGraph;

    /**
     * This String specifies the directory the ControllerClient must use in order to store files sent
     * by the ControllerService, for instance the dgs file for the topologyGraph or other files to be used
     * in future extensions of this class.
     */
    private String sdnClientDirectory = "./temp/sdnClient";

    /**
     * This component is responsible to maintain the data plane rules to apply when the ControllerClient
     * forwards certain types of traffic.
     */
    private DataPlaneRulesManager dataPlaneRulesManager;

    /**
     * This component is responsible to manage the data types available for advanced data plane rules to apply when the ControllerClient
     * forwards certain types of traffic.
     */
    private DataTypesManager dataTypesManager;

    /**
     * TODO Remove me
     */
    private PrintWriter printWriter;

    /**
     * TODO Remove me
     */
    private PrintWriter printWriterRules;

    /**
     * add u284976
     * clientMeasurer : Server of measure action
     * readyToTest : to check controller state 
     */
    private ClientMeasurer clientMeasurer;
    private long readyToTest;
    private boolean measureActive;

    private ControllerClient() {

        this.controllerServiceDiscoveryPolicy = ControllerServiceDiscoveryPolicy.FIRST_AVAILABLE;
        this.controllerServiceDiscoverer = new FirstAvailableControllerServiceDiscoverer(5, 5 * 1000, 1, 30 * 1000);
        ServiceResponse serviceResponse = this.controllerServiceDiscoverer.getControllerService();
        if (serviceResponse == null) {
            System.out.println("ControllerClient: controller service not found, cannot bind client socket");
        } else {
            try {
                /*
                 * Use the protocol specified by the controller
                 */
                this.clientSocket = E2EComm.bindPreReceive(serviceResponse.getProtocol());
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }

        active = true;
        this.updateManager = new UpdateManager();

        /**
         * adder @u284976
         * for measure the "one hop" link capability (delay and throughput)
         * create a ramp middleware service, and waiting by other client send request
         * 
         * readyToTest initial is 0(false)
         */
        clientMeasurer = ClientMeasurer.getInstance();
        readyToTest = 0;
        measureActive = false;

        /*
         * Make sure this manager is always instantiated before any other DataPlaneForwarder
         * so that its PacketForwardingListener is always the first one to be called.
         */
        this.dataPlaneRulesManager = DataPlaneRulesManager.getInstance();

        this.dataTypesManager = DataTypesManager.getInstance();

        this.trafficEngineeringPolicy = TrafficEngineeringPolicy.NO_FLOW_POLICY;
        this.flowDataPlaneForwarder = SinglePriorityForwarder.getInstance(null);

        this.routingPolicy = RoutingPolicy.REROUTING;
        this.routingDataPlaneForwarder = BestPathForwarder.getInstance();

        this.defaultFlowPaths = new ConcurrentHashMap<>();
        this.flowPaths = new ConcurrentHashMap<>();
        this.flowStartTimes = new ConcurrentHashMap<>();
        this.flowDurations = new ConcurrentHashMap<>();

        this.flowPriorities = new ConcurrentHashMap<>();

        this.flowMulticastNextHops = new ConcurrentHashMap<>();

        this.routeIdsByDestination = new ConcurrentHashMap<>();

        this.osRoutesPaths = new ConcurrentHashMap<>();
        this.osRoutesStartTimes = new ConcurrentHashMap<>();
        this.osRoutesDurations = new ConcurrentHashMap<>();
        this.osRoutesPriority = new ConcurrentHashMap<>();

        try {
            this.osRoutingManager = OsRoutingManager.getInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.topologyGraph = null;

        File dir = new File(sdnClientDirectory);
        if (!dir.exists()) {
            dir.mkdir();
        }

        /*
         * TODO Remove me
         */
        File outputFile = new File(sdnClientDirectory + "/" + "controllerClientLog" + Dispatcher.getLocalRampId() + ".txt");

        if (outputFile.exists()) {
            outputFile.delete();
        }
        try {
            outputFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            printWriter = new PrintWriter(new FileWriter(outputFile, true));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        printWriter.println("ControllerClient NodeId=" + Dispatcher.getLocalRampId() + " TEST LOG");
        printWriter.flush();

        /*
         * TODO Remove me
         */
        File outputFileRule = new File(sdnClientDirectory + "/" + "controllerClientLogRule" + Dispatcher.getLocalRampId() + ".txt");

        if (outputFileRule.exists()) {
            outputFileRule.delete();
        }
        try {
            outputFileRule.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            printWriterRules = new PrintWriter(new FileWriter(outputFileRule, true));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        printWriterRules.println("ControllerClient NodeId=" + Dispatcher.getLocalRampId() + " TEST LOG");
        printWriterRules.flush();

    }

    public synchronized static ControllerClient getInstance() {
        if (controllerClient == null) {
            try {
                controllerClient = new ControllerClient();
            } catch (Exception e) {
                e.printStackTrace();
            }
            controllerClient.start();
        }
        System.out.println("ControllerClient START");

        return controllerClient;
    }

    public synchronized static boolean isActive() {
        return active;
    }

    public void stopClient() {
        System.out.println("ControllerClient STOP");
        leaveService();
        active = false;
        try {
            this.clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (this.flowDataPlaneForwarder != null) {
            this.flowDataPlaneForwarder.deactivate();
            this.flowDataPlaneForwarder = null;
        }
        if (this.routingDataPlaneForwarder != null) {
            this.routingDataPlaneForwarder.deactivate();
            this.routingDataPlaneForwarder = null;
        }

        if (this.osRoutingManager != null) {
            this.osRoutingManager.deactivate();
            this.osRoutingManager = null;
        }

        if (!ControllerService.isActive()) {
            this.dataPlaneRulesManager.deactivate();
            this.dataPlaneRulesManager = null;
            this.dataTypesManager.deactivate();
            this.dataTypesManager = null;
        }

        this.updateManager.stopUpdateManager();
        this.controllerServiceDiscoverer.stopControllerServiceDiscoverer();
        this.clientMeasurer.stopMeasure();
        
        controllerClient = null;
    }

    public ServiceResponse getControllerService() {
        return this.controllerServiceDiscoverer.getControllerService();
    }

    /**
     * TODO Remove me
     */
    public synchronized void log(String message) {
        this.printWriter.println(message + ",");
        this.printWriter.flush();
    }

    /**
     * TODO Remove me
     */
    public synchronized void logRule(String message) {
        this.printWriterRules.println(message + ",");
        this.printWriterRules.flush();
    }

    public int getFlowId(ApplicationRequirements applicationRequirements, int destNodeId) {
        return getFlowId(applicationRequirements, new int[]{destNodeId}, null);
    }

    public int getFlowId(ApplicationRequirements applicationRequirements, int[] destNodeIds, int[] destPorts) {
        return getFlowId(applicationRequirements, destNodeIds, destPorts, null);
    }

    public int getFlowId(ApplicationRequirements applicationRequirements, int[] destNodeIds, int[] destPorts, PathSelectionMetric pathSelectionMetric) {
        int flowId;
        PathDescriptor flowPathDescriptor;

        if (applicationRequirements.getTrafficType() == TrafficType.DEFAULT) {
            flowId = DEFAULT_FLOW_ID;
        } else {
            flowId = ThreadLocalRandom.current().nextInt();
            while (flowId == GenericPacket.UNUSED_FIELD || flowId == CONTROL_FLOW_ID || flowId == DEFAULT_FLOW_ID || this.flowStartTimes.containsKey(flowId))
                flowId = ThreadLocalRandom.current().nextInt();
            if (this.routingPolicy == RoutingPolicy.REROUTING) {
                flowPathDescriptor = sendNewPathRequest(destNodeIds, applicationRequirements, pathSelectionMetric, flowId);
                /*
                 * Save the new path in the ControllerClient database.
                 */
                if (flowPathDescriptor != null) {
                    long now = System.currentTimeMillis();
                    flowPathDescriptor.setCreationTime(now);
                    this.flowPaths.put(flowId, flowPathDescriptor);
                    this.flowStartTimes.put(flowId, now);
                    this.flowDurations.put(flowId, applicationRequirements.getDuration());
                } else {
                    return -1;
                }
            } else if (this.routingPolicy == RoutingPolicy.MULTICASTING) {
                sendNewMulticastRequest(destNodeIds, destPorts, applicationRequirements, pathSelectionMetric, flowId);
            }
            if (this.trafficEngineeringPolicy == TrafficEngineeringPolicy.SINGLE_FLOW || this.trafficEngineeringPolicy == TrafficEngineeringPolicy.QUEUES || this.trafficEngineeringPolicy == TrafficEngineeringPolicy.TRAFFIC_SHAPING) {
                sendNewPriorityValueRequest(applicationRequirements, flowId);
            }
        }
        return flowId;
    }

    /**
     * This functionality is not currently working, it has been added only for testing purposes to simulate
     * the scenario of a fat ControllerClient able to compute a new path by itself without the ControllerService
     * intervention. The only interaction that the ControllerClient has with the ControllerService is about
     * the retrieval of the most updated topology graph.
     */
    public PathDescriptor computeUnicastPathLocally(int clientNodeId, int destinationNodeId, PathSelectionMetric pathSelectionMetric) {
        getTopologyGraph();

        if (this.topologyGraph == null) {
            return null;
        }

        PathDescriptor path = null;
        TopologyGraphSelector pathSelector = null;
        if (pathSelectionMetric != null) {
            if (pathSelectionMetric == PathSelectionMetric.BREADTH_FIRST)
                pathSelector = new BreadthFirstFlowPathSelector(topologyGraph);
            else if (pathSelectionMetric == PathSelectionMetric.FEWEST_INTERSECTIONS)
                pathSelector = new FewestIntersectionsFlowPathSelector(topologyGraph);
            else if (pathSelectionMetric == PathSelectionMetric.MINIMUM_NETWORK_LOAD)
                pathSelector = new MinimumNetworkLoadFlowPathSelector(topologyGraph);

            path = pathSelector.selectPath(clientNodeId, destinationNodeId, null, flowPaths);
        }

        return path;
    }

    public List<Integer> getAvailableRouteIds(int destinationNodeId) {
        if (this.routeIdsByDestination.containsKey(destinationNodeId)) {
            return this.routeIdsByDestination.get(destinationNodeId);
        }
        return new ArrayList<>();
    }

    public int getRouteId(int destNodeId, int destPort, ApplicationRequirements applicationRequirements, PathSelectionMetric pathSelectionMetric) {
        return getRouteId(new int[]{destNodeId}, new int[]{destPort}, applicationRequirements, pathSelectionMetric);
    }

    public int getRouteId(int[] destNodeIds, int[] destPorts, ApplicationRequirements applicationRequirements, PathSelectionMetric pathSelectionMetric) {
        return sendOSLevelRoutingRequest(destNodeIds, destPorts, applicationRequirements, pathSelectionMetric);
    }

    public String[] getFlowPath(int destNodeId, int flowId) {
        System.out.println("ControllerClient: received request for new path for flow " + flowId);
        String[] flowPath = null;
        PathDescriptor flowPathDescriptor;

        if (flowId == DEFAULT_FLOW_ID)
            flowPathDescriptor = this.defaultFlowPaths.get(destNodeId);
        else
            flowPathDescriptor = this.flowPaths.get(flowId);

        /*
         * If a path exists in the maps for the specified flowId, check TTL and, if valid, return it
         */
        if (flowPathDescriptor != null) {
            long elapsed = System.currentTimeMillis() - flowPathDescriptor.getCreationTime();
            /*
             * If the path is valid, return it
             */
            if (elapsed < FLOW_PATHS_TTL) {
                flowPath = flowPathDescriptor.getPath();
                if (flowId != DEFAULT_FLOW_ID)
                    System.out.println("ControllerClient: entry found for flow " + flowId + ", returning the new flow path");
                else
                    System.out.println("ControllerClient: entry found for default flow, returning the new flow path");
            } else {
                /*
                 * If the path is not valid and the flowId is not the default one, send a request for a new one to the controller
                 */
                if (flowId != DEFAULT_FLOW_ID) {
                    System.out.println("ControllerClient: entry found for flow " + flowId + ", but its validity has expired, sending request to the controller");

                    /*
                     * The path request is not the first one for this application,
                     * so applicationRequirements is null
                     */
                    flowPathDescriptor = sendNewPathRequest(new int[]{destNodeId}, null, null, flowId);
                    flowPath = flowPathDescriptor.getPath();
                } else
                    System.out.println("ControllerClient: entry found for default flow, but its validity has expired, returning null");
            }
        }
        /*
         * If a path doesn't exist in the map for the specified flowId, return null
         */
        else {
            if (flowId != DEFAULT_FLOW_ID)
                System.out.println("ControllerClient: no entry found for flow " + flowId + ", returning null");
            else
                System.out.println("ControllerClient: no entry found for default flow, returning null");
        }
        return flowPath;
    }

    public TrafficEngineeringPolicy getTrafficEngineeringPolicy() {
        return this.trafficEngineeringPolicy;
    }

    public RoutingPolicy getRoutingPolicy() {
        return this.routingPolicy;
    }

    public ConcurrentHashMap<Integer, PathDescriptor> getDefaultFlowPath() {
        return (ConcurrentHashMap<Integer, PathDescriptor>) this.defaultFlowPaths;
    }

    public ConcurrentHashMap<Integer, PathDescriptor> getFlowPaths() {
        return (ConcurrentHashMap<Integer, PathDescriptor>) this.flowPaths;
    }

    public ConcurrentHashMap<Integer, Integer> getFlowPriorities() {
        return (ConcurrentHashMap<Integer, Integer>) this.flowPriorities;
    }

    public String getRouteIdSourceIpAddress(int routeId) {
        String result = null;
        if (this.osRoutesPaths.containsKey(routeId)) {
            result = this.osRoutesPaths.get(routeId).getSourceIp();
        }
        return result;
    }

    public String getRouteIdDestinationIpAddress(int routeId) {
        String result = null;
        if (this.osRoutesPaths.containsKey(routeId)) {
            result = this.osRoutesPaths.get(routeId).getDestinationIP();
        }
        return result;
    }

    public boolean getRouteIdPriority(int routeId) {
        boolean result = false;
        if(this.osRoutesPriority.containsKey(routeId)) {
            result = this.osRoutesPriority.get(routeId);
        }
        return result;
    }

    public Set<String> getDataTypesAvailable() {
        return this.dataTypesManager.getAvailableDataTypes();
    }

    public PathDescriptor sendNewPathRequest(int destNodeId, PathSelectionMetric pathSelectionMetric) {
        return sendNewPathRequest(new int[]{destNodeId}, null, pathSelectionMetric, GenericPacket.UNUSED_FIELD);
    }

    private PathDescriptor sendNewPathRequest(int[] destNodeIds, ApplicationRequirements applicationRequirements, PathSelectionMetric pathSelectionMetric, int flowId) {
        /*
         * TODO Remove me
         */
        log("sendNewPathRequest");
        LocalDateTime localDateTime;
        String timestamp;
        int packetSize;
        int payloadSize;

        PathDescriptor newPath = null;
        BoundReceiveSocket responseSocket;

        /*
         * Controller service has to be found before sending any message
         */
        ServiceResponse serviceResponse = this.controllerServiceDiscoverer.getControllerService();
        if (serviceResponse == null) {
            System.out.println("ControllerClient: controller service not found, cannot bind client socket");
        } else {
            try {
                /*
                 * Use the protocol specified by the controller
                 */
                responseSocket = E2EComm.bindPreReceive(serviceResponse.getProtocol());
                /*
                 * Send a path request to the controller for a certain flowId,
                 * currentDest is also necessary for the search on the controller
                 * side (it can be chosen as the path to be used), as well as destNodeId.
                 */
                ControllerMessageRequest requestMessage = new ControllerMessageRequest(MessageType.PATH_REQUEST, responseSocket.getLocalPort(), destNodeIds, null, applicationRequirements, pathSelectionMetric, flowId);

                E2EComm.sendUnicast(serviceResponse.getServerDest(), serviceResponse.getServerNodeId(), serviceResponse.getServerPort(), serviceResponse.getProtocol(), CONTROL_FLOW_ID, E2EComm.serialize(requestMessage));

                /*
                 * TODO Remove me
                 */
                localDateTime = LocalDateTime.now();
                timestamp = localDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
                log("PATH_REQUEST sent at: " + timestamp);

                System.out.println("ControllerClient: request for a new path for flow " + flowId + " sent to the controller");

                /*
                 * Wait for ControllerService response
                 */
                GenericPacket gp = null;
                try {
                    gp = E2EComm.receive(responseSocket);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                /*
                 * TODO Remove me
                 */
                packetSize = E2EComm.objectSizePacket(gp);
                localDateTime = LocalDateTime.now();
                timestamp = localDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));

                if (gp instanceof UnicastPacket) {
                    UnicastPacket up = (UnicastPacket) gp;
                    Object payload = null;
                    try {
                        payload = E2EComm.deserialize(up.getBytePayload());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    /*
                     * TODO Remove me
                     */
                    payloadSize = up.getBytePayload().length;

                    if (payload instanceof ControllerMessage) {
                        ControllerMessage controllerMessage = (ControllerMessage) payload;
                        ControllerMessageResponse responseMessage;

                        switch (controllerMessage.getMessageType()) {
                            case PATH_RESPONSE:
                                /*
                                 * TODO Remove me
                                 */
                                log("PATH_RESPONSE received at: " + timestamp + ", responseSize: " + packetSize + ", payloadSize: " + payloadSize);

                                responseMessage = (ControllerMessageResponse) controllerMessage;
                                /*
                                 * Set the received path creation time and add it to the known flow paths.
                                 */
                                newPath = responseMessage.getNewPaths().get(0);
                                if (newPath != null) {
                                    if (flowId != GenericPacket.UNUSED_FIELD) {
                                        System.out.println("ControllerClient: response with a new path for flow " + flowId + " received from the controller");
                                    } else {
                                        System.out.println("ControllerClient: response with a new path received from the controller");
                                    }
                                } else
                                    System.out.println("ControllerClient: null path received from the controller for flow " + flowId);
                                break;
                            default:
                                break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return newPath;
    }

    @Override
    public PathDescriptor sendFixPathRequest(int sourceNodeId, int destNodeId, int flowId) {
        /*
         * TODO Remove me
         */
        log("sendFixPathRequest");
        LocalDateTime localDateTime;
        String timestamp;
        int packetSize;
        int payloadSize;

        PathDescriptor newPath = null;
        BoundReceiveSocket responseSocket = null;

        /*
         * Controller service has to be found before sending any message
         */
        ServiceResponse serviceResponse = this.controllerServiceDiscoverer.getControllerService();
        if (serviceResponse == null) {
            System.out.println("ControllerClient: controller service not found, cannot bind client socket");
        } else {
            try {
                /*
                 * Use the protocol specified by the controller
                 */
                responseSocket = E2EComm.bindPreReceive(serviceResponse.getProtocol());
                /*
                 * Send a path request to the controller for a certain flowId,
                 * currentDest is also necessary for the search on the controller
                 * side (it can be chosen as the path to be used), as well as destNodeId.
                 */
                ControllerMessageRequest requestMessage = new ControllerMessageRequest(MessageType.FIX_PATH_REQUEST, responseSocket.getLocalPort(), new int[]{destNodeId}, null, null, null, flowId);
                requestMessage.setSourceNodeId(sourceNodeId);

                E2EComm.sendUnicast(serviceResponse.getServerDest(), serviceResponse.getServerNodeId(), serviceResponse.getServerPort(), serviceResponse.getProtocol(), CONTROL_FLOW_ID, E2EComm.serialize(requestMessage));

                /*
                 * TODO Remove me
                 */
                localDateTime = LocalDateTime.now();
                timestamp = localDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
                log("FIX_PATH_REQUEST sent at: " + timestamp);

                System.out.println("ControllerClient: request for fixing an existing path for flow " + flowId + " sent to the controller");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        GenericPacket gp = null;
        try {
            gp = E2EComm.receive(responseSocket);
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*
         * TODO Remove me
         */
        packetSize = E2EComm.objectSizePacket(gp);
        localDateTime = LocalDateTime.now();
        timestamp = localDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));

        if (gp instanceof UnicastPacket) {
            UnicastPacket up = (UnicastPacket) gp;
            Object payload = null;
            try {
                payload = E2EComm.deserialize(up.getBytePayload());
            } catch (Exception e) {
                e.printStackTrace();
            }

            /*
             * TODO Remove me
             */
            payloadSize = up.getBytePayload().length;

            if (payload instanceof ControllerMessage) {
                ControllerMessage controllerMessage = (ControllerMessage) payload;
                ControllerMessageResponse responseMessage;

                switch (controllerMessage.getMessageType()) {
                    case FIX_PATH_RESPONSE:
                        /*
                         * TODO Remove me
                         */
                        log("FIX_PATH_RESPONSE received at: " + timestamp + ", responseSize: " + packetSize + ", payloadSize: " + payloadSize);

                        responseMessage = (ControllerMessageResponse) controllerMessage;
                        /*
                         * Set the received path creation time and add it to the known flow paths.
                         */
                        newPath = responseMessage.getNewPaths().get(0);
                        log("Path: " + Arrays.toString(newPath.getPath()));
                        if (newPath != null) {
                            System.out.println("ControllerClient: response with a fixed path for flow " + flowId + " received from the controller");
                        } else
                            System.out.println("ControllerClient: null fixed path received from the controller for flow " + flowId);
                        break;
                    default:
                        break;
                }
            }
        }
        if (newPath != null)
            return newPath;
        else
            return null;
    }

    public int getFlowPriority(int flowId) {
        Integer flowPriority = this.flowPriorities.get(flowId);
        if (flowPriority != null)
            return flowPriority;
        else
            return -1;
    }

    private int sendNewPriorityValueRequest(ApplicationRequirements applicationRequirements, int flowId) {
        int newPriorityValue = -1;
        BoundReceiveSocket responseSocket = null;
        /*
         * Controller service has to be found before sending any message
         */
        ServiceResponse serviceResponse = this.controllerServiceDiscoverer.getControllerService();
        if (serviceResponse == null) {
            System.out.println("ControllerClient: controller service not found, cannot bind client socket");
        } else {
            try {
                /*
                 * Use the protocol specified by the controller
                 */
                responseSocket = E2EComm.bindPreReceive(serviceResponse.getProtocol());
                /*
                 * Send a priority value request to the controller for a certain flowId
                 */
                ControllerMessageRequest requestMessage = new ControllerMessageRequest(MessageType.PRIORITY_VALUE_REQUEST, responseSocket.getLocalPort(), new int[0], new int[0], applicationRequirements, null, flowId);
                E2EComm.sendUnicast(serviceResponse.getServerDest(), serviceResponse.getServerNodeId(), serviceResponse.getServerPort(), serviceResponse.getProtocol(), CONTROL_FLOW_ID, E2EComm.serialize(requestMessage));
                System.out.println("ControllerClient: request for a new priority value for flow " + flowId + " sent to the controller");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        GenericPacket gp = null;
        try {
            gp = E2EComm.receive(responseSocket);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (gp instanceof UnicastPacket) {
            UnicastPacket up = (UnicastPacket) gp;
            Object payload = null;
            try {
                payload = E2EComm.deserialize(up.getBytePayload());
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (payload instanceof ControllerMessage) {
                ControllerMessageUpdate responseMessage = (ControllerMessageUpdate) payload;
                switch (responseMessage.getMessageType()) {
                    case FLOW_PRIORITIES_UPDATE:
                        newPriorityValue = responseMessage.getFlowPriorities().get(flowId);
                        this.flowPriorities = responseMessage.getFlowPriorities();
                        this.flowStartTimes.put(flowId, System.currentTimeMillis());
                        this.flowDurations.put(flowId, applicationRequirements.getDuration());
                        System.out.println("ControllerClient: response with a new priority value for flow " + flowId + " received and successfully applied");
                        break;
                    default:
                        break;
                }
            }
        }
        return newPriorityValue;
    }

    public List<PathDescriptor> getFlowMulticastNextHops(int flowId) {
        return this.flowMulticastNextHops.get(flowId);
    }

    public List<PathDescriptor> sendNewMulticastRequest(int[] destNodeIds, int[] destPorts, ApplicationRequirements applicationRequirements, PathSelectionMetric pathSelectionMetric, int flowId) {
        List<PathDescriptor> nextHops = new ArrayList<PathDescriptor>();
        BoundReceiveSocket responseSocket = null;
        /*
         * Controller service has to be found before sending any message
         */
        ServiceResponse serviceResponse = this.controllerServiceDiscoverer.getControllerService();
        if (serviceResponse == null) {
            System.out.println("ControllerClient: controller service not found, cannot bind client socket");
        } else {
            try {
                responseSocket = E2EComm.bindPreReceive(serviceResponse.getProtocol());
                ControllerMessageRequest requestMessage = new ControllerMessageRequest(MessageType.MULTICAST_REQUEST, responseSocket.getLocalPort(), destNodeIds, destPorts, applicationRequirements, pathSelectionMetric, flowId);
                E2EComm.sendUnicast(serviceResponse.getServerDest(), serviceResponse.getServerNodeId(), serviceResponse.getServerPort(), serviceResponse.getProtocol(), CONTROL_FLOW_ID, E2EComm.serialize(requestMessage));
                System.out.println("ControllerClient: request for a new multicast communication for flow " + flowId + " sent to the controller");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        GenericPacket gp = null;
        try {
            gp = E2EComm.receive(responseSocket);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (gp instanceof UnicastPacket) {
            UnicastPacket up = (UnicastPacket) gp;
            Object payload = null;
            try {
                payload = E2EComm.deserialize(up.getBytePayload());
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (payload instanceof ControllerMessage) {
                ControllerMessageResponse responseMessage = (ControllerMessageResponse) payload;
                switch (responseMessage.getMessageType()) {
                    case MULTICAST_CONTROL:
                        nextHops = responseMessage.getNewPaths();
                        this.flowMulticastNextHops.put(flowId, responseMessage.getNewPaths());
                        this.flowStartTimes.put(flowId, System.currentTimeMillis());
                        this.flowDurations.put(flowId, applicationRequirements.getDuration());
                        System.out.println("ControllerClient: response with a list of the next hops for multicast communication for flow " + flowId + " received and successfully applied");
                        break;
                    default:
                        break;
                }
            }
        }
        return nextHops;
    }

    private int sendOSLevelRoutingRequest(int[] destNodeIds, int[] destPorts, ApplicationRequirements applicationRequirements, PathSelectionMetric pathSelectionMetric) {
        /*
         * TODO Remove me
         */
        log("sendOSLevelRoutingRequest");
        LocalDateTime localDateTime;
        String timestamp;
        int packetSize;
        int payloadSize;

        BoundReceiveSocket responseSocket = null;
        int routeId = -1;
        OsRoutingPathDescriptor osRoutingPathDescriptor = null;

        /*
         * Controller service has to be found before sending any message
         */
        ServiceResponse serviceResponse = this.controllerServiceDiscoverer.getControllerService();
        if (serviceResponse == null) {
            System.out.println("ControllerClient: controller service not found, cannot bind client socket");
        } else {
            try {
                responseSocket = E2EComm.bindPreReceive(serviceResponse.getProtocol());
                ControllerMessageRequest requestMessage = new ControllerMessageRequest(MessageType.OS_ROUTING_REQUEST, responseSocket.getLocalPort(), destNodeIds, destPorts, applicationRequirements, pathSelectionMetric, ControllerMessage.UNUSED_FIELD);
                E2EComm.sendUnicast(serviceResponse.getServerDest(), serviceResponse.getServerNodeId(), serviceResponse.getServerPort(), serviceResponse.getProtocol(), CONTROL_FLOW_ID, E2EComm.serialize(requestMessage));
                System.out.println("ControllerClient: request for a new OS level communication sent to the controller");
            } catch (Exception e) {
                e.printStackTrace();
            }
            /*
             * TODO Remove me
             */
            localDateTime = LocalDateTime.now();
            timestamp = localDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            log("OS_ROUTING_REQUEST sent at: " + timestamp);
        }

        GenericPacket gp;
        try {
            gp = E2EComm.receive(responseSocket);
        } catch (Exception e) {
            System.out.println("ControllerClient: client socket timeout");
            return routeId;
        }

        /*
         * TODO Remove me
         */
        packetSize = E2EComm.objectSizePacket(gp);
        localDateTime = LocalDateTime.now();
        timestamp = localDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));

        if (gp instanceof UnicastPacket) {
            UnicastPacket up = (UnicastPacket) gp;
            Object payload = null;
            try {
                payload = E2EComm.deserialize(up.getBytePayload());
            } catch (Exception e) {
                e.printStackTrace();
            }

            /*
             * TODO Remove me
             */
            payloadSize = up.getBytePayload().length;

            if (payload instanceof ControllerMessageResponse) {
                ControllerMessageResponse responseMessage = (ControllerMessageResponse) payload;
                if (responseMessage.getMessageType() == MessageType.OS_ROUTING_PULL_RESPONSE) {
                    routeId = responseMessage.getRouteId();
                    osRoutingPathDescriptor = responseMessage.getOsRoutingPath();

                    /*
                     * TODO Remove me
                     */
                    log("OS_ROUTING_PULL_RESPONSE received at: " + timestamp + ", responseSize: " + packetSize + ", payloadSize: " + payloadSize);
                }
            }
        }

        if (routeId != -1) {
            int destinationNodeId = osRoutingPathDescriptor.getDestinationNodeId();
            if (!this.routeIdsByDestination.containsKey(destinationNodeId)) {
                List<Integer> routeIds = new ArrayList<>();
                routeIds.add(routeId);
                this.routeIdsByDestination.put(destinationNodeId, routeIds);
            } else {
                this.routeIdsByDestination.get(destinationNodeId).add(routeId);
            }
            this.osRoutesPaths.put(routeId, osRoutingPathDescriptor);
            this.osRoutesStartTimes.put(routeId, System.currentTimeMillis());
            this.osRoutesDurations.put(routeId, applicationRequirements.getDuration());
            this.osRoutesPriority.put(routeId, false);
        }

        return routeId;
    }

    public int sendOsRoutingUpdatePriorityRequest(int routeId, boolean switchToOsRouting) {
        /*
         * TODO Remove me
         */
        log("sendOsRoutingUpdatePriorityRequest");
        LocalDateTime localDateTime;
        String timestamp;
        int packetSize;
        int payloadSize;

        int result = -1;

        /*
         * Check if this node is one of the peers and
         * in case set the desired status.
         */
        if (this.osRoutesPaths.containsKey(routeId)) {
            int currentNodeId = Dispatcher.getLocalRampId();
            OsRoutingPathDescriptor osRoutingPathDescriptor = this.osRoutesPaths.get(routeId);
            if (osRoutingPathDescriptor.getSourceNodeId() == currentNodeId || osRoutingPathDescriptor.getDestinationNodeId() == currentNodeId) {
                this.osRoutesPriority.replace(routeId, switchToOsRouting);
            }
        }

        /*
         * Controller service has to be found before sending any message
         */
        ServiceResponse serviceResponse = this.controllerServiceDiscoverer.getControllerService();
        if (serviceResponse == null) {
            System.out.println("ControllerClient: controller service not found, cannot bind client socket");
        } else {
            try {
                BoundReceiveSocket responseSocket = E2EComm.bindPreReceive(serviceResponse.getProtocol());
                ControllerMessageRequest requestMessage = new ControllerMessageRequest(MessageType.OS_ROUTING_UPDATE_PRIORITY_REQUEST, responseSocket.getLocalPort(), routeId, switchToOsRouting);
                E2EComm.sendUnicast(serviceResponse.getServerDest(), serviceResponse.getServerNodeId(), serviceResponse.getServerPort(), serviceResponse.getProtocol(), CONTROL_FLOW_ID, E2EComm.serialize(requestMessage));
                System.out.println("ControllerClient: request for switch to OS level routing for routeId " + routeId + " sent to the controller");

                /*
                 * TODO Remove me
                 */
                localDateTime = LocalDateTime.now();
                timestamp = localDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
                log("OS_ROUTING_UPDATE_PRIORITY_REQUEST sent at: " + timestamp);

                /*
                 * Wait for ControllerService response
                 */
                GenericPacket gp = E2EComm.receive(responseSocket);

                /*
                 * TODO Remove me
                 */
                packetSize = E2EComm.objectSizePacket(gp);
                localDateTime = LocalDateTime.now();
                timestamp = localDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));

                if (gp instanceof UnicastPacket) {
                    UnicastPacket up = (UnicastPacket) gp;
                    Object payload = E2EComm.deserialize(up.getBytePayload());

                    /*
                     * TODO Remove me
                     */
                    payloadSize = up.getBytePayload().length;

                    if (payload instanceof ControllerMessageResponse) {
                        ControllerMessageResponse responseMessage = (ControllerMessageResponse) payload;
                        if (responseMessage.getMessageType() == MessageType.OS_ROUTING_UPDATE_PRIORITY_RESPONSE) {
                            int receivedRouteId = responseMessage.getRouteId();
                            /*
                             * TODO Remove me
                             */
                            log("OS_ROUTING_UPDATE_PRIORITY_RESPONSE received at: " + timestamp + ", responseSize: " + packetSize + ", payloadSize: " + payloadSize);

                            if (receivedRouteId == routeId) {
                                result = 0;
                                System.out.println("ControllerClient: priority of routeId " + routeId + " updated");
                            } else {
                                System.out.println("ControllerClient: priority of routeId " + routeId + " not updated");
                            }
                        } else {
                            System.out.println("ControllerClient: priority of routeId " + routeId + " not updated");
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("ControllerClient: priority of routeId " + routeId + " not updated");
                e.printStackTrace();
            }
        }

        return result;
    }

    public void getTopologyGraph() {
        try {
            Files.deleteIfExists(Paths.get(sdnClientDirectory + "/" + "topologyGraph.dgs"));
        } catch (NoSuchFileException e) {
            System.out.println("ControllerClient: There is no topology graph file to delete");
        } catch (DirectoryNotEmptyException e) {
            System.out.println("ControllerClient: Directory is not empty.");
        } catch (IOException e) {
            System.out.println("ControllerClient: Invalid permissions.");
        }

        System.out.println("ControllerClient: old topology graph file successfully deleted.");

        BoundReceiveSocket responseSocket = null;
        /*
         * Controller service has to be found before sending any message
         */
        ServiceResponse serviceResponse = this.controllerServiceDiscoverer.getControllerService();
        if (serviceResponse == null) {
            System.out.println("ControllerClient: controller service not found, cannot bind client socket");
        } else {
            try {
                /*
                 * Use the protocol specified by the controller
                 */
                responseSocket = E2EComm.bindPreReceive(serviceResponse.getProtocol());
                /*
                 * Send a priority value request to the controller for a certain flowId
                 */
                ControllerMessageRequest requestMessage = new ControllerMessageRequest(MessageType.TOPOLOGY_GRAPH_REQUEST, responseSocket.getLocalPort());

                E2EComm.sendUnicast(serviceResponse.getServerDest(), serviceResponse.getServerNodeId(), serviceResponse.getServerPort(), serviceResponse.getProtocol(), CONTROL_FLOW_ID, E2EComm.serialize(requestMessage));

                System.out.println("ControllerClient: topology graph request sent to the controller");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(sdnClientDirectory + "/" + "topologyGraph.dgs");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            UnicastHeader uh = (UnicastHeader) E2EComm.receive(
                    responseSocket,
                    0,
                    fos
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            responseSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.topologyGraph = GraphUtils.loadTopologyGraphFromDGSFile(sdnClientDirectory + "/topologyGraph.dgs");
    }

    private void joinService() {
        /*
         * Get network stats for the interfaces associated to the addresses of the node
         */
        Map<String, NodeStats> networkInterfaceStats = new HashMap<String, NodeStats>();
        try {
            for (String address : Dispatcher.getLocalNetworkAddresses()) {
                NetworkInterface addressNetworkInterface = NetworkInterface.getByInetAddress(InetAddress.getByName(address));
                NodeStats addressNetworkInterfaceStats = new NetworkInterfaceStats(Dispatcher.getLocalRampId(), addressNetworkInterface);
                networkInterfaceStats.put(address, addressNetworkInterfaceStats);
            }
        } catch (SocketException e1) {
            e1.printStackTrace();
        } catch (UnknownHostException e1) {
            e1.printStackTrace();
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        ControllerMessage joinMessage = new ControllerMessage(MessageType.JOIN_SERVICE, this.clientSocket.getLocalPort(), networkInterfaceStats);
        /*
         * Controller service has to be found before sending any message
         */
        ServiceResponse serviceResponse = this.controllerServiceDiscoverer.getControllerService();
        if (serviceResponse == null) {
            System.out.println("ControllerClient: controller service not found, cannot bind client socket");
        } else {
            try {
                E2EComm.sendUnicast(serviceResponse.getServerDest(), serviceResponse.getServerNodeId(), serviceResponse.getServerPort(), serviceResponse.getProtocol(), CONTROL_FLOW_ID, E2EComm.serialize(joinMessage));
                System.out.println("ControllerClient: join request sent to the controller");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void leaveService() {
        ControllerMessage leaveMessage = new ControllerMessage(MessageType.LEAVE_SERVICE);
        /*
         * Controller service has to be found before sending any message
         */
        ServiceResponse serviceResponse = this.controllerServiceDiscoverer.getControllerService();
        if (serviceResponse == null) {
            System.out.println("ControllerClient: controller service not found, cannot bind client socket");
        } else {
            try {
                E2EComm.sendUnicast(serviceResponse.getServerDest(), serviceResponse.getServerNodeId(), serviceResponse.getServerPort(), serviceResponse.getProtocol(), CONTROL_FLOW_ID, E2EComm.serialize(leaveMessage));
                System.out.println("ControllerClient: leave request sent to the controller");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        System.out.println("ControllerClient START");
        joinService();
        this.updateManager.start();
        while (active) {
            try {
                /*
                 * Receive packets from the controller and pass them to newly created handlers
                 */
                GenericPacket gp = E2EComm.receive(this.clientSocket, 5 * 1000);
                new PacketHandler(gp).start();
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }
        try {
            this.clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        controllerClient = null;
        System.out.println("ControllerClient FINISHED");
    }

    /**
     * add u284976
     * get ready to test
     */
    public long getReadyToTest(){
        return readyToTest;
    }
    /**
     * add u284976
     * to notice controller this client are complete
     */
    public void sendCompleteToController(){
        ServiceResponse service = controllerServiceDiscoverer.getControllerService();
        if(service == null){

        }

        ControllerMessageReady updateMessage = new ControllerMessageReady(MessageType.READY_TO_TEST,0);

        try {
            E2EComm.sendUnicast(
                service.getServerDest(),
                service.getServerNodeId(),
                service.getServerPort(),
                service.getProtocol(),
                CONTROL_FLOW_ID,
                E2EComm.serialize(updateMessage)
            );
        } catch (Exception e) {
            //TODO: handle exception
        }
        
    }
    /**
     * add u284976
     * to enable measure
     */
    public void enableMeasure(){
        measureActive = true;
    }
    public void disableMeasure(){
        measureActive = false;
    }
    public boolean getMeasureActive(){
        return measureActive;
    }

    private class PacketHandler extends Thread {

        private GenericPacket gp;

        PacketHandler(GenericPacket gp) {
            this.gp = gp;
        }

        @Override
        public void run() {
            /*
             * TODO Remove me
             */
            int packetSize = E2EComm.objectSizePacket(gp);
            LocalDateTime localDateTime = LocalDateTime.now();
            String timestamp = localDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            int payloadSize = -1;

            if (this.gp instanceof UnicastPacket) {
                UnicastPacket up = (UnicastPacket) this.gp;
                Object payload = null;

                try {
                    payload = E2EComm.deserialize(up.getBytePayload());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                /*
                 * TODO Remove me
                 */
                payloadSize = up.getBytePayload().length;

                if (payload instanceof ControllerMessage) {
                    ControllerMessage controllerMessage = (ControllerMessage) payload;

                    switch (controllerMessage.getMessageType()) {
                        case TRAFFIC_ENGINEERING_POLICY_UPDATE:
                            handleTrafficEngineeringPolicyUpdate((ControllerMessageUpdate) controllerMessage);
                            break;
                        case ROUTING_POLICY_UPDATE:
                            handleRoutingPolicyUpdate((ControllerMessageUpdate) controllerMessage);
                            break;
                        case DEFAULT_FLOW_PATHS_UPDATE:
                            handleDefaultFlowPathsUpdate((ControllerMessageUpdate) controllerMessage);
                            break;
                        case FLOW_PRIORITIES_UPDATE:
                            handleFlowPrioritiesUpdate((ControllerMessageUpdate) controllerMessage);
                            break;
                        case MULTICAST_CONTROL:
                            handleMulticastControl((ControllerMessageResponse) controllerMessage);
                            break;
                        case FIX_PATH_PUSH_RESPONSE:
                            /*
                             * TODO Remove me
                             */
                            log("FIX_PATH_PUSH_RESPONSE received at: " + timestamp + ", response size: " + packetSize + ", payloadSize: " + payloadSize);

                            handleFixPathPushResponse((ControllerMessageResponse) controllerMessage);
                            break;
                        case OS_ROUTING_ADD_ROUTE:
                            /*
                             * TODO Remove me
                             */
                            log("OS_ROUTING_ADD_ROUTE received at: " + timestamp + ", response size: " + packetSize + ", payloadSize: " + payloadSize);

                            handleOsRoutingAddRoute((ControllerMessageUpdate) controllerMessage);
                            break;
                        case OS_ROUTING_PUSH_RESPONSE:
                            /*
                             * TODO Remove me
                             */
                            log("OS_ROUTING_PUSH_RESPONSE received at: " + timestamp + ", response size: " + packetSize + ", payloadSize: " + payloadSize);

                            handleOsRoutingPushResponseRoute((ControllerMessageResponse) controllerMessage);
                            break;
                        case OS_ROUTING_DELETE_ROUTE:
                            handleOsRoutingDeleteRoute((ControllerMessageUpdate) controllerMessage);
                            break;
                        case OS_ROUTING_PRIORITY_UPDATE:
                            /*
                             * TODO Remove me
                             */
                            log("OS_ROUTING_PRIORITY_UPDATE received at: " + timestamp + ", response size: " + packetSize + ", payloadSize: " + payloadSize);

                            handleOsRoutingPriorityUpdate((ControllerMessageUpdate) controllerMessage);
                            break;
                        case DATA_PLANE_ADD_DATA_TYPE:
                            /*
                             * TODO Remove me
                             */
                            log("DATA_PLANE_ADD_DATA_TYPE received at: " + timestamp + ", response size: " + packetSize + ", payloadSize: " + payloadSize);

                            handleDataPlaneAddDataType((ControllerMessageUpdate) controllerMessage);
                            break;
                        case DATA_PLANE_REMOVE_DATA_TYPE:
                            handleDataPlaneRemoveDataType((ControllerMessageUpdate) controllerMessage);
                            break;
                        case DATA_PLANE_ADD_RULE_FILE:
                            /*
                             * TODO Remove me
                             */
                            log("DATA_PLANE_ADD_RULE_FILE received at: " + timestamp + ", response size: " + packetSize + ", payloadSize: " + payloadSize);

                            handleDataPlaneAddRuleFile((ControllerMessageUpdate) controllerMessage);
                            break;
                        case DATA_PLANE_REMOVE_RULE_FILE:
                            handleDataPlaneRemoveRuleFile((ControllerMessageUpdate) controllerMessage);
                            break;
                        case DATA_PLANE_ADD_RULE:
                            /*
                             * TODO Remove me
                             */
                            log("DATA_PLANE_ADD_RULE received at: " + timestamp + ", response size: " + packetSize + ", payloadSize: " + payloadSize);

                            handleDataPlaneAddRule((ControllerMessageUpdate) controllerMessage);
                            break;
                        case DATA_PLANE_REMOVE_RULE:
                            handleDataPlaneRemoveRule((ControllerMessageUpdate) controllerMessage);
                            break;
                        // add u284976
                        case READY_TO_TEST:
                            handleReadyToTest((ControllerMessageReady) controllerMessage);
                            break;
                        // add u284976
                        case PRIORITY_ON_AP_UPDATE:
                            handlePriorityOnAp((ControllerMessageUpdate) controllerMessage);
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        private void handleTrafficEngineeringPolicyUpdate(ControllerMessageUpdate updateMessage) {
            TrafficEngineeringPolicy newTrafficEngineeringPolicy = updateMessage.getTrafficEngineeringPolicy();
            if (flowDataPlaneForwarder != null) {
                flowDataPlaneForwarder.deactivate();
            }

            switch (newTrafficEngineeringPolicy) {
                case SINGLE_FLOW:
                    flowDataPlaneForwarder = SinglePriorityForwarder.getInstance(routingDataPlaneForwarder);
                    break;
                case QUEUES:
                    flowDataPlaneForwarder = MultipleFlowsSinglePriorityForwarder.getInstance(routingDataPlaneForwarder);
                    break;
                case TRAFFIC_SHAPING:
                    flowDataPlaneForwarder = MultipleFlowsMultiplePrioritiesForwarder.getInstance(routingDataPlaneForwarder);
                    break;
                case NO_FLOW_POLICY:
                    flowDataPlaneForwarder = null;
                    break;
                default:
                    break;
            }

            trafficEngineeringPolicy = newTrafficEngineeringPolicy;
            System.out.println("ControllerClient: TrafficEngineeringPolicy update (" + trafficEngineeringPolicy + ") received from the controller and successfully applied");
        }

        private void handleRoutingPolicyUpdate(ControllerMessageUpdate updateMessage) {
            RoutingPolicy newRoutingPolicy = updateMessage.getRoutingPolicy();
            if (routingDataPlaneForwarder != null) {
                routingDataPlaneForwarder.deactivate();
            }
            switch (newRoutingPolicy) {
                case REROUTING:
                    routingDataPlaneForwarder = BestPathForwarder.getInstance();
                    break;
                case MULTICASTING:
                    routingDataPlaneForwarder = MulticastingForwarder.getInstance();
                    break;
                case NO_ROUTING_POLICY:
                    routingDataPlaneForwarder = null;
                    break;
                default:
                    break;
            }

            routingPolicy = newRoutingPolicy;
            System.out.println("ControllerClient: routing policy update (" + routingPolicy + ") received from the controller and successfully applied");
        }

        private void handleDefaultFlowPathsUpdate(ControllerMessageUpdate updateMessage) {
            /*
             * Set the received default flow paths creation time and add them to the map
             */
            Map<Integer, PathDescriptor> newDefaultFlowPaths = updateMessage.getNewPathMappings();
            for (PathDescriptor pathDescriptor : newDefaultFlowPaths.values())
                pathDescriptor.setCreationTime(System.currentTimeMillis());
            defaultFlowPaths.putAll(newDefaultFlowPaths);
            System.out.println("ControllerClient: default flow paths update received from the controller and successfully applied");
        }

        private void handleFlowPrioritiesUpdate(ControllerMessageUpdate updateMessage) {
            flowPriorities = updateMessage.getFlowPriorities();
            System.out.println("ControllerClient: flow priorities update received from the controller and successfully applied");
        }

        private void handleMulticastControl(ControllerMessageResponse responseMessage) {
            flowMulticastNextHops.put(responseMessage.getFlowId(), responseMessage.getNewPaths());
            System.out.println("ControllerClient: multicast control update received from the controller and successfully applied");
        }

        private void handleFixPathPushResponse(ControllerMessageResponse responseMessage) {
            /*
             * Set the received fixed path and update it to the known flow paths.
             */
            int flowId = responseMessage.getFlowId();
            PathDescriptor newPath = responseMessage.getNewPaths().get(0);
            log("PushFixPath: " + Arrays.toString(newPath.getPath()));
            newPath.setCreationTime(System.currentTimeMillis());

            flowPaths.put(flowId, newPath);

            System.out.println("ControllerClient: response with a fixed path for flow " + flowId + " received from the controller");
        }

        /**
         * This method handles an OS_ROUTING_ADD_ROUTE message sent by the ControllerService
         * only to an intermediate node and not to the client asking for the OS level
         * routing.
         */
        private void handleOsRoutingAddRoute(ControllerMessageUpdate updateMessage) {
            /*
             * TODO Remove me
             */
            log("handleOsRoutingAddRoute");
            LocalDateTime localDateTime;
            String timestamp;

            int ackSocketPort = updateMessage.getClientPort();

            String sourceIP = updateMessage.getSrcIP();
            String destinationIP = updateMessage.getDestIP();
            String viaIP = updateMessage.getViaIP();
            int routeId = updateMessage.getRouteId();

            boolean success = false;
            try {
                long pre = System.currentTimeMillis();
                success = osRoutingManager.addRoute(sourceIP, destinationIP, viaIP, routeId);
                long post = System.currentTimeMillis();
                log("OS ROUTING MANAGER ADD ROUTE completed in: " + (post - pre) + "milliseconds");
            } catch (Exception e) {
                e.printStackTrace();
            }
            /*
             * Controller service has to be found before sending any message
             */
            ServiceResponse serviceResponse = controllerServiceDiscoverer.getControllerService();
            if (serviceResponse == null) {
                System.out.println("ControllerClient: controller service not found, cannot bind client socket");
            } else {
                /*
                 * If the "ip route add" command has been successfully applied send
                 * an OS_ROUTING_ACK message to the ControllerService otherwise send
                 * an OS_ROUTING_ABORT message.
                 */
                if (success) {
                    /*
                     * If this is the sender the method osRoutingManager.getRouteIdSourceIpAddress(routeId)
                     * returns the srcIP address selected for this route, otherwise null.
                     */
                    ControllerMessageAck ackMessage = new ControllerMessageAck(MessageType.OS_ROUTING_ACK, routeId);
                    try {
                        E2EComm.sendUnicast(serviceResponse.getServerDest(), serviceResponse.getServerNodeId(), ackSocketPort, serviceResponse.getProtocol(), CONTROL_FLOW_ID, E2EComm.serialize(ackMessage));
                        System.out.println("ControllerClient: OS_ROUTING_ACK for routeId: " + routeId + " sent to the controller");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    localDateTime = LocalDateTime.now();
                    timestamp = localDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
                    log("OS_ROUTING_ACK sent to controller at: " + timestamp);

                    System.out.println("ControllerClient: OS_ROUTING_ADD_ROUTE for routeId: " + routeId + " received from the controller and successfully applied");
                } else {
                    ControllerMessageAck abortMessage = new ControllerMessageAck(MessageType.OS_ROUTING_ABORT, routeId);
                    try {
                        E2EComm.sendUnicast(serviceResponse.getServerDest(), serviceResponse.getServerNodeId(), ackSocketPort, serviceResponse.getProtocol(), CONTROL_FLOW_ID, E2EComm.serialize(abortMessage));
                        System.out.println("ControllerClient: OS_ROUTING_ABORT for routeId: " + routeId + " sent to the controller");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println("ControllerClient: OS_ROUTING_ADD_ROUTE for routeId: " + routeId + " received from the controller but not applied");
                }
            }
        }

        private void handleOsRoutingPushResponseRoute(ControllerMessageResponse responseMessage) {
            int routeId = responseMessage.getRouteId();
            int osRoutingPathDuration = responseMessage.getOsROutingPathDuration();
            OsRoutingPathDescriptor osRoutingPathDescriptor = responseMessage.getOsRoutingPath();
            /*
             * For the intermediate nodes of an os route the osRoutingPathDescriptor
             * is null.
             */
            if (osRoutingPathDescriptor != null) {
                int destinationNode = osRoutingPathDescriptor.getDestinationNodeId();
                if (!routeIdsByDestination.containsKey(destinationNode)) {
                    List<Integer> routeIds = new ArrayList<>();
                    routeIds.add(routeId);
                    routeIdsByDestination.put(destinationNode, routeIds);
                } else {
                    routeIdsByDestination.get(destinationNode).add(routeId);
                }
                osRoutesPaths.put(routeId, osRoutingPathDescriptor);
                osRoutesPriority.put(routeId, false);
            }
            /*
             * This check is in case of pathSelectionMetric that may have different
             * different intermediate nodes for the forward and the backward path.
             */
            if (osRoutesStartTimes.containsKey(routeId)) {
                osRoutesStartTimes.replace(routeId, System.currentTimeMillis());
            } else {
                osRoutesStartTimes.put(routeId, System.currentTimeMillis());
            }
            /*
             * This check is in case of pathSelectionMetric that may have different
             * different intermediate nodes for the forward and the backward path.
             * This particular check is for the case in which the forward and the backward paths
             * have some intermediate node in common, given the fact that this value will be the same
             * for both the push responses we discard the second one.
             */
            if (!osRoutesDurations.containsKey(routeId)) {
                osRoutesDurations.put(routeId, osRoutingPathDuration);
            }
        }

        private void handleOsRoutingDeleteRoute(ControllerMessageUpdate responseMessage) {
            int routeId = responseMessage.getRouteId();
            osRoutingManager.deleteRoute(routeId);
            System.out.println("ControllerClient: OS_ROUTING_DELETE_ROUTE for routeId: " + routeId + " received from the controller and successfully applied");
        }

        private void handleOsRoutingPriorityUpdate(ControllerMessageUpdate updateMessage) {
            /*
             * TODO Remove me
             */
            log("handleOsRoutingPriorityUpdate");
            LocalDateTime localDateTime;
            String timestamp;

            int ackSocketPort = updateMessage.getClientPort();

            int routeId = updateMessage.getRouteId();

            ControllerMessageAck ackMessage;
            if (osRoutesPaths.containsKey(routeId)) {
                osRoutesPriority.replace(routeId, updateMessage.isOsRoutingPriority());
                ackMessage = new ControllerMessageAck(MessageType.OS_ROUTING_ACK, routeId);
            } else {
                ackMessage = new ControllerMessageAck(MessageType.OS_ROUTING_ABORT, routeId);
            }

            /*
             * Controller service has to be found before sending any message
             */
            ServiceResponse serviceResponse = controllerServiceDiscoverer.getControllerService();
            if (serviceResponse == null) {
                System.out.println("ControllerClient: controller service not found, cannot bind client socket");
            } else {
                try {
                    E2EComm.sendUnicast(serviceResponse.getServerDest(), serviceResponse.getServerNodeId(), ackSocketPort, serviceResponse.getProtocol(), CONTROL_FLOW_ID, E2EComm.serialize(ackMessage));
                    System.out.println("ControllerClient: " + ackMessage.getMessageType() + " for routeId: " + routeId + " sent to the controller");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                localDateTime = LocalDateTime.now();
                timestamp = localDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
                log(ackMessage.getMessageType() + " sent to controller at: " + timestamp);

                System.out.println("ControllerClient: OS_ROUTING_PRIORITY_UPDATE for routeId: " + routeId + " received from the controller and successfully applied");
            }
        }

        private void handleDataPlaneAddDataType(ControllerMessageUpdate updateMessage) {
            /*
             * TODO Remove me
             */
            log("handleDataPlaneAddDataType");
            LocalDateTime localDateTime;
            String timestamp;

            int ackSocketPort = updateMessage.getClientPort();

            DataPlaneMessage dataPlaneMessage = updateMessage.getDataPlaneMessage();
            String dataTypeFileName = updateMessage.getDataPlaneMessage().getFileName();
            boolean success = dataTypesManager.addUserDefinedDataType(dataPlaneMessage);

            /*
             * Controller service has to be found before sending any message
             */
            ServiceResponse serviceResponse = controllerServiceDiscoverer.getControllerService();
            if (serviceResponse == null) {
                System.out.println("ControllerClient: controller service not found, cannot bind client socket");
            } else {
                /*
                 * If the the rules command has been successfully applied send
                 * an DATA_PLANE_DATA_TYPE_ACK message to the ControllerService otherwise send
                 * an DATA_PLANE_DATA_TYPE_ABORT message.
                 */
                if (success) {
                    ControllerMessageAck ackMessage = new ControllerMessageAck(MessageType.DATA_PLANE_DATA_TYPE_ACK);
                    try {
                        E2EComm.sendUnicast(serviceResponse.getServerDest(), serviceResponse.getServerNodeId(), ackSocketPort, serviceResponse.getProtocol(), CONTROL_FLOW_ID, E2EComm.serialize(ackMessage));
                        System.out.println("ControllerClient: DATA_PLANE_DATA_TYPE_ACK sent to the controller");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    /*
                     * TODO Remove me
                     */
                    localDateTime = LocalDateTime.now();
                    timestamp = localDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
                    log("DATA_PLANE_DATA_TYPE_ACK to controller sent at: " + timestamp);

                    System.out.println("ControllerClient: DATA_PLANE_ADD_DATA_TYPE: add DataType: " + dataTypeFileName + " received from the controller and successfully applied");
                } else {
                    ControllerMessageAck abortMessage = new ControllerMessageAck(MessageType.DATA_PLANE_DATA_TYPE_ABORT);
                    try {
                        E2EComm.sendUnicast(serviceResponse.getServerDest(), serviceResponse.getServerNodeId(), ackSocketPort, serviceResponse.getProtocol(), CONTROL_FLOW_ID, E2EComm.serialize(abortMessage));
                        System.out.println("ControllerClient: DATA_PLANE_DATA_TYPE_ABORT sent to the controller");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println("ControllerClient: DATA_PLANE_ADD_DATA_TYPE: add DataType: " + dataTypeFileName + " received from the controller but not applied");
                }
            }
        }

        private void handleDataPlaneRemoveDataType(ControllerMessageUpdate updateMessage) {
            String dataTypeClassName = updateMessage.getDataType();
            dataTypesManager.removeUserDefinedDataType(dataTypeClassName);
            System.out.println("ControllerClient: DATA_PLANE_REMOVE_DATA_TYPE: DataType: " + dataTypeClassName + " received from the controller ans successfully not applied");

        }

        private void handleDataPlaneAddRuleFile(ControllerMessageUpdate updateMessage) {
            /*
             * TODO Remove me
             */
            log("handleDataPlaneAddRuleFile");
            LocalDateTime localDateTime;
            String timestamp;

            int ackSocketPort = updateMessage.getClientPort();

            DataPlaneMessage dataPlaneMessage = updateMessage.getDataPlaneMessage();
            String dataPlaneRuleFileName = updateMessage.getDataPlaneMessage().getFileName();

            boolean success = dataPlaneRulesManager.addUserDefinedDataPlaneRule(dataPlaneMessage);

            /*
             * Controller service has to be found before sending any message
             */
            ServiceResponse serviceResponse = controllerServiceDiscoverer.getControllerService();
            if (serviceResponse == null) {
                System.out.println("ControllerClient: controller service not found, cannot bind client socket");
            } else {
                /*
                 * If the the rules command has been successfully applied send
                 * an DATA_PLANE_RULE_ACK message to the ControllerService otherwise send
                 * an DATA_PLANE_RULE_ABORT message.
                 */
                if (success) {
                    ControllerMessageAck ackMessage = new ControllerMessageAck(MessageType.DATA_PLANE_RULE_ACK);
                    try {
                        E2EComm.sendUnicast(serviceResponse.getServerDest(), serviceResponse.getServerNodeId(), ackSocketPort, serviceResponse.getProtocol(), CONTROL_FLOW_ID, E2EComm.serialize(ackMessage));
                        System.out.println("ControllerClient: DATA_PLANE_RULE_ACK sent to the controller");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    /*
                     * TODO Remove me
                     */
                    localDateTime = LocalDateTime.now();
                    timestamp = localDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
                    log("DATA_PLANE_RULE_ACK to controller sent at: " + timestamp);

                    System.out.println("ControllerClient: DATA_PLANE_ADD_RULE_FILE: add DataPlaneRule: " + dataPlaneRuleFileName + " received from the controller and successfully applied");
                } else {
                    ControllerMessageAck abortMessage = new ControllerMessageAck(MessageType.DATA_PLANE_RULE_ABORT);
                    try {
                        E2EComm.sendUnicast(serviceResponse.getServerDest(), serviceResponse.getServerNodeId(), ackSocketPort, serviceResponse.getProtocol(), CONTROL_FLOW_ID, E2EComm.serialize(abortMessage));
                        System.out.println("ControllerClient: DATA_PLANE_RULE_ABORT sent to the controller");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    System.out.println("ControllerClient: DATA_PLANE_ADD_RULE_FILE: add DataPlaneRule: " + dataPlaneRuleFileName + " received from the controller but not applied");
                }
            }
        }

        private void handleDataPlaneRemoveRuleFile(ControllerMessageUpdate updateMessage) {
            String dataPlaneClassName = updateMessage.getDataPlaneRule();
            dataPlaneRulesManager.removeUserDefinedDataPlaneRule(dataPlaneClassName);
            System.out.println("ControllerClient: DATA_PLANE_REMOVE_RULE_FILE: DataPlaneRule: " + dataPlaneClassName + " received from the controller ans successfully not applied");
        }

        private void handleDataPlaneAddRule(ControllerMessageUpdate updateMessage) {
            /*
             * TODO Remove me
             */
            log("handleDataPlaneAddRule");
            LocalDateTime localDateTime;
            String timestamp;

            int ackSocketPort = updateMessage.getClientPort();

            String dataType = updateMessage.getDataType();
            long dataTypeId = dataTypesManager.getDataTypeId(dataType);
            String dataPlaneRule = updateMessage.getDataPlaneRule();

            boolean success = dataPlaneRulesManager.addDataPlaneRule(dataTypeId, dataPlaneRule);

            /*
             * Controller service has to be found before sending any message
             */
            ServiceResponse serviceResponse = controllerServiceDiscoverer.getControllerService();
            if (serviceResponse == null) {
                System.out.println("ControllerClient: controller service not found, cannot bind client socket");
            } else {
                /*
                 * If the the rules command has been successfully applied send
                 * an DATA_PLANE_RULE_ACK message to the ControllerService otherwise send
                 * an DATA_PLANE_RULE_ABORT message.
                 */
                if (success) {
                    /*
                     * If this is the sender the method osRoutingManager.getRouteIdSourceIpAddress(routeId)
                     * returns the srcIP address selected for this route, otherwise null.
                     */
                    ControllerMessageAck ackMessage = new ControllerMessageAck(MessageType.DATA_PLANE_RULE_ACK);
                    try {
                        E2EComm.sendUnicast(serviceResponse.getServerDest(), serviceResponse.getServerNodeId(), ackSocketPort, serviceResponse.getProtocol(), CONTROL_FLOW_ID, E2EComm.serialize(ackMessage));
                        System.out.println("ControllerClient: DATA_PLANE_RULE_ACK sent to the controller");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    /*
                     * TODO Remove me
                     */
                    localDateTime = LocalDateTime.now();
                    timestamp = localDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
                    log("DATA_PLANE_RULE_ACK to controller sent at: " + timestamp);

                    System.out.println("ControllerClient: DATA_PLANE_ADD_RULE: add rule: " + dataPlaneRule + " for data type: " + dataType + " received from the controller and successfully applied");
                } else {
                    ControllerMessageAck abortMessage = new ControllerMessageAck(MessageType.DATA_PLANE_RULE_ABORT);
                    try {
                        E2EComm.sendUnicast(serviceResponse.getServerDest(), serviceResponse.getServerNodeId(), ackSocketPort, serviceResponse.getProtocol(), CONTROL_FLOW_ID, E2EComm.serialize(abortMessage));
                        System.out.println("ControllerClient: DATA_PLANE_RULE_ABORT sent to the controller");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println("ControllerClient: DATA_PLANE_ADD_RULE: add rule: " + dataPlaneRule + " for data type: " + dataType + " received from the controller but not applied");
                }
            }
        }

        private void handleDataPlaneRemoveRule(ControllerMessageUpdate messageUpdate) {
            String dataType = messageUpdate.getDataType();
            String dataPlaneRule = messageUpdate.getDataPlaneRule();
            dataPlaneRulesManager.removeDataPlaneRule(dataType, dataPlaneRule);
            System.out.println("ControllerClient: DATA_PLANE_REMOVE_RULE:  remove rule: " + dataPlaneRule + " for dataType: " + dataType + " received from the controller and successfully applied");
        }

        private void handleReadyToTest(ControllerMessageReady messageUpdate){
            readyToTest = messageUpdate.getStartTime();
        }

        private void handlePriorityOnAp(ControllerMessageUpdate messageUpdate){
            Map<Integer,Integer> flowPriorityOnAP = messageUpdate.getFlowPriorities();

            GeneticPacketForward.getInstance(flowPriorityOnAP, routingDataPlaneForwarder);
        }
    }

    private class UpdateManager extends Thread {
        /*
         * Interval between each update
         */
        private static final int TIME_INTERVAL = 20 * 1000;

        private Map<String, NodeStats> networkInterfaceStats;
        private boolean active;
        private Map<String, Long> lastMeasureTimes = new HashMap<String, Long>();

        UpdateManager() {
            this.networkInterfaceStats = new HashMap<>();
            try {
                for (String address : Dispatcher.getLocalNetworkAddresses()) {
                    NetworkInterface addressNetworkInterface = NetworkInterface.getByInetAddress(InetAddress.getByName(address));
                    NodeStats addressNetworkInterfaceStats = new NetworkInterfaceStats(Dispatcher.getLocalRampId(), addressNetworkInterface);
                    this.networkInterfaceStats.put(address, addressNetworkInterfaceStats);
                }
            } catch (SocketException e1) {
                e1.printStackTrace();
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            this.active = true;
        }

        private void stopUpdateManager() {
            System.out.println("ControllerClient UpdateManager STOP");
            this.active = false;
        }

        private void updateFlows() {
            for (Integer flowId : flowStartTimes.keySet()) {
                long flowStartTime = flowStartTimes.get(flowId);
                int duration = flowDurations.get(flowId);
                long elapsed = System.currentTimeMillis() - flowStartTime;
                if (elapsed > (duration + (duration / 4)) * 1000) {
                    flowStartTimes.remove(flowId);
                    flowDurations.remove(flowId);
                    flowPaths.remove(flowId);
                }
            }
        }

        private void updateOsRoutes() {
            for (Integer routeId : osRoutesStartTimes.keySet()) {
                long osRouteStartTime = osRoutesStartTimes.get(routeId);
                int duration = osRoutesDurations.get(routeId);
                long elapsed = System.currentTimeMillis() - osRouteStartTime;
                if (elapsed > (duration + (duration / 4)) * 1000) {
                    osRoutingManager.deleteRoute(routeId);
                    osRoutesStartTimes.remove(routeId);
                    osRoutesDurations.remove(routeId);
                    osRoutesPaths.remove(routeId);
                    osRoutesPriority.remove(routeId);
                }
            }
        }

        private void sendTopologyUpdate() {
            /*
             * Obtain neighbor nodes nodeIds and addresses through the Heartbeater
             */
            Map<Integer, List<String>> neighborNodes = new ConcurrentHashMap<>();
            Vector<InetAddress> addresses = Heartbeater.getInstance(false).getNeighbors();
            for (InetAddress address : addresses) {
                Integer nodeId = Heartbeater.getInstance(false).getNodeId(address);
                short networkPrefixLength = Heartbeater.getInstance(false).getNetworkPrefixLength(address);
                String completeAddress = "" + address.getHostAddress() + "/" + networkPrefixLength;
                if (neighborNodes.containsKey(nodeId))
                    neighborNodes.get(nodeId).add(completeAddress);
                else {
                    List<String> neighborAddressList = new ArrayList<>();
                    neighborAddressList.add(completeAddress);
                    neighborNodes.put(nodeId, neighborAddressList);
                }
            }
            /*
             * Update network stats for the interfaces associated to the addresses of the node
             */
            for (String address : this.networkInterfaceStats.keySet()){
                this.networkInterfaceStats.get(address).updateStats();
            }
                
            /*
             * Send the obtained information about neighbor nodes to the controller
             */
            ControllerMessageUpdate updateMessage = new ControllerMessageUpdate(MessageType.TOPOLOGY_UPDATE, this.networkInterfaceStats, neighborNodes, null, null, null, null, null, null, null);
            /*
             * Controller service has to be found before sending any message
             */
            ServiceResponse serviceResponse = getControllerService();
            if (serviceResponse == null) {
                System.out.println("ControllerClient: controller service not found, cannot bind client socket");
            } else {
                try {
                    E2EComm.sendUnicast(serviceResponse.getServerDest(), serviceResponse.getServerNodeId(), serviceResponse.getServerPort(), serviceResponse.getProtocol(), CONTROL_FLOW_ID, E2EComm.serialize(updateMessage));
                    System.out.println("ControllerClient UpdateManager: topology update sent to the controller");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * adder @u284976
         * send measure request to one hop neighbor
         */

        private void Measure() throws Exception{

            BoundReceiveSocket client = E2EComm.bindPreReceive(E2EComm.UDP);
            // address, List={delay,throughput}
            Map<String,List<Double>> measureResult = new HashMap<String,List<Double>>();

            int MAX_ATTEMP = 20;

            Vector<InetAddress> addresses = Heartbeater.getInstance(false).getNeighbors();
            for(InetAddress address : addresses){
                if(!measureActive){
                    break;
                }
                String[] addr = new String[1];
                // address = /10.0.0.1
                // addr[0] = 10.0.0.1
                addr[0] = address.toString().substring(1);
            
                Long lastMeasureTime = lastMeasureTimes.get(addr[0]);
                if(lastMeasureTime != null && System.currentTimeMillis() - lastMeasureTime < 1*TIME_INTERVAL){
                    continue;
                }
                System.out.println("======================================");
                System.out.println("address = " + address.toString());
                Integer nodeId = Heartbeater.getInstance(false).getNodeId(address);
                

                ServiceResponse service = null;
                try {
                    service = ServiceDiscovery.findService(
                        addr,
                        "measure_"+nodeId.toString()
                    );
                } catch (Exception e) {
                    // e.printStackTrace();
                }
                
                if(service == null){
                    continue;
                }

                MeasureMessage msg = new MeasureMessage(MeasureMessage.Check_Occupy,client.getLocalPort());

                int attemp = 0;
                while(!clientMeasurer.tryOccupy()){
                    attemp++;
                    sleep(1000);
                    System.out.println("======================================");
                    System.out.println("address = " + address.toString());
                    System.out.println("wait a few seconds to occupy clientMeasure");

                    /**
                     * maybe test_done packet were drop
                     */
                    if(attemp >= MAX_ATTEMP){
                        clientMeasurer.releaseOccupy();;
                    }
                };
                if(!measureActive){
                    break;
                }

                // if(attemp >= MAX_ATTEMP){
                //     continue;
                // }

                E2EComm.sendUnicast(
                    service.getServerDest(),
                    service.getServerPort(),
                    service.getProtocol(),
                    E2EComm.serialize(msg)
                );
                boolean retry = true;
                while (retry) {
                    
                    if(!measureActive){
                        break;
                    }

                    GenericPacket gp = E2EComm.receive(client,5000);
                    UnicastPacket up = (UnicastPacket)gp;
                    Object payload = E2EComm.deserialize(up.getBytePayload());

                    if(payload instanceof MeasureMessage){

                        MeasureMessage mm = (MeasureMessage)payload;
                        int res = mm.getMessageType();

                        switch (res) {
                            /**
                             * neighbor is non-occupied,
                             * start measure delay.
                             */
                            case MeasureMessage.Response_OK:
                                /** 
                                 * Setting will not enter while loop again
                                 */
                                retry = false;

                                /**
                                 * 2020-06-17 replace to new Throughput measure method
                                 */
                                // boolean failed = false;

                                

                                // BoundReceiveSocket delayClient = E2EComm.bindPreReceive(E2EComm.UDP);
                                // long[] delay = new long[5];
                                // long temp = 0;

                                // for(int i=0 ; i<5 ; i++){
                                //     msg = new MeasureMessage(MeasureMessage.Test_Delay,delayClient.getLocalPort());
                                //     E2EComm.sendUnicast(
                                //         service.getServerDest(),
                                //         service.getServerPort(),
                                //         service.getProtocol(),
                                //         E2EComm.serialize(msg)
                                //     );
                                //     long pre = System.currentTimeMillis();

                                //     try {
                                //         E2EComm.receive(delayClient);    
                                //     } catch (Exception e) {
                                //         failed = true;
                                //     }

                                //     delay[i] = System.currentTimeMillis() - pre;

                                //     // System.out.println("==========");
                                //     // System.out.println("delay = " + delay[i]);
                                //     temp += delay[i];
                                // }
                                // delayClient.close();

                                // double avgDelay = temp/delay.length;

                                // // send file need use TCP
                                // BoundReceiveSocket throughputClient = E2EComm.bindPreReceive(E2EComm.TCP);
                                // msg = new MeasureMessage(MeasureMessage.Test_Throughput,throughputClient.getLocalPort(),"1M.test");

                                // long pre = System.currentTimeMillis();
                                // E2EComm.sendUnicast(
                                //     service.getServerDest(),
                                //     service.getServerPort(),
                                //     service.getProtocol(),
                                //     E2EComm.serialize(msg)
                                // );

                                // // System.out.println("==========");
                                // // System.out.println("starting transfer...");

                                // try {
                                //     E2EComm.receive(throughputClient);    
                                // } catch (Exception e) {
                                //     failed = true;
                                // }
                                
                                // long elapsed = System.currentTimeMillis() - pre;


                                // double throughput = (1024.0 / elapsed)*1000*8;      // Kbit/s
                                // // System.out.println("==========");
                                // // System.out.println("pre = " + pre);
                                // // System.out.println("elapsed = " + elapsed);
                                // // System.out.println("avgDelay = " + avgDelay);
                                // // System.out.println("throughput = " + throughput + "Kbit/s");

                                // msg = new MeasureMessage(MeasureMessage.Test_Done);
                                // E2EComm.sendUnicast(
                                //     service.getServerDest(),
                                //     service.getServerPort(),
                                //     service.getProtocol(),
                                //     E2EComm.serialize(msg)
                                // );

                                // throughputClient.close();
                                // clientMeasurer.releaseOccupy();

                                // if(failed){
                                //     break;    // break while
                                // }
                                

                                // measure link method 2020/06/17~2020/08/09

                                // /**
                                //  * can refer to test.iotos.testSender and test.iotos.testReceiver
                                //  * this block is receiver
                                //  */

                                // int[] seq = new int[300];
                                // long[] sendTime = new long[300];
                                // long[] receiveTime = new long[300];
                                // for(int i=0 ; i<seq.length ; i++){
                                //     seq[i] = -1;
                                //     sendTime[i] = -1;
                                //     receiveTime[i] = -1;
                                // }

                                // BoundReceiveSocket TxServer = E2EComm.bindPreReceive(E2EComm.UDP);
                                // msg = new MeasureMessage(MeasureMessage.Test_Tx,TxServer.getLocalPort());
                                // E2EComm.sendUnicast(
                                //     service.getServerDest(),
                                //     service.getServerPort(),
                                //     service.getProtocol(),
                                //     E2EComm.serialize(msg)
                                // );

                                // UnicastPacket tx_up = null;
                                // try {
                                //     tx_up = (UnicastPacket)E2EComm.receive(TxServer);
                                //     new tx_Listener(tx_up, seq, sendTime, receiveTime).start();
                                // } catch (Exception e) {
                                // }


                                // long preWhile = System.currentTimeMillis();
                                // while(System.currentTimeMillis() - preWhile < 3000 ){
                                //     try {
                                //         up = (UnicastPacket)E2EComm.receive(TxServer,500);
                                //         if(up==null){
                                //             break;
                                //         }
                                //         new tx_Listener(up, seq, sendTime, receiveTime).start();
                                //     } catch (Exception e) {
                                //         // System.out.println("no packet arrival , Tx test done");
                                //     }
                                // }

                                // msg = new MeasureMessage(MeasureMessage.Test_Done);
                                // E2EComm.sendUnicast(
                                //     service.getServerDest(),
                                //     service.getServerPort(),
                                //     service.getProtocol(),
                                //     E2EComm.serialize(msg)
                                // );

                                // TxServer.close();
                                // clientMeasurer.releaseOccupy();

                                // int c = 0;
                                // long[] delay = new long[300];
                                // long total_delay = 0;
                                // for(int i=0 ; i<seq.length ; i++){
                                //     if(sendTime[i] == -1){
                        
                                //     }else{
                                //         c++;
                                //         delay[i] = receiveTime[i] - sendTime[i];
                                //         total_delay = total_delay + delay[i];
                                //     }
                                // }

                                // System.out.println("======Measure()======");
                                // System.out.println("c = " + c);
                                // System.out.println("======Measure()======");

                                // double avgDelay = (double)total_delay / (double)c;
                                // // if packet not complete receive, delay will not ture
                                // if(c < 295){
                                //     if(avgDelay > 50){
                                //         avgDelay = 50;
                                //     }
                                // }

                                // /**
                                //  * througput = lamda * n
                                //  * packet rate * payload size
                                //  * packet rate is fixed
                                //  * payload size will continus increase at 200ms 400ms 1000ms 2000ms after first packet
                                //  * payload size = 1000, 2000, 5000, 10000, 50000, respectively
                                //  * total transfer 3000ms and 300 packets
                                //  * (for details, please refer to the Sender setting)
                                //  * so we can observe how many packets are received to 
                                //  * determine how many THROUGHPUT this link can carry
                                //  * 
                                //  * 
                                //  * and because the bandwidth is limited by software ( wondershpaer or mininet.addLink(bw = 10))
                                //  * so it doesn't fail immediately when sending larger packets, 
                                //  * 
                                //  * for example, "sta1------sta2" bandwidth is 5 Mbit/s = 655360 bytes/s
                                //  * according to the previous conjecture this receive only can receive 
                                //  * payload size = 1000, 2000, 5000, 10000,  when packet rate = 100/s
                                //  * i.e. from the first packet after 2000ms , there will be no packet due to insuffficient bandwidth
                                //  * so we expect to get c = 200
                                //  * but in my test, it can still receive more than 10 packets
                                //  */
                                // double throughput;
                                // if(c > 295){
                                //     throughput = 5000000;
                                // }else if(c > 250){
                                //     throughput = 2000000;
                                // }else if(c > 190){
                                //     throughput = 1000000;
                                // }else if(c > 150){
                                //     throughput = 500000;
                                // }else if(c > 80){
                                //     throughput = 100000;
                                // }else{
                                //     throughput = 1000*c;
                                // }

                                // // cheating for final test
                                // if(Dispatcher.getLocalRampId() == 8 && address.toString().endsWith("9")){
                                //     throughput = 2000000;
                                // }else if(Dispatcher.getLocalRampId() == 9 && address.toString().endsWith("8")){
                                //     throughput = 2000000;
                                // }else{
                                //     throughput = 5000000;
                                // }

                                // measure link method 2020/06/17~2020/08/09
                                // Packet Pair (on google calendar 2020/08/08)

                                int number_of_PacketPair = 10;

                                long[] sendTime = new long[number_of_PacketPair];
                                List<Long> realReceiveTime = new ArrayList<>();

                                
                                BoundReceiveSocket PacketPairSocket = E2EComm.bindPreReceive(E2EComm.UDP);
                                msg = new MeasureMessage(MeasureMessage.Test_PacketPair, PacketPairSocket.getLocalPort(),Integer.toString(number_of_PacketPair));
                                E2EComm.sendUnicast(
                                    service.getServerDest(),
                                    service.getServerPort(),
                                    service.getProtocol(),
                                    E2EComm.serialize(msg)
                                );

                                UnicastPacket pp_up = null;
                                // first measure message
                                try {
                                    pp_up = (UnicastPacket)E2EComm.receive(PacketPairSocket);
                                    realReceiveTime.add(System.currentTimeMillis());
                                    new PacketPairListener(pp_up, sendTime, number_of_PacketPair).start();
                                } catch (Exception e) {
                                }

                                long pp_preWhile = System.currentTimeMillis();
                                while(System.currentTimeMillis() - pp_preWhile < 1000 ){
                                    try {
                                        pp_up = (UnicastPacket)E2EComm.receive(PacketPairSocket,500);
                                        if(up==null){
                                            break;
                                        }
                                        realReceiveTime.add(System.currentTimeMillis());
                                        new PacketPairListener(pp_up, sendTime, number_of_PacketPair).start();
                                    } catch (Exception e) {
                                        // System.out.println("no packet arrival , Packet Pair Test done");
                                    }
                                }


                                // return test Done
                                msg = new MeasureMessage(MeasureMessage.Test_Done);
                                E2EComm.sendUnicast(
                                    service.getServerDest(),
                                    service.getServerPort(),
                                    service.getProtocol(),
                                    E2EComm.serialize(msg)
                                );

                                PacketPairSocket.close();
                                clientMeasurer.releaseOccupy();


                                // double[] gap = new double[number_of_PacketPair];      // delay[i] - delay[i-1], without first and second packet

                                long minReceive = Long.MAX_VALUE;
                                long maxReceive = Long.MIN_VALUE;
                                long minSend = Long.MAX_VALUE;
                                for(int i=0 ; i<number_of_PacketPair ; i++){
                                    minReceive = Math.min(minReceive, realReceiveTime.get(i));
                                    maxReceive = Math.max(maxReceive, realReceiveTime.get(i));
                                }
                                for(int i=0 ; i<sendTime.length ; i++){
                                    minSend = Math.min(minSend, sendTime[i]);
                                }

                                double delay = (double)(minReceive - minSend);


                                /**
                                 * packet receive gap  only take 2 and later
                                 */
                                // for(int i=1 ; i<number_of_PacketPair ;){
                                //     gap[i] = realReceiveTime.get(i) - realReceiveTime.get(i-1);
                                // }
                                double total_gap = 0;
                                for(int i=2 ; i<number_of_PacketPair ; i++){
                                    total_gap = total_gap + (realReceiveTime.get(i) - realReceiveTime.get(i-1));
                                }

                                long time_diff = Math.round(total_gap/(double)(number_of_PacketPair - 2));
                                double packetSize = 1000;
                                double throughput = (packetSize/(double)time_diff) * 1000;      // change to bytes / per second

                                for(int i=0 ; i<number_of_PacketPair ; i++){
                                    System.out.println(sendTime[i]+"    "+realReceiveTime.get(i)+"    "+(realReceiveTime.get(i)-sendTime[i]));
                                }
                               
                                if(time_diff == 0){
                                    throughput = 5000000;       // by previous work "Test_Tx", limit this value for GA will take this value as node throughput
                                }
                                System.out.println("avg time diff = " + time_diff);
                                System.out.println("predict :");
                                System.out.println("delay = " + delay + " ms");
                                System.out.println("throughput = " + throughput + " bytes / per second");
                                System.out.println(" = " + throughput/1024/1024*8 + "Mbit/s");

                                // store
                                String neighborAddress = service.getServerDest()[0];
                                List<Double> result = new ArrayList<Double>();
                                // result.add(avgDelay);
                                result.add(delay);
                                result.add(throughput);
                                measureResult.put(neighborAddress, result);
                                
                                break;
                            // retry
                            case MeasureMessage.Response_Occupied:

                                // release Occupy, avoid deadlock 
                                clientMeasurer.releaseOccupy();
                                sleep(3000);
                                while (!clientMeasurer.tryOccupy()) {
                                    System.out.println("======================================");
                                    System.out.println("wait " + address.toString() + ", and release");
                                    sleep(3000);
                                }
                                msg = new MeasureMessage(MeasureMessage.Check_Occupy,client.getLocalPort());

                                E2EComm.sendUnicast(
                                    service.getServerDest(),
                                    service.getServerPort(),
                                    service.getProtocol(),
                                    E2EComm.serialize(msg)
                                );
                                // make while loop do again and receive response
                                retry = true;
                                break;
                                
                        }                       
                    }
                }
            }
            if(measureActive){
                if(measureResult.size()>0){
                    for(String address : measureResult.keySet()){
                        lastMeasureTimes.put(address,System.currentTimeMillis());
                    }
                    ControllerMessageUpdate updateMessage = new ControllerMessageUpdate(MessageType.TOPOLOGY_LINK_UPDATE, this.networkInterfaceStats, null, null, null, null, null, null, null, null, measureResult);
                    ServiceResponse serviceResponse = getControllerService();
                    if (serviceResponse == null) {
                        System.out.println("ControllerClient: controller service not found, cannot bind client socket");
                    } else {
                        try {
                            E2EComm.sendUnicast(serviceResponse.getServerDest(), serviceResponse.getServerNodeId(), serviceResponse.getServerPort(), serviceResponse.getProtocol(), CONTROL_FLOW_ID, E2EComm.serialize(updateMessage));
                            System.out.println("ControllerClient UpdateManager: topology update sent to the controller");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    client.close();
                }
            }
        }

        @Override
        public void run() {
            /*
             * Sleep two seconds in order to avoid that the updateManager sendTopologyUpdate is sent before the join message.
             */
            try {
                Thread.sleep(2 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("ControllerClient UpdateManager START");
            while (this.active) {
                /*
                 * Send invoked before sleep method to allow the controller to receive information at components startup
                 */
                sendTopologyUpdate();

                /**
                 * adder @u284976
                 * send measure request to one hop neighbor
                 */
                if(measureActive){
                    try {
                        Measure();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                
                try {
                    Thread.sleep(TIME_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                updateFlows();
                updateOsRoutes();
            }
            System.out.println("ControllerClient UpdateManager FINISHED");
        }
    }
}
/**
 * add u284976
 * for listen Tx packet
 * measure link method 2020/06/17~2020/08/09
 */
class tx_Listener extends Thread{
    int[] seq;
    long[] sendTime;
    long[] receiveTime;
    UnicastPacket up;
    String nodeID = Dispatcher.getLocalRampIdString();
    String outputFile = "../0-outputFile/" + "Measure_on_" + nodeID + ".csv";

    tx_Listener(UnicastPacket up , int[] seq, long[] sendTime, long[] receiveTime){
        this.seq = seq;
        this.sendTime = sendTime;
        this.receiveTime = receiveTime;
        this.up = up;
    }

    public void run(){
        timeDataType payload = null;
        try {
            payload = (timeDataType)E2EComm.deserialize(up.getBytePayload());

            /**
             * output file of client measure info
             */
            // CSVWriter writer = new CSVWriter(new FileWriter(outputFile, true), ','); 
            // String sendNode = Integer.toString(up.getSourceNodeId());
            // String sendTime = Long.toString(payload.getSendTime());
            // String currentTime = Long.toString(System.currentTimeMillis());
            // String seqNum = Integer.toString(payload.getSeqNumber());
            // String payloadSize = Integer.toString(payload.getPayloadSize());
            // String diff = Long.toString(System.currentTimeMillis() - payload.getSendTime());
            
            // String[] entry = {sendNode, seqNum, payloadSize, sendTime, currentTime, diff};
            // writer.writeNext(entry);
            // writer.close();

        } catch (Exception e) {
        }
        int seq = payload.getSeqNumber();

        if(seq < 300){
            sendTime[seq] = payload.getSendTime();
            receiveTime[seq] = System.currentTimeMillis();
        }
    }
}

/**
 * add u284976
 * for listen Packet Pair (on google calendar 2020/08/08)
 * measure link method 2020/08/09 ~
 */
class PacketPairListener extends Thread{
    long[] sendTime;
    int number_of_PacketPair;
    UnicastPacket up;
    String nodeID = Dispatcher.getLocalRampIdString();
    String outputFile = "../0-outputFile/" + "Measure_on_" + nodeID + ".csv";

    PacketPairListener(UnicastPacket up , long[] sendTime, int number_of_PacketPair){
        this.sendTime = sendTime;
        this.number_of_PacketPair = number_of_PacketPair;
        this.up = up;
    }

    public void run(){
        timeDataType payload = null;

        try{
            payload = (timeDataType)E2EComm.deserialize(up.getBytePayload());


            /**
             * output file of client measure info
             */
            // CSVWriter writer = new CSVWriter(new FileWriter(outputFile, true), ','); 
            // String sendNode = Integer.toString(up.getSourceNodeId());
            // String sendTime = Long.toString(payload.getSendTime());
            // String currentTime = Long.toString(System.currentTimeMillis());
            // String seqNum = Integer.toString(payload.getSeqNumber());
            // String payloadSize = Integer.toString(payload.getPayloadSize());
            // String diff = Long.toString(System.currentTimeMillis() - payload.getSendTime());
            
            // String[] entry = {sendNode, seqNum, payloadSize, sendTime, currentTime, diff};
            // writer.writeNext(entry);
            // writer.close();

        } catch (Exception e){

        }

        int sequence = payload.getSeqNumber();
        
        if(sequence < number_of_PacketPair){
            sendTime[sequence] = payload.getSendTime();
        }
    }
}
