package devices;

import javax.swing.*;
import java.awt.*;

/** Toast no modal, centrado (o en posiciones comunes). */
public final class Toast {
    private Toast() {}

    public enum Position { CENTER, TOP, BOTTOM, TOP_RIGHT, BOTTOM_RIGHT }

    public static void show(Window owner, String message, int millis, Position pos) {
        if (owner == null) return;
        final JWindow w = new JWindow(owner);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        panel.setBackground(new Color(0, 0, 0, 200));

        JLabel label = new JLabel(message);
        label.setForeground(Color.WHITE);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 16f));
        panel.add(label);

        w.setBackground(new Color(0, 0, 0, 0));
        w.getContentPane().add(panel);
        w.pack();

        // Posición
        Rectangle b = owner.getBounds();
        Dimension d = w.getSize();
        int x = b.x + (b.width - d.width) / 2;
        int y = b.y + (b.height - d.height) / 2;

        switch (pos == null ? Position.CENTER : pos) {
            case TOP:           y = b.y + 40; break;
            case BOTTOM:        y = b.y + b.height - d.height - 40; break;
            case TOP_RIGHT:     x = b.x + b.width - d.width - 40; y = b.y + 40; break;
            case BOTTOM_RIGHT:  x = b.x + b.width - d.width - 40; y = b.y + b.height - d.height - 40; break;
            case CENTER:
            default:            break;
        }

        w.setLocation(x, y);
        w.setAlwaysOnTop(true);
        w.setVisible(true);

        final int delay = Math.max(1000, millis);
        final Timer t = new Timer(delay, new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                w.setVisible(false);
                w.dispose();
            }
        });
        t.setRepeats(false);
        t.start();
    }
}
