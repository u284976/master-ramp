package it.unibo.deis.lia.ramp.service.application;

import it.unibo.deis.lia.ramp.service.management.ServiceManager;

/**
 *
 * @author u284976
 */

public class AssistantClient extends Thread{

    private static AssistantClient assistantClient;

    public static synchronized AssistantClient getInstance(){
        assistantClient = new AssistantClient();
        return assistantClient;
    }

    private AssistantClient(){
        // TODO register service to response neighbor client with single hop
        // ServiceManager.getInstance(false).registerService(
        //     "SingleHopClient",
        //     servicePort,
        //     protocol
        // );
    }

    @Override
    public void run(){
        // TODO

    }
}