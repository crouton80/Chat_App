import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Timer;

//import Protocol.java
import Protocol.*;

public class Server implements Runnable {

    private ArrayList<String> clients;    
    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private ExecutorService pool;


    public Server(){
        connections = new ArrayList<>();
    }

    public void shutdown(){
        
        try{
            if(!server.isClosed()){
                server.close();
            }
            for (ConnectionHandler ch : connections){
                ch.shutdown();
            }
        } catch(IOException e){
            //ignore
        }
        
    }
    
    @Override
    public void run(){
        try {
            int port = 80;
            ServerSocket server = new ServerSocket(port);
            pool = Executors.newCachedThreadPool(); //thread pool

            while(true){
                Socket client = server.accept(); //accept incoming connections
                ConnectionHandler connHandler = new ConnectionHandler(client);
                connections.add(connHandler); //List of all connections
                
                pool.execute(connHandler); //assign a thread for each connection to support multiple connections
            }
            
        } catch (IOException e) {
            
            shutdown();
        }
    }

    class ConnectionHandler implements Runnable {
        //vars accessible across all methods in ConnectionHandler
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;
        private Timer timer;
        private volatile boolean muted; 
        
        
        //constructor
        public ConnectionHandler(Socket client){
            this.client = client;
        }

        
        
        //implement function that mutes a user for a period of time, when the time runs out, the user is unmuted.
        public void mute(String nickname, int time){
            for(ConnectionHandler ch : connections){
                if(ch != null && ch != this && ch.nickname.equals(nickname)){
                    ch.setMuted(true);
                    ch.sendMessage("You have been muted.");
                    timer = new Timer();
                    timer.schedule(new TimerTask(){
                        @Override
                        public void run(){
                            ch.setMuted(false);
                            ch.sendMessage("You have been unmuted");
                            timer.cancel();
                            timer.purge();
                        }
                    }, time * 1000);
                    return; //Once user is muted, exit loop

                }
            }
            sendMessage("User " + nickname + "is not found or already muted.");
            
        }
        //implement setMuted method.
            public void setMuted(boolean muted){
                this.muted = muted;
                if(muted){
                    //if user is muted, inform only him that he has been muted
                    this.sendMessage("You have been muted.");
                    
                } else {
                    this.sendMessage("You have been unmuted.");
                    
                }
                broadcast(nickname + " has been " + (muted ? "muted" : "unmuted")); // Broadcast to all clients
            }
               

        //implement function that promotes a user to an admin that can mute other users
        public void promote(String nickname, String admin){
            //TODO: handle promote function


        }


        public void sendPrivateMessage(String recipientNickname,String message){
            boolean recipientFound = false;
            for(ConnectionHandler ch : connections){
               
                if(ch != null && ch != this && ch.nickname.equals(recipientNickname)){
                    ch.sendMessage("Whisper from " + nickname + " : " + message); //what recipient sees
                    this.sendMessage("To " + recipientNickname + " : " + message); //what sender sees
                     recipientFound = true; //nickname exists
                    return; //Exit loop after message has been sent

                }
                
            }
            if(!recipientFound){
                this.sendMessage("User " + recipientNickname + " is offline.");
            }
            // //Send this message if user has not been found
            // sendMessage(recipientNickname + " does not exist or has changed his nickname.");
        }

        public void broadcast(String message){
            for(ConnectionHandler ch : connections){
                if(ch != null) {
                    ch.sendMessage(message);
                }
            }
        }

        @Override
        public void run(){
            try {
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out = new PrintWriter(client.getOutputStream(), true);
                //send msg to client
                out.println("Please enter your nickname");
                nickname = in.readLine();
                System.out.println(nickname + " has connected!");
                broadcast(nickname + " has joined this chat!");

                String message;
                while((message = in.readLine()) != null){
                    if(muted){
                        continue;
                    }
                    if(message.startsWith("/nick")){
                        //handling nickname
                        String[] splitMessage = message.split(" ", 2);
                        if(splitMessage.length == 2){
                            broadcast(nickname + " has changed its name to " + splitMessage[1]);
                            System.out.println(nickname + " has changed its name to " + splitMessage[1]);
                            nickname = splitMessage[1];
                            out.println("You have successfully changed your nickname to " + nickname);
                        } else {
                            sendMessage("Nickname provided is not valid.");
                        }
                    } else if(message.startsWith("/whisper")){
                        //handles whisper logic
                        String[] splitMessage = message.split(" ",3);
                        if(splitMessage.length >= 3){
                            
                            String recipientName = splitMessage[1];
                            String privateMessage = splitMessage[2];
                            sendPrivateMessage(recipientName, privateMessage);
                        } 
                         else {
                            sendMessage("Invalid /whisper syntax. Correct syntax: /whisper <nickname> <message>.");
                        }
                    } else if(message.startsWith("/mute")){
                        //TODO: handle mute functionality
                        String[] muteSplitMessage = message.split(" ",3);
                        if(muteSplitMessage.length >= 3){
                            String recipientName = muteSplitMessage[1];
                            int time = Integer.parseInt(muteSplitMessage[2]);
                            mute(recipientName, time);
                            broadcast("User " + recipientName + " has been muted for " + time + " seconds.");
                        }
                    } else if(message.startsWith("/quit")) {
                        broadcast(nickname + " has left the chat.");
                        // QUIT
                        shutdown();
                    } else {
                        broadcast(nickname + ": " + message );
                    }
                }

            } catch (IOException e) {
                shutdown();
            }
        }

        public void sendMessage(String message){
            //if user is muted, do not send message
            if(muted){
                return;
            } 
                out.println(message);
        
            
        }

        //Shutdown connections by closing resources
        public void shutdown(){
            try{
                // Closing streams
                in.close();
                out.close();
                
                pool.shutdown();

                if(!client.isClosed()){
                    client.close();
                }
            } catch(IOException e){
                //ignore any exceptions during shutdown
            }
        }
    }

    public static void main(String[] args){
        Server server = new Server();
        server.run();
    }
}
