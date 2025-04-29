import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.regex.*;

public class RozwiazywanieRownan {
    private static final String INPUT_FILE = "rownania.txt";
    private static final Lock lock = new ReentrantLock();
    private static final BlockingQueue<Rownanie> queue = new LinkedBlockingQueue<>();

    public static void main(String[] args) throws IOException, InterruptedException {
        ExecutorService readerExecutor = Executors.newFixedThreadPool(2);
        ExecutorService solverExecutor = Executors.newFixedThreadPool(2);

        List<String> lines = new ArrayList<>();

        // Czytanie pliku przez BufferedReader
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(INPUT_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }

        List<String> updatedLines = new ArrayList<>(lines);

        // Wątki czytające
        for (int i = 0; i < lines.size(); i++) {
            int lineNumber = i;
            readerExecutor.submit(() -> {
                String equation = updatedLines.get(lineNumber);
                try {
                    queue.put(new Rownanie(lineNumber, equation));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Wątki liczące
        for (int i = 0; i < lines.size(); i++) {
            solverExecutor.submit(() -> {
                try {
                    Rownanie entry = queue.take();
                    SolveTask task = new SolveTask(entry, updatedLines, lock);
                    FutureTask<Void> futureTask = new FutureTask<>(task) {
                        @Override
                        protected void done() {
                            lock.lock();
                            try {
                                System.out.println("Równanie rozwiązane: " + updatedLines.get(entry.lineNumber));
                                Files.write(Paths.get(INPUT_FILE), updatedLines, StandardOpenOption.TRUNCATE_EXISTING);
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                lock.unlock();
                            }
                        }
                    };
                    futureTask.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        readerExecutor.shutdown();
        readerExecutor.awaitTermination(1, TimeUnit.MINUTES);

        solverExecutor.shutdown();
        solverExecutor.awaitTermination(1, TimeUnit.MINUTES);
    }
}

class Rownanie {
    public final int lineNumber;
    public final String equation;

    public Rownanie(int lineNumber, String equation) {
        this.lineNumber = lineNumber;
        this.equation = equation;
    }
}

class SolveTask implements Callable<Void> {
    private final Rownanie entry;
    private final List<String> updatedLines;
    private final Lock lock;

    public SolveTask(Rownanie entry, List<String> updatedLines, Lock lock) {
        this.entry = entry;
        this.updatedLines = updatedLines;
        this.lock = lock;
    }

    @Override
    public Void call() {
        try {
            String cleanedEquation = entry.equation.replaceAll("=", "").trim();
            List<String> postfix = toPostfix(cleanedEquation);
            double result = evaluatePostfix(postfix);

            lock.lock();
            try {
                updatedLines.set(entry.lineNumber, entry.equation + " " + result);
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<String> toPostfix(String expr) {
        List<String> output = new ArrayList<>();
        Deque<String> stack = new ArrayDeque<>();
        Pattern pattern = Pattern.compile("\\d+|[+\\-*/^()]");
        Matcher matcher = pattern.matcher(expr);

        while (matcher.find()) {
            String token = matcher.group();
            if (token.matches("\\d+")) {
                output.add(token);
            } else if (token.equals("(")) {
                stack.push(token);
            } else if (token.equals(")")) {
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    output.add(stack.pop());
                }
                stack.pop();
            } else {
                while (!stack.isEmpty() && precedence(stack.peek()) >= precedence(token)) {
                    output.add(stack.pop());
                }
                stack.push(token);
            }
        }

        while (!stack.isEmpty()) {
            output.add(stack.pop());
        }

        return output;
    }

    private double evaluatePostfix(List<String> postfix) {
        Deque<Double> stack = new ArrayDeque<>();

        for (String token : postfix) {
            if (token.matches("\\d+")) {
                stack.push(Double.parseDouble(token));
            } else {
                double b = stack.pop();
                double a = stack.pop();
                switch (token) {
                    case "+": stack.push(a + b); break;
                    case "-": stack.push(a - b); break;
                    case "*": stack.push(a * b); break;
                    case "/": stack.push(a / b); break;
                    case "^": stack.push(Math.pow(a, b)); break;
                }
            }
        }
        return stack.pop();
    }

    private int precedence(String operator) {
        switch (operator) {
            case "+": case "-": return 1;
            case "*": case "/": return 2;
            case "^": return 3;
            default: return -1;
        }
    }
}
