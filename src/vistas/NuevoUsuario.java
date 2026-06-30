package vistas;

import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.*;
import models.NuevoUsuarioModel;

public class NuevoUsuario extends javax.swing.JFrame implements WindowListener {

    // ── Colores ───────────────────────────────────────────────────────────────
    private static final Color NAVY      = new Color(0x1A, 0x27, 0x44);
    private static final Color NAVY_MID  = new Color(0x26, 0x38, 0x6B);
    private static final Color NAVY_DARK = new Color(0x0E, 0x17, 0x2D);
    private static final Color BG_PAGE   = new Color(0xEE, 0xF2, 0xF7);
    private static final Color BG_CARD   = Color.WHITE;
    private static final Color BG_FIELD  = new Color(0xF6, 0xF8, 0xFA);
    private static final Color BG_ACTIVA = new Color(0xE8, 0xED, 0xF7);
    private static final Color BORDER_C  = new Color(0xDC, 0xE5, 0xEF);
    private static final Color TXT_PRI   = new Color(0x1E, 0x29, 0x3B);
    private static final Color TXT_MUT   = new Color(0x94, 0xA3, 0xB8);
    private static final Color RED_BTN   = new Color(0xE5, 0x3E, 0x3E);
    private static final int   ARC       = 14;

    // ── Estado de huellas (índice 1-10) ───────────────────────────────────────
    private final boolean[] huellaActiva = new boolean[11];

    // ── Referencias a sub-componentes de cada slot ────────────────────────────
    private final JPanel[]  slotCard   = new JPanel[11];
    private final JPanel[]  slotIcon   = new JPanel[11];
    private final JLabel[]  slotBadge  = new JLabel[11];
    private final JButton[] slotAction = new JButton[11];

    // ── Campos de datos ───────────────────────────────────────────────────────
    JTextField txtcc, txtNombre, txtCargo, txtEmpresa;
    JTextField txtSede, txtDepartamento, txtCiudad;
    JTextField txtHoraIngreso, txtHoraSalida, txtFijo, txtCambio;

    // ── Labels estado huellas (usados por el modelo) ──────────────────────────
    JLabel lblHuella1, lblHuella2, lblHuella3, lblHuella4, lblHuella5;
    JLabel lblHuella6, lblHuella7, lblHuella8, lblHuella9, lblHuella10;

    // ── Botones funcionales (usados por el modelo / Captura) ──────────────────
    JButton btnBuscar;
    JButton btnHuella1, btnHuella2, btnHuella3, btnHuella4, btnHuella5;
    JButton btnHuella6, btnHuella7, btnHuella8, btnHuella9, btnHuella10;
    JButton btnBorrar1, btnBorrar2, btnBorrar3, btnBorrar4, btnBorrar5;
    JButton btnBorrar6, btnBorrar7, btnBorrar8, btnBorrar9, btnBorrar10;
    JButton btnGuardaryCerrar, btnGuardarContinuar, btnCancelar, btnImprimir;

    // ── Labels heredados (compatibilidad) ─────────────────────────────────────
    JLabel lblicon, jLabel1, jLabel2, jLabel3, jLabel4, jLabel5, jLabel6;
    JLabel jLabel7, jLabel8, jLabel9, jLabel11, jLabel12, jLabel13, jLabel14, jLabel17;
    JPanel jPanel1, jPanel2;

    // ── Contador footer ───────────────────────────────────────────────────────
    private JLabel lblContador;

    NuevoUsuarioModel nuevousuarioModel;

    // ─────────────────────────────────────────────────────────────────────────
    public NuevoUsuario() { }

