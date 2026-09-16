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
import com.digitalpersona.onetouch.verification.DPFPVerificationResult;

import database.ConexionDb;
import process.JSONTask;
import process.Sincronizar;
import vistas.IngresoTarjeta;
import vistas.Inicio;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterJob;
import java.awt.print.*;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.DocAttributeSet;
import javax.print.attribute.HashDocAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.standard.PrinterName;
import javax.print.attribute.standard.PrinterResolution;
import javax.swing.SwingUtilities;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.export.JRPrintServiceExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimplePrintServiceExporterConfiguration;

// *** Integración del diagnóstico ***
import models.ValeDiagnostic;
import models.EscposValePrinter;

/**
 * Lógica de ingreso con Huella/Tarjeta y emisión de vales.
 * Versión saneada para robustez, rendimiento y mantenimiento.
 */
public final class IngresoModelTarjeta extends javax.swing.JFrame {

    // ===================== Constantes de eventos =====================
    private static final String EVT_INGRESO   = "Ingreso";
    private static final String EVT_SALIDA    = "Salida";
    private static final String EVT_VALE      = "Impresion-Vale";
    private static final String EVT_VALE_NOH  = "Imprimir-Vale-No-Horario";
    private static final String EVT_CAST8     = "Castigo-8";
    private static final String EVT_CAST30    = "Castigo-30";

    // ===================== Biometría DP =====================
    public DPFPCapture Lector = DPFPGlobal.getCaptureFactory().createCapture();
    public DPFPEnrollment Reclutador = DPFPGlobal.getEnrollmentFactory().createEnrollment();
    public DPFPVerification Verificador = DPFPGlobal.getVerificationFactory().createVerification();
    public DPFPTemplate template;
    public static String TEMPLATE_PROPERTY = "template";
    public DPFPFeatureSet featuresinscripcion;
    public DPFPFeatureSet featuresverificacion;

    // ===================== Estado / Variables de negocio =====================
    public int n_huella;
    public String mensaje_vale = "";
    public String entra_sale = "";
    public String valido_vale = "";
    public String valido_vacaciones = "";
    public String quesede;
    public String wrotativo_validar_turno = "";
    public String wfijo_genera_vale = "";
    public String tiene_ingreso = "";
    public String wnombres = "";
    public String wdepto = "";
    public String wcedula = "";
    public String wfecha = "";
    public String wderecho_vale = "";
    public String widpersona = "";
    public String wnro_vale = "";
    public Integer wnro_valex = 0;
    public String wfecha_castigo;
    public String wesfijo = "0";
    public String wh_ingreso = "";
    public String wh_salida = "";
    public String wtarjeta = "";
    public String wforma = "";
    public String wformaregistro = "";
    public String wwimpresion = "N";
    public String wminutosretardo = "";
    public String wmensaje1 = "";
    /* =========================================
   CONTROL LOCAL DE IMPRESIÓN DE VALES
    ========================================= */
    public String wimprimir_vales = "NO";
    private boolean wimprimirValesCacheCargado = false;

    public Inicio inicio;
    public int lblwidth;
    public int lblHeight;
    public Statement statement;

    public volatile ArrayList<HashMap<String, String>> empleados_cache = null;
    public volatile ArrayList<HashMap<String, String>> empleados = null;
    public volatile boolean leyendoEmpleados = true;

    public IngresoTarjeta vista;
    boolean is_adentro;

    // ===================== Constructor =====================
    public IngresoModelTarjeta(IngresoTarjeta vista, Inicio i) {
        if (vista == null || i == null) {
            throw new IllegalArgumentException("La vista o el inicio no pueden ser nulos.");
        }
        wformaregistro = Inicio.wformaregistropersonaini != null ? Inicio.wformaregistropersonaini : "H";
        this.inicio = i;
        this.vista = vista;

        queSede();
        queMensaje1();
        queImprimirVales();
        initLector();
        // ================= ICONO IMPRIMIR VALES =================
        vista.actualizarIconoVales(isImpresionValeActiva());
        lblwidth = vista.lblHuella_getWidth();
        lblHeight = vista.lblHuella_getHeight();
    }

    // ===================== Utilidades =====================
    public void setTemplate(DPFPTemplate template) {
        DPFPTemplate old = this.template;
        this.template = template;
        firePropertyChange(TEMPLATE_PROPERTY, old, template);
    }

    public DPFPFeatureSet extraerCaracteristicas(DPFPSample sample, DPFPDataPurpose purpose) {
        DPFPFeatureExtraction extractor = DPFPGlobal.getFeatureExtractionFactory().createFeatureExtraction();
        try {
            return extractor.createFeatureSet(sample, purpose);
        } catch (DPFPImageQualityException e) {
            return null;
        }
    }

