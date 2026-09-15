package process;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.AbstractBorder;

/**
 * Diálogo que muestra el detalle de fallo de licencia.
 * Mismo estilo visual navy del resto de la app.
 */
public class LicenciaErrorDialog extends JDialog {

    private static final Color NAVY     = new Color(0x1A, 0x27, 0x44);
    private static final Color BG_PAGE  = new Color(0xEE, 0xF2, 0xF7);
    private static final Color BG_CARD  = Color.WHITE;
    private static final Color BORDER_C = new Color(0xDC, 0xE5, 0xEF);
    private static final Color TXT_PRI  = new Color(0x1E, 0x29, 0x3B);
    private static final Color TXT_MUT  = new Color(0x94, 0xA3, 0xB8);
    private static final Color OK_COLOR = new Color(0x16, 0xA3, 0x4A);
    private static final Color ER_COLOR = new Color(0xDC, 0x26, 0x26);

    public LicenciaErrorDialog(LicenciaManager.Resultado r) {
        super((Frame) null, "Error de licencia", true);
        buildUI(r);
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);
    }

    private void buildUI(LicenciaManager.Resultado r) {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        setSize(440, 350);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_PAGE);
        setContentPane(root);

        // Topbar
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(NAVY);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 52));
        bar.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));

        JLabel titulo = new JLabel("Error de licencia");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titulo.setForeground(Color.WHITE);
        bar.add(titulo, BorderLayout.WEST);

        JLabel code = new JLabel(r.codigo);
        code.setFont(new Font("Segoe UI Mono", Font.PLAIN, 12));
        code.setForeground(new Color(0xA0, 0xAE, 0xC4));
        bar.add(code, BorderLayout.EAST);

        root.add(bar, BorderLayout.NORTH);

        // Card central
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(14, 18, 4, 18));

        JPanel card = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 12));
                g2.fillRoundRect(2, 3, getWidth()-3, getHeight()-2, 16, 16);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth()-2, getHeight()-3, 14, 14);
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BORDER_C);
                g2.drawRoundRect(0, 0, getWidth()-3, getHeight()-4, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        // Mensaje principal
        g.gridx=0; g.gridy=0; g.gridwidth=2; g.insets=new Insets(0,0,14,0);
        String mensajePrincipal;
        switch (r.codigo) {
            case "LIC-004": mensajePrincipal = "La licencia de este equipo está BLOQUEADA."; break;
            case "LIC-005": mensajePrincipal = "La licencia de este equipo aún no ha sido activada."; break;
            case "LIC-006": mensajePrincipal = "La licencia no es válida. Contacte al administrador."; break;
            default:        mensajePrincipal = "No se pudo validar la licencia en este equipo."; break;
        }
        JLabel msg = new JLabel(mensajePrincipal);
        msg.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        msg.setForeground(TXT_PRI);
        card.add(msg, g);

        g.gridwidth=1;
        boolean esServidor = "LIC-004".equals(r.codigo) || "LIC-005".equals(r.codigo) || "LIC-006".equals(r.codigo);

        // Nota
        int filaNote = esServidor ? 1 : 5;
        g.gridx=0; g.gridy=filaNote; g.gridwidth=2; g.insets=new Insets(16,0,0,0);
        JLabel nota = new JLabel("Contacte al administrador del sistema.");
        nota.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        nota.setForeground(TXT_MUT);
        card.add(nota, g);

        GridBagConstraints wc = new GridBagConstraints();
        wc.fill = GridBagConstraints.BOTH;
        wc.weightx = 1; wc.weighty = 1;
        wrapper.add(card, wc);
        root.add(wrapper, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 10));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 18, 8, 18));

        JButton btnCerrar = new JButton("Cerrar") {
            @Override protected void paintComponent(Graphics g2) {
                Graphics2D gx = (Graphics2D) g2.create();
                gx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gx.setColor(NAVY);
                gx.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                gx.dispose();
                super.paintComponent(g2);
            }
        };
        btnCerrar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCerrar.setForeground(Color.WHITE);
        btnCerrar.setContentAreaFilled(false);
        btnCerrar.setBorderPainted(false);
        btnCerrar.setFocusPainted(false);
        btnCerrar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCerrar.setBorder(BorderFactory.createEmptyBorder(8, 28, 8, 28));
        btnCerrar.addActionListener(e -> { dispose(); System.exit(0); });
        footer.add(btnCerrar);
        root.add(footer, BorderLayout.SOUTH);
    }

    private void addFila(JPanel card, GridBagConstraints g, int row, String label, String estado) {
        g.gridx=0; g.gridy=row; g.weightx=0; g.insets=new Insets(4,0,4,16);
        JLabel lbl = new JLabel("• " + label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(TXT_PRI);
        card.add(lbl, g);

        g.gridx=1; g.gridy=row; g.weightx=1; g.insets=new Insets(4,0,4,0);
        boolean esOk = estado.equals("OK") || estado.equals("CREADO");
        JLabel val = new JLabel(estado);
        val.setFont(new Font("Segoe UI", Font.BOLD, 12));
        val.setForeground(esOk ? OK_COLOR : ER_COLOR);
        card.add(val, g);
    }
}
