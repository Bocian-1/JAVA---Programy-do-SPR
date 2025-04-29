import java.util.concurrent.Callable;

public class PalindromeThread implements Callable<Boolean> {
    private final String name;
    private final String sample;
    private final int size;

    public PalindromeThread(String name, String text) {
        this.name = name;
        this.sample = text.toLowerCase();
        this.size = text.length();
    }

    public String getName() {
        return name;
    }

    @Override
    public Boolean call() throws Exception {
        for (int i = 0; i < size / 2; i++) {
            // Check for interruption
            if (Thread.currentThread().isInterrupted()) {
                System.out.println(name + " został przerwany.");
                throw new InterruptedException("Zadanie przerwane");
            }

            Thread.sleep(1000);

            if (sample.charAt(i) != sample.charAt(size - 1 - i)) {
                return false;
            }
        }
        return true;
    }
}
