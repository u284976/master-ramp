package test.iotos.testbatch;

import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.ApplicationRequirements;

public interface SetupTestBatch {
    public String getTestBatchName();
    public int getNumberOfClient();
    public String getAppTarget(String nodeID);
    public boolean getReceive(String nodeID);
    public ApplicationRequirements getApplicationRequirement(String nodeID);
    
    

}