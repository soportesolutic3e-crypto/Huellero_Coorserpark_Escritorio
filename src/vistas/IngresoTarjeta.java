package vistas;

import com.digitalpersona.onetouch.DPFPGlobal;
import com.digitalpersona.onetouch.DPFPSample;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.*;
import process.Sincronizar;
import models.IngresoModelTarjeta;

public class IngresoTarjeta extends JFrame implements WindowListener, Runnable {

    // ── Paleta ────────────────────────────────────────────────────────────────
    private static final Color NAVY      = new Color(0x1A, 0x27, 0x44);
    private static final Color NAVY_DARK = new Color(0x0E, 0x17, 0x2D);
    private static final Color NAVY_MID  = new Color(0x26, 0x38, 0x6B);
    private static final Color BG_PAGE   = new Color(0xEE, 0xF2, 0xF7);
    private static final Color BG_CARD   = Color.WHITE;
    private static final Color BG_FIELD  = new Color(0xF6, 0xF8, 0xFA);
    private static final Color BG_BLUE   = new Color(0xEB, 0xF4, 0xFF);
    private static final Color BORDER_C  = new Color(0xDC, 0xE5, 0xEF);
    private static final Color BORDER_B  = new Color(0xB0, 0xCF, 0xF8);
    private static final Color TXT_PRI   = new Color(0x1E, 0x29, 0x3B);
    private static final Color TXT_MUT   = new Color(0x94, 0xA3, 0xB8);
    private static final Color GREEN_BG  = new Color(0xED, 0xFD, 0xF4);
    private static final Color GREEN_BD  = new Color(0xA7, 0xF3, 0xD0);
    private static final Color GREEN_TXT = new Color(0x05, 0x7A, 0x3A);
    private static final Color AVATAR_BD = new Color(0x22, 0xC5, 0x5E);
    private static final int   ARC       = 16;

    // ── Componentes requeridos por IngresoModelTarjeta ────────────────────────
    private JLabel     lblicon;
    private JLabel     jLabel2;
    private JLabel     lblMensajex;
    private JLabel     lbldiahora;
    private JLabel     lblFechaTopbar;
    private JTextField nrotarje;
    private JLabel     lblAvatar;
    private JLabel     lblVerificado;
    private JLabel     lblccSmall;
    public  JLabel     lblHuella;
    private JTextArea  txtArea;
    private JScrollPane jScrollPane1;
    private JTextField txtcc;
    private JTextField txtNombre;
    private JTextField txtEmpresa;
    private JTextField txtCiudad;
    private JTextField txtDepartamento;
    private JTextField txtCargo;
    private JTextField txtRotativo;
    private JTextField txtHIngreso;
    private JTextField txtHoraSalida;
    private JLabel     lblMensaje1;
    private JLabel     lblMensaje;
    private JLabel     lblfecha;
    private JLabel     lblEstadoVale;
    private JLabel     verestadoconexion;
    private JButton    btnSincronizar;
    private JLabel     jLabel1;
    private JTextPane  txtDedo;
    private JButton    btnImprimir;

    // ── Estado ────────────────────────────────────────────────────────────────
    public  String  wformaregistroIT;
    public  Inicio  inicio;
    private Timer   timerReloj;
    private Thread  ramupdate;
    public  volatile boolean leyendoEmpleados        = true;
    private volatile boolean procesandoLecturaTarjeta = false;
    IngresoModelTarjeta ingresoModelTarjeta;

