package EcoSim;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;

import java.awt.Color;

public class LineChart extends ApplicationFrame {
    
    String title;


    /*
     * Constructor with LinkedHashMap with a name to population deliniation
    **/
    public LineChart(final String title,
            final LinkedHashMap<String, ArrayList<Float>> populations, 
            boolean isTemperatureData,
            String x,
            String y
            ) {
        super (title);
        this.title = title;

        final XYDataset dataset = createDataset(populations, isTemperatureData);
        final JFreeChart chart = createLineChart(dataset, title, x, y, isTemperatureData);
        final ChartPanel chartPanel = new ChartPanel(chart);
        //chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
        setContentPane(chartPanel);
    }

    
    private XYDataset createDataset(LinkedHashMap<String, ArrayList<Float>> populations, boolean isTemperatureData) {
        final XYSeriesCollection dataset = new XYSeriesCollection();
        for (String name : populations.keySet()) {
            ArrayList<Float> population = populations.get(name);
            XYSeries series = new XYSeries(name);
            int step = 0;
            float totalSpecies = 0;
            if (isTemperatureData) {
                step -= ReturnObject.tempOffset;
                for (Float i : population) {
                    totalSpecies += i;
                }
            }
            for (float num : population) {
                if (isTemperatureData) {
                    series.add(step++, (float) num / totalSpecies);
                } else {
                    if (num != 0) {
                        series.add(step++, num);
                    }
                }
            }
            dataset.addSeries(series);
        }

        return dataset;
    }

    private JFreeChart createLineChart(final XYDataset dataset, String title, String x, String y, boolean isTemperatureData) {
        final JFreeChart chart = ChartFactory.createXYLineChart(
            title,    // chart title
            x,                    // x axis label
            y,                    // y axis label
            dataset,                // data
            PlotOrientation.VERTICAL,
            true,                   // include legend
            true,                   // tooltips
            false                   // urls
        );
        // optional customization
        chart.setBackgroundPaint(Color.white);
        // final StandardLegend legend = (StandardLegend) chart.getLegend();
        // legend.setDIsplaySeriesShapes(true);
        final XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.lightGray);
        // plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);

        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesShapesVisible(0, false);
        renderer.setSeriesShapesVisible(1, false);
        plot.setRenderer(renderer);

        // change the auto tick unit selection to integer units only...
        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        if (!isTemperatureData) rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        return chart;
    }

}
