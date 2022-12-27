import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.*;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class TerminationHandler extends Thread{
    private final ExecutorService pool;
    private final ServerSocket serverSocket;
    private final List<User> users;

    public TerminationHandler(ExecutorService pool, ServerSocket serverSocket, List<User> users) {
        this.pool = pool;
        this.serverSocket = serverSocket;
        this.users = users;
    }

    /**
     * Metodo che aggiorna il file users.json con i dati degli utenti aggiornati
     * @throws IOException Se il file users.json non esiste
     */
    public void updateJSON() throws IOException {
        Type userListType = new TypeToken<List<User>>(){}.getType();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(users, userListType);
        Class<WordleServerMain> cls = WordleServerMain.class;
        BufferedWriter file = new BufferedWriter(new FileWriter(cls.getResource("users.json").getFile()));
        file.close();

    }

    public void run(){
        System.out.println("WorldeServer is shutting down");
        try{ serverSocket.close(); } catch (IOException e) { System.out.println("ERR: Can't close serverSocket"); }
        pool.shutdown();
        try { updateJSON(); } catch (IOException e) { System.out.println("ERR: Can't update users.json"); }
        try { if (!pool.awaitTermination(10, TimeUnit.MILLISECONDS)) { pool.shutdownNow(); } } catch (InterruptedException e) { pool.shutdownNow(); }
    }
}