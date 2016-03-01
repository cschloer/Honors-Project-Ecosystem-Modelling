package EcoSim;

public class Triple {

    public Triple(int h, int w, int l) {
        height = h;
        width = w;
        length = l;
    }

    int height;
    int width;
    int length;

    public boolean equals(Object o) {
    	if (!(o instanceof Triple)) return false;
    	Triple t = (Triple) o;
    	return (t.height == height && t.width == width && t.length == length);
    }

    public String toString() {
        return "{height: " + height + ", width: " + width + ", length: " + length + "}";
    }

}
