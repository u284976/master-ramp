package test.iotos;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;

import it.unibo.deis.lia.ramp.core.e2e.BroadcastPacket;
import it.unibo.deis.lia.ramp.core.e2e.GenericPacket;
import it.unibo.deis.lia.ramp.core.e2e.UnicastHeader;
import it.unibo.deis.lia.ramp.core.e2e.UnicastPacket;
import it.unibo.deis.lia.ramp.core.internode.Dispatcher;
import it.unibo.deis.lia.ramp.core.internode.sdn.controllerClient.ControllerClientInterface;
import it.unibo.deis.lia.ramp.core.internode.sdn.dataPlaneForwarder.DataPlaneForwarder;
import it.unibo.deis.lia.ramp.util.componentLocator.ComponentLocator;
import it.unibo.deis.lia.ramp.util.componentLocator.ComponentType;

public class GeneticPacketForward implements DataPlaneForwarder{

    private static final int CONTROL_FLOW_ID = 0;

    private UpdateManager updateManager;

    private ControllerClientInterface controllerClient = null;

    private Map<NetworkInterface, Long> lastPacketSendStartTimes;

    private Map<Integer, Map<Integer, Integer>> prioritiesFlowIdsSentPackets;

    private Map<Integer, Map<Integer, Integer>> previousPrioritiesFlowIdsSentPackets;

    private Map<Integer, Integer> previousPrioritiesTotalFlows;

    private Map<Integer, Integer> previousPrioritiesTotalSentPackets;

    private Map<NetworkInterface, List<Long>> lastFivePacketsSendStartTimesLists;

    private Map<NetworkInterface, Double> lastPacketSendDurations;

    private Map<String, NetworkInterface> networkInterfaces;
    
    private static GeneticPacketForward gpf = null;

    private static Map<Integer,Integer> flowPrioritys;



    public synchronized static GeneticPacketForward getInstance(Map<Integer,Integer> flowPriorityOnAP, DataPlaneForwarder routingForwarder){

        System.out.println("===========gpf============");
        System.out.println("flow priority = ");
        for(int flowID : flowPriorityOnAP.keySet()){
            System.out.println(flowID + " " + flowPriorityOnAP.get(flowID));
        }
        System.out.println("===========gpf============");
        if(gpf == null){
            gpf = new GeneticPacketForward();

            gpf.updateManager = new UpdateManager();
            gpf.updateManager.start();
            
            gpf.networkInterfaces = new ConcurrentHashMap<>();
            gpf.lastPacketSendStartTimes = new ConcurrentHashMap<>();

            gpf.prioritiesFlowIdsSentPackets = new ConcurrentHashMap<>();
            gpf.previousPrioritiesFlowIdsSentPackets = new ConcurrentHashMap<>();
            gpf.previousPrioritiesTotalFlows = new ConcurrentHashMap<>();
            gpf.previousPrioritiesTotalSentPackets = new ConcurrentHashMap<>();
            gpf.lastFivePacketsSendStartTimesLists = new ConcurrentHashMap<>();
            gpf.lastPacketSendDurations = new ConcurrentHashMap<>();

            Dispatcher.getInstance(false).addPacketForwardingListenerBeforeAnother(gpf, routingForwarder);
            System.out.println("==========GeneticPacketForward==========");
            System.out.println("GeneticPacketForward starting...");
            System.out.println("==========GeneticPacketForward==========");
        }
        flowPrioritys = flowPriorityOnAP;

        return gpf;
    }
    public void deactivate(){
        if (gpf != null) {
            Dispatcher.getInstance(false).removePacketForwardingListener(gpf);
            gpf = null;
            System.out.println("==========GeneticPacketForward==========");
            System.out.println("GeneticPacketForward closing...");
            System.out.println("==========GeneticPacketForward==========");
        }
    }

