import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client implements Runnable {

    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private boolean done;

    @Override
    public void run(){
        int port = 80;
        done = false;
        try{
            client = new Socket("127.0.0.1", port);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(client.getOutputStream(),true);

            InputHandler inHandler = new InputHandler();
            Thread t = new Thread(inHandler);
            t.start();

            String inMessage;
            while ((inMessage = in.readLine()) != null){
                System.out.println(inMessage);
            }
        }catch(IOException e){
            // ignores
        }
    }

    public void shutdown(){
        try {
            done = true;
            in.close();
            out.close();
        
            if(!client.isClosed())
                client.close();
        } catch (Exception e) {
            // ignore
        }
    }

    public class InputHandler implements Runnable{

        @Override
        public void run(){
            // Constantly ask for console inputs.
        try{
            BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
            while(!done){
                String message = inReader.readLine();

                //Debug statement
                // System.out.println("Sending message to the server: " + message);


                if(message.equals("/quit")){
                    out.println(message);
                    inReader.close();
                    shutdown();
                } else {
                    out.println(message);
                    //Debug statment
                    // System.out.println("Message has been sent to the server: " + message);
                };
            }
        } catch(IOException e){
            
            e.printStackTrace();

        }
        }
        
    }
    
    public static void main(String[] args){
        Client client = new Client();
        client.run();
    }
}