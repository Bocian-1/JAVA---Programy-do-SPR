package tcp;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ServerTCP implements Runnable {
    private ArrayList<ConnectionHandler> connetions;
    private ServerSocket server;
    final int PORT = 5000;
    boolean done = false;
    private ExecutorService pool;
    private static final int MAX_CLIENTS = 250;
    
    //SQL
    private final String DB_URL = "jdbc:mysql://localhost:3306/quiz";
    private final String DB_USER = "root";
    private final String DB_PASS = "";
    
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Nie znaleziono sterownika MySQL!");
        }
    }
    
    
    public ServerTCP() {
        connetions = new ArrayList<>();
        initializeDatabase();
    }

    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/", DB_USER, DB_PASS);
             Statement stmt = conn.createStatement()) {

            // Create the quiz database if it doesn't exist
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS quiz");
            stmt.executeUpdate("USE quiz");

            // Now connect to the quiz DB and set up tables
            try (Connection quizConn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 Statement quizStmt = quizConn.createStatement()) {

                // Create tables
                quizStmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS pytania (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        tresc TEXT NOT NULL,
                        odpowiedzi TEXT NOT NULL,
                        poprawna VARCHAR(255) NOT NULL
                    )
                """);

                quizStmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS odpowiedzi (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        klient_id INT,
                        pytanie TEXT,
                        odpowiedz TEXT,
                        poprawna TEXT
                    )
                """);

                quizStmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS wyniki (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        klient_id INT,
                        wynik INT
                    )
                """);

                // Insert test questions only if table is empty
                ResultSet rs = quizStmt.executeQuery("SELECT COUNT(*) FROM pytania");
                rs.next();
                int count = rs.getInt(1);

                if (count == 0) {
                	quizStmt.executeUpdate("""
                		    INSERT INTO pytania (tresc, odpowiedzi, poprawna) VALUES
                		    ('Ile wynosi wysokość nad poziomem morza Morskiego Oka?', 'a) Chrzest Polski\\nb) Bitwa pod Grunwaldem\\nc) Upadek Komuny Paryskiej', 'b'),
                		    ('Amerykańskie taryfy na Chiny?', 'a) 145%\\nb) 10%\\nc) ???', 'c'),
                		    ('Które państwo ma granice londowe z dokładnie 2 innymi państwami?', 'a) Dżibuti\\nb) Bhutan\\nc) Brunei', 'b'),
                		    ('Prędkość transferu danych w USB 3.1?', 'a) 5GBit/s\\nb) 0.5GBit/s\\nc) 10GBit/s', 'a'),
                		    ('Który pierwiastek chemiczny ma symbol Fe?', 'a) Fluor\\nb) Żelazo\\nc) Fenyloalanina', 'b'),
                		    ('W którym roku rozpoczęła się II wojna światowa?', 'a) 1937\\nb) 1939\\nc) 1941', 'b'),
                		    ('Która planeta w Układzie Słonecznym jest największa?', 'a) Saturn\\nb) Jowisz\\nc) Uran', 'b'),
                		    ('Kto napisał „Zbrodnię i karę”?', 'a) Tołstoj\\nb) Czechow\\nc) Dostojewski', 'c'),
                		    ('Jak nazywa się stolica Kanady?', 'a) Toronto\\nb) Ottawa\\nc) Vancouver', 'b'),
                		    ('W jakim języku mówi się w Brazylii?', 'a) Hiszpańskim\\nb) Portugalskim\\nc) Francuskim', 'b'),
                		    ('Który ocean jest największy?', 'a) Atlantycki\\nb) Indyjski\\nc) Spokojny (Pacyfik)', 'c'),
                		    ('Jaką jednostką mierzy się natężenie prądu?', 'a) Wat\\nb) Amper\\nc) Volt', 'b'),
                		    ('Co to jest haiku?', 'a) Styl malarski\\nb) Forma japońskiej poezji\\nc) Rodzaj ceremonii herbacianej', 'b'),
                		    ('Kto był pierwszym człowiekiem w kosmosie?', 'a) Neil Armstrong\\nb) Jurij Gagarin\\nc) John Glenn', 'b'),
                		    ('Jak nazywa się najdłuższa rzeka świata?', 'a) Amazonka\\nb) Nil\\nc) Jangcy', 'a'),
                		    ('Ile trwała wojna stuletnia?', 'a) 100 lat\\nb) 116 lat\\nc) 98 lat', 'b'),
                		    ('Który z podanych krajów nie leży w Europie?', 'a) Albania\\nb) Armenia\\nc) Andora', 'b'),
                		    ('Jakie zwierzę widnieje w godle Polski?', 'a) Orzeł\\nb) Lew\\nc) Niedźwiedź', 'a'),
                		    ('Który język programowania jest nazywany „językiem internetu”?', 'a) Python\\nb) JavaScript\\nc) C++', 'b'),
                		    ('Jaką walutą posługują się Węgrzy?', 'a) Forint\\nb) Euro\\nc) Korona', 'a')
                		""");

                    System.out.println("Dodano przykładowe pytania.");
                }

            }

        } catch (SQLException e) {
            System.err.println("Błąd podczas inicjalizacji bazy danych: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(PORT);
            pool = Executors.newCachedThreadPool();
            while (!done) {
                Socket client = server.accept();
                if (connetions.size() >= MAX_CLIENTS) {
                    PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                    out.println("Serwer osiągnął limit połączeń. Spróbuj później.");
                    client.close();
                    continue;
                }
                // Tworzymy unikalny indeks dla każdego klienta za pomocą statycznego 'index'
                ConnectionHandler handler = new ConnectionHandler(client);
                connetions.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
            shutdown();
        }
    }

    public void broadcast(String message) {
        for (ConnectionHandler connection : connetions) {
            if (connection != null) {
                connection.sendMessage(message);
            }
        }
    }

    public void shutdown() {
        try {
            done = true;
            if (!server.isClosed()) {
                server.close();
            }
            for (ConnectionHandler handler : connetions) {
                handler.shutdown();
            }
        } catch (Exception e) {
            // ignore
        }
    }

    class ConnectionHandler implements Runnable {
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private int clientIndex;
        private boolean testDone = false;
        int questionNumber;

        // Static index zarządza nadawaniem unikalnych indeksów dla każdego klienta
        private static int staticIndex = 1;

        public ConnectionHandler(Socket socket)
        {
            this.client = socket;
            this.clientIndex = staticIndex;
            staticIndex++;
            questionNumber = 0;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                int score = 0;
                Pytanie pytanie;
                while ((pytanie = getNextPytanie()) != null) {
                    sendMessage(pytanie.toString());
                    String odp = in.readLine(); // odpowiedz klienta

                    // test poprwawności odpowiedzi
                    if (odp != null && odp.equalsIgnoreCase(pytanie.poprawna)) {
                        score++;
                    }

                    // Zapis do bazy danych
                    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                        PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO odpowiedzi (klient_id, pytanie, odpowiedz, poprawna) VALUES (?, ?, ?, ?)"
                        );
                        stmt.setInt(1, clientIndex);
                        stmt.setString(2, pytanie.tresc);
                        stmt.setString(3, odp);
                        stmt.setString(4, pytanie.poprawna);
                        stmt.executeUpdate();
                    } catch (SQLException e) {
                        System.out.println("Błąd zapisu odpowiedzi: " + e.getMessage());
                    }
                }

                sendMessage("Test zakończony. Twój wynik: " + score);
                saveResults(clientIndex, score); // zapisuje wynik do bazy
                shutdown(); // zamknij połączenie
            } catch (Exception e) {
                System.out.println("Błąd w połączeniu z klientem.");
            }
        }


        private void saveResults(int clientIndex, int score) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO wyniki (klient_id, wynik) VALUES (?, ?)"
                );
                stmt.setInt(1, clientIndex);
                stmt.setInt(2, score);
                stmt.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Błąd zapisu wyniku: " + e.getMessage());
            }
        }

        private Pytanie getNextPytanie() {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM pytania ORDER BY id LIMIT 1 OFFSET ?"
                );
                stmt.setInt(1, questionNumber);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    questionNumber++;
                    return new Pytanie(
                        rs.getString("tresc"),
                        rs.getString("odpowiedzi"),
                        rs.getString("poprawna")
                    );
                } else {
                    return null; // koniec pytań
                }
            } catch (SQLException e) {
                System.out.println("Błąd bazy danych (getNextPytanie): " + e.getMessage());
                return null;
            }
        }


        public void sendMessage(String message)
        {
            out.println(message);
        }

        public void shutdown() {
            try {
                in.close();
                out.close();
                if (!client.isClosed())
                {
                    client.close();
                }
            } catch (Exception e) {
                // ignore
            }
        }

        class Pytanie
        {
            String tresc;
            String odpowiedzi;
            String poprawna;

            public Pytanie(String tresc, String odpowiedzi, String poprawna)
            {
                this.tresc = tresc;
                this.odpowiedzi = odpowiedzi;
                this.poprawna = poprawna;
            }
            
            public String toString() {
                return tresc + "\n" + odpowiedzi;
            }
        }
    }

    public static void main(String[] args)
    {
        ServerTCP server = new ServerTCP();
        server.run();
    }
}
