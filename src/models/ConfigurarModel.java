package models;

import database.ConexionDb;
import database.Db;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import vistas.Configurar;
import vistas.Inicio;

/**
 * Lógica para la configuración de sede e IP.
 */
public class ConfigurarModel {

    // nombreSede -> id_sede
    private final Map<String, String> nombreToId = new HashMap<>();
    // id_sede -> nombreSede
    private final Map<String, String> idToNombre = new HashMap<>();

    private final Configurar vista;
    // Debe ser público porque la vista la accede directamente.
    public final Inicio vistaPrincipal;

    public ConfigurarModel(Configurar vista, Inicio vistaPrincipal) {
        if (vista == null) throw new IllegalArgumentException("vista no puede ser null");
        if (vistaPrincipal == null) throw new IllegalArgumentException("vistaPrincipal no puede ser null");

        this.vista = vista;
        this.vistaPrincipal = vistaPrincipal;

        cargarSedesYSeleccion();
    }

    private void cargarSedesYSeleccion() {
        vista.jbxSedes_removeAllItems();
        nombreToId.clear();
        idToNombre.clear();

        ConexionDb d = new ConexionDb();
        try {
            d.Conectar();

            // 1) Cargar sedes
            ArrayList<HashMap<String, String>> sedes = d.selectAll("SELECT * FROM sedes_tmp");
            if (sedes != null) {
                for (HashMap<String, String> sede : sedes) {
                    String id = safeGet(sede, "id_sede");
                    String nombre = safeGet(sede, "sede");
                    if (!id.isEmpty() && !nombre.isEmpty()) {
                        nombreToId.put(nombre, id);
                        idToNombre.put(id, nombre);
                        vista.jbxSedes_addItem(nombre);
                    }
                }
            }
            /*  VERSION 21 ORIGINAL
            // 2) IP actual
            String ipActual = "999.999.999.999:99";
            HashMap<String, String> resIp = d.selectOne("SELECT valor FROM config WHERE dato='ip'");
            if (resIp != null) {
                String v = resIp.get("valor");
                if (v != null && !v.trim().isEmpty()) ipActual = v.trim();
            }
            vista.txtip_setText(ipActual);
            */
            
            // VERSION V23 15-09-2026
            // 2) IP actual
            String ipActual = "999.999.999.999:99";
            HashMap<String, String> resIp = d.selectOne("SELECT valor FROM config WHERE dato='ip'");
            if (resIp != null) {
                String v = resIp.get("valor");
                if (v != null && !v.trim().isEmpty()) ipActual = v.trim();
            }
            vista.txtip_setText(ipActual);

            // 2b) IP alterna actual (opcional, puede estar vacía)
            String ipAlternaActual = "";
            HashMap<String, String> resIpAlt = d.selectOne("SELECT valor FROM config WHERE dato='ip_alterna'");
            if (resIpAlt != null && resIpAlt.get("valor") != null) {
                ipAlternaActual = resIpAlt.get("valor").trim();
            }
            vista.txtipalt_setText(ipAlternaActual);

            // 3) Sede seleccionada
            HashMap<String, String> resSede = d.selectOne("SELECT valor FROM config WHERE dato='id_sede'");
            if (resSede != null) {
                String idSede = safeGet(resSede, "valor");
                String nombreSede = idToNombre.get(idSede);
                if (nombreSede != null) {
                    vista.jbxSedes_setSelectedItem(nombreSede);
                } else {
                    // Seleccionar la primera disponible (si hay)
                    if (!nombreToId.isEmpty()) {
                        String firstName = nombreToId.keySet().iterator().next();
                        vista.jbxSedes_setSelectedItem(firstName);
                    }
                }
            } else {
                // No hay config previa: selecciona primera (si hay)
                if (!nombreToId.isEmpty()) {
                    String firstName = nombreToId.keySet().iterator().next();
                    vista.jbxSedes_setSelectedItem(firstName);
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            try { d.close(); } catch (Exception ignore) {}
        }
    }

    /**
     * Guarda la configuración elegida en la vista.
     * @param nombreSede nombre como aparece en el combo.
     */
    public void guardar(String nombreSede) throws SQLException {
        if (nombreSede == null || nombreSede.trim().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar una sede válida.");
        }

        String idSede = nombreToId.get(nombreSede);
        if (idSede == null || idSede.trim().isEmpty()) {
            throw new IllegalArgumentException("Sede no válida: " + nombreSede);
        }

        String ip = vista.txtip_getText();
        if (ip == null) ip = "";
        ip = ip.trim();

        ConexionDb d = new ConexionDb();
        try {
            d.Conectar();

            /* VERSION 21 ORIGINAL
            // id_sede
            d.getStatement().executeUpdate("DELETE FROM config WHERE dato='id_sede'");
            HashMap<String, String> camposSede = new HashMap<>();
            camposSede.put("dato", "id_sede");
            camposSede.put("valor", idSede);
            Db.insertHashMap(camposSede, "config", d.getStatement());

            // ip
            d.getStatement().executeUpdate("DELETE FROM config WHERE dato='ip'");
            HashMap<String, String> camposIp = new HashMap<>();
            camposIp.put("dato", "ip");
            camposIp.put("valor", ip);
            Db.insertHashMap(camposIp, "config", d.getStatement()); */
            
            
            d.getStatement().executeUpdate("DELETE FROM config WHERE dato='id_sede'");
            HashMap<String, String> camposSede = new HashMap<>();
            camposSede.put("dato", "id_sede");
            camposSede.put("valor", idSede);
            Db.insertHashMap(camposSede, "config", d.getStatement());

            
            // VERSION 23 15-09-2026
            // ip
            d.getStatement().executeUpdate("DELETE FROM config WHERE dato='ip'");
            HashMap<String, String> camposIp = new HashMap<>();
            camposIp.put("dato", "ip");
            camposIp.put("valor", ip);
            Db.insertHashMap(camposIp, "config", d.getStatement());

            // ip_alterna (opcional)
            String ipAlterna = vista.txtipalt_getText();
            if (ipAlterna == null) ipAlterna = "";
            ipAlterna = ipAlterna.trim();

            d.getStatement().executeUpdate("DELETE FROM config WHERE dato='ip_alterna'");
            HashMap<String, String> camposIpAlt = new HashMap<>();
            camposIpAlt.put("dato", "ip_alterna");
            camposIpAlt.put("valor", ipAlterna);
            Db.insertHashMap(camposIpAlt, "config", d.getStatement());

        } finally {
            try { d.close(); } catch (Exception ignore) {}
        }
    }

    private static String safeGet(Map<String, String> map, String key) {
        if (map == null) return "";
        String v = map.get(key);
        return v == null ? "" : v.trim();
    }
}
