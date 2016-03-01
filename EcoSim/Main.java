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

public class Main {    
    static final int simWidth = 1;
    static final int simHeight = 5;
    static final int simLength = 5;
    static final float initWaterTemp = (float) 17.5;
    static final float summerHigh = 35;
    static final float winterHigh  = 0;
    static final int numYears = 1;
    // Number of populations within each generation of the genetic algorithm
    static int numPopulations = 100;
    // Number of populations per generation that are completely randomly generated
    static int numRandom = 10;
    // Number of populations to keep from each generation
    static int numKeepGeneration = 15;
    // Number of children per population kept from the prev generation that are exact copies + mutation 
    static int numMutate = 0;
    // Number of children created by crossing the top "numKeepGeneration" species
    static int numBreed = 90;
    // numBreed + numMutate * numKeepGeneration + numRandom = numPopulations
    static int numGenerations = 2000;
    // Rate of mutation
    static final float mutationRate = .25f;
    // The number of extra seeds that are used to average the eval of a given simulation
    static final int numExtraSeeds = 0;
    // Whether we're just testing a single simulation
    static final boolean testOne = false;
    // Whether all of the ecosystems are printed to file or just the best for each generations
    static boolean printAll = false;

    private static ArrayList<ArrayList<Species>> genePopulations;

    public static void main(String[] args) {
        runSimulation(args);
    }

