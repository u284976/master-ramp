package test.iotos.testbatch;

import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.ApplicationRequirements;
import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.PathSelectionMetric;
import it.unibo.deis.lia.ramp.core.internode.sdn.trafficEngineeringPolicy.TrafficEngineeringPolicy;

public interface SetupTestBatch {
    public String getTestBatchName();
    public String getTestBatchTime();
    
    /**
     * Mobility
     * true : will not stop measure link capacity
     * false : when application start transfer , clientMeasure will close
     */
    public boolean getMobility();

    /**
     * EnableFixedness
     * true : let Genetic alog consider node fixedness
     *          if articulation point fixedness is less than threshold, Genetic algo will not let application wait other application transfer done.
     * false : on the contrary
     */
    public boolean getEnableFixedness();

    /**
     * return 
     * PathSelectionMetric.GENETIC_ALGO
     *          test Genetic algorithm     
     * PathSelectionMetric.BREADTH_FIRST
     *          test no Genetic algorithm case
     *      
     */ 
    public PathSelectionMetric getPathSelectionMetric();

    /**
     * return
     *      TrafficEngineeringPolicy.NO_FLOW_POLICY
     *           test Genetic algorithm
     *      Other policy
     *          test no Genetic algorithm case
     */
    public TrafficEngineeringPolicy getTrafficEngineeringPolicy();

    /**
     * NumberOfClient = 0, means this testBatch not care about topo complete
     */
    public int getNumberOfClient();
    public int getNumberOfEdge();
    /**
     * TestSecond will setup application transfer time, please equal to application requirement "duration"
     */
    public int getTestSecond();

    public String getAppTarget(String nodeID);
    public boolean getReceive(String nodeID);
    public ApplicationRequirements getApplicationRequirement(String nodeID);
    
    

}