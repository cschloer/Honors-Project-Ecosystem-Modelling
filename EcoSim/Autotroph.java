package EcoSim;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Random;

public class Autotroph extends Species {

    // How far the offspring move
    int reproductionDistance;

    public Autotroph(String name) {
        this.name = name;
        this.autotroph = true;
        mobility = 0;
    }


    public int reproduce(int age, float health, Random rand) {
        boolean oldEnough = age >= reproductionAge;
        boolean healthyEnough = health <= .5;
        if (oldEnough && healthyEnough && rand.nextDouble() <= reproductionRate) {
            return reproductionAmount;
        }
        return 0;
    }
    public float evalCell(Cell c, float hunger, Random rand) {
        int val = 0;
        // To simulate random floating in space
        val += rand.nextInt(10);  
        return val;
    }

    // This method will never be called in this class
    public float eatPrey(Cell curCell, float hunger, Random rand) {
        return hunger;
    }

    public Species makeCopy() {
        Autotroph s = new Autotroph(name);
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

        s.reproductionDistance = this.reproductionDistance;
        return s;    
    }

}
