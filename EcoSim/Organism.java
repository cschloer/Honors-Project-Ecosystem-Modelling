package EcoSim;

import java.util.Random;
import java.util.LinkedHashMap;
import java.util.ArrayList;

/**
 * A class representing an Organism in the simulation
 * Contains a reference to the species class representing this organism
 */
public class Organism {

    Species s;
    boolean isHeterotroph;
    Random rand;

    long numOrganisms;
    ArrayList<Float> hungerList; 
    ArrayList<Integer> ageList; 
    Cell cell;
    Triple location;

    /**
     * Eating is simple:
     * hunger value that increases by "consumptionRate" every time step
     * every time an organism is consumed hunger is reduced by a value equal to that organism's mass
     * low hunger leads to greater health
     * If hunger reaches above the ceiling, organism dies
     */

    /**
     * Number is between 0 and 1, with 0 being neutral and 1 being very unhealthy, dead probably 
     *
     * What impacts health?
     * - hunger
     * - age
     * - temperature
     * - crowding levels
     */

    // Update in Simulator.java
    int numHealthImpacts; // The number of factors that impact health  

    public Organism(Species s, Cell c, Triple l, Random rand) {
        this.s = s;
        this.rand = rand;
        this.cell = c;
        this.location = l;
        hungerList = new ArrayList<Float>();
        ageList = new ArrayList<Integer>();
        numOrganisms = 0;

        numHealthImpacts = Simulator.numAutoImpacts;
        if (s instanceof Heterotroph) {
            numHealthImpacts += Simulator.numHeteroImpacts;
            isHeterotroph = true;
        }
    }

    public String getName() {
        return s.name;
    }

    public long getNumOrganisms() {
        return numOrganisms;
    }

    public long addOrganism(int age, float hunger) {
        ageList.add(age);
        hungerList.add(hunger);
        return ++numOrganisms;
    }

    public long removeOrganism(long numRemove, String predator) {
        if (numRemove > numOrganisms) {
            throw new RuntimeException("Number of organisms eaten greater than number of organisms existing (" + numRemove + " and " + numOrganisms + ")");
        }
        //System.out.println("Removing " + numRemove + " organisms");
        for (int i = 0; i < numRemove; i++, numOrganisms--) {
            // Need to fix from int to long if it gets big enough
            int index = rand.nextInt((int) numOrganisms); 
            // TAKE THiS INDX out of the list
            hungerList.remove(index);
            ageList.remove(index);
        }
        cell.addDeathData(s.name, "Got eaten by " + predator, (int) numRemove);  
        return numOrganisms;
    }

    public void doAllAction(ArrayList<ArrayList<ArrayList<Cell>>> cellList, Organism other) {
        long origNum = numOrganisms;
        for (int i = 0; i < origNum; i++) {
            doSingleAction(cellList, other);
        }
        // numOrganisms should be 0 at the end
        if (numOrganisms != 0) {
            throw new RuntimeException("Error in doAllAction() method in Organism.java, num organisms is " + numOrganisms);
        }
    }


    public void doSingleAction(ArrayList<ArrayList<ArrayList<Cell>>> cellList, Organism other) {
        if (numOrganisms == 0) {
            throw new RuntimeException("Num Organisms in a Cell was already 0 and an action was requested");
        }
        numOrganisms--;
        int age = ageList.remove(0);
        age++;
        float hunger = hungerList.remove(0);
        if (s instanceof Heterotroph) {
            Heterotroph heteroS = (Heterotroph) s;
            hunger += heteroS.hungerRate; 
        }

        float health = getHealth(cell, age, hunger);
        // if dead, stop this method
        if (rand.nextFloat() <= (1 + s.deathRate) * health) {
            cell.addDeathData(s.name, "Died of other factors", 1);  
            return;
        }

        Triple newLocation = doMovement(location, hunger, cellList);

        if (s instanceof Heterotroph) {
            Heterotroph heteroS = (Heterotroph) s;
            hunger = eatPrey(hunger, newLocation, cellList);
        }

        int numOffspring = reproduce(age, health);
        Cell newCell = cellList
            .get(newLocation.height)
            .get(newLocation.width)
            .get(newLocation.length);
        for (int i = 0; i < numOffspring; i++) {
            newCell.addOrganism(s.name, 0);
        }
        // Add to other Organism data structure
        newCell.addOrganism(s.name, age, hunger);
    }


