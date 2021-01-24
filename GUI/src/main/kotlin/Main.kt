import GUIConstants.MAIN_FRAME_HEIGHT
import GUIConstants.MAIN_FRAME_WIDTH
import SwingDSL.button
import SwingDSL.column
import SwingDSL.comboBox
import SwingDSL.label
import SwingDSL.panel
import SwingDSL.row
import ru.itmo.java.architectures.experiment.ServerArchitectureType
import java.awt.Color
import java.awt.Rectangle
import javax.swing.BoxLayout
import javax.swing.JFrame
import javax.swing.SwingUtilities


fun main() {
    SwingUtilities.invokeLater { mainFrame() }
}


private fun mainFrame() =
    JFrame("HelloWorldSwing").run {
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        setSize(MAIN_FRAME_WIDTH, MAIN_FRAME_HEIGHT)
        contentPane.layout = BoxLayout(contentPane, BoxLayout.Y_AXIS)

        displayPane()

        controlPane()

        isVisible = true
    }


private fun JFrame.displayPane() =
    panel {
        background = Color.WHITE

        button("Hello") {
            bounds = Rectangle(50, 100, 80, 30)
            background = Color.YELLOW
        }

        button("World") {
            setBounds(100, 100, 80, 30)
            background = Color.green
        }
    }


private fun JFrame.controlPane() =
    panel {
        background = Color.WHITE

        column {
            row {
                label("Architecture:")
                comboBox(ServerArchitectureType.values()) {}
            }

            row {
                label("Number of requests per client (X):")
            }

            row {
                label("Parameter of interest:")
                comboBox(ParametersOfInterest.values()) {}
            }

            row {
                label("N")
                label("M")
                label("Delta")
            }

            row {
                label("Parameter of interest bounds:")
                label("From")
                label("To")
                label("Step")
            }
        }
    }
