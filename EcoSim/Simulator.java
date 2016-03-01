package EcoSim;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.io.PrintWriter;
import java.util.Random;
import java.util.Collections;
import java.util.concurrent.Callable;

/**
 * A simulator that runs x simulations on the inputed speciesList, where x is the number of Random objects given
 */
public class Simulator implements Callable<ReturnObject> {
 
    static int stepMax = 24; // Number of time steps in a day
    static int tempDiffVal = 30;
    static int maxSpecies = 20000;

    // The number of variables that impact health
    static int numAutoImpacts = 2;
    static int numHeteroImpacts = 1; 

    PrintWriter writer;
    ArrayList<Species> speciesList;
    TemperatureObject outside;
    ArrayList<ArrayList<ArrayList<Cell>>> cellList;
    ArrayList<HighLowTemperature> seasonTemperatures;
    // A list of triples to randomize and iterate over to pick cells randomly
    ArrayList<Triple> tripleList;
    // Write to stdout
    boolean printOut = false;
    // Write to file
    boolean writeOut = true;
    int generation;
    int population;
    int maxTime;
    ArrayList<Random> randList;

    /**
     * Total data for all of the seeds, will be averaged at the end to give to ReturnObject
     */
    ArrayList<LinkedHashMap<String, ArrayList<Integer>>> totalPopulationData;
    ArrayList<LinkedHashMap<String, ArrayList<Integer>>> totalTemperatureData;
    ArrayList<LinkedHashMap<String, LinkedHashMap<String, Integer>>> totalDeathData;


    public Simulator(
            ArrayList<Species> speciesList, 
            int population,
            int generation,
            int maxTime,
            ArrayList<Random> randList,
            PrintWriter writer) {
        this.speciesList = speciesList;
        this.generation = generation;
        this.population = population;
        this.maxTime = maxTime;
        if (writer == null) {
            writeOut = false;
        } else {
            this.writer = writer;
        }
        this.randList = randList;
        totalPopulationData = new ArrayList<LinkedHashMap<String, ArrayList<Integer>>>();
        totalTemperatureData = new ArrayList<LinkedHashMap<String, ArrayList<Integer>>>();
        totalDeathData = new ArrayList<LinkedHashMap<String, LinkedHashMap<String, Integer>>>();
        for (int i = 0; i < randList.size(); i++) {
            LinkedHashMap<String, ArrayList<Integer>> totalPopulations = new LinkedHashMap<String, ArrayList<Integer>>();
            LinkedHashMap<String, ArrayList<Integer>> temperaturePopulations = new LinkedHashMap<String, ArrayList<Integer>>(); 
            LinkedHashMap<String, LinkedHashMap<String, Integer>> deathData = new LinkedHashMap<String, LinkedHashMap<String, Integer>>();
	        for (Species s : speciesList) {
	            totalPopulations.put(s.name, new ArrayList<Integer>());
	            ArrayList<Integer> tempRange = new ArrayList<Integer>();
	            // 60 is an assumed range. Can be increased or decreased as needed
	            for (int j = 0; j < 60; j++) {
	                tempRange.add(0);
	            }
	            temperaturePopulations.put(s.name, tempRange);
                LinkedHashMap<String, Integer> temp = new LinkedHashMap<String, Integer>();
	            for (Species pred : speciesList) {
	                if (!pred.autotroph) {
	                    temp.put("Got eaten by " + pred.name, 0);
	                }
	            }
                temp.put("Died of other factors", 0);
                deathData.put(s.name, temp);
	        }
            totalPopulationData.add(totalPopulations);
            totalTemperatureData.add(temperaturePopulations);
            totalDeathData.add(deathData);
        }

    }

