package test.iotos.messagetype;

import java.io.Serializable;

/**
 * @author u284976
 */

public class MeasureMessage implements Serializable{
    private static final long serialVersionUID = 1;

    public static final int Check_Occupy = 0;
    public static final int Response_OK = 1;
    public static final int Response_Occupied = 2;
    public static final int Test_Delay = 3;
    public static final int Test_Throughput = 4;
    public static final int Test_Done = 5;
    
    private int messagetype;

    private int port;

    private String filename;

    public MeasureMessage(int messagetype){
        this.messagetype = messagetype;
    }
    public MeasureMessage(int messagetype,int localPort){
        this.messagetype = messagetype;
        this.port = localPort;
    }
    public MeasureMessage(int messagetype,int localPort,String targetFile){
        this.messagetype = messagetype;
        this.port = localPort;
        this.filename = targetFile;
    }
    
    public int getMessageType(){
        return messagetype;
    }
    public int getClientPort(){
        return port;
    }
    public String getFilename(){
        return filename;
    }
}