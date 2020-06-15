package test.iotos;

import it.unibo.deis.lia.ramp.core.internode.sdn.controllerMessage.ControllerMessage;
import it.unibo.deis.lia.ramp.core.internode.sdn.controllerMessage.MessageType;

/**
 * @@author u284976
 */

public class ControllerMessageReady extends ControllerMessage{

    private long startTime;

    // 5 seconds later, start get flow path
    public ControllerMessageReady(MessageType messageType){
        this(messageType, System.currentTimeMillis() + 5000);
    }

    public ControllerMessageReady(MessageType messageType, long startTime){
        super(messageType);
        this.startTime = startTime;
    }

    public long getStartTime(){
        return startTime;
    }
}