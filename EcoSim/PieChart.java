package EcoSim;

import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import java.text.DecimalFormat;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import java.util.LinkedHashMap;

public class PieChart extends ApplicationFrame {
    static String title;

    public PieChart(String title, LinkedHashMap<String, Float> data) {
        super(title);
        this.title = title;
        setContentPane(createDemoPanel(data));
        System.out.println(data);
    }

    private static PieDataset createDataset(LinkedHashMap<String, Float> data) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (String key : data.keySet()) {
            dataset.setValue(key, data.get(key));
        }
        return dataset;
    }

    private static JFreeChart createChart(PieDataset dataset) {
        JFreeChart chart = ChartFactory.createPieChart(
            title,  // chart title
            dataset,            // data
            true,               // include legend
            true,
            false);
        PiePlot plot = (PiePlot) chart.getPlot();
        PieSectionLabelGenerator generator = new StandardPieSectionLabelGenerator("{0} = {2}", new DecimalFormat("0"), new DecimalFormat("0.00%"));
        plot.setLabelGenerator(generator);
        return chart;
    }

    public static JPanel createDemoPanel(LinkedHashMap<String, Float> data) {
        JFreeChart chart = createChart(createDataset(data));
        return new ChartPanel(chart);
    }
}


