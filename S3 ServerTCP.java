package tcp_v2;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerTCP implements Runnable {
    private ArrayList<ConnectionHandler> connetions;
    private ServerSocket server;
    final int PORT = 5000;
    boolean done = false;
    private ExecutorService pool;
    private static final int MAX_CLIENTS = 250;
    final String BAZA_ODPOWIEDZI = "bazaOdpowiedzi.txt";

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

    private void clearBazaOdpowiedzi() {
        try (FileWriter writer = new FileWriter(BAZA_ODPOWIEDZI)) {
            writer.write(""); // Zapisz pustą zawartość do pliku
        } catch (IOException e) {
            System.out.println("Błąd przy czyszczeniu pliku bazaOdpowiedzi.txt: " + e.getMessage());
        }
    }

    class ConnectionHandler implements Runnable {
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private int clientIndex;
        private boolean testDone = false;
        int questionNumber;
        final String BAZAPYTAN = "bazaPytan.txt";
        final String WYNIKI = "wyniki.txt";

        // Static index to zarządza nadawaniem unikalnych indeksów dla każdego klienta
        private static int staticIndex = 1;

        public ConnectionHandler(Socket socket)
        {
            this.client = socket;
            this.clientIndex = staticIndex;
            staticIndex++;
            questionNumber = 0;
        }

        @Override
        public void run()
        {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                int score = 0;
                Pytanie pytanie;
                while ((pytanie = getNextPytanie()) != null) {
                    sendMessage(pytanie.tresc);
                    sendMessage(pytanie.odpowiedzi);

                    String odp = in.readLine();
                    if (odp == null) break;

                    if (odp.equalsIgnoreCase(pytanie.poprawna)) {
                        score++;
                    }

                    // zapisz odpowiedź do pliku
                    try (FileWriter fw = new FileWriter("bazaOdpowiedzi.txt", true);
                         BufferedWriter bw = new BufferedWriter(fw);
                         PrintWriter pw = new PrintWriter(bw)) {
                        pw.println("Student " + clientIndex + ": " + pytanie.tresc);
                        pw.println("Odpowiedź: " + odp + " (poprawna: " + pytanie.poprawna + ")");
                    }
                }

                sendMessage("Test zakończony. Twój wynik: " + score);
                saveResults(clientIndex, score); // zapisuje wynik do pliku
                shutdown(); // zamknij połączenie
            } catch (Exception e) {
                System.out.println("Błąd w połączeniu z klientem.");
            }
        }

        private void saveResults(int clientIndex, int score)
        {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(WYNIKI, true))) {
                writer.write("Student " + clientIndex + ": " + score + " punktów\n");
            } catch (IOException e) {
                System.out.println("Błąd podczas zapisywania wyników do pliku: " + e.getMessage());
            }
        }

        Pytanie getNextPytanie()
        {
            try (BufferedReader reader = new BufferedReader(new FileReader(BAZAPYTAN))) {
                int liniaNumer = 0;
                String linia;
                int targetStart = questionNumber * 3;
                String tresc = null, odpowiedzi = null, poprawna = null;

                while ((linia = reader.readLine()) != null)
                {
                    if (liniaNumer == targetStart)
                    {
                        tresc = linia;
                    } else if (liniaNumer == targetStart + 1)
                    {
                        odpowiedzi = linia;
                    } else if (liniaNumer == targetStart + 2) {

                        poprawna = linia;
                        break;
                    }
                    liniaNumer++;
                }

                if (tresc == null || odpowiedzi == null || poprawna == null) {
                    return null;
                }

                questionNumber++;
                return new Pytanie(tresc, odpowiedzi, poprawna);
            } catch (IOException e) {
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
        }
    }

    public static void main(String[] args)
    {
        ServerTCP server = new ServerTCP();
        server.run();
    }
}
