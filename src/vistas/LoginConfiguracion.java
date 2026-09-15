package vistas;

import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.border.*;

public class LoginConfiguracion extends JFrame implements WindowListener {

    private static final Color NAVY     = new Color(0x1A, 0x27, 0x44);
    private static final Color NAVY_MID = new Color(0x26, 0x38, 0x6B);
    private static final Color BG_PAGE  = new Color(0xEE, 0xF2, 0xF7);
    private static final Color BG_CARD  = Color.WHITE;
    private static final Color BG_FIELD = new Color(0xF6, 0xF8, 0xFA);
    private static final Color BORDER_C = new Color(0xDC, 0xE5, 0xEF);
    private static final Color TXT_PRI  = new Color(0x1E, 0x29, 0x3B);
    private static final Color TXT_MUT  = new Color(0x94, 0xA3, 0xB8);
    private static final int   ARC      = 14;

    private JTextField     txtUser;
    private JPasswordField txtPass;
    private JButton        btnIngresar;

    private final Inicio inicio;

    public LoginConfiguracion(Inicio inicio) {
        super("Acceso a Configuración");
        this.inicio = inicio;
        buildUI();
        addWindowListener(this);
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);
        setVisible(true);
    }

    private void buildUI() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);
        setSize(420, 310);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_PAGE);
        setContentPane(root);

        root.add(buildTopbar(), BorderLayout.NORTH);
        root.add(buildForm(),   BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
    }

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

        JLabel logo = new JLabel();
        try {
            ImageIcon raw = new ImageIcon(getClass().getResource("/vistas/logop.png"));
            Image sc = raw.getImage().getScaledInstance(-1, 36, Image.SCALE_SMOOTH);
            logo.setIcon(new ImageIcon(sc));
        } catch (Exception ex) {
            logo.setText("CORSERPARK");
            logo.setFont(new Font("Segoe UI", Font.BOLD, 13));
            logo.setForeground(Color.WHITE);
        }
        bar.add(logo, BorderLayout.WEST);

        JLabel titulo = new JLabel("Configuración");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titulo.setForeground(Color.WHITE);
        bar.add(titulo, BorderLayout.EAST);

        return bar;
    }

    private JPanel buildForm() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));

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
        card.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;

        // Usuario
        g.gridx=0; g.gridy=0; g.weightx=0; g.insets=new Insets(0,0,4,14);
        card.add(makeLabel("USUARIO"), g);

        txtUser = new JTextField() {
            @Override protected void paintComponent(Graphics g2) {
                Graphics2D gx = (Graphics2D) g2.create();
                gx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gx.setColor(BG_FIELD);
                gx.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                gx.dispose();
                super.paintComponent(g2);
            }
        };
        txtUser.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtUser.setForeground(TXT_PRI);
        txtUser.setBackground(BG_FIELD);
        txtUser.setOpaque(false);
        txtUser.setPreferredSize(new Dimension(240, 34));
        txtUser.setBorder(new CompoundBorder(
            new RoundedBorder(BORDER_C, 8),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        g.gridx=1; g.gridy=0; g.weightx=1; g.insets=new Insets(0,0,4,0);
        card.add(txtUser, g);

        // Separador
        g.gridx=0; g.gridy=1; g.gridwidth=2; g.insets=new Insets(10,0,10,0);
        card.add(new JSeparator(), g);

        // Contraseña
        g.gridwidth=1;
        g.gridx=0; g.gridy=2; g.weightx=0; g.insets=new Insets(0,0,0,14);
        card.add(makeLabel("CONTRASEÑA"), g);

        txtPass = new JPasswordField() {
            @Override protected void paintComponent(Graphics g2) {
                Graphics2D gx = (Graphics2D) g2.create();
                gx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gx.setColor(BG_FIELD);
                gx.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                gx.dispose();
                super.paintComponent(g2);
            }
        };
        txtPass.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtPass.setForeground(TXT_PRI);
        txtPass.setBackground(BG_FIELD);
        txtPass.setOpaque(false);
        txtPass.setPreferredSize(new Dimension(240, 34));
        txtPass.setBorder(new CompoundBorder(
            new RoundedBorder(BORDER_C, 8),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        txtPass.addActionListener(e -> ingresar());
        g.gridx=1; g.gridy=2; g.weightx=1; g.insets=new Insets(0,0,0,0);
        card.add(txtPass, g);

        GridBagConstraints wc = new GridBagConstraints();
        wc.fill = GridBagConstraints.BOTH;
        wc.weightx = 1; wc.weighty = 1;
        wrapper.add(card, wc);
        return wrapper;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 20, 6, 20));

        JLabel lblSolutic = new JLabel("@ Solutic 3E | Transformando Ideas");
        lblSolutic.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblSolutic.setForeground(TXT_PRI);
        footer.add(lblSolutic, BorderLayout.WEST);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btns.setOpaque(false);

        btnIngresar = new JButton("Ingresar") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? NAVY_MID : NAVY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnIngresar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnIngresar.setForeground(Color.WHITE);
        btnIngresar.setContentAreaFilled(false);
        btnIngresar.setBorderPainted(false);
        btnIngresar.setFocusPainted(false);
        btnIngresar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnIngresar.setBorder(BorderFactory.createEmptyBorder(8, 28, 8, 28));
        btnIngresar.addActionListener(e -> ingresar());
        btns.add(btnIngresar);
        footer.add(btns, BorderLayout.EAST);

        return footer;
    }

    private void ingresar() {
        String usuario = txtUser.getText().trim();
        String clave   = new String(txtPass.getPassword());

        if (usuario.isEmpty() || clave.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese usuario y contraseña.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnIngresar.setEnabled(false);
        btnIngresar.setText("Verificando...");

        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    database.ConexionDb d = new database.ConexionDb();
                    d.Conectar();
                    HashMap<String, String> u = d.selectOne("SELECT valor FROM config WHERE dato='login_usuario'");
                    HashMap<String, String> c = d.selectOne("SELECT valor FROM config WHERE dato='login_clave'");
                    d.close();

                    if (u == null || c == null) return false;

                    String usuarioGuardado = u.get("valor");
                    String claveGuardada   = c.get("valor");

                    return usuarioGuardado.equals(usuario) && claveGuardada.equals(sha1(clave));
                } catch (Exception ignored) { }
                return false;
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        dispose();
                        Configurar c = new Configurar(inicio);
                        c.setIconImage(new ImageIcon(Inicio.class.getResource("icon_frame.png")).getImage());
                        inicio.setVisible(false);
                        c.setVisible(true);
                    } else {
                        JOptionPane.showMessageDialog(LoginConfiguracion.this,
                            "Usuario o contraseña incorrectos.", "Acceso denegado", JOptionPane.ERROR_MESSAGE);
                        txtPass.setText("");
                        txtPass.requestFocus();
                    }
                } catch (Exception ignored) { }
                btnIngresar.setEnabled(true);
                btnIngresar.setText("Ingresar");
            }
        };
        worker.execute();
    }

    private String sha1(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
            byte[] b = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

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
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(TXT_PRI);
        return l;
    }

    @Override public void windowClosing(WindowEvent e) {
        inicio.setVisible(true);
        dispose();
    }
    @Override public void windowOpened(WindowEvent e)      { }
    @Override public void windowClosed(WindowEvent e)      { }
    @Override public void windowIconified(WindowEvent e)   { }
    @Override public void windowDeiconified(WindowEvent e) { }
    @Override public void windowActivated(WindowEvent e)   { }
    @Override public void windowDeactivated(WindowEvent e) { }
}