    public void setUp(Random rand, int randNum) {

        /*********************************
         Create initial cell list and season temperature list
        *********************************/
            seasonTemperatures = new ArrayList<HighLowTemperature>();
            cellList = new ArrayList<ArrayList<ArrayList<Cell>>>();
            createSeason(Main.summerHigh, Main.winterHigh);
            createCellList(Main.simWidth, Main.simHeight, Main.simLength, Main.summerHigh, Main.winterHigh, randNum, rand); 
        /*
         * Populate the triple list with all of the cell indices
         */
        tripleList = new ArrayList<Triple>(); 
        for (int h = 0; h < cellList.size(); h++) {
            ArrayList<ArrayList<Cell>> curLayer = cellList.get(h);
            for (int w = 0; w < curLayer.size(); w++) {
                ArrayList<Cell> curSection = curLayer.get(w);
                for (int l = 0; l < curSection.size(); l++) {
                    Triple t = new Triple(h, w, l);
                    tripleList.add(t);
                }
            }
        }
    
    }

    public ReturnObject call() {
        //System.out.println("On generation " + generation + ", population " + population + "... Using " + randList.size() + " seeds");
        return simulate(); 
    }

    public ReturnObject simulate() {
        int counter = 0;
        ReturnObject returnObject = new ReturnObject(speciesList, randList);
        returnObject.population = population;
        for (Random rand : randList) {
        //    System.out.println("FIRST RAND VALUE: " + rand.nextInt());
            int numHours = 0;
            setUp(rand, counter);
            populateCells(rand);
	        try {
	            printCellList(writer, true, counter);
                simulationloop:
	            for (int year = 0; year < Main.numYears; year++) {
	                for (int day = 0; day < 365; day++) {
	                    if (printOut) System.out.println("Year: " + year + ", Day: " + day);
	                    for (int step = 0; step < stepMax; step++) {
                            //System.out.println(cellList);
                            //if (numHours++ > 2) {
                            if (numHours++ > maxTime) {
                                break simulationloop; 
                            }
	                        //writer.println("Year " + year + ", day " + day + ", step " + step);
	                        updateOutside(day, step);
	                        // z, x, y. First section of the cellList is the height
	                        for (ArrayList<ArrayList<Cell>> cellLayer : cellList) {
	                            for (ArrayList<Cell> cellSection : cellLayer) {
	                                for (Cell cell : cellSection) {
	                                    cell.updateTemperature();
	                                }
	                            }
	                        }
                            // here is problem, doAction
	                        doAction(rand);
	                        printCellList(writer, step == 0, counter);
	                    }
	                }
	            }
	        } catch (PopulationOutOfBoundsException e) {
	            if (printOut) System.out.println(e.getMessage());
	            //System.out.println(e.getMessage());
	            //writer.println(e.getMessage());
	        }
            counter++;
        }
        // Average all of the data

        for (Species s : speciesList) {
            String sName = s.name;
	        int index = 0;
	        boolean populationLeft = true;
            double totalAveragePop = 0;

	        while (populationLeft) {
	            populationLeft = false;
                float averagedPop = 0;
                for (LinkedHashMap<String, ArrayList<Integer>> totalPop : totalPopulationData) {
                    ArrayList<Integer> speciesPop = totalPop.get(sName);
                    if (speciesPop.size() > index) {
                        populationLeft = true;
                        averagedPop += (float) speciesPop.get(index) / totalPopulationData.size(); 
                        totalAveragePop += averagedPop / speciesPop.size();
                    }
                }
                returnObject.totalPopulations.get(sName).add(averagedPop);
                index++;
	        }
            LinkedHashMap<String, Float> averagedDeath = new LinkedHashMap<String, Float>();
            for (Species pred : speciesList) {
                if (!pred.autotroph) {
                    averagedDeath.put("Got eaten by " + pred.name, 0f);
                }
            }
            averagedDeath.put("Died of other factors", 0f);
            // Go through each rand seed
            for (LinkedHashMap<String, LinkedHashMap<String, Integer>> deathData : totalDeathData) {
                LinkedHashMap<String, Integer> temp = deathData.get(sName);
                for (String deathReason : temp.keySet()) {
                    float prev = averagedDeath.get(deathReason);
                    averagedDeath.put(deathReason, prev + (temp.get(deathReason) / totalDeathData.size())); 
                }
            }
            returnObject.deathData.put(sName, averagedDeath);
            ArrayList<Float> averageSpeciesTemperature = null;
            for (LinkedHashMap<String, ArrayList<Integer>> temperatureData : totalTemperatureData) {
                ArrayList<Integer> speciesTemperature = temperatureData.get(sName);
                // Initialize the averageSpeciesTemperature array
                if (averageSpeciesTemperature == null) {
                    averageSpeciesTemperature = new ArrayList<Float>();
                    for (int i = 0; i < speciesTemperature.size(); i++) {
                        float newVal = speciesTemperature.get(i) / totalTemperatureData.size() / speciesTemperature.size() / (float) totalAveragePop;
                        averageSpeciesTemperature.add(newVal);
                    }   
                } else {
                    for (int i = 0; i < speciesTemperature.size(); i++) { 
                        float newVal = averageSpeciesTemperature.get(i) + (speciesTemperature.get(i) / totalTemperatureData.size() / speciesTemperature.size() / (float) totalAveragePop);
                        averageSpeciesTemperature.set(i, newVal);
                    }
                }
            }
            returnObject.temperaturePopulations.put(sName, averageSpeciesTemperature);
        }
        return returnObject;
    }

