import GUIConstants.APP_NAME
import GUIConstants.DEFAULT_DELAY_MS
import GUIConstants.DEFAULT_N_CLIENTS
import GUIConstants.DEFAULT_N_ELEMENTS
import GUIConstants.DEFAULT_N_REQUESTS
import GUIConstants.FIELD_PADDING
import GUIConstants.INCORRECT_INPUT_FORMAT_MESSAGE
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
import ru.itmo.java.architectures.experiment.FileUtils.asYaml
import ru.itmo.java.architectures.experiment.ServerArchitectureType
import ru.itmo.java.architectures.experiment.schedulers.ConstantScheduler
import ru.itmo.java.architectures.experiment.schedulers.LinearScheduler
import java.awt.Color
import java.awt.Container
import java.awt.GridLayout
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Executors
import javax.swing.*


fun main() {
    val app = Application()
    app.launch()
    Runtime.getRuntime().addShutdownHook(Thread(app::close))
}


class Application {

    data class ParameterFields(val nClients: JTextField, val nElements: JTextField, val delayMs: JTextField)

    data class SchedulerFields(val from: JTextField, val to: JTextField, val step: JTextField)

    private val experimentPool = Executors.newSingleThreadExecutor()

    private lateinit var displayPanel: JPanel
    private lateinit var controlPanel: JPanel
    private lateinit var nRequestsField: JTextField
    private lateinit var parameterFields: ParameterFields
    private lateinit var schedulerFields: SchedulerFields

    private lateinit var architectureComboBox: JComboBox<ServerArchitectureType>
    private lateinit var parameterComboBox: JComboBox<ParametersOfInterest>

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
            background = Color.LIGHT_GRAY

