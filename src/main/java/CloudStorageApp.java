import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

// Add import for SupabaseStorageUtil
import java.util.Objects;
import java.util.List;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;

public class CloudStorageApp extends JFrame {
    private JPanel contentPane;
    private JTable fileTable;
    private JTextField searchField;
    private JButton uploadButton, downloadButton, newFolderButton, deleteButton, shareButton;
    private JLabel statusLabel, storageLabel;
    private JTree folderTree;
    private boolean isLoggedIn = true;
    private String currentUser;
    
    // Remove the sample data and initialize as empty
    private String[][] fileData = {};
    
    private String[] columnNames = {"Name", "Type", "Size", "Modified Date"};

    private String currentUserId;
    
    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    // Remove direct instantiation since we'll create it from LoginPage
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the frame.
     */
    public CloudStorageApp(User user) {
        this.currentUser = user.getUsername();
        this.currentUserId = String.valueOf(user.getId());
        setTitle("Cloud Storage System - Welcome " + currentUser);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 1000, 600);
        initializeComponents();
        setVisible(true);
    }
    
    private void initializeComponents() {
        // Main content pane
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);
        
        // Create menu bar
        createMenuBar();
        
        // Create toolbar
        createToolbar();
        
        // Create main split pane
        JSplitPane splitPane = new JSplitPane();
        splitPane.setDividerLocation(200);
        contentPane.add(splitPane, BorderLayout.CENTER);
        
        // Create folder tree
        createFolderTree(splitPane);
        
        // Create file table panel
        createFilePanel(splitPane);
        
        // Create status bar
        createStatusBar();
    }
    
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        
        JMenuItem uploadItem = new JMenuItem("Upload Files...");
        fileMenu.add(uploadItem);
        
        JMenuItem downloadItem = new JMenuItem("Download Selected");
        fileMenu.add(downloadItem);
        
        fileMenu.addSeparator();
        
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        
        JMenu editMenu = new JMenu("Edit");
        menuBar.add(editMenu);
        
        JMenuItem renameItem = new JMenuItem("Rename");
        editMenu.add(renameItem);
        
        JMenuItem deleteItem = new JMenuItem("Delete");
        editMenu.add(deleteItem);
        
        JMenu viewMenu = new JMenu("View");
        menuBar.add(viewMenu);
        
        JMenuItem refreshItem = new JMenuItem("Refresh");
        viewMenu.add(refreshItem);
        
        JMenu helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);
        
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> 
            JOptionPane.showMessageDialog(this, 
                "Cloud Storage System v1.0\n© 2024 Cloud Storage Inc.", 
                "About", 
                JOptionPane.INFORMATION_MESSAGE)
        );
        helpMenu.add(aboutItem);
    }
    
    private void createToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        contentPane.add(toolBar, BorderLayout.NORTH);
        
        uploadButton = new JButton("Upload");
        uploadButton.setToolTipText("Upload files to cloud");
        uploadButton.addActionListener(e -> uploadFiles());
        toolBar.add(uploadButton);
        
        downloadButton = new JButton("Download");
        downloadButton.setToolTipText("Download selected files");
        downloadButton.addActionListener(e -> downloadFiles());
        downloadButton.setEnabled(false);
        toolBar.add(downloadButton);
        
        toolBar.addSeparator();
        
        newFolderButton = new JButton("New Folder");
        newFolderButton.setToolTipText("Create new folder");
        newFolderButton.addActionListener(e -> createNewFolder());
        toolBar.add(newFolderButton);
        
        deleteButton = new JButton("Delete");
        deleteButton.setToolTipText("Delete selected files");
        deleteButton.addActionListener(e -> deleteFiles());
        deleteButton.setEnabled(false);
        toolBar.add(deleteButton);
        
        toolBar.addSeparator();
        
        shareButton = new JButton("Share");
        shareButton.setToolTipText("Share selected files");
        shareButton.addActionListener(e -> shareFiles());
        shareButton.setEnabled(false);
        toolBar.add(shareButton);
        
        toolBar.add(Box.createHorizontalGlue());
        
        JLabel searchLabel = new JLabel("Search: ");
        toolBar.add(searchLabel);
        
        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(200, 25));
        // Add document listener for real-time search
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { searchFiles(); }
            public void removeUpdate(DocumentEvent e) { searchFiles(); }
            public void insertUpdate(DocumentEvent e) { searchFiles(); }
        });
        toolBar.add(searchField);
    }
    
    private void createFolderTree(JSplitPane splitPane) {
        // Create root node
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("My Cloud");
        
        // Create child nodes
        DefaultMutableTreeNode documents = new DefaultMutableTreeNode("Documents");
        DefaultMutableTreeNode images = new DefaultMutableTreeNode("Images");
        DefaultMutableTreeNode videos = new DefaultMutableTreeNode("Videos");
        DefaultMutableTreeNode music = new DefaultMutableTreeNode("Music");
        DefaultMutableTreeNode shared = new DefaultMutableTreeNode("Shared with me");
        
        // Add child nodes to root
        root.add(documents);
        root.add(images);
        root.add(videos);
        root.add(music);
        root.add(shared);
        
        // Add some sub-folders
        documents.add(new DefaultMutableTreeNode("Work"));
        documents.add(new DefaultMutableTreeNode("Personal"));
        images.add(new DefaultMutableTreeNode("Vacation"));
        images.add(new DefaultMutableTreeNode("Family"));
        
        // Create the tree
        folderTree = new JTree(root);
        folderTree.setShowsRootHandles(true);
        
        // Add tree to a scroll pane
        JScrollPane treeScrollPane = new JScrollPane(folderTree);
        
        // Create a panel with vertical layout for the left side
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout());
        leftPanel.add(treeScrollPane, BorderLayout.CENTER);
        
        // Bottom panel for user profile button and logged in label
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        JButton userProfileButton = new JButton("User Profile");
        userProfileButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        userProfileButton.addActionListener(e -> showUserProfile());
        bottomPanel.add(userProfileButton);
        bottomPanel.add(Box.createVerticalStrut(8));
        
        JLabel loggedInLabel = new JLabel("Logged in as: " + currentUser);
        loggedInLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        bottomPanel.add(loggedInLabel);
        
        leftPanel.add(bottomPanel, BorderLayout.SOUTH);
        splitPane.setLeftComponent(leftPanel);
    }
    
    private void createFilePanel(JSplitPane splitPane) {
        JPanel filePanel = new JPanel(new BorderLayout());
        
        // Create table model with data
        DefaultTableModel model = new DefaultTableModel(fileData, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        // Create table
        fileTable = new JTable(model);
        fileTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fileTable.setRowHeight(25);
        fileTable.getTableHeader().setReorderingAllowed(false);
        
        // Add selection listener
        fileTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                boolean hasSelection = fileTable.getSelectedRow() != -1;
                downloadButton.setEnabled(hasSelection);
                deleteButton.setEnabled(hasSelection);
                shareButton.setEnabled(hasSelection);
            }
        });
        
        // Add table to scroll pane
        JScrollPane tableScrollPane = new JScrollPane(fileTable);
        filePanel.add(tableScrollPane, BorderLayout.CENTER);
        
        // Create bottom panel for logout button only
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> logout());
        rightPanel.add(logoutButton);
        bottomPanel.add(rightPanel, BorderLayout.EAST);
        filePanel.add(bottomPanel, BorderLayout.SOUTH);
        
        // Add to split pane
        splitPane.setRightComponent(filePanel);
    }
    
    private void createStatusBar() {
        JPanel statusPanel = new JPanel();
        statusPanel.setBorder(new EmptyBorder(2, 5, 2, 5));
        statusPanel.setLayout(new BorderLayout());
        
        statusLabel = new JLabel();
        statusPanel.add(statusLabel, BorderLayout.WEST);
        
        storageLabel = new JLabel("Storage: 10.9 MB / 15 GB used");
        statusPanel.add(storageLabel, BorderLayout.EAST);
        
        contentPane.add(statusPanel, BorderLayout.SOUTH);
        
        updateStatusBar();
    }
    
    private void updateStatusBar() {
        if (isLoggedIn) {
            statusLabel.setText("");
        } else {
            statusLabel.setText("Not logged in");
        }
    }
    
    // Action methods
    private void uploadFiles() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            for (File file : files) {
                try {
                    System.out.println("Attempting to upload file: " + file.getName());
                    byte[] fileContent = Files.readAllBytes(file.toPath());
                    System.out.println("File size: " + fileContent.length + " bytes");
                    
                    boolean success = SupabaseStorageUtil.uploadFile(currentUserId, file.getName(), fileContent);
                    if (!success) {
                        throw new IOException("Supabase upload failed - check server response");
                    }
                    
                    DefaultTableModel model = (DefaultTableModel) fileTable.getModel();
                    String type = getFileType(file.getName());
                    String size = formatFileSize(file.length());
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                    String date = sdf.format(new Date(file.lastModified()));
                    model.addRow(new Object[]{file.getName(), type, size, date});
                    
                    System.out.println("File uploaded successfully: " + file.getName());
                    JOptionPane.showMessageDialog(this,
                        "File uploaded successfully: " + file.getName(),
                        "Upload Success",
                        JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException e) {
                    System.err.println("Upload error: " + e.getMessage());
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this,
                        "Failed to upload file: " + e.getMessage() + "\nPlease check your internet connection and try again.",
                        "Upload Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    private String getFileType(String fileName) {
        if (fileName.lastIndexOf('.') != -1 && fileName.lastIndexOf('.') != 0) {
            String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
            switch (extension.toLowerCase()) {
                case "doc":
                case "docx":
                    return "Document";
                case "xls":
                case "xlsx":
                    return "Spreadsheet";
                case "ppt":
                case "pptx":
                    return "Presentation";
                case "jpg":
                case "jpeg":
                case "png":
                case "gif":
                    return "Image";
                case "mp3":
                case "wav":
                    return "Audio";
                case "mp4":
                case "avi":
                case "mov":
                    return "Video";
                case "txt":
                    return "Text";
                case "pdf":
                    return "PDF";
                default:
                    return extension.toUpperCase();
            }
        }
        return "Unknown";
    }
    
    private void downloadFiles() {
        int[] selectedRows = fileTable.getSelectedRows();
        if (selectedRows.length > 0) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File saveDir = fileChooser.getSelectedFile();
                for (int row : selectedRows) {
                    String fileName = (String) fileTable.getValueAt(row, 0);
                    try {
                        byte[] fileContent = SupabaseStorageUtil.downloadFile(currentUserId, fileName);
                        File outputFile = new File(saveDir, fileName);
                        Files.write(outputFile.toPath(), fileContent);
                        JOptionPane.showMessageDialog(this,
                            "File downloaded successfully: " + fileName,
                            "Download Success",
                            JOptionPane.INFORMATION_MESSAGE);
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(this,
                            "Failed to download file: " + e.getMessage(),
                            "Download Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
    }
    
    private void createNewFolder() {
        String folderName = JOptionPane.showInputDialog(this,
            "Enter folder name:",
            "New Folder",
            JOptionPane.QUESTION_MESSAGE);
        if (folderName != null && !folderName.trim().isEmpty()) {
            try {
                boolean success = SupabaseStorageUtil.createFolder(currentUserId, folderName);
                if (!success) throw new IOException("Supabase folder creation failed");
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)
                    folderTree.getLastSelectedPathComponent();
                if (selectedNode == null) {
                    selectedNode = (DefaultMutableTreeNode) folderTree.getModel().getRoot();
                }
                DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(folderName);
                selectedNode.add(newNode);
                ((DefaultTreeModel) folderTree.getModel()).reload(selectedNode);
                JOptionPane.showMessageDialog(this,
                    "Folder created successfully: " + folderName,
                    "Folder Created",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Failed to create folder: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void deleteFiles() {
        int[] selectedRows = fileTable.getSelectedRows();
        if (selectedRows.length > 0) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete the selected files?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                DefaultTableModel model = (DefaultTableModel) fileTable.getModel();
                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    String fileName = (String) fileTable.getValueAt(selectedRows[i], 0);
                    try {
                        boolean success = SupabaseStorageUtil.deleteFile(currentUserId, fileName);
                        if (!success) throw new IOException("Supabase delete failed");
                        model.removeRow(selectedRows[i]);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(this,
                            "Failed to delete file: " + e.getMessage(),
                            "Delete Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
    }
    
    private void shareFiles() {
        int[] selectedRows = fileTable.getSelectedRows();
        if (selectedRows.length > 0) {
            StringBuilder files = new StringBuilder();
            for (int row : selectedRows) {
                String fileName = (String) fileTable.getValueAt(row, 0);
                try {
                    String shareUrl = SupabaseStorageUtil.shareFile(currentUserId, fileName);
                    files.append(fileName).append(" - ").append(shareUrl).append("\n");
                } catch (IOException e) {
                    files.append(fileName).append(" - Failed to generate share URL: ").append(e.getMessage()).append("\n");
                }
            }
            
            JOptionPane.showMessageDialog(this, 
                "Share URLs for selected files:\n\n" + files.toString(), 
                "Share URLs", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void searchFiles() {
        String searchTerm = searchField.getText().trim().toLowerCase();
        DefaultTableModel model = (DefaultTableModel) fileTable.getModel();
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        fileTable.setRowSorter(sorter);
        
        if (searchTerm.isEmpty()) {
            sorter.setRowFilter(null);
            return;
        }
        
        // Create a filter that checks if any column contains the search term
        RowFilter<DefaultTableModel, Object> filter = new RowFilter<DefaultTableModel, Object>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Object> entry) {
                for (int i = 0; i < entry.getValueCount(); i++) {
                    if (entry.getStringValue(i).toLowerCase().contains(searchTerm)) {
                        return true;
                    }
                }
                return false;
            }
        };
        
        sorter.setRowFilter(filter);
        
        // Update status bar with search results
        int visibleRows = fileTable.getRowCount();
        statusLabel.setText("Found " + visibleRows + " matching files");
    }
    
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return (size / 1024) + " KB";
        if (size < 1024 * 1024 * 1024) return (size / (1024 * 1024)) + " MB";
        return (size / (1024 * 1024 * 1024)) + " GB";
    }
    
    private void refreshFileList() {
        try {
            List<FileInfo> files = SupabaseStorageUtil.listFiles(currentUserId);
            DefaultTableModel model = (DefaultTableModel) fileTable.getModel();
            model.setRowCount(0);
            
            for (FileInfo file : files) {
                model.addRow(new Object[]{
                    file.getName(),
                    file.getFormattedSize(),
                    file.getLastModified()
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error refreshing file list: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showUserProfile() {
        JDialog profileDialog = new JDialog(this, "User Profile", true);
        profileDialog.setLayout(new BorderLayout());
        profileDialog.setSize(500, 400);
        profileDialog.setLocationRelativeTo(this);

        JPanel profilePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel titleLabel = new JLabel("User Profile Details", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 20, 10);
        profilePanel.add(titleLabel, gbc);

        // Reset insets for other components
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridwidth = 1;

        // User details section
        gbc.gridx = 0;
        gbc.gridy = 1;
        profilePanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        profilePanel.add(new JLabel(currentUser), gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        profilePanel.add(new JLabel("User ID:"), gbc);
        gbc.gridx = 1;
        profilePanel.add(new JLabel(currentUserId), gbc);

        // Storage section
        gbc.gridx = 0;
        gbc.gridy = 3;
        profilePanel.add(new JLabel("Storage Used:"), gbc);
        gbc.gridx = 1;
        profilePanel.add(new JLabel(storageLabel.getText()), gbc);

        // File statistics
        gbc.gridx = 0;
        gbc.gridy = 4;
        profilePanel.add(new JLabel("Total Files:"), gbc);
        gbc.gridx = 1;
        profilePanel.add(new JLabel(String.valueOf(fileTable.getRowCount())), gbc);

        // Last login
        gbc.gridx = 0;
        gbc.gridy = 5;
        profilePanel.add(new JLabel("Last Login:"), gbc);
        gbc.gridx = 1;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        profilePanel.add(new JLabel(sdf.format(new Date())), gbc);

        // Account status
        gbc.gridx = 0;
        gbc.gridy = 6;
        profilePanel.add(new JLabel("Account Status:"), gbc);
        gbc.gridx = 1;
        profilePanel.add(new JLabel("Active"), gbc);

        // Add some spacing
        gbc.gridy = 7;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        profilePanel.add(Box.createVerticalStrut(20), gbc);

        // Add buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> profileDialog.dispose());
        buttonPanel.add(closeButton);

        // Add edit profile button
        JButton editButton = new JButton("Edit Profile");
        editButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(profileDialog,
                "Profile editing functionality will be available in the next update.",
                "Coming Soon",
                JOptionPane.INFORMATION_MESSAGE);
        });
        buttonPanel.add(editButton);

        profileDialog.add(profilePanel, BorderLayout.CENTER);
        profileDialog.add(buttonPanel, BorderLayout.SOUTH);
        profileDialog.setVisible(true);
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to logout?",
            "Confirm Logout",
            JOptionPane.YES_NO_OPTION);
            
        if (confirm == JOptionPane.YES_OPTION) {
            // Clear user data
            currentUser = null;
            currentUserId = null;
            isLoggedIn = false;
            
            // Clear file table
            DefaultTableModel model = (DefaultTableModel) fileTable.getModel();
            model.setRowCount(0);
            
            // Update status
            updateStatusBar();
            
            // Close current window
            this.dispose();
            
            // Open login window
            EventQueue.invokeLater(() -> {
                try {
                    LoginPage loginPage = new LoginPage();
                    loginPage.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}