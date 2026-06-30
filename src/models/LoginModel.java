package models;

import database.ConexionDb;
import java.sql.SQLException;
import java.util.HashMap;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import process.JSONTask;
import vistas.Inicio;
import vistas.Login;
import vistas.NuevoUsuario;

/**
 * Modelo para la pantalla de Login.
 * Asegura que exista una clase LoginModel (antes este archivo
 * contenía por error otra clase pública).
 */
public class LoginModel {

    public final Login vista;
    public final Inicio inicio;

    private final ConexionDb db;

    public LoginModel(Login vista, Inicio inicio) {
        this.vista = vista;
        this.inicio = inicio;
        this.db = new ConexionDb();
    }

    /**
     * Autentica contra una tabla local.
     * Ajusta el nombre de la tabla/campos según tu BD (usuarios_tmp/usuarios).
     * Devuelve true si las credenciales son válidas.
     */
    public boolean autenticar(String usuario, String clave) throws SQLException {
        try {
            db.Conectar();
            // Ajusta a tu esquema real (tabla/columnas). Aquí: usuarios_tmp(usuario, clave)
            String q = "SELECT * FROM usuarios_tmp WHERE usuario = '" + escape(usuario)
                     + "' AND clave = '" + escape(clave) + "' LIMIT 1";
            HashMap<String, String> row = db.selectOne(q);
            return row != null;
        } finally {
            try { db.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * Flujo que la vista invoca sin parámetros:
     * - Lee usuario/clave desde la vista (probando varios getters comunes).
     * - Llama autenticar().
     * - Si OK: cierra Login y muestra Inicio.
     */
    public void ingresar_cambio() {
        try {
            String usuario = leerTextoVista(
                "txtUsuario_getText", "getUsuario", "getTxtUsuario", "usuario_getText", "txtusuario_getText"
            );
            String clave = leerTextoVista(
                "txtClave_getText", "getClave", "getTxtClave", "clave_getText", "txtclave_getText", "getPasswordText"
            );

            if (usuario == null || usuario.isEmpty() || clave == null || clave.isEmpty()) {
                JOptionPane.showMessageDialog(vista, "Debes ingresar usuario y clave.");
                return;
            }

            if (autenticar(usuario, clave)) {
                // Éxito: cerrar login y abrir inicio
                cerrarLogin();
                mostrarInicio();
            } else {
                JOptionPane.showMessageDialog(vista, "Usuario o clave inválidos.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(vista, "Error al autenticar: " + e.getMessage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(vista, "No pude leer los campos de la vista: " + e.getMessage());
        }
    }

    
    public void ingresar() {
        HashMap<String, String> parametros = new HashMap<String, String>();

        parametros.put("usuario", vista.txtUser_getText());
        parametros.put("clave", vista.txtPass_getText());
        
        
             //   txtPass_getText());

        JSONTask t = new JSONTask("loginApp", parametros);

        if (t.enviar(true, this)){
            
            HashMap<String, String> res = t.respuesta;

            String acceso = res.get("acceso");

            if (acceso.equals("0")) {
                JOptionPane.showMessageDialog(vista, "Credenciales incorrectas");
            } else {
                NuevoUsuario nu = new NuevoUsuario(inicio);

                nu.setIconImage(
                        new ImageIcon(Inicio.class.getResource("icon_frame.png")).getImage()
                );

                //nu.show();
                nu.setVisible(true);
                
                vista.setVisible(false);
            }
        }
    }
    
    /** Muestra la ventana de Inicio (si tu flujo lo requiere). */
    public void mostrarInicio() {
        if (inicio != null) {
            inicio.setVisible(true);
        }
    }

    /** Cierra/oculta la vista de Login (si tu flujo lo requiere). */
    public void cerrarLogin() {
        if (vista != null) {
            vista.setVisible(false);
            vista.dispose();
        }
    }

    // Utilidad ultra simple para evitar inyección en este contexto local.
    // Si tu ConexionDb ya usa PreparedStatement, migra a eso y elimina este escape.
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("'", "''").trim();
    }

    /**
     * Intenta invocar, mediante reflexión, el primer método existente en la vista
     * de la lista dada y devuelve su String.
     * Métodos típicos que probamos: txtUsuario_getText, getUsuario, getTxtUsuario, etc.
     */
    private String leerTextoVista(String... posiblesMetodos) throws Exception {
        for (String m : posiblesMetodos) {
            try {
                java.lang.reflect.Method method = vista.getClass().getMethod(m);
                Object val = method.invoke(vista);
                if (val != null) {
                    String s = val.toString();
                    return s != null ? s.trim() : null;
                }
            } catch (NoSuchMethodException ignored) {
                // probar siguiente
            }
        }
        // Si ninguno existe, lanzamos una excepción para avisar claramente
        throw new Exception("No encontré getters compatibles en la vista (probé: "
                + String.join(", ", posiblesMetodos) + ").");
    }
}
