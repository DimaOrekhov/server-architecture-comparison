import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection

object PlotUtils {

    fun plot(xValues: Iterable<Double>,
             yValues: Iterable<Double>,
             title: String,
             xLabel: String,
             yLabel: String,
             label: String): JFreeChart {
        val dataset = XYSeriesCollection()
        val series = XYSeries(label).apply {
            (xValues zip yValues).forEach { (x, y) -> add(x, y) }
        }
        dataset.addSeries(series)

        val plot = ChartFactory.createXYLineChart(title, xLabel, yLabel, dataset, PlotOrientation.VERTICAL,
            true,true,false)

        plot.title.font = GUIConstants.PLOT_FONT
        plot.xyPlot.domainAxis.labelFont = GUIConstants.AXIS_FONT
        plot.xyPlot.rangeAxis.labelFont = GUIConstants.AXIS_FONT

        return plot
    }

    fun JFreeChart.asPanel(width: Int, height: Int) = ChartPanel(this, width, height, width, height, width, height,
        false, true, true, true, true, true)
}
