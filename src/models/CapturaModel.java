package models;

import com.digitalpersona.onetouch.DPFPDataPurpose;
import com.digitalpersona.onetouch.DPFPFeatureSet;
import com.digitalpersona.onetouch.DPFPGlobal;
import com.digitalpersona.onetouch.DPFPSample;
import com.digitalpersona.onetouch.DPFPTemplate;
import com.digitalpersona.onetouch.capture.DPFPCapture;
import com.digitalpersona.onetouch.capture.event.DPFPDataAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPDataEvent;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusEvent;
import com.digitalpersona.onetouch.capture.event.DPFPSensorAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPSensorEvent;
import com.digitalpersona.onetouch.processing.DPFPEnrollment;
import com.digitalpersona.onetouch.processing.DPFPFeatureExtraction;
import com.digitalpersona.onetouch.processing.DPFPImageQualityException;
import com.digitalpersona.onetouch.verification.DPFPVerification;

import javax.swing.*;
import java.awt.*;
import java.util.Base64;

import vistas.Captura;
import vistas.NuevoUsuario;

/**
 * Clase encargada de la lógica del proceso de captura de nuevas huellas.
 */
public class CapturaModel extends JFrame {

    // --- Dependencias de la vista ---
    private final Captura vista;
    // Hacer público para ser compatible con vistas.Captura (accede a capturaModel.ns)
    public final NuevoUsuario ns;

    // --- SDK DigitalPersona ---
    public final DPFPCapture Lector = DPFPGlobal.getCaptureFactory().createCapture();
    public final DPFPEnrollment Reclutador = DPFPGlobal.getEnrollmentFactory().createEnrollment();
    public final DPFPVerification Verificador = DPFPGlobal.getVerificationFactory().createVerification();

    // --- Estado de captura ---
    public DPFPTemplate template;
    public static final String TEMPLATE_PROPERTY = "template";
    // ya era público y lo dejamos así porque vistas.Captura lo usa
    public int n_huella;

    public DPFPFeatureSet featuresinscripcion;
    public DPFPFeatureSet featuresverificacion;

    /**
     * @param vista     instancia de la vista de captura
     * @param ns        instancia de la vista de NuevoUsuario (donde se setean las huellas codificadas)
     * @param n_huella  número de huella que se está capturando (1..10)
     */
    public CapturaModel(Captura vista, NuevoUsuario ns, int n_huella) {
        if (vista == null) throw new IllegalArgumentException("vista no puede ser null");
        if (ns == null) throw new IllegalArgumentException("NuevoUsuario no puede ser null");
        if (n_huella <= 0) throw new IllegalArgumentException("n_huella debe ser > 0");

        this.vista = vista;
        this.ns = ns;
        this.n_huella = n_huella;

        initLector();
    }

    /**
     * Inicializa el lector y registra listeners.
     */
    private void initLector() {
        try {
            Lector.startCapture();
        } catch (Exception e) {
            vista.closeCapture("El dispositivo no está listo: " + e.getMessage());
            return;
        }

        Lector.addReaderStatusListener(new DPFPReaderStatusAdapter() {
            @Override
            public void readerConnected(final DPFPReaderStatusEvent e) {
                // opcional: estado en UI
            }

            @Override
            public void readerDisconnected(final DPFPReaderStatusEvent e) {
                SwingUtilities.invokeLater(() -> vista.closeCapture("El dispositivo no está listo"));
            }
        });

        Lector.addSensorListener(new DPFPSensorAdapter() {
            @Override
            public void fingerTouched(final DPFPSensorEvent e) {
                SwingUtilities.invokeLater(vista::pintarDedoPuesto);
            }

            @Override
            public void fingerGone(final DPFPSensorEvent e) {
                SwingUtilities.invokeLater(vista::pintarDedonoPuesto);
            }
        });

        Lector.addDataListener(new DPFPDataAdapter() {
            @Override
            public void dataAcquired(final DPFPDataEvent e) {
                SwingUtilities.invokeLater(() -> ProcesarCaptura(e.getSample()));
            }
        });
    }

    /**
     * Procesa una muestra capturada.
     */
    public void ProcesarCaptura(DPFPSample sample) {
        featuresinscripcion = extraerCaracteristicas(sample, DPFPDataPurpose.DATA_PURPOSE_ENROLLMENT);
        featuresverificacion = extraerCaracteristicas(sample, DPFPDataPurpose.DATA_PURPOSE_VERIFICATION);

        if (featuresinscripcion == null) {
            return; // calidad pobre, esperar otra captura
        }

        try {
            Reclutador.addFeatures(featuresinscripcion);

            Image image = DPFPGlobal.getSampleConversionFactory().createImage(sample);
            vista.DibujarHuella(image);

        } catch (DPFPImageQualityException ex) {
            System.err.println("Error calidad de imagen: " + ex.getMessage());
            return;
        } finally {
            vista.ContadorHuellas();
        }

        switch (Reclutador.getTemplateStatus()) {
            case TEMPLATE_STATUS_READY:
                DPFPTemplate old = this.template;
                this.template = Reclutador.getTemplate();
                firePropertyChange(TEMPLATE_PROPERTY, old, template);

                byte[] tplBytes = template.serialize();
                String tplBase64 = Base64.getEncoder().encodeToString(tplBytes);

                ns.setHuella(n_huella, tplBase64);

                try { Lector.stopCapture(); } catch (Exception ignore) { }

                SwingUtilities.invokeLater(() -> {
                    ns.setVisible(true);
                    setVisible(false);
                    dispose();
                });
                break;

            case TEMPLATE_STATUS_FAILED:
                vista.reiniciar("La huella no fue capturada, inténtelo de nuevo");
                break;

            default:
                // Espera más muestras
                break;
        }
    }

    /**
     * Extrae características de una muestra según el propósito.
     */
    public DPFPFeatureSet extraerCaracteristicas(DPFPSample sample, DPFPDataPurpose purpose) {
        try {
            DPFPFeatureExtraction extractor =
                    DPFPGlobal.getFeatureExtractionFactory().createFeatureExtraction();
            return extractor.createFeatureSet(sample, purpose);
        } catch (DPFPImageQualityException e) {
            return null;
        }
    }

    /**
     * Setter del template para PropertyChangeListeners.
     */
    public void setTemplate(DPFPTemplate template) {
        DPFPTemplate old = this.template;
        this.template = template;
        firePropertyChange(TEMPLATE_PROPERTY, old, template);
    }

    /**
     * Detiene la captura.
     */
    public void stopCaptureM() {
        try { Lector.stopCapture(); } catch (Exception ignore) { }
    }

    /**
     * Cierra captura y vuelve a la vista de NuevoUsuario.
     */
    public void closeCaptureM() {
        if (ns != null) {
            SwingUtilities.invokeLater(() -> ns.setVisible(true));
        }
        try { Lector.stopCapture(); } catch (Exception ignore) { }
        SwingUtilities.invokeLater(() -> {
            setVisible(false);
            dispose();
        });
    }
}
