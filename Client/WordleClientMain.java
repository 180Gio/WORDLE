import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class WordleClientMain implements Runnable {
    private static int PORT;
    private static String SERVERADDRESS;
    private static BufferedReader input;
    private static PrintWriter output;
    private static Socket socket;
    private static String username;
    private static String password;
    private static BufferedReader readerFromCLI;
    private static boolean isLogged = false;
    private static boolean done = false;
    private static final List<String> stats = new ArrayList<>();

    /**
     * Metodo che invia al server username e password per la registrazione. Se l'username è già presente sul server o se la password è vuota, l'utente non viene registrato.
     * @param username Username dell'utente da registrare
     * @param password Password dell'utente da registrare
     * @throws IOException Se non è possibile ricevere la risposta dal server
     */
    private static void register(String username, String password) throws IOException {
        output.println("REGISTER");
        output.printf("%s|%s\n",username,password);
        String status = input.readLine();
        switch (status) {
            case "OK":
                System.out.println("Registration successful");
                break;
            case "USERNAME_ALREADY_EXISTS":
                System.out.println("Registration failed: username already exists");
                break;
            case "EMPTY_PASSWORD":
                System.out.println("Registration failed: password can't be empty");
                break;
            case "EMPTY_USERNAME":
                System.out.println("Registration failed: username can't be empty");
                break;
        }
    }

    /**
     * Metodo che invia al server username e password per il login. Se l'username non è presente sul server o se la password è errata, l'utente non viene autenticato.
     * @param username Username dell'utente da autenticare
     * @param password Password dell'utente da autenticare
     * @throws IOException Se non è possibile ricevere la risposta dal server
     */
    private static void login(String username, String password) throws IOException {
        output.println("LOGIN");
        output.printf("%s|%s\n",username,password);
        String status = input.readLine();
        switch (status) {
            case "OK":
                System.out.println("Login successful");
                isLogged = true;
                break;
            case "WRONG_PASSWORD":
                System.out.println("Login failed: wrong password");
                break;
            case "USERNAME_DOES_NOT_EXIST":
                System.out.println("Login failed: username does not exist");
                break;
        }
    }

    /**
     * Metodo che invia al server la richiesta di logout.
     */
    private void logout() {
        output.println("LOGOUT");
        isLogged = false;
    }

    /**
     * Metodo che invia al server la richiesta d'iniziare una nuova partita.
     * @throws IOException Se non è possibile ricevere la risposta dal server
     */
    private static void playWORDLE() throws IOException {
        output.println("PLAY_WORDLE");
        if(input.readLine().equals("WORD_ALREADY_GUESSED")) {
            System.out.println("You can't play twice in a day!");
        } else {
            String stat = input.readLine();
            System.out.println(stat);
            while(!stat.equals("You lose!") && !stat.equals("You win!")) {
                String word = readerFromCLI.readLine();
                sendWord(word);
                stat = input.readLine();
                System.out.println(stat);
            }
        }

    }

    /**
     * Metodo che invia al server la parola inserita dall'utente.
     * @param word Parola inserita dall'utente
     */
    private static void sendWord(String word) {
        output.println(word);
    }

    /**
     * Metodo che invia al server la richiesta di visualizzare le statistiche dell'utente autenticato.
     * @throws IOException Se non è possibile ricevere la risposta dal server
     */
    private static void sendMeStatistics() throws IOException {
        output.println("STATISTICS");
        String stat = input.readLine();
        if(stat.equals("NO_STATISTICS")) {
            System.out.println("No statistics available");
        } else {
            String gamesPlayed = stat.split("\\|")[0];
            String gamesWon = stat.split("\\|")[1];
            String gamesPercentual = stat.split("\\|")[2];
            String streak = stat.split("\\|")[3];
            String streakMax = stat.split("\\|")[4];
            System.out.printf("Games played: %s, Games won:  %s (%s%%) | Streak: %s (max: %s)\n",gamesPlayed, gamesWon, gamesPercentual, streak, streakMax);
            String[] attempts = new String[12];
            for (int i = 5; i < stat.split("\\|").length; i++) { attempts[i-5] = stat.split("\\|")[i]; }
            System.out.println("Attempts: ");
            for (int i = 0; i < attempts.length; i++) {
                String tempAttempts = attempts[i].split("/")[0];
                String tempPercentual = attempts[i].split("/")[1];
                System.out.printf("%d: %s words (%s%%)\n", i + 1, tempAttempts, tempPercentual);
            }
        }
    }

    /**
     * Metodo che invia al server la richiesta di condividere il risultato della parola del giorno dell'utenza autenticata.
     */
    public static void share() {
        output.println("SHARE");
    }

    /**
     * Metodo che visualizza le statistiche sulla parola del giorno di tutti gli utenti che l'hanno indovinata e condivisa.
     */
    public static void showMeSharing() {
        if(stats.isEmpty()) { System.out.println("No one has guessed the word yet!"); }
        else { for (String stat : stats) { System.out.println(stat); } }
    }

    /**
     * Metodo che rimane in ascolto su un gruppo multicast per ricevere le statistiche sulla parola del giorno di tutti gli utenti che l'hanno indovinata e condivisa.
     */
    public void run() {
        try(MulticastSocket multicastSocket = new MulticastSocket(5001)) {
            InetAddress group = InetAddress.getByName("225.255.255.255");
            byte[] buf = new byte[256];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            multicastSocket.joinGroup(group);
            while(true) {
                multicastSocket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                if(received.equals("RESET_LIST")) { stats.clear(); }
                else if(!stats.contains(received)) stats.add(received);
            }
        }catch (IOException e) {
            System.out.println("ERR: Can't connect to multicast group");
            System.exit(1);
        }
    }

    /**
     * Metodo che legge le proprietà del client dal file di configurazione.
     * @throws IOException Se non è possibile leggere il file di configurazione
     * @throws FileNotFoundException Se il file di configurazione non esiste
     */
    public static void readProperties() throws IOException, FileNotFoundException {
        Class<WordleClientMain> cls = WordleClientMain.class;
        InputStream file = cls.getResourceAsStream("WordleClientMain.properties");
        if(file == null) throw new FileNotFoundException("File not found");
        Properties properties = new Properties();
        properties.load(file);
        SERVERADDRESS = properties.getProperty("SERVERADDRESS");
        PORT = Integer.parseInt(properties.getProperty("PORT"));
        file.close();
    }

    /**
     * Metodo che legge username e password dalla riga di comando.
     * @throws IOException Se non è possibile leggere dalla riga di comando
     */
    public static void getUserAndPass() throws IOException {
        readerFromCLI = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Username: ");
        username = readerFromCLI.readLine();
        System.out.print("Password: ");
        password = readerFromCLI.readLine();
        if(username.equals("")) { username = " "; }
        if(password.equals("")) { password = " "; }
    }

    /**
     * Metodo che permette di scegliere se registrarsi o effettuare il login.
     */
    public static void getSelectLogin() {
        int choice;
        while (!isLogged && !done) {
            System.out.println("Select an option:\n1) Register\n2) Login\n3) Exit");
            choice = getChoice();
            switch (choice) {
                case 1:
                    System.out.println("--- REGISTER ---");
                    try { getUserAndPass(); } catch (IOException e) { System.out.println("ERR: Can't read from CLI"); System.exit(1); }
                    try { register(username, password); } catch (IOException e) { System.out.println("ERR: Can't connect to server to register"); System.exit(1); }
                    System.out.println("----------------");
                    break;
                case 2:
                    System.out.println("--- LOGIN ---");
                    try { getUserAndPass(); } catch (IOException e) { System.out.println("ERR: Can't read from CLI"); System.exit(1); }
                    try { login(username, password); } catch (IOException e) { System.out.println("ERR: Can't connect to server to login"); System.exit(1); }
                    System.out.println("-------------");
                    break;
                case 3:
                    System.out.println("--- GOODBYE ---");
                    output.println("EXIT");
                    done = true;
                    System.out.println("---------------");
                    break;
                default:
                    System.out.println("ERR: Invalid choice.");
            }
        }
    }

    /**
     * Metodo che permette di scegliere se giocare o visualizzare le statistiche.
     * @param client Client che è connesso al server
     */
    public static void getSelectGame(WordleClientMain client){
        int choice;
        boolean end = false;
        while(!end){
            System.out.println("Select an option:\n1) Play WORDLE\n2) Share\n3) Show User Statistics\n4) My statistics \n5) Logout");
            choice = getChoice();
            switch (choice) {
                case 1:
                    System.out.println("--- GAME ---");
                    try { playWORDLE(); } catch (IOException e) { System.out.println("ERR: Can't connect to server to play"); System.exit(1); }
                    System.out.println("------------");
                    break;
                case 2:
                    System.out.println("--- SHARE ---");
                    share();
                    System.out.println("-------------");
                    break;
                case 3:
                    System.out.println("--- USER STATISTICS ---");
                    showMeSharing();
                    System.out.println("-----------------------");
                    break;
                case 4:
                    System.out.println("--- MY STATISTICS ---");
                    try { sendMeStatistics(); } catch (IOException e) { System.out.println("ERR: Unable to get statistics."); System.exit(1); }
                    System.out.println("---------------------");
                    break;
                case 5:
                    System.out.println("--- LOGOUT ---");
                    client.logout();
                    end=true;
                    System.out.println("--------------");
                    break;
            }
        }
    }
    public static int getChoice(){
        int select = 0;
        boolean isNumber = false;
        while(!isNumber){
            try {
                select = Integer.parseInt(readerFromCLI.readLine());
                isNumber = true;
            } catch (IOException e) {
                System.out.println("ERR: Can't read from CLI");
                System.exit(1);
            } catch (NumberFormatException e) { System.out.println("Please enter a number"); }
        }
        return select;
    }
    public static void main(String[] args) {
        WordleClientMain client = new WordleClientMain();
        try { readProperties(); } catch (FileNotFoundException e) { System.out.println("ERR: Can't find properties file"); System.exit(1); } catch (IOException e) { System.out.println("ERR: Can't read properties file"); System.exit(1); }
        try{
            socket = new Socket(SERVERADDRESS, PORT);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("ERR: Unable to connect to server.");
            System.exit(1);
        }
        //Thread che si occupa di ricevere le statistiche
        Thread listenMulticast = new Thread(new WordleClientMain());
        listenMulticast.start();
        readerFromCLI = new BufferedReader(new InputStreamReader(System.in));
        Runtime.getRuntime().addShutdownHook(new TerminationHandlerClient(socket, input, output));
        System.out.println("----------------------");
        System.out.println("| Welcome to Wordle! |");
        System.out.println("----------------------");
        while(!done){
            getSelectLogin();
            if(!done) { getSelectGame(client); }
        }
        try { input.close(); } catch (IOException e) { System.out.println("ERR: Can't close input stream"); System.exit(1); }
        output.close();
        try { readerFromCLI.close(); } catch (IOException e) { System.out.println("ERR: Can't close reader from CLI"); System.exit(1); }
        System.exit(0);
    }
}