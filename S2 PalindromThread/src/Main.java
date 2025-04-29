import java.util.*;
import java.util.concurrent.*;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<FutureTask<Boolean>> taskList = new ArrayList<>();
        List<String> taskNames = new ArrayList<>();

        boolean running = true;
        while (running) {
            System.out.println("\n--- MENU ---");
            System.out.println("1. Dodaj nowe zadanie");
            System.out.println("2. Wyświetl listę zadań");
            System.out.println("3. Pokaż stan zadania");
            System.out.println("4. Anuluj zadanie");
            System.out.println("5. Pokaż wynik zadania");
            System.out.println("6. Zakończ");
            System.out.print("Wybierz opcję: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> {
                    System.out.print("Podaj nazwę zadania: ");
                    String name = scanner.nextLine();
                    System.out.print("Podaj tekst do sprawdzenia: ");
                    String text = scanner.nextLine();
                    PalindromeThread task = new PalindromeThread(name, text);
                    FutureTask<Boolean> futureTask = new FutureTask<>(task) {
                        // Dodajemy słuchacza
                        @Override
                        protected void done()
                        {
                            try
                            {
                                if (isCancelled())
                                {
                                    System.out.println("Zadanie \"" + name + "\" zostało anulowane.");
                                } else
                                {
                                    Boolean result = get(); // Pobierz wynik po zakończeniu
                                    System.out.println("Zadanie \"" + name + "\" zakończone. Wynik: " +
                                            (result ? "Palindrom" : "Nie palindrom"));
                                }
                            }
                            catch (InterruptedException | ExecutionException e)
                            {
                                System.out.println("Błąd wykonania zadania: " + e.getMessage());
                            }
                        }
                    };
                    executor.execute(futureTask); // Uruchom zadanie
                    taskList.add(futureTask); // Dodaj do listy
                    taskNames.add(name); // Dodaj nazwę zadania
                }
                case "2" -> {
                    if (taskList.isEmpty()) {
                        System.out.println("Brak zadań.");
                    } else {
                        for (int i = 0; i < taskList.size(); i++) {
                            System.out.println(i + ". " + taskNames.get(i));
                        }
                    }
                }
                case "3" -> {
                    System.out.print("Podaj numer zadania: ");
                    int id = Integer.parseInt(scanner.nextLine());
                    if (id >= 0 && id < taskList.size()) {
                        FutureTask<Boolean> f = taskList.get(id);
                        if (f.isCancelled()) {
                            System.out.println("Zadanie zostało anulowane.");
                        } else if (f.isDone()) {
                            System.out.println("Zadanie zakończone.");
                        } else {
                            System.out.println("Zadanie jest w trakcie wykonywania.");
                        }
                    } else {
                        System.out.println("Nieprawidłowy numer zadania.");
                    }
                }
                case "4" -> {
                    System.out.print("Podaj numer zadania do anulowania: ");
                    int id = Integer.parseInt(scanner.nextLine());
                    if (id >= 0 && id < taskList.size()) {
                        boolean result = taskList.get(id).cancel(true);
                        System.out.println(result ? "Zadanie anulowane." : "Nie można anulować zadania.");
                    } else {
                        System.out.println("Nieprawidłowy numer zadania.");
                    }
                }
                case "5" -> {
                    System.out.print("Podaj numer zadania: ");
                    int id = Integer.parseInt(scanner.nextLine());
                    if (id >= 0 && id < taskList.size()) {
                        FutureTask<Boolean> future = taskList.get(id);
                        try {
                            Boolean result = future.get();
                            System.out.println("Wynik: " + (result ? "Palindrom" : "Nie palindrom"));
                        } catch (CancellationException e) {
                            System.out.println("Zadanie zostało anulowane.");
                        } catch (ExecutionException e) {
                            System.out.println("Błąd wykonania: " + e.getCause().getMessage());
                        } catch (InterruptedException e) {
                            System.out.println("Zadanie zostało przerwane.");
                        }
                    } else {
                        System.out.println("Nieprawidłowy numer zadania.");
                    }
                }
                case "6" -> {
                    running = false;
                    executor.shutdownNow();
                    System.out.println("Zamykam program.");
                }
                default -> System.out.println("Nieprawidłowa opcja.");
            }
        }
    }
}
