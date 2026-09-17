package frontend;

import service.BackendService;
import dao.AssetDAO;
import dao.ClaimDAO;
import dao.PaymentDAO;
import dao.PolicyDAO;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainFrame extends JFrame {

    private BackendService backendService;
    
    // DAOs for independent panel wiring
    private AssetDAO assetDAO;
    private PolicyDAO policyDAO;
    private ClaimDAO claimDAO;
    private PaymentDAO paymentDAO;

    // -- Palette -----------------------------------------------------------
    private static final Color CLR_SIDEBAR_BG     = new Color(15,  23,  42);
    private static final Color CLR_SIDEBAR_ACCENT = new Color(30,  41,  59);
    private static final Color CLR_BRAND          = new Color(56, 189, 248);
    private static final Color CLR_BRAND_DIM      = new Color(14, 116, 144);
    private static final Color CLR_SIDEBAR_FG     = new Color(148, 163, 184);
    private static final Color CLR_SIDEBAR_FG_ACT = Color.WHITE;
    private static final Color CLR_BTN_HOVER      = new Color(51,  65,  85);
    private static final Color CLR_BTN_ACTIVE     = new Color(56, 189, 248, 40);
    private static final Color CLR_HEADER_BG      = new Color(248, 250, 252);
    private static final Color CLR_HEADER_BORDER  = new Color(226, 232, 240);
    private static final Color CLR_CONTENT_BG     = new Color(241, 245, 249);
    private static final Color CLR_BADGE_BG       = new Color(220, 252, 231);
    private static final Color CLR_BADGE_FG       = new Color(22,  163,  74);

    // -- Dimensions --------------------------------------------------------
    private static final int SIDEBAR_WIDTH = 230;
    private static final int HEADER_HEIGHT = 60;

    // -- Navigation items --------------------------------------------------
    private static final String[][] NAV_ITEMS = {
        { "dashboard", "   Dashboard" },
        { "customers", "   Customers" },
        { "policies",  "   Policies"  },
        { "claims",    "   Claims"    },
        { "payments",  "   Payments"  },
        { "assets",    "   Assets"    },
        { "sql",       "   SQL Query" }
    };

    // -- State -------------------------------------------------------------
    private CardLayout cardLayout;
    private JPanel     contentPanel;
    private JLabel     headerTitleLabel;
    private String     activeKey = "dashboard";
    private JButton[]  navButtons;

    // -- Panels ------------------------------------------------------------
    private AssetPanel assetPanel;
    private PolicyPanel policyPanel;
    private ClaimPanel claimPanel;
    private PaymentPanel paymentPanel;

    public MainFrame() {
        this.backendService = new BackendService();
        this.assetDAO = new AssetDAO();
        this.policyDAO = new PolicyDAO();
        this.claimDAO = new ClaimDAO();
        this.paymentDAO = new PaymentDAO();

        applyLookAndFeel();
        initFrame();
        buildUI(); 
        showCard("dashboard");
        setVisible(true);
    }

    private void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }
    }

    private void initFrame() {
        setTitle("InsureTrack - Insurance Policy Claims System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 650));
        setSize(new Dimension(1280, 760));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
    }

    private void buildUI() {
        add(buildHeader(),  BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);
        add(buildContent(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(CLR_HEADER_BORDER);
                g.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        header.setBackground(CLR_HEADER_BG);
        header.setPreferredSize(new Dimension(0, HEADER_HEIGHT));
        header.setBorder(new EmptyBorder(0, 20, 0, 20));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        JLabel appIcon = new JLabel("[Shield]");
        appIcon.setFont(new Font("Segoe UI", Font.BOLD, 16));
        appIcon.setForeground(CLR_BRAND);
        left.add(appIcon);

        headerTitleLabel = new JLabel("Dashboard");
        headerTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerTitleLabel.setForeground(new Color(15, 23, 42));
        left.add(headerTitleLabel);
        header.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        JLabel clockLabel = new JLabel();
        clockLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        clockLabel.setForeground(new Color(100, 116, 139));
        updateClock(clockLabel);
        Timer clockTimer = new Timer(30_000, e -> updateClock(clockLabel));
        clockTimer.start();
        right.add(clockLabel);

        right.add(makeVerticalSeparator());
        right.add(makeStatusBadge());
        right.add(makeVerticalSeparator());
        right.add(makeUserArea());
        header.add(right, BorderLayout.EAST);

        return header;
    }

    private void updateClock(JLabel label) {
        label.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm")));
    }

    private JSeparator makeVerticalSeparator() {
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 24));
        sep.setForeground(CLR_HEADER_BORDER);
        return sep;
    }

    private JLabel makeStatusBadge() {
        JLabel badge = new JLabel("  System Ready") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CLR_BADGE_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badge.setForeground(CLR_BADGE_FG);
        badge.setOpaque(false);
        badge.setBorder(new EmptyBorder(4, 10, 4, 10));
        return badge;
    }

    private JPanel makeUserArea() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        panel.setOpaque(false);

        JLabel avatar = new JLabel("A") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CLR_BRAND_DIM);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatar.setHorizontalAlignment(SwingConstants.CENTER);
        avatar.setVerticalAlignment(SwingConstants.CENTER);
        avatar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        avatar.setForeground(Color.WHITE);
        avatar.setPreferredSize(new Dimension(32, 32));
        avatar.setOpaque(false);
        panel.add(avatar);

        JPanel info = new JPanel(new GridLayout(2, 1, 0, 0));
        info.setOpaque(false);
        JLabel nameLabel = new JLabel("Admin User");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        nameLabel.setForeground(new Color(15, 23, 42));
        JLabel roleLabel = new JLabel("Administrator");
        roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        roleLabel.setForeground(new Color(100, 116, 139));
        info.add(nameLabel);
        info.add(roleLabel);
        panel.add(info);

        return panel;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(CLR_SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));
        sidebar.add(buildBrandPanel(),    BorderLayout.NORTH);
        sidebar.add(buildNavPanel(),      BorderLayout.CENTER);
        sidebar.add(buildSidebarFooter(), BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel buildBrandPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(CLR_SIDEBAR_BG);
        panel.setBorder(new EmptyBorder(24, 20, 20, 20));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setOpaque(false);

        JLabel iconLabel = new JLabel("[IT]");
        iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        iconLabel.setForeground(CLR_BRAND);
        brand.add(iconLabel);

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setOpaque(false);

        JLabel appName = new JLabel("InsureTrack");
        appName.setFont(new Font("Segoe UI", Font.BOLD, 17));
        appName.setForeground(Color.WHITE);

        JLabel appSub = new JLabel("Policy Claims System");
        appSub.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        appSub.setForeground(CLR_SIDEBAR_FG);

        text.add(appName);
        text.add(appSub);
        brand.add(text);
        panel.add(brand, BorderLayout.CENTER);

        JPanel divider = new JPanel();
        divider.setBackground(CLR_SIDEBAR_ACCENT);
        divider.setPreferredSize(new Dimension(0, 1));
        panel.add(divider, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildNavPanel() {
        JPanel nav = new JPanel();
        nav.setBackground(CLR_SIDEBAR_BG);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBorder(new EmptyBorder(14, 0, 14, 0));

        navButtons = new JButton[NAV_ITEMS.length];
        for (int i = 0; i < NAV_ITEMS.length; i++) {
            JButton btn = makeNavButton(NAV_ITEMS[i][1], NAV_ITEMS[i][0]);
            navButtons[i] = btn;
            nav.add(btn);
            nav.add(Box.createRigidArea(new Dimension(0, 2)));
        }
        return nav;
    }

    private JButton makeNavButton(String label, String cardKey) {
        final boolean[] hovered = { false };

        JButton btn = new JButton(label) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean active = cardKey.equals(activeKey);
                if (active) {
                    g2.setColor(CLR_BRAND);
                    g2.fillRoundRect(0, 4, 4, getHeight() - 8, 4, 4);
                    g2.setColor(CLR_BTN_ACTIVE);
                    g2.fillRoundRect(8, 2, getWidth() - 16, getHeight() - 4, 8, 8);
                } else if (hovered[0]) {
                    g2.setColor(CLR_BTN_HOVER);
                    g2.fillRoundRect(8, 2, getWidth() - 16, getHeight() - 4, 8, 8);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setForeground(CLR_SIDEBAR_FG);
        btn.setBackground(CLR_SIDEBAR_BG);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(10, 22, 10, 14));
        btn.setMaximumSize(new Dimension(SIDEBAR_WIDTH, 44));
        btn.setMinimumSize(new Dimension(SIDEBAR_WIDTH, 44));
        btn.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 44));

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered[0] = true;  btn.repaint(); }
            @Override public void mouseExited (MouseEvent e) { hovered[0] = false; btn.repaint(); }
        });
        btn.addActionListener(e -> showCard(cardKey));
        return btn;
    }

    private JPanel buildSidebarFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(CLR_SIDEBAR_BG);
        footer.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, CLR_SIDEBAR_ACCENT),
                new EmptyBorder(14, 20, 18, 20)));

        JLabel ver = new JLabel("v1.0.0  -  DBMS Project");
        ver.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        ver.setForeground(new Color(71, 85, 105));
        footer.add(ver, BorderLayout.CENTER);
        return footer;
    }

    private JPanel buildContent() {
        cardLayout   = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(CLR_CONTENT_BG);

        // 1. Pre-wired panels
        contentPanel.add(new DashboardPanel(this.backendService), "dashboard");
        contentPanel.add(new CustomerPanel(this.backendService), "customers");
        contentPanel.add(new SQLQueryPanel(this.backendService), "sql");

        // 2. Setup and wire Policy Panel
        policyPanel = new PolicyPanel();
        setupPolicyBridge();
        contentPanel.add(policyPanel, "policies");

        // 3. Setup and wire Claim Panel
        claimPanel = new ClaimPanel();
        setupClaimBridge();
        contentPanel.add(claimPanel, "claims");

        // 4. Setup and wire Payment Panel
        paymentPanel = new PaymentPanel();
        setupPaymentBridge();
        contentPanel.add(paymentPanel, "payments");

        // 5. Setup and wire Asset Panel
        assetPanel = new AssetPanel();
        setupAssetBridge();
        contentPanel.add(assetPanel, "assets");

        return contentPanel;
    }

    private void setupAssetBridge() {
        assetPanel.setDataHandler(new AssetPanel.AssetDataHandler() {
            @Override public java.util.List<String[]> loadAllAssets() { return assetDAO.displayAssets(); }
            @Override public boolean addAsset(String id, String desc, String date, double val) { return assetDAO.addAsset(id, desc, date, val); }
            @Override public boolean updateAsset(String id, String desc, String date, double val) { return assetDAO.updateAsset(id, desc, date, val); }
            @Override public boolean deleteAsset(String id) { return assetDAO.deleteAsset(id); }
        });
        assetPanel.loadAssets(); // Trigger initial load
    }

    private void setupPolicyBridge() {
        policyPanel.setDataHandler(new PolicyPanel.PolicyDataHandler() {
            @Override public java.util.List<String[]> loadAllPolicies() { return policyDAO.displayPolicies(); }
            @Override public String[] findPolicy(String pNo) { return policyDAO.findPolicy(pNo); }
            @Override public boolean addPolicy(String pNo, String start, String expiry, String audit, double premium) { return policyDAO.addPolicy(pNo, start, expiry, audit, premium); }
            @Override public boolean updatePolicy(String pNo, String start, String expiry, String audit, double premium) { return policyDAO.updatePolicy(pNo, start, expiry, audit, premium); }
            @Override public boolean deletePolicy(String pNo) { return policyDAO.deletePolicy(pNo); }
        });
        policyPanel.loadPolicies(); // Trigger initial load
    }

    private void setupClaimBridge() {
        claimPanel.setDataHandler(new ClaimPanel.ClaimDataHandler() {
            @Override public java.util.List<String[]> loadAllClaims() { return claimDAO.displayClaims(); }
            @Override public String[] findClaim(String id) { return null; }
            @Override public double getClaimAmount(String id) { return claimDAO.getClaimAmount(id); }
            @Override public boolean addClaim(String id, String pNo, String date, String desc, String status) { return claimDAO.registerClaim(id, pNo, date, desc, status); }
            @Override public boolean updateClaim(String id, String pNo, String date, String desc, double amt, String status) { return claimDAO.updateClaim(id, pNo, date, desc, amt, status); }
            @Override public boolean deleteClaim(String id) { return claimDAO.deleteClaim(id); }
        });
        claimPanel.loadClaims(); // Trigger initial load
    }

    private void setupPaymentBridge() {
        paymentPanel.setDataHandler(new PaymentPanel.PaymentDataHandler() {
            @Override public java.util.List<String[]> loadAllPayments() { return paymentDAO.displayPayments(); }
            @Override public boolean addPayment(String claimId, String payId, String date, String method) { return paymentDAO.makePayment(claimId, payId, date, method); }
            @Override public boolean updatePayment(String claimId, String payId, String date, String method, double amt) { return paymentDAO.updatePayment(claimId, payId, date, method, amt); }
            @Override public boolean deletePayment(String claimId, String payId) { return paymentDAO.deletePayment(claimId, payId); }
        });
        paymentPanel.loadPayments(); // Trigger initial load
    }

    private void showCard(String key) {
        activeKey = key;
        cardLayout.show(contentPanel, key);
        updateHeaderTitle(key);
        refreshNavButtons();

        // Refresh data whenever tab is clicked
        switch (key) {
            case "policies" -> policyPanel.loadPolicies();
            case "claims"   -> claimPanel.loadClaims();
            case "payments" -> paymentPanel.loadPayments();
            case "assets"   -> assetPanel.loadAssets();
        }
    }

    private void updateHeaderTitle(String key) {
        String title = switch (key) {
            case "dashboard" -> "Dashboard";
            case "customers" -> "Customers";
            case "policies"  -> "Policies";
            case "claims"    -> "Claims";
            case "payments"  -> "Payments";
            case "assets"    -> "Assets";
            case "sql"       -> "SQL Query";
            default          -> key;
        };
        headerTitleLabel.setText(title);
    }

    private void refreshNavButtons() {
        if (navButtons == null) return;
        for (int i = 0; i < navButtons.length; i++) {
            boolean active = NAV_ITEMS[i][0].equals(activeKey);
            navButtons[i].setForeground(active ? CLR_SIDEBAR_FG_ACT : CLR_SIDEBAR_FG);
            navButtons[i].setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 13));
            navButtons[i].repaint();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainFrame::new);
    }
}