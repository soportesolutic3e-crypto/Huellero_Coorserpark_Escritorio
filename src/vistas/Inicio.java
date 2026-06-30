package vistas;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import devices.VerificarDactilar;
import javax.swing.*;
import javax.swing.border.*;

public class Inicio extends JFrame {

    // ── Campos estáticos y públicos requeridos externamente ───────────────────
    public static String wformaregistropersonaini = "";
    Ingreso ingreso = null;
    IngresoTarjeta ingresoTarjeta = null;
    String ultimaS = "";
    public String mensajeconexion = "";
    public String detallesede = "";
    public Integer wresultadoconexion = 1;
    public String wqueaccion;
    private boolean isProcessingClick = false;

    // ── Componentes requeridos por código externo (Sincronizar, etc.) ─────────
    private JLabel versincronizacion;
    private JPanel jPanel1;
    private JPanel jPanel2;
    private JLabel lblIconIngreso;
    private JLabel lblIconIngreso1;
    private JLabel lblIconRegistro;
    private JButton btnConfigurar;
    private JLabel jLabel1;
    private JLabel jLabel3;
    private JLabel lblLogo;
    private JLabel lblIndentificaion;
    private JLabel lblRegistro;

    // ── UI interna ────────────────────────────────────────────────────────────
    private JLabel    lbldiahora;
    private JLabel    lblFecha;
    private JLabel    lblSede;
    private JPanel    bodyPanel;
    private CardLayout bodyLayout;
    private JProgressBar progressBar;
    private Timer     timerReloj;

    // ── Paleta ────────────────────────────────────────────────────────────────
    private static final Color NAVY      = new Color(0x1A, 0x27, 0x44);
    private static final Color NAVY_DARK = new Color(0x0E, 0x17, 0x2D);
    private static final Color NAVY_MID  = new Color(0x26, 0x38, 0x6B);
    private static final Color BG_PAGE   = new Color(0xEE, 0xF2, 0xF7);
    private static final Color BG_CARD   = Color.WHITE;
    private static final Color BORDER_C  = new Color(0xDC, 0xE5, 0xEF);
    private static final Color TXT_MUT   = new Color(0x94, 0xA3, 0xB8);
    private static final int   ARC       = 16;

