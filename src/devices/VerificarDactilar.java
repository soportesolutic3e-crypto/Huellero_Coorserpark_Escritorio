package devices;

import com.digitalpersona.onetouch.capture.DPFPCapture;
import com.digitalpersona.onetouch.DPFPGlobal;

/** Chequeo y monitoreo del lector DigitalPersona. */
public final class VerificarDactilar {

    public enum Estado {
        LECTOR_LISTO,
        SIN_LECTORES,
        LECTOR_OCUPADO,
        SDK_NO_DISPONIBLE,
        ERROR
    }

    public static final class DactilarStatus {
        public final Estado estado;
        public final String detalle;
        public DactilarStatus(Estado e, String d) { this.estado = e; this.detalle = d; }
        @Override public String toString() { return estado + (detalle != null ? (" - " + detalle) : ""); }
    }

    public interface StatusListener {
        void onStatus(DactilarStatus status);
    }

    private VerificarDactilar() {}

    // ===== Chequeo puntual =====
    public static DactilarStatus check() {
        try {
            DPFPCapture cap = DPFPGlobal.getCaptureFactory().createCapture();
            if (cap == null) {
                return new DactilarStatus(Estado.SIN_LECTORES, "No se pudo crear el dispositivo");
            }
            try {
                cap.startCapture();
                cap.stopCapture();
            } catch (Throwable t) {
                // Si lanzar start/stop falla, asumimos que el lector no está listo/ocupado
                String msg = t.getMessage() == null ? t.toString() : t.getMessage();
                return new DactilarStatus(Estado.LECTOR_OCUPADO, "No fue posible iniciar captura: " + msg);
            }
            return new DactilarStatus(Estado.LECTOR_LISTO, "Dispositivo OK");
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            return new DactilarStatus(Estado.SDK_NO_DISPONIBLE, "SDK/driver DigitalPersona no disponible");
        } catch (Throwable t) {
            String msg = t.getMessage() == null ? t.toString() : t.getMessage();
            return new DactilarStatus(Estado.ERROR, msg);
        }
    }

    // ===== Monitor =====
    private static java.util.Timer monitorTimer;
    private static StatusListener monitorListener;

    /** Inicia el monitor con período en ms y callback. */
    public static synchronized void startMonitor(long periodMillis, StatusListener listener) {
        stopMonitor();
        monitorListener = listener;

        monitorTimer = new java.util.Timer("DactilarMonitor", true);
        monitorTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override public void run() {
                DactilarStatus st = check();
                StatusListener l = monitorListener;
                if (l != null) {
                    try { l.onStatus(st); } catch (Throwable ignore) {}
                }
            }
        }, 0L, Math.max(periodMillis, 1000L));
    }

    /** Overload legacy (solo período). Puedes ignorarlo si no lo usas. */
    public static synchronized void startMonitor(long periodMillis) {
        startMonitor(periodMillis, null);
    }

    public static synchronized void stopMonitor() {
        monitorListener = null;
        if (monitorTimer != null) {
            try { monitorTimer.cancel(); } catch (Throwable ignore) {}
            monitorTimer = null;
        }
    }
}
