
package models;

import database.ConexionDb;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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
import process.JSONTask;
import vistas.Ingreso;
import vistas.Inicio;
import vistas.NuevoUsuario;

/**
 *
 * @author Jose Luis
 * 
 * Clase encargada de la logica y de controlar el acceso a datos del proceso Captura de huellas de nuevo usuario
 * 
 */
public class NuevoUsuarioModel {
    
    
     public Inicio inicio;

    public HashMap<Integer, String> captura_h;

    private String CC = "";
    
    NuevoUsuario vista;

    /**
     * 
     * Constructor del modulo recibe instancias de interfaz grafica.
     * 
     * @param vista Instancia de la vista nuevo usuario
     * @param inicio Instancia de la vista inicio
     */
    public NuevoUsuarioModel(    NuevoUsuario vista,Inicio inicio) {
        

        this.vista = vista;
        this.inicio = inicio;
        
          captura_h = new HashMap<Integer, String>();

        
        this.inicio = inicio;
        

        
    }
    /**
     * Validacion si el vale ya fue impresa hoy para que no permita imprimir multiples vales manuales a un solo empleado
     * 
     * @return  true si ya fue impreso false si no
     */
     public boolean isImpresoValidacion() {

         HashMap<String, String> parametros = new HashMap<String, String>();

        parametros.put("cc", vista.txtcc_getText());

        JSONTask t = new JSONTask("validarVale", parametros);

        if (t.enviar(false, this)){
            
            HashMap<String, String> m = t.respuesta;

            if (m == null) {
                return false;
            }

//            int size = Integer.parseInt(m.get("size") + "");
//
//            if (size == 0) {
//                return false;
//            }
            String res = m.get("impreso");

            if (res.equals("0")) {
                return false;
            }

//            btnImprimir.setEnabled(false);
            System.out.println("El vale ya fue impreso");
            return true;
        }

        return false;
    }
    
