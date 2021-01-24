import java.awt.Font

object GUIConstants {
    const val APP_NAME = "Server architecture study"

    const val MAIN_FRAME_WIDTH = 800
    const val MAIN_FRAME_HEIGHT = 600

    const val FIELD_PADDING = 10

    const val PLOT_WIDTH = 200
    const val PLOT_HEIGHT = 200

    val PLOT_FONT = Font("Arial", Font.PLAIN, 16)
    val AXIS_FONT = Font("Arial", Font.PLAIN, 12)

    const val PLOT_Y_LABEL = "Milliseconds"

    const val INCORRECT_INPUT_FORMAT_MESSAGE = "Some input data is either missing or of incorrect format"

    const val RESULTS_DIRECTORY = "results"

    const val DEFAULT_N_REQUESTS = "20"
    const val DEFAULT_N_ELEMENTS = "1000"
    const val DEFAULT_N_CLIENTS = "10"
    const val DEFAULT_DELAY_MS = "150"
}
