import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

// UI
public class Tokenizer extends JFrame {

    // Color
    private static final Color RED_DARK      = new Color(180, 20,  20);
    private static final Color RED_MID       = new Color(220, 40,  40);
    private static final Color GRAY_DARK     = new Color(45,  45,  50);
    private static final Color GRAY_MID      = new Color(80,  85,  95);
    private static final Color GRAY_LIGHT    = new Color(180, 185, 195);
    private static final Color WHITE         = new Color(248, 250, 252);
    private static final Color BLUE_ACCENT   = new Color(50,  120, 200);
    private static final Color YELLOW_ACCENT = new Color(255, 210, 50);
    private static final Color SCREEN_BG     = new Color(20,  30,  20);
    private static final Color SCREEN_GREEN  = new Color(100, 200, 120);
    private static final Color TABLE_BG      = new Color(18,  22,  28);
    private static final Color ROW_ALT       = new Color(26,  30,  38);
    private static final Color ROW_HOVER     = new Color(40,  46,  58);

    // Token type colors
    private static final Color[] TOKEN_COLORS = {
        new Color(150, 150, 150),  // COMMENT          - light gray
        new Color(50,  180, 100),  // STRING_LITERAL  - green
        new Color(220, 100, 180),  // CHAR_LITERAL    - pink
        new Color(50,  200, 200),  // BOOLEAN_LITERAL - cyan
        new Color(160, 160, 160),  // NULL_LITERAL    - gray
        new Color(140, 80,  200),  // KEYWORD          - purple
        new Color(50,  150, 220),  // FLOAT_LITERAL    - blue
        new Color(220, 50,  50),   // INTEGER_LITERAL  - red
        new Color(200, 140, 50),   // IDENTIFIER       - orange
        new Color(200, 200, 50),   // OPERATOR         - yellow
        new Color(255, 140, 0),    // SEPARATOR        - dark orange
        new Color(100, 100, 100),  // UNKNOWN          - dark gray
    };

    private JTextArea   inputArea;
    private JPanel      tokenRowsPanel;
    private JScrollPane tokenScrollPane;
    private JLabel      statusLabel;
    private JLabel      tokenCountLabel;

    private List<Token> allTokens   = new ArrayList<>();
    private TokenType   activeFilter = null;

    private final Map<TokenType, JButton> legendButtons = new LinkedHashMap<>();
    private JButton allBtn;

    private JSplitPane splitPane;

    // Constructor
    public Tokenizer() {
        setTitle("Token Analyzer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 900);
        setMinimumSize(new Dimension(800, 700));
        setLocationRelativeTo(null);
        setResizable(true);

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(RED_DARK);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(GRAY_DARK);
                int[] xp = { getWidth() / 3 + 30, getWidth(), getWidth() };
                int[] yp = { 0, 0, getHeight() };
                g2.fillPolygon(xp, yp, 3);
                // Removed hinge bar and bolts for a cleaner background
            }
        };
        root.setOpaque(true);
        root.add(buildTopBody(), BorderLayout.NORTH);
        
