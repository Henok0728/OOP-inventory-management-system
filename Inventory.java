import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.util.ArrayList;

import java.awt.*;



public class Inventory{
    protected JFrame frame;
    protected JButton[] buttonChoice = new JButton[8]; // home,
    protected JButton sign;
    protected JTextField search;
    protected JPanel leftButton;
    protected JPanel namePanel;
    protected JPanel products; // displays table  n product details
    protected JPanel rightLeft; // to display table
    protected JPanel rightRight; // to display product details
    protected JTextField adminName;
    protected JComboBox<String> c;
    protected JButton addingNewSupp;
    protected JButton[] addEditDel; // add edit and del button
    protected JLabel[] ppdqcsB;
    Font myFont = new Font("Segoe Script", Font.BOLD,15);

    Inventory(){
        frame = new JFrame("Team-HA OOP proj");
        frame.setTitle("Inventory");
        frame.setSize(700,500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocation(550,0);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setFont(myFont);

        buttonChoice[0] = new JButton("Home");
        buttonChoice[1] = new JButton("Products");
        buttonChoice[2] = new JButton("Current Stock");
        buttonChoice[3] = new JButton("Customers");
        buttonChoice[4] = new JButton("Suppliers");
        buttonChoice[5] = new JButton("Sales");
        buttonChoice[6] = new JButton("Purchase");
        buttonChoice[7] = new JButton("Users");
        for(int i= 0; i< 8; i++){
            buttonChoice[i].addActionListener(new InventoryEvents(this));
        }

        leftButton = new JPanel();
        leftButton.setLayout(new GridLayout(8,1,0,10));
        leftButton.setBorder(null);
        leftButton.setBounds(50,100,100,400);

        for(int i = 0; i < 8;i++){
            leftButton.add(buttonChoice[i]);            

        }

        rightRight = new JPanel();
        rightRight.setBorder(new TitledBorder("Product Table"));
        rightRight.setLayout(new BorderLayout(50,50));
        ArrayList<String> listOfSuppliers = new ArrayList<>();
        listOfSuppliers.add("Dell");
        listOfSuppliers.add("HP");
        listOfSuppliers.add("Toshiba");
        c = new JComboBox<>(listOfSuppliers.toArray(new String[0]));
        
        addEditDel = new JButton[3];
        addEditDel[0] = new JButton("Add");
        addEditDel[1] = new JButton("Edit");
        addEditDel[2] = new JButton("Delete");
        JPanel butPanel = new JPanel(); // sub panel for ADD EDIT DELETE
        butPanel.setLayout(new FlowLayout());
        for(int i = 0; i < 3; i++){
            butPanel.add(addEditDel[i]);
            addEditDel[i].setFont(myFont);
        }
        for(int i = 0; i < 3; i++){
            addEditDel[i].addActionListener(new InventoryEvents(this));
        }
        ppdqcsB = new JLabel[7];
        ppdqcsB[0] = new JLabel("Product Code: ");
        ppdqcsB[1] = new JLabel("Product Name: ");
        ppdqcsB[2] = new JLabel("Data: ");
        ppdqcsB[3] = new JLabel("Quantity: ");
        ppdqcsB[4] = new JLabel("Cost Price: ");
        ppdqcsB[5] = new JLabel("Selling Price: ");
        ppdqcsB[6] = new JLabel("Brand: ");

        addingNewSupp = new JButton("Click to add a new Suplier");
        JTextField[] fields = new JTextField[7];
        GridBagConstraints forRightLeft = new GridBagConstraints();
        forRightLeft.anchor = GridBagConstraints.WEST;
        forRightLeft.insets = new Insets(5,5,5,5);
        rightLeft = new JPanel(); // to displays table
        rightLeft.setBorder(new TitledBorder("Product Details"));
        rightLeft.setLayout(new GridBagLayout());
        forRightLeft.gridx = 0;
        forRightLeft.gridy = 0;
        forRightLeft.gridwidth = 2;
        forRightLeft.fill = GridBagConstraints.HORIZONTAL;
        forRightLeft.weighty = 1.0;
        rightLeft.add(c, forRightLeft);
        for(int i = 0; i < 8; i++){
            if(i == 0){
                forRightLeft.gridx = 0;
                forRightLeft.gridy = 1;
                forRightLeft.gridwidth = 2;
                rightLeft.add(addingNewSupp, forRightLeft);
            }
            else{
                forRightLeft.weightx = 1.0;
                fields[i-1] = new JTextField(15);
                forRightLeft.gridy = i + 1;
                forRightLeft.gridx = 0;
                forRightLeft.gridwidth = 1;
                rightLeft.add(ppdqcsB[i-1], forRightLeft);
                forRightLeft.gridx =1; 
                rightLeft.add(fields[i -1], forRightLeft);
            }
            if(i >= 0 && i < 3){
                forRightLeft.gridwidth = 2;
                forRightLeft.gridy = 9;
                rightLeft.add(butPanel, forRightLeft);
            }
        }

        adminName = new JTextField();
        adminName.setEditable(false);
        adminName.setLayout(new BorderLayout());
        adminName.setBounds(10,10,450,150);
        adminName.setText("User");

        sign = new JButton("Sign in/out");

        namePanel = new JPanel();
        namePanel.setLayout(new BorderLayout());
        namePanel.setBorder(new TitledBorder("User"));
        namePanel.setBounds(0,0,500,100);
        namePanel.setSize(400,100);
        namePanel.add(adminName, BorderLayout.CENTER);
        namePanel.add(sign, BorderLayout.AFTER_LINE_ENDS);
        
        search = new JTextField();
        search.setEditable(true);
        search.setBounds(5,5,100,100);

        products = new JPanel();
        products.setBorder(new TitledBorder("Products"));
        products.setLayout(new BorderLayout());
        products.add(search, BorderLayout.BEFORE_FIRST_LINE);
        products.add(rightLeft, BorderLayout.EAST);
        products.add(rightRight, BorderLayout.CENTER);


        frame.add(namePanel, BorderLayout.NORTH);
        frame.add(leftButton, BorderLayout.WEST);
        frame.add(products);
        frame.setVisible(true);
    
    }
    public static void main(String[] args){
        new Inventory();
    }

    
}
class DisplaysTable{
    DatabaseConnector nn = new DatabaseConnector();
    // Column headers
    String[] columnNames = nn.GetColumns();
    Object[][] data = nn.GetData();
    public JScrollPane getTablePanel() {
        JTable table = new JTable(data, columnNames);
        table.setRowHeight(25);
        table.setShowGrid(true);

        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(0).setMinWidth(100);
        return new JScrollPane(table);
    }
    
}
class DatabaseConnector{
    int rows = 10;
    int cols = 10;
    String[] columns = new String[cols];
    String[] GetColumns(){
        for(int i = 0; i < cols;i++){
            columns[i] = "Demo";
            
        }
        return columns;
    }
    String[][] data = new String[cols][rows];
    String[][] GetData(){
        for(int i = 0; i < rows;i++){
            for(int j = 0; j < cols;j++){
                data[i][j] = "TRIAL DATA";
            }
            
        }
        return data;
    }
    
}
