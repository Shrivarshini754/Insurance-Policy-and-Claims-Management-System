package frontend;

import service.BackendService;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * CustomerPanel - Customer Management screen.
 *
 * Displays a searchable, sortable customer table and a full CRUD form.
 *
 * Integration boundary:
 *   The panel now integrates directly with BackendService.
 *
 * Does NOT import or use:
 *   Connection, DriverManager, PreparedStatement,
 *   CallableStatement, ResultSet  — no JDBC anywhere in this class.
 */
public class CustomerPanel extends JPanel {

    // -----------------------------------------------------------------------
    //  Palette (matches MainFrame / DashboardPanel exactly)
    // -----------------------------------------------------------------------
    private static final Color CLR_BG          = new Color(241, 245, 249);  // slate-100
    private static final Color CLR_CARD        = Color.WHITE;
    private static final Color CLR_BORDER      = new Color(226, 232, 240);  // slate-200
    private static final Color CLR_ACCENT      = new Color(56,  189, 248);  // sky-400
    private static final Color CLR_ACCENT_DARK = new Color(14,  116, 144);  // sky-700
    private static final Color CLR_TEXT_PRI    = new Color(15,   23,  42);  // slate-900
    private static final Color CLR_TEXT_SEC    = new Color(100, 116, 139);  // slate-500
    private static final Color CLR_BTN_FG      = Color.WHITE;
    private static final Color CLR_SEL_BG      = new Color(224, 242, 254);  // sky-100
    private static final Color CLR_DANGER      = new Color(220,  38,  38);  // red-600
    private static final Color CLR_WARNING_BG  = new Color(254, 243, 199);  // amber-100
    private static final Color CLR_WARNING_FG  = new Color(146,  64,  14);  // amber-800
    private static final Color CLR_SUCCESS_BG  = new Color(220, 252, 231);  // green-100
    private static final Color CLR_SUCCESS_FG  = new Color( 22, 163,  74);  // green-600
    private static final Color CLR_ERROR_BG    = new Color(254, 226, 226);  // red-100
    private static final Color CLR_ERROR_FG    = new Color(185,  28,  28);  // red-700
    private static final Color CLR_MUTED_BG    = new Color(241, 245, 249);  // slate-100
    private static final Color CLR_MUTED_FG    = new Color(100, 116, 139);  // slate-500

    // -----------------------------------------------------------------------
    //  Table columns
    // -----------------------------------------------------------------------
    private static final String[] COLUMNS = {
        "Person ID", "First Name", "DOB", "Email", "Aadhar", "Address"
    };

    // -----------------------------------------------------------------------
    //  UI state
    // -----------------------------------------------------------------------
    private DefaultTableModel tableModel;
    private JTable            customerTable;
    private JTextField        searchField;
    private JLabel            statusLabel;
    private JLabel            rowCountLabel;

    // Form fields
    private JTextField fldPersonId;
    private JTextField fldFirstName;
    private JTextField fldDob;
    private JTextField fldEmail;
    private JTextField fldAadhar;
    private JTextField fldAddress;

    // Buttons whose enabled state changes based on selection / mode
    private JButton btnUpdate;
    private JButton btnDelete;
    private JButton btnSaveAdd;

    // Tracks whether the form is in "edit" mode (row selected) or "add" mode
    private boolean editMode = false;

    // Backend integration
    private BackendService backendService;

    // -----------------------------------------------------------------------
    //  Constructor
    // -----------------------------------------------------------------------
    public CustomerPanel(BackendService backendService) {
        this.backendService = backendService;
        
        setBackground(CLR_BG);
        setLayout(new BorderLayout());

        add(buildPageHeader(), BorderLayout.NORTH);
        add(buildBody(),       BorderLayout.CENTER);
        add(buildStatusBar(),  BorderLayout.SOUTH);

        // Start with Update/Delete disabled — they need a selection
        updateButtonStates();

        setStatus("Ready", StatusType.MUTED);
        
        // Initialize the table with actual data
        loadCustomers();
    }

    // =======================================================================
    //  PUBLIC INTEGRATION API
    // =======================================================================

