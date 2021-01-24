import java.awt.Component
import java.awt.Container
import java.awt.event.ActionListener
import javax.swing.*

object SwingDSL {

    fun Container.panel(panelInitializer: JPanel.() -> Unit): JPanel =
        JPanel().also {
            it.panelInitializer()
            add(it)
        }

    fun Container.button(title: String = "", buttonInitializer: JButton.() -> Unit): JButton =
        JButton(title).also{
            it.buttonInitializer()
            add(it)
        }

    fun Container.label(text: String = "", labelInitializer: JLabel.() -> Unit = {}): JLabel =
        JLabel(text).also {
            it.labelInitializer()
            add(it)
        }

    fun Container.box(layout: Int, doCenter: Boolean = true, initializer: Container.() -> Unit) =
        Box(layout).apply {
            initializer()
            if (!doCenter) {
                return@apply
            }

            components.forEach {
                when (it) {
                    is JLabel -> it.alignmentX = Component.CENTER_ALIGNMENT
                    is JButton -> it.alignmentX = Component.CENTER_ALIGNMENT
                    is JRadioButton -> it.alignmentX = Component.CENTER_ALIGNMENT
                }
            }
        }.also { add(it) }

    fun Container.row(doCenter: Boolean = true, initializer: Container.() -> Unit) =
        box(BoxLayout.X_AXIS, doCenter) { initializer() }

    fun Container.column(doCenter: Boolean = true, initializer: Container.() -> Unit) =
        box(BoxLayout.Y_AXIS, doCenter) { initializer() }

    fun <T> Container.comboBox(options: Array<T>, initializer: JComboBox<T>.() -> Unit): JComboBox<T> =
        JComboBox(options).also {
            it.initializer()
            add(it)
        }

    infix fun <T> JComboBox<T>.onAction(listener: ActionListener) {

    }
}
