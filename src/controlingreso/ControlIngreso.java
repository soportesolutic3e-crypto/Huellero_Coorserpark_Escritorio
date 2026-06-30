/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controlingreso;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Enumeration;
import vistas.Inicio;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import process.LicenciaErrorDialog;
import process.LicenciaManager;
import process.Sincronizar;


/**
 *
 * 
 * 
 * Clase responsable de inicializar la aplicacion.
 * 
 */
public class ControlIngreso {

    /**
     * @param args the command line arguments
     */
    private static ServerSocket SERVER_SOCKET;
            

    
    public static void main(String[] args) throws UnknownHostException, SocketException {
        // TODO code application logic here
                
        try {
            com.formdev.flatlaf.FlatLightLaf.setup();
            javax.swing.UIManager.put("Component.accentColor", new java.awt.Color(0x1A, 0x27, 0x44));
            javax.swing.UIManager.put("Button.arc", 10);
            javax.swing.UIManager.put("Component.arc", 8);
            javax.swing.UIManager.put("TextComponent.arc", 6);
            javax.swing.UIManager.put("ScrollBar.thumbArc", 999);
            javax.swing.UIManager.put("ScrollBar.width", 8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        //inicio
       
        //termino
         
        
        // ── Validación de licencia ────────────────────────────────────────────
        LicenciaManager.Resultado lic = LicenciaManager.verificar();
        if (!lic.ok) {
            new LicenciaErrorDialog(lic).setVisible(true);
            return;
        }
        // ─────────────────────────────────────────────────────────────────────

        Inicio n=  new Inicio();
        
    try {
        SERVER_SOCKET = new ServerSocket(1134);
        //JOptionPane.showMessageDialog(null,"Entrado");
        n.setIconImage(
        new ImageIcon(Inicio.class.getResource("icon_frame.png")).getImage());
        n.setVisible(true);
        try {
            /**procedimiento para sincronizar la base principal del huellero**/
            /*Oculto el panel mientras sincroniza*/
            n.ocultar_opcion1();
            new Sincronizar(n);
        } catch (Exception e) {
            e.printStackTrace();
        }
       }
        
        catch (IOException e) {
           JOptionPane.showMessageDialog(null,"La aplicación se encuentra en funcionamiento!");
            System.exit(0);
             
        }
        
        
    }
/************************************************************************************************************************/


/**************************************************************************************************************************/    
    
    
    
}