    // ─────────────────────────────────────────────────────────────────────────
    public Inicio() {
        super("Sistema de control de ingreso");
        buildUI();
        iniciarReloj();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                VerificarDactilar.stopMonitor();
            }
        });
        setLocationRelativeTo(null);
    }

    // ── Construcción UI ───────────────────────────────────────────────────────
    private void buildUI() {
        setResizable(false);
        setSize(760, 500);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_PAGE);
        setContentPane(root);

        root.add(buildTopbar(),    BorderLayout.NORTH);
        root.add(buildBody(),      BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);
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
        bar.setPreferredSize(new Dimension(0, 70));

        // Logo izquierda
        JPanel leftBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        leftBox.setOpaque(false);
        lblLogo = new JLabel();
        try {
            ImageIcon raw = new ImageIcon(getClass().getResource("/vistas/logop.png"));
            Image sc = raw.getImage().getScaledInstance(-1, 44, Image.SCALE_SMOOTH);
            lblLogo.setIcon(new ImageIcon(sc));
        } catch (Exception ex) {
            lblLogo.setText("CORSERPARK");
            lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 15));
            lblLogo.setForeground(NAVY);
            lblLogo.setBackground(Color.WHITE);
            lblLogo.setOpaque(true);
            lblLogo.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        }
        leftBox.add(lblLogo);
        bar.add(leftBox, BorderLayout.WEST);

        // Reloj derecha
        JPanel clockBox = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(NAVY_DARK);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
        };
        clockBox.setOpaque(false);
        clockBox.setBorder(BorderFactory.createEmptyBorder(4, 16, 4, 16));

        GridBagConstraints cg = new GridBagConstraints();
        cg.gridx = 0; cg.gridy = 0; cg.anchor = GridBagConstraints.CENTER;
        lbldiahora = new JLabel("00:00:00");
        lbldiahora.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lbldiahora.setForeground(Color.WHITE);
        clockBox.add(lbldiahora, cg);

        cg.gridy = 1;
        lblFecha = new JLabel("...");
        lblFecha.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblFecha.setForeground(new Color(0x64, 0x74, 0x8B));
        clockBox.add(lblFecha, cg);

        JPanel rightWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        rightWrap.setOpaque(false);
        rightWrap.add(clockBox);
        bar.add(rightWrap, BorderLayout.EAST);

        return bar;
    }

    // ── Cuerpo con CardLayout ─────────────────────────────────────────────────
    private JPanel buildBody() {
        bodyLayout = new CardLayout();
        bodyPanel  = new JPanel(bodyLayout);
        bodyPanel.setOpaque(false);

        bodyPanel.add(buildLoadingPanel(), "LOADING");
        bodyPanel.add(buildMenuPanel(),    "MENU");

        bodyLayout.show(bodyPanel, "LOADING");
        return bodyPanel;
    }

    // ── Pantalla 1: Cargando ──────────────────────────────────────────────────
    private JPanel buildLoadingPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.anchor = GridBagConstraints.CENTER;

        // Ícono circular con flecha de sincronización
        JLabel syncIcon = new JLabel("↻") {
            @Override protected void paintComponent(Graphics g2) {
                Graphics2D gx = (Graphics2D) g2.create();
                gx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gx.setColor(NAVY);
                gx.fill(new Ellipse2D.Float(0, 0, getWidth(), getHeight()));
                gx.setFont(getFont());
                gx.setColor(Color.WHITE);
                FontMetrics fm = gx.getFontMetrics();
                String t = getText();
                int tx = (getWidth()  - fm.stringWidth(t)) / 2;
                int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                gx.drawString(t, tx, ty);
                gx.dispose();
            }
        };
        syncIcon.setFont(new Font("Segoe UI", Font.BOLD, 34));
        syncIcon.setPreferredSize(new Dimension(70, 70));
        syncIcon.setMaximumSize (new Dimension(70, 70));
        syncIcon.setOpaque(false);
        g.gridy = 0; g.insets = new Insets(0, 0, 20, 0); g.fill = GridBagConstraints.NONE;
        panel.add(syncIcon, g);

        // Título fijo
        JLabel titleLabel = new JLabel("Sincronizando con el servidor");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 19));
        titleLabel.setForeground(new Color(0x1E, 0x29, 0x3B));
        g.gridy = 1; g.insets = new Insets(0, 0, 8, 0);
        panel.add(titleLabel, g);

        // Mensaje dinámico (actualizado por inicioMensaje)
        versincronizacion = new JLabel("Por favor espere un momento...");
        versincronizacion.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        versincronizacion.setForeground(TXT_MUT);
        g.gridy = 2; g.insets = new Insets(0, 0, 22, 0);
        panel.add(versincronizacion, g);

        // Barra de progreso indeterminada
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(280, 5));
        progressBar.setBackground(new Color(0xDC, 0xE5, 0xEF));
        progressBar.setForeground(NAVY);
        progressBar.setBorderPainted(false);
        progressBar.setStringPainted(false);
        g.gridy = 3; g.insets = new Insets(0, 0, 0, 0); g.fill = GridBagConstraints.NONE;
        panel.add(progressBar, g);

        return panel;
    }

    // ── Pantalla 2: Menú ──────────────────────────────────────────────────────
    private JPanel buildMenuPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 24, 10, 24));

        // Sede activa
        JPanel sedePanel = new JPanel(new GridBagLayout());
        sedePanel.setOpaque(false);
        sedePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        GridBagConstraints sg = new GridBagConstraints();
        sg.gridx = 0; sg.gridy = 0; sg.anchor = GridBagConstraints.CENTER;

        JLabel lblSedeLabel = new JLabel("Sede activa");
        lblSedeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSedeLabel.setForeground(TXT_MUT);
        sedePanel.add(lblSedeLabel, sg);

        sg.gridy = 1;
        lblSede = new JLabel(detallesede.isEmpty() ? "—" : detallesede);
        lblSede.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblSede.setForeground(NAVY);
        sedePanel.add(lblSede, sg);
        panel.add(sedePanel, BorderLayout.NORTH);

        // Dos cards
        JPanel cardsRow = new JPanel(new GridLayout(1, 2, 14, 0));
        cardsRow.setOpaque(false);
        jPanel1 = buildRegistroCard();
        jPanel2 = buildEnrolamientoCard();
        cardsRow.add(jPanel1);
        cardsRow.add(jPanel2);
        panel.add(cardsRow, BorderLayout.CENTER);

        // Botón Configurar — visible en la parte inferior
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        bottom.setOpaque(false);
        btnConfigurar = new JButton("⚙  Configurar") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? NAVY_MID : NAVY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnConfigurar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnConfigurar.setForeground(new Color(0xCC, 0xD5, 0xE0));
        btnConfigurar.setContentAreaFilled(false);
        btnConfigurar.setBorderPainted(false);
        btnConfigurar.setFocusPainted(false);
        btnConfigurar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnConfigurar.setBorder(BorderFactory.createEmptyBorder(7, 20, 7, 20));
        btnConfigurar.addActionListener(e -> btnConfigurarActionPerformed());
        bottom.add(btnConfigurar);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    // ── Card Registro (navy) ──────────────────────────────────────────────────
    private JPanel buildRegistroCard() {
        JPanel card = makeNavyCard();
        card.setLayout(new GridBagLayout());
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.anchor = GridBagConstraints.CENTER;

        // Ícono huella grande (sin doble círculo)
        lblIconIngreso = new JLabel();
        try {
            ImageIcon raw = new ImageIcon(getClass().getResource("/vistas/huelladigital_100_1.png"));
            Image sc = raw.getImage().getScaledInstance(90, 90, Image.SCALE_SMOOTH);
            lblIconIngreso.setIcon(new ImageIcon(sc));
        } catch (Exception ex) { lblIconIngreso.setText("◉"); lblIconIngreso.setFont(new Font("Segoe UI", Font.PLAIN, 52)); lblIconIngreso.setForeground(Color.WHITE); }
        lblIconIngreso.setHorizontalAlignment(SwingConstants.CENTER);

        // Ícono RFID pequeño al lado (para que exista pero no moleste)
        lblIconIngreso1 = new JLabel();
        try {
            ImageIcon raw = new ImageIcon(getClass().getResource("/vistas/sensor_rfid_100.png"));
            Image sc = raw.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH);
            lblIconIngreso1.setIcon(new ImageIcon(sc));
        } catch (Exception ex) { lblIconIngreso1.setText(""); }
        lblIconIngreso1.setVisible(false); // oculto — solo existe para el modelo

        g.gridy = 0; g.insets = new Insets(0, 0, 18, 0);
        card.add(lblIconIngreso, g);

        lblIndentificaion = new JLabel("Registro");
        lblIndentificaion.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblIndentificaion.setForeground(Color.WHITE);
        g.gridy = 1; g.insets = new Insets(0, 0, 5, 0);
        card.add(lblIndentificaion, g);

        JLabel sub = new JLabel("Huella / Tarjeta RFID");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(0x7B, 0x93, 0xBF));
        g.gridy = 2; g.insets = new Insets(0, 0, 0, 0);
        card.add(sub, g);

        MouseAdapter click = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { lblIconIngresoMouseClicked(e); }
            @Override public void mouseEntered(MouseEvent e) { card.repaint(); }
            @Override public void mouseExited(MouseEvent e)  { card.repaint(); }
        };
        card.addMouseListener(click);
        lblIconIngreso.addMouseListener(click);

        return card;
    }

    // ── Card Enrolamiento (blanco) ────────────────────────────────────────────
    private JPanel buildEnrolamientoCard() {
        JPanel card = makeWhiteCard();
        card.setLayout(new GridBagLayout());
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.anchor = GridBagConstraints.CENTER;

        lblIconRegistro = new JLabel();
        try {
            ImageIcon raw = new ImageIcon(getClass().getResource("/vistas/usuarios_100_1.png"));
            Image sc = raw.getImage().getScaledInstance(90, 90, Image.SCALE_SMOOTH);
            lblIconRegistro.setIcon(new ImageIcon(sc));
        } catch (Exception ex) { lblIconRegistro.setText("👤"); lblIconRegistro.setFont(new Font("Segoe UI", Font.PLAIN, 52)); }
        lblIconRegistro.setHorizontalAlignment(SwingConstants.CENTER);

        g.gridy = 0; g.insets = new Insets(0, 0, 18, 0);
        card.add(lblIconRegistro, g);

        lblRegistro = new JLabel("Enrolamiento");
        lblRegistro.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblRegistro.setForeground(NAVY);
        g.gridy = 1; g.insets = new Insets(0, 0, 5, 0);
        card.add(lblRegistro, g);

        JLabel sub = new JLabel("Nuevo empleado");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(TXT_MUT);
        g.gridy = 2; g.insets = new Insets(0, 0, 0, 0);
        card.add(sub, g);

        MouseAdapter click = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { lblIconRegistroMouseClicked(e); }
        };
        card.addMouseListener(click);
        lblIconRegistro.addMouseListener(click);

        return card;
    }

    // ── Status bar ────────────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(NAVY);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 34));
        bar.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));

        jLabel3 = new JLabel("@ SOLUTIC 3E SAS");
        jLabel3.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        jLabel3.setForeground(new Color(0xCC, 0xD5, 0xE0));

        jLabel1 = new JLabel("Ver 21.0 · 25032026");
        jLabel1.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        jLabel1.setForeground(new Color(0x64, 0x74, 0x8B));

        bar.add(jLabel3, BorderLayout.WEST);
        bar.add(jLabel1, BorderLayout.EAST);
        return bar;
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────
    private JPanel makeNavyCard() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(NAVY);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, ARC, ARC);
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(NAVY_MID);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, ARC, ARC);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(26, 20, 26, 20));
        return p;
    }

    private JPanel makeWhiteCard() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 16));
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
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(26, 20, 26, 20));
        return p;
    }

    // ── Reloj ─────────────────────────────────────────────────────────────────
    private void iniciarReloj() {
        java.text.SimpleDateFormat fH = new java.text.SimpleDateFormat("HH:mm:ss");
        java.text.SimpleDateFormat fD = new java.text.SimpleDateFormat("EEEE dd · MMM · yyyy",
                new java.util.Locale("es", "CO"));
        timerReloj = new Timer(1000, e -> {
            java.util.Date now = new java.util.Date();
            lbldiahora.setText(fH.format(now));
            String fd = fD.format(now);
            lblFecha.setText(fd.isEmpty() ? "" : Character.toUpperCase(fd.charAt(0)) + fd.substring(1));
        });
        timerReloj.start();
        java.util.Date now = new java.util.Date();
        lbldiahora.setText(fH.format(now));
    }

    // ── Handlers de eventos ───────────────────────────────────────────────────
    private void lblIconRegistroMouseClicked(MouseEvent evt) {
        Login lo = new Login(this);
        lo.setIconImage(new javax.swing.ImageIcon(Inicio.class.getResource("icon_frame.png")).getImage());
        lo.setVisible(true);
        this.setVisible(false);
    }

    private void lblIconIngresoMouseClicked(MouseEvent evt) {
        if (isProcessingClick) return;
        isProcessingClick = true;
        wformaregistropersonaini = "T";
        try {
            if (ingresoTarjeta != null && ingresoTarjeta.isVisible()) {
                ingresoTarjeta.toFront(); ingresoTarjeta.requestFocus(); return;
            }
            ingresoTarjeta = new IngresoTarjeta(this);
            ingresoTarjeta.setIconImage(new javax.swing.ImageIcon(Inicio.class.getResource("icon_frame.png")).getImage());
            this.setVisible(false);
            ingresoTarjeta.setVisible(true);
        } catch (Exception e) { e.printStackTrace(); }
        finally { isProcessingClick = false; }
    }

    private void lblIconIngreso1MouseClicked(MouseEvent evt) {
        if (isProcessingClick) return;
        isProcessingClick = true;
        wformaregistropersonaini = "T";
        try {
            if (ingresoTarjeta != null && ingresoTarjeta.isVisible()) {
                ingresoTarjeta.toFront(); ingresoTarjeta.requestFocus(); return;
            }
            ingresoTarjeta = new IngresoTarjeta(this);
            ingresoTarjeta.setIconImage(new javax.swing.ImageIcon(Inicio.class.getResource("icon_frame.png")).getImage());
            this.setVisible(false);
            ingresoTarjeta.setVisible(true);
        } catch (Exception e) { e.printStackTrace(); }
        finally { isProcessingClick = false; }
    }

    private void btnConfigurarActionPerformed() {
        Configurar i = new Configurar(this);
        i.setIconImage(new javax.swing.ImageIcon(Inicio.class.getResource("icon_frame.png")).getImage());
        this.setVisible(false);
        i.setVisible(true);
    }

    // ── API pública requerida por Sincronizar y ControlIngreso ────────────────
    public void setUltimaSincronizacion(String d) { this.ultimaS = d; }

    public void startSinc() { if (ingreso != null) ingreso.setOcupado(); }

    public void finSinc()   { if (ingreso != null) ingreso.setLibre(); }

    public void inicioMensaje(String wmensaje) {
        SwingUtilities.invokeLater(() -> versincronizacion.setText(wmensaje));
    }

    public void ocultar_opcion1() {
        jPanel1.setVisible(false);
        jPanel2.setVisible(false);
        bodyLayout.show(bodyPanel, "LOADING");
        if (progressBar != null) progressBar.setIndeterminate(true);
    }

    public void mostrar_opcion1() {
        if (!detallesede.isEmpty() && lblSede != null) lblSede.setText(detallesede);
        lblIconIngreso.setEnabled(true);
        jPanel1.setVisible(true);
        jPanel2.setVisible(true);
        bodyLayout.show(bodyPanel, "MENU");
        if (progressBar != null) progressBar.setIndeterminate(false);
    }
}
