package EcoSim;

import java.util.Random;
import java.util.LinkedHashMap;
import java.util.ArrayList;

public abstract class Species {
    
    // Name of the species
    String name;

    /** Variables inputed by the user */
    // Whether the species needs to consume food or not
    boolean autotroph; 
    // Biomass size, simplified as the same for all species 
    float mass;
    // How many offspring it produces
    int reproductionAmount = 1;
    // How many steps pass before the organism can reproduce
    int reproductionAge;
    // How many tiles it can move
    int mobility = 5;
    // Optimal temperature
    float temperatureOptimal;
    // +- range that determines at what temperatures a species dies
    float temperatureRange;

    // Initial populations size, taken from inputed graph
    int initNum;
    // Max population size, based on inputed graph
    int maxNum;

    /** Variables that the simulation generates */
    // How often it reproduces
    double reproductionRate;
    // How crowded a cell can be before dieing
    // This is the same across all species
    float crowdingMax = 50;
    // Death rate of the organism to go along with health. As of 10/18 this is 0
    float deathRate = 0;
    // How step the curve of health variables increases. Very high means impacts of a
    // variable won't have as much weight until it is close to the limit
    int curveSteepness;

    abstract int reproduce(int age, float health, Random rand);
    abstract float eatPrey(Cell curCell, float hunger, Random rand);
    // Evaluate the attractiveness of a cell
    abstract float evalCell(Cell c, float hunger, Random rand);

    abstract Species makeCopy();


    public String toString() {
        String rString = "[";
        rString += "Name=" + name + "&";
        rString += "Autotroph=" + autotroph + "&";
        rString += "Mass=" + mass + "&";
        rString += "Temperature Optimal=" + temperatureOptimal + "&";
        rString += "Temperature Range=" + temperatureRange + "&";
        rString += "Init Num=" + initNum + "&";
        rString += "Max Num=" + maxNum + "&";
        rString += "Reproduction Rate=" + reproductionRate + "&";
        rString += "Reproduction Age=" + reproductionAge + "&";
        rString += "Death Rate=" + deathRate + "&";
        rString += "Curve Steepness=" + curveSteepness;
        rString += "]";
        return rString;
    }

}