    public NuevoUsuario(Inicio inicio) {
        initHuellaButtons();
        buildUI();
        nuevousuarioModel = new NuevoUsuarioModel(this, inicio);
        addWindowListener(this);
        try {
            setIconImage(new ImageIcon(Inicio.class.getResource("icon_frame.png")).getImage());
        } catch (Exception ignored) { }
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /** Inicializar botones funcionales (invisibles, usados por el modelo). */
    private void initHuellaButtons() {
        btnHuella1  = new JButton(); btnHuella2  = new JButton();
        btnHuella3  = new JButton(); btnHuella4  = new JButton();
        btnHuella5  = new JButton(); btnHuella6  = new JButton();
        btnHuella7  = new JButton(); btnHuella8  = new JButton();
        btnHuella9  = new JButton(); btnHuella10 = new JButton();

        btnBorrar1  = new JButton(); btnBorrar2  = new JButton();
        btnBorrar3  = new JButton(); btnBorrar4  = new JButton();
        btnBorrar5  = new JButton(); btnBorrar6  = new JButton();
        btnBorrar7  = new JButton(); btnBorrar8  = new JButton();
        btnBorrar9  = new JButton(); btnBorrar10 = new JButton();

        lblHuella1  = new JLabel("[Vacio]"); lblHuella2  = new JLabel("[Vacio]");
        lblHuella3  = new JLabel("[Vacio]"); lblHuella4  = new JLabel("[Vacio]");
        lblHuella5  = new JLabel("[Vacio]"); lblHuella6  = new JLabel("[Vacio]");
        lblHuella7  = new JLabel("[Vacio]"); lblHuella8  = new JLabel("[Vacio]");
        lblHuella9  = new JLabel("[Vacio]"); lblHuella10 = new JLabel("[Vacio]");

        // conectar botones enrolar
        for (int i = 1; i <= 10; i++) {
            final int n = i;
            getHuellaBtn(n).addActionListener(e -> {
                Captura c = new Captura(NuevoUsuario.this, n);
                c.setVisible(true);
                NuevoUsuario.this.setVisible(false);
            });
            getBorrarBtn(n).addActionListener(e -> {
                enableGuardar();
                try { nuevousuarioModel.captura_h.put(n, ""); } catch (Exception ex) { }
                huellaActiva[n] = false;
                getHuellaLabel(n).setText("[Vacio]");
                refreshSlot(n);
                actualizarContador();
            });
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    private void buildUI() {
        setTitle("Registro de nuevas huellas");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);
        setSize(880, 630);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_PAGE);
        setContentPane(root);

        JPanel body = buildBody();
        JScrollPane scroll = new JScrollPane(body,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BG_PAGE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        root.add(buildTopbar(), BorderLayout.NORTH);
        root.add(scroll,        BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        jPanel1 = new JPanel();
        jPanel2 = new JPanel();
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
        bar.setPreferredSize(new Dimension(0, 54));
        bar.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 18));

        lblicon = new JLabel();
        try {
            ImageIcon raw = new ImageIcon(getClass().getResource("/vistas/logop.png"));
            Image sc = raw.getImage().getScaledInstance(-1, 34, Image.SCALE_SMOOTH);
            lblicon.setIcon(new ImageIcon(sc));
        } catch (Exception ex) {
            lblicon.setText("CORSERPARK");
            lblicon.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblicon.setForeground(Color.WHITE);
        }
        bar.add(lblicon, BorderLayout.WEST);

        jLabel1 = new JLabel("Enrolamiento biométrico");
        jLabel1.setFont(new Font("Segoe UI", Font.BOLD, 14));
        jLabel1.setForeground(new Color(0xA0, 0xAE, 0xC4));
        bar.add(jLabel1, BorderLayout.EAST);

        return bar;
    }

    // ── Cuerpo ────────────────────────────────────────────────────────────────
    private JPanel buildBody() {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(14, 16, 4, 16));
        body.add(buildDatosCard());
        body.add(Box.createVerticalStrut(10));
        body.add(buildHuellasCard());
        return body;
    }

