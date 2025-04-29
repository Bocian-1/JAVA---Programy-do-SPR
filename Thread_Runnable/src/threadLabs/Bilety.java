package threadLabs;

public class Bilety {
    private boolean[] bilet;
    private int ileBiletow;
    
    public Bilety(int ileBiletow) {
        this.ileBiletow = ileBiletow;
        bilet = new boolean[ileBiletow];
        initBilety();
    }
    
    private void initBilety() {
        for (int i = 0; i < ileBiletow; i++) {
            bilet[i] = true;
        }
    }
    
    public void zarezerwuj(int numerBiletu) throws Exception {
        if (!bilet[numerBiletu]) {
            throw new Exception();
        }
        bilet[numerBiletu] = false;
    }
    
    public void zwolnij(int numerBiletu) {
        bilet[numerBiletu] = true;
    }
}