    public byte[] concat(byte[] a, byte[] b) {
        byte[] c = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }
//************************************************************************************************
    private boolean isImpresionValeActiva() {
        try (ConexionDb db = new ConexionDb()) {
            db.Conectar();

            HashMap<String, String> row =
                    db.selectOne("SELECT valor FROM config WHERE dato='imprimir_vales'");

            String valor = (row != null && row.get("valor") != null)
                    ? row.get("valor").trim().toUpperCase()
                    : "NO";

            System.out.println("DEBUG imprimir_vales (DB) = " + valor);

            return "SI".equals(valor);

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    
    
//************************************************************************************************

    public void refrescarIconoVales() {
        vista.actualizarIconoVales(isImpresionValeActiva());
    }
    
//************************************************************************************************    
    private boolean validarImpresora() {
        return PrintServiceLookup.lookupDefaultPrintService() != null;
    }

    // ===================== Reglas: Castigo / Retardo =====================
    /**
  * =========================================================
  * MÉTODO: castigar(int hora, int minutos, int segundos)
  * ---------------------------------------------------------
  * OBJETIVO:
  * Validar si la hora real de ingreso supera la hora permitida
  * (turno o permiso) más la tolerancia definida en
  * wminutosretardo.
  *
  * PARÁMETROS:
  * - hora, minutos, segundos:
  *   Hora real de la marcación que se desea evaluar.
  *
  * LÓGICA:
  * 1. Construye la hora marcada real en formato HH:mm:ss
  * 2. Obtiene la hora límite desde vista.txtHIngreso_getText()
  * 3. Normaliza la hora programada
  * 4. Aplica tolerancia en minutos (wminutosretardo)
  * 5. Si la hora real supera la esperada + tolerancia:
  *      - valida si ya existe castigo hoy
  *      - si ya existe, NO registra otro
  *      - si no existe, registra evento de castigo en:
  *          * eventos_tmp
  *          * eventos_sitio
  *      - intenta enviarlo inmediatamente al servidor central
  *      - asigna mensaje_vale
  *      - retorna true
  *
  * RETORNO:
  * - true  -> se aplicó castigo / ya estaba castigado
  * - false -> no aplica castigo o hubo condición no válida
  *
  * NOTAS:
  * - Si ya existe un castigo hoy, no inserta otro
  * - Si falla el WebService, el castigo NO se revierte
  * - El evento queda localmente y con sync = 0 para que luego
  *   lo procese la sincronización automática
  * =========================================================
  */
 private boolean castigar(int hora, int minutos, int segundos) {

     System.out.println("Entramos a verificar Castigo");

     try {
         /* =====================================================
            1. FORMATEAR HORA REAL DE MARCACIÓN
         ===================================================== */
         DateFormat df = new SimpleDateFormat("HH:mm:ss");
         df.setLenient(false);

         String horaMarcada = String.format("%02d:%02d:%02d", hora, minutos, segundos);
         Date entrada = df.parse(horaMarcada);

         /* =====================================================
            2. OBTENER HORA LÍMITE (PERMISO O TURNO)
         ===================================================== */
         String horaProg = vista.txtHIngreso_getText();
         if (horaProg == null || horaProg.trim().isEmpty()) {
             return false;
         }

         /* =====================================================
            3. NORMALIZAR FORMATO DE HORA PROGRAMADA
         ===================================================== */
         String horaProgNormalizada;

         if (horaProg.length() == 5) {
             horaProgNormalizada = horaProg + ":00";
         } else if (horaProg.length() >= 8) {
             horaProgNormalizada = horaProg.substring(0, 8);
         } else {
             Logger.getLogger(IngresoTarjeta.class.getName()).log(
                     Level.WARNING,
                     "Formato de hora inválido en txtHIngreso: " + horaProg
             );
             return false;
         }

         Date esperada = df.parse(horaProgNormalizada);

         /* =====================================================
            4. APLICAR TOLERANCIA DE RETARDO
         ===================================================== */
         long baseRetardoMs = 0L;

         if (wminutosretardo != null && !wminutosretardo.trim().isEmpty()) {
            try {
                long segundosRetardo = Long.parseLong(wminutosretardo.trim());
                baseRetardoMs = segundosRetardo * 1000L; // 🔴 CAMBIO AQUÍ
            } catch (NumberFormatException ex) {
                Logger.getLogger(IngresoTarjeta.class.getName()).log(
                        Level.WARNING,
                        "Valor inválido en wminutosretardo: " + wminutosretardo,
                        ex
                );
            }
        }

         esperada = new Date(esperada.getTime() + baseRetardoMs);

         /* =====================================================
            5. COMPARAR HORA REAL VS HORA PERMITIDA + TOLERANCIA
         ===================================================== */
         if (entrada.after(esperada)) {

             try (ConexionDb n = new ConexionDb()) {
                 n.Conectar();

                 mensaje_vale = "Llegada Tarde - Retención de Vale";
                 String horaEntradaStr = new SimpleDateFormat("HH:mm:ss").format(entrada);
String horaPermitidaStr = new SimpleDateFormat("HH:mm:ss").format(esperada);

mensaje_vale = "Llegada tarde - Retención de Vale - Límite: " + horaPermitidaStr;
                 /* =================================================
                    5.1 VALIDAR SI YA EXISTE CASTIGO HOY
                    - Si ya existe, no registrar otro
                    - Pero sí retornar true porque ya está castigado
                 ================================================= */
                 DateFormat dfDia = new SimpleDateFormat("yyyy-MM-dd");
                 String hoy = dfDia.format(new Date());

                 String inicioHoy = hoy + " 00:00:00";
                 String finHoy = hoy + " 23:59:59";

                 String sqlExiste = "SELECT 1 FROM eventos_tmp "
                         + "WHERE cc = ? "
                         + "AND fecha >= ? AND fecha <= ? "
                         + "AND (evento = ? OR evento = ?) "
                         + "LIMIT 1";

                 try (PreparedStatement ps = n.getConnection().prepareStatement(sqlExiste)) {
                     ps.setString(1, vista.txtcc_getText());
                     ps.setString(2, inicioHoy);
                     ps.setString(3, finHoy);
                     ps.setString(4, EVT_CAST8);
                     ps.setString(5, EVT_CAST30);

                     if (ps.executeQuery().next()) {
                         System.out.println("[CASTIGO] Ya existe castigo hoy, no se registra otro.");
                         return true;
                     }
                 }

                 /* =================================================
                    5.2 ARMAR CAMPOS DEL EVENTO DE CASTIGO
                 ================================================= */
                 HashMap<String, String> campos = new HashMap<>();
                 campos.put("cc", vista.txtcc_getText());
                 campos.put("evento", EVT_CAST8);

                 DateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                 String fechaEvento = dateFormat2.format(new Date());
                 campos.put("fecha", fechaEvento);

                 /* =================================================
                    5.3 REGISTRO LOCAL DEL CASTIGO
                 ================================================= */
                 n.insertHashMap(campos, "eventos_tmp");
                 n.insertHashMap(campos, "eventos_sitio");

                 /* =================================================
                    5.4 ENVÍO INMEDIATO AL SERVIDOR CENTRAL
                    - Si falla, NO se revierte el castigo
                    - Queda pendiente sync = 0
                 ================================================= */
                 boolean enviado = enviarEventoServidorCentral(campos, fechaEvento);

                 if (!enviado) {
                     System.out.println("No se pudo enviar inmediatamente el CASTIGO al servidor central.");
                     System.out.println("El castigo queda registrado localmente para sincronización posterior.");
                 }

                 System.out.println("[CASTIGO] Aplicado castigo CAST8 para CC=" + campos.get("cc"));
                 return true;

             } catch (SQLException ex) {
                 Logger.getLogger(IngresoTarjeta.class.getName()).log(
                         Level.SEVERE,
                         "Error al registrar/verificar castigo en base de datos",
                         ex
                 );
             }
         }

         return false;

     } catch (ParseException ex) {
         Logger.getLogger(IngresoTarjeta.class.getName()).log(
                 Level.WARNING,
                 "Error al interpretar hora en castigar()",
                 ex
         );
         return false;
     }
 }
    
    // ===================== WebService auxiliares =====================
    public void setIngresoServidor() {
        ConexionDb Db = null;
        try {
            HashMap<String, String> parametros = new HashMap<>();
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String fecha2 = df.format(new Date());

            Db = new ConexionDb();
            Db.Conectar();
            HashMap<String, String> id_sede_obj = Db.selectOne("SELECT valor FROM config WHERE dato='id_sede' ");
            parametros.put("sede", id_sede_obj != null ? id_sede_obj.get("valor") : "1");
            parametros.put("fecha", fecha2);
            parametros.put("cc", vista.txtcc_getText());

            JSONTask t = new JSONTask("setIngreso", parametros);
            if (!t.enviar(false, this)) {
                System.out.print("++++NO HAY COMUNICACION CON EL SERVIDOR");
            }
        } catch (SQLException ignored) {
        } finally {
            try { if (Db != null) Db.close(); } catch (SQLException ignored) {}
        }
    }

    public void queSede() {
        try (ConexionDb db = new ConexionDb()) {
            db.Conectar();
            HashMap<String, String> idSede = db.selectOne("SELECT valor FROM config WHERE dato='id_sede'");
            String wsede = idSede != null ? idSede.get("valor") : "1";
            HashMap<String, String> row = db.selectOne("SELECT sede FROM sedes_tmp WHERE id_sede='" + wsede + "'");
            String nombre = (row != null && row.get("sede") != null) ? row.get("sede") : "";
            quesede = nombre;
            vista.lblMensaje1_setText(nombre);
        } catch (SQLException ignored) {}
    }

    public void queMensaje1() {
        try (ConexionDb db = new ConexionDb()) {
            db.Conectar();
            HashMap<String, String> row = db.selectOne("SELECT valor FROM config WHERE dato='mensaje1'");
            wmensaje1 = (row != null && row.get("valor") != null && !row.get("valor").trim().isEmpty())
                    ? " - " + row.get("valor")
                    : "";
        } catch (SQLException ignored) {}
    }
//*****************************************************************************************************************************
    
//*****************************************************************************************************************************    
    // ===================== Vale =====================
    /**
    * =========================================================
    * MÉTODO: isImpresoValidacion()
    * ---------------------------------------------------------
    * OBJETIVO:
    * Validar mediante WebService si el vale de almuerzo ya fue
    * impreso para el empleado (cc) en el día actual.
    *
    * FUNCIONAMIENTO:
    * - Envía petición al WebService "validarVale"
    * - Envía como parámetro el número de identificación (cc)
    * - Espera una respuesta tipo JSON convertida a HashMap
    *
    * RESPUESTA ESPERADA DEL WS:
    * {
    *   "impreso": "1"  → ya existe vale
    *   "impreso": "0"  → no existe vale
    * }
    *
    * VALORES DE RETORNO:
    * - true  → el vale YA fue impreso (bloquear impresión)
    * - false → el vale NO ha sido impreso (permitir continuar)
    * - null  → error en WebService (bloquear por seguridad)
    *
    * REGLA DE NEGOCIO:
    * - Si el WebService falla → NO se permite imprimir
    * - Solo se imprime si el WS responde explícitamente que
    *   el vale NO ha sido impreso
    *
    * DEPENDENCIAS:
    * - JSONTask (envío de petición HTTP)
    * - vista.txtcc_getText() (obtiene identificación actual)
    *
    * NOTAS:
    * - Este método NO realiza validaciones locales
    * - Solo consulta el estado en el sistema externo
    * =========================================================
    */
   public Boolean isImpresoValidacion() {

       try {
           // -----------------------------------------------------
           // 1. Construcción de parámetros para el WebService
           // -----------------------------------------------------
           HashMap<String, String> parametros = new HashMap<>();
           parametros.put("cc", vista.txtcc_getText());

           // -----------------------------------------------------
           // 2. Ejecución del WebService
           // -----------------------------------------------------
           // JSONTask t = new JSONTask("validarVale", parametros); /*VERSION V21 ORIGINAL*/
           
           JSONTask t = new JSONTask("validarVale", parametros, true); // V23-15-09-2026
           boolean ok = t.enviar(false, this);

           // -----------------------------------------------------
           // 3. Validación de comunicación
           // -----------------------------------------------------
           if (!ok) {
               System.out.println("WS validarVale: fallo en la comunicación.");
               return null; // ERROR → bloquear impresión
           }

           // -----------------------------------------------------
           // 4. Validación de respuesta
           // -----------------------------------------------------
           HashMap<String, String> m = t.respuesta;

           if (m == null) {
               System.out.println("WS validarVale: respuesta nula.");
               return null; // ERROR → bloquear impresión
           }

           // -----------------------------------------------------
           // 5. Lectura del campo "impreso"
           // -----------------------------------------------------
           String res = m.get("impreso");
           System.out.println("WS validarVale -> impreso = " + res);

           if (res == null) {
               return null; // ERROR → bloquear impresión
           }

           // -----------------------------------------------------
           // 6. Interpretación del resultado
           // -----------------------------------------------------
           return "1".equals(res.trim());

       } catch (Exception e) {
           // -----------------------------------------------------
           // 7. Manejo de excepciones
           // -----------------------------------------------------
           e.printStackTrace();
           return null; // ERROR → bloquear impresión
       }
   }

//*********************************************************************************************************************************    
    public void imprimirVale() {
        System.out.println("++++LLEGAMOS A IMPRIMIR VALE");
        vista.txtMensajeVale_setText("Imprimiendo...");
        vista.btnImprimir_setEnabled(false);

        PrintService printService = PrintServiceLookup.lookupDefaultPrintService();
        if (printService == null) {
            vista.txtMensajeVale_setText("No hay impresora por defecto configurada");
            return;
        }

        try {
            StringBuilder texto = new StringBuilder("\r\n");
            texto.append("Nombre: ").append(vista.txtNombre_getText()).append("\r\n")
                 .append("Departamento: ").append(vista.txtDepartamento_getText()).append("\r\n");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            texto.append("Fecha: ").append(df.format(new Date())).append("\r\n")
                 .append("Generado en: ").append(quesede).append("\r\n\r\n");

            byte[] espacio = {27, 100, 3};
            byte[] corte   = {27, 105, 3};
            byte[] vacio   = {};
            byte[] letra1  = {29, 33, 34};
            byte[] letra2  = {29, 33, 0};
            byte[] letra3  = {29, 33, 17};

            byte[] bytes = concat(vacio, letra1);
            bytes = concat(bytes, vista.txtEmpresa_getText().getBytes());
            bytes = concat(bytes, letra3);
            bytes = concat(bytes, "\r\nVale de almuerzo".getBytes());
            bytes = concat(bytes, letra2);
            bytes = concat(bytes, texto.toString().getBytes());
            bytes = concat(bytes, espacio);
            bytes = concat(bytes, espacio);
            bytes = concat(bytes, espacio);
            bytes = concat(bytes, corte);

            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            DocPrintJob job = printService.createPrintJob();
            DocAttributeSet das = new HashDocAttributeSet();
            das.add(new PrinterResolution(180, 180, PrinterResolution.DPI));
            Doc doc = new SimpleDoc(bytes, flavor, das);

            try {
                job.print(doc, null);
            } catch (PrintException e) {
                vista.txtMensajeVale_setText("Error de impresión: " + e.getMessage());
                return;
            }

            try (ConexionDb db = new ConexionDb()) {
                db.Conectar();
                HashMap<String, String> campos = new HashMap<>();
                campos.put("cc", vista.txtcc_getText());
                campos.put("evento", EVT_VALE);
                campos.put("fecha", df.format(new Date()));
                db.insertHashMap(campos, "eventos_tmp");
                db.insertHashMap(campos, "eventos_sitio");
            } catch (SQLException ex) {
                Logger.getLogger(IngresoTarjeta.class.getName()).log(Level.SEVERE, null, ex);
            }

        } catch (Exception e) {
            vista.txtMensajeVale_setText("Error al imprimir: " + e.getMessage());
        }
    }

    private void imprimirEspecialNoHorario(String tipo) {
        DateFormat d3 = new SimpleDateFormat("yyyy-MM-dd");
        String hoy = d3.format(new Date());

        try (ConexionDb db = new ConexionDb()) {
            db.Conectar();
            ArrayList<HashMap<String, String>> rep = db.selectAll(
                    "SELECT * FROM eventos_tmp WHERE CC = '" + vista.txtcc_getText() + "' " +
                            "AND fecha > '" + hoy + " 00:00:00' AND  fecha < '" + hoy + " 23:59:59' " +
                            "AND (evento ='" + EVT_VALE_NOH + "')");
            if (rep != null) {
                mensaje_vale = "El vale de hoy ya fue impreso - No Horario";
                vista.txtMensajeVale_setText(mensaje_vale);
                return;
            }
        } catch (SQLException ex) {
            Logger.getLogger(IngresoTarjeta.class.getName()).log(Level.SEVERE, null, ex);
        }

        imprimirVale();
    }

  /******************************************************************************************************************************/ 
    // ===================== Validaciones de para la generación del vale =====================
  /**
 * =========================================================
 * MÉTODO: validarVale()
 * ---------------------------------------------------------
 * OBJETIVO:
 * Validar si un empleado puede generar un vale de almuerzo.
 *
 * LÓGICA:
 * 1. Verifica si ya existe un vale registrado hoy en eventos_tmp
 * 2. Si no existe localmente, valida por WebService
 * 3. Si el WS falla → NO permite imprimir
 * 4. Solo permite imprimir si:
 *      - No existe en BD local
 *      - El WS responde que NO hay vale
 *
 * REGLAS ADICIONALES:
 * - Validación de castigos
 * - Validación de horarios / permisos
 * - Validación de tipo de empleado
 * - Validación de ingreso previo
 *
 * RESULTADO:
 * - true  → se puede imprimir vale
 * - false → NO se puede imprimir (mensaje en mensaje_vale)
 * =========================================================
 */
    private boolean validarVale() {

        System.out.println("LLEGAMOS A VALIDAR VALE");

        mensaje_vale = "";
        tiene_ingreso = "N";

        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = ahora.toLocalDate();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter dtfDateOnly = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        String inicioHoy = hoy.atStartOfDay().format(dtf);
        String finHoyExclusivo = hoy.plusDays(1).atStartOfDay().format(dtf);

        String cc = vista.txtcc_getText();

        try (ConexionDb db = new ConexionDb()) {

            db.Conectar();
            Connection conn = db.getConnection();

            /* =========================================================
               1. VALIDACIÓN LOCAL: ¿YA EXISTE VALE HOY?
            ========================================================= */
            if (existeEvento(conn, cc, EVT_VALE, inicioHoy, finHoyExclusivo)) {
                mensaje_vale = "El Vale de hoy ya fue Impreso";
                return false;
            }

            /* =========================================================
               2. VALIDACIÓN POR WEBSERVICE
               - Si falla → NO imprimir
               - Si existe → NO imprimir
            ========================================================= */
            Boolean respuestaWs = isImpresoValidacion();

            if (respuestaWs == null) {
                mensaje_vale = "Sin Validación Vale - NO conexión";
                return false;
            }

            if (respuestaWs) {
                mensaje_vale = "El Vale de hoy ya fue Impreso";
                return false;
            }

            /* =========================================================
               3. CASO ROTATIVO SIN VALIDACIÓN DE TURNO
            ========================================================= */
            if ("0".equals(wesfijo) && "N".equals(wrotativo_validar_turno)) {
                System.out.println("Rotativo sin validación de turno - Impresión directa");
                return true;
            }

            /* =========================================================
               4. VALIDACIÓN DE CASTIGOS
            ========================================================= */
            String sqlCastigo = "SELECT 1 FROM eventos_tmp " +
                    "WHERE CC = ? AND fecha >= ? AND fecha < ? " +
                    "AND (evento LIKE 'Castigo-%' OR evento = ? OR evento = ?) " +
                    "LIMIT 1";

            try (PreparedStatement ps = conn.prepareStatement(sqlCastigo)) {
                ps.setString(1, cc);
                ps.setString(2, inicioHoy);
                ps.setString(3, finHoyExclusivo);
                ps.setString(4, EVT_CAST8);
                ps.setString(5, EVT_CAST30);

                if (ps.executeQuery().next()) {
                    mensaje_vale = "Bloqueado por acumulados";
                    return false;
                }
            }

            /* =========================================================
               5. OBTENER HORA LÍMITE (PERMISO O TURNO)
            ========================================================= */
            LocalDateTime horaLimite = null;

            String sqlPermiso = "SELECT FECHA_FIN FROM permisos_tmp " +
                    "WHERE CC = ? AND FECHA_INICIO <= ? AND FECHA_FIN >= ? " +
                    "ORDER BY FECHA_FIN DESC LIMIT 1";

            try (PreparedStatement ps = conn.prepareStatement(sqlPermiso)) {
                ps.setString(1, cc);
                ps.setString(2, hoy.atTime(23, 59, 59).format(dtf));
                ps.setString(3, inicioHoy);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String fechaFin = rs.getString("FECHA_FIN");
                        if (fechaFin != null && !fechaFin.trim().isEmpty()) {
                            horaLimite = LocalDateTime.parse(fechaFin, dtf);
                            vista.txtHIngreso_setText(horaLimite.toLocalTime().format(timeFormatter));
                        }
                    }
                }
            }

            if (horaLimite == null) {
                String sqlTurno = "SELECT INICIO FROM horarios_tmp WHERE CC = ? AND FECHA = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlTurno)) {
                    ps.setString(1, cc);
                    ps.setString(2, hoy.format(dtfDateOnly));

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String inicioTurnoStr = rs.getString("INICIO");
                            if (inicioTurnoStr != null && !inicioTurnoStr.trim().isEmpty()) {
                                LocalTime inicioTurno = LocalTime.parse(inicioTurnoStr);
                                horaLimite = hoy.atTime(inicioTurno);
                                vista.txtHIngreso_setText(inicioTurno.format(timeFormatter));
                            }
                        }
                    }
                }
            }

            /* =========================================================
               6. OBTENER HORA REAL DE INGRESO
            ========================================================= */
            LocalDateTime horaIngresoReal = null;

            String sqlIngreso = "SELECT fecha FROM eventos_tmp " +
                    "WHERE CC = ? AND evento = ? AND fecha >= ? AND fecha < ? " +
                    "ORDER BY fecha ASC LIMIT 1";

            try (PreparedStatement ps = conn.prepareStatement(sqlIngreso)) {
                ps.setString(1, cc);
                ps.setString(2, EVT_INGRESO);
                ps.setString(3, inicioHoy);
                ps.setString(4, finHoyExclusivo);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        tiene_ingreso = "S";
                        horaIngresoReal = LocalDateTime.parse(rs.getString("fecha"), dtf);
                    }
                }
            }

            /* =========================================================
               7. VALIDAR LLEGADA TARDE (CASTIGO)
            ========================================================= */
            if ("S".equals(tiene_ingreso) && horaIngresoReal != null) {
                int h = horaIngresoReal.getHour();
                int m = horaIngresoReal.getMinute();
                int s = horaIngresoReal.getSecond();

                if (castigar(h, m, s)) {
                    //mensaje_vale = "Llegada tarde no se imprime vale";
                    return false;
                }
            }

        } catch (SQLException ex) {
            Logger.getLogger(IngresoTarjeta.class.getName()).log(Level.SEVERE, "Error en validarVale()", ex);
            return false;
        }

        /* =========================================================
           8. VALIDACIONES FINALES
        ========================================================= */
        if ("0".equals(valido_vale)) {
            mensaje_vale = "Persona sin Vale o no aplica para este día";
            return false;
        }

        if ("No".equals(vista.txtRotativo_getText())) {
            if ("M".equals(wfijo_genera_vale)) {
                int horaActual = ahora.getHour();
                if (horaActual < 11 || horaActual >= 14) {
                    mensaje_vale = "Los vales se generan entre las 11:00 y 13:59";
                    return false;
                }
            }

            if ("I".equals(wfijo_genera_vale) && is_adentro && "N".equals(tiene_ingreso)) {
                mensaje_vale = "El vale se genera en marcación de entrada";
                return false;
            }
        }

        if ("Si".equals(vista.txtRotativo_getText()) && is_adentro) {
            mensaje_vale = "Personal rotativo: vale solo al ingreso";
            return false;
        }

        return true;
    }

    // Método auxiliar: verifica si existe al menos un evento en el rango
    private boolean existeEvento(Connection conn, String cc, String evento, String inicio, String fin) throws SQLException {
         System.out.println("LLEGO A VERIFICAR EXISTENCIA DEL EVENTO: "+evento);
        String sql = "SELECT 1 FROM eventos_tmp WHERE CC = ? AND evento = ? AND fecha >= ? AND fecha < ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cc);
            ps.setString(2, evento);
            ps.setString(3, inicio);
            ps.setString(4, fin);
            return ps.executeQuery().next();
        }
    }
    
    
