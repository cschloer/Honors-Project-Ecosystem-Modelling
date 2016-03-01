package EcoSim;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import org.jfree.ui.RefineryUtilities;
import org.jfree.chart.ChartFactory;
import javax.swing.JTabbedPane;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.Dimension;
import java.util.Random;

public class Resimulate {

    static int lastGeneration = -1;
    static int maxGeneration = -1;
    
    public static void main(String[] args) throws FileNotFoundException, IOException, ParseException {
        /*if (args.length < 1) {
            System.out.println("Required two arguments: filename, generation #. Only recieved " + args.length + " arguments.");
            System.exit(0);
        }*/
        String fileName;
        // Default file name
        if (args.length == 0 || args[0].equals("-")) {
            fileName = "../output.txt";
        } else {
            fileName = args[0];
        }

        File f = new File(fileName); 
        ArrayList<Integer> generationNums = new ArrayList<Integer>();
        // To automatically graph the best one
        if (args.length == 0 || args.length == 1) {
            generationNums.add(-2);
        }
        for (int i = 1; i < args.length;  i++) {
            generationNums.add(Integer.parseInt(args[i]));
        }

        FileInputStream fis = null;
        BufferedInputStream bis = null;
        BufferedReader br = null;
		fis = new FileInputStream(f);
		bis = new BufferedInputStream(fis);
 		br = new BufferedReader(new InputStreamReader(bis));


        ArrayList<Long> randomSeeds = new ArrayList<Long>();
        boolean foundGeneration = false;

        
        String line = br.readLine();
        while(line.startsWith("Random seed:")) {
	        String[] split = line.split(":");
	        if (split.length != 2) throw new ParseException("Incorrect syntax for initial random seed", 0);
	        long randomSeed = Long.parseLong(split[1].trim());
	        System.out.println("Using seed --" + randomSeed + "--");
            randomSeeds.add(randomSeed);

            line = br.readLine();
        }
        fis.close();
        bis.close();
        br.close();
        
	    JTabbedPane tabbedPane = new JTabbedPane();
        for (int generation : generationNums) {
            ArrayList<Species> result = getGeneration(f, generation);
            System.out.println(result);
            ArrayList<Random> randList = new ArrayList<Random>();
            for (long randomSeed : randomSeeds) {
                randList.add(new Random(randomSeed)); 
            }

	        String populationFileName = "../PopulationFile";
	        File populationFile = new File(populationFileName);
	        LinkedHashMap<String, ArrayList<Float>> populationDesired = null;
	        try {
	            populationDesired = Main.estimateFullPop(FileReader.parsePopulationFile(populationFile, result));
	        } catch (IOException | ParseException e) {
		        e.printStackTrace();
	            return;
	        }
            int maxTime = Main.getMaxTime(populationDesired);

            if (generation == -1) generation = maxGeneration;
	        Simulator sim = new Simulator(result, 0, generation, maxTime, randList, null);
	        ReturnObject data = sim.simulate();

		    float eval = Main.evalData(data.totalPopulations, populationDesired); 
            System.out.println("Calculated eval: " + eval);
            LineChart chart;
            if (generation == -2) {
	            chart = new LineChart("Best Generation (generation " + maxGeneration + ") eval:" + eval, data.totalPopulations, false, "Hours", "Number of Organisms");	
            } else {
	            chart = new LineChart("Best Population for Generation " + generation + ", eval: " + eval, data.totalPopulations, false, "Hours", "Number of Organisms"); 	
            }
            tabbedPane.add(chart.title, chart.getContentPane());
            for (String speciesName : data.deathData.keySet()) {
                PieChart pie = new PieChart("Ways of Dying for " + speciesName, data.deathData.get(speciesName));
                tabbedPane.add(pie.title, pie.getContentPane());
            }
            LineChart temperatureChart = new LineChart("Temperature Data", data.temperaturePopulations, true, "Temperature", "# of Organisms Scaled to Average Population"); 
            tabbedPane.add(temperatureChart.title, temperatureChart.getContentPane());
        }
	    JFrame frame = new JFrame();
	    frame.setContentPane(tabbedPane);
	    frame.pack();
	    frame.setVisible(true);

    }

    public static ArrayList<Species> parseGeneration(String line) throws IOException, FileNotFoundException, ParseException {
        Species curSpecies = null;
        ArrayList<Species> speciesList = new ArrayList<Species>();
        String[] speciesSplit = line.split(",");
        for (int i = 0; i < speciesSplit.length; i++) {
            String species = speciesSplit[i];
            String[] varSplit = species.trim().substring(1, species.trim().length() - 1).split("&");
            String[] nameVar = varSplit[0].split("=");
            String[] autoVar = varSplit[1].split("=");
            if (!nameVar[0].trim().equals("Name") || !autoVar[0].trim().equals("Autotroph")) {
                throw new ParseException("First variable for a species was not the name or second variable was not autotroph", 0);
            }
            String speciesName = nameVar[1];
            boolean isAutotroph = Boolean.parseBoolean(autoVar[1].trim());
            Species newSpecies;
            if (isAutotroph) {
                newSpecies = new Autotroph(speciesName);
            } else {
                newSpecies = new Heterotroph(speciesName);
            }
            speciesList.add(newSpecies);
            for (int j = 2; j < varSplit.length; j++) {
                String var = varSplit[j];
                parseVariable(var, newSpecies);
            }
        }
        return speciesList;
    }

