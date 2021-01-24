import GUIConstants.APP_NAME
import GUIConstants.FIELD_PADDING
import GUIConstants.MAIN_FRAME_HEIGHT
import GUIConstants.MAIN_FRAME_WIDTH
import GUIConstants.PLOT_HEIGHT
import GUIConstants.PLOT_WIDTH
import PlotUtils.asPanel
import PlotUtils.plot
import SwingDSL.button
import SwingDSL.column
import SwingDSL.comboBox
import SwingDSL.label
import SwingDSL.panel
import SwingDSL.rigidArea
import SwingDSL.row
import SwingDSL.textField
import ru.itmo.java.architectures.experiment.Experiment
import ru.itmo.java.architectures.experiment.ExperimentConfig
import ru.itmo.java.architectures.experiment.ExperimentResult
import ru.itmo.java.architectures.experiment.ServerArchitectureType
import ru.itmo.java.architectures.experiment.schedulers.ConstantScheduler
import ru.itmo.java.architectures.experiment.schedulers.LinearScheduler
import java.awt.Color
import java.awt.Container
import java.awt.GridLayout
import java.util.concurrent.Executors
import javax.swing.BoxLayout
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities


fun main() {
    Application().launch()
}


class Application {

    private val experimentPool = Executors.newSingleThreadExecutor()

    private lateinit var displayPanel: JPanel
    private lateinit var controlPanel: JPanel


    fun launch() = SwingUtilities.invokeLater { buildMainFrame() }

    private fun buildMainFrame() =
        JFrame(APP_NAME).apply {
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            setSize(MAIN_FRAME_WIDTH, MAIN_FRAME_HEIGHT)
            contentPane.layout = BoxLayout(contentPane, BoxLayout.Y_AXIS)

            displayPanel = displayPane()

            controlPanel = controlPane()

            isVisible = true
        }

    private fun JFrame.displayPane() =
        panel {
            layout = GridLayout(1, 0)
        }

    private fun JFrame.controlPane() =
        panel {
            val panel = this
            background = Color.LIGHT_GRAY

            column {
                row {
                    label("Architecture:")
                    comboBox(ServerArchitectureType.values()) {}
                }

                row {
                    label("Number of requests per client (X):")
                    textField()
                }

                row {
                    label("Parameter of interest:")
                    comboBox(ParametersOfInterest.values())
                }

                schedulerFields()

                parameterFields()

                button("Launch") {
                    experimentPool.submit {
                        val config = getConfigFromFields()
                        val results = Experiment(config).run()
                        SwingUtilities.invokeLater { showResults(results) }
                        saveResults(results)
                    }
                }
            }
        }

    private fun Container.parameterFields() =
        row {
            label("N")
            textField()
            rigidArea(FIELD_PADDING, 0)

            label("M")
            textField()
            rigidArea(FIELD_PADDING, 0)

            label("Delta")
            textField()
        }

    private fun Container.schedulerFields() =
        row {
            label("Bounds:")
            rigidArea(FIELD_PADDING, 0)

            label("From")
            textField()
            rigidArea(FIELD_PADDING, 0)

            label("To")
            textField()
            rigidArea(FIELD_PADDING, 0)

            label("Step")
            textField()
        }

    private fun getConfigFromFields(): ExperimentConfig {
        return ExperimentConfig(ServerArchitectureType.ASYNCHRONOUS,
            nRequestsScheduler = ConstantScheduler(10),
            nClientsScheduler = ConstantScheduler(2),
            nElementsScheduler = LinearScheduler(100, 200, 200),
            requestDelayMsScheduler = ConstantScheduler(100)
        )
    }

    private fun showResults(results: ExperimentResult) {
        displayPanel.removeAll()

        val plot = plot(intArrayOf(1, 2, 3), intArrayOf(1, 4, 9), "Mean client-side time",
            "param", "Milliseconds", "arch")

        displayPanel.add(plot.asPanel(PLOT_WIDTH, PLOT_HEIGHT))
        displayPanel.add(plot.asPanel(PLOT_WIDTH, PLOT_HEIGHT))
        displayPanel.add(plot.asPanel(PLOT_WIDTH, PLOT_HEIGHT))
        displayPanel.revalidate()
    }

    private fun saveResults(results: ExperimentResult) {

    }

    fun close() {
        experimentPool.shutdown()
    }
}
