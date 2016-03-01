package EcoSim;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.io.PrintWriter;
import java.io.FileWriter;
import org.jfree.ui.RefineryUtilities;
import org.jfree.chart.ChartFactory;
import javax.swing.JTabbedPane;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.Dimension;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;

public class Randomize {    

    public static void main(String[] args) {
        int randVal = 1;
        Random simRand = new Random();

        File outputFile = new File("../output.txt");
        PrintWriter writer;
        try {
            writer = new PrintWriter(outputFile);
        } catch (IOException e) {
            System.out.println("Unable to print output, output file not found (" + outputFile.getAbsolutePath() + ")");
            System.out.println(e.getMessage());
            return;
        }

        File speciesFile = Main.getSpeciesFile(args);
        ArrayList<Species> speciesList = null;
        try {
            speciesList = Main.getSpeciesList(speciesFile);
        } catch (FileNotFoundException e) {
	        e.printStackTrace();
            return;
	    } catch (IOException e) {
	        e.printStackTrace();
            return;
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }
        ArrayList<ReturnObject> populations = new ArrayList<ReturnObject>();
        ExecutorService executor = Executors.newFixedThreadPool(30);
        long totalPopsTried = 0;
        while (populations.size() < 100) {
            ArrayList<ArrayList<Species>> newRand = new ArrayList<ArrayList<Species>>();
            for (int i = 0; i < 100; i++) {
                ArrayList<Species> speciesListCopy = new ArrayList<Species>(); 
                for (Species s : speciesList) {
                    speciesListCopy.add(Main.randomizeSpecies(s.makeCopy(), simRand));
                }
                newRand.add(speciesListCopy);
            }
            ArrayList<Simulator> simList = new ArrayList<Simulator>();
            for (ArrayList<Species> list : newRand) {
                ArrayList<Random> randList = new ArrayList<Random>();
                randList.add(new Random(randVal));
	            Simulator sim = new Simulator(list, 0, 0, 2000, randList, writer);
	            simList.add(sim);


            }
            int counter = 0;
            ArrayList<ReturnObject> results = new ArrayList<ReturnObject>();
            try {
                List<Future<ReturnObject>> futureList = executor.invokeAll(simList); 
                for (Future<ReturnObject> f : futureList) {
                    ReturnObject data = f.get();
                    // Evaluate how close the data is to the desired population, then add to TreeMap
                    results.add(data);
                }
            } catch (InterruptedException | ExecutionException e) {
                System.out.println("Something went wrong in Randomize: " + e.getMessage());
                System.exit(0);
            }
            for (ReturnObject r : results) {
                totalPopsTried++;
                if (r.getDuration() >= 1000) {
                    System.out.println("Adding a population with size " + r.getDuration());
                    populations.add(r);
                    writer.println("");
                    writer.println("Population " + (populations.size() - 1));
                    writer.println(r.speciesList); 
                    writer.flush();

                    System.out.println("");
                    System.out.println("Population " + (populations.size() - 1));
                    System.out.println(r.speciesList);

                }
                if (totalPopsTried % 10000 == 0) {
                    System.out.println("Tried " + totalPopsTried + " total populations");
                }
            }
    
        }

        for (ReturnObject r : populations) {
        }

	    JTabbedPane tabbedPane = new JTabbedPane();
        int counter = 0;
        for(ReturnObject r : populations) { 
	        LineChart chart = new LineChart("Population " + counter++, r.totalPopulations, false, "Hours", "Number of organisms"); 	
            tabbedPane.add(chart.title, chart.getContentPane());
        }
	    JFrame frame = new JFrame();
	    frame.setContentPane(tabbedPane);
	    frame.pack();
	    frame.setVisible(true);
    }

}
