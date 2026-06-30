package models;

import database.ConexionDb;
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
import database.Db;
import process.JSONTask;
import java.awt.Color;
import java.awt.Image;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
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
import javax.print.attribute.standard.PrinterResolution;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.view.JasperViewer;
import vistas.Ingreso;
import vistas.Inicio;

/* Clase encargada de la logica y el controlar el acceso a datos del Ingreso de empleados
 */
public class IngresoModel extends javax.swing.JFrame{
   
    public DPFPCapture Lector = DPFPGlobal.getCaptureFactory().createCapture();
    public DPFPEnrollment Reclutador = DPFPGlobal.getEnrollmentFactory().createEnrollment();
    public DPFPVerification Verificador = DPFPGlobal.getVerificationFactory().createVerification();
    public DPFPTemplate template;
    public static String TEMPLATE_PROPERTY = "template";

    public int n_huella;

    public String mensaje_vale = "";
    public String entra_sale = "";
    public String valido_vale = "";
    public String valido_vacaciones = "";
    public String quesede;
    public String wrotativo_validar_turno="";
    public String wfijo_genera_vale="";
    public String tiene_ingreso="";
    /******/
    public String wnombres="";
    public String wdepto="";
    public String wcedula="";
    public String wfecha="";
    public String widpersona="";
    public String wnro_vale="";
    public Integer wnro_valex=0;
    public String wfecha_castigo;
    public String wwfecha_castigo;
    public String wwimpresion = "N";
    public String wmensaje1="";
    /******/

    public DPFPFeatureSet featuresinscripcion;
    public DPFPFeatureSet featuresverificacion; 

    public Inicio inicio;
    public int lblwidth;
    public int lblHeight;
    public Statement statement;

    public volatile ArrayList<HashMap<String, String>> empleados_cache = null;
    public volatile ArrayList<HashMap<String, String>> empleados = null;
    public volatile boolean leyendoEmpleados = true;

    public Ingreso vista ;
    ConexionDb n;
    boolean is_adentro;

    /**
     * Constructor encargado de iniciar el lector y mapear las varialbes que se van a utilizar.
     */
    public IngresoModel(Ingreso vista,Inicio i) {
        System.out.println("inicio ingreso_model");
        this.inicio = i;
        this.vista = vista;
        queSede();
        initLector();
        lblwidth = vista.lblHuella_getWidth();
        lblHeight = vista.lblHuella_getHeight();
    }
    
    /**
     * Compara una susecion de huellas para extraer una caracterisitcia final de una lista de 4 huellas
     */
    public void setTemplate(DPFPTemplate template) {
        DPFPTemplate old = this.template;
        this.template = template;
        firePropertyChange(TEMPLATE_PROPERTY, old, template);
    }

