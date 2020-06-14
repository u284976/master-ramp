package test.iotos;

public class threadTest{


    static Boolean a = true;

    public static void main(String args[]){

        
        Thread t = new myThread(a);
        t.start();
        


        try {
            Thread.sleep(7000);    
        } catch (Exception e) {
            //TODO: handle exception
        }
        
        a = false;
        t.stop();
        System.out.println("set a = false");
    }
}

class myThread extends Thread{

    Boolean active;

    public myThread(Boolean active){
        this.active = active;
    }

    public void run(){
        while(active){
            System.out.println("hihi");
            
            try{
                sleep(2000);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}