/*************************************************************************************************************************/
    
    // ===================== Captura / Lector =====================
    public void closeCapture() {
        if (Lector != null) {
            try {
                Lector.stopCapture();
                inicio.setVisible(true);
            } catch (Exception e) {
                System.err.println("Error al detener el lector: " + e.getMessage());
            }
        }
    }

    private void initLector() {
        try {
            Lector.startCapture();
        } catch (Exception e) {
            if (!"T".equals(wformaregistro)) {
                vista.closeCapture("El dispositivo NO está listo (1): " + e.getMessage());
                return;
            }
        }

        Lector.addReaderStatusListener(new DPFPReaderStatusAdapter() {
            @Override public void readerConnected(final DPFPReaderStatusEvent e) {
                SwingUtilities.invokeLater(() -> vista.EnviarTexto("Sensor Huella Digital Activado"));
            }
            @Override public void readerDisconnected(final DPFPReaderStatusEvent e) {
                SwingUtilities.invokeLater(() -> {
                    if (!"T".equals(wformaregistro)) {
                        vista.closeCapture("Dispositivo Huella NO listo (T)");
                        vista.EnviarTexto("Dispositivo Huella NO listo (T)");
                    }
                });
            }
        });

        Lector.addSensorListener(new DPFPSensorAdapter() {
            @Override public void fingerTouched(final DPFPSensorEvent e) {
                SwingUtilities.invokeLater(() -> {
                    vista.EnviarTexto("Dedo colocado sobre el Lector");
                    vista.pintarDedoPuesto();
                });
            }
            @Override public void fingerGone(final DPFPSensorEvent e) {
                SwingUtilities.invokeLater(() -> {
                    vista.EnviarTexto("Dedo retirado del Lector");
                    vista.pintarDedonoPuesto();
                });
            }
        });

        Lector.addDataListener(new DPFPDataAdapter() {
            @Override public void dataAcquired(final DPFPDataEvent e) {
                // 1) Dibuja huella en EDT
                SwingUtilities.invokeLater(() -> {
                    Image img = vista.CrearImagenHuella(e.getSample());
                    if (img != null) vista.DibujarHuella(img);
                });

                // 2) Procesa huella en segundo plano
                new Thread(() -> {
                    try {
                        featuresinscripcion = extraerCaracteristicas(e.getSample(), DPFPDataPurpose.DATA_PURPOSE_ENROLLMENT);
                        featuresverificacion = extraerCaracteristicas(e.getSample(), DPFPDataPurpose.DATA_PURPOSE_VERIFICATION);
                        if (featuresinscripcion == null || featuresverificacion == null) return;

                        SwingUtilities.invokeLater(() -> {
                            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                            vista.lblHora_setText(now);
                            vista.EnviarTexto("La Huella fue Capturada");
                        });

                        // Cargar empleados
                        try (ConexionDb db = new ConexionDb()) {
                            db.Conectar();
                            empleados = db.selectAll("SELECT * FROM empleado_tmp");
                        } catch (SQLException ex) {
                            Logger.getLogger(IngresoTarjeta.class.getName()).log(Level.SEVERE, null, ex);
                            empleados = null;
                        }

                        if (empleados == null || empleados.isEmpty()) {
                            noResultadosPersonasBd();
                            return;
                        }

                        boolean identificada = false;

                        // Comparar h1..h2 (ampliable)
                        for (int i = 1; i <= 2 && !identificada; i++) {
                            String col = "h" + i;
                            for (HashMap<String, String> emp : empleados) {
                                String hu = emp.get(col);
                                if (hu != null && hu.length() > 3) {
                                    try {
                                        byte[] decoded = Base64.getDecoder().decode(hu.getBytes());
                                        DPFPTemplate ref = DPFPGlobal.getTemplateFactory().createTemplate(decoded);
                                        setTemplate(ref);
                                        DPFPVerificationResult res = Verificador.verify(featuresverificacion, template);
                                        if (res.isVerified()) {
                                            wforma = "H";
                                            wcedula = emp.get("cc");
                                            mostrarResultados();
                                            identificada = true;
                                            break;
                                        }
                                    } catch (Exception ignore) {}
                                }
                            }
                        }

                        if (!identificada) {
                            noResultados();
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(IngresoModelTarjeta.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        SwingUtilities.invokeLater(() -> vista.pintarDedonoPuesto());
                    }
                }).start();
            }
        });
    }

    // ===================== RFID =====================
    public void validarTarjeta() throws SQLException {
        String raw = vista.nrotarje_getText();
        if (raw == null) raw = "";
        String card = raw.trim().replace("\r", "").replace("\n", "").replace("\t", "");

        if (card.isEmpty()) {
            vista.txtMensajeVale_setText("No se leyó ninguna tarjeta.");
            return;
        }

        vista.ImagenTarjeta();
        this.wtarjeta = card;

        try (ConexionDb db = new ConexionDb()) {
            db.Conectar();
            String safe = card.replace("'", "''");
            HashMap<String, String> row = db.selectOne(
                "SELECT * FROM empleado_tmp WHERE tarjeta = '" + safe + "'"
            );

            if (row != null) {
                this.wforma  = "T";
                this.wcedula = row.get("cc");
                vista.nrotarje_setText("");
                mostrarResultados();
            } else {
                noResultadosTarjeta();
            }
        } finally {
            vista.nrotarje_setText("");
        }
    }

    // ===================== Flujo de UI (Resultados) =====================
    /**
    * =========================================================
    * MÉTODO: mostrarResultados()
    * ---------------------------------------------------------
    * OBJETIVO:
    * Cargar la información completa del empleado identificado
    * por huella o tarjeta, registrar la marcación y ejecutar
    * las validaciones posteriores del flujo.
    *
    * LÓGICA:
    * 1. Actualiza estado visual de WebServices
    * 2. Muestra hora actual en pantalla
    * 3. Consulta datos del empleado en empleado_tmp
    * 4. Carga variables internas y campos visibles
    * 5. Registra la marcación identificada
    * 6. Aplica validaciones posteriores:
    *      - Inactivo
    *      - Vacaciones
    * 7. Ejecuta reglas de impresión
    * 8. Ejecuta diagnóstico automático
    *
    * NOTA:
    * - La marcación se registra ANTES de validar estado
    *   activo/vacaciones, porque la regla actual del sistema
    *   exige registrar toda marcación identificada
    * =========================================================
    */
   public void mostrarResultados() throws SQLException {
       System.out.println("LLEGO A MOSTRAR RESULTADOS");

       /* =====================================================
          1. ESTADO VISUAL DE CONEXIÓN / WEBSERVICES
       ===================================================== */
       if (Sincronizar.wsinconexion == 1) {
           vista.webServices("Nada");
       } else {
           vista.webServices("Todo");
           vista.EnviarTexto("Lectura Tarjeta RFID");
       }

       vista.EnviarTexto("Lectura con Huella / Tarjeta");
       vista.pintarDedoPuesto();

       DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
       vista.lblHora_setText(df.format(new Date()));

       try (ConexionDb db = new ConexionDb()) {
           db.Conectar();
           Connection conn = db.getConnection();

           /* =====================================================
              2. CONSULTAR DATOS DEL EMPLEADO IDENTIFICADO
           ===================================================== */
           String sqlEmpleado = "SELECT * FROM empleado_tmp WHERE cc = ?";

           HashMap<String, String> wdatos = null;

           try (PreparedStatement ps = conn.prepareStatement(sqlEmpleado)) {
               ps.setString(1, wcedula);

               try (ResultSet rs = ps.executeQuery()) {
                   if (rs.next()) {
                       wdatos = new HashMap<>();
                       java.sql.ResultSetMetaData meta = rs.getMetaData();
                       int cols = meta.getColumnCount();

                       for (int i = 1; i <= cols; i++) {
                           String col = meta.getColumnLabel(i);
                           String val = rs.getString(i);
                           wdatos.put(col, val);
                       }
                   }
               }
           }

           if (wdatos == null) {
               noResultados();
               return;
           }

           /* =====================================================
              3. CARGAR DATOS EN PANTALLA
           ===================================================== */
           vista.txtcc_setText(safeGet(wdatos, "cc"));
           vista.txtNombre_setText(safeGet(wdatos, "nombre"));

           /* =====================================================
              4. CARGAR VARIABLES INTERNAS
           ===================================================== */
           wcedula         = safeGet(wdatos, "cc");
           wnombres        = safeGet(wdatos, "nombre");
           widpersona      = safeGet(wdatos, "id_persona");
           wnro_vale       = safeGet(wdatos, "nro_vale");
           wesfijo         = safeGet(wdatos, "fijo");
           wnro_valex      = safeParseInt(wnro_vale, 0);
           wh_ingreso      = safeGet(wdatos, "h_ingreso");
           wh_salida       = safeGet(wdatos, "h_salida");
           wminutosretardo = safeGet(wdatos, "cond3");

           vista.txtCargo_setText(safeGet(wdatos, "cargo"));
           vista.txtEmpresa_setText(safeGet(wdatos, "empresa"));
           vista.lblMensaje1_setText(safeGet(wdatos, "sede"));
           quesede = safeGet(wdatos, "sede");

           valido_vale       = safeGet(wdatos, "derecho_vale");
           wderecho_vale     = valido_vale;
           valido_vacaciones = safeGet(wdatos, "vacaciones");

           vista.txtDepartamento_setText(safeGet(wdatos, "area"));
           wdepto = safeGet(wdatos, "area");

           vista.txtCiudad_setText(safeGet(wdatos, "ciudad"));
           vista.txtRotativo_setText("0".equals(safeGet(wdatos, "fijo")) ? "Si" : "No");

           wrotativo_validar_turno = safeGet(wdatos, "cond1");
           wfijo_genera_vale       = safeGet(wdatos, "cond2");

           /* =====================================================
              5. TRAZA DE TIPO DE EMPLEADO
           ===================================================== */
           if ("1".equals(wesfijo)) {
               System.out.println("PERSONA FIJA");
           } else {
               System.out.println("PERSONA ROTATIVA");
           }

           System.out.println("FIJO / ROTATIVO: " + wesfijo);

           is_adentro = "1".equals(safeGet(wdatos, "is_dentro"));
           System.out.println("llega o sale: " + is_adentro);

           /* =====================================================
              6. REGISTRO OBLIGATORIO DE MARCACIÓN
              Si ya se identificó la persona por huella o tarjeta,
              la marcación se registra siempre.
           ===================================================== */
           registrarMarcacionIdentificada();

           /* =====================================================
              7. VALIDACIONES POSTERIORES SOLO PARA MENSAJE / FLUJO
           ===================================================== */
           if ("0".equals(safeGet(wdatos, "activo")) || safeGet(wdatos, "activo").trim().isEmpty()) {
               vista.lblMensaje_setForeground(Color.red);
               vista.lblMensaje_setText("Inactivo");
               return;
           }

           if ("2".equals(valido_vacaciones)) {
               vista.lblMensaje_setForeground(Color.red);
               vista.lblMensaje_setText("Vacaciones");
               return;
           }

           /* =====================================================
              8. AJUSTES VISUALES FINALES
           ===================================================== */
           vista.pintarDedonoPuesto();

           if (Sincronizar.wsinconexion == 1) {
               vista.webServices("Sin Conexión a Red: verificar!");
           }
           
           /* =====================================================
            8.1 ESTADO DE IMPRESIÓN DE VALES (ICONO)
            ===================================================== */
            boolean activo = isImpresionValeActiva();
            vista.actualizarIconoVales(activo);
           /* =====================================================
              9. REGLAS DE IMPRESIÓN
           ===================================================== */
           accionesResultados();

           /* =====================================================
              10. DIAGNÓSTICO AUTOMÁTICO
           ===================================================== */
           try {
               ValeDiagnostic.Result diag = ValeDiagnostic.diagnosticarPorCC(wcedula);
               vista.EnviarTexto(diag.toString());
           } catch (Exception __e) {
               // silencioso
           }

       } catch (SQLException ex) {
           Logger.getLogger(IngresoTarjeta.class.getName()).log(Level.SEVERE, null, ex);
       }
   }  

    public void noResultados() {
        vista.txtMensajeVale_setText("Verificar Utilización de este Huellero");
        vista.lblMensaje_setForeground(Color.red);
        vista.btnImprimir_setEnabled(false);
        vista.txtcc_setText("........");
        vista.txtNombre_setText("Huella NO Registrada!");
        vista.txtCargo_setText("");
        vista.txtEmpresa_setText("");
        vista.txtDepartamento_setText("");
        vista.txtHIngreso_setText("");
        vista.txtHoraSalida_setText("");
        vista.txtCiudad_setText("");
        vista.txtRotativo_setText("");
        leyendoEmpleados = false;
        if (Sincronizar.wsinconexion == 1) vista.webServices("Sin Conexión a Red: verificar!");
    }

    public void noResultadosTarjeta() {
        vista.txtMensajeVale_setText("Verificar Utilización de este Huellero");
        vista.lblMensaje_setForeground(Color.red);
        vista.btnImprimir_setEnabled(false);
        vista.txtcc_setText("........");
        vista.txtNombre_setText("Tarjeta NO Registrada!");
        vista.txtCargo_setText("");
        vista.txtEmpresa_setText("");
        vista.txtDepartamento_setText("");
        vista.txtHIngreso_setText("");
        vista.txtHoraSalida_setText("");
        vista.txtCiudad_setText("");
        vista.txtRotativo_setText("");
        leyendoEmpleados = false;
        if (Sincronizar.wsinconexion == 1) vista.webServices("Sin Conexión a Red: verificar!");
    }

    public void noResultadosPersonasBd() {
        vista.txtMensajeVale_setText("Verificar Utilización de este Huellero");
        vista.lblMensaje_setForeground(Color.red);
        vista.btnImprimir_setEnabled(false);
        vista.txtcc_setText("........");
        vista.txtNombre_setText("BD Personas SIN Registros!");
        vista.txtCargo_setText("");
        vista.txtEmpresa_setText("");
        vista.txtDepartamento_setText("");
        vista.txtHIngreso_setText("");
        vista.txtHoraSalida_setText("");
        vista.txtCiudad_setText("");
        vista.txtRotativo_setText("");
        leyendoEmpleados = false;
        if (Sincronizar.wsinconexion == 1) vista.webServices("Sin Conexión a Red: verificar!");
    }

 /*****************************************************************************************************/
    
 /**
 * =========================================================
 * MÉTODO: accionesResultados()
 * ---------------------------------------------------------
 * OBJETIVO:
 * Evaluar si la persona identificada puede generar e imprimir
 * vale de almuerzo, validando horario, derecho a vale,
 * bloqueos por castigo y reglas integrales del negocio.
 *
 * LÓGICA:
 * 1. Cargar horario del día desde horarios_tmp
 * 2. Validar si el empleado debe tener horario
 * 3. Validar si tiene derecho a vale
 * 4. Validar bloqueo por acumulados / fecha_castigo
 * 5. Ejecutar validación integral con validarVale()
 * 6. Si todo está correcto, habilitar botón e imprimir
 *
 * NOTAS:
 * - El personal fijo siempre requiere horario
 * - El personal rotativo solo requiere horario si
 *   wrotativo_validar_turno = "S"
 * - Este método actualmente imprime automáticamente
 *   al final si la validación es exitosa
 * =========================================================
 */
    public void accionesResultados() throws SQLException {
        System.out.println("ENTRO A ACCIONES");
        vista.btnImprimir_setEnabled(false);
        /* =====================================================
            0. VALIDAR SI LA IMPRESIÓN DE VALES ESTÁ ACTIVA
            - Si imprimir_vales = NO:
              no valida impresora
              no valida vale
              no intenta imprimir
         ===================================================== */
         if (!isImpresionValeActiva()) {
             System.out.println("IMPRESION DE VALES DESACTIVADA POR CONFIGURACION");
             vista.txtMensajeVale_setText("");
             vista.EnviarTexto("Impresión de vales desactivada por configuración.");
             return;
         }
        final boolean es_fijo = !"0".equals(wesfijo); // fijo: "1", rotativo: "0"
        boolean tiene_horario = true;

        try (ConexionDb db = new ConexionDb()) {
            db.Conectar();
            Connection conn = db.getConnection();

            /* =====================================================
               1. CARGAR HORARIO DEL DÍA
            ===================================================== */
            String hoy = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

            String sqlHorario = "SELECT * FROM horarios_tmp WHERE CC = ? AND fecha = ? LIMIT 1";
            try (PreparedStatement psHorario = conn.prepareStatement(sqlHorario)) {
                psHorario.setString(1, wcedula);
                psHorario.setString(2, hoy);

                try (ResultSet rsHorario = psHorario.executeQuery()) {

                    if (!rsHorario.next()) {
                        tiene_horario = false;
                        System.out.println("NO Encontro Horario");

                        // El personal fijo siempre debe contar con horario
                        if (es_fijo) {
                            vista.txtMensajeVale_setText("El empleado no tiene registro Turnos");
                            return;
                        }

                        // El personal rotativo solo se bloquea si debe validar turno
                        if (!es_fijo && "S".equals(wrotativo_validar_turno)) {
                            vista.txtMensajeVale_setText("El empleado no tiene registro Turnos");
                            return;
                        }

                    } else {
                        String inicio_s = rsHorario.getString("INICIO");
                        String fin_s    = rsHorario.getString("FIN");

                        if (inicio_s == null) inicio_s = "";
                        if (fin_s == null) fin_s = "";

                        if (inicio_s.trim().isEmpty() || fin_s.trim().isEmpty()) {
                            tiene_horario = false;
                        }

                        vista.txtHIngreso_setText(inicio_s);
                        vista.txtHoraSalida_setText(fin_s);

                        System.out.println("SI Encontro Horario");
                    }
                }
            }

            /* =====================================================
               2. VALIDAR DERECHO A VALE
            ===================================================== */
            if (!"1".equals(wderecho_vale)) {
                vista.txtMensajeVale_setText("Impresión de vale desactivada");
                return;
            }

            /* =====================================================
               3. BLOQUEO POR ACUMULADOS / CASTIGO
            ===================================================== */
            String sqlBloqueo = "SELECT 1 FROM empleado_tmp WHERE CC = ? AND fecha_castigo > ? LIMIT 1";
            try (PreparedStatement psBloqueo = conn.prepareStatement(sqlBloqueo)) {
                psBloqueo.setString(1, wcedula);
                psBloqueo.setString(2, hoy + " 00:00:00");

                try (ResultSet rsBloqueo = psBloqueo.executeQuery()) {
                    if (rsBloqueo.next()) {
                        vista.txtMensajeVale_setText("Bloqueado por acumulados");
                        return;
                    }
                }
            }

            /* =====================================================
               4. VALIDACIÓN INTEGRAL DEL VALE
            ===================================================== */
            if (!validarVale()) {
                System.out.println("Se determinó que no hay impresión de Vale");
                vista.txtMensajeVale_setText(mensaje_vale);
                return;
            }

            /* =====================================================
               5. IMPRESIÓN
               OJO:
               Aquí no solo habilitas botón, también imprimes directo
            ===================================================== */
            vista.btnImprimir_setEnabled(true);
            System.out.println("Se envió impresión Vale");

            // pruebaImpresion();
            // imprimirValeJava2D();
            // imprimirValeEscPos();
            imprimirValeEscPosProfesional();
        }
    }
  
    
    
/******************************************************************************************************/    

    // ===================== Helpers =====================
    private static String safeGet(Map<String, String> m, String k) {
        String v = (m == null) ? null : m.get(k);
        return v == null ? "" : v;
    }

    private static int safeParseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    public void pruebaImpresion() {
        System.out.println("llego a void pruebaImpresion ");
        String queimpresora="TIQUETES";
        PrintService service = null;

        PrintService[] services = PrinterJob.lookupPrintServices();
        for (int i=0; service == null && i<services.length;i++) {
            if(services[i].getName().toUpperCase().contains(queimpresora)) {
                service =services[i];
                System.out.println("Encontro Impresora Tiquetes");
            }
        }
        if (service == null) {
            vista.txtMensajeVale_setText("No hay impresora configurada");
        } else {
            try {
                JasperReport reporte = null;
                String path = "src\\reportes\\vale.jasper";
                Map<String,Object> parametros = new HashMap<>();
                parametros.put("p_nombres", wnombres);
                parametros.put("p_depto", wdepto);
                parametros.put("p_sede","Generado en: " + quesede);
                parametros.put("p_cedula", wcedula);
                parametros.put("p_empresa", vista.txtEmpresa_getText());
                parametros.put("p_vale","VALE ALMUERZO ("+wcedula+")");

                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = new Date();
                wnro_valex = wnro_valex+1;
                String cantvale= Integer.toString(wnro_valex);
                parametros.put("p_barras1", widpersona + cantvale);
                parametros.put("p_fecha", "Fecha: " + dateFormat.format(date));
                String wwnumero=widpersona+cantvale;

                reporte = (JasperReport) JRLoader.loadObjectFromFile(path);
                JasperPrint jprint = JasperFillManager.fillReport(reporte, parametros, new JREmptyDataSource());

                PrintServiceAttributeSet printServiceAttributeSet = new HashPrintServiceAttributeSet();
                JRPrintServiceExporter exporter = new JRPrintServiceExporter();
                exporter.setExporterInput(new SimpleExporterInput(jprint));
                SimplePrintServiceExporterConfiguration configuration = new SimplePrintServiceExporterConfiguration();
                configuration.setPrintRequestAttributeSet(null);
                configuration.setPrintServiceAttributeSet(printServiceAttributeSet);
                configuration.setDisplayPageDialog(false);
                configuration.setDisplayPrintDialog(false);
                exporter.setConfiguration(configuration);
                exporter.exportReport();

                try (ConexionDb n = new ConexionDb()) {
                    DateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String fecha2 = dateFormat2.format(new Date());

                    n.Conectar();
                    HashMap<String, String> campos = new HashMap<>();
                    campos.put("cc", vista.txtcc_getText());
                    campos.put("evento", "Impresion-Vale");
                    campos.put("forma", wforma);
                    campos.put("fecha", fecha2);
                    campos.put("impresion", wwnumero);
                    campos.put("ctrl_impresion", cantvale);

                   n.insertHashMap(campos, "eventos_tmp");
                    n.insertHashMap(campos, "eventos_sitio");
                    vista.txtMensajeVale_setText("Se ha generado un Vale!");
                } catch (SQLException ex) {
                    Logger.getLogger(IngresoTarjeta.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (JRException ex) {
                Logger.getLogger(Inicio.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
/*****************************************************************************************************************/
   public void imprimirValeJava2D() {
        try {
            // 1) Resolver la impresora EXACTA "TIQUETES"
            PrintServiceAttributeSet attr = new HashPrintServiceAttributeSet();
            attr.add(new PrinterName("TIQUETES", null));
            PrintService[] matches = PrintServiceLookup.lookupPrintServices(null, attr);
            if (matches == null || matches.length == 0) {
                vista.txtMensajeVale_setText("La impresora \"TIQUETES\" no está disponible.");
                return;
            }
            PrintService svc = matches[0];

            // 2) PrinterJob en esa impresora
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintService(svc);

            // 3) Página tipo ticket (80mm). Ajusta alto si necesitas más líneas.
            PageFormat pf = job.defaultPage();
            Paper paper = pf.getPaper();
            double widthPts  = 226;   // ~80mm
            double heightPts = 420;   // alto del ticket
            paper.setSize(widthPts, heightPts);
            paper.setImageableArea(5, 5, widthPts - 10, heightPts - 10);
            pf.setPaper(paper);

            // 4) Datos a imprimir (usa los que ya tienes en la vista/modelo)
            String empresa = vista.txtEmpresa_getText();
            String titulo  = "VALE DE ALMUERZO";
            String nombres = vista.txtNombre_getText();
            String depto   = vista.txtDepartamento_getText();
            String cedula  = wcedula;
            String sede    = "Generado en: " + quesede;
            String fecha   = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());

            // 5) Contenido: Java2D
            Printable printable = (graphics, pageFormat, pageIndex) -> {
                if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
                Graphics2D g = (Graphics2D) graphics;
                g.setColor(Color.BLACK);

                int x = (int) pageFormat.getImageableX();
                int y = (int) pageFormat.getImageableY();
                int w = (int) pageFormat.getImageableWidth();
                int lh = 12, line = 0;

                g.setFont(new Font("Monospaced", Font.BOLD, 11));
                drawCenter(g, empresa, x, w, y + lh * (++line));

                g.setFont(new Font("Monospaced", Font.PLAIN, 9));
                drawCenter(g, titulo, x, w, y + lh * (++line));
                line++;

                g.drawString("Nombre: " + nombres,     x + 2, y + lh * (++line));
                g.drawString("Departamento: " + depto, x + 2, y + lh * (++line));
                g.drawString("Cédula: " + cedula,      x + 2, y + lh * (++line));
                g.drawString("Fecha: " + fecha,        x + 2, y + lh * (++line));
                g.drawString(sede,                      x + 2, y + lh * (++line));

                line += 2;
                drawCenter(g, "------------------------------", x, w, y + lh * (++line));
                drawCenter(g, "Gracias", x, w, y + lh * (++line));
                drawCenter(g, "------------------------------", x, w, y + lh * (++line));

                return Printable.PAGE_EXISTS;
            };

            job.setPrintable(printable, pf);

            // 6) Imprimir sin diálogo (directo a "TIQUETES")
            job.print();

            // 7) Registrar el evento en BD (usa tu método actual)
            registrarEventoImpresion();

            vista.txtMensajeVale_setText("Vale impreso en: " + svc.getName());
        } catch (PrinterException e) {
            vista.txtMensajeVale_setText("Error impresión: " + e.getMessage());
        }
    }

    // helper para centrar texto
    private static void drawCenter(Graphics2D g, String s, int x, int w, int y) {
        FontMetrics fm = g.getFontMetrics();
        int sw = fm.stringWidth(s);
        g.drawString(s, x + (w - sw) / 2, y);
    }

/*****************************************************************************************************************/
    public void imprimirValeEscPos() {
        // 0) Buscar la impresora
        String nombreObjetivo = "TIQUETES"; // parte del nombre
        PrintService service = buscarImpresoraPorNombre(nombreObjetivo);
        if (service == null) {
            vista.txtMensajeVale_setText("No se encontró impresora: " + nombreObjetivo);
            return;
        }

        try {
            // 1) Construir contenido del vale (texto) con tus datos actuales
            String empresa = vista.txtEmpresa_getText();
            String titulo  = "VALE DE ALMUERZO";
            String nombres = vista.txtNombre_getText();
            String depto   = vista.txtDepartamento_getText();
            String sede    = "Generado en: " + quesede;
            String cedula  = wcedula;
            String fecha   = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());

            // 2) ESC/POS bytes
            byte[] init        = new byte[]{0x1B, '@'};               // init
            byte[] alignCenter = new byte[]{0x1B, 'a', 0x01};         // center
            byte[] alignLeft   = new byte[]{0x1B, 'a', 0x00};         // left
            byte[] boldOn      = new byte[]{0x1B, 'E', 0x01};         // bold on
            byte[] boldOff     = new byte[]{0x1B, 'E', 0x00};         // bold off
            byte[] dblWH_on    = new byte[]{0x1D, '!', 0x11};         // double width + height
            byte[] dblWH_off   = new byte[]{0x1D, '!', 0x00};         // normal size
            byte[] lineFeed    = new byte[]{0x0A};
            byte[] cut         = new byte[]{0x1D, 'V', 0x42, 0x00};   // parcial cut (depende modelo)

            // (opcional) si tu impresora soporta CP437/Latin
            Charset cs = Charset.forName("CP437"); // o "ISO-8859-1" según impresora

            // 3) Armar el ticket
            BytesBuilder bb = new BytesBuilder();
            bb.add(init);
            bb.add(alignCenter);
            bb.add(boldOn); bb.add(dblWH_on);
            bb.add((empresa + "\n").getBytes(cs));
            bb.add(dblWH_off); bb.add(boldOff);

            bb.add((titulo + "\n").getBytes(cs));
            bb.add(lineFeed);

            bb.add(alignLeft);
            bb.add(("Nombre: " + nombres + "\n").getBytes(cs));
            bb.add(("Departamento: " + depto + "\n").getBytes(cs));
            bb.add(("Cédula: " + cedula + "\n").getBytes(cs));
            bb.add(("Fecha: " + fecha + "\n").getBytes(cs));
            bb.add((sede + "\n").getBytes(cs));
            bb.add(lineFeed); bb.add(lineFeed);

            //(Opcional) código de barras con ESC/POS (ejemplo Code128 si tu impresora lo soporta)
             String code = widpersona + String.valueOf(wnro_valex + 1);
             bb.add(new byte[]{0x1D, 'h', 80});        // altura
             bb.add(new byte[]{0x1D, 'w', 2});        // ancho módulo
             bb.add(new byte[]{0x1D, 'H', 2});        // texto debajo
             bb.add(new byte[]{0x1D, 'k', 73});       // GS k 73 = CODE128 auto
             bb.add(new byte[]{(byte)code.length()}); // longitud
             bb.add(code.getBytes(cs));
             bb.add(lineFeed);

            bb.add(cut);

            byte[] data = bb.toByteArray();

            // 4) Enviar a la impresora
            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            DocPrintJob job = service.createPrintJob();
            DocAttributeSet das = new HashDocAttributeSet();
            das.add(new PrinterResolution(180, 180, PrinterResolution.DPI));
            Doc doc = new SimpleDoc(data, flavor, das);
            job.print(doc, null);

            // 5) Registrar el evento como ya lo haces
            registrarEventoImpresion();

            vista.txtMensajeVale_setText("Se ha generado un Vale (ESC/POS)!");
        } catch (Exception ex) {
            vista.txtMensajeVale_setText("Error al imprimir ESC/POS: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

 
public void imprimirValeEscPosProfesional() {
    try {
        EscposValePrinter.Params p = new EscposValePrinter.Params();
        p.p_empresa = vista.txtEmpresa_getText();
        p.p_vale = wmensaje1;
        p.p_nombres = "Nombre: " +vista.txtNombre_getText();
        p.p_depto = "Depto : " +vista.txtDepartamento_getText();
        p.p_sede = "Genera: " + quesede;
        p.p_cedula = "Cédula: " +wcedula;
        p.p_fecha = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
        p.p_barras1 = widpersona + String.valueOf(wnro_valex + 1);
        p.impresora = "TIQUETES";
        p.barcode = EscposValePrinter.Barcode.CODE128;
        p.mostrarTextoBajoBarcode = false;

        EscposValePrinter.imprimir(p);

        registrarEventoImpresion();
        vista.txtMensajeVale_setText("Se ha generado un Vale!");
    } catch (Exception ex) {
        vista.txtMensajeVale_setText("Error al imprimir vale profesional: " + ex.getMessage());
        ex.printStackTrace();
    }
}
    
    private PrintService buscarImpresoraPorNombre(String fragmento) {
        PrintService[] services = PrinterJob.lookupPrintServices();
        for (PrintService ps : services) {
            if (ps.getName().toUpperCase().contains(fragmento.toUpperCase())) return ps;
        }
        return null;
    }

    // Pequeño helper para concatenar bytes
    static class BytesBuilder {
        private java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        void add(byte[] b) { try { out.write(b); } catch (Exception ignored) {} }
        byte[] toByteArray() { return out.toByteArray(); }
    }
//*************************************************************************************************************************************
    
   /**
 * =========================================================
 * MÉTODO: registrarEventoImpresion()
 * ---------------------------------------------------------
 * OBJETIVO:
 * Registrar localmente la impresión del vale y luego intentar
 * enviarla inmediatamente al servidor central.
 *
 * LÓGICA:
 * 1. Genera/incrementa el consecutivo del vale
 * 2. Construye el número final del vale
 * 3. Inserta el evento en eventos_tmp
 * 4. Inserta el evento en eventos_sitio
 * 5. Intenta enviarlo inmediatamente al servidor central
 * 6. Si el envío falla, el registro queda sync = 0
 *
 * IMPORTANTE:
 * - El consecutivo del vale se incrementa AQUÍ
 * - El envío NO vuelve a incrementar nada
 * =========================================================
 */
private void registrarEventoImpresion() {
    try (ConexionDb n = new ConexionDb()) {
        n.Conectar();

        DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String fecha2 = df2.format(new Date());

        /* =====================================================
           1. GENERAR / INCREMENTAR CONSECUTIVO DEL VALE
        ===================================================== */
        wnro_valex = wnro_valex + 1;
        String cantvale = Integer.toString(wnro_valex);
        String wwnumero = widpersona + cantvale;

        /* =====================================================
           2. ARMAR CAMPOS DEL EVENTO DE IMPRESIÓN
        ===================================================== */
        HashMap<String, String> campos = new HashMap<>();
        campos.put("cc", vista.txtcc_getText());
        campos.put("evento", "Impresion-Vale");
        campos.put("forma", wforma);
        campos.put("fecha", fecha2);
        campos.put("impresion", wwnumero);
        campos.put("ctrl_impresion", cantvale);

        /* =====================================================
           3. REGISTRO LOCAL
        ===================================================== */
        n.insertHashMap(campos, "eventos_tmp");
        n.insertHashMap(campos, "eventos_sitio");

        /* =====================================================
           4. ENVÍO INMEDIATO AL SERVIDOR CENTRAL
        ===================================================== */
        boolean enviado = enviarEventoServidorCentral(campos, fecha2);

        if (!enviado) {
            System.out.println("No se pudo enviar inmediatamente la impresión del vale al servidor central.");
            System.out.println("Queda pendiente para sincronización automática.");
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
}
/*****************************************************************************************/
    /**
    * =========================================================
    * MÉTODO: registrarMarcacionIdentificada()
    * ---------------------------------------------------------
    * OBJETIVO:
    * Registrar localmente la marcación identificada por huella
    * o tarjeta y enviarla inmediatamente al servidor central.
    *
    * LÓGICA:
    * 1. Determina si el evento es Ingreso o Salida
    * 2. Inserta en eventos_tmp
    * 3. Inserta en eventos_sitio
    * 4. Intenta enviar inmediatamente al servidor central
    * 5. SOLO si el envío fue exitoso:
    *      - actualiza empleado_tmp.is_dentro
    * 6. Si el WebService falla:
    *      - NO actualiza empleado_tmp.is_dentro
    *      - el evento queda pendiente con sync = 0
    *
    * NOTA:
    * - wforma indica el origen de la marcación:
    *      H = Huella
    *      T = Tarjeta
    * =========================================================
    */
   private void registrarMarcacionIdentificadaborrar() {
       try (ConexionDb db = new ConexionDb()) {
           db.Conectar();

           DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
           String fechaEvento = df2.format(new Date());

           HashMap<String, String> campos = new HashMap<>();
           campos.put("fecha", fechaEvento);
           campos.put("cc", wcedula);
           campos.put("forma", wforma);

           if (is_adentro) {
               vista.lblMensaje_setText("Salida");
               campos.put("evento", EVT_SALIDA);
           } else {
               vista.lblMensaje_setText("Ingreso");
               campos.put("evento", EVT_INGRESO);
           }

           /* =====================================================
              1. REGISTRO LOCAL
           ===================================================== */
           db.insertHashMap(campos, "eventos_tmp");
           db.insertHashMap(campos, "eventos_sitio");

           /* =====================================================
              2. ENVÍO INMEDIATO AL SERVIDOR CENTRAL
           ===================================================== */
           boolean enviado = enviarEventoServidorCentral(campos, fechaEvento);

           /* =====================================================
              3. VALIDAR RESULTADO DEL WEBSERVICE
              - Si falla, NO se actualiza empleado_tmp.is_dentro
              - El evento queda sync = 0 para sincronización posterior
           ===================================================== */
           if (!enviado) {
               System.out.println("No se pudo enviar inmediatamente la marcación al servidor central.");
               //System.out.println("NO se actualiza empleado_tmp.is_dentro.");
               System.out.println("El evento queda pendiente para sincronización automática.");
               //return;
           }

           /* =====================================================
              4. ACTUALIZAR ESTADO LOCAL DE ENTRADA / SALIDA
              SOLO SI EL ENVÍO FUE EXITOSO
           ===================================================== */
           String valor = is_adentro ? "0" : "1";
           db.statement.executeUpdate(
                   "UPDATE empleado_tmp SET is_dentro = " + valor + " WHERE cc = '" + wcedula + "'"
           );
           System.out.println("Se actualiza empleado_tmp.is_dentro..."+is_adentro);

       } catch (SQLException ex) {
           Logger.getLogger(IngresoTarjeta.class.getName())
                 .log(Level.SEVERE, "Error al registrar marcación identificada", ex);
       }
   } 
    private void registrarMarcacionIdentificada() {
        try (ConexionDb db = new ConexionDb()) {
            db.Conectar();

            DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String fechaEvento = df2.format(new Date());

            HashMap<String, String> campos = new HashMap<>();
            campos.put("fecha", fechaEvento);
            campos.put("cc", wcedula);
            campos.put("forma", wforma);

            if (is_adentro) {
                vista.lblMensaje_setText("Salida");
                campos.put("evento", EVT_SALIDA);
            } else {
                vista.lblMensaje_setText("Ingreso");
                campos.put("evento", EVT_INGRESO);
            }

            // 1. Registro local
            db.insertHashMap(campos, "eventos_tmp");
            db.insertHashMap(campos, "eventos_sitio");

            // 2. Actualizar estado local inmediatamente
            String valor = is_adentro ? "0" : "1";
            db.statement.executeUpdate(
                    "UPDATE empleado_tmp SET is_dentro = " + valor + " WHERE cc = '" + wcedula + "'"
            );

            // 3. Intentar envío inmediato sin bloquear
            intentarEnvioInmediatoNoBloqueante(campos, fechaEvento);

        } catch (SQLException ex) {
            Logger.getLogger(IngresoTarjeta.class.getName())
                  .log(Level.SEVERE, "Error al registrar marcación identificada", ex);
        }
    }
/**********************************************************************************************************************
    /**
    * =========================================================
    * MÉTODO: enviarEventoServidorCentral(...)
    * ---------------------------------------------------------
    * OBJETIVO:
    * Enviar inmediatamente al servidor central un evento recién
    * registrado localmente en eventos_tmp.
    *
    * PARÁMETROS:
    * - campos:
    *   HashMap con los datos exactos insertados localmente
    * - fechaEvento:
    *   Fecha exacta del registro local
    *
    * RETORNO:
    * - true  -> el WebService respondió exitosamente y el evento
    *            fue marcado localmente como sync = 1
    * - false -> falló el envío; el evento queda sync = 0 para
    *            que luego lo procese la sincronización automática
    *
    * LÓGICA:
    * 1. Construye el bloque "info" con el formato esperado por
    *    el WebService setEventos_jhcm_ht
    * 2. Consulta id_sede desde la tabla config
    * 3. Envía el evento al servidor central
    * 4. Si el envío es exitoso:
    *      - marca SOLO ese registro como sync = 1
    * 5. Si el envío falla:
    *      - deja el registro con sync = 0
    *
    * NOTAS:
    * - Este método sirve para:
    *      * Impresion-Vale
    *      * Ingreso
    *      * Salida
    *      * otros eventos futuros
    * - No recalcula ni altera los datos del evento
    * =========================================================
    */
   private boolean enviarEventoServidorCentral(HashMap<String, String> campos, String fechaEvento) {
       try (ConexionDb db = new ConexionDb()) {
           db.Conectar();

           /* =====================================================
              1. CONSTRUIR FORMATO "info" PARA EL WEBSERVICE
              Formato esperado:
              [[campo---*---valor:::::campo---*---valor]]
           ===================================================== */
           java.util.List<String> pares = new java.util.ArrayList<>();

           for (java.util.Map.Entry<String, String> entry : campos.entrySet()) {
               String clave = (entry.getKey() == null) ? "" : entry.getKey();
               String valor = (entry.getValue() == null) ? "" : entry.getValue();

               clave = clave.replace("---*---", "\\---\\*\\---")
                            .replace(":::::", "\\:::::")
                            .replace(";;;;;", "\\;;;;;");

               valor = valor.replace("---*---", "\\---\\*\\---")
                            .replace(":::::", "\\:::::")
                            .replace(";;;;;", "\\;;;;;");

               pares.add(clave + "---*---" + valor);
           }

           String eventoStr   = "[" + String.join(":::::", pares) + "]";
           String dataCompleta = "[[" + eventoStr + "]]";

           /* =====================================================
              2. CONSULTAR id_sede DESDE CONFIG
           ===================================================== */
           String idSede = "1";

           try {
               HashMap<String, String> idSedeObj =
                       db.selectOne("SELECT valor FROM config WHERE dato='id_sede'");

               if (idSedeObj != null
                       && idSedeObj.get("valor") != null
                       && !idSedeObj.get("valor").trim().isEmpty()) {
                   idSede = idSedeObj.get("valor").trim();
               }
           } catch (Exception ex) {
               System.out.println("No se pudo consultar id_sede; se usará valor por defecto = 1");
           }

           /* =====================================================
              3. ARMAR PARÁMETROS DEL WEBSERVICE
           ===================================================== */
           HashMap<String, String> parametros = new HashMap<>();
           parametros.put("info", dataCompleta);
           parametros.put("id_sede", idSede);

           /* =====================================================
              4. ENVIAR AL SERVIDOR CENTRAL
           ===================================================== */
           //JSONTask t = new JSONTask("setEventos_jhcm_ht", parametros); /*VERSION V21 ORIGINAL*/
                      JSONTask t = new JSONTask("setEventos_jhcm_ht", parametros, true); //V23 15-09-2026
           boolean envioExitoso = t.enviar(false, null);

           /* =====================================================
              5. SI EL ENVÍO FUE EXITOSO, MARCAR sync = 1
              SOLO PARA EL EVENTO RECIÉN INSERTADO
           ===================================================== */
           if (envioExitoso) {
               String sqlUpdate = "UPDATE eventos_tmp "
                       + "SET sync = 1 "
                       + "WHERE cc = ? "
                       + "AND evento = ? "
                       + "AND fecha = ?";

               try (PreparedStatement ps = db.getConnection().prepareStatement(sqlUpdate)) {
                   ps.setString(1, campos.get("cc"));
                   ps.setString(2, campos.get("evento"));
                   ps.setString(3, fechaEvento);

                   int actualizados = ps.executeUpdate();
                   System.out.println("Evento enviado al servidor central. Registros sync=1: " + actualizados);
               }

               return true;
           }

           /* =====================================================
              6. SI FALLA EL ENVÍO, DEJAR sync = 0
              PARA REINTENTO AUTOMÁTICO POSTERIOR
           ===================================================== */
           System.out.println("No fue posible enviar inmediatamente el evento al servidor central.");
           System.out.println("El evento queda pendiente con sync = 0.");
           return false;

       } catch (Exception ex) {
           System.out.println("Error en enviarEventoServidorCentral(): " + ex.getMessage());
           ex.printStackTrace();
           return false;
       }
   }  
 //**************************************************************************************************************************
   
   private void intentarEnvioInmediatoNoBloqueante(final HashMap<String, String> campos, final String fechaEvento) {

        Thread hiloEnvio = new Thread(() -> {
            try {
                if (!process.Sincronizar.lockEnvioEventos.compareAndSet(false, true)) {
                    System.out.println("Marcación: ya existe un envío de eventos en curso. Se deja pendiente para sincronización automática.");
                    return;
                }

                try {
                    boolean enviado = enviarEventoServidorCentral(campos, fechaEvento);

                    if (!enviado) {
                        System.out.println("No se pudo enviar inmediatamente la marcación al servidor central.");
                        System.out.println("Queda pendiente para sincronización automática.");
                    }
                } finally {
                    process.Sincronizar.lockEnvioEventos.set(false);
                }

            } catch (Exception ex) {
                System.out.println("Error en envío inmediato no bloqueante: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, "EnvioInmediatoMarcacion");

        hiloEnvio.setDaemon(true);
        hiloEnvio.start();
    }
//***************************************************************************************************************************

    public void queImprimirVales() {
        try (ConexionDb db = new ConexionDb()) {
            db.Conectar();
            HashMap<String, String> row = db.selectOne("SELECT valor FROM config WHERE dato='imprimir_vales'");
            wimprimir_vales = (row != null && row.get("valor") != null && !row.get("valor").trim().isEmpty())
                    ? row.get("valor").trim().toUpperCase()
                    : "NO";
            wimprimirValesCacheCargado = true;
            
            System.out.println("CONFIG imprimir_vales = " + wimprimir_vales);
            if ("NO".equalsIgnoreCase(wimprimir_vales)) {
                vista.EnviarTexto("⚠ Impresión de vales desactivada");
            }
            
        } catch (SQLException ignored) {
            wimprimir_vales = "NO";
            wimprimirValesCacheCargado = true;
            System.out.println("CONFIG imprimir_vales = NO (por excepción)");
            vista.EnviarTexto("⚠ Impresión de vales desactivada");
        }
    }
   
//***************************************************************************************************************************    
}