        // Create split pane for adjustable input/token areas
        JPanel inputPanel = buildInputSection();
        JPanel tokenPanel = buildBottomBody();
        
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputPanel, tokenPanel) {
            @Override protected void paintComponent(Graphics g) {
            }
        };
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(0.5);
        splitPane.setDividerSize(8);
        splitPane.setContinuousLayout(true);
        splitPane.setOpaque(false);
        
        root.add(splitPane, BorderLayout.CENTER);

        setContentPane(root);
        setVisible(true);
    }

    // Separate input section for split pane
    private JPanel buildInputSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(8, 18, 8, 18));

        JLabel screenLbl = new JLabel("▸ INPUT TERMINAL");
        screenLbl.setFont(new Font("Monospaced", Font.BOLD, 11));
        screenLbl.setForeground(YELLOW_ACCENT);
        panel.add(screenLbl, BorderLayout.NORTH);

        JPanel screenSurround = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GRAY_MID);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(GRAY_LIGHT);
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 16, 16);
            }
        };
        screenSurround.setOpaque(false);
        screenSurround.setBorder(new EmptyBorder(7, 7, 7, 7));

        inputArea = new JTextArea(8, 50);
        inputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        inputArea.setBackground(SCREEN_BG);
        inputArea.setForeground(SCREEN_GREEN);
        inputArea.setCaretColor(YELLOW_ACCENT);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBorder(new EmptyBorder(6, 10, 6, 10));
        inputArea.setText("// Type or paste any expression here...\n42 + 3.14 * myVar != \"hello\" || true");

        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setBorder(BorderFactory.createLineBorder(new Color(40, 70, 40), 1));
        screenSurround.add(inputScroll);
        panel.add(screenSurround, BorderLayout.CENTER);

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        btnRow.setOpaque(false);
        JButton analyzeBtn = makePokeButton("⚡ ANALYZE", RED_MID,    WHITE);
        JButton clearBtn   = makePokeButton("✕  CLEAR",  GRAY_MID,   WHITE);
        JButton demoBtn    = makePokeButton("★  DEMO",   BLUE_ACCENT, WHITE);

        analyzeBtn.addActionListener(e -> tokenize());
        clearBtn.addActionListener(e -> clearAll());
        demoBtn.addActionListener(e -> {
            inputArea.setText(
                "int x = 42;\n" +
                "float pi = 3.14159;\n" +
                "String text = \"Hello World\";\n" +
                "char ch = 'Z';\n" +
                "boolean isValid = true;\n" +
                "Object obj = null;\n" +
                "if (x != 0 && isValid) { x++; }\n" +
                "@unknown_symbol"
            );
            tokenize();
        });
        inputArea.getInputMap().put(KeyStroke.getKeyStroke("ctrl ENTER"), "tok");
        inputArea.getActionMap().put("tok", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { tokenize(); }
        });

        JLabel hint = new JLabel("  Ctrl+Enter to analyze");
        hint.setFont(new Font("Monospaced", Font.ITALIC, 10));
        hint.setForeground(GRAY_LIGHT);
        btnRow.add(analyzeBtn); btnRow.add(clearBtn); btnRow.add(demoBtn); btnRow.add(hint);
        panel.add(btnRow, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildTopBody() {
        JPanel panel = new JPanel(new BorderLayout(10, 8));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(14, 18, 8, 18));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel(" Tokenizer");
        title.setFont(new Font("Monospaced", Font.BOLD, 20));
        title.setForeground(WHITE);
        JPanel leds = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 3));
        leds.setOpaque(false);
        leds.add(makeLed(BLUE_ACCENT, 20));
        leds.add(makeLed(new Color(255, 80, 80), 11));
        leds.add(makeLed(YELLOW_ACCENT, 11));
        leds.add(makeLed(SCREEN_GREEN, 11));
        header.add(title, BorderLayout.WEST);
        header.add(leds,  BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);
        return panel;
    }

    private JPanel buildBottomBody() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(6, 18, 14, 18));

        // Status bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setOpaque(false);
        statusLabel = new JLabel("▸ Awaiting input...");
        statusLabel.setFont(new Font("Monospaced", Font.BOLD, 11));
        statusLabel.setForeground(YELLOW_ACCENT);
        tokenCountLabel = new JLabel("Tokens: 0");
        tokenCountLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        tokenCountLabel.setForeground(YELLOW_ACCENT);
        statusBar.add(statusLabel,     BorderLayout.WEST);
        statusBar.add(tokenCountLabel, BorderLayout.EAST);
        panel.add(statusBar, BorderLayout.NORTH);

        // Center: legend + column header + token table
        JPanel centerPanel = new JPanel(new BorderLayout(0, 6));
        centerPanel.setOpaque(false);

        // Filter legend
        centerPanel.add(buildFilterLegend(), BorderLayout.NORTH);

        // Table area (column header + scrollable rows)
        JPanel tableArea = new JPanel(new BorderLayout(0, 0));
        tableArea.setOpaque(false);
        tableArea.add(buildColumnHeader(), BorderLayout.NORTH);

        tokenRowsPanel = new JPanel();
        tokenRowsPanel.setLayout(new BoxLayout(tokenRowsPanel, BoxLayout.Y_AXIS));
        tokenRowsPanel.setBackground(TABLE_BG);

        tokenScrollPane = new JScrollPane(tokenRowsPanel);
        tokenScrollPane.setBorder(BorderFactory.createLineBorder(GRAY_MID, 2));
        tokenScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tokenScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        tokenScrollPane.getVerticalScrollBar().setUnitIncrement(20);
        tokenScrollPane.setBackground(TABLE_BG);

        showPlaceholder();
        tableArea.add(tokenScrollPane, BorderLayout.CENTER);
        centerPanel.add(tableArea, BorderLayout.CENTER);
        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildColumnHeader() {
        JPanel hdr = new JPanel() {
            private final String[] cols = { "#", "TOKEN VALUE", "TYPE", "LINE", "POSITION" };
            private final double[] xs   = { 0.02, 0.08, 0.35, 0.55, 0.75 };

            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30, 35, 45));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(GRAY_MID);
                g2.setStroke(new BasicStroke(1));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);

                g2.setFont(new Font("Monospaced", Font.BOLD, 11));
                g2.setColor(GRAY_LIGHT);
                FontMetrics fm = g2.getFontMetrics();
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;

                for (int i = 0; i < cols.length; i++) {
                    int cx = (int)(getWidth() * xs[i]);
                    g2.drawString(cols[i], cx, y);
                    // vertical divider before each col except first
                    if (i > 0) {
                        g2.setColor(new Color(55, 62, 75));
                        g2.setStroke(new BasicStroke(1));
                        g2.drawLine(cx - 8, 4, cx - 8, getHeight() - 4);
                        g2.setColor(GRAY_LIGHT);
                    }
                }
            }

            @Override public Dimension getPreferredSize() { return new Dimension(0, 30); }
        };
        hdr.setOpaque(false);
        return hdr;
    }

    // Filter Legend
    private JPanel buildFilterLegend() {
        JPanel legend = new JPanel(new WrapLayout(FlowLayout.LEFT, 6, 4));
        legend.setOpaque(false);

        allBtn = makeLegendButton("ALL", GRAY_LIGHT, true);
        allBtn.addActionListener(e -> {
            activeFilter = null;
            refreshLegendStates();
            renderTokenRows();
        });
        legendButtons.put(null, allBtn);
        legend.add(allBtn);

        TokenType[] types = TokenType.values();
        for (int i = 0; i < types.length; i++) {
            TokenType t   = types[i];
            Color     col = TOKEN_COLORS[Math.min(i, TOKEN_COLORS.length - 1)];
            JButton   btn = makeLegendButton(t.name(), col, false);
            btn.addActionListener(e -> {
                activeFilter = t;
                refreshLegendStates();
                renderTokenRows();
            });
            legendButtons.put(t, btn);
            legend.add(btn);
        }
        return legend;
    }

    private void refreshLegendStates() {
        for (Map.Entry<TokenType, JButton> entry : legendButtons.entrySet()) {
            boolean active = Objects.equals(entry.getKey(), activeFilter);
            entry.getValue().putClientProperty("active", active);
            entry.getValue().repaint();
        }
    }

    // Legend
    private JButton makeLegendButton(String text, Color col, boolean startActive) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean active = Boolean.TRUE.equals(getClientProperty("active"));
                boolean hover  = getModel().isRollover();

                // Background fill
                if (active) {
                    g2.setColor(col);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                } else if (hover) {
                    g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 40));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                } else {
                    g2.setColor(new Color(60, 65, 75));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
                // Border
                g2.setColor(active
                    ? col.brighter()
                    : hover ? col : new Color(100, 105, 115));
                g2.setStroke(new BasicStroke(active ? 2f : 1f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);

                // Dot
                int dotSize = 8, dotY = (getHeight() - dotSize) / 2;
                g2.setColor(active ? Color.WHITE : (hover ? col : Color.LIGHT_GRAY));
                g2.fillOval(8, dotY, dotSize, dotSize);

                // Label
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.setColor(active ? Color.WHITE : (hover ? Color.WHITE : Color.LIGHT_GRAY));
                g2.drawString(getText(), 22, ty);
            }
        };
        btn.putClientProperty("active", startActive);
        btn.setFont(new Font("Monospaced", Font.BOLD, 11));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        int w = text.length() * 8 + 38;
        btn.setPreferredSize(new Dimension(Math.max(w, 62), 26));
        return btn;
    }

    // Tokenizer Logic
    private void tokenize() {
        String input = inputArea.getText().trim();
        if (input.isEmpty()) { statusLabel.setText("▸ No input detected!"); return; }
        try {
            allTokens = Lexer.lex(input);
            activeFilter = null;
            refreshLegendStates();
            renderTokenRows();
            statusLabel.setText("▸ Tokenization complete!");
            tokenCountLabel.setText("Tokens: " + allTokens.size());
        } catch (Exception e) {
            statusLabel.setText("▸ Error: " + e.getMessage());
            tokenCountLabel.setText("Tokens: 0");
            allTokens.clear();
            showPlaceholder();
        }
    }

    private void renderTokenRows() {
        tokenRowsPanel.removeAll();

        List<Token> visible = new ArrayList<>();
        for (Token t : allTokens) {
            if (activeFilter == null || t.type == activeFilter) visible.add(t);
        }

        if (visible.isEmpty()) {
            JPanel ph = makeCenteredMsg(
                activeFilter != null
                    ? "⚠  No " + activeFilter.name() + " tokens in this input."
                    : "⚠  No tokens to display.",
                GRAY_LIGHT);
            tokenRowsPanel.add(ph);
        } else {
            for (int i = 0; i < visible.size(); i++)
                tokenRowsPanel.add(makeTokenRow(visible.get(i), i % 2 == 0));
        }

        tokenRowsPanel.revalidate();
        tokenRowsPanel.repaint();
        SwingUtilities.invokeLater(() ->
            tokenScrollPane.getVerticalScrollBar().setValue(0));

        tokenCountLabel.setText(activeFilter != null
            ? "Showing: " + visible.size() + " / " + allTokens.size()
            : "Tokens: " + allTokens.size());
    }

    // Token Row
    private JPanel makeTokenRow(Token token, boolean even) {
        Color baseCol  = getTokenColor(token.type);
        Color bgNormal = even ? TABLE_BG : ROW_ALT;

        double[] xs = { 0.02, 0.08, 0.35, 0.55, 0.75 };
        JPanel row = new JPanel() {
            boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                    @Override public void mouseExited (MouseEvent e) { hovered = false; repaint(); }
                });
            }

            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Row bg
                g2.setColor(hovered ? ROW_HOVER : bgNormal);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Left accent stripe
                g2.setColor(baseCol);
                g2.fillRect(0, 0, 4, getHeight());

                // Bottom divider
                g2.setColor(new Color(35, 40, 52));
                g2.setStroke(new BasicStroke(1));
                g2.drawLine(4, getHeight() - 1, getWidth(), getHeight() - 1);

                int H = getHeight();

                // Col 0: index
                Font fSml = new Font("Monospaced", Font.PLAIN, 11);
                g2.setFont(fSml);
                FontMetrics fmS = g2.getFontMetrics();
                g2.setColor(new Color(100, 110, 130));
                String idx = String.format("%03d", token.index + 1);
                int yS = (H + fmS.getAscent() - fmS.getDescent()) / 2;
                g2.drawString(idx, (int)(getWidth() * xs[0]) + 6, yS);

                // Col 1: value
                Font fBig = new Font("Monospaced", Font.BOLD, 14);
                g2.setFont(fBig);
                FontMetrics fmB = g2.getFontMetrics();
                g2.setColor(WHITE);
                String val = token.value.length() > 30
                    ? token.value.substring(0, 28) + "…" : token.value;
                int yB = (H + fmB.getAscent() - fmB.getDescent()) / 2;
                g2.drawString(val, (int)(getWidth() * xs[1]), yB);

                // Col 2: type badge pill
                String typeName = token.type.name();
                Font fBadge = new Font("Monospaced", Font.BOLD, 11);
                g2.setFont(fBadge);
                FontMetrics fmBadge = g2.getFontMetrics();
                int badgeW = fmBadge.stringWidth(typeName) + 22;
                int badgeH = 22;
                int bx     = (int)(getWidth() * xs[2]);
                int by     = (H - badgeH) / 2;

                g2.setColor(new Color(baseCol.getRed(), baseCol.getGreen(), baseCol.getBlue(), 40));
                g2.fillRoundRect(bx, by, badgeW, badgeH, badgeH, badgeH);
                g2.setColor(new Color(baseCol.getRed(), baseCol.getGreen(), baseCol.getBlue(), 180));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(bx, by, badgeW, badgeH, badgeH, badgeH);
                g2.setColor(baseCol);
                int ty = by + (badgeH + fmBadge.getAscent() - fmBadge.getDescent()) / 2;
                g2.drawString(typeName, bx + 11, ty);

                // Col 3: line
                g2.setFont(fSml);
                g2.setColor(new Color(120, 130, 150));
                String lineStr = "line " + token.line;
                int lx = (int)(getWidth() * xs[3]);
                g2.drawString(lineStr, lx, yS);

                // Col 4: position
                g2.setColor(new Color(120, 130, 150));
                String pos = "col " + token.position;
                int px = (int)(getWidth() * xs[4]);
                g2.drawString(pos, px, yS);
            }

            @Override public Dimension getPreferredSize() {
                return new Dimension(super.getPreferredSize().width, 42);
            }
            @Override public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, 42);
            }
            @Override public Dimension getMinimumSize() {
                return new Dimension(0, 42);
            }
        };

        row.setOpaque(false);
        row.setLayout(null);
        row.setToolTipText(
            "<html><b>Value:</b> " + escHtml(token.value) +
            "<br><b>Type:</b> "    + token.type.name()   +
            "<br><b>Line:</b> "     + token.line          +
            "<br><b>Position:</b> col " + token.position  + "</html>"
        );
        return row;
    }

    // Placeholder
    private void showPlaceholder() {
        tokenRowsPanel.removeAll();
        tokenRowsPanel.add(makeCenteredMsg(
            "⚡  Press ANALYZE to tokenize your input", GRAY_LIGHT));
        tokenRowsPanel.revalidate();
        tokenRowsPanel.repaint();
    }

    private JPanel makeCenteredMsg(String msg, Color col) {
        JPanel ph = new JPanel(new GridBagLayout());
        ph.setBackground(TABLE_BG);
        ph.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        ph.setPreferredSize(new Dimension(0, 100));
        JLabel lbl = new JLabel(msg);
        lbl.setFont(new Font("Monospaced", Font.ITALIC, 13));
        lbl.setForeground(col);
        ph.add(lbl);
        return ph;
    }

    private void clearAll() {
        inputArea.setText("");
        allTokens.clear();
        activeFilter = null;
        refreshLegendStates();
        showPlaceholder();
        statusLabel.setText("▸ Awaiting input...");
        tokenCountLabel.setText("Tokens: 0");
    }

    // UI Helpers
    private Color getTokenColor(TokenType type) {
        return switch (type) {
            case COMMENT           -> TOKEN_COLORS[0];
            case STRING_LITERAL    -> TOKEN_COLORS[1];
            case CHAR_LITERAL      -> TOKEN_COLORS[2];
            case BOOLEAN_LITERAL   -> TOKEN_COLORS[3];
            case NULL_LITERAL      -> TOKEN_COLORS[4];
            case KEYWORD           -> TOKEN_COLORS[5];
            case FLOAT_LITERAL     -> TOKEN_COLORS[6];
            case INTEGER_LITERAL   -> TOKEN_COLORS[7];
            case IDENTIFIER        -> TOKEN_COLORS[8];
            case OPERATOR          -> TOKEN_COLORS[9];
            case SEPARATOR         -> TOKEN_COLORS[10];
            case UNKNOWN           -> TOKEN_COLORS[11];
        };
    }

    private JLabel makeLed(Color color, int size) {
        return new JLabel() {
            { setPreferredSize(new Dimension(size, size)); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color.darker());
                g2.fillOval(0, 0, size, size);
                g2.setColor(color);
                g2.fillOval(1, 1, size - 3, size - 3);
                g2.setColor(color.brighter());
                g2.fillOval(size / 4, size / 4, size / 4, size / 4);
            }
        };
    }

    private JButton makePokeButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = getModel().isPressed()  ? bg.darker().darker() :
                          getModel().isRollover() ? bg.brighter()        : bg;
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(c.brighter());
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Monospaced", Font.BOLD, 12));
        btn.setForeground(fg);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(130, 32));
        return btn;
    }

    private String escHtml(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    // WrapLayout
    static class WrapLayout extends FlowLayout {
        WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }
        @Override public Dimension preferredLayoutSize(Container t) { return layoutSize(t, true);  }
        @Override public Dimension minimumLayoutSize(Container t)   { return layoutSize(t, false); }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int tw = target.getSize().width;
                if (tw == 0) tw = Integer.MAX_VALUE;
                Insets ins = target.getInsets();
                int maxW = tw - (ins.left + ins.right + getHgap() * 2);
                int height = 0, rowW = 0, rowH = 0;
                for (int i = 0; i < target.getComponentCount(); i++) {
                    Component m = target.getComponent(i);
                    if (!m.isVisible()) continue;
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if (rowW + d.width > maxW && rowW > 0) {
                        height += rowH + getVgap(); rowW = 0; rowH = 0;
                    }
                    if (rowW > 0) rowW += getHgap();
                    rowW += d.width; rowH = Math.max(rowH, d.height);
                }
                height += rowH + ins.top + ins.bottom + getVgap() * 2;
                return new Dimension(tw, height);
            }
        }
    }

    // Main
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(Tokenizer::new);
    }
}