    // ─────────────────────────────────────────────────────────────────────────
    public IngresoTarjeta(Inicio i) {
        super("Sistema Control de Ingreso");
        this.inicio = i;

        // Campos que NO van al layout pero el modelo los necesita
        btnImprimir   = new JButton();  btnImprimir.setEnabled(false);
        lblfecha      = new JLabel();
        lblEstadoVale = new JLabel();

        buildUI();
        iniciarReloj();

        nrotarje.setFocusable(true);
        nrotarje.requestFocusInWindow();
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) {
                SwingUtilities.invokeLater(() -> nrotarje.requestFocusInWindow());
            }
        });

        ingresoModelTarjeta = new IngresoModelTarjeta(this, i);
        cerrar();

        System.out.println("LLego con el tipo de registroxxx.." + Inicio.wformaregistropersonaini);

        try {
            ramupdate = new Thread(this);
            if (Sincronizar.wsinconexion == 1)
                verestadoconexion.setText("Sin Conexión a Red: verificar!");
            else if (Sincronizar.wsinconexion == 2)
                verestadoconexion.setText("No hay Datos Empleados");
        } catch (Exception e) { }
    }

    // ── Construcción UI ───────────────────────────────────────────────────────
    private void buildUI() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);
        setSize(960, 610);
        setLocationRelativeTo(null);

        // Campo RFID: 1 px, invisible
        nrotarje = new JTextField();
        nrotarje.setPreferredSize(new Dimension(1, 1));
        nrotarje.setMaximumSize(new Dimension(1, 1));
        nrotarje.setBackground(NAVY);
        nrotarje.setForeground(NAVY);
        nrotarje.setCaretColor(NAVY);
        nrotarje.setBorder(null);
        nrotarje.addActionListener(e -> onRFID());

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_PAGE);
        setContentPane(root);

        root.add(buildTopbar(),    BorderLayout.NORTH);
        root.add(buildBody(),      BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);
    }

    // ── Topbar ────────────────────────────────────────────────────────────────
    private JPanel buildTopbar() {
        JPanel bar = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(NAVY);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 74));

        // Logo izquierda
        JPanel leftBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        leftBox.setOpaque(false);
        lblicon = new JLabel();
        try {
            ImageIcon raw = new ImageIcon(getClass().getResource("/vistas/logop.png"));
            Image sc = raw.getImage().getScaledInstance(-1, 46, Image.SCALE_SMOOTH);
            lblicon.setIcon(new ImageIcon(sc));
        } catch (Exception ex) {
            lblicon.setText("CORSERPARK");
            lblicon.setFont(new Font("Segoe UI", Font.BOLD, 15));
            lblicon.setForeground(Color.WHITE);
        }
        leftBox.add(lblicon);
        bar.add(leftBox, BorderLayout.WEST);

        // Centro: título + sede
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0; gc.anchor = GridBagConstraints.CENTER;

        jLabel2 = new JLabel("CONTROL REGISTRO PERSONAL");
        jLabel2.setFont(new Font("Segoe UI", Font.BOLD, 16));
        jLabel2.setForeground(Color.WHITE);
        center.add(jLabel2, gc);

        gc.gridy = 1;
        lblMensajex = new JLabel(" ");
        lblMensajex.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblMensajex.setForeground(Color.WHITE);
        center.add(lblMensajex, gc);
        bar.add(center, BorderLayout.CENTER);

        // Reloj derecha
        JPanel clockBox = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Borde degradado azul claro → blanco
                java.awt.LinearGradientPaint grad = new java.awt.LinearGradientPaint(
                    0, 0, getWidth(), getHeight(),
                    new float[]{0f, 1f},
                    new Color[]{new Color(0x60, 0xA5, 0xFA), Color.WHITE}
                );
                g2.setPaint(grad);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                // Fondo interior navy
                g2.setColor(NAVY_DARK);
                g2.fillRoundRect(2, 2, getWidth()-4, getHeight()-4, 10, 10);
                g2.dispose();
            }
        };
        clockBox.setOpaque(false);
        clockBox.setBorder(BorderFactory.createEmptyBorder(4, 18, 4, 18));

        GridBagConstraints cg = new GridBagConstraints();
        cg.gridx = 0; cg.gridy = 0; cg.anchor = GridBagConstraints.CENTER;
        lbldiahora = new JLabel("00:00:00");
        lbldiahora.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lbldiahora.setForeground(Color.WHITE);
        clockBox.add(lbldiahora, cg);

        cg.gridy = 1;
        lblFechaTopbar = new JLabel("...");
        lblFechaTopbar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblFechaTopbar.setForeground(Color.WHITE);
        clockBox.add(lblFechaTopbar, cg);

        JPanel rightWrap = new JPanel(new GridBagLayout());
        rightWrap.setOpaque(false);
        GridBagConstraints rw = new GridBagConstraints();
        rw.insets = new java.awt.Insets(0, 0, 0, 14);
        rightWrap.add(clockBox, rw);
        bar.add(rightWrap, BorderLayout.EAST);

        return bar;
    }

    // ── Cuerpo ────────────────────────────────────────────────────────────────
    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(10, 0));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(10, 12, 6, 12));
        body.add(buildLeftColumn(),  BorderLayout.WEST);
        body.add(buildRightColumn(), BorderLayout.CENTER);
        return body;
    }

    // ── Columna izquierda ─────────────────────────────────────────────────────
    private JPanel buildLeftColumn() {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);
        col.setPreferredSize(new Dimension(178, 0));

        // Panel identidad navy redondeado
        JPanel idCard = makeNavyCard();
        idCard.setLayout(new BoxLayout(idCard, BoxLayout.Y_AXIS));
        idCard.setBorder(BorderFactory.createEmptyBorder(16, 10, 16, 10));

        // Avatar circular con paintComponent personalizado
        lblAvatar = new JLabel("  ") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(NAVY_MID);
                g2.fill(new Ellipse2D.Float(1, 1, getWidth()-3, getHeight()-3));
                String t = getText() == null ? "" : getText().trim();
                if (!t.isEmpty()) {
                    g2.setFont(getFont());
                    g2.setColor(Color.WHITE);
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = (getWidth() - fm.stringWidth(t)) / 2;
                    int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(t, tx, ty);
                }
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                String t = getText() == null ? "  " : getText();
                g2.setColor("  ".equals(t) ? NAVY_MID : AVATAR_BD);
                g2.setStroke(new BasicStroke(2.5f));
                g2.draw(new Ellipse2D.Float(1.5f, 1.5f, getWidth()-4, getHeight()-4));
                g2.dispose();
            }
        };
        lblAvatar.setOpaque(false);
        lblAvatar.setFont(new Font("Segoe UI", Font.BOLD, 20));
        Dimension avSize = new Dimension(66, 66);
        lblAvatar.setPreferredSize(avSize);
        lblAvatar.setMaximumSize(avSize);
        lblAvatar.setMinimumSize(avSize);
        lblAvatar.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblVerificado = new JLabel(" ");
        lblVerificado.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblVerificado.setForeground(new Color(0x22, 0xC5, 0x5E));
        lblVerificado.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblccSmall = new JLabel(" ");
        lblccSmall.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblccSmall.setForeground(Color.WHITE);
        lblccSmall.setAlignmentX(Component.CENTER_ALIGNMENT);

        idCard.add(Box.createVerticalStrut(2));
        idCard.add(lblAvatar);
        idCard.add(Box.createVerticalStrut(10));
        idCard.add(lblVerificado);
        idCard.add(Box.createVerticalStrut(3));
        idCard.add(lblccSmall);
        idCard.add(Box.createVerticalStrut(2));

        // Card huella capturada
        JPanel huellaCard = makeWhiteCard();
        huellaCard.setLayout(new BoxLayout(huellaCard, BoxLayout.Y_AXIS));
        huellaCard.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel huellaTitle = new JLabel("HUELLA CAPTURADA");
        huellaTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        huellaTitle.setForeground(TXT_PRI);
        huellaTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblHuella = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_FIELD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblHuella.setHorizontalAlignment(SwingConstants.CENTER);
        lblHuella.setPreferredSize(new Dimension(170, 145));
        lblHuella.setMaximumSize(new Dimension(Integer.MAX_VALUE, 145));
        lblHuella.setMinimumSize(new Dimension(100, 100));
        lblHuella.setOpaque(false);
        lblHuella.setAlignmentX(Component.CENTER_ALIGNMENT);
        try {
            ImageIcon raw = new ImageIcon(getClass().getResource("/vistas/reloj.png"));
            Image sc = raw.getImage().getScaledInstance(90, 90, Image.SCALE_SMOOTH);
            lblHuella.setIcon(new ImageIcon(sc));
        } catch (Exception ex) { }

        txtArea = new JTextArea(3, 13);
        txtArea.setEditable(false);
        txtArea.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        txtArea.setBackground(BG_FIELD);
        txtArea.setForeground(TXT_PRI);
        txtArea.setBorder(null);
        txtArea.setLineWrap(true);
        txtArea.setWrapStyleWord(true);
        jScrollPane1 = new JScrollPane(txtArea);
        jScrollPane1.setBorder(BorderFactory.createEmptyBorder());
        jScrollPane1.setBackground(BG_FIELD);
        jScrollPane1.getViewport().setBackground(BG_FIELD);
        jScrollPane1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        jScrollPane1.setPreferredSize(new Dimension(170, 52));

        huellaCard.add(huellaTitle);
        huellaCard.add(Box.createVerticalStrut(6));
        huellaCard.add(lblHuella);
        huellaCard.add(Box.createVerticalStrut(6));
        huellaCard.add(jScrollPane1);

        huellaCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        col.add(idCard);
        col.add(Box.createVerticalStrut(8));
        col.add(huellaCard);

        return col;
    }

    // ── Columna derecha ───────────────────────────────────────────────────────
    private JPanel buildRightColumn() {
        JPanel col = new JPanel(new GridBagLayout());
        col.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.weightx = 1;

        // Card CC + Nombre
        JPanel idCard = makeWhiteCard();
        idCard.setLayout(new GridBagLayout());
        GridBagConstraints ig = new GridBagConstraints();
        ig.fill = GridBagConstraints.HORIZONTAL; ig.weighty = 0;

        txtcc     = makeBigField();
        txtNombre = makeBigField();
        txtcc.setPreferredSize(new Dimension(145, 36));

        ig.gridx=0; ig.gridy=0; ig.weightx=0; ig.insets=new Insets(0,0,3,12); idCard.add(makeLabel("NO. IDENTIFICACIÓN"), ig);
        ig.gridx=1; ig.gridy=0; ig.weightx=1; ig.insets=new Insets(0,0,3,0);  idCard.add(makeLabel("NOMBRES Y APELLIDOS"), ig);
        ig.gridx=0; ig.gridy=1; ig.weightx=0; ig.insets=new Insets(0,0,0,12); idCard.add(txtcc, ig);
        ig.gridx=1; ig.gridy=1; ig.weightx=1; ig.insets=new Insets(0,0,0,0);  idCard.add(txtNombre, ig);

        g.gridx=0; g.gridy=0; g.weighty=0; g.insets=new Insets(0,0,8,0);
        col.add(idCard, g);

        // Fila media: Entorno + Jornada
        JPanel midRow = new JPanel(new GridLayout(1, 2, 8, 0));
        midRow.setOpaque(false);
        midRow.add(buildEntornoCard());
        midRow.add(buildJornadaCard());

        g.gridx=0; g.gridy=1; g.weighty=1; g.insets=new Insets(0,0,0,0);
        col.add(midRow, g);

        return col;
    }

    private JPanel buildEntornoCard() {
        JPanel card = makeWhiteCard();
        card.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx=0; g.gridy=0; g.gridwidth=2; g.weightx=1;
        g.insets=new Insets(0,4,10,4);
        card.add(sectionTitle("Entorno laboral"), g);

        g.gridwidth=1; g.insets=new Insets(3,4,3,6);
        txtEmpresa      = makeField();
        txtCiudad       = makeField();
        txtDepartamento = makeField();
        txtCargo        = makeField();
        txtRotativo     = makeField();

        String[]     lbls = {"EMPRESA","CIUDAD","DEPARTAMENTO","CARGO","ROTATIVO"};
        JTextField[] flds = {txtEmpresa,txtCiudad,txtDepartamento,txtCargo,txtRotativo};
        for (int i = 0; i < lbls.length; i++) {
            g.gridx=0; g.gridy=i+1; g.weightx=0; card.add(makeLabel(lbls[i]), g);
            g.gridx=1; g.gridy=i+1; g.weightx=1; card.add(flds[i], g);
        }
        // Card vale — redondeada gris oscuro, debajo de ROTATIVO
        JPanel valeCard = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4)) {
            @Override protected void paintComponent(Graphics g2) {
                Graphics2D gx = (Graphics2D) g2.create();
                gx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gx.setColor(new Color(0xE2, 0xE8, 0xF0));
                gx.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                gx.dispose();
            }
            @Override protected void paintBorder(Graphics g2) {
                Graphics2D gx = (Graphics2D) g2.create();
                gx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gx.setColor(new Color(0xC8, 0xD3, 0xDF));
                gx.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                gx.dispose();
            }
        };
        valeCard.setOpaque(false);
        valeCard.setBorder(BorderFactory.createEmptyBorder(7, 8, 7, 8));

        lblEstadoVale.setPreferredSize(new Dimension(32, 32));
        lblEstadoVale.setHorizontalAlignment(SwingConstants.CENTER);
        valeCard.add(lblEstadoVale);

        lblfecha.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        lblfecha.setForeground(new Color(0x55, 0x65, 0x7A));
        lblfecha.setText("Impresión de vales desactivada");
        valeCard.add(lblfecha);

        g.gridx=0; g.gridy=lbls.length+1; g.gridwidth=2; g.weightx=1; g.weighty=0;
        g.fill=GridBagConstraints.HORIZONTAL; g.insets=new Insets(8,4,2,4);
        card.add(valeCard, g);

        return card;
    }

    private JPanel buildJornadaCard() {
        JPanel card = makeWhiteCard();
        card.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx=0; g.gridy=0; g.gridwidth=2; g.weightx=1;
        g.insets=new Insets(0,4,10,4);
        card.add(sectionTitle("Jornada laboral"), g);

        g.gridwidth=1; g.insets=new Insets(3,4,3,6);
        txtHIngreso   = makeField();
        txtHoraSalida = makeField();

        g.gridx=0; g.gridy=1; g.weightx=0; card.add(makeLabel("HORA ENTRADA"), g);
        g.gridx=1; g.gridy=1; g.weightx=1; card.add(txtHIngreso, g);
        g.gridx=0; g.gridy=2; g.weightx=0; card.add(makeLabel("HORA SALIDA"), g);
        g.gridx=1; g.gridy=2; g.weightx=1; card.add(txtHoraSalida, g);

        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_C);
        g.gridx=0; g.gridy=3; g.gridwidth=2; g.fill=GridBagConstraints.HORIZONTAL;
        g.insets=new Insets(10,4,8,4);
        card.add(sep, g);

        // Label fecha evento — redondeado
        lblMensaje1 = new JLabel(" ") {
            @Override protected void paintComponent(Graphics g2) {
                Graphics2D gx = (Graphics2D) g2.create();
                gx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gx.setColor(BG_BLUE);
                gx.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                gx.dispose();
                super.paintComponent(g2);
            }
            @Override protected void paintBorder(Graphics g2) {
                Graphics2D gx = (Graphics2D) g2.create();
                gx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gx.setColor(BORDER_B);
                gx.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                gx.dispose();
            }
        };
        lblMensaje1.setOpaque(false);
        lblMensaje1.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblMensaje1.setForeground(NAVY);
        lblMensaje1.setHorizontalAlignment(SwingConstants.CENTER);
        lblMensaje1.setPreferredSize(new Dimension(0, 32));
        lblMensaje1.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        g.gridx=0; g.gridy=4; g.gridwidth=2; g.weightx=1; g.insets=new Insets(2,4,6,4);
        g.fill=GridBagConstraints.HORIZONTAL;
        card.add(lblMensaje1, g);

        // Label INGRESO / SALIDA — redondeado verde
        lblMensaje = new JLabel(" ") {
            @Override protected void paintComponent(Graphics g2) {
                Graphics2D gx = (Graphics2D) g2.create();
                gx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gx.setColor(GREEN_BG);
                gx.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                gx.dispose();
                super.paintComponent(g2);
            }
            @Override protected void paintBorder(Graphics g2) {
                Graphics2D gx = (Graphics2D) g2.create();
                gx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gx.setColor(GREEN_BD);
                gx.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                gx.dispose();
            }
        };
        lblMensaje.setOpaque(false);
        lblMensaje.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblMensaje.setForeground(GREEN_TXT);
        lblMensaje.setHorizontalAlignment(SwingConstants.CENTER);
        lblMensaje.setPreferredSize(new Dimension(0, 72));
        lblMensaje.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        lblMensaje.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        g.gridx=0; g.gridy=5; g.gridwidth=2; g.weighty=0;
        g.fill=GridBagConstraints.HORIZONTAL; g.insets=new Insets(0,4,2,4);
        card.add(lblMensaje, g);

        return card;
    }

    // ── Status bar ────────────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        JPanel bar = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(NAVY);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(7, 16, 7, 16));

        //jLabel1 = new JLabel("@SOLUTIC 3E SAS (Ver 22.0&25032026)");
        jLabel1 = new JLabel("@SOLUTIC 3E SAS (Ver 23.0&15092026)");
        jLabel1.setFont(new Font("Segoe UI", Font.BOLD, 11));
        jLabel1.setForeground(Color.WHITE);

        verestadoconexion = new JLabel(" ");
        verestadoconexion.setFont(new Font("Segoe UI", Font.BOLD, 18));
        verestadoconexion.setForeground(new Color(0xFA, 0xCC, 0x15));
        verestadoconexion.setHorizontalAlignment(SwingConstants.CENTER);

        btnSincronizar = new JButton("Sincronizar") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? Color.WHITE : NAVY_MID);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                setForeground(getModel().isRollover() ? NAVY : Color.WHITE);
                super.paintComponent(g);
            }
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 8, 8);
                g2.dispose();
            }
        };
        btnSincronizar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSincronizar.setForeground(Color.WHITE);
        btnSincronizar.setContentAreaFilled(false);
        btnSincronizar.setBorderPainted(false);
        btnSincronizar.setFocusPainted(false);
        btnSincronizar.setBorder(BorderFactory.createEmptyBorder(6, 18, 6, 18));
        btnSincronizar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSincronizar.addActionListener(e -> onSincronizar());

        bar.add(jLabel1,           BorderLayout.WEST);
        bar.add(verestadoconexion, BorderLayout.CENTER);
        bar.add(btnSincronizar,    BorderLayout.EAST);

        txtDedo = new JTextPane();
        txtDedo.setEditable(false);
        txtDedo.setBackground(NAVY);
        txtDedo.setPreferredSize(new Dimension(0, 4));
        txtDedo.setBorder(null);

        wrapper.add(bar,      BorderLayout.CENTER);
        wrapper.add(txtDedo,  BorderLayout.SOUTH);
        wrapper.add(nrotarje, BorderLayout.NORTH);
        return wrapper;
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────

    private JPanel makeWhiteCard() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // sombra sutil
                g2.setColor(new Color(0, 0, 0, 16));
                g2.fillRoundRect(2, 3, getWidth()-3, getHeight()-2, ARC+2, ARC+2);
                // fondo blanco redondeado
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
        p.setBackground(BG_CARD);
        p.setBorder(BorderFactory.createEmptyBorder(12, 14, 14, 14));
        return p;
    }

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
        p.setBackground(NAVY);
        return p;
    }

    private JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(TXT_PRI);
        return l;
    }

    private JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 16));
        l.setForeground(NAVY);
        return l;
    }

    private JTextField makeBigField() {
        JTextField f = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        f.setFont(new Font("Segoe UI", Font.BOLD, 17));
        f.setForeground(NAVY);
        f.setBackground(BG_BLUE);
        f.setOpaque(false);
        f.setEditable(false);
        f.setPreferredSize(new Dimension(200, 36));
        f.setBorder(new CompoundBorder(
            new RoundedBorder(BORDER_B, 8),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        return f;
    }

    private JTextField makeField() {
        JTextField f = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 7, 7);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        f.setFont(new Font("Segoe UI", Font.BOLD, 12));
        f.setForeground(TXT_PRI);
        f.setBackground(BG_BLUE);
        f.setOpaque(false);
        f.setEditable(false);
        f.setPreferredSize(new Dimension(100, 28));
        f.setBorder(new CompoundBorder(
            new RoundedBorder(BORDER_C, 7),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        return f;
    }

    private static class RoundedBorder extends AbstractBorder {
        private final Color color; private final int arc;
        RoundedBorder(Color c, int a) { color = c; arc = a; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, w-1, h-1, arc, arc);
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(1,1,1,1); }
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
            lblFechaTopbar.setText(fd.isEmpty() ? "" : Character.toUpperCase(fd.charAt(0)) + fd.substring(1));
        });
        timerReloj.start();
        java.util.Date now = new java.util.Date();
        lbldiahora.setText(fH.format(now));
    }

    // ── Avatar ────────────────────────────────────────────────────────────────
    private void actualizarAvatar(String cc) {
        if (cc == null || cc.trim().isEmpty()) {
            lblAvatar.setText("  ");
            lblVerificado.setText(" ");
            lblccSmall.setText(" ");
        } else {
            String nombre = txtNombre.getText().trim();
            String init   = cc.length() >= 2 ? cc.substring(0, 2) : cc;
            if (!nombre.isEmpty()) {
                String[] pts = nombre.split("\\s+");
                init = pts.length >= 2
                    ? "" + pts[0].charAt(0) + pts[1].charAt(0)
                    : "" + pts[0].charAt(0);
            }
            lblAvatar.setText(init.toUpperCase());
            lblVerificado.setText("✓  Identidad verificada");
            lblccSmall.setText(cc);
        }
        lblAvatar.repaint();
    }

    // ── RFID ANTERIOR V21 ──────────────────────────────────────────────────────────────────
    /*
    private void onRFID() {
        if (procesandoLecturaTarjeta) return;
        procesandoLecturaTarjeta = true;
        try {
            ingresoModelTarjeta.validarTarjeta();
        } catch (SQLException ex) {
            Logger.getLogger(IngresoTarjeta.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            nrotarje.setText("");
            SwingUtilities.invokeLater(() -> nrotarje.requestFocusInWindow());
            procesandoLecturaTarjeta = false;
        }
    }
    */
    
    /* VERSION V23 15-09-2026 */
    private void onRFID() {
        if (procesandoLecturaTarjeta) return;
        procesandoLecturaTarjeta = true;

        // Se saca de la interfaz para que un WebService lento (o el failover
        // probando la ruta alterna) nunca congele la ventana durante el swipe.
        new Thread(() -> {
            try {
                ingresoModelTarjeta.validarTarjeta();
            } catch (SQLException ex) {
                Logger.getLogger(IngresoTarjeta.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                SwingUtilities.invokeLater(() -> {
                    nrotarje.setText("");
                    nrotarje.requestFocusInWindow();
                    procesandoLecturaTarjeta = false;
                });
            }
        }, "ValidarTarjeta-Thread").start();
    }

    /******** VERSION V21 ANTERIOR ORIGINAL *********/
    /*private void onSincronizar() {
        process.LicenciaManager.Resultado lic = process.LicenciaManager.verificarServidor();
        if (!lic.ok) {
            new process.LicenciaErrorDialog(lic).setVisible(true);
            return;
        }
        if (ramupdate != null && ramupdate.getState() != Thread.State.RUNNABLE) {
            Sincronizar.registroaccion = "SINCRONIZACION MANUAL";
            ramupdate = new Thread(this);
            ramupdate.start();
            nrotarje.requestFocus();
        }
    }*/
    
    /*VERSION V23 15-09-2026*/
        private void onSincronizar() {
        // La verificación de licencia (llamada de red) se saca de la interfaz;
        // el resto de la lógica queda exactamente igual, solo que se resuelve
        // cuando la respuesta llegue, en vez de congelar el botón mientras espera.
        new Thread(() -> {
            process.LicenciaManager.Resultado lic = process.LicenciaManager.verificarServidor();
            SwingUtilities.invokeLater(() -> {
                if (!lic.ok) {
                    new process.LicenciaErrorDialog(lic).setVisible(true);
                    return;
                }
                if (ramupdate != null && ramupdate.getState() != Thread.State.RUNNABLE) {
                    Sincronizar.registroaccion = "SINCRONIZACION MANUAL";
                    ramupdate = new Thread(this);
                    ramupdate.start();
                    nrotarje.requestFocus();
                }
            });
        }, "VerificarLicencia-Thread").start();
    }

    // ── Cierre ────────────────────────────────────────────────────────────────
    public void cerrar() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { confirmarSalida(); }
        });
        setVisible(true);
    }

    public void confirmarSalida() {
        Sincronizar sincro = new Sincronizar();
        int r = JOptionPane.showConfirmDialog(this,
            "¿Está seguro de salir?", "Advertencia",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r == JOptionPane.YES_OPTION) {
            try {
                setVisible(false);
                Sincronizar.registroaccion = "CIERRE HUELLERO";
                if (ramupdate != null && ramupdate.isAlive()) {
                    ramupdate.interrupt(); ramupdate.join(2000);
                }
                sincro.runManual();
                if (timerReloj != null && timerReloj.isRunning()) { timerReloj.stop(); timerReloj = null; }
                System.exit(0);
            } catch (Exception e) {
                if (timerReloj != null) { timerReloj.stop(); timerReloj = null; }
                System.exit(1);
            }
        }
    }

    // ── API requerida por IngresoModelTarjeta ─────────────────────────────────

    public void EnviarTexto(String string) { txtArea.append(string + "\n"); }

    public void closeCapture(String msg) {
        JOptionPane.showMessageDialog(this, msg);
        ingresoModelTarjeta.closeCapture();
        setVisible(false);
        dispose();
    }

    public Image CrearImagenHuella(DPFPSample sample) {
        return DPFPGlobal.getSampleConversionFactory().createImage(sample);
    }

    public void DibujarHuellaBorrar(Image image) {
        lblHuella.setIcon(null);
        lblHuella.setIcon(new ImageIcon(
            image.getScaledInstance(ingresoModelTarjeta.lblwidth,
                                    ingresoModelTarjeta.lblHeight, Image.SCALE_DEFAULT)));
        repaint();
    }

    public void DibujarHuella(Image image) {
        if (image == null) return;
        SwingUtilities.invokeLater(() -> {
            try {
                int w = lblHuella.getWidth();  if (w <= 0) w = 170;
                int h = lblHuella.getHeight(); if (h <= 0) h = 145;
                lblHuella.setIcon(new ImageIcon(image.getScaledInstance(w, h, Image.SCALE_SMOOTH)));
                lblHuella.revalidate(); lblHuella.repaint();
            } catch (Exception ex) { ex.printStackTrace(); }
        });
    }

    public void ImagenTarjeta() {
        System.out.println("LLego a la imagen de la tarjeta");
        lblHuella.setIcon(new ImageIcon(getClass().getResource("/vistas/lectorrfid1.jpg")));
    }

    private void limpiarCampos() {
        txtcc.setText(""); txtNombre.setText(""); txtCargo.setText("");
        txtEmpresa.setText(""); txtDepartamento.setText("");
        txtHIngreso.setText(""); txtHoraSalida.setText("");
        txtCiudad.setText(""); txtRotativo.setText("");
        lblAvatar.setText("  ");
        lblVerificado.setText(" ");
        lblccSmall.setText(" ");
        lblAvatar.repaint();
    }

    public void barra_avance() {
        txtDedo.setBackground(Color.GREEN);
        lblMensaje1.setText(" ");
        lblMensaje.setForeground(GREEN_TXT);
        lblMensaje.setText(" ");
        btnImprimir.setEnabled(false);
        limpiarCampos();
        leyendoEmpleados = false;
        verestadoconexion.setText("Verificando, Por favor esperar!");
        if (Sincronizar.wsinconexion == 1)
            verestadoconexion.setText("Sin Conexión a Red: verificar!");
    }

    public void pintarDedoPuesto() {
        txtDedo.setBackground(Color.GREEN);
        lblMensaje1.setText(" ");
        lblMensaje.setForeground(GREEN_TXT);
        lblMensaje.setText(" ");
        btnImprimir.setEnabled(false);
        limpiarCampos();
        leyendoEmpleados = false;
        verestadoconexion.setText("Verificando, Por favor esperar!");
        if (Sincronizar.wsinconexion == 1)
            verestadoconexion.setText("Sin Conexión a Red: verificar!");
    }

    public void pintarDedonoPuesto() {
        txtDedo.setBackground(NAVY);
        verestadoconexion.setText("");
        if (Sincronizar.wsinconexion == 1)
            verestadoconexion.setText("Sin Conexión a Red: verificar!");
    }

    public void pintarubicacion() { verestadoconexion.setText("Error en comunicacion"); }

    public void actualizarIconoVales(boolean activo) {
        String ruta = activo ? "/vistas/vale_on.png" : "/vistas/vale_off.png";
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource(ruta));
            Image img = icon.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
            lblEstadoVale.setIcon(new ImageIcon(img));
        } catch (Exception e) { lblEstadoVale.setIcon(null); }
        lblfecha.setText(activo ? "Impresión de vales activa" : "Impresión de vales desactivada");
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public int    lblHuella_getWidth()          { return lblHuella.getWidth();  }
    public int    lblHuella_getHeight()         { return lblHuella.getHeight(); }
    public String txtcc_getText()               { return txtcc.getText(); }
    public String nrotarje_getText()            { return nrotarje.getText(); }
    public String txtRotativo_getText()         { return txtRotativo.getText(); }
    public String txtHIngreso_getText()         { return txtHIngreso.getText(); }
    public String txtNombre_getText()           { return txtNombre.getText(); }
    public String txtDepartamento_getText()     { return txtDepartamento.getText(); }
    public String txtEmpresa_getText()          { return txtEmpresa.getText(); }
    public String txtMensajeVale_getText()      { return lblfecha.getText(); }
    public boolean btnImprimir_isEnabled()      { return btnImprimir.isEnabled(); }

    public void txtcc_setText(String v)             { txtcc.setText(v); }
    public void nrotarje_setText(String v)           { nrotarje.setText(v); }
    public void txtNombre_setText(String v)          { txtNombre.setText(v); actualizarAvatar(txtcc.getText()); }
    public void txtCargo_setText(String v)           { txtCargo.setText(v); }
    public void txtEmpresa_setText(String v)         { txtEmpresa.setText(v); }
    public void txtDepartamento_setText(String v)    { txtDepartamento.setText(v); }
    public void txtCiudad_setText(String v)          { txtCiudad.setText(v); }
    public void txtRotativo_setText(String v)        { txtRotativo.setText(v); }
    public void txtHIngreso_setText(String v)        { txtHIngreso.setText(v); }
    public void txtHoraSalida_setText(String v)      { txtHoraSalida.setText(v); }
    public void txtMensajeVale_setText(String v)     { lblfecha.setText(v); }
    public void btnImprimir_setEnabled(boolean b)    { btnImprimir.setEnabled(b); }
    public void lblEstadoVale_setIcon(ImageIcon i)   { lblEstadoVale.setIcon(i); }
    public void lblHora_setText(String v)            { lblMensaje1.setText(v); }
    public void lblMensaje_setForeground(Color c)    { lblMensaje.setForeground(c); }
    public void lblMensaje_setText(String v)         { lblMensaje.setText(v); }
    public void lblMensaje1_setText(String v)        { lblMensajex.setText(v); }
    public void lblMensajex_setText(String v)        { lblMensajex.setText(v); }
    public void setOcupado()                         { verestadoconexion.setText("Sincronizando... por favor esperar!"); }
    public void setLibre()                           { verestadoconexion.setText(""); }
    public void webServices(String m)                { verestadoconexion.setText(m); }
    public void sihayerror(String m)                 { verestadoconexion.setText(m); }
    public void mostrarConexion(String m)            { verestadoconexion.setText(m); }
    public void txtTitulo_setText(String m)          { verestadoconexion.setText(m); }

    // ── Hilo sincronización ───────────────────────────────────────────────────
    @Override
    public void run() {
        SwingUtilities.invokeLater(() -> {
            btnSincronizar.setText("Sincronizando...");
            verestadoconexion.setText("Sincronizando...");
        });
        final Sincronizar s = new Sincronizar();
        s.wsino_red = "SI";
        try {
            s.runManual();
            if (ingresoModelTarjeta != null)
                SwingUtilities.invokeLater(() -> ingresoModelTarjeta.refrescarIconoVales());
        } catch (Exception e) { }
        SwingUtilities.invokeLater(() -> {
            btnSincronizar.setText("Sincronizar");
            verestadoconexion.setText("NO".equals(s.wsino_red)
                ? "NO hay conexión de Red, Verificar!"
                : "Sincronización Finalizada!");
            if (s.wwsinconexion == 1)
                verestadoconexion.setText("Problemas en la Red!");
        });
        if (ramupdate != null && !Thread.currentThread().isInterrupted())
            ramupdate.interrupt();
    }

    // ── WindowListener ────────────────────────────────────────────────────────
    @Override public void windowOpened(WindowEvent e)      { }
    @Override public void windowClosing(WindowEvent e)     { }
    @Override public void windowClosed(WindowEvent e)      { }
    @Override public void windowIconified(WindowEvent e)   { }
    @Override public void windowDeiconified(WindowEvent e) { }
    @Override public void windowActivated(WindowEvent e)   { }
    @Override public void windowDeactivated(WindowEvent e) { }
}