    // ── Card datos ────────────────────────────────────────────────────────────
    private JPanel buildDatosCard() {
        JPanel card = makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        card.add(makeSmallLabel("DATOS DEL USUARIO"));
        card.add(Box.createVerticalStrut(10));

        // Fila CC + Buscar
        txtcc = makeField(true);
        txtcc.addActionListener(e -> nuevousuarioModel.buscar());
        btnBuscar = makeNavyButton("Buscar");
        btnBuscar.addActionListener(e -> nuevousuarioModel.buscar());

        JPanel rowCC = new JPanel(new BorderLayout(10, 0));
        rowCC.setOpaque(false);
        rowCC.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JPanel ccLeft = new JPanel(new BorderLayout(0, 3));
        ccLeft.setOpaque(false);
        ccLeft.add(makeSmallLabel("N° IDENTIFICACIÓN"), BorderLayout.NORTH);
        ccLeft.add(txtcc, BorderLayout.CENTER);
        rowCC.add(ccLeft, BorderLayout.CENTER);
        rowCC.add(btnBuscar, BorderLayout.EAST);
        card.add(rowCC);
        card.add(Box.createVerticalStrut(4));
        card.add(new JSeparator());
        card.add(Box.createVerticalStrut(6));

        // Fila 3 cols: Nombre / Cargo / Empresa
        txtNombre  = makeField(false);
        txtCargo   = makeField(false);
        txtEmpresa = makeField(false);
        card.add(makeRow3("NOMBRE", txtNombre, "CARGO", txtCargo, "EMPRESA", txtEmpresa));
        card.add(Box.createVerticalStrut(6));

        // Fila 3 cols: Sede / Departamento / Ciudad
        txtSede         = makeField(false);
        txtDepartamento = makeField(false);
        txtCiudad       = makeField(false);
        card.add(makeRow3("SEDE", txtSede, "DEPARTAMENTO", txtDepartamento, "CIUDAD", txtCiudad));
        card.add(Box.createVerticalStrut(6));

        // Fila 4 cols: Hr.Ingreso / Hr.Salida / Fijo / Último cambio
        txtHoraIngreso = makeField(false);
        txtHoraSalida  = makeField(false);
        txtFijo        = makeField(false);
        txtCambio      = makeField(false);
        card.add(makeRow4("HR. INGRESO", txtHoraIngreso, "HR. SALIDA", txtHoraSalida,
                          "FIJO", txtFijo, "ÚLTIMO CAMBIO", txtCambio));

        return card;
    }

    private JPanel makeRow3(String l1, JTextField f1, String l2, JTextField f2,
                            String l3, JTextField f3) {
        JPanel row = new JPanel(new GridLayout(1, 3, 8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        row.add(makeLabeledField(l1, f1));
        row.add(makeLabeledField(l2, f2));
        row.add(makeLabeledField(l3, f3));
        return row;
    }

    private JPanel makeRow4(String l1, JTextField f1, String l2, JTextField f2,
                            String l3, JTextField f3, String l4, JTextField f4) {
        JPanel row = new JPanel(new GridLayout(1, 4, 8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        row.add(makeLabeledField(l1, f1));
        row.add(makeLabeledField(l2, f2));
        row.add(makeLabeledField(l3, f3));
        row.add(makeLabeledField(l4, f4));
        return row;
    }

    private JPanel makeLabeledField(String label, JTextField field) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);
        p.add(makeSmallLabel(label), BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    // ── Card huellas ──────────────────────────────────────────────────────────
    private JPanel buildHuellasCard() {
        JPanel card = makeCard();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JLabel titulo = makeSmallLabel("HUELLAS DIGITALES");
        titulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        card.add(titulo, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 5, 8, 8));
        grid.setOpaque(false);
        for (int i = 1; i <= 10; i++) {
            grid.add(buildSlot(i));
        }
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildSlot(final int n) {
        // Card contenedor
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(huellaActiva[n] ? BG_ACTIVA : BG_FIELD);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (huellaActiva[n]) {
                    g2.setColor(NAVY);
                    g2.setStroke(new BasicStroke(1.8f));
                    g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 12, 12);
                } else {
                    g2.setColor(BORDER_C);
                    float[] dash = {5f, 3f};
                    g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_MITER, 10f, dash, 0f));
                    g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 12, 12);
                }
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(10, 8, 8, 8));
        slotCard[n] = card;

