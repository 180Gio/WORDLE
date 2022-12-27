import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class TerminationHandlerClient extends Thread {
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    TerminationHandlerClient(Socket socket, BufferedReader in, PrintWriter out) {
        this.socket = socket;
        this.out = out;
        this.in = in;
    }
    @Override
    public void run(){
        out.println("EXIT");
        try{
            in.close();
            socket.close();
            out.close();
        } catch (IOException e) { System.out.println("ERR: Can't close communication channels"); }
    }
}