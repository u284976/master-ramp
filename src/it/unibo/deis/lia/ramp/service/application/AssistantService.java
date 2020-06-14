package it.unibo.deis.lia.ramp.service.application;

/**
 *
 * @author u284976
 */

public class AssistantService{
    private static AssistantService assistantService;
    
    public static synchronized AssistantService getInstance(){
        assistantService = new AssistantService();
        return assistantService;
    }

    private AssistantService(){

    }
}