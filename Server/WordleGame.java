import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class WordleGame implements Runnable {
    private final Socket client;
    private final List<User> usersList;
    private BufferedReader input = null;
    private final List<String> words;
    private User userLogged;
    private String username;
    private String password;

    WordleGame(Socket client, List<User> usersList, List<String> words) {
        this.client = client;
        this.usersList = usersList;
        this.words = words;
    }

    /**
     * Metodo che acquisisce dal client username e password dell'utente
     * @throws IOException Se non è possibile leggere dal client
     */
    public void setUserPass() throws IOException {
        String userPass = input.readLine();
        username = userPass.split("\\|")[0];
        password = userPass.split("\\|")[1];
    }

    /**
     * Metodo che cripta la password dell'utente usando un algoritmo di hashing
     * @param password Password dell'utente
     * @param salt Codice generato casualmente usato per codificare la password
     * @return Password criptata
     * @throws InvalidKeySpecException Se la chiave crittografica generata non è valida
     * @throws NoSuchAlgorithmException Se l'algoritmo di hashing non esiste
     */
    public String encryptPassword(String password, String salt) throws InvalidKeySpecException, NoSuchAlgorithmException {
        String algorithm = "PBKDF2WithHmacSHA1";
        int derivedKeyLength = 160;
        int iterations = 20000;
        byte[] saltBytes = Base64.getDecoder().decode(salt);
        KeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, iterations, derivedKeyLength);
        SecretKeyFactory factor = SecretKeyFactory.getInstance(algorithm);
        byte[] encBytes = factor.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(encBytes);
    }

    /**
     * Metodo che data una parola, controlla quali lettere sono presenti nella parola da indovinare e quali sono in posizione corretta.
     * @param word Parola inserita dall'utente da controllare
     * @param wordToGuess Parola da indovinare
     * @return Stringa colorata i cui colori rappresentano: verde, lettere presenti e in posizione corretta; giallo, lettere presenti ma non in posizione corretta; rosso, lettere non presenti.
     */
    public String getColoredWord(String word, String wordToGuess){
        String[] tempColoredWord = new String[word.length()]; //Inizialmente l'array è vuoto
        String coloredWord = "";
        String wordCopy = word;
        String wordToGuessCopy = wordToGuess;
        //Controllo se la lettera i-esima è "verde". Se lo è la aggiungo ad un array di caratteri temporaneo, tempColoredWord, nella posizione i-esima e la modifico in wordCopy e wordToGuessCopy
        StringBuilder sbWordCopy = new StringBuilder(wordCopy);
        StringBuilder sbWordToGuessCopy = new StringBuilder(wordToGuessCopy);
        for(int i = 0; i < word.length(); i++){
            if(word.charAt(i) == wordToGuess.charAt(i)) {
                tempColoredWord[i] = "\u001B[32m" + word.charAt(i) + "\u001B[0m"; //GREEN
                sbWordCopy.setCharAt(i, '1');
                sbWordToGuessCopy.setCharAt(i, '1');
            }
        }
        //Controllo se la lettera i-esima è "gialla". In caso positivo procedo come nel caso della lettera verde.
        //La correttezza deriva dal fatto che se una lettera è verde viene sostituita con un carattere che non è possibile trovare nelle parole del vocabolario, perciò se ho un riscontro della lettera nella parola da indovinare significa che è "gialla".
        for(int i=0;i<wordCopy.length();i++){
            if(wordCopy.charAt(i)!='1'){
                for(int j=0;j<wordToGuessCopy.length();j++){
                    if(tempColoredWord[i]==null && wordCopy.charAt(i)==wordToGuessCopy.charAt(j)){
                        tempColoredWord[i] = "\u001B[33m" + word.charAt(i) + "\u001B[0m"; //YELLOW
                        sbWordCopy.setCharAt(i, '1');
                        sbWordToGuessCopy.setCharAt(j, '1');
                        break;
                    }
                }
            }
        }
        /* Qui vado ad esclusione: dato che la lettera può essere "verde", "gialla" o "rossa" se nell'array temporaneo non ho trovato nulla significa che è "rossa".
        A questo punto sono sicuro che tutte le lettere rimanenti siano "rosse" perchè:
        1) non possono essere "verdi" per la dimostrazione di correttezza precedente
        2) non possono essere "gialle" perchè nel ciclo precedente ho controllato le lettere sostituendole con un "1" e facendo un break nel ciclo per evitare di sostituire più volte la stessa lettera.
        */
        for(int i=0;i< tempColoredWord.length;i++){ if(tempColoredWord[i]==null){ tempColoredWord[i] = "\u001B[31m" + word.charAt(i) + "\u001B[0m"; } }//RED

        //Unisco le posizioni, date da una stringa che indica il colore e dalla lettera, dell'array temporaneo in una stringa
        for (String s : tempColoredWord) { coloredWord += s; }
        return coloredWord;
    }

    /**
     * Metodo che invia su una socket di multicast il messaggio di vittoria dell'utente
     * @param message Messaggio da inoltrare a tutti i client connessi alla rete multicast
     */
    public static void multiCastSender(String message) {
        try(MulticastSocket ms = new MulticastSocket(5001)) {
            InetAddress ia = InetAddress.getByName("225.255.255.255");
            ms.joinGroup(ia);
            byte[] buf = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, ia, 5001); //Preparo il pacchetto da inviare
            ms.send(packet);
        } catch (IOException e) {
            System.out.println("ERR: Can't start MulticastSocket on port: "+5001);
            System.exit(1);
        }
    }

    /**
     * Metodo principale del server in cui vengono accettare e gestite le varie richieste dei client. Vengono stampate anche delle informazioni sullo stato del server.
     */
    @Override
    public void run(){
        System.out.println("Client: "+client);
        try{
            //Creo gli stream di input/output da/per il client
            input = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintWriter output = new PrintWriter(client.getOutputStream(), true);
            //Fino a quando il client non è chiuso gestisco le sue richieste
            while(!client.isClosed()){
                String request = input.readLine();
                System.out.println("Request: "+request);
                switch (request){
                    case "REGISTER":
                        boolean userExists = false;
                        setUserPass();
                        if(username.equals(" ")){
                            output.println("EMPTY_USERNAME");
                            userExists = true;
                        }else{
                            if(password.equals(" ")) {
                                output.println("EMPTY_PASSWORD");
                                userExists = true;
                            }else{
                                for (User user : usersList) {
                                    if(user.username.equals(username)) {
                                        output.println("USERNAME_ALREADY_EXISTS");
                                        userExists = true;
                                    }
                                }
                            }
                        }
                        if(!userExists){
                            //Se l'utente non esiste cripto la sua password e lo aggiungo alla lista degli utenti registrati
                            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
                            byte[] saltByte = new byte[8];
                            random.nextBytes(saltByte);
                            String salt = Base64.getEncoder().encodeToString(saltByte);
                            password = encryptPassword(password, salt);
                            output.println("OK");
                            usersList.add(new User(username, password, salt));
                        }
                        break;
                    case "LOGIN":
                        setUserPass();
                        boolean found = false;
                        for (User user : usersList) {
                            if(user.username.equals(username)) {
                                found = true;
                                if(user.password.equals(encryptPassword(password, user.salt))) {
                                    userLogged = user;
                                    output.println("OK");
                                }
                                else { output.println("WRONG_PASSWORD"); }
                                break;
                            }
                        }
                        if(!found) { output.println("USERNAME_DOES_NOT_EXIST"); }
                        break;
                    case "LOGOUT":
                        username = null;
                        password = null;
                        userLogged = null;
                        break;
                    case "PLAY_WORDLE":
                        //Quando l'utente vuole giocare a Wordle, viene bloccata la parola da indovinare cosi da evitare che cambi mentre l'utente sta giocando
                        WordleServerMain.semaphore.acquire();
                        boolean end = false;
                        //L'utente non può giocare due volte con la stessa parola
                        if(userLogged.lastWordGuessed.equals(WordleServerMain.wordToGuess)) {
                            output.println("WORD_ALREADY_GUESSED");
                            WordleServerMain.semaphore.release();
                            end = true;
                        } else {
                            output.println("CAN_PLAY");
                            userLogged.gamesPlayed++;
                        }
                        if(end) { break; }
                        output.println("WELCOME TO WORDLE! You have 12 attempts to guess the word. Good luck!");
                        int attempts = 0;
                        String word;
                        //Logica effettiva del gioco. Vengono anche aggiornate le statistiche dell'utente
                        while(attempts < 12 && !end){
                            word = input.readLine();
                            System.out.println("Word: "+word);
                            if(word.equals(WordleServerMain.wordToGuess)){
                                output.println("You win!");
                                userLogged.gamesWon++;
                                userLogged.attempts[attempts]++;
                                userLogged.lastAttempt = attempts+1;
                                userLogged.lastWordGuessed = WordleServerMain.wordToGuess;
                                userLogged.streak++;
                                if(userLogged.streak > userLogged.maxStreak) {
                                    userLogged.maxStreak = userLogged.streak;
                                }
                                end = true;
                            }
                            else if(words.contains(word)){
                                attempts++;
                                String coloredWord = getColoredWord(word, WordleServerMain.wordToGuess);
                                output.println(coloredWord);
                            }
                            //In caso l'utente inserisca una parola che non è presente nel vocabolario viene notificato ma non viene contato come tentativo
                            else{ output.println("Invalid word"); }
                        }
                        if(attempts == 12){
                            output.println("You lose!");
                            userLogged.lastWordGuessed = WordleServerMain.wordToGuess;
                            userLogged.streak = 0;
                        }
                        WordleServerMain.semaphore.release();
                        //Alla fine della partita rilascio il semaforo
                        break;
                    case "STATISTICS":
                        String stats;
                        if(userLogged.gamesPlayed == 0){ stats = "NO_STATISTICS"; }
                        else {
                            //Costruisco una stringa contenente tutte le statistiche dell'utente. La stringa viene poi processata dal client
                            stats = userLogged.gamesPlayed + "|" + userLogged.gamesWon + "|" + (userLogged.gamesWon*100)/ userLogged.gamesPlayed + "|"+userLogged.streak+"|"+userLogged.maxStreak;
                            int sumAttempts = Arrays.stream(userLogged.attempts).sum();
                            for (int i = 0; i < userLogged.attempts.length; i++) {
                                stats += "|" + userLogged.attempts[i];
                                if(sumAttempts==0) { stats += "/0";}
                                else { stats += "/" + (userLogged.attempts[i]*100)/ sumAttempts; }
                            }
                        }
                        output.println(stats);
                        break;
                    case "EXIT":
                        client.close();
                        break;
                    case "SHARE":
                        //Invio la notifica solo l'utente ha indovinato la parola
                        if(userLogged.lastWordGuessed.equals(WordleServerMain.wordToGuess)){
                            String message;
                            if(userLogged.lastAttempt == 0) { message = "User " + username + " didn't guessed the word!"; }
                            else { message = "User " + username + " has guessed the word in " + userLogged.lastAttempt + " attempts!"; }
                            System.out.println(message);
                            multiCastSender(message);
                        }
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("ERR: Can't read from client " + client.getInetAddress());
            System.exit(1);
        } catch (InterruptedException e) {
            System.out.println("ERR: Semaphore interrupted");
            System.exit(1);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            System.out.println("ERR: Can't encrypt password");
            System.exit(1);
        }
    }
}
