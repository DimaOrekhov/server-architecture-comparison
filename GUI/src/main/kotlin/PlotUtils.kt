import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.category.DefaultCategoryDataset

object PlotUtils {

    fun plot(xValues: IntArray,
             yValues: IntArray,
             title: String,
             xLabel: String,
             yLabel: String,
             label: String): JFreeChart {
        val dataset = DefaultCategoryDataset().apply {
            (xValues zip yValues).forEach { (x, y) -> addValue(x, label, y) }
        }
        val plot = ChartFactory.createLineChart(title, xLabel, yLabel, dataset, PlotOrientation.VERTICAL,
            true,true,false)

        plot.title.font = GUIConstants.PLOT_FONT
        plot.categoryPlot.domainAxis.labelFont = GUIConstants.AXIS_FONT
        plot.categoryPlot.rangeAxis.labelFont = GUIConstants.AXIS_FONT

        return plot
    }

    fun JFreeChart.asPanel(width: Int, height: Int) = ChartPanel(this, width, height, width, height, width, height,
        false, true, true, true, true, true)
}