    public void doAction(Random rand) {
        Collections.shuffle(tripleList, rand);
        for (Triple t : tripleList) {
            Cell curCell = cellList.get(t.height).get(t.width).get(t.length);
            curCell.doAction(cellList);
        }
        for (Triple t : tripleList) {
            Cell curCell = cellList.get(t.height).get(t.width).get(t.length);
            curCell.clearDoneAction();
        }
    }

    /**
     * Populate the cells with initial species
     * TODO: intelligently populate in the beginning
     */
    public void populateCells(Random rand) {
        for (Species s : speciesList) {
            float tempMax = s.temperatureOptimal + s.temperatureRange;
		    float tempMin = s.temperatureOptimal - s.temperatureRange;
            for (int i = 0; i < s.initNum; i++) { 
                int counter = 0;
                while (true) {
	                int randH = rand.nextInt(cellList.size());
	                ArrayList<ArrayList<Cell>> cellLayer = cellList.get(randH);
	                int randW = rand.nextInt(cellLayer.size());
	                ArrayList<Cell> cellSection = cellLayer.get(randW);
                    int randL = rand.nextInt(cellSection.size());
                    Cell cell = cellSection.get(randL);
		            // Don't populate here if too cold / too hot
		            if (counter++ < 10 && (cell.getTemp() >= tempMax || cell.getTemp() <= tempMin)) continue;
                    cell.addOrganism(s.name, rand.nextInt(s.reproductionAge * 2));
                    break;
                }
            }
        }
    }


    /*
     * Print out the cell temperatures for each layer
     * And record chart data
     */
    private void printCellList(PrintWriter writer, boolean printNumInfo, int simNum) throws PopulationOutOfBoundsException {
        int i = 0;
        /*System.out.println("------");    
        System.out.println("Outside temperature: ");
        System.out.println(outside.toString()); */
        /*writer.println("------");    
        writer.println("Outside temperature: ");
        writer.println(outside.toString());*/    
        LinkedHashMap<String, Integer> totalPopulations = new LinkedHashMap<String, Integer>();
        for (Species s : speciesList) {
            totalPopulations.put(s.name, 0);
        }

        for (ArrayList<ArrayList<Cell>> cellLayer : cellList) {
            /*System.out.println("------");    
            System.out.println("Layer " + ++i + " temperature:");    
            System.out.println(cellLayer.toString());   */ 

         /*   writer.println("------");    
            writer.println("Layer " + ++i + ": ");
            writer.println("\tTemperature:" + cellLayer.get(0).get(0).getTemp());    
            writer.println("\tSpecies:"); */
            for (Species s : speciesList) {
                int numSpecies = 0;
    //ArrayList<LinkedHashMap<String, Integer>> totalDeathData;
                LinkedHashMap<String, Integer> deathCause = new LinkedHashMap<String, Integer>(); 
                for (ArrayList<Cell> cellSection : cellLayer) {
                    for (Cell c : cellSection) {
                        // Record # of species in layer
                        numSpecies += c.getNumOrganisms(s.name);
                        // Record # of species for each temperature
                        int cellTemp = Math.round(c.getTemp());
                        ArrayList<Integer> tempRange = totalTemperatureData.get(simNum).get(s.name);
                        Integer prevValue = tempRange.get(cellTemp + ReturnObject.tempOffset);
                        tempRange.set(cellTemp + ReturnObject.tempOffset, prevValue + numSpecies);
                    }
                }
                //writer.println("\t\t" + s.name + ": " + numSpecies);
                totalPopulations.put(s.name, totalPopulations.get(s.name) + numSpecies);
            }    
        }
    

        for (Species s : speciesList) {
            if (totalPopulations.get(s.name) == 0) throw new PopulationOutOfBoundsException("species " + s.name + " is extinct");
            if (totalPopulations.get(s.name) > s.maxNum) throw new PopulationOutOfBoundsException("species " + s.name + " is too large");
            totalPopulationData.get(simNum).get(s.name).add(totalPopulations.get(s.name));
        }
        //System.out.println("------");    
        //writer.println("------");    
    }

    /**
     * Update the temperature of the outside object to simulate day, night, and seasons
     */
    private void updateOutside(int day, int step) {
        HighLowTemperature curDay = seasonTemperatures.get(day);
        HighLowTemperature nextDay = seasonTemperatures.get((day + 1) % 365 );
        float curTemp = outside.getTemp();
        // Go from hottest of current day to lowest of current day
        float tempChange1 = (curDay.high - curDay.low) / ((float) stepMax / 2);
        // Go from coldest of current day to hottest of next day
        float tempChange2 = (nextDay.high - curDay.low) / ((float) stepMax / 2);
        if (step / (float) stepMax >= 0.5) {
            // Reached the coldest part of the day (stepMax / 2)
            outside.setTemp(curTemp + tempChange2);
        } else {
            // Reached the hottest part of the day (0)
            outside.setTemp(curTemp - tempChange1);
        }

    }

    /*
     * The initial creation of the "Lake" of cells
     * Currently just a cube
     * TODO: Make it more lake-like!
     */
    public void createCellList(int width, int height, int length, float maxHigh, float minHigh, int randNum, Random rand) {
        float tempDiff = (maxHigh - minHigh) / 2 / height;
        for (int h = 0; h < height; h++) {
            // New z layer
            cellList.add(new ArrayList<ArrayList<Cell>> ());
            ArrayList<ArrayList<Cell>> curLayer = cellList.get(h);
            for (int w = 0; w < width; w++) {
                // New x layer
                curLayer.add(new ArrayList<Cell> ());
                ArrayList<Cell> curSection = curLayer.get(w);
                for (int l = 0; l < length; l++) {
                    // new y layer
                    Cell newCell;
                    // At the top, give the outside temperature object
                    if (h == 0) {
                        newCell = new Cell(maxHigh - tempDiff * h, outside, new Triple(h, w, l), speciesList, this, randNum, rand);
                    } else {
                        Cell aboveCell = cellList.get(h-1).get(w).get(l);
                        newCell = new Cell(maxHigh - tempDiff * h, aboveCell, new Triple(h, w, l), speciesList, this, randNum, rand); 
                    }
                    curSection.add(newCell);
                }
            }
        }
    }


    /*
        Simple season creation, with a high at the beginning and low at the middle
        And constant difference of 10 degrees celcius per day
    */
    public void createSeason(float maxHigh, float minHigh) {
        float curHigh = maxHigh;
        float tempChange = (maxHigh - minHigh) / 182;
        for (int i = 0; i < 365; i++) {
            // Since 365 is an odd number, let's make it simple and not change temperature at 182
            if (i == 182) {
                seasonTemperatures.add(new HighLowTemperature(curHigh, curHigh-10));
            } else if (i > 182) { // Temperature increasing
                curHigh += tempChange;
                seasonTemperatures.add(new HighLowTemperature(curHigh, curHigh-10));
            } else if (i < 182) { // Temperature decreasing
                curHigh -= tempChange;
                seasonTemperatures.add(new HighLowTemperature(curHigh, curHigh-10));
            }
            //System.out.println("Day " + i + ", high: " + curHigh);
        }
        outside = new TemperatureObject(seasonTemperatures.get(0).high);
    }


    class PopulationOutOfBoundsException extends Exception {
        public PopulationOutOfBoundsException() {}

        public PopulationOutOfBoundsException(String message) {
            super(message);
        }
    }

}
