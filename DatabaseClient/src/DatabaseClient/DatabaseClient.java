import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseClient extends JFrame {
    private Connection connection;
    private JTree databaseTree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    private JTextArea queryArea;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JTextArea resultTextArea;
    private JTabbedPane resultPane;
    private JSplitPane mainSplitPane;
    private JSplitPane rightSplitPane;
    
    // Connection components
    private JTextField hostField;
    private JTextField portField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> databaseTypeCombo;
    
    public DatabaseClient() {
        initializeComponents();
        showConnectionDialog();
    }
    
    private void initializeComponents() {
        setTitle("Database Client Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        
        // Initialize main components
        initializeDatabaseNavigator();
        initializeQueryPanel();
        initializeResultPanel();
        
        // Layout setup
        setupLayout();
    }
    
    private void initializeDatabaseNavigator() {
        rootNode = new DefaultMutableTreeNode("Databases");
        treeModel = new DefaultTreeModel(rootNode);
        databaseTree = new JTree(treeModel);
        databaseTree.setRootVisible(true);
        
        // Add mouse listener for double-click on database nodes
        databaseTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = databaseTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        handleTreeDoubleClick(node);
                    }
                } else if (e.getClickCount() == 1) {
                    TreePath path = databaseTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        handleTreeSingleClick(node);
                    }
                }
            }
        });
    }
    
    private void initializeQueryPanel() {
        queryArea = new JTextArea();
        queryArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        queryArea.setTabSize(4);
        queryArea.setText("-- Enter your SQL query here\nSELECT 1;");
        
        JButton executeButton = new JButton("Execute Query");
        executeButton.addActionListener(e -> executeQuery());
    }
    
    private void initializeResultPanel() {
        // Table for SELECT results
        tableModel = new DefaultTableModel();
        resultTable = new JTable(tableModel);
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Text area for other results/errors
        resultTextArea = new JTextArea();
        resultTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        resultTextArea.setEditable(false);
        
        // Tabbed pane for results
        resultPane = new JTabbedPane();
        resultPane.addTab("Table", new JScrollPane(resultTable));
        resultPane.addTab("Messages", new JScrollPane(resultTextArea));
    }
    
    private void setupLayout() {
        // Left panel - Database Navigator
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("Database Navigator"));
        leftPanel.add(new JScrollPane(databaseTree), BorderLayout.CENTER);
        leftPanel.setPreferredSize(new Dimension(250, 0));
        
        // Right panel - Query and Results
        JPanel queryPanel = new JPanel(new BorderLayout());
        queryPanel.setBorder(BorderFactory.createTitledBorder("Query"));
        
        JButton executeButton = new JButton("Execute Query");
        executeButton.addActionListener(e -> executeQuery());
        
        JPanel queryButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        queryButtonPanel.add(executeButton);
        
        queryPanel.add(new JScrollPane(queryArea), BorderLayout.CENTER);
        queryPanel.add(queryButtonPanel, BorderLayout.SOUTH);
        
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(BorderFactory.createTitledBorder("Results"));
        resultPanel.add(resultPane, BorderLayout.CENTER);
        
        // Split panes
        rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, queryPanel, resultPanel);
        rightSplitPane.setDividerLocation(300);
        rightSplitPane.setResizeWeight(0.4);
        
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightSplitPane);
        mainSplitPane.setDividerLocation(250);
        mainSplitPane.setResizeWeight(0.2);
        
        add(mainSplitPane, BorderLayout.CENTER);
        
        // Status bar
        JLabel statusBar = new JLabel("Ready");
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        add(statusBar, BorderLayout.SOUTH);
    }
    
    private void showConnectionDialog() {
        JDialog dialog = new JDialog(this, "Database Connection", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Database Type
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Database Type:"), gbc);
        gbc.gridx = 1;
        databaseTypeCombo = new JComboBox<>(new String[]{"MySQL", "PostgreSQL", "SQLite"});
        panel.add(databaseTypeCombo, gbc);
        
        // Host
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Host:"), gbc);
        gbc.gridx = 1;
        hostField = new JTextField("localhost", 20);
        panel.add(hostField, gbc);
        
        // Port
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1;
        portField = new JTextField("3306", 20);
        panel.add(portField, gbc);
        
        // Username
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        usernameField = new JTextField("root", 20);
        panel.add(usernameField, gbc);
        
        // Password
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        panel.add(passwordField, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton connectButton = new JButton("Connect");
        JButton cancelButton = new JButton("Cancel");
        
        connectButton.addActionListener(e -> {
            if (connectToDatabase()) {
                dialog.dispose();
                loadDatabases();
            }
        });
        
        cancelButton.addActionListener(e -> {
            dialog.dispose();
            System.exit(0);
        });
        
        buttonPanel.add(connectButton);
        buttonPanel.add(cancelButton);
        
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(buttonPanel, gbc);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private boolean connectToDatabase() {
        try {
            String dbType = (String) databaseTypeCombo.getSelectedItem();
            String host = hostField.getText();
            String port = portField.getText();
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            
            String url = buildConnectionUrl(dbType, host, port);
            
            // Load appropriate JDBC driver
            switch (dbType) {
                case "MySQL":
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    break;
                case "PostgreSQL":
                    Class.forName("org.postgresql.Driver");
                    break;
                case "SQLite":
                    Class.forName("org.sqlite.JDBC");
                    break;
            }
            
            connection = DriverManager.getConnection(url, username, password);
            
            JOptionPane.showMessageDialog(this, "Connected successfully!", "Success", 
                                        JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Connection failed: " + e.getMessage(), 
                                        "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    
    private String buildConnectionUrl(String dbType, String host, String port) {
        switch (dbType) {
            case "MySQL":
                return "jdbc:mysql://" + host + ":" + port + "/";
            case "PostgreSQL":
                return "jdbc:postgresql://" + host + ":" + port + "/";
            case "SQLite":
                return "jdbc:sqlite:" + host; // For SQLite, host field should contain file path
            default:
                return "";
        }
    }
    
    private void loadDatabases() {
        try {
            rootNode.removeAllChildren();
            
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet databases = metaData.getCatalogs();
            
            while (databases.next()) {
                String dbName = databases.getString("TABLE_CAT");
                if (!isSystemDatabase(dbName)) {
                    DefaultMutableTreeNode dbNode = new DefaultMutableTreeNode(dbName);
                    dbNode.setUserObject(new DatabaseInfo(dbName, false)); // Not expanded initially
                    rootNode.add(dbNode);
                }
            }
            
            treeModel.reload();
            databaseTree.expandPath(new TreePath(rootNode.getPath()));
        } catch (SQLException e) {
            showError("Failed to load databases: " + e.getMessage());
        }
    }
    
    private boolean isSystemDatabase(String dbName) {
        return dbName.equals("information_schema") || 
               dbName.equals("mysql") || 
               dbName.equals("performance_schema") ||
               dbName.equals("sys");
    }
    
    private void handleTreeDoubleClick(DefaultMutableTreeNode node) {
        Object userObject = node.getUserObject();
        if (userObject instanceof DatabaseInfo) {
            DatabaseInfo dbInfo = (DatabaseInfo) userObject;
            if (!dbInfo.isExpanded()) {
                loadTables(node, dbInfo.getName());
                dbInfo.setExpanded(true);
            }
        }
    }
    
    private void handleTreeSingleClick(DefaultMutableTreeNode node) {
        Object userObject = node.getUserObject();
        if (userObject instanceof TableInfo) {
            TableInfo tableInfo = (TableInfo) userObject;
            String query = "SELECT * FROM " + tableInfo.getName() + " LIMIT 100;";
            queryArea.setText(query);
            executeQuery();
        }
    }
    
    private void loadTables(DefaultMutableTreeNode dbNode, String databaseName) {
        try {
            // Switch to the selected database
            connection.setCatalog(databaseName);
            
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet tables = metaData.getTables(databaseName, null, "%", new String[]{"TABLE"});
            
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                DefaultMutableTreeNode tableNode = new DefaultMutableTreeNode(tableName);
                tableNode.setUserObject(new TableInfo(tableName));
                dbNode.add(tableNode);
            }
            
            treeModel.reload(dbNode);
            databaseTree.expandPath(new TreePath(dbNode.getPath()));
        } catch (SQLException e) {
            showError("Failed to load tables: " + e.getMessage());
        }
    }
    
    private void executeQuery() {
        String query = queryArea.getText().trim();
        if (query.isEmpty()) {
            showError("Please enter a query");
            return;
        }
        
        try {
            Statement stmt = connection.createStatement();
            
            // Check if it's a SELECT query
            if (query.toUpperCase().trim().startsWith("SELECT")) {
                ResultSet rs = stmt.executeQuery(query);
                displayResultSet(rs);
                resultPane.setSelectedIndex(0); // Show table tab
            } else {
                int updateCount = stmt.executeUpdate(query);
                String message = "Query executed successfully. Rows affected: " + updateCount;
                resultTextArea.setText(message);
                resultPane.setSelectedIndex(1); // Show messages tab
                
                // If it's a DDL query, refresh the database navigator
                String upperQuery = query.toUpperCase().trim();
                if (upperQuery.startsWith("CREATE") || upperQuery.startsWith("DROP") || 
                    upperQuery.startsWith("ALTER")) {
                    loadDatabases();
                }
            }
        } catch (SQLException e) {
            showError("Query execution failed: " + e.getMessage());
            resultPane.setSelectedIndex(1); // Show messages tab
        }
    }
    
    private void displayResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        // Clear previous data
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);
        
        // Set column names
        String[] columnNames = new String[columnCount];
        for (int i = 1; i <= columnCount; i++) {
            columnNames[i - 1] = metaData.getColumnName(i);
        }
        tableModel.setColumnIdentifiers(columnNames);
        
        // Add rows
        while (rs.next()) {
            Object[] row = new Object[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                row[i - 1] = rs.getObject(i);
            }
            tableModel.addRow(row);
        }
        
        // Auto-resize columns
        for (int i = 0; i < resultTable.getColumnCount(); i++) {
            resultTable.getColumnModel().getColumn(i).setPreferredWidth(100);
        }
    }
    
    private void showError(String message) {
        resultTextArea.setText("ERROR: " + message);
        resultPane.setSelectedIndex(1); // Show messages tab
    }
    
    // Helper classes
    private static class DatabaseInfo {
        private String name;
        private boolean expanded;
        
        public DatabaseInfo(String name, boolean expanded) {
            this.name = name;
            this.expanded = expanded;
        }
        
        public String getName() { return name; }
        public boolean isExpanded() { return expanded; }
        public void setExpanded(boolean expanded) { this.expanded = expanded; }
        
        @Override
        public String toString() { return name; }
    }
    
    private static class TableInfo {
        private String name;
        
        public TableInfo(String name) {
            this.name = name;
        }
        
        public String getName() { return name; }
        
        @Override
        public String toString() { return name; }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            new DatabaseClient().setVisible(true);
        });
    }
}