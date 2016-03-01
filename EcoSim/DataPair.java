package EcoSim;

// For use in inputed data
public class DataPair {

    int hour;
    int population;

    boolean localMaximum;

    public DataPair(int hour, int population) {
        this.hour = hour;
        this.population = population;
    }

    public DataPair(int hour, boolean localMaximum) {
        this.hour = hour;
        this.localMaximum = localMaximum;
    }

    public String toString() {
        if (population == 0) {
            return "(Hour: " + hour + ", Is a peak: " + localMaximum + ")";
        } 
        return "(Hour: " + hour + ", Population: " + population + ")";
    }

}
