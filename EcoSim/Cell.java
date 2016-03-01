package EcoSim;

import java.util.ArrayList;
import java.util.Random;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Collections;

public class Cell extends TemperatureObject {
    
    TemperatureObject cellAbove;
    ArrayList<Organism> organismsInCell;
    Triple curLocation;
    LinkedHashMap<String, Organism> haveMovedOrganisms;
    LinkedHashMap<String, Organism> notMovedOrganisms;
    Random rand;
    Simulator sim;
    int randNum;

    public Cell(float temp, TemperatureObject cellAbove, Triple curLocation, ArrayList<Species> speciesList, Simulator sim, int randNum, Random rand) {
        super(temp);
        this.cellAbove = cellAbove;
        this.rand = rand;
        this.sim = sim;
        this.randNum = randNum;
        organismsInCell = new ArrayList<Organism>();
        haveMovedOrganisms = new LinkedHashMap<String, Organism>();
        notMovedOrganisms = new LinkedHashMap<String, Organism>();
        for (Species s : speciesList) {
            Organism o1 = new Organism(s, this, curLocation, rand); 
            Organism o2 = new Organism(s, this, curLocation, rand); 
            haveMovedOrganisms.put(s.name, o1);
            notMovedOrganisms.put(s.name, o2);
        }
        this.curLocation = curLocation;
    }

    /*
    * Calculates the temperature of this cell
    * Based on the temperature of the above cell
    */
    public float updateTemperature() {
        float newTemp = getTemp() + (cellAbove.getTemp() - getTemp()) / 2 / 30;
        setTemp(newTemp);
        return newTemp;

    }

    /**
     * 
     */
    public void clearDoneAction() {
        for (String s : notMovedOrganisms.keySet()) {
            Organism temp = notMovedOrganisms.get(s); 
            if (temp.getNumOrganisms() != 0) {
                throw new RuntimeException("Some organisms of species " + temp.s.name + " have not done an action");
            }
            notMovedOrganisms.put(s, haveMovedOrganisms.get(s));
            haveMovedOrganisms.put(s, temp);
        }

    }

    //public void addDeathData

    @Override
    public String toString() {
        return "" + getTemp();
    }

    public void addOrganism(String s) {
        addOrganism(s, 0);
    }

    public void addOrganism(String s, int age) {
        addOrganism(s, age, 0);
    }

    public void addOrganism(String s, int age, float hunger) {
        Organism o = haveMovedOrganisms.get(s);    
        o.addOrganism(age, hunger);
    }

    public void addDeathData(String species, String howDied, int num) {
        int prevVal = sim.totalDeathData.get(randNum).get(species).get(howDied);
        sim.totalDeathData.get(randNum).get(species).put(howDied, prevVal + num);
    }


    /**
     * Count the number of species of a given type in this cell
     */
    public long getNumOrganisms(String name) {
        Organism o1 = haveMovedOrganisms.get(name);
        Organism o2 = notMovedOrganisms.get(name);
        return o1.getNumOrganisms() + o2.getNumOrganisms();
    }

    public void doAction(ArrayList<ArrayList<ArrayList<Cell>>> cellList) {
        ArrayList<Organism> totalOrgList = new ArrayList<Organism>();
        for (Organism o : notMovedOrganisms.values()) {
            totalOrgList.add(o);
        }
        Collections.shuffle(totalOrgList, rand);
        for (Organism o : totalOrgList) {
            o.doAllAction(cellList, haveMovedOrganisms.get(o.s.name));
        }
    }


    public static Cell getCell(ArrayList<ArrayList<ArrayList<Cell>>> cellList, Triple t) {
        return cellList.get(t.height).get(t.width).get(t.length);
    }

}
