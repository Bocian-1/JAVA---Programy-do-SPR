package threadLabs;
import java.util.Random;

public class Task implements Runnable{
    private Thread t;
    private String name;
    private int nrBiletu;
    private Bilety pulaBiletow;
    private boolean rezerwacja;
    private Random rand;
    
    Task(String name, int nrBiletu, Bilety pulaBiletow) {
        this.name = name;
        this.nrBiletu = nrBiletu;
        this.pulaBiletow = pulaBiletow;
        rand = new Random();
        rezerwacja = false;
    }
    
    public String getName() {
        return name;
    }
    
    public String getState() {
    	return (isAlive() ? "aktywny" : "przerwany");
    }
    
    public String getResult() {
    	return (rezerwacja ? "zarezerwowany" : "wolny");
    }
    
    @Override
    public void run() {
        while(true) {
            try {
                pulaBiletow.zarezerwuj(nrBiletu);
                rezerwacja = true;
                //System.out.println("Wątek " + name + " zarezerwowal bilet nr " + nrBiletu);
            } catch(Exception e) {
                //System.out.println("Wątek " + name + " czeka na zwolnienie biletu nr " + nrBiletu);
            }
            
            try {
                Thread.sleep(rand.nextInt(5000)+1000);
            } catch (InterruptedException e) {
                break;
            }

            
            if (rezerwacja) {
                pulaBiletow.zwolnij(nrBiletu);
                rezerwacja = false;
                //System.out.println("Wątek " + name + " zwolnił rezerwację biletu nr " + nrBiletu);
            }

        }
    }
    
    public void start() {
        t = new Thread(this, name);
        t.start();
    }
    
    public void interrupt() {
        t.interrupt();
    }

    public boolean isAlive() {
        return t.isAlive();
    }
}