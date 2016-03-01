package EcoSim;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class FileReader {
    
    public static LinkedHashMap<String, ArrayList<DataPair>> parsePopulationFile(File f, ArrayList<Species> speciesList) throws IOException, FileNotFoundException, ParseException {
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        BufferedReader br = null;
		fis = new FileInputStream(f);
		bis = new BufferedInputStream(fis);
 		br = new BufferedReader(new InputStreamReader(bis));

        String curSpecies = "";
        LinkedHashMap<String, ArrayList<DataPair>> populationData = new LinkedHashMap<String, ArrayList<DataPair>>();
        for (Species s : speciesList) {
            populationData.put(s.name, new ArrayList<DataPair>());
        }

        String line;
		while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("//")) continue;
            if (line.endsWith(":")) {
                curSpecies = line.substring(0, line.length() - 1);
                if (!populationData.containsKey(curSpecies)) {
                    throw new ParseException("Did not succesfully parse the input file. Unrecognized species: " + curSpecies, 0);
                }
            } else {
                if (curSpecies.isEmpty()) {
                    throw new ParseException("Did not succesfully parse the input file. Data given before species name was processed", 0); 
                }
                String[] split = line.split("=");
		        // The line should be a simple assignment ("x=a"), so the length should be exactly 2
		        if (split.length != 2) { 
                    throw new ParseException("Unrecognized line: " + line, 0); 
                }
		        Integer hour = Integer.parseInt(split[0].trim());
		        Integer population = Integer.parseInt(split[1].trim());
                if (populationData.get(curSpecies).size() == 0 && hour != 0) {
                    throw new ParseException("First data point for species " + curSpecies + " was not at hour 0: " + line, 0);
                }
                populationData.get(curSpecies).add(new DataPair(hour, population));
            }
  		}

        fis.close();
        bis.close();
        br.close();

        return populationData;
    }
    
    public static ArrayList<Species> parseSpeciesFile(File f) throws IOException, FileNotFoundException, ParseException {
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        BufferedReader br = null;
		fis = new FileInputStream(f);
		bis = new BufferedInputStream(fis);
 		br = new BufferedReader(new InputStreamReader(bis));

        Species curSpecies = null;
        ArrayList<Species> speciesList = new ArrayList<Species>();

        String line;
		while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("//")) continue;
            if (!parseVariable(line, curSpecies)) {
                Species newSpecies = parseSpecies(line);
                if (newSpecies == null) {
                    throw new ParseException("Did not succesfully parse the input file", 0);
                }
                speciesList.add(newSpecies);
                curSpecies = newSpecies;
            }
  		}

        fis.close();
        bis.close();
        br.close();

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
            case "mass":
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
            case "reproductionAge":
                s.reproductionAge = Integer.parseInt(value);
                break;
            case "reproductionDistance":
                confirmAuto(s, "reproductionDistance");
                autoS = (Autotroph) s;
                autoS.reproductionAmount = Integer.parseInt(value);
                break;
            case "preyPreference":
                confirmHetero(s, "preyPreference");
                heteroS = (Heterotroph) s;
                split = value.split(":");
                heteroS.preyPreference.put(split[0], Float.parseFloat(split[1]));
                break;
            case "preyList":
                confirmHetero(s, "preyList");
                heteroS = (Heterotroph) s;
                heteroS.preyList.add(value);
                break;
            case "temperatureOptimal":
                s.temperatureOptimal = Float.parseFloat(value);
                break;
            case "temperatureRange":
                s.temperatureRange = Float.parseFloat(value);
                break;
            default:
                throw new ParseException("Parsed a variable (" + variable + ") that was not recognized", 0);
        }
        System.out.println("Variable: " + variable + ", Value: " + value);
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
    
    /**
     * Parse a species name from the line and create an Autotroph or Heterotroph object
     * */
    public static Species parseSpecies(String line) {
        if (!line.endsWith(":")) return null;
        String newSpecies = line.substring(0, line.length() - 1);
        // Asterix at the end of the name to denote an autotroph (which has not prey)
        if (newSpecies.endsWith("*")) {
            System.out.println("New autotroph: " + newSpecies);
            return new Autotroph(newSpecies.substring(0, newSpecies.length() - 1));
        }
        System.out.println("New heterotroph: " + newSpecies);
        return new Heterotroph(newSpecies);


    }
}