            column {
                row {
                    label("Architecture:")
                    architectureComboBox = comboBox(ServerArchitectureType.values())
                }

                row {
                    label("Number of requests per client (X):")
                    nRequestsField = textField(DEFAULT_N_REQUESTS)
                }

                row {
                    label("Parameter of interest:")
                    parameterComboBox = comboBox(ParametersOfInterest.values())
                }

                schedulerFields()

                parameterFields()

                button("Launch") {
                    experimentPool.submit {
                        val configWithParameter = getConfigFromFields()

                        if (configWithParameter == null) {
                            SwingUtilities.invokeLater {
                                notifyIncorrectInput()
                            }
                            return@submit
                        }

                        val (config, parameter) = configWithParameter

                        val results = Experiment(config).run()
                        SwingUtilities.invokeLater { showResults(results, parameter) }
                        saveResults(results, parameter)
                    }
                }
            }
        }

    private fun Container.parameterFields() =
        row {
            label("N")
            val nElementsField = textField(DEFAULT_N_ELEMENTS)
            rigidArea(FIELD_PADDING, 0)

            label("M")
            val nClientsField = textField(DEFAULT_N_CLIENTS)
            rigidArea(FIELD_PADDING, 0)

            label("Delta")
            val delayMsField = textField(DEFAULT_DELAY_MS)

            parameterFields = ParameterFields(nClients = nClientsField, nElements = nElementsField, delayMs = delayMsField)
        }

    private fun Container.schedulerFields() =
        row {
            label("Bounds:")
            rigidArea(FIELD_PADDING, 0)

            label("From")
            val fromField = textField()
            rigidArea(FIELD_PADDING, 0)

            label("To")
            val toField = textField()
            rigidArea(FIELD_PADDING, 0)

            label("Step")
            val stepField = textField()

            schedulerFields = SchedulerFields(fromField, toField, stepField)
        }

    data class ConfigWithParameter(val config: ExperimentConfig, val parametersOfInterest: ParametersOfInterest)

    private fun getConfigFromFields(): ConfigWithParameter? {
        val architectureType = architectureComboBox.selectedItem as ServerArchitectureType
        val nRequestsScheduler = ConstantScheduler(nRequestsField.getInt() ?: return null)

        val fromValue = schedulerFields.from.getInt() ?: return null
        val toValue = schedulerFields.to.getInt()?.run { this + 1 } ?: return null
        val stepValue = schedulerFields.step.getInt() ?: return null

        val nClientsConstantScheduler =
            ConstantScheduler(parameterFields.nClients.getInt() ?: return null)
        val nElementsConstantScheduler =
            ConstantScheduler(parameterFields.nElements.getInt() ?: return null)
        val requestDelayMsConstantScheduler =
            ConstantScheduler(parameterFields.delayMs.getInt()?.toLong() ?: return null)

        val parameter = parameterComboBox.selectedItem as ParametersOfInterest
        val config = when (parameter) {
            ParametersOfInterest.N -> ExperimentConfig(architectureType,
                nRequestsScheduler = nRequestsScheduler,
                nClientsScheduler = nClientsConstantScheduler,
                nElementsScheduler = LinearScheduler(fromValue, toValue, stepValue),
                requestDelayMsScheduler = requestDelayMsConstantScheduler)

            ParametersOfInterest.M -> ExperimentConfig(architectureType,
                nRequestsScheduler = nRequestsScheduler,
                nClientsScheduler = LinearScheduler(fromValue, toValue, stepValue),
                nElementsScheduler = nElementsConstantScheduler,
                requestDelayMsScheduler = requestDelayMsConstantScheduler)

            ParametersOfInterest.Delta -> ExperimentConfig(architectureType,
                nRequestsScheduler = nRequestsScheduler,
                nClientsScheduler = nClientsConstantScheduler,
                nElementsScheduler = nElementsConstantScheduler,
                requestDelayMsScheduler = LinearScheduler(fromValue.toLong(), toValue.toLong(), stepValue.toLong()))
        }

        return ConfigWithParameter(config, parameter)
    }

    private fun JTextField.getInt() = try { text.toInt() } catch (e: NumberFormatException) { null }

    private fun notifyIncorrectInput() {
        JOptionPane.showMessageDialog(controlPanel, INCORRECT_INPUT_FORMAT_MESSAGE)
    }

    private fun showResults(results: ExperimentResult,
                            parametersOfInterest: ParametersOfInterest) {
        displayPanel.removeAll()

        val xRange = results.config.run {
            when (parametersOfInterest) {
                ParametersOfInterest.N -> nElementsScheduler
                ParametersOfInterest.M -> nClientsScheduler
                ParametersOfInterest.Delta -> requestDelayMsScheduler
            }.map { it.toDouble() }
        }
        val architectureName = results.config.architectureType.showName

        val yClientSide = results.stepResults.map { it.meanClientSideRequestResponseTimeMs }
        val clientSide = plot(xRange, yClientSide, "Mean client-side time",
            parametersOfInterest.name, GUIConstants.PLOT_Y_LABEL, architectureName)

        val yServerSide = results.stepResults.map { it.meanServerSideRequestResponseTimeMs }
        val serverSide = plot(xRange, yServerSide, "Mean server-side total time",
            parametersOfInterest.name, GUIConstants.PLOT_Y_LABEL, architectureName)

        val yTaskTime = results.stepResults.map { it.meanServerSideTaskTimeMs }
        val serverSideTask = plot(xRange, yTaskTime,"Mean server-side task time",
            parametersOfInterest.name, GUIConstants.PLOT_Y_LABEL, architectureName)

        displayPanel.add(serverSideTask.asPanel(PLOT_WIDTH, PLOT_HEIGHT))
        displayPanel.add(serverSide.asPanel(PLOT_WIDTH, PLOT_HEIGHT))
        displayPanel.add(clientSide.asPanel(PLOT_WIDTH, PLOT_HEIGHT))
        displayPanel.revalidate()
    }

    private fun saveResults(results: ExperimentResult, parameter: ParametersOfInterest) {
        val globalResultDir = Files.createDirectories(Paths.get(GUIConstants.RESULTS_DIRECTORY))
        val parameterResultDir = Files.createDirectories(Paths.get(globalResultDir.toString(), parameter.dirName))

        val architectureType = results.config.architectureType
        val resultFile = Files.createTempFile(parameterResultDir, "$architectureType", ".yml")
        resultFile.toFile().writeText(results.asYaml())

        SwingUtilities.invokeLater {
            JOptionPane.showMessageDialog(controlPanel, "Results are saved to $resultFile")
        }
    }

    fun close() {
        experimentPool.shutdown()
    }
}