    public static ArrayList<Species> getGeneration(File f, int generation) throws FileNotFoundException, ParseException, IOException {

        FileInputStream fis = null;
        BufferedInputStream bis = null;
        BufferedReader br = null;
		fis = new FileInputStream(f);
		bis = new BufferedInputStream(fis);
 		br = new BufferedReader(new InputStreamReader(bis));

        ArrayList<Species> speciesList = new ArrayList<Species>();
        lastGeneration = -1; 
        maxGeneration = -1;
        float maxGenerationVal = -1;

        boolean foundGeneration = false;

        String line;
		while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("//")) continue;
            if (line.startsWith("Best population for generation")) { 
                String[] split = line.split(":");
                float evalVal = Float.parseFloat(split[1]);
                if (evalVal < maxGenerationVal || maxGeneration == -1) {
                    maxGeneration = lastGeneration + 1;
                    maxGenerationVal = evalVal;
                }
                lastGeneration++;
                if (line.startsWith("Best population for generation " + generation)) { 
                    System.out.println("Found line: " + line);
	                foundGeneration = true;
	                line = br.readLine();
	                // Take away the initial brackets
	                speciesList = parseGeneration(line.trim().substring(1, line.length() - 1));
	                break;
                }
            } else if (line.startsWith("Population " + generation)) { 
                foundGeneration = true;
                line = br.readLine();
                System.out.println("");
                System.out.println(line);
                System.out.println("");
                // Take away the initial brackets
                speciesList = parseGeneration(line.trim().substring(1, line.length() - 1));
                break;
            }
  		}

        fis.close();
        bis.close();
        br.close();
        if (generation == -1) {
            return getGeneration(f, lastGeneration);
        }
        if (generation == -2) {
            return getGeneration(f, maxGeneration);
        }
        if (!foundGeneration) {
            throw new ParseException("Generation " + generation + " not found in the inputed file " + f.getName(), 0);
        }
        //System.out.println("Generation: " + lastGeneration);
        return speciesList;
    }

    /**
     * Parse a variable from the line and put it into the inputed species object
     * */
    public static boolean parseVariable(String line, Species s) throws ParseException {
        String[] split = line.split("=");
        // The line should be a simple assignment ("x=a"), so the length should be exactly 2
        if (split.length != 2) return false;
        if (s == null) {
            throw new ParseException("Parsed a variable line before a species had been defined", 0);
        }
        String variable = split[0].trim();
        String value = split[1].trim();
        Heterotroph heteroS;
        Autotroph autoS;

        // Each case is a different variable in the Species/Heterotroph/Autotroph objects
        switch (variable) {
            case "Mass":
                s.mass = Float.parseFloat(value);
                break;
        /** Removing for now because it doesn't properly take into account R species vs K species
         * If each species had an individual "ageImpact" variable that denoted how much being
         * young (or old) negatively impacted the health, this could work - Otherwise the program
         * will artificially decrease the reproduction rate (wheras now it will artificially increase
         * it, which is better because a super lower reproduction rate won't properly model the population
         * when the population is very low <-
            case "reproductionAmount":
                s.reproductionAmount = Integer.parseInt(value);
                break; */
            case "Reproduction Age":
                s.reproductionAge = Integer.parseInt(value);
                break;
            case "Init Num":
                s.initNum = Integer.parseInt(value);
                break;
            case "Max Num":
                s.maxNum = Integer.parseInt(value);
                break;
            case "Reproduction Distance":
                confirmAuto(s, "Reproduction Distance");
                autoS = (Autotroph) s;
                autoS.reproductionAmount = Integer.parseInt(value);
                break;
            case "Prey Preference":
                confirmHetero(s, "Prey Preference");
                heteroS = (Heterotroph) s;
                split = value.split(":");
                heteroS.preyPreference.put(split[0], Float.parseFloat(split[1]));
                break;
            case "Prey Catch Rate":
                confirmHetero(s, "Prey Catch Rate");
                heteroS = (Heterotroph) s;
                split = value.split(":");
                heteroS.preyCatchRate.put(split[0], Double.parseDouble(split[1]));
                break;
            case "Prey List":
                confirmHetero(s, "preyList");
                heteroS = (Heterotroph) s;
                heteroS.preyList.add(value);
                break;
            case "Temperature Optimal":
                s.temperatureOptimal = Float.parseFloat(value);
                break;
            case "Temperature Range":
                s.temperatureRange = Float.parseFloat(value);
                break;
            case "Reproduction Rate":
                s.reproductionRate = Double.parseDouble(value);
                break;
            case "Death Rate":
                s.deathRate = Float.parseFloat(value);
                break;
            case "Curve Steepness":
                s.curveSteepness = Integer.parseInt(value);
                break;
            case "Hunger Rate":
                confirmHetero(s, "Prey Preference");
                heteroS = (Heterotroph) s;
                heteroS.hungerRate = Double.parseDouble(value);
                break;
            case "Hunger Ceiling":
                confirmHetero(s, "Prey Preference");
                heteroS = (Heterotroph) s;
                heteroS.hungerCeiling = Double.parseDouble(value);
                break;
            default:
                throw new ParseException("Parsed a variable (" + variable + ") that was not recognized", 0);
        }
//        System.out.println("Variable: " + variable + ", Value: " + value);
        return true;


    }

    public static void confirmHetero(Species s, String var) throws ParseException {
        if (!(s instanceof Heterotroph)) {
            throw new ParseException("Parsed a heterotroph-only variable (" + var + ") for an autotroph (" + s.name + ")", 0);
        }
    }

    public static void confirmAuto(Species s, String var) throws ParseException {
        if (!(s instanceof Autotroph)) {
            throw new ParseException("Parsed a autotroph-only variable (" + var + ") for a heterotroph (" + s.name + ")", 0);
        }

    }
    

}
