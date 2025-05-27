package tcp;
import java.io.*;
import java.net.*;


public class ClientTCP implements Runnable
{
    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private boolean done = false;


    @Override
    public void run()
    {
        try
        {
            client = new Socket("localhost",5000);
            out = new PrintWriter(client.getOutputStream(),true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            InputHandler inHandler = new InputHandler();
            Thread t = new Thread(inHandler);
            t.start();

            String inMessage;
            while((inMessage = in.readLine()) != null)
            {
                System.out.println(inMessage);
            }
        }
        catch (IOException e)
        {
            shutdown();
        }
    }
    public void shutdown()
    {
        done = true;
        try {
            in.close();
            out.close();
            if(!client.isClosed())
            {
                client.close();
            }
        }
        catch (IOException e)
        {
            System.out.println("blad polaczenia");
            //ignore
        }
    }
    class InputHandler implements Runnable
    {

        @Override
        public void run()
        {
            try
            {
                BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
                while(!done)
                {
                    String message = inReader.readLine();
                    sendMessageToServer(message);
                }
            }
            catch (IOException e)
            {
                System.out.println("blad polaczenia");
                shutdown();
            }
        }
        void sendMessageToServer(String message)
        {
            out.println(message);
        }
    }

    public static void main(String[] args)
    {
        ClientTCP client = new ClientTCP();
        client.run();
    }
}