    /**
     * Replace the table contents with the supplied rows.
     * Each String[] must have 6 elements: Person_ID, F_Name, DOB, Email, Aadhar, Address.
     * Call this from the integration layer after a successful load or mutation.
     * Does NOT perform any database access.
     */
    public void setCustomerData(List<String[]> data) {
        tableModel.setRowCount(0);
        if (data != null) {
            for (String[] row : data) {
                tableModel.addRow(row);
            }
        }
        updateRowCountLabel();
        clearForm();
    }

    /**
     * Ask the integration layer to reload all customers and refresh the table.
     */
    public void loadCustomers() {
        if (backendService == null) {
            setStatus("Backend integration pending.", StatusType.WARNING);
            return;
        }
        setStatus("Loading customers...", StatusType.MUTED);
        List<String[]> customers = backendService.displayCustomers();
        setCustomerData(customers);
        int count = tableModel.getRowCount();
        setStatus(count == 0 ? "No customers found." : count + " customer(s) loaded.", StatusType.MUTED);
    }

    // =======================================================================
    //  PAGE HEADER
    // =======================================================================
    private JPanel buildPageHeader() {
        JPanel header = new JPanel();
        header.setBackground(CLR_BG);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(24, 32, 16, 32));

        JLabel title = new JLabel("Customers");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(CLR_TEXT_PRI);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Manage registered customers");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(CLR_TEXT_SEC);
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(subtitle);
        return header;
    }

    // =======================================================================
    //  BODY  (toolbar + table on the left, form on the right)
    // =======================================================================
    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(0, 32, 16, 32));

        body.add(buildToolbar(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                buildTableCard(),
                buildFormCard());
        split.setBorder(null);
        split.setOpaque(false);
        split.setDividerSize(10);
        split.setResizeWeight(0.65);
        split.setDividerLocation(0.65);
        body.add(split, BorderLayout.CENTER);

        return body;
    }

    // =======================================================================
    //  TOOLBAR
    // =======================================================================
    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);

        // Search field
        searchField = new JTextField(20);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setPreferredSize(new Dimension(220, 34));
        searchField.setBorder(new CompoundBorder(
                new LineBorder(CLR_BORDER, 1, true),
                new EmptyBorder(4, 10, 4, 10)));
        searchField.setToolTipText("Enter Person ID to search");
        // Allow pressing Enter in the search field to trigger search
        searchField.addActionListener(e -> handleSearch());
        bar.add(searchField);

        JButton btnSearch = makeButton("Search", CLR_ACCENT_DARK, CLR_BTN_FG);
        btnSearch.setToolTipText("Search by Person ID via backend");
        btnSearch.addActionListener(e -> handleSearch());
        bar.add(btnSearch);

        bar.add(Box.createHorizontalStrut(16));

        JButton btnAdd = makeButton("+ Add Customer", CLR_ACCENT_DARK, CLR_BTN_FG);
        btnAdd.setToolTipText("Clear form to enter a new customer");
        btnAdd.addActionListener(e -> enterAddMode());
        bar.add(btnAdd);

        JButton btnRefresh = makeButton("Refresh", new Color(71, 85, 105), CLR_BTN_FG);
        btnRefresh.setToolTipText("Reload all customers from the backend");
        btnRefresh.addActionListener(e -> {
            searchField.setText("");
            loadCustomers();
        });
        bar.add(btnRefresh);

        return bar;
    }

    // =======================================================================
    //  TABLE CARD
    // =======================================================================
    private JPanel buildTableCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CLR_CARD);
        card.setBorder(new CompoundBorder(
                new LineBorder(CLR_BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)));

        // Card header row: title + row count pill
        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setOpaque(false);
        cardHeader.setBorder(new EmptyBorder(0, 0, 12, 0));

        JLabel cardTitle = new JLabel("Customer Records");
        cardTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        cardTitle.setForeground(CLR_TEXT_PRI);
        cardHeader.add(cardTitle, BorderLayout.WEST);

        rowCountLabel = new JLabel("0 records");
        rowCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        rowCountLabel.setForeground(CLR_TEXT_SEC);
        cardHeader.add(rowCountLabel, BorderLayout.EAST);

        card.add(cardHeader, BorderLayout.NORTH);

        // Table — populated via setCustomerData()
        tableModel = new DefaultTableModel(new Object[0][], COLUMNS) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        customerTable = new JTable(tableModel);
        customerTable.setRowHeight(28);
        customerTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        customerTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        customerTable.getTableHeader().setBackground(new Color(248, 250, 252));
        customerTable.getTableHeader().setForeground(CLR_TEXT_PRI);
        customerTable.setSelectionBackground(CLR_SEL_BG);
        customerTable.setSelectionForeground(CLR_TEXT_PRI);
        customerTable.setShowVerticalLines(false);
        customerTable.setGridColor(CLR_BORDER);
        customerTable.setFillsViewportHeight(true);
        customerTable.setIntercellSpacing(new Dimension(8, 0));
        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Column widths
        int[] colWidths = { 80, 140, 95, 190, 130, 190 };
        for (int i = 0; i < colWidths.length; i++) {
            customerTable.getColumnModel().getColumn(i).setPreferredWidth(colWidths[i]);
        }

        // Populate form when a row is selected
        customerTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onTableSelectionChanged();
        });

        JScrollPane scroll = new JScrollPane(customerTable);
        scroll.setBorder(new LineBorder(CLR_BORDER, 1, true));
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    // =======================================================================
    //  FORM CARD
    // =======================================================================
    private JPanel buildFormCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CLR_CARD);
        card.setBorder(new CompoundBorder(
                new LineBorder(CLR_BORDER, 1, true),
                new EmptyBorder(20, 20, 20, 20)));

        JLabel cardTitle = new JLabel("Customer Details");
        cardTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        cardTitle.setForeground(CLR_TEXT_PRI);
        cardTitle.setBorder(new EmptyBorder(0, 0, 16, 0));
        card.add(cardTitle, BorderLayout.NORTH);

        // Form grid
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 0, 6, 8);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill   = GridBagConstraints.HORIZONTAL;

        fldPersonId  = new JTextField();
        fldFirstName = new JTextField();
        fldDob       = new JTextField();
        fldEmail     = new JTextField();
        fldAadhar    = new JTextField();
        fldAddress   = new JTextField();

        fldDob.setToolTipText("Format: YYYY-MM-DD  (e.g. 1990-06-15)");
        fldAadhar.setToolTipText("12-digit Aadhar number");
        fldEmail.setToolTipText("Valid e-mail address");

        JTextField[] fields = { fldPersonId, fldFirstName, fldDob, fldEmail, fldAadhar, fldAddress };
        String[]     labels = { "Person ID *", "First Name *", "DOB *", "Email *", "Aadhar *", "Address *" };

        for (int i = 0; i < labels.length; i++) {
            gc.gridx = 0; gc.gridy = i; gc.weightx = 0;
            form.add(makeFormLabel(labels[i]), gc);

            gc.gridx = 1; gc.weightx = 1.0;
            styleFormField(fields[i]);
            form.add(fields[i], gc);
        }

        card.add(form, BorderLayout.CENTER);
        card.add(buildFormButtons(), BorderLayout.SOUTH);

        return card;
    }

    private JPanel buildFormButtons() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(16, 0, 0, 0));

        btnSaveAdd = makeButton("Save / Add",  CLR_ACCENT_DARK,              CLR_BTN_FG);
        btnUpdate  = makeButton("Update",      new Color(  5, 150, 105),     CLR_BTN_FG); // emerald-600
        btnDelete  = makeButton("Delete",      CLR_DANGER,                   CLR_BTN_FG);
        JButton btnClear = makeButton("Clear", new Color( 71,  85, 105),     CLR_BTN_FG);

        btnSaveAdd.setToolTipText("Validate and add a new customer via backend");
        btnUpdate .setToolTipText("Update the selected customer via backend");
        btnDelete .setToolTipText("Delete the selected customer after confirmation");
        btnClear  .setToolTipText("Clear all form fields and deselect table row");

        btnSaveAdd.addActionListener(e -> handleAdd());
        btnUpdate .addActionListener(e -> handleUpdate());
        btnDelete .addActionListener(e -> handleDelete());
        btnClear  .addActionListener(e -> {
            clearForm();
            setStatus("Ready", StatusType.MUTED);
        });

        panel.add(btnSaveAdd);
        panel.add(btnUpdate);
        panel.add(btnDelete);
        panel.add(btnClear);

        return panel;
    }

    // =======================================================================
    //  STATUS BAR
    // =======================================================================
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(248, 250, 252));
        bar.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, CLR_BORDER),
                new EmptyBorder(6, 32, 6, 32)));

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        styleStatus(StatusType.MUTED);
        bar.add(statusLabel, BorderLayout.WEST);

        JLabel hint = new JLabel("* Required fields");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(CLR_TEXT_SEC);
        bar.add(hint, BorderLayout.EAST);

        return bar;
    }

    // =======================================================================
    //  EVENT HANDLERS
    // =======================================================================

    /** Called when the table selection changes. */
    private void onTableSelectionChanged() {
        int row = customerTable.getSelectedRow();
        if (row < 0) {
            // No selection — go back to add mode
            editMode = false;
            updateButtonStates();
            return;
        }
        // Populate form from the selected row
        fldPersonId .setText(tableModel.getValueAt(row, 0).toString());
        fldFirstName.setText(tableModel.getValueAt(row, 1).toString());
        fldDob      .setText(tableModel.getValueAt(row, 2).toString());
        fldEmail    .setText(tableModel.getValueAt(row, 3).toString());
        fldAadhar   .setText(tableModel.getValueAt(row, 4).toString());
        fldAddress  .setText(tableModel.getValueAt(row, 5).toString());

        // Person ID is not editable once a record exists
        fldPersonId.setEditable(false);
        fldPersonId.setBackground(new Color(248, 250, 252));

        editMode = true;
        updateButtonStates();
        setStatus("Customer selected. Edit fields and click Update, or click Delete to remove.", StatusType.MUTED);
    }

    /** Enter clean Add mode — clear form, allow all fields to be typed. */
    private void enterAddMode() {
        clearForm();
        editMode = false;
        updateButtonStates();
        fldPersonId.requestFocusInWindow();
        setStatus("Enter customer details and click Save / Add.", StatusType.MUTED);
    }

    /** Search by Person ID via the integration boundary. */
    private void handleSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            setStatus("Please enter a Person ID to search.", StatusType.WARNING);
            searchField.requestFocusInWindow();
            return;
        }

        if (backendService == null) {
            setStatus("Backend integration pending.", StatusType.WARNING);
            return;
        }

        setStatus("Searching for \"" + query + "\"...", StatusType.MUTED);
        String[] result = backendService.findCustomer(query);

        if (result == null) {
            setStatus("No customer found with Person ID: " + query, StatusType.WARNING);
            clearForm();
            return;
        }

        // Show the single result in the table and populate the form
        tableModel.setRowCount(0);
        tableModel.addRow(result);
        updateRowCountLabel();

        // Select the row and let the selection listener populate the form
        customerTable.setRowSelectionInterval(0, 0);
        setStatus("Customer found.", StatusType.SUCCESS);
    }

    /** Validate and submit an Add request to the integration layer. */
    private void handleAdd() {
        String[] values = collectAndValidateForm();
        if (values == null) return;   // validation failed; status already set

        if (backendService == null) {
            setStatus("Backend integration pending.", StatusType.WARNING);
            return;
        }

        boolean ok = backendService.addCustomer(
            values[0], values[1], values[2], values[3], values[4], values[5]
        );

        if (ok) {
            JOptionPane.showMessageDialog(this, "Customer added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            setStatus("Customer added successfully. Refreshing table...", StatusType.SUCCESS);
            loadCustomers();
        } else {
            JOptionPane.showMessageDialog(this, "Add failed. The Person ID may already exist or a required field is invalid.", "Error", JOptionPane.ERROR_MESSAGE);
            setStatus("Add failed. The Person ID may already exist or a required field is invalid.", StatusType.ERROR);
        }
    }

    /** Validate and submit an Update request to the integration layer. */
    private void handleUpdate() {
        if (!editMode || customerTable.getSelectedRow() < 0) {
            setStatus("Please select a customer from the table to update.", StatusType.WARNING);
            return;
        }

        // Re-validate all except Person ID (which is already in the table)
        String[] values = collectAndValidateForm();
        if (values == null) return;

        if (backendService == null) {
            setStatus("Backend integration pending.", StatusType.WARNING);
            return;
        }

        boolean ok = backendService.updateCustomer(
            values[0], values[1], values[2], values[3], values[4], values[5]
        );

        if (ok) {
            JOptionPane.showMessageDialog(this, "Customer updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            setStatus("Customer updated successfully. Refreshing table...", StatusType.SUCCESS);
            loadCustomers();
        } else {
            JOptionPane.showMessageDialog(this, "Update failed. The customer may not exist or the data is invalid.", "Error", JOptionPane.ERROR_MESSAGE);
            setStatus("Update failed. The customer may not exist or the data is invalid.", StatusType.ERROR);
        }
    }

    /** Confirm and submit a Delete request to the integration layer. */
    private void handleDelete() {
        int row = customerTable.getSelectedRow();
        if (row < 0) {
            setStatus("Please select a customer from the table to delete.", StatusType.WARNING);
            return;
        }

        String personId   = tableModel.getValueAt(row, 0).toString();
        String firstName  = tableModel.getValueAt(row, 1).toString();

        // Confirmation dialog — match existing app style
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "<html><b>Are you sure you want to delete this customer?</b><br><br>"
                + "Person ID:  " + personId + "<br>"
                + "Name:       " + firstName + "<br><br>"
                + "<i>This action cannot be undone.</i></html>",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            setStatus("Delete cancelled.", StatusType.MUTED);
            return;
        }

        if (backendService == null) {
            setStatus("Backend integration pending.", StatusType.WARNING);
            return;
        }

        boolean ok = backendService.deleteCustomer(personId);

        if (ok) {
            JOptionPane.showMessageDialog(this, "Customer deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            setStatus("Customer deleted successfully. Refreshing table...", StatusType.SUCCESS);
            loadCustomers();
        } else {
            JOptionPane.showMessageDialog(this, "Delete failed. The customer may be referenced by another record (e.g. a policy).", "Error", JOptionPane.ERROR_MESSAGE);
            setStatus(
                "Delete failed. The customer may be referenced by another record (e.g. a policy).",
                StatusType.ERROR
            );
        }
    }

    // =======================================================================
    //  FORM VALIDATION
    // =======================================================================

    /**
     * Collect form values, apply frontend validation.
     * @return String[6] of trimmed values if valid, or null if invalid
     *         (status label is already updated before returning null).
     */
    private String[] collectAndValidateForm() {
        String personId   = fldPersonId  .getText().trim();
        String firstName  = fldFirstName .getText().trim();
        String dob        = fldDob       .getText().trim();
        String email      = fldEmail     .getText().trim();
        String aadhar     = fldAadhar    .getText().trim();
        String address    = fldAddress   .getText().trim();

        // --- Required fields ---
        if (personId.isEmpty()) {
            setStatus("Person ID is required.", StatusType.ERROR);
            fldPersonId.requestFocusInWindow();
            return null;
        }
        if (firstName.isEmpty()) {
            setStatus("First Name is required.", StatusType.ERROR);
            fldFirstName.requestFocusInWindow();
            return null;
        }
        if (dob.isEmpty()) {
            setStatus("Date of Birth is required.", StatusType.ERROR);
            fldDob.requestFocusInWindow();
            return null;
        }
        if (email.isEmpty()) {
            setStatus("Email is required.", StatusType.ERROR);
            fldEmail.requestFocusInWindow();
            return null;
        }
        if (aadhar.isEmpty()) {
            setStatus("Aadhar number is required.", StatusType.ERROR);
            fldAadhar.requestFocusInWindow();
            return null;
        }
        if (address.isEmpty()) {
            setStatus("Address is required.", StatusType.ERROR);
            fldAddress.requestFocusInWindow();
            return null;
        }

        // --- DOB format: YYYY-MM-DD ---
        if (!dob.matches("\\d{4}-\\d{2}-\\d{2}")) {
            setStatus("DOB must be in YYYY-MM-DD format  (e.g. 1990-06-15).", StatusType.ERROR);
            fldDob.requestFocusInWindow();
            return null;
        }

        // --- Basic date range check ---
        try {
            int year  = Integer.parseInt(dob.substring(0, 4));
            int month = Integer.parseInt(dob.substring(5, 7));
            int day   = Integer.parseInt(dob.substring(8, 10));
            if (year < 1900 || year > 2100 || month < 1 || month > 12 || day < 1 || day > 31) {
                setStatus("DOB contains an invalid date.", StatusType.ERROR);
                fldDob.requestFocusInWindow();
                return null;
            }
        } catch (NumberFormatException ex) {
            setStatus("DOB must be in YYYY-MM-DD format.", StatusType.ERROR);
            fldDob.requestFocusInWindow();
            return null;
        }

        // --- Email format: must contain @ and at least one dot after @ ---
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            setStatus("Invalid email format  (e.g. name@example.com).", StatusType.ERROR);
            fldEmail.requestFocusInWindow();
            return null;
        }

        // --- Aadhar: exactly 12 digits ---
        if (!aadhar.matches("\\d{12}")) {
            setStatus("Aadhar must be exactly 12 digits.", StatusType.ERROR);
            fldAadhar.requestFocusInWindow();
            return null;
        }

        return new String[]{ personId, firstName, dob, email, aadhar, address };
    }

    // =======================================================================
    //  FORM HELPERS
    // =======================================================================

    /** Clear all form fields, restore editable state, deselect table row. */
    private void clearForm() {
        fldPersonId .setText("");
        fldFirstName.setText("");
        fldDob      .setText("");
        fldEmail    .setText("");
        fldAadhar   .setText("");
        fldAddress  .setText("");

        // Person ID is editable again in Add mode
        fldPersonId.setEditable(true);
        fldPersonId.setBackground(Color.WHITE);

        customerTable.clearSelection();
        editMode = false;
        updateButtonStates();
    }

    /** Enable/disable action buttons based on current mode. */
    private void updateButtonStates() {
        boolean hasSelection = editMode;
        btnUpdate .setEnabled(hasSelection);
        btnDelete .setEnabled(hasSelection);
        // Save / Add is always available (user may want to add at any time)
        btnSaveAdd.setEnabled(true);
    }

    private void updateRowCountLabel() {
        int count = tableModel.getRowCount();
        rowCountLabel.setText(count + (count == 1 ? " record" : " records"));
    }

    // =======================================================================
    //  STATUS BAR HELPER
    // =======================================================================

    private enum StatusType { MUTED, SUCCESS, WARNING, ERROR }

    private void setStatus(String message, StatusType type) {
        statusLabel.setText(message);
        styleStatus(type);
    }

    private void styleStatus(StatusType type) {
        switch (type) {
            case SUCCESS -> {
                statusLabel.setForeground(CLR_SUCCESS_FG);
            }
            case WARNING -> {
                statusLabel.setForeground(CLR_WARNING_FG);
            }
            case ERROR -> {
                statusLabel.setForeground(CLR_ERROR_FG);
            }
            default -> {
                statusLabel.setForeground(CLR_MUTED_FG);
            }
        }
    }

    // =======================================================================
    //  UI HELPERS
    // =======================================================================

    /** Styled button matching the application palette. */
    private JButton makeButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                Color draw = isEnabled()
                        ? (getModel().isRollover() ? bg.darker() : bg)
                        : new Color(200, 200, 200);
                g2.setColor(draw);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(fg);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(7, 14, 7, 14));
        return btn;
    }

    /** Form label with muted styling. */
    private JLabel makeFormLabel(String text) {
        JLabel lbl = new JLabel(text + ":");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(CLR_TEXT_SEC);
        return lbl;
    }

    /** Apply consistent styling to a form text field. */
    private void styleFormField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBackground(Color.WHITE);
        field.setPreferredSize(new Dimension(0, 30));
        field.setBorder(new CompoundBorder(
                new LineBorder(CLR_BORDER, 1, true),
                new EmptyBorder(4, 8, 4, 8)));
    }
}