        // Ícono huella
        JPanel icon = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = huellaActiva[n] ? NAVY : new Color(0xC4, 0xD0, 0xE4);
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                if (huellaActiva[n]) {
                    g2.setColor(c);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawOval(cx-16, cy-18, 32, 36);
                    g2.setStroke(new BasicStroke(1.6f));
                    g2.drawOval(cx-10, cy-11, 20, 23);
                    g2.setStroke(new BasicStroke(1.4f));
                    g2.drawOval(cx-5,  cy-5,  10, 12);
                    g2.fillOval(cx-2,  cy-1,   4,  5);
                } else {
                    g2.setColor(c);
                    float[] dash = {4f, 3f};
                    BasicStroke ds = new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_MITER, 10f, dash, 0f);
                    g2.setStroke(ds);
                    g2.drawOval(cx-16, cy-18, 32, 36);
                    g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_MITER, 10f, dash, 0f));
                    g2.drawOval(cx-10, cy-11, 20, 23);
                }
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(48, 52); }
            @Override public Dimension getMinimumSize()   { return getPreferredSize(); }
        };
        icon.setOpaque(false);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        slotIcon[n] = icon;

        // Nombre
        JLabel nombre = new JLabel("Huella " + n, SwingConstants.CENTER);
        nombre.setFont(new Font("Segoe UI", Font.BOLD, 11));
        nombre.setForeground(TXT_PRI);
        nombre.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Badge estado
        JLabel badge = new JLabel("vacía", SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(huellaActiva[n] ? NAVY : new Color(0xE8, 0xEE, 0xF6));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(new Font("Segoe UI", Font.BOLD, 9));
        badge.setForeground(huellaActiva[n] ? Color.WHITE : TXT_MUT);
        badge.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        badge.setOpaque(false);
        badge.setAlignmentX(Component.CENTER_ALIGNMENT);
        slotBadge[n] = badge;

        // Botón acción
        JButton accion = new JButton(huellaActiva[n] ? "✕ quitar" : "+ enrolar");
        accion.setFont(new Font("Segoe UI", Font.BOLD, 10));
        accion.setForeground(huellaActiva[n] ? RED_BTN : NAVY);
        accion.setContentAreaFilled(false);
        accion.setBorderPainted(false);
        accion.setFocusPainted(false);
        accion.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        accion.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        accion.setAlignmentX(Component.CENTER_ALIGNMENT);
        accion.addActionListener(e -> {
            if (huellaActiva[n]) {
                getBorrarBtn(n).doClick();
            } else {
                getHuellaBtn(n).doClick();
            }
        });
        slotAction[n] = accion;

        card.add(icon);
        card.add(Box.createVerticalStrut(4));
        card.add(nombre);
        card.add(Box.createVerticalStrut(4));
        card.add(badge);
        card.add(Box.createVerticalStrut(2));
        card.add(accion);

        return card;
    }

    /** Repinta un slot según huellaActiva[n]. */
    private void refreshSlot(int n) {
        if (n < 1 || n > 10) return;
        JLabel badge = slotBadge[n];
        if (badge != null) {
            badge.setText(huellaActiva[n] ? "ACTIVA" : "vacía");
            badge.setForeground(huellaActiva[n] ? Color.WHITE : TXT_MUT);
        }
        JButton btn = slotAction[n];
        if (btn != null) {
            btn.setText(huellaActiva[n] ? "✕ quitar" : "+ enrolar");
            btn.setForeground(huellaActiva[n] ? RED_BTN : NAVY);
        }
        if (slotCard[n] != null) slotCard[n].repaint();
        if (slotIcon[n] != null) slotIcon[n].repaint();
    }

    private void actualizarContador() {
        if (lblContador == null) return;
        int activas = 0;
        for (int i = 1; i <= 10; i++) if (huellaActiva[i]) activas++;
        lblContador.setText(activas + " huellas registradas · " + (10 - activas) + " vacías");
    }

    // ── Footer ────────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(4, 18, 14, 18));

        lblContador = new JLabel("0 huellas registradas · 10 vacías");
        lblContador.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblContador.setForeground(TXT_MUT);
        footer.add(lblContador, BorderLayout.WEST);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);

        btnCancelar = makeGrayButton("Cancelar");
        btnCancelar.addActionListener(e -> {
            setVisible(false);
            nuevousuarioModel.inicio.setVisible(true);
            dispose();
        });

        btnImprimir = makeOutlineButton("Imprimir vale");
        btnImprimir.addActionListener(e -> nuevousuarioModel.imprimirValeManual());

        btnGuardarContinuar = makeNavyButton("Guardar y continuar");
        btnGuardarContinuar.addActionListener(e -> {
            if (nuevousuarioModel.Guardar()) {
                setVisible(false);
                NuevoUsuario ns = new NuevoUsuario(nuevousuarioModel.inicio);
                ns.setVisible(true);
                dispose();
            }
        });

        btnGuardaryCerrar = makeDarkNavyButton("Guardar y cerrar");
        btnGuardaryCerrar.addActionListener(e -> {
            if (nuevousuarioModel.Guardar()) {
                nuevousuarioModel.inicio.setVisible(true);
                dispose();
            }
        });

        btns.add(btnCancelar);
        btns.add(btnImprimir);
        btns.add(btnGuardarContinuar);
        btns.add(btnGuardaryCerrar);
        footer.add(btns, BorderLayout.EAST);
        return footer;
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────
    private JPanel makeCard() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 12));
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
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return card;
    }

    private JTextField makeField(boolean editable) {
        JTextField f = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_FIELD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        f.setEditable(editable);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        f.setForeground(TXT_PRI);
        f.setBackground(BG_FIELD);
        f.setOpaque(false);
        f.setMinimumSize(new Dimension(60, 28));
        f.setPreferredSize(new Dimension(160, 28));
        f.setBorder(new CompoundBorder(
            new RoundedBorder(BORDER_C, 8),
            BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        return f;
    }

    private JLabel makeSmallLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        l.setForeground(TXT_MUT);
        return l;
    }

    private JButton makeNavyButton(String txt) {
        JButton b = new JButton(txt) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? NAVY_MID : NAVY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        styleBtn(b, Color.WHITE, Font.BOLD);
        return b;
    }

    private JButton makeDarkNavyButton(String txt) {
        JButton b = new JButton(txt) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? NAVY : NAVY_DARK);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        styleBtn(b, Color.WHITE, Font.BOLD);
        return b;
    }

    private JButton makeGrayButton(String txt) {
        JButton b = new JButton(txt) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? BORDER_C : BG_FIELD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(BORDER_C);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        styleBtn(b, TXT_PRI, Font.PLAIN);
        return b;
    }

    private JButton makeOutlineButton(String txt) {
        JButton b = new JButton(txt) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? BG_PAGE : BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(NAVY);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        styleBtn(b, NAVY, Font.BOLD);
        return b;
    }

    private void styleBtn(JButton b, Color fg, int style) {
        b.setFont(new Font("Segoe UI", style, 12));
        b.setForeground(fg);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(7, 20, 7, 20));
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

    // ── Acceso a botones por índice ───────────────────────────────────────────
    private JButton getHuellaBtn(int n) {
        switch (n) {
            case 1:  return btnHuella1;  case 2:  return btnHuella2;
            case 3:  return btnHuella3;  case 4:  return btnHuella4;
            case 5:  return btnHuella5;  case 6:  return btnHuella6;
            case 7:  return btnHuella7;  case 8:  return btnHuella8;
            case 9:  return btnHuella9;  case 10: return btnHuella10;
            default: return new JButton();
        }
    }

    private JButton getBorrarBtn(int n) {
        switch (n) {
            case 1:  return btnBorrar1;  case 2:  return btnBorrar2;
            case 3:  return btnBorrar3;  case 4:  return btnBorrar4;
            case 5:  return btnBorrar5;  case 6:  return btnBorrar6;
            case 7:  return btnBorrar7;  case 8:  return btnBorrar8;
            case 9:  return btnBorrar9;  case 10: return btnBorrar10;
            default: return new JButton();
        }
    }

    JLabel getHuellaLabel(int n) {
        switch (n) {
            case 1:  return lblHuella1;  case 2:  return lblHuella2;
            case 3:  return lblHuella3;  case 4:  return lblHuella4;
            case 5:  return lblHuella5;  case 6:  return lblHuella6;
            case 7:  return lblHuella7;  case 8:  return lblHuella8;
            case 9:  return lblHuella9;  case 10: return lblHuella10;
            default: return null;
        }
    }

    // ── API pública requerida por NuevoUsuarioModel / Captura ─────────────────
    public String txtcc_getText()            { return txtcc.getText(); }
    public String txtSede_getText()          { return txtSede.getText(); }
    public String txtNombre_getText()        { return txtNombre.getText(); }
    public String txtDepartamento_getText()  { return txtDepartamento.getText(); }
    public String txtEmpresa_getText()       { return txtEmpresa.getText(); }

    public void txtNombre_setText(String s)      { txtNombre.setText(s); }
    public void txtDepartamento_setText(String s){ txtDepartamento.setText(s); }
    public void txtSede_setText(String s)        { txtSede.setText(s); }
    public void txtCiudad_setText(String s)      { txtCiudad.setText(s); }
    public void txtHoraSalida_setText(String s)  { txtHoraSalida.setText(s); }
    public void txtEmpresa_setText(String s)     { txtEmpresa.setText(s); }
    public void txtHoraIngreso_setText(String s) { txtHoraIngreso.setText(s); }
    public void txtCargo_setText(String s)       { txtCargo.setText(s); }
    public void txtFijo_setText(String s)        { txtFijo.setText(s); }
    public void txtCambio_setText(String s)      { txtCambio.setText(s); }

    public void setHuella(int n, String ss) {
        nuevousuarioModel.captura_h.put(n, ss);
        enableGuardar();
        huellaActiva[n] = true;
        getHuellaLabel(n).setText("Pendiente por guardar");
        refreshSlot(n);
        actualizarContador();
        Iterator it = nuevousuarioModel.captura_h.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());
        }
    }

    public void vaciarHuella() {
        for (int i = 1; i <= 10; i++) {
            huellaActiva[i] = false;
            getHuellaLabel(i).setText("[vacio]");
            refreshSlot(i);
        }
        nuevousuarioModel.captura_h = new HashMap<Integer, String>();
        actualizarContador();
    }

    public void validarHuellasGet(HashMap<String, String> res) {
        for (int i = 1; i <= 10; i++) {
            String key = "h" + i;
            if (res.containsKey(key) && res.get(key).length() != 0) {
                nuevousuarioModel.captura_h.put(i, res.get(key));
                huellaActiva[i] = true;
                getHuellaLabel(i).setText("[Usado]");
                refreshSlot(i);
            }
        }
        actualizarContador();
    }

    public void disableHuellas() {
        for (int i = 1; i <= 10; i++) getHuellaBtn(i).setEnabled(false);
    }

    public void enableHuellas() {
        for (int i = 1; i <= 10; i++) getHuellaBtn(i).setEnabled(true);
    }

    public void disableGuardar() {
        if (btnGuardarContinuar != null) btnGuardarContinuar.setEnabled(false);
        if (btnGuardaryCerrar  != null) btnGuardaryCerrar.setEnabled(false);
    }

    public void enableGuardar() {
        if (btnGuardarContinuar != null) btnGuardarContinuar.setEnabled(true);
        if (btnGuardaryCerrar  != null) btnGuardaryCerrar.setEnabled(true);
    }

    // ── WindowListener ────────────────────────────────────────────────────────
    @Override public void windowClosing(WindowEvent e) {
        nuevousuarioModel.inicio.setVisible(true);
        dispose();
    }
    @Override public void windowOpened(WindowEvent e)      { }
    @Override public void windowClosed(WindowEvent e)      { }
    @Override public void windowIconified(WindowEvent e)   { }
    @Override public void windowDeiconified(WindowEvent e) { }
    @Override public void windowActivated(WindowEvent e)   { }
    @Override public void windowDeactivated(WindowEvent e) { }
}
