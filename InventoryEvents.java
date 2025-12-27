import java.awt.event.*;
import java.awt.*;

public class InventoryEvents implements ActionListener {
    private Inventory mainUI;

    public InventoryEvents(Inventory mainUI) {
        this.mainUI = mainUI;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
         // incase the products button is clicked we need to remmove all the things in the right right panel
        if (e.getActionCommand().equals("Products")) {
            mainUI.rightRight.removeAll();
            DisplaysTable dt = new DisplaysTable();
            mainUI.rightRight.add(dt.getTablePanel(), BorderLayout.CENTER);
        }
        mainUI.products.revalidate();
        mainUI.products.repaint();
    }
}