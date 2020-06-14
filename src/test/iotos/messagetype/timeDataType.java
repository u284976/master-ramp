package test.iotos.messagetype;

import java.io.Serializable;

import it.unibo.deis.lia.ramp.service.application.SDNControllerMessage;

public class timeDataType extends SDNControllerMessage implements Serializable {
    
    private static final long serialVersionUID = 2L;

    private long sendTime;
    
    public timeDataType(){
        super();
    }
    public timeDataType(int seqNumber, int payloadSize, long sendTime) {
        super(seqNumber, payloadSize);
        this.sendTime = sendTime;
    }
    public void setSendTime(long sendTime){
        this.sendTime = sendTime;
    }
    public long getSendTime() {
        return sendTime;
    }
}