    /**
     * Returns true if alive, false if dead
     */
    public float getHealth(Cell curCell, int age, float hunger) {
        float health = 0; 
        float curTemp = curCell.getTemp();

        // TEMPERATURE
        /**      curTemp - temperatureOptimal
         * x = ________________________________
         *      
         *              temperatureRange
         * 
         * The farther away from the optimalTemperature the farther away from 0
         * Up to +/-1 when reaching the temperature range
         *          
         *
         * tempImpact = ( 1 / numHealthImpacts) * (x ^ 2)
         * This has the effect of exponentially increasing from 0 to 1, more impact the closer to 1 
         * The maximum value is 1 / numHealthImpacts, so if all health impacts are on the edge the health is -1 
         */
        double tempImpact = (1 / numHealthImpacts) * Math.pow(((curTemp - s.temperatureOptimal) / s.temperatureRange), s.curveSteepness); 
        health += tempImpact;

        // AGE
        /**
         * If between reproductionAge and reproductionAge * 2, health is not impacted by age
         * Otherwise health is negatively impacted as you get farther from reproductionAge or reproducetiveAge * 2
         *
         *  reproductionAge * 3
         *      ---
         *       | // Currently not implementing the above
         *       | <---- This section has exponetionally higher impact on health the closer to reproductionAge * 3 >
         *       |
         *       - reproductionAge * 2
         *       |
         *       | <---- This section has no impact on health >
         *       |
         *       - reproductionAge
         *       | 
         *       | <---- This section has exponentionally higher impact on health the closer to 0, but not nearly as high >
         *       |
         *      --- 
         *       0 
         *
         */
        double ageImpact = 0; 
        if (age < s.reproductionAge) {
            ageImpact = Math.pow( 1 - (age / s.reproductionAge), s.curveSteepness) / numHealthImpacts / 10;
        } /* Animals rarely die of old age, currently no reason to implement
            else if (age > 2 * s.reproductionAge) {
            ageImpact = Math.pow(((age - 2 * s.reproductionAge) / s.reproductionAge), s.curveSteepness) / numHealthImpacts / 50;
        } */
        if (ageImpact > .1) 
        health += ageImpact;

        // CROWDING
        // TODO: Decide if crowding will be added

        // HUNGER
        double hungerImpact = 0;
        if (s instanceof Heterotroph) {
            Heterotroph heteroS = (Heterotroph) s;
            /*if (hunger >= heteroS.hungerCeiling) {
                health = 0;
                return;
            }*/
            /**        hunger
             * x = ________________
             * 
             *       hungerCeiling
             *
             * The farther away from 0 hunger the more unhealthy it is
             * This value is between 0 and 1 if hunger is below hungerCeiling, 1 being the least health
             * hungerImpact = ( 1 / numHealthImpacts) * (x ^ 2)
             */
            hungerImpact = Math.pow((hunger / heteroS.hungerCeiling), s.curveSteepness) / numHealthImpacts; 
            health += hungerImpact;
        }
        return health; 
    }

    public int reproduce(int age, float health) {
        return s.reproduce(age, health, rand); 
    }

    public Triple doMovement(Triple curLocation, float hunger, ArrayList<ArrayList<ArrayList<Cell>>> cellList) {
        Cell curCell = cellList
            .get(curLocation.height)
            .get(curLocation.width)
            .get(curLocation.length);
        float curMaxEval = s.evalCell(curCell, hunger, rand);
        Triple curMaxLocation = curLocation;
        for (int h = -1 * s.mobility; Math.abs(h) <= s.mobility; h++) {
            int hVal = curLocation.height + h;
            // Ensure this hVal index is in the array, if not continue to next index
            if (hVal < 0 || hVal >= cellList.size())  continue;
            // Magnitude of height for use in next for loop
            int magH = Math.abs(h);
            for (int w = -1 * (s.mobility - magH); Math.abs(w) + magH <= s.mobility; w++) {
                int wVal = curLocation.width + w;
                if (wVal < 0 || wVal >= cellList.get(hVal).size()) continue;
                int magW = Math.abs(w);
                for (int l = -1 * (s.mobility - magH - magW); Math.abs(l) + magH + magW <= s.mobility; l++) {
                    int lVal = curLocation.length + l;
                    if (lVal < 0 || lVal >= cellList.get(hVal).get(wVal).size()) continue;
                    Triple newLocation = new Triple(hVal, wVal, lVal);
                    float newEval = s.evalCell(Cell.getCell(cellList, newLocation), hunger, rand);
                    if (newEval >= curMaxEval) {
                        curMaxLocation = newLocation;
                        curMaxEval = newEval;
                    }
                    //System.out.println("h: " + h + ", w: " + w + ", l " + l); 
                }
            }
        }
        return curMaxLocation;
    }

    public float eatPrey(float hunger, Triple curLocation, ArrayList<ArrayList<ArrayList<Cell>>> cellList) {
	    Cell curCell = cellList
                .get(curLocation.height)
                .get(curLocation.width)
                .get(curLocation.length);
        hunger = s.eatPrey(curCell, hunger, rand);  
        return hunger;
        
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public String toString() {
        return "" + getNumOrganisms();
    }


}
