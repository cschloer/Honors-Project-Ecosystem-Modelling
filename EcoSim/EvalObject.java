package EcoSim;

// A wrapper class around the eval value so that multiple data sets
// with the same eval value can exist in the same hashmap

public class EvalObject implements Comparable {
    
    float val;
    int population;
    Object tieBreaker;

    public EvalObject(float val, int population) {
        this.val = val;
        this.population = population;
        tieBreaker = new Object();
    }

    @Override
    public int compareTo(Object o) {
        /* We never want two objects of these objects to equal eachother,
         * otherwise the whole point of this object is moot */
        EvalObject other = (EvalObject) o; 
        if (this.val == other.val) {
            return this.tieBreaker.hashCode() - other.tieBreaker.hashCode();
        }
        if (this.val - other.val > 0) return 1; 
        if (this.val - other.val < 0) return -1; 
        return 0;
    }

    public String toString() {
        return "" + val;
    }

}
