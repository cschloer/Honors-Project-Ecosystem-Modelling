package EcoSim;


/*
 * A class that has a temperature field and get method
 * 
 */
public class TemperatureObject {
    
    float temperature;

    public TemperatureObject(float temperature) {
        this.temperature = temperature;
    }

    public float getTemp() {
        return temperature;
    }

    public void setTemp(float newTemp) {
        temperature = newTemp;
    }

    public String toString() {
        return "" + getTemp();
    }

}
