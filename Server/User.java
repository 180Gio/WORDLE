/**
 * Classe usata per rappresentare un utente e tutte le sue informazioni.
 */
public class User {
    public final String username;
    public final String password;
    public final String salt;
    public int gamesPlayed;
    public int gamesWon;
    public String lastWordGuessed;
    public int streak;
    public int maxStreak;
    public int[] attempts;
    public int lastAttempt;
    public User (String username, String password, String salt) {
        this.username = username;
        this.password = password;
        this.salt = salt;
        this.gamesPlayed = 0;
        this.gamesWon = 0;
        this.lastWordGuessed = " ";
        this.streak = 0;
        this.maxStreak = 0;
        this.attempts = new int[12];
        this.lastAttempt = 0;
    }
}