    /**
     * Valida si un vale puede ser impreso segun las reglas de negocio.
     */
    private boolean validarVale(){    
        DateFormat dateFormat3 = new SimpleDateFormat("yyyy-MM-dd");
        Date date3 = new Date();
        String fecha3 = dateFormat3.format(date3) + "";
        Calendar now = Calendar.getInstance();
        int hora = now.get(Calendar.HOUR_OF_DAY);
        int minutos = now.get(Calendar.MINUTE);
        int segundos = now.get(Calendar.SECOND);
        tiene_ingreso="N";

        try {
            ArrayList<HashMap<String, String>> repuesta = n.selectAll(
                "SELECT * FROM eventos_tmp WHERE CC = '" + vista.txtcc_getText()+ 
                "' AND fecha > '" + fecha3 + " 00:00:00' AND  fecha < '" + fecha3 + 
                " 23:59:59' AND (evento ='Ingreso')");
            if (repuesta != null) {
               tiene_ingreso="S";
            }
        }catch(SQLException ex){
           Logger.getLogger(Ingreso.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("ESTOY EN OPCION VALIDAR VALE()");
        try{
            System.out.println("VALIDO SI LA PERSONA TIENE YA LE FUE IMPRESO EL VALE HOY - 1");
            ArrayList<HashMap<String, String>> repuesta = n.selectAll(
                "SELECT * FROM eventos_tmp WHERE CC = '" + vista.txtcc_getText()+ 
                "' AND fecha > '" + fecha3 + " 00:00:00' AND  fecha < '" + fecha3 + 
                " 23:59:59' AND (evento ='Impresion-Vale')");
            if (repuesta != null) {
                System.out.println("El vale de hoy ya fue impreso");
                mensaje_vale = "El vale de hoy ya fue impreso";
                return false;
            }
        }catch (SQLException ex){
            Logger.getLogger(Ingreso.class.getName()).log(Level.SEVERE, null, ex);
        }
         
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        String fecha = dateFormat.format(date) + "";

        //validamos si ya tiene castigo hoy
        try {
            System.out.println("VALIDO SI LA PERSONA TIENE CASTIGO HOY");
            ArrayList<HashMap<String, String>> repuesta = n.selectAll(
                "SELECT * FROM eventos_tmp WHERE CC = '" + vista.txtcc_getText() + 
                "' AND fecha > '" + fecha + " 00:00:00' AND  fecha < '" + fecha + 
                " 23:59:59' AND (evento ='Castigo-8' OR evento ='Castigo-30')");
            System.out.println("Verificar si ya fue castigado");
            if(repuesta != null){
                mensaje_vale = "Llegada tarde no se imprime vale.";
                return false;
            }
        } catch (SQLException ex) {
            Logger.getLogger(Ingreso.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            n.Conectar();
            System.out.println("SELECT * FROM permisos_tmp WHERE CC = '" + vista.txtcc_getText() + 
                               "' AND  FECHA_INICIO >= '" + fecha + " 00:00:00'  AND   FECHA_FIN <= '" + fecha + " 23:23:23'");
            HashMap<String, String> repuesta = n.selectOne(
                "SELECT FECHA_FIN FROM permisos_tmp WHERE CC = '" + vista.txtcc_getText() + 
                "' AND  FECHA_INICIO >= '" + fecha + " 00:00:00'  AND   FECHA_FIN <= '" + fecha + " 23:23:23'");
            if (repuesta != null) {
                String hora_ingreso = repuesta.get("FECHA_FIN");
                vista.txtHIngreso_setText(hora_ingreso.split(" ")[1]);
            }
        } catch (SQLException ex) {
            Logger.getLogger(Ingreso.class.getName()).log(Level.SEVERE, null, ex);
        }

        boolean provarcastigo = true;
        try {
            System.out.println("EL CASTIGO YA NO VALE YA ENTRO HOY");
            ArrayList<HashMap<String, String>> repuesta = n.selectAll(
                "SELECT * FROM eventos_tmp WHERE CC = '" + vista.txtcc_getText()+ 
                "' AND fecha > '" + fecha3 + " 00:00:00' AND  fecha < '" + fecha3 + 
                " 23:59:59' AND (evento ='Ingreso')");
            if (repuesta != null) {
                System.out.println("No se prueba el castigo ya ingreso hoy");
                provarcastigo = false;
            }
        }catch(SQLException ex){
            Logger.getLogger(Ingreso.class.getName()).log(Level.SEVERE, null, ex);
        }
           
        if ("sale".equals(entra_sale)) {
            if (provarcastigo) {
                try {
                    if (castigar(hora, minutos, segundos)) {
                        if (mensaje_vale.length() == 0) {
                            mensaje_vale = "Llegada tarde no se imprime vale";
                        }
                        return false;
                    }
                } catch (ParseException e) {}
            }
        }
        mensaje_vale = "";
        System.out.println("validando vale");

        //Solo fijos: lunes-viernes
        if (vista.txtRotativo_getText().equals("No")) {
            int day = now.get(Calendar.DAY_OF_WEEK);
            boolean DAY = day >= Calendar.MONDAY && day <= Calendar.FRIDAY;
            if (!DAY) {
                mensaje_vale = "Los vales solo estan disponibles de lunes a viernes";
                return false;
            }
        }

        // derecho a vale
        if (valido_vale.equals("0")) {
            mensaje_vale = "El empleado no tiene derecho a vale";
            return false;
        }

        // ventana horaria para fijos (M = mediodía)
        if(vista.txtRotativo_getText().equals("No")){
            if (wfijo_genera_vale.equals("M") && (!((hora >= 11) && (hora < 14)))) {
                mensaje_vale = "Los vales se generan entre las 11:00 y 13:59 ";
                return false;
             }
        }

        if(vista.txtRotativo_getText().equals("No")){
            if (wfijo_genera_vale.equals("I") && (is_adentro == true) && "N".equals(tiene_ingreso)) {
                mensaje_vale = "Los vales se generan cuando el tipo de marcación es Entrada";
                return false;
            }
        }

        // rotativo: solo al ingreso
        if (vista.txtRotativo_getText().equals("Si")) {
            if (is_adentro) {
                mensaje_vale = "Para rotativos vale solo al ingreso";
                return false;
            }
        }
        return true;
    }
     
    /**
     * Aplica “castigo” por llegada tarde (registra evento).
     */
    private boolean castigar(int hora, int minutos, int segundos) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        String fecha = dateFormat.format(date) + "";

        Date esperada;
        Date esperada_5;
        SimpleDateFormat parser = new SimpleDateFormat("HH:mm:ss");

        if (vista.txtRotativo_getText().equals("Si")) {
            String ingreso_esperado = vista.txtHIngreso_getText();
            esperada = parser.parse(ingreso_esperado);
            long t = esperada.getTime();
            esperada_5 = new Date(t + (4 * 60000) + (1 * 60000));
            esperada = new Date(t + (1 * 60000));
            System.out.println("Rotativo castigo");
        } else {
            esperada = parser.parse(vista.txtHIngreso_getText());
            long t = esperada.getTime();
            esperada_5 =  new Date(t + (4 * 60000) + (1 * 60000));
            System.out.println("fijo castigo");
            esperada = new Date(t + (1 * 60000));
        }

        String s_hora = (hora < 10) ? "0"+hora : String.valueOf(hora);
        String s_minuto = (minutos < 10) ? "0"+minutos : String.valueOf(minutos);
        String s_segundo = (segundos < 10) ? "0"+segundos : String.valueOf(segundos);

        Date entrada = parser.parse(s_hora + ":" + s_minuto + ":" + s_segundo);
        System.out.println("Hora llegada " + entrada);
        System.out.println("Hora esperada " + esperada);

        if (entrada.after(esperada)) {
            if (entrada.before(esperada_5)) {
                try {
                    mensaje_vale = "LLegada tarde se aplica retención de vale por 8 días";
                    HashMap<String, String> campos = new HashMap<>();
                    campos.put("cc", vista.txtcc_getText());
                    campos.put("evento", "Castigo-8");
                    DateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String fecha2 = dateFormat2.format(new Date());
                    campos.put("fecha", fecha2);
                    //n.insertHashMap(campos, "eventos");
                    n.insertHashMap(campos, "eventos_tmp");
                    n.insertHashMap(campos, "eventos_sitio");
                } catch (SQLException ex) {
                    Logger.getLogger(Ingreso.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                try {
                    mensaje_vale = "LLegada tarde se aplica retención de vale por 30 días";
                    HashMap<String, String> campos = new HashMap<>();
                    campos.put("cc", vista.txtcc_getText());
                    campos.put("evento", "Castigo-30");
                    DateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String fecha2 = dateFormat2.format(new Date());
                    campos.put("fecha", fecha2);
                    //n.insertHashMap(campos, "eventos");
                    n.insertHashMap(campos, "eventos_tmp");
                    n.insertHashMap(campos, "eventos_sitio");
                } catch (SQLException ex) {
                    Logger.getLogger(Ingreso.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return true;
        } else {
            System.out.println("NO Castigar Empleado Planta");
            return false;
        }
    }

    /** Utilidad concatenar bytes (impresora térmica) */
    public byte[] concat(byte[] a, byte[] b) {
        int aLen = a.length;
        int bLen = b.length;
        byte[] c = new byte[aLen + bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }
      
    public void setIngresoServidor(){
        ConexionDb Db = null;
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            HashMap<String, String> parametros = new HashMap<>();
            String fecha2 = dateFormat.format(new Date()) + "";
            Db = new ConexionDb();
            Db.Conectar();
            HashMap<String, String> id_sede_obj = Db.selectOne("SELECT valor FROM config WHERE dato='id_sede' ");
            if (id_sede_obj != null){
                parametros.put("sede", id_sede_obj.get("valor"));
            } else {
                parametros.put("sede", "1");
            }
            parametros.put("fecha", fecha2);
            parametros.put("cc", vista.txtcc_getText());
            JSONTask t = new JSONTask("setIngreso", parametros);
            if (t.enviar(false, this)){
                System.out.print("OOOOOK Set ingreso servidor ++++++++");
            } else {
                System.out.print("++++NO HAY COMUNICACION CON EL SERVIDOR");
            }
        } catch (SQLException e) {
        } finally {
            try { if (Db!=null) Db.close(); } catch (SQLException e) {}
        }
    }
    
    public void queSede(){
        ConexionDb Db = null;
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Db = new ConexionDb();
            Db.Conectar();
            HashMap<String, String> id_sede_obj = Db.selectOne("SELECT valor FROM config WHERE dato='id_sede' ");
            String wsede = (id_sede_obj != null) ? id_sede_obj.get("valor") : "1";
            System.out.println("Valido codigo sede .."+wsede);
            HashMap<String, String> wsede_obj = Db.selectOne("SELECT sede FROM sedes_tmp WHERE id_sede='"+wsede+"'");
            String wwsede = (wsede_obj != null) ? wsede_obj.get("sede") : "xxxxxxx";
            System.out.println("Valido nombre  sede .."+wwsede);
            vista.lblMensaje1_setText(wwsede);
        } catch (SQLException e) {
        } finally {
            try { if (Db!=null) Db.close(); } catch (SQLException e) {}
        }
    }

    /**
     * Imprimir vale en impresora térmica
     */
    public void imprimirVale(){
        vista.txtMensajeVale_setText("Imprimiendo...");
        System.out.println("Imprimiendo vale");
        vista.btnImprimir_setEnabled(false);
        String texto = "\r\n";
        texto += "Nombre: " + vista.txtNombre_getText() + "\r\n";
        texto += "Departamento: " + vista.txtDepartamento_getText() + "\r\n";
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        texto += "Fecha: " + dateFormat.format(date) + "\r\n";
        texto += "Generado en: " + quesede + "\r\n";
        texto += "\r\n\r\n";

        PrintService printService = PrintServiceLookup.lookupDefaultPrintService();
        DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
        DocPrintJob docPrintJob = printService.createPrintJob();
        DocAttributeSet das = new HashDocAttributeSet();
        das.add(new PrinterResolution(180, 180, PrinterResolution.DPI));
        byte[] espacio = {27, 100, 3};
        byte[] corte = {27, 105, 3};
        byte[] vacio = {};
        byte[] letra1 = {29, 33, 34};
        byte[] letra2 = {29, 33, 0};
        byte[] letra3 = {29, 33, 17};

        byte[] bytes = concat(vacio, letra1);
        bytes = concat(bytes, vista.txtEmpresa_getText().getBytes());
        bytes = concat(bytes, letra3);
        bytes = concat(bytes, "\r\nVale de almuerzo".getBytes());
        bytes = concat(bytes, letra2);
        bytes = concat(bytes, texto.getBytes());
        bytes = concat(bytes, espacio);
        bytes = concat(bytes, espacio);
        bytes = concat(bytes, espacio);
        bytes = concat(bytes, corte);

        Doc doc = new SimpleDoc(bytes, flavor, das);
        try {
            docPrintJob.print(doc, null);
        } catch (PrintException e) { }

        try {
            String fecha2 = dateFormat.format(new Date()) + "";
            n.Conectar();
            HashMap<String, String> campos = new HashMap<String, String>();
            campos.put("cc", vista.txtcc_getText());
            campos.put("evento", "Impresion-Vale");
            campos.put("fecha", fecha2);
            //n.insertHashMap(campos, "eventos");
            n.insertHashMap(campos, "eventos_tmp");
            n.insertHashMap(campos, "eventos_sitio");
        } catch (SQLException ex) {
            Logger.getLogger(Ingreso.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try { n.close(); } catch (SQLException e) { }
        }

        ConexionDb Db = null;
        try {
            HashMap<String, String> parametros = new HashMap<>();
            String fecha2 = dateFormat.format(new Date()) + "";
            Db = new ConexionDb();
            Db.Conectar();
            HashMap<String, String> id_sede_obj = Db.selectOne("SELECT valor FROM config WHERE dato='id_sede' ");
            parametros.put("sede", id_sede_obj != null ? id_sede_obj.get("valor") : "1");
            parametros.put("fecha", fecha2);
            parametros.put("cc", vista.txtcc_getText());
            // JSONTask t = new JSONTask("setImpVale", parametros);
            // if (t.enviar(false, this)) { ... }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (Db!=null) Db.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    /**
     * Impresión especial para rotativo sin turno
     */
    private void imprimirEspecialNoHorario(String tipo) {
        DateFormat dateFormat3 = new SimpleDateFormat("yyyy-MM-dd");
        String fecha3 = dateFormat3.format(new Date()) + "";
        try {
            ArrayList<HashMap<String, String>> repuesta = n.selectAll(
                "SELECT * FROM eventos_tmp WHERE CC = '" + vista.txtcc_getText() + 
                "' AND fecha > '" + fecha3 + " 00:00:00' AND  fecha < '" + fecha3 + 
                " 23:59:59' AND (evento ='Imprimir-Vale-No-Horario')");
            if (repuesta != null) {
                System.out.println("El vale de hoy ya fue impreso");
                mensaje_vale = "El vale de hoy ya fue impreso - No Horario";
                vista.txtMensajeVale_setText(this.mensaje_vale);
                return;
            }
        } catch (SQLException ex) {
            Logger.getLogger(Ingreso.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("Imprimiendo vale");
        vista.btnImprimir_setEnabled(false);
        String texto = "\r\n";
        texto += "Nombre: " + vista.txtNombre_getText() + "\r\n";
        texto += "Departamento: " + vista.txtDepartamento_getText() + "\r\n";
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        texto += "Fecha: " + dateFormat.format(new Date()) + "\r\n";

        PrintService printService = PrintServiceLookup.lookupDefaultPrintService();
        DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
        DocPrintJob docPrintJob = printService.createPrintJob();
        DocAttributeSet das = new HashDocAttributeSet();
        das.add(new PrinterResolution(180, 180, PrinterResolution.DPI));
        byte[] espacio = {27, 100, 3};
        byte[] corte = {27, 105, 3};
        byte[] vacio = {};
        byte[] letra1 = {29, 33, 34};
        byte[] letra2 = {29, 33, 0};
        byte[] letra3 = {29, 33, 17};

        byte[] bytes = concat(vacio, letra1);
        bytes = concat(bytes, vista.txtEmpresa_getText().getBytes());
        bytes = concat(bytes, letra3);
        bytes = concat(bytes, "\r\nVale de almuerzo".getBytes());
        bytes = concat(bytes, letra2);
        bytes = concat(bytes, texto.getBytes());
        bytes = concat(bytes, espacio);
        bytes = concat(bytes, espacio);
        bytes = concat(bytes, espacio);
        bytes = concat(bytes, corte);

        Doc doc = new SimpleDoc(bytes, flavor, das);
        try { docPrintJob.print(doc, null); } catch (PrintException e) { e.printStackTrace(); }

        try {
            String fecha2 = dateFormat.format(new Date()) + "";
            n.Conectar();
            HashMap<String, String> campos = new HashMap<String, String>();
            campos.put("cc", vista.txtcc_getText());
            campos.put("evento", tipo);
            campos.put("fecha", fecha2);
            //n.insertHashMap(campos, "eventos");
            n.insertHashMap(campos, "eventos_tmp");
            n.insertHashMap(campos, "eventos_sitio");
        } catch (SQLException ex) {
            Logger.getLogger(Ingreso.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try { n.close(); } catch (Exception e) { }
        }
    }
    
    /**
     * Valida si el vale del dia de hoy ya fue impreso consultando el WS.
     */
    public boolean isImpresoValidacion() {
        HashMap<String, String> parametros = new HashMap<String, String>();
        parametros.put("cc", vista.txtcc_getText());
        JSONTask t = new JSONTask("validarVale", parametros);
        if (t.enviar(false, this)){
            HashMap<String, String> m = t.respuesta;
            if (m == null) return false;
            String res = m.get("impreso");
            if ("0".equals(res)) return false;
            vista.btnImprimir_setEnabled(false);
            vista.txtMensajeVale_setText("El vale ya fue impreso");
            return true;
        }
        return false;
    }
    
    /**
     * Extrae características de una muestra de huella
     */
    public DPFPFeatureSet extraerCaracteristicas(DPFPSample sample, DPFPDataPurpose purpose) {
        DPFPFeatureExtraction extractor = DPFPGlobal.getFeatureExtractionFactory().createFeatureExtraction();
        try {
            return extractor.createFeatureSet(sample, purpose);
        } catch (DPFPImageQualityException e) {
            return null;
        }
    }

    /**
     * Proceso principal tras capturar una huella
     */
    public void ProcesarCaptura(DPFPSample sample) {
        featuresinscripcion = extraerCaracteristicas(sample, DPFPDataPurpose.DATA_PURPOSE_ENROLLMENT);
        featuresverificacion = extraerCaracteristicas(sample, DPFPDataPurpose.DATA_PURPOSE_VERIFICATION);
        if (featuresinscripcion != null) {
            try {
                Image image=vista.CrearImagenHuella(sample);
                vista.DibujarHuella(image);  
                System.out.println("Las Caracteristicas de la Huella han sido creada -Ingreso Model--");

                DateFormat dateFormat4 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String textoFeca = dateFormat4.format(new Date());
                vista.lblHora_setText(textoFeca);

                n = new ConexionDb();
                n.Conectar();

                empleados = n.selectAll("SELECT * FROM empleado_tmp");
                leyendoEmpleados = true;

                for (int i = 1; i < 3; i++) {    
                    for (int i_e = 0; i_e < empleados.size(); i_e++) {
                        try {
                            HashMap<String, String> empleado = empleados.get(i_e);
                            String hu = empleado.get("h" + i);
                            if (hu != null && hu.length() > 3) { 
                                byte[] decodedBytes = Base64.getDecoder().decode(hu.getBytes());
                                DPFPTemplate referenceTemplate = DPFPGlobal.getTemplateFactory().createTemplate(decodedBytes);
                                setTemplate(referenceTemplate);
                                DPFPVerificationResult result = Verificador.verify(featuresverificacion, this.template);
                                if (result.isVerified()) {
                                    boolean insertar_registro = true;
                                    System.out.println("OK");
                                    vista.txtcc_setText(empleado.get("cc"));
                                    wcedula=(empleado.get("cc"));
                                    vista.txtNombre_setText(empleado.get("nombre"));
                                    wnombres=(empleado.get("nombre"));
                                    widpersona=(empleado.get("id_persona"));
                                    wnro_vale=(empleado.get("nro_vale"));
                                    wnro_valex = Integer.parseInt(wnro_vale);
                                    vista.txtCargo_setText(empleado.get("cargo"));
                                    vista.txtEmpresa_setText(empleado.get("empresa"));
                                    vista.lblMensaje1_setText(empleado.get("sede"));
                                    quesede=empleado.get("sede");
                                    this.valido_vale = empleado.get("derecho_vale");
                                    this.valido_vacaciones = empleado.get("vacaciones");
                                    vista.txtDepartamento_setText(empleado.get("area"));
                                    wdepto=(empleado.get("area"));
                                    vista.txtCiudad_setText(empleado.get("ciudad"));
                                    vista.txtRotativo_setText(empleado.get("fijo").equals("0") ? "Si" : "No");
                                    wrotativo_validar_turno=empleado.get("cond1");
                                    wfijo_genera_vale=empleado.get("cond2");

                                    if (empleado.get("activo").equals("0") || empleado.get("activo").length() == 0) {
                                        vista.lblMensaje_setForeground(Color.red);
                                        vista.lblMensaje_setText("Inactivo");
                                        return;
                                    }
                                    if (empleado.get("vacaciones").equals("2")) {
                                        vista.lblMensaje_setForeground(Color.red);
                                        vista.lblMensaje_setText("Vacaciones");
                                        return;
                                    }
                                    is_adentro = empleado.get("is_dentro").equals("1");
                                    //vista.lblMensaje_setText("Identificado");

                                    boolean tiene_horario = true;
                                    boolean es_fijo = true;
                                    boolean wseguir = true;
                                    if (is_adentro) { entra_sale="entra"; } else {entra_sale="sale"; }
                                     System.out.println("PERSONA ROTATIVA***");
                                    // Rotativo / Fijo
                                    if (empleado.get("fijo").equals("0")){//rotativo
                                        System.out.println("PERSONA ROTATIVA");
                                        es_fijo = false;
                                        tiene_horario = true;

                                        String hoy = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                                        String sql = "SELECT INICIO, FIN FROM horarios_tmp WHERE CC = ? AND fecha = ? LIMIT 1";
                                        System.out.println("[ROTATIVO] SQL: " + sql);
                                        System.out.println("[ROTATIVO] params -> CC=" + vista.txtcc_getText() + ", fecha=" + hoy);

                                        try (ConexionDb dbx = new ConexionDb()) {
                                            dbx.Conectar();
                                            try (PreparedStatement ps = dbx.getConnection().prepareStatement(sql)) {
                                                ps.setString(1, vista.txtcc_getText());
                                                ps.setString(2, hoy);
                                                try (ResultSet rs = ps.executeQuery()) {
                                                    if (rs.next()) {
                                                        String inicio_s = rs.getString("INICIO");
                                                        String fin_s    = rs.getString("FIN");

                                                        if (inicio_s == null || inicio_s.isEmpty() || fin_s == null || fin_s.isEmpty()) {
                                                            tiene_horario = false;
                                                            System.out.println("[ROTATIVO] Turno inválido (INICIO o FIN vacío).");
                                                        } else {
                                                            vista.txtHIngreso_setText(inicio_s);
                                                            vista.txtHoraSalida_setText(fin_s);
                                                            System.out.println("[ROTATIVO] Turno encontrado -> INICIO: " + inicio_s + " | FIN: " + fin_s);
                                                        }
                                                    } else {
                                                        tiene_horario = false;
                                                        System.out.println("[ROTATIVO] No hay turnos para hoy.");
                                                    }
                                                }
                                            }
                                        } catch (SQLException e) {
                                            tiene_horario = false; // ante error, mejor no asumir turno
                                            System.out.println("[ROTATIVO][ERROR] " + e.getMessage());
                                        }

                                    } else {//fijo
                                        String inicio_s = empleado.get("h_ingreso").substring(11);
                                        String fin_s = empleado.get("h_salida").substring(11);
                                        vista.txtHIngreso_setText(inicio_s);
                                        vista.txtHoraSalida_setText(fin_s);
                                        System.out.println("tiene horario de fijo");
                                    }

                                    // FIJO
                                    if (es_fijo) {
                                        System.out.println("ENTRA CONDICIONES COMO FIJO");
                                        if (tiene_horario) {
                                            System.out.println("ENTRA EL FIJO CON HORARIO");
                                            if (this.validarVale()) {
                                                vista.btnImprimir_setEnabled(true);
                                                vista.txtMensajeVale_setText("");
                                            } else {
                                                vista.txtMensajeVale_setText(this.mensaje_vale);
                                                vista.btnImprimir_setEnabled(false);
                                            } 
                                            DateFormat dateFormat3 = new SimpleDateFormat("yyyy-MM-dd");
                                            String fecha = dateFormat3.format(new Date()) + "";
                                            ArrayList<HashMap<String, String>> repuesta = n.selectAll(
                                                "SELECT * FROM empleado_tmp  WHERE CC = '" + vista.txtcc_getText() + 
                                                "' AND  fecha_castigo  > '" + fecha + " 00:00:00'");
                                            if (repuesta != null) {
                                                wseguir = false;
                                                vista.txtMensajeVale_setText(vista.txtMensajeVale_getText() + "" + " - Bloqueado por acumulados ");
                                                vista.btnImprimir_setEnabled(false);
                                            }
                                            if (vista.btnImprimir_isEnabled()) {
                                                System.out.println("******************************llego B");
                                                vista.AccionImprimirVale();
                                            }
                                        } else {
                                            System.out.println("ENTRA EL FIJO SIN HORARIO");
                                            vista.txtMensajeVale_setText("Persona fija sin horario!");
                                        }
                                    }

                                    // ROTATIVO
                                    if (!es_fijo)  {
                                        System.out.println("ENTRA CONDICIONES COMO ROTATIVO");
                                        Calendar calendario = new GregorianCalendar();
                                        int hora = calendario.get(Calendar.HOUR_OF_DAY);
                                        int minutos = calendario.get(Calendar.MINUTE);
                                        int segundos = calendario.get(Calendar.SECOND);
                                        if ((tiene_horario) && ("S".equals(wrotativo_validar_turno))) {
                                            System.out.println("ENTRA EL ROTATIVO CON HORARIO");
                                            if (this.validarVale()) {
                                                vista.btnImprimir_setEnabled(true);
                                                vista.txtMensajeVale_setText("");
                                            } else {
                                                vista.txtMensajeVale_setText(this.mensaje_vale);
                                                vista.btnImprimir_setEnabled(false);
                                            } 
                                            DateFormat dateFormat3 = new SimpleDateFormat("yyyy-MM-dd");
                                            String fecha = dateFormat3.format(new Date()) + "";
                                            ArrayList<HashMap<String, String>> repuesta = n.selectAll(
                                                "SELECT * FROM empleado_tmp  WHERE CC = '" + vista.txtcc_getText() + 
                                                "' AND  fecha_castigo  > '" + fecha + " 00:00:00'");
                                            if (repuesta != null) {
                                                wseguir = false;
                                                vista.txtMensajeVale_setText(vista.txtMensajeVale_getText() + "" + " - Bloqueado por acumulados ");
                                                vista.btnImprimir_setEnabled(false);
                                            }
                                        }
                                        if ((!tiene_horario) && ("S".equals(wrotativo_validar_turno))) {
                                            vista.btnImprimir_setEnabled(false);
                                            vista.txtMensajeVale_setText("La persona no tiene turnos registrados");
                                        }
                                        if ("N".equals(wrotativo_validar_turno)) {
                                            vista.btnImprimir_setEnabled(true);
                                            if (valido_vale.equals("0")) {
                                                vista.btnImprimir_setEnabled(false);
                                                vista.txtMensajeVale_setText("El empleado no tiene derecho a vale");
                                            }
                                            DateFormat dateFormat3 = new SimpleDateFormat("yyyy-MM-dd");
                                            String fecha = dateFormat3.format(new Date()) + "";
                                            // (ARREGLO) antes decía empleado_tmpERE
                                            ArrayList<HashMap<String, String>> repuesta = n.selectAll(
                                                "SELECT * FROM empleado_tmp WHERE CC = '" + vista.txtcc_getText() + 
                                                "' AND  fecha_castigo  > '" + fecha + " 00:00:00'");
                                            if (repuesta != null) {
                                                vista.txtMensajeVale_setText(vista.txtMensajeVale_getText() + "" + " - Bloqueado por acumulados ");
                                                vista.btnImprimir_setEnabled(false);
                                            }
                                            try{
                                                System.out.println("VALIDO SI LA PERSONA TIENE YA LE FUE IMPRESO EL VALE HOY - 2");
                                                ArrayList<HashMap<String, String>> repuestax = n.selectAll(
                                                    "SELECT * FROM eventos_tmp WHERE CC = '" + vista.txtcc_getText()+ 
                                                    "' AND fecha > '" + fecha + " 00:00:00' AND  fecha < '" + fecha + 
                                                    " 23:59:59' AND (evento ='Impresion-Vale')");
                                                if (repuestax != null) {
                                                    System.out.println("El vale de hoy ya fue impreso");
                                                    vista.txtMensajeVale_setText("El vale de hoy ya fue generado");
                                                    vista.btnImprimir_setEnabled(false);
                                                }
                                            }catch (SQLException ex){
                                                Logger.getLogger(Ingreso.class.getName()).log(Level.SEVERE, null, ex);
                                            }
                                        }
                                        if (vista.btnImprimir_isEnabled()) {
                                            System.out.println("********************************ENTROA");
                                            vista.AccionImprimirVale();
                                        }
                                    }
                                    else {System.out.println("ENTRA CONDICIONES *******");}

                                    // Registro evento Ingreso / Salida
                                    if (insertar_registro) {
                                        DateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                        String fecha2 = dateFormat2.format(new Date()) + "";
                                        HashMap<String, String> campos = new HashMap<String, String>();
                                        campos.put("fecha", fecha2);
                                        campos.put("cc", empleado.get("cc"));
                                        if (is_adentro) {
                                            vista.lblMensaje_setText("Salida");
                                            campos.put("evento", "Salida");
                                        }
                                        if (!is_adentro) {
                                            vista.lblMensaje_setText("Ingreso");
                                            campos.put("evento", "Ingreso");
                                        }
                                        //n.insertHashMap(campos, "eventos");
                                        n.insertHashMap(campos, "eventos_tmp");
                                        n.insertHashMap(campos, "eventos_sitio");    
                                        String valor = is_adentro ? "0" : "1";
                                        n.statement.executeUpdate("UPDATE empleado SET is_dentro = " + valor + " WHERE  cc = " + empleado.get("cc"));
                                        System.out.println("INSERTADO REGISTRO DE SALIDA/ENTRADA");
                                    }
                                    return;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                System.out.println(">>>" + (empleados==null?0:empleados.size()));
                System.out.println("NO");
                vista.txtMensajeVale_setText("");
                vista.lblMensaje_setForeground(Color.red);
                vista.btnImprimir_setEnabled(false);
                vista.txtcc_setText("........");
                vista.txtNombre_setText("PERSONA NO IDENTIFICADA");
                vista.txtCargo_setText("");
                vista.txtEmpresa_setText("");
                vista.txtDepartamento_setText("");
                vista.txtHIngreso_setText("");
                vista.txtHoraSalida_setText("");
                vista.txtCiudad_setText("");
                vista.txtRotativo_setText("");
                leyendoEmpleados = false;
            } catch (Exception ex) {
                System.err.println("Error: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, ex.getMessage());
            } finally {
                leyendoEmpleados = false;
                try { if (n!=null) n.close(); } catch (SQLException e) {}
            }
        }
        vista.txtTitulo_setText("");
    }

    /**
     * Finaliza el proceso de ingreso de empleados
     */
    public void closeCapture (){
        inicio.setVisible(true);
    }
    
    /**
     * Inicia el lector de huella y listeners
     */
    private void initLector() {
        System.out.println("llego inilector");
        try {
            Lector.startCapture();
        } catch (Exception e) {
            vista.closeCapture("El dispositivo NO esta listo:(T) " + e.getMessage());
        }
        System.out.println("sigo con inilector");

        Lector.addReaderStatusListener(new DPFPReaderStatusAdapter() {
            @Override
            public void readerConnected(final DPFPReaderStatusEvent e) {
                SwingUtilities.invokeLater(() -> {
                    System.out.println("Sensor Huella Digital Activado");
                    vista.EnviarTexto("Sensor Huella Digital Activado");
                });
            }
            @Override
            public void readerDisconnected(final DPFPReaderStatusEvent e) {
                SwingUtilities.invokeLater(() -> {
                    vista.closeCapture("Dispositivo Huella NO listo");
                    vista.EnviarTexto("Dispositivo Huella NO listo");
                });
            }
        });
        
        Lector.addSensorListener(new DPFPSensorAdapter() {
            @Override 
            public void fingerTouched(final DPFPSensorEvent e) {
                SwingUtilities.invokeLater(() -> {
                    System.out.println("Dedo colocado sobre el Lector");
                    vista.EnviarTexto("Dedo colocado sobre el Lector");
                    vista.pintarDedoPuesto();
                });
            }
            @Override
            public void fingerGone(final DPFPSensorEvent e) {
                SwingUtilities.invokeLater(() -> {
                    System.out.println("Dedo retirado del Lector");
                    vista.EnviarTexto("Dedo retirado del Lector");
                    vista.pintarDedonoPuesto();
                });
            }
        });

        Lector.addDataListener(new DPFPDataAdapter() {
            @Override
            public void dataAcquired(final DPFPDataEvent e) {
                SwingUtilities.invokeLater(() -> {
                    System.out.println("La Huella fue Capturada");
                    vista.EnviarTexto("La Huella fue Capturada");
                    ProcesarCaptura(e.getSample());
                });
            }
        });
    }

    /**
     * (Opcional) Impresión por Jasper – la dejé porque ya estaba; si no la usas puedes eliminarla.
     */
    public void pruebaImpresion() {
        try {
            JasperReport reporte = null;
            String path = "src\\reportes\\vale.jasper";
            Map parametros = new HashMap();
            String wbarras="79136011";
            String wqr="190.248.139.202:89\\recursos_humanos";
            parametros.put("p_nombres", wnombres);
            parametros.put("p_depto",wdepto);
            parametros.put("p_sede","Generado en: " + quesede);
            parametros.put("p_cedula",wcedula);
            parametros.put("p_empresa",vista.txtEmpresa_getText());
            parametros.put("p_vale","VALE ALMUERZO ("+wcedula+")");
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            DateFormat conse = new SimpleDateFormat("HHmmss");
            String fechaCadena = conse.format(new Date());
            wnro_valex = wnro_valex+1;
            String cantvale= Integer.toString(wnro_valex);
            parametros.put("p_barras1",widpersona+cantvale);
            parametros.put("p_fecha", "Fecha: " + dateFormat.format(new Date()));
            String wwnumero=widpersona+cantvale;

            reporte = (JasperReport) JRLoader.loadObjectFromFile(path);
            JasperPrint jprint = JasperFillManager.fillReport(reporte, parametros, new JREmptyDataSource());
            JasperPrintManager.printReport(jprint, false);

            try {
                String fecha2 = dateFormat.format(new Date()) + "";
                n.Conectar();
                HashMap<String, String> campos = new HashMap<>();
                campos.put("cc", vista.txtcc_getText());
                campos.put("evento", "Impresion-Vale");
                campos.put("fecha", fecha2);
                campos.put("impresion", wwnumero);
                campos.put("ctrl_impresion", cantvale);
                //n.insertHashMap(campos, "eventos");
                n.insertHashMap(campos, "eventos_tmp");
                n.insertHashMap(campos, "eventos_sitio");
                vista.txtMensajeVale_setText("Se ha generado un Vale!");
            } catch (SQLException ex) {
                Logger.getLogger(Ingreso.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try { n.close(); } catch (SQLException e) {}
            }
        } catch (JRException ex) {
            Logger.getLogger(Inicio.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
