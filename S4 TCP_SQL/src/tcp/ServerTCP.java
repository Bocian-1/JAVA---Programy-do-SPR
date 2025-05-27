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