	public void receivedUdpUnicastPacket(UnicastPacket up){

        int flowId = up.getFlowId();

        if (flowPrioritys.containsKey(flowId) && up.getDestNodeId() != Dispatcher.getLocalRampId()) {
            int flowPriority = flowPrioritys.get(flowId);

            NetworkInterface nextSendNetworkInterface = getNextSendNetworkInterface(up.getDest()[up.getCurrentHop()]);
            /*
             * If the packet is the first to arrive, save the send information and occupy the transmission channel
             */
            if (this.lastPacketSendStartTimes.get(nextSendNetworkInterface) == null && this.lastPacketSendDurations.get(nextSendNetworkInterface) == null) {
                synchronized (this) {
                    // double sendDuration = ((double) up.getBytePayload().length / networkSpeed) * 1000;
                    double sendDuration = getAverageInterPacketTime(nextSendNetworkInterface) * 1.25;
                    if (sendDuration < 25)
                        sendDuration = 25;
                    this.lastPacketSendDurations.put(nextSendNetworkInterface, sendDuration);
                    this.lastPacketSendStartTimes.put(nextSendNetworkInterface, System.currentTimeMillis());
                    List<Long> lastFivePacketsSendStartTimes = this.lastFivePacketsSendStartTimesLists.get(nextSendNetworkInterface);
                    if (lastFivePacketsSendStartTimes == null)
                        lastFivePacketsSendStartTimes = new ArrayList<>();
                    lastFivePacketsSendStartTimes.add(0, System.currentTimeMillis());
                    if (lastFivePacketsSendStartTimes.size() == 6)
                        lastFivePacketsSendStartTimes.remove(5);
                    this.lastFivePacketsSendStartTimesLists.put(nextSendNetworkInterface, lastFivePacketsSendStartTimes);
                    Map<Integer, Integer> priorityFlowIdsSentPackets = new ConcurrentHashMap<>();
                    priorityFlowIdsSentPackets.put(flowId, 1);
                    this.prioritiesFlowIdsSentPackets.put(flowPriority, priorityFlowIdsSentPackets);
                }
            }
            /*
             * If the packet has a priority value, get the elapsed time since the last send start
             * and get the send probability
             */
            else {
                long elapsed = System.currentTimeMillis() - this.lastPacketSendStartTimes.get(nextSendNetworkInterface);
                int sendProbability = getSendProbability(flowId, flowPriority);
                int randomNumber = ThreadLocalRandom.current().nextInt(100);
                /*
                 * If the packet is not allowed to proceed, wait and retry
                 */
                while (elapsed < this.lastPacketSendDurations.get(nextSendNetworkInterface) && randomNumber > sendProbability) {
                    elapsed = System.currentTimeMillis() - this.lastPacketSendStartTimes.get(nextSendNetworkInterface);
                    // long timeToWait = Math.round((this.lastPacketSendDurations.get(nextSendNetworkInterface) - elapsed) * 1.2);
                    long timeToWait = getAverageInterPacketTime(nextSendNetworkInterface);
                    if (timeToWait > 4000)
                        timeToWait = 4000;
                    if (timeToWait < 10)
                        timeToWait = 10;

                    try {
                        Thread.sleep(timeToWait);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    sendProbability = getSendProbability(flowId, flowPriority);
                    randomNumber = ThreadLocalRandom.current().nextInt(100);
                }
                synchronized (this) {
                    double sendDuration = getAverageInterPacketTime(nextSendNetworkInterface) * 1.25;
                    if (sendDuration < 25)
                        sendDuration = 25;
                    this.lastPacketSendDurations.put(nextSendNetworkInterface, sendDuration);
                    this.lastPacketSendStartTimes.put(nextSendNetworkInterface, System.currentTimeMillis());
                    List<Long> lastFivePacketsSendStartTimes = this.lastFivePacketsSendStartTimesLists.get(nextSendNetworkInterface);
                    if (lastFivePacketsSendStartTimes == null)
                        lastFivePacketsSendStartTimes = new ArrayList<>();
                    lastFivePacketsSendStartTimes.add(0, System.currentTimeMillis());
                    if (lastFivePacketsSendStartTimes.size() == 6)
                        lastFivePacketsSendStartTimes.remove(5);
                    this.lastFivePacketsSendStartTimesLists.put(nextSendNetworkInterface, lastFivePacketsSendStartTimes);
                    Map<Integer, Integer> priorityFlowIdsSentPackets = this.prioritiesFlowIdsSentPackets.get(flowPriority);
                    Map<Integer, Integer> previousPriorityFlowIdsSentPackets = this.previousPrioritiesFlowIdsSentPackets.get(flowPriority);
                    if (priorityFlowIdsSentPackets == null) {
                        priorityFlowIdsSentPackets = new ConcurrentHashMap<>();
                        this.prioritiesFlowIdsSentPackets.put(flowPriority, priorityFlowIdsSentPackets);
                    }
                    if (priorityFlowIdsSentPackets.containsKey(flowId))
                        priorityFlowIdsSentPackets.put(flowId, priorityFlowIdsSentPackets.get(flowId) + 1);
                    else if (previousPriorityFlowIdsSentPackets != null && previousPriorityFlowIdsSentPackets.containsKey(flowId)) {
                        int sentPackets = (previousPriorityFlowIdsSentPackets.get(flowId) / 2) + 1;
                        priorityFlowIdsSentPackets.put(flowId, sentPackets);
                    } else
                        priorityFlowIdsSentPackets.put(flowId, 1);
                }
            }
        }
    }
	public void receivedUdpBroadcastPacket(BroadcastPacket bp){
        // System.out.println("==========GeneticPacketForward==========");
        // System.out.println("receivedUdpBroadcastPacket");
        // System.out.println("==========GeneticPacketForward==========");
    }
	
	public void receivedTcpUnicastPacket(UnicastPacket up){
        // System.out.println("==========GeneticPacketForward==========");
        // System.out.println("receivedTcpUnicastPacket");
        // System.out.println("==========GeneticPacketForward==========");
        receivedUdpUnicastPacket(up);
    }
	public void receivedTcpUnicastHeader(UnicastHeader uh){
        // System.out.println("==========GeneticPacketForward==========");
        // System.out.println("receivedTcpUnicastHeader");
        // System.out.println("==========GeneticPacketForward==========");
    }
	public void receivedTcpPartialPayload(UnicastHeader uh, byte[] payload, int off, int len, boolean lastChunk){
        // System.out.println("==========GeneticPacketForward==========");
        // System.out.println("receivedTcpPartialPayload");
        // System.out.println("==========GeneticPacketForward==========");
    }
	public void receivedTcpBroadcastPacket(BroadcastPacket bp){
        // System.out.println("==========GeneticPacketForward==========");
        // System.out.println("receivedTcpBroadcastPacket");
        // System.out.println("==========GeneticPacketForward==========");
    }
	
	public void sendingTcpUnicastPacketException(UnicastPacket up, Exception e){
        // System.out.println("==========GeneticPacketForward==========");
        // System.out.println("sendingTcpUnicastPacketException");
        // System.out.println("==========GeneticPacketForward==========");
    }
    public void sendingTcpUnicastHeaderException(UnicastHeader uh, Exception e){
        // System.out.println("==========GeneticPacketForward==========");
        // System.out.println("sendingTcpUnicastHeaderException");
        // System.out.println("==========GeneticPacketForward==========");
    }

    private NetworkInterface getNextSendNetworkInterface(String nextAddress) {
        NetworkInterface networkInterface = this.networkInterfaces.get(nextAddress);
        if (networkInterface == null) {
            try {
                for (String localAddress : Dispatcher.getLocalNetworkAddresses()) {
                    InetAddress localInetAddress = InetAddress.getByName(localAddress);
                    NetworkInterface localNetworkInterface = NetworkInterface.getByInetAddress(localInetAddress);
                    short networkPrefixLength = localNetworkInterface.getInterfaceAddresses().get(0).getNetworkPrefixLength();
                    String completeLocalAddress = localAddress + "/" + networkPrefixLength;
                    SubnetUtils subnetUtils = new SubnetUtils(completeLocalAddress);
                    SubnetInfo subnetInfo = subnetUtils.getInfo();
                    if (subnetInfo.isInRange(nextAddress)) {
                        networkInterface = localNetworkInterface;
                        this.networkInterfaces.put(nextAddress, networkInterface);
                    }
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return networkInterface;
    }

    private synchronized long getAverageInterPacketTime(NetworkInterface networkInterface) {
        long[] interPacketTimes = new long[4];
        List<Long> lastFivePacketsStartTimes = this.lastFivePacketsSendStartTimesLists.get(networkInterface);
        long averageInterPacketTime = 0;
        if (lastFivePacketsStartTimes != null && lastFivePacketsStartTimes.size() == interPacketTimes.length + 1) {
            long totalInterPacketTime = 0;
            for (int i = 0; i < interPacketTimes.length; i++) {
                interPacketTimes[i] = lastFivePacketsStartTimes.get(i) - lastFivePacketsStartTimes.get(i + 1);
                totalInterPacketTime = totalInterPacketTime + interPacketTimes[i];
            }
            averageInterPacketTime = totalInterPacketTime / interPacketTimes.length;
        }
        return averageInterPacketTime;
    }
    

    private synchronized int getSendProbability(int flowId, int flowPriority) {
        int sendProbability = 100;
        int totalFlows = 0;
        for (Map<Integer, Integer> priorityFlowIdsSentPackets : this.prioritiesFlowIdsSentPackets.values())
            totalFlows = totalFlows + priorityFlowIdsSentPackets.keySet().size();
        if (totalFlows > 1) {
            Map<Integer, Integer> priorityFlowIdsSentPackets = this.prioritiesFlowIdsSentPackets.get(flowPriority);
            Map<Integer, Integer> previousPriorityFlowIdsSentPackets = this.previousPrioritiesFlowIdsSentPackets.get(flowPriority);
            if (previousPriorityFlowIdsSentPackets != null) {
                Integer previousSentPackets = previousPriorityFlowIdsSentPackets.get(flowId);
                if (previousSentPackets != null && priorityFlowIdsSentPackets != null && priorityFlowIdsSentPackets.keySet().size() > 1) {
                    int previousPriorityTotalFlows = this.previousPrioritiesTotalFlows.get(flowPriority);
                    int sentPacketsTarget = this.previousPrioritiesTotalSentPackets.get(flowPriority) / previousPriorityTotalFlows;
                    int sentPacketsOffset = previousSentPackets - sentPacketsTarget;
                    int offsetPercentage = (sentPacketsOffset * 100) / sentPacketsTarget;
                    sendProbability = sendProbability - (offsetPercentage * previousPriorityTotalFlows);
                }
            }
            int higherPriorityTotalFlows = 0;
            for (int i = 0; i < flowPriority; i++) {
                Map<Integer, Integer> higherPriorityFlowIdsSentPackets = this.prioritiesFlowIdsSentPackets.get(i);
                if (higherPriorityFlowIdsSentPackets != null)
                    higherPriorityTotalFlows = higherPriorityTotalFlows + higherPriorityFlowIdsSentPackets.keySet().size();
            }
            if (higherPriorityTotalFlows > 0)
                sendProbability = sendProbability / (flowPriority + 1);
        }
        return sendProbability;
    }

    private synchronized void resetPrioritiesFlowIdsSentPackets() {
        this.previousPrioritiesFlowIdsSentPackets.clear();
        this.previousPrioritiesTotalFlows.clear();
        this.previousPrioritiesTotalSentPackets.clear();
        for (Integer priorityValue : this.prioritiesFlowIdsSentPackets.keySet()) {
            Map<Integer, Integer> priorityFlowIdsSentPackets = this.prioritiesFlowIdsSentPackets.get(priorityValue);
            Map<Integer, Integer> previousPriorityFlowIdsSentPackets = new ConcurrentHashMap<>();
            int previousPriorityTotalSentPackets = 0;
            for (Integer flowId : priorityFlowIdsSentPackets.keySet()) {
                int flowIdSentPackets = priorityFlowIdsSentPackets.get(flowId);
                previousPriorityFlowIdsSentPackets.put(flowId, flowIdSentPackets);
                previousPriorityTotalSentPackets = previousPriorityTotalSentPackets + flowIdSentPackets;
            }
            this.previousPrioritiesFlowIdsSentPackets.put(priorityValue, previousPriorityFlowIdsSentPackets);
            this.previousPrioritiesTotalFlows.put(priorityValue, priorityFlowIdsSentPackets.keySet().size());
            this.previousPrioritiesTotalSentPackets.put(priorityValue, previousPriorityTotalSentPackets);
            priorityFlowIdsSentPackets.clear();
        }
    }

    private static class UpdateManager extends Thread {

        private static final int TIME_INTERVAL = 2 * 1000;

        private boolean active;

        UpdateManager() {
            this.active = true;
        }

        public void stopUpdateManager() {
            this.active = false;
        }

        public void run() {
            while (this.active) {
                try {
                    Thread.sleep(TIME_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(gpf != null) {
                    gpf.resetPrioritiesFlowIdsSentPackets();
                }
            }
        }
    }
}