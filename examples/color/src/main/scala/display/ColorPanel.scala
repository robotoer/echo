import java.awt.Color
import javax.swing.JPanel
import com.github.oetzi.echo.Behaviour
import com.github.oetzi.echo.Witness

package display {
	class ColorPanel extends JPanel {
		def setBackground(color : Behaviour[Color]) {
			super.setBackground(color.now)
			color.sample.foreach(change => super.setBackground(change))	
		}
	}
}