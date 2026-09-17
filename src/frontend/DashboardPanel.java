package frontend;

import service.BackendService;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class DashboardPanel extends JPanel {

    private static final Color CLR_BG_CONTENT     = new Color(241, 245, 249);
    private static final Color CLR_CARD_BG        = Color.WHITE;
    private static final Color CLR_CARD_BORDER    = new Color(226, 232, 240);
    private static final Color CLR_ACCENT         = new Color(56,  189, 248);
    private static final Color CLR_TEXT_PRIMARY   = new Color(15,   23,  42);
    private static final Color CLR_TEXT_SECONDARY = new Color(100, 116, 139);

    private static final String[] CLAIMS_COLUMNS = {
        "Claim ID", "Policy No.", "Incident Date", "Description", "Claimed Amount", "Status"
    };

    private BackendService backendService;

    // UI elements to update
    private JLabel lblCustomersCard;
    private JLabel lblPoliciesCard;
    private JLabel lblClaimsCard;
    private JLabel lblPaymentsCard;

    private JLabel lblCustomersOverview;
    private JLabel lblPoliciesOverview;
    private JLabel lblClaimsOverview;
    private JLabel lblAssetsOverview;

    private DefaultTableModel recentClaimsModel;

    public DashboardPanel(BackendService backendService) {
        this.backendService = backendService;
        
        setBackground(CLR_BG_CONTENT);
        setLayout(new BorderLayout());
        add(buildHeader(),      BorderLayout.NORTH);
        add(buildMainContent(), BorderLayout.CENTER);
        
        // Fetch and load the real database data as soon as the panel is created
        loadDashboardData();
    }

    public void loadDashboardData() {
        if (backendService == null) return;
        
        // Fetch data from backend and update UI
        setCustomerCount(backendService.getCustomerCount());
        setActivePolicyCount(backendService.getActivePolicyCount());
        setClaimsThisYearCount(backendService.getClaimsThisYearCount());
        setTotalPaymentsReceived(backendService.getTotalPaymentsReceived());

        setSystemOverview(
            backendService.getCustomerCount(),
            backendService.getPolicyCount(),
            backendService.getClaimCount(),
            backendService.getAssetCount()
        );

        setRecentClaims(backendService.getRecentClaims(10));
    }

    public void setCustomerCount(int count) {
        lblCustomersCard.setText(String.valueOf(count));
    }

    public void setActivePolicyCount(int count) {
        lblPoliciesCard.setText(String.valueOf(count));
    }

    public void setClaimsThisYearCount(int count) {
        lblClaimsCard.setText(String.valueOf(count));
    }

    public void setTotalPaymentsReceived(double amount) {
        lblPaymentsCard.setText(String.format("$%.2f", amount));
    }

    public void setSystemOverview(int customers, int policies, int claims, int assets) {
        lblCustomersOverview.setText(String.valueOf(customers));
        lblPoliciesOverview .setText(String.valueOf(policies));
        lblClaimsOverview   .setText(String.valueOf(claims));
        lblAssetsOverview   .setText(String.valueOf(assets));
    }

    public void setRecentClaims(List<String[]> data) {
        recentClaimsModel.setRowCount(0);
        if (data != null) {
            for (String[] row : data) {
                recentClaimsModel.addRow(row);
            }
        }
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setBackground(CLR_BG_CONTENT);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(24, 32, 24, 32));

        JLabel title = new JLabel("Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(CLR_TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Insurance Policy Claims System – overview of key metrics");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(CLR_TEXT_SECONDARY);
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(subtitle);
        return header;
    }

    private JScrollPane buildMainContent() {
        JPanel container = new JPanel();
        container.setBackground(CLR_BG_CONTENT);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBorder(new EmptyBorder(0, 32, 32, 32));

        container.add(buildSummaryCards());
        container.add(Box.createRigidArea(new Dimension(0, 24)));
        container.add(buildRecentClaimsTable());
        container.add(Box.createRigidArea(new Dimension(0, 24)));
        container.add(buildSystemOverview());
        container.add(Box.createRigidArea(new Dimension(0, 24)));
        container.add(buildStatusArea());

        JScrollPane scroll = new JScrollPane(container);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel buildSummaryCards() {
        JPanel cardsPanel = new JPanel(new GridLayout(1, 4, 16, 0));
        cardsPanel.setOpaque(false);
        
        JPanel custCard = createSummaryCard("Customers", "Total registered customers");
        lblCustomersCard = getCardValueLabel(custCard);
        cardsPanel.add(custCard);

        JPanel polCard = createSummaryCard("Policies", "Active insurance policies");
        lblPoliciesCard = getCardValueLabel(polCard);
        cardsPanel.add(polCard);

        JPanel claimCard = createSummaryCard("Claims", "Claims filed this year");
        lblClaimsCard = getCardValueLabel(claimCard);
        cardsPanel.add(claimCard);

        JPanel payCard = createSummaryCard("Payments", "Premiums received");
        lblPaymentsCard = getCardValueLabel(payCard);
        cardsPanel.add(payCard);

        return cardsPanel;
    }

    private JPanel createSummaryCard(String title, String caption) {
        JPanel card = new JPanel();
        card.setBackground(CLR_CARD_BG);
        card.setBorder(new CompoundBorder(
                new LineBorder(CLR_CARD_BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblTitle.setForeground(CLR_TEXT_SECONDARY);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblValue = new JLabel("—");
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblValue.setForeground(CLR_TEXT_PRIMARY);
        lblValue.setBorder(new EmptyBorder(8, 0, 8, 0));
        lblValue.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblCaption = new JLabel(caption);
        lblCaption.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblCaption.setForeground(CLR_TEXT_SECONDARY);
        lblCaption.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(lblTitle);
        card.add(lblValue);
        card.add(lblCaption);
        return card;
    }

    private JLabel getCardValueLabel(JPanel card) {
        return (JLabel) card.getComponent(1);
    }

    private JPanel buildRecentClaimsTable() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel tableTitle = new JLabel("Recent Claims");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        tableTitle.setForeground(CLR_TEXT_PRIMARY);
        tableTitle.setBorder(new EmptyBorder(0, 0, 8, 0));
        panel.add(tableTitle, BorderLayout.NORTH);

        recentClaimsModel = new DefaultTableModel(new Object[0][], CLAIMS_COLUMNS) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(recentClaimsModel);
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.setSelectionBackground(new Color(224, 242, 254));
        table.setSelectionForeground(CLR_TEXT_PRIMARY);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(200);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(80);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new CompoundBorder(
                new LineBorder(CLR_CARD_BORDER, 1, true),
                new EmptyBorder(8, 8, 8, 8)));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSystemOverview() {
        JPanel overview = new JPanel(new GridLayout(1, 4, 12, 0));
        overview.setOpaque(false);

        JPanel custPanel = createOverviewItem("Customers");
        lblCustomersOverview = getOverviewValueLabel(custPanel);
        overview.add(custPanel);

        JPanel polPanel = createOverviewItem("Policies");
        lblPoliciesOverview = getOverviewValueLabel(polPanel);
        overview.add(polPanel);

        JPanel claimPanel = createOverviewItem("Claims");
        lblClaimsOverview = getOverviewValueLabel(claimPanel);
        overview.add(claimPanel);

        JPanel assetPanel = createOverviewItem("Assets");
        lblAssetsOverview = getOverviewValueLabel(assetPanel);
        overview.add(assetPanel);

        return overview;
    }

    private JPanel createOverviewItem(String label) {
        JPanel item = new JPanel();
        item.setBackground(CLR_CARD_BG);
        item.setBorder(new CompoundBorder(
                new LineBorder(CLR_CARD_BORDER, 1, true),
                new EmptyBorder(12, 12, 12, 12)));
        item.setLayout(new BoxLayout(item, BoxLayout.Y_AXIS));

        JLabel lblValue = new JLabel("—");
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblValue.setForeground(CLR_ACCENT);
        lblValue.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblLabel.setForeground(CLR_TEXT_SECONDARY);
        lblLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblLabel.setBorder(new EmptyBorder(4, 0, 0, 0));

        item.add(lblValue);
        item.add(lblLabel);
        return item;
    }

    private JLabel getOverviewValueLabel(JPanel item) {
        return (JLabel) item.getComponent(0);
    }

    private JPanel buildStatusArea() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);

        JLabel badge = new JLabel("  System Ready") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(220, 252, 231));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        badge.setForeground(new Color(22, 163, 74));
        badge.setBorder(new EmptyBorder(4, 12, 4, 12));
        panel.add(badge);
        return panel;
    }
}