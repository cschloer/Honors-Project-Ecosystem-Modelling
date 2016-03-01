package EcoSim;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Collections;

public final class Heterotroph extends Species {

    /** Variables inputed by user */
    // What it eats
    ArrayList<String> preyList;
    // Preference of prey, sum should be 1
    LinkedHashMap<String, Float> preyPreference;

    /** Variables determined through simulation */
    // How likely it is to catch the prey
    LinkedHashMap<String, Double> preyCatchRate;
    // How much the organism's hunger increases each step
    double hungerRate;
    // When the organism dies of hunger
    double hungerCeiling;
    

    public Heterotroph(String name) {
        this.name = name;
        preyList = new ArrayList<String>();
        preyPreference = new LinkedHashMap<String, Float>();
        preyCatchRate = new LinkedHashMap<String, Double>();
    }

    public int reproduce(int age, float health, Random rand) {
        // Only reproduce if healthy and old enough
        boolean oldEnough = age >= reproductionAge;
        boolean healthyEnough = health <= .5;
        if (oldEnough && healthyEnough && rand.nextDouble() <= reproductionRate) {
            return reproductionAmount;
        }
        return 0;
    }

    /**
     * Higher number means more likely to go to cell
     */
    public float evalCell(Cell c, float hunger, Random rand) {
        int diffHighLow = 40;
        float val = 0;
        float cellTemp = c.getTemp();
        float tempRatio = 0.1f; // importance of temperature
        val += rand.nextInt(5);

        // Exponentionally more important the hungrier the organism is 
        float foodPresenceImportance = (float) Math.pow((double) (hunger / hungerCeiling), 2.0); 
        for (String name : preyList) {
            val += c.getNumOrganisms(name) * foodPresenceImportance * preyPreference.get(name); 
        }
        
        float tempImpact = Math.abs(cellTemp - temperatureOptimal) / temperatureRange;

        val -= 1 / (1 - tempImpact);  

        return val;


    }

    public float eatPrey(Cell curCell, float hunger, Random rand) {
        ArrayList<String> preyListTemp = new ArrayList<String>(preyList);
        Collections.shuffle(preyListTemp, rand);
        for (String s :  preyListTemp) {
            double catchRate = preyCatchRate.get(s);
            Organism o1 = curCell.haveMovedOrganisms.get(s);
            long numOrgs = o1.getNumOrganisms();
            for (int i = 0; i < numOrgs; i++) {
                if (rand.nextDouble() < (3*hunger / hungerCeiling) * catchRate) {
                        o1.removeOrganism(1, this.name); 
                        hunger -= (o1.s.mass / this.mass);
                }
            }
            // Give a random range of .5 to 2, allowing a range of half to double of randomness added to the expected probability 
            // Then multiply by numOrgs to pick how many of the organisms are eaten
            //long numEat = (long) (numOrgs * (((3*hunger / hungerCeiling) * catchRate) * ((rand.nextDouble() * 1.5) + .5))); 
            //System.out.println("Numeat: " + numEat);
            //o1.removeOrganism(numEat);
            //hunger -= numEat * (o1.s.mass / this.mass);

            Organism o2 = curCell.notMovedOrganisms.get(s);
            numOrgs = o2.getNumOrganisms();
            for (int i = 0; i < numOrgs; i++) {
                if (rand.nextDouble() < (3*hunger / hungerCeiling) * catchRate) {
                        o2.removeOrganism(1, this.name); 
                        hunger -= (o2.s.mass / this.mass);
                }
            }
            // Give a random range of .5 to 2, allowing a range of half to double of randomness added to the expected probability 
            // Then multiply by numOrgs to pick how many of the organisms are eaten
            //numEat = (long) (numOrgs * (((3*hunger / hungerCeiling) * catchRate) * ((rand.nextDouble() * 1.5) + .5))); 
            //o2.removeOrganism(numEat);
            //hunger -= o2.s.mass / this.mass;
        }
        return hunger;
    }

    public Species makeCopy() {
        Heterotroph s = new Heterotroph(name);
        s.autotroph = this.autotroph;
        s.mass = this.mass;
        s.reproductionAmount = this.reproductionAmount;
        s.reproductionAge = this.reproductionAge;
        s.mobility = this.mobility;
        s.temperatureOptimal = this.temperatureOptimal;
        s.temperatureRange = this.temperatureRange;
        s.initNum = this.initNum;
        s.maxNum = this.maxNum;
        s.reproductionRate = this.reproductionRate;
        s.crowdingMax = this.crowdingMax;
        s.deathRate = this.deathRate;
        s.curveSteepness = this.curveSteepness;

        s.preyList = new ArrayList<String>(preyList);
        s.preyPreference = new LinkedHashMap<String, Float>(preyPreference);
        s.preyCatchRate = new LinkedHashMap<String, Double>(preyCatchRate);
        s.hungerRate = hungerRate;
        s.hungerCeiling = hungerCeiling;

        return s;    
    }

    public String toString() {
        String s = super.toString();
        s = s.substring(0, s.length() - 1);
        s += "&Hunger Rate=" + hungerRate + "&";
        s += "Hunger Ceiling=" + hungerCeiling + "&";
        for (String species : preyList) {
            s += "Prey List=" + species + "&";
        }
        for (String species : preyCatchRate.keySet()) {
            s += "Prey Catch Rate=" + species + ":" + preyCatchRate.get(species) + "&"; 
        }
        for (String species : preyPreference.keySet()) {
            s += "Prey Preference=" + species + ":" + preyPreference.get(species) + "&"; 
        }
        // Remove that last "&" symbol
        s = s.substring(0, s.length() - 1);
        s += "]"; 
        return s;
    }



}