     /**
      * 
      * Metodo para hace un merge de dos arreglos de bytes
      * 
      * @param a
      * @param b
      * @return array de bytes
      */
    public byte[] concat(byte[] a, byte[] b) {
        int aLen = a.length;
        int bLen = b.length;
        byte[] c = new byte[aLen + bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }
    
    /**
     * realiza la impresion del vale.
     */
    public void imprimirValeManual(){
    
        if(!isImpresoValidacion()){
            
            System.out.println("EL VALE NO HA SIDO IMPRESO");
            //Por el momento se permite imprimir cuantos vales manuales se quieran
            return;
        }
        
        if(vista.txtSede_getText().length() == 0){
        
            return;
        }
        ConexionDb n = new ConexionDb();
          System.out.println("Imprimiendo vale");
      
        String texto = "\r\n";
        texto += "Sede: " + vista.txtSede_getText() + "\r\n";
        texto += "Nombre: " + vista.txtNombre_getText() + "\r\n";
        texto += "Departamento: " + vista.txtDepartamento_getText() + "\r\n";

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();

        texto += "Fecha: " + dateFormat.format(date) + "\r\n";

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

        bytes = concat(bytes, corte);

        //Doc doc = new SimpleDoc(texto.getBytes(), flavor, das);
        Doc doc = new SimpleDoc(bytes, flavor, das);
        try {

            docPrintJob.print(doc, null);

        } catch (PrintException e) {

            e.printStackTrace();

        }

        try {

            DateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date2 = new Date();
            String fecha2 = dateFormat.format(date2) + "";

            n.Conectar();

            HashMap<String, String> campos = new HashMap<String, String>();

            campos.put("cc", vista.txtcc_getText());
            campos.put("evento", "Impresion-Vale-manual");

            campos.put("fecha", fecha2);

            //n.insertHashMap(campos, "eventos_tmp");

        } catch (SQLException ex) {
            Logger.getLogger(Ingreso.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                n.close();
            } catch (Exception e) {

            }
        }
        
          ConexionDb Db  = null;
        try {
/*
            
            cc
            fecha
            sede
            
            
            
*/
            HashMap<String, String> parametros = new HashMap<String, String>();

            
            DateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date2 = new Date();
            String fecha2 = dateFormat.format(date2) + "";
            
            
            Db  = new ConexionDb();
            Db.Conectar();
            HashMap<String, String> id_sede_obj = Db.selectOne("SELECT valor FROM config WHERE dato='id_sede' ");

            if (id_sede_obj != null) {
                parametros.put("sede", id_sede_obj.get("valor"));
            } else {
                parametros.put("sede", "1");
            }
            
            
            parametros.put("fecha", fecha2);

            parametros.put("cc", vista.txtcc_getText());

         
            JSONTask t = new JSONTask("setImpVale", parametros);

            if (t.enviar(false, this)) {
                System.out.print("ok Set IMPRESION VALE una");
//                return true;
            } else {
                //JOptionPane.showMessageDialog(this, "No fue posible guardar la información Intente de nuevo: " + t.mensaje_e);
//                return false;

                
                    System.out.print("++++NO HAY COMUNICACION CON EL SERVIDOR");
            }

        } catch (Exception e) {
            e.printStackTrace();
           
        }finally{
         try {
                Db.close();
                
            } catch (Exception e) {
                e.printStackTrace();
            }
         
        }
        
        
    }
    
    /**
     * 
     * Segun la cedula que se ingresa este metodo envia una peticion por Web  Services para optener los datos del usuario si existe
     * 
     */
    public void buscar(){
        
        if (vista.txtcc_getText().length() == 0) {
            return;

        }
        HashMap<String, String> parametros = new HashMap<String, String>();

        parametros.put("cc", vista.txtcc_getText());

        JSONTask t = new JSONTask("getEmpleado", parametros);

        if (t.enviar(true, this)) {
            HashMap<String, String> res = t.respuesta;
            String existe = res.get("existe");

            if (existe.equals("0")) {
                JOptionPane.showMessageDialog(vista, "No existe el empleado");
                vista.disableHuellas();
                vista.disableGuardar();
                vista.vaciarHuella();

                vista.txtNombre_setText("");

                vista.txtDepartamento_setText("");
                vista.txtSede_setText("");
                vista.txtCiudad_setText("");
                vista.txtHoraSalida_setText("");
                vista.txtEmpresa_setText("");
                vista.txtHoraIngreso_setText("");
                vista.txtCargo_setText("");
                vista.txtFijo_setText("");

            } else {

                CC = vista.txtcc_getText();
                vista.disableGuardar();
                vista.enableHuellas();
                vista.vaciarHuella();

                vista.txtNombre_setText(res.get("NOMBRE") + " " + res.get("APELLIDO"));

                vista.txtDepartamento_setText(res.get("NOMBRE_AREA"));
                vista.txtSede_setText(res.get("NOMBRE_SEDE"));
                vista.txtCiudad_setText(res.get("NOMBRE_CIUDAD"));
                vista.txtHoraSalida_setText(res.get("HORA_FIN"));
                vista.txtEmpresa_setText(res.get("NOMBRE_EMPRESA"));
                vista.txtHoraIngreso_setText(res.get("HORA_INICIO"));
                vista.txtCargo_setText(res.get("NOMBRE_CARGO"));
                vista.txtFijo_setText(res.get("FIJO").equals("1") ? "Si" : "No");
                vista.txtCambio_setText(res.get("update_huellas"));

                vista.validarHuellasGet(res);

            }
        }
    }
    
    /**
     * Una vez caturadas las huellas este metodo envia una peticion por Web Services para
     * almacenar en e servidor las nuevas huellas
     * 
     * @return true si se guarda exitosamente, false si no.
     */
    public boolean Guardar(){

        
        HashMap<String, String> parametros = new HashMap<String, String>();

        parametros.put("COD_EMPLEADO", this.CC);

        parametros.put("cc", vista.txtcc_getText());

        Iterator it = captura_h.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            System.out.println("Captura Huellas: "+"h"+pair.getKey() + " = " + pair.getValue());
            parametros.put("h" + pair.getKey(), pair.getValue() + "");
            
        }
        
        JSONTask t = new JSONTask("setHuellas", parametros);

        if(t.enviar(false, this)){
                System.out.print("ok");
                return true;
        }else{
            System.out.print("FALLO REGISTRO HUELLAS");
            JOptionPane.showMessageDialog(vista, "No fue posible guardar la información Intente de nuevo: " + t.mensaje_e);
            return false;
        }
    }
}
