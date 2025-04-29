package threadLabs;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String args[]) {
        int ileWatkow = 10;
        int ileBiletow = 3;
        
        Bilety pula = new Bilety(ileBiletow);
        Random rand = new Random();
        Task[] watek = new Task[ileWatkow];
        for (int i = 0; i < ileWatkow; i++) {
            watek[i] = new Task("Watek " + i, rand.nextInt(ileBiletow), pula);
            System.out.println("Tworzenie wątku " + watek[i].getName());
        }
        
        for (int i = 0; i < ileWatkow; i++) {
            watek[i].start();
            System.out.println("uruchomienie wątku " + watek[i].getName());
        } 
        
        
        //menu
        Scanner scanner = new Scanner(System.in);
        String input;
        Pattern pattern = Pattern.compile("^(\\d+\\s[sar]|q)$", Pattern.CASE_INSENSITIVE);
        Matcher matcher;
        String[] opcode = {"",""}; //0 - wybrany wątek, 1 - wybrane polecenie do wykonania na wybranym watku
        int wybranyWatek;
        while(true) {
        	//interfejs
        	System.out.println("Menu:\n[nr. watku] s - sprawdz stan watku (state)\n[nr. watku] a - zatrzymaj watek (abort)\n[nr. watku] r - pokaż wynik (result)\nq - wyjdz (quit application)\n");
        	System.out.println("Aktywne Wątki:");
        	for (int i = 0; i < ileWatkow; i++) {
            	System.out.println(i + ". " + watek[i].getName());
        	}
        	//polecenie urzytkownika
            input = scanner.nextLine();
            matcher = pattern.matcher(input);
            if (input.equalsIgnoreCase("q")) {
            	System.out.println("Zamykam aplikację... Przerywam wszystkie wątki.");
            	for (int i = 0; i < ileWatkow; i++) {
            	    watek[i].interrupt();
            	}
            	break; //polecenie wyjścia z aplikacji
            }
            if (!matcher.find()) continue;	//jeśli polecenie jest niepoprawne, zakończ iterację
            opcode = input.split(" ");
            wybranyWatek = Integer.parseInt(opcode[0]);
            if (wybranyWatek < 0 || wybranyWatek > ileWatkow) {
            	System.out.println("Wybrany watek nie istnieje\n");
            	continue;
            }
            
            switch (opcode[1]) {
            case "s":
            	System.out.println(watek[wybranyWatek].getState());
            	break;
            case "a":
            	System.out.println("Przerywam watek " + watek[wybranyWatek].getName());
            	watek[wybranyWatek].interrupt();
            	break;
            case "r":
            	System.out.println(watek[wybranyWatek].getResult());
            	break;
            }
        }
        scanner.close();
    }
}