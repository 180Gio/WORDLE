import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;
import java.io.*;
import java.lang.reflect.Type;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class WordleServerMain implements Runnable {
    private static int PORT;
    private static int CHANGEWORDTIME;
    private static List<User> users;
    private static final Gson gson = new Gson();
    private static final Type userListType = new TypeToken<List<User>>(){}.getType();
    private static final ExecutorService poolThread = Executors.newCachedThreadPool();
    private static final List<String> words = new ArrayList<>();
    public static String wordToGuess;
    public static Semaphore semaphore = new Semaphore(1);
    public static Scanner fromFile;

    /**
     * Metodo che legge il file di configurazione e inizializza le variabili
     * @throws IOException Se il file WordleServerMain.properties non esiste
     */
    public static void getProp() throws IOException {
        Class<WordleServerMain> c = WordleServerMain.class;
        InputStream file = c.getResourceAsStream("WordleServerMain.properties");
        if(file == null) throw new FileNotFoundException();
        Properties properties = new Properties();
        properties.load(file);
        PORT = Integer.parseInt(properties.getProperty("PORT"));
        CHANGEWORDTIME = Integer.parseInt(properties.getProperty("CHANGEWORDTIME"));
        file.close();
    }

    /**
     * Metodo che legge il file words.txt e lo salva in una lista
     * @throws FileNotFoundException Se il file words.txt non esiste
     */
    public static void getWords() throws FileNotFoundException{
        Class<WordleServerMain> c = WordleServerMain.class;
        InputStream file = c.getResourceAsStream("words.txt");
        if(file == null) throw new FileNotFoundException();
        fromFile = new Scanner(file);
        while(fromFile.hasNextLine()){ words.add(fromFile.nextLine()); }
        fromFile.close();
    }

    /**
     * Metodo utilizzato da un thread per cambiare la wordToGuess ogni CHANGEWORDTIME millisecondi. Viene utilizzato un semaforo per evitare che la parola sia cambiata mentre un utente sta ancora giocando.
     */
    public void run(){
        Random ran = new Random();
        while(true){
            try {
                semaphore.acquire();
                wordToGuess = words.get(ran.nextInt(words.size()));
                System.out.println("Word to guess: " + wordToGuess);
                semaphore.release();
                Thread.sleep(CHANGEWORDTIME);
            } catch (InterruptedException e) {
                System.out.println("ERR: Can't sleep.");
                System.exit(1);
            }
        }
    }

    public static void main(String[] args) {
        try {
            getProp();
            getWords();
        } catch (FileNotFoundException e) {
            System.out.println("ERR: Can't read words file.");
            System.exit(1);
        } catch (IOException e) {
            System.out.println("ERR: Can't read properties file.");
            System.exit(1);
        }
        // Thread che si occupa di cambiare la parola da indovinare ogni CHANGEWORDTIME millisecondi
        Thread wordThread = new Thread(new WordleServerMain());
        wordThread.start();
        // Carico la lista degli utenti dal file users.json
        try {
            Class<WordleServerMain> cls = WordleServerMain.class;
            InputStream file = cls.getResourceAsStream("users.json");
            if(file == null) throw new FileNotFoundException();
            BufferedReader reader = new BufferedReader(new InputStreamReader(file));
            users = gson.fromJson(reader, userListType);
            if(users == null) users = new ArrayList<>();
        } catch (FileNotFoundException e) {
            System.out.println("ERR: Can't find users.json");
            System.exit(1);
        }
        //Avvio il server e accetto le connessioni dei client passandole ad un pool di thread di tipo cached
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server address: " + InetAddress.getLocalHost().getHostAddress());
            System.out.println("Server started on port: "+PORT);
            Runtime.getRuntime().addShutdownHook(new TerminationHandler(poolThread, serverSocket, users));
            //Uso una SocketException per uscire dal while(true) generata quando il server prova ad accettare una nuova connessione sulla serverSocket che viene chiusa dal TerminationHandler
            while (true) { try { poolThread.execute(new WordleGame(serverSocket.accept(), users, words)); } catch (SocketException e) { break; } }
        } catch (IOException e) {
            System.out.println("ERR: Can't start server on port: "+PORT);
            System.exit(1);
        }
    }
}