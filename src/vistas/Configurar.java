package vistas;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import models.ConfigurarModel;

public class Configurar extends JFrame implements java.awt.event.WindowListener {

    // ── Colores ───────────────────────────────────────────────────────────────
    private static final Color NAVY     = new Color(0x1A, 0x27, 0x44);
    private static final Color NAVY_MID = new Color(0x26, 0x38, 0x6B);
    private static final Color BG_PAGE  = new Color(0xEE, 0xF2, 0xF7);
    private static final Color BG_CARD  = Color.WHITE;
    private static final Color BG_FIELD = new Color(0xF6, 0xF8, 0xFA);
    private static final Color BORDER_C = new Color(0xDC, 0xE5, 0xEF);
    private static final Color TXT_PRI  = new Color(0x1E, 0x29, 0x3B);
    private static final Color TXT_MUT  = new Color(0x94, 0xA3, 0xB8);
    private static final int   ARC      = 14;

    // ── Componentes requeridos por ConfigurarModel ────────────────────────────
    private JComboBox<String> jbxSedes;
    private JTextField        txtip;
    private JButton           btnGuardar;
    private JButton           btnCancelar;
    private JLabel            lblicon;
    private JLabel            jLabel1;
    private JLabel            jLabel2;
    private JLabel            jLabel3;

    ConfigurarModel configurarModel;

    // ─────────────────────────────────────────────────────────────────────────
    public Configurar(Inicio i) {
        super("Configurar");
        buildUI();
        configurarModel = new ConfigurarModel(this, i);
        addWindowListener(this);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    private void buildUI() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);
        setSize(460, 310);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_PAGE);
        setContentPane(root);

        root.add(buildTopbar(), BorderLayout.NORTH);
        root.add(buildForm(),   BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
    }

    // ── Topbar ────────────────────────────────────────────────────────────────
    private JPanel buildTopbar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(NAVY);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 58));
        bar.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));

        lblicon = new JLabel();
        try {
            ImageIcon raw = new ImageIcon(getClass().getResource("/vistas/logop.png"));
            Image sc = raw.getImage().getScaledInstance(-1, 36, Image.SCALE_SMOOTH);
            lblicon.setIcon(new ImageIcon(sc));
        } catch (Exception ex) {
            lblicon.setText("CORSERPARK");
            lblicon.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblicon.setForeground(Color.WHITE);
        }
        bar.add(lblicon, BorderLayout.WEST);

        jLabel2 = new JLabel("Configurar");
        jLabel2.setFont(new Font("Segoe UI", Font.BOLD, 15));
        jLabel2.setForeground(new Color(0xA0, 0xAE, 0xC4));
        bar.add(jLabel2, BorderLayout.EAST);

        return bar;
    }

    // ── Formulario ────────────────────────────────────────────────────────────
    private JPanel buildForm() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));

        // Card blanca redondeada
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 14));
                g2.fillRoundRect(2, 3, getWidth()-3, getHeight()-2, ARC+2, ARC+2);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth()-2, getHeight()-3, ARC, ARC);
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BORDER_C);
                g2.drawRoundRect(0, 0, getWidth()-3, getHeight()-4, ARC, ARC);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;

        // ── Sede
        jLabel1 = makeLabel("SEDE");
        g.gridx=0; g.gridy=0; g.weightx=0; g.insets=new Insets(0,0,4,14);
        card.add(jLabel1, g);

        jbxSedes = new JComboBox<String>() {
            @Override protected void paintComponent(Graphics g2) {
                Graphics2D gx = (Graphics2D) g2.create();
                gx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gx.setColor(BG_FIELD);
                gx.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                gx.dispose();
                super.paintComponent(g2);
            }
        };
        jbxSedes.setFont(new Font("Segoe UI", Font.BOLD, 13));
        jbxSedes.setForeground(TXT_PRI);
        jbxSedes.setBackground(BG_FIELD);
        jbxSedes.setBorder(new CompoundBorder(
            new RoundedBorder(BORDER_C, 8),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        jbxSedes.setPreferredSize(new Dimension(250, 34));
        jbxSedes.addActionListener(e -> {});
        g.gridx=1; g.gridy=0; g.weightx=1; g.insets=new Insets(0,0,4,0);
        card.add(jbxSedes, g);

        // Separador
        g.gridx=0; g.gridy=1; g.gridwidth=2; g.insets=new Insets(10,0,10,0);
        card.add(new JSeparator(), g);

        // ── IP PORT
        g.gridwidth=1;
        jLabel3 = makeLabel("IP PORT");
        g.gridx=0; g.gridy=2; g.weightx=0; g.insets=new Insets(0,0,0,14);
        card.add(jLabel3, g);

        txtip = new JTextField() {
            @Override protected void paintComponent(Graphics g2) {
                Graphics2D gx = (Graphics2D) g2.create();
                gx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gx.setColor(BG_FIELD);
                gx.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                gx.dispose();
                super.paintComponent(g2);
            }
        };
        txtip.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtip.setForeground(TXT_PRI);
        txtip.setBackground(BG_FIELD);
        txtip.setOpaque(false);
        txtip.setPreferredSize(new Dimension(250, 34));
        txtip.setBorder(new CompoundBorder(
            new RoundedBorder(BORDER_C, 8),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        g.gridx=1; g.gridy=2; g.weightx=1; g.insets=new Insets(0,0,0,0);
        card.add(txtip, g);

        GridBagConstraints wc = new GridBagConstraints();
        wc.fill = GridBagConstraints.BOTH;
        wc.weightx = 1; wc.weighty = 1;
        wrapper.add(card, wc);
        return wrapper;
    }

    // ── Footer con botones ────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 20, 6, 20));

        btnCancelar = new JButton("Cancelar") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover()
                    ? new Color(0xDC, 0xE5, 0xEF) : BG_FIELD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(BORDER_C);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnCancelar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnCancelar.setForeground(TXT_PRI);
        btnCancelar.setContentAreaFilled(false);
        btnCancelar.setBorderPainted(false);
        btnCancelar.setFocusPainted(false);
        btnCancelar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCancelar.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        btnCancelar.addActionListener(e -> btnCancelarActionPerformed());
        footer.add(btnCancelar);

        btnGuardar = new JButton("Guardar") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? NAVY_MID : NAVY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnGuardar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setContentAreaFilled(false);
        btnGuardar.setBorderPainted(false);
        btnGuardar.setFocusPainted(false);
        btnGuardar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnGuardar.setBorder(BorderFactory.createEmptyBorder(8, 24, 8, 24));
        btnGuardar.addActionListener(e -> btnGuardarActionPerformed());
        footer.add(btnGuardar);

        return footer;
    }

    // ── Border redondeado ─────────────────────────────────────────────────────
    private static class RoundedBorder extends AbstractBorder {
        private final Color color; private final int arc;
        RoundedBorder(Color c, int a) { color=c; arc=a; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, w-1, h-1, arc, arc);
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(1,1,1,1); }
    }

    private JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        l.setForeground(TXT_MUT);
        return l;
    }

    // ── Acciones ──────────────────────────────────────────────────────────────
    private void btnCancelarActionPerformed() {
        setVisible(false);
        configurarModel.vistaPrincipal.setVisible(true);
        dispose();
    }

    private void btnGuardarActionPerformed() {
        try {
            configurarModel.guardar(jbxSedes.getSelectedItem() + "");
            setVisible(false);
            configurarModel.vistaPrincipal.setVisible(true);
            dispose();
        } catch (Exception e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(configurarModel.vistaPrincipal,
                "Error al almacenar los datos: " + e.getMessage());
        }
    }

    // ── API pública requerida por ConfigurarModel ─────────────────────────────
    public void jbxSedes_removeAllItems()         { jbxSedes.removeAllItems(); }
    public void jbxSedes_addItem(String item)     { jbxSedes.addItem(item); }
    public void jbxSedes_setSelectedItem(String s){ jbxSedes.setSelectedItem(s); }
    public void txtip_setText(String s)           { txtip.setText(s); }
    public String txtip_getText()                 { return txtip.getText(); }

    // ── WindowListener ────────────────────────────────────────────────────────
    @Override public void windowClosing(WindowEvent e) {
        setVisible(false);
        configurarModel.vistaPrincipal.setVisible(true);
        dispose();
    }
    @Override public void windowOpened(WindowEvent e)      { }
    @Override public void windowClosed(WindowEvent e)      { }
    @Override public void windowIconified(WindowEvent e)   { }
    @Override public void windowDeiconified(WindowEvent e) { }
    @Override public void windowActivated(WindowEvent e)   { }
    @Override public void windowDeactivated(WindowEvent e) { }
}
