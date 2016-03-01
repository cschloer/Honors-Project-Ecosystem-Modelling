package EcoSim;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Random;

// A wrapper class containing fields that represent graphs
public class ReturnObject {

    // Offset for temp so negative temperatures can be indexed
    static int tempOffset = 20;

    LinkedHashMap<String, ArrayList<Float>> totalPopulations;
    LinkedHashMap<String, ArrayList<Float>> temperaturePopulations;     
    LinkedHashMap<String, LinkedHashMap<String, Float>> deathData;
    EvalObject eval = null;
    int population;

    ArrayList<Random> randList;

    ArrayList<Species> speciesList;

    ArrayList<Integer> temperatureRange;

    /**
     * Init all of the graphs
     */
    public ReturnObject(ArrayList<Species> speciesList, ArrayList<Random> randList) {
        this.speciesList = speciesList;
        this.randList = randList;
        totalPopulations = new LinkedHashMap<String, ArrayList<Float>>();
        temperaturePopulations = new LinkedHashMap<String, ArrayList<Float>>(); 
        deathData = new LinkedHashMap<String, LinkedHashMap<String, Float>>();
        for (Species s : speciesList) {
            totalPopulations.put(s.name, new ArrayList<Float>());
            ArrayList<Float> tempRange = new ArrayList<Float>();
            // 60 is an assumed range. Can be increased or decreased as needed
            for (int i = 0; i < 60; i++) {
                tempRange.add(0f);
            }
            temperaturePopulations.put(s.name, tempRange);
            deathData.put(s.name, new LinkedHashMap<String, Float>());
        }
    }

    public int getDuration() {
        int curMax = 0;
        for (ArrayList<Float> list : totalPopulations.values()) {
            if (list.size() > curMax) curMax = list.size();
        }
        return curMax;
    }
/*
 *
 *
    public void addPopulationData(String species, int data, int simNum) {
        totalPopulations.get(simNum).get(species).add(data);

    }

    public void addTemperatureData(String species, int temperature, int data, int simNum) {
        ArrayList<Integer> tempRange = temperaturePopulations.get(simNum).get(species);
        Integer prevValue = tempRange.get(temperature + tempOffset);
        tempRange.set(temperature + tempOffset, prevValue + data); 
    }
    */
}