    public static ArrayList<ReturnObject> runSimulation(String[] args) {
        if (numBreed + numMutate * numKeepGeneration + numRandom != numPopulations) {
            System.out.println("Combination of inputed variables do not add up correctly");
            System.exit(0);
        }
        if (testOne) {
                numPopulations = 1;
                numGenerations = 1;       
                numKeepGeneration = 1;
                numMutate = 0;
                numBreed = 1;
                numRandom = 0;
        }
        File outputFile = new File("../output.txt");
        PrintWriter writer;
        try {
            writer = new PrintWriter(outputFile);
        } catch (IOException e) {
            System.out.println("Unable to print output, output file not found (" + outputFile.getAbsolutePath() + ")");
            System.out.println(e.getMessage());
            return null;
        }
        try {
     
            /*********************************
             Parse the input file
            *********************************/
            File speciesFile = getSpeciesFile(args);
            File populationFile = getPopulationFile(args);
            ArrayList<Species> speciesList = null;
            LinkedHashMap<String, ArrayList<Float>> populationDesired = null;
            try {
                speciesList = getSpeciesList(speciesFile);
                populationDesired = getPopulationDesired(populationFile, speciesList);
            } catch (FileNotFoundException e) {
    	        e.printStackTrace();
                return null;
    	    } catch (IOException e) {
    	        e.printStackTrace();
                return null;
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }
            int maxTime = getMaxTime(populationDesired);

            long seed = System.currentTimeMillis();
            if (testOne) {
                seed = 19l;
            }
            writer.println("Random seed: " + seed);
            Random simRand = new Random(seed);

            ArrayList<Long> seedList = new ArrayList<Long>();
            seedList.add(seed);
            for (int i = 0; i < numExtraSeeds; i++) {
                writer.println("Random seed: " + (seed + 1 + i));
                seedList.add(seed + 1 + i);
            }

            /**
             * Create initial populations randomly 
             */
            genePopulations = new ArrayList<ArrayList<Species>>();
            for (int i = 0; i < numPopulations; i++) {
                ArrayList<Species> speciesListCopy = new ArrayList<Species>(); 
                for (Species s : speciesList) {
                    speciesListCopy.add(randomizeSpecies(s.makeCopy(), simRand));
                }
                genePopulations.add(speciesListCopy);
            }
    
            /**
             * Run through generations!!!
             * */

            ExecutorService executor = Executors.newFixedThreadPool(30);
            ArrayList<ReturnObject> recentPopulations = null;
            // # of Generations
            for (int i = 0 ; i < numGenerations; i++) {
                long startTime = System.currentTimeMillis();
                System.out.println("On generation " + i + "...");
                ArrayList<Simulator> simList = new ArrayList<Simulator>();
                for (int j = 0; j < numPopulations; j++) { 
                    ArrayList<Random> randList = new ArrayList<Random>();
                    for (long s : seedList) {
	                    randList.add(new Random(s));
                    }
	                Simulator sim = new Simulator(genePopulations.get(j), j, i, maxTime, randList, null);
	                simList.add(sim);
                } 
                TreeMap<EvalObject, ReturnObject> generation = new TreeMap<EvalObject, ReturnObject>();
                try {
	                List<Future<ReturnObject>> futureList = executor.invokeAll(simList); 
	                for (Future<ReturnObject> f : futureList) {
	                    ReturnObject data = f.get();
	                    // Evaluate how close the data is to the desired population, then add to TreeMap
                        float eval = evalData(data.totalPopulations, populationDesired);
		                data.eval = new EvalObject(eval, data.population);
                        generation.put(data.eval, data);
                        if (testOne) {
                            System.out.println("Number of time steps: " + data.totalPopulations.get("species1").size());
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    System.out.println("Something went wrong in Main: " + e.getMessage());
                    System.exit(0);
                }

                System.out.println("Speed of generation " + i + ": " + ((float) (System.currentTimeMillis() - startTime) / 1000) + " seconds");
                recentPopulations = new ArrayList<ReturnObject>(generation.values());
                ReturnObject topData = recentPopulations.get(0);
                writer.println("");
                writer.println("Best population for generation " + i + ", eval: " + topData.eval);
                writer.println(topData.speciesList);
                writer.flush();
                ArrayList<ReturnObject> parentPopulations = getParentPopulation(recentPopulations, simRand);
                genePopulations = swapAndMutate(parentPopulations, simRand);
                if (genePopulations.size() != numPopulations) {
                    System.out.println("Size of new group of populations is not " + numPopulations + " but rather " + genePopulations.size());
                    System.exit(0);
                }
            }
            executor.shutdown();
            return recentPopulations;
    
        } finally {
            writer.close();
        }
    }

    public static File getSpeciesFile(String[] args) {
        File speciesFile = null;
        if (args.length > 1) {
            System.out.println("Attempting to parse the species file: " + args[0]);
            speciesFile = new File(args[0]);
        } else {
            String speciesFileName = "../SpeciesFile";
            System.out.println("Using the default species file " + speciesFileName);
            speciesFile = new File(speciesFileName);
        }
        return speciesFile;
    }

    public static File getPopulationFile(String[] args) {
        File populationFile = null;
        if (args.length > 1) {
            System.out.println("Attempting to parse the population data file: " + args[1]);
            populationFile = new File(args[1]);
        } else {
            String populationFileName = "../PopulationFile";
            System.out.println("Using the default population data file " + populationFileName);
            populationFile = new File(populationFileName);
        }
        return populationFile;
    }

    public static ArrayList<Species> getSpeciesList(File speciesFile) throws IOException, ParseException {
            return FileReader.parseSpeciesFile(speciesFile);
    }

    public static LinkedHashMap<String, ArrayList<Float>> getPopulationDesired(File populationFile, ArrayList<Species> speciesList) throws IOException, ParseException {
            LinkedHashMap<String, ArrayList<DataPair>> populationFileData = FileReader.parsePopulationFile(populationFile, speciesList);
            for (Species s : speciesList) {
                s.initNum = populationFileData.get(s.name).get(0).population;
                int max = 0;
                for (DataPair dp : populationFileData.get(s.name)) {
                    if (dp.population > max) max = dp.population;
                }
                s.maxNum = max * 5;
            }
            return estimateFullPop(populationFileData);
    }

    public static int getMaxTime(LinkedHashMap<String, ArrayList<Float>> populationDesired) {
        int curMax = 0;
        for (ArrayList<Float> f : populationDesired.values()) {
            if (curMax < f.size() - 1) {
                curMax = f.size() - 1;
            }
        }
        return curMax;
    }


    /**
    * Returns a value ranking the closeness of this population data to the desired data
    * closer to 0 is better
    * TODO: Update this method for more sophistication
    */
    public static float evalData(LinkedHashMap<String, ArrayList<Float>> actualPopulation, LinkedHashMap<String, ArrayList<Float>> desiredPopulation) {
        // measure the difference in area between two graphs
        float totalDiff = 0;
        for (String species : desiredPopulation.keySet()) {
            float speciesDiff = 0;
            float avgPop = 0;
            ArrayList<Float> speciesPopulation = actualPopulation.get(species);
            ArrayList<Float> speciesPopulationDesired = desiredPopulation.get(species);
            for (int hour = 0; hour < speciesPopulationDesired.size(); hour++) {
                // Species went extinct before this hour happened
                if (speciesPopulation.size() <= hour) {
                    speciesDiff += speciesPopulationDesired.get(hour);
                } else { 
                    speciesDiff += Math.abs(speciesPopulationDesired.get(hour) - speciesPopulation.get(hour));
                }
                avgPop += speciesPopulationDesired.get(hour);
            }

            avgPop = avgPop / speciesPopulationDesired.size();
            // Scale it based on size
            totalDiff += speciesDiff / avgPop;
        }

        // Measures the difference in location of the derivative's x-intercept, AKA where the graph changes from positive to negative slope and vice versa
        //totalDiff = 0;
        for (String species : desiredPopulation.keySet()) {
            float speciesDiff = 0;
            float avgPop = 0;
            ArrayList<DataPair> optIntercepts = new ArrayList<DataPair>();
            ArrayList<DataPair> actualIntercepts = new ArrayList<DataPair>();

            float optPrev = 0;
            float actualPrev = 0;

            boolean positiveSlopeOpt = true;
            boolean positiveSlopeActual = true;

            ArrayList<Float> speciesPopulation = actualPopulation.get(species);
            ArrayList<Float> speciesPopulationDesired = desiredPopulation.get(species);
            for (int hour = 0; hour < speciesPopulationDesired.size(); hour++) {
                // Check if changed direction for actual 
                if (speciesPopulation.size() > hour) {
                    float actualNew = speciesPopulation.get(hour);
                    if (positiveSlopeActual) {
                    // If it's been going, check if it's no longer going up 
                        if ((actualNew - actualPrev) < 0) {
                            actualIntercepts.add(new DataPair(hour, positiveSlopeActual));
                            positiveSlopeActual = false;
                        }   
                    } else {
                    // Check if it's no longer going down
                        if ((actualPrev - actualNew) < 0) {
                            actualIntercepts.add(new DataPair(hour, positiveSlopeActual));
                            positiveSlopeActual = true;
                        }   
                    }
                    actualPrev = actualNew;
                }

                // Now do the same thing for the opt intercept
                float optNew = speciesPopulationDesired.get(hour);
                if (positiveSlopeOpt) {
                // If it's been going, check if it's no longer going up 
                    if ((optNew - optPrev) < 0) {
                        optIntercepts.add(new DataPair(hour, positiveSlopeOpt));
                        positiveSlopeOpt = false;
                    }   
                } else {
                // Check if it's no longer going down
                    if ((optPrev - optNew) < 0) {
                        optIntercepts.add(new DataPair(hour, positiveSlopeOpt));
                        positiveSlopeOpt = true;
                    }   
                }
                optPrev = optNew;
            }
            // For each of the desired intercepts, find the best closest intercept
            for (DataPair curIntercept : optIntercepts) {
                DataPair bestOther = null;
                int curDiff = -1;
                for (DataPair otherIntercept : actualIntercepts) {
                    // Make sure they're the same type of intercept (max or min)
                    if (otherIntercept.localMaximum != curIntercept.localMaximum) continue;
                    // Calculate the distance from the curIntercept local max/min
                    int newDiff = Math.abs(otherIntercept.hour - curIntercept.hour); 
                    if (bestOther == null || newDiff < curDiff) {
                        bestOther = otherIntercept;
                        curDiff = newDiff;
                    }
                    
                }
                totalDiff += curDiff; 
            }
 
        }
        if (testOne) System.out.println("Eval: " + totalDiff);

        return totalDiff;
    }

    /**
     * Extrapolate data for hours that are in between hours that were inputed
     */
    public static LinkedHashMap<String, ArrayList<Float>> estimateFullPop(LinkedHashMap<String, ArrayList<DataPair>> desiredPop) {
        LinkedHashMap<String, ArrayList<Float>> returnMap = new LinkedHashMap<String, ArrayList<Float>>();
        for (String species : desiredPop.keySet()) {
            ArrayList<DataPair> speciesPop = desiredPop.get(species);
            DataPair lastPair = speciesPop.get(speciesPop.size() - 1); 
            // Make sure the ArrayList is large enough to hold all of the hours
            ArrayList<Float> returnList = new ArrayList<Float>(lastPair.hour + 1);
            for (int i = 0; i < lastPair.hour + 1; i++) {
                returnList.add(0f);
            }
            int totalCounter = 0;
            for (int i = 0; i < speciesPop.size() - 1; i++) {
                DataPair firstPair = speciesPop.get(i);
                DataPair nextPair = speciesPop.get(i + 1);
                int sectionCounter = 0;
                float timeDiff = nextPair.hour - firstPair.hour;
                float valDiff = nextPair.population - firstPair.population;
                float step = valDiff / timeDiff;
                for (int j = 0; j < timeDiff; j++) {
                    returnList.set(totalCounter++, firstPair.population + step * sectionCounter++);  
                }

            }
            returnList.set(totalCounter, (float) lastPair.population); 
            // Checks to make sure this worked correctly
            boolean isAccurate = true;
            for (DataPair dp : speciesPop) {
                isAccurate = isAccurate && (dp.population == returnList.get(dp.hour));
            }
            if (totalCounter != lastPair.hour || !isAccurate) {
                throw new RuntimeException("Estimated population was not created correctly. Check source code");
            }
            returnMap.put(species, returnList);
        }
        return returnMap; 
    }


    /**
     * Tournament style, each returnObject compares with 10 other returnObjects
     * TODO: Make it so they can't repeat returnObjects / battle themselves
     */
    public static ArrayList<ReturnObject> getParentPopulation(ArrayList<ReturnObject> topPopulations, Random rand) {
        int NUMFIGHTS = 10;
        TreeMap<EvalObject, ReturnObject> topParents = new TreeMap<EvalObject, ReturnObject>();
        for (ReturnObject r : topPopulations) {
            int numWins = 0;
            for (int i = 0; i < NUMFIGHTS; i++) {
                ReturnObject otherR = topPopulations.get(rand.nextInt(topPopulations.size()));
                if (r.eval.compareTo(otherR.eval) >= 0) {
                    numWins++;
                }
            }
            // We are using EvalObject's compareTo method for a quick way to sort these ReturnObjects
            topParents.put(new EvalObject(numWins, r.population), r);
        }
        ArrayList<ReturnObject> parents = new ArrayList<ReturnObject>(topParents.values());
        parents.subList(numKeepGeneration, topPopulations.size()).clear();
        return parents;
    }

    /**
     * Takes the top competitors from the previous population and
     * combines variables + mutates
     */
    public static ArrayList<ArrayList<Species>> swapAndMutate(ArrayList<ReturnObject> data, Random rand) {
        ArrayList<ArrayList<Species>> newPopulations = new ArrayList<ArrayList<Species>>();
        ArrayList<Species> exampleSpeciesList = data.get(0).speciesList;
        for (int j = 0; j < numRandom; j++) {
            ArrayList<Species> newList = new ArrayList<Species>();
            for (Species s : exampleSpeciesList) {
                Species newS = s.makeCopy();
                newList.add(randomizeSpecies(newS, rand));
            }
            newPopulations.add(newList);
        }
        for (int j = 0; j < numKeepGeneration * numMutate; j++) {
            ArrayList<Species> pop = data.get(rand.nextInt(data.size())).speciesList;
            ArrayList<Species> newList = new ArrayList<Species>();
            for (Species s : pop) {
                Species newS = s.makeCopy();
                newList.add(mutateSpecies(newS, rand));
            }
            newPopulations.add(newList);
        }
        for (int j = 0; j < numBreed; j++) {
            ArrayList<Species> pop1 = data.get(rand.nextInt(data.size())).speciesList;
            ArrayList<Species> pop2 = data.get(rand.nextInt(data.size())).speciesList;
            ArrayList<Species> newList = new ArrayList<Species>();
            for (int h = 0; h < pop1.size(); h++) {
                Species s1 = pop1.get(h);
                Species s2 = pop2.get(h);
                if (!s1.name.equals(s2.name)) {
                    System.out.println("Species Lists not in same order, rework logic");
                    return null;
                }
                newList.add(mutateSpecies(combineSpecies(s1, s2, rand), rand));
            }
            newPopulations.add(newList);
        }
        // Add exact copies of parents 
        /*for (ReturnObject r : data) {
            newPopulations.add(r.speciesList);
        }*/

        return newPopulations;
    }

    /**
     * Idea is to make the initial species a copy of s1, and if(randBool), switch that variable to s2
     */
    public static Species combineSpecies(Species s1, Species s2, Random rand) {
        Species newS = s1.makeCopy();
        if (s1 instanceof Heterotroph) {
            Heterotroph heteroS2 = (Heterotroph) s2;
            Heterotroph heteroNewS = (Heterotroph) newS;
            if (rand.nextBoolean()) heteroNewS.hungerRate = heteroS2.hungerRate; 
            if (rand.nextBoolean()) heteroNewS.hungerCeiling = heteroS2.hungerCeiling; 
            for (String prey : heteroNewS.preyCatchRate.keySet()) {
                if (rand.nextBoolean()) {
                    heteroNewS.preyCatchRate.put(prey, heteroS2.preyCatchRate.get(prey));
                }
            } 
        }
        if (rand.nextBoolean()) newS.reproductionRate = s2.reproductionRate; 
        if (rand.nextBoolean()) newS.curveSteepness = s2.curveSteepness; 
        return newS;
    }

    public static Species mutateSpecies(Species s, Random rand) {
        if (s instanceof Heterotroph) {
            Heterotroph heteroS = (Heterotroph) s;
            if (rand.nextFloat() <= mutationRate) { 
	            double d = rand.nextDouble();
	            while (heteroS.hungerRate + ((d / 10) - .05)  <= 0) {
	                d = rand.nextDouble();
	            }
                heteroS.hungerRate += ((d / 10) - .05); 
            }
            if (heteroS.hungerRate > 1) heteroS.hungerRate = 1;
            if (rand.nextFloat() <= mutationRate) heteroS.hungerCeiling += (rand.nextDouble() - .5); 
            if (heteroS.hungerCeiling < 1) heteroS.hungerCeiling = 1;
            for (String prey : heteroS.preyCatchRate.keySet()) {
                if (rand.nextFloat() <= mutationRate) {
                    double newRate = heteroS.preyCatchRate.get(prey) + ((rand.nextDouble() / 50) - .01);
                    if (newRate > 1) newRate = 1;
                    if (newRate < 0) newRate = 0;
                    heteroS.preyCatchRate.put(prey, newRate);
                }
            } 
        } 
        // Mutate reproduction rate
        if (rand.nextFloat() <= mutationRate) {
            double d = rand.nextDouble();
            while (s.reproductionRate + ((d / 100.0) - .005)  <= 0.0) {
                d = rand.nextDouble();
            }
            s.reproductionRate += ((d / 100.0) - .005); 
        }
        // Mutate curvesteepness
        if (s.reproductionRate > 1) s.reproductionRate = 1;
        if (rand.nextFloat() <= mutationRate) s.curveSteepness += (rand.nextInt(20) - 10); 
        if (s.curveSteepness < 0) s.curveSteepness = 0;
        return s;
    }

    /**
     * To add a new computed variable, update:
     *      randomizeSpecies() in Main
     *      mutateSpecies() in Main
     *      combineSpecies() in Main
     *      toString() in Species/Heterotroph/Autotroph
     *
     */
    public static Species randomizeSpecies(Species s, Random rand) {
        
	    s.reproductionRate = rand.nextDouble() / 10.0;
        while (s.reproductionRate == 0.0) {
            s.reproductionRate = rand.nextDouble() / 10.0;
        }
	    s.curveSteepness = rand.nextInt(30);
	    if (s instanceof Heterotroph) {
	        Heterotroph heteroS = (Heterotroph) s;
	        heteroS.hungerRate = rand.nextDouble();
            while (heteroS.hungerRate == 0) {
                heteroS.hungerRate = rand.nextDouble();
            }
	        heteroS.hungerCeiling = 1.0 + rand.nextDouble();
	        for (String prey : heteroS.preyList) { 
                double temp = rand.nextDouble() / 10;
                while (temp == 0) {
                    temp = rand.nextDouble() / 10;
                }
	            heteroS.preyCatchRate.put(prey, temp);

	        }
        } 
        // For specifying the model exactly
        if (testOne) {    
	        if (s.name.equals("species1")) {
	            // Autotroph
			    s.reproductionRate = .05767056058852214;
			    s.curveSteepness = 40;
	        } else {
		        // Heterotroph
				Heterotroph heteroS = (Heterotroph) s;
	            if (s.name.equals("species2")) {
				    heteroS.reproductionRate = .005572413837987131;
				    heteroS.curveSteepness = 45;
			        heteroS.hungerRate = .01373009166139621;
			        heteroS.hungerCeiling = 1.6402811810753843;
			        for (String prey : heteroS.preyList) { 
			            heteroS.preyCatchRate.put(prey, .0154743309171327);
			        }
                }
                if (s.name.equals("species3")) {
                    heteroS.reproductionRate = .001972413837987131;
                    heteroS.curveSteepness = 45;
                    heteroS.hungerRate = .01373009166139621;
                    heteroS.hungerCeiling = 4.4402811810753843; 
                    heteroS.preyCatchRate.put("species1", .0354743309171327);
                    heteroS.preyCatchRate.put("species2", .0000094743309171327);
                }
	        }
        }
        // */

        return s;
    }
}
