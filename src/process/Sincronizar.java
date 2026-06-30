package process;

import database.Db;
import vistas.Inicio;
import vistas.IngresoTarjeta;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
// import java.sql.Date;  // <- no se usa, se elimina
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.JOptionPane;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.atomic.AtomicBoolean;

public class Sincronizar implements Runnable {

    private Thread hilo;
    private volatile boolean running = true;

    private String folder = "";
    private String ruta = "";
    private final String dbName = "datosv7.sqlite";

    private Inicio inicio;
    private IngresoTarjeta ingreso; // si no se usa, puedes eliminarlo

    public static volatile int wsinconexion = 0; // 0 = OK, !=0 error
    public static volatile String wjson = "";
    public volatile int wwsinconexion = 0;
    public static volatile String registroaccion;
    
    /* =========================================================
        BLOQUEO COMPARTIDO PARA ENVÍO DE EVENTOS
        ---------------------------------------------------------
        Evita que al mismo tiempo se ejecute:
        - sincroEnviarEventos()
        - enviarEventoServidorCentral()
     ========================================================= */
     public static final AtomicBoolean lockEnvioEventos = new AtomicBoolean(false);


    private String wqueversion = "21.0.0";
    private String wwclave = "";

    public String wsino_red = "";

    public Sincronizar() {
        String home = System.getProperty("user.home");
        this.folder = home;
        this.ruta = home + File.separator + "huapp" + File.separator + dbName;
    }

    public Sincronizar(Inicio inicio) {
        this();
        this.inicio = inicio;
        this.ingreso = null;
        registroaccion = "INICIO HUELLERO";
        verifiExistDatabase();
        hilo = new Thread(this, "Sincronizar-Thread");
        hilo.setDaemon(true);
        hilo.start();
    }

    // Helper: ejecutar cambios de UI en el hilo de Swing (EDT)
    private void ui(Runnable r) {
        try {
            javax.swing.SwingUtilities.invokeLater(r);
        } catch (Throwable ignore) {}
    }

    @Override
    public void run() {
        final String url = "jdbc:sqlite:" + ruta;

        while (running) {
            try {
                wsinconexion = 0;
                if (inicio != null) ui(() -> inicio.inicioMensaje("*** Sincronizando, Por favor esperar ***"));

                try (Connection cn = java.sql.DriverManager.getConnection(url)) {
                    cn.setAutoCommit(false);
                    try (Statement st = cn.createStatement()) {
                        st.setQueryTimeout(30);

                        // --- pipeline de sincronización automática ---
                        sincroInicio(st);                          cn.commit();
                        if (ok()) { sincroSedes_app(st);                   cn.commit(); }
                        if (ok()) { ActualizarVersion(st);                 cn.commit(); }
                        if (ok()) { ActualizarMensaje1(st);                cn.commit(); } // SIN WS
                        if (ok()) { ActualizarImprimirVales(st);           cn.commit(); }
                        if (ok()) { sincroEnviarEventos(st);        cn.commit(); }
                        if (ok()) { sincroEmpleados_jhcm(st);              cn.commit(); }
                        if (ok()) { sincroHorarios(st);                    cn.commit(); }
                        if (ok()) { sincroPermisos(st);                    cn.commit(); }
                        if (ok()) { sincroDescargaEventos(st);             cn.commit(); }
                        if (ok()) { sincroEnviarEventos_backup(st);        cn.commit(); }
                        if (ok()) { sincroActividad_version(st, "SINCRONIZACION AUTOMATICA"); cn.commit(); }
                        if (ok()) { sincronizar_consulta(st);              cn.commit(); }

                        verificarDatosEmpleados(st);               cn.commit();
                        marcarUltimaSincronizacion(st, cn);        // hace commit interno
                        verificarVersiones(st);                    cn.commit();
                    }
                } catch (Exception ex) {
                    wsinconexion = 1;
                    wjson = "Error de sincronización: " + ex.getMessage();
                    ex.printStackTrace();
                }

                if (inicio != null) {
                    if (ok()) {
                        ui(() -> { 
                            inicio.inicioMensaje("*** Dispositivo Listo ***"); 
                            inicio.mostrar_opcion1(); 
                        });
                    } else {
                        // wjson ya contiene el último mensaje de error significativo (incluye 404 detallado de JSONTask)
                        ui(() -> { 
                            inicio.inicioMensaje(wjson);  // sin prefijo "Error: " para no duplicar
                            inicio.mostrar_opcion1(); 
                        });
                    }
                }

                // espera entre ciclos
                sleepMinutes(25);

            } catch (Throwable t) {
                t.printStackTrace();
                sleepMinutes(5);
            }
        }
    }

    public void stop() {
        running = false;
        if (hilo != null) hilo.interrupt();
    }

    // ------------------------------------------------------------------------------------
    // Ejecución manual pública
    // ------------------------------------------------------------------------------------
    public void runManual() {
        final String url = "jdbc:sqlite:" + ruta;
        wsinconexion = 0;
        wwsinconexion = 0;
        wsino_red = "SI";

        try (Connection cn = java.sql.DriverManager.getConnection(url)) {
            cn.setAutoCommit(false);
            try (Statement st = cn.createStatement()) {
                st.setQueryTimeout(30);

                // --- pipeline de sincronización manual ---
                
                sincroInicio(st);                                  cn.commit();
                if (ok()) { sincroSedes_app(st);                   cn.commit(); }
                if (ok()) { ActualizarVersion(st);                 cn.commit(); }
                if (ok()) { ActualizarMensaje1(st);                cn.commit(); } // SIN WS
                if (ok()) { ActualizarImprimirVales(st);           cn.commit(); }
                if (ok()) { sincroEnviarEventos(st);        cn.commit(); }
                if (ok()) { sincroEmpleados_jhcm(st);              cn.commit(); }
                if (ok()) { sincroHorarios(st);                    cn.commit(); }
                if (ok()) { sincroPermisos(st);                    cn.commit(); }
                if (ok()) { sincroDescargaEventos(st);      cn.commit(); }
                if (ok()) { sincroEnviarEventos_backup(st);        cn.commit(); }
                if (ok()) { sincroActividad_version(st, "SINCRONIZACION MANUAL"); cn.commit(); }
                if (ok()) { sincronizar_consulta(st);              cn.commit(); }

                verificarDatosEmpleados(st);               cn.commit();
                marcarUltimaSincronizacion(st, cn);
                verificarVersiones(st);                    cn.commit();
                
            }
        } catch (Exception e) {
            wsinconexion = 1;
            wjson = "Error manual: " + e.getMessage();
            e.printStackTrace();
        } finally {
            wwsinconexion = wsinconexion;
        }
    }

    // ------------------------------------------------------------------------------------
    // DB inicial
    // ------------------------------------------------------------------------------------
    public boolean verifiExistDatabase() {
        File f = new File(folder + File.separator + "huapp" + File.separator + dbName);
        if (!f.exists()) {
            CrearBase();
            return false;
        }
        if (inicio != null) inicio.detallesede = "LA MEJOR SEDE";
        return true;
    }

    private void CrearBase() {
        int len = 50;
        wwclave = generateRandomPasswordApp(len);

        try {
            Path path = Paths.get(folder, "huapp");
            Files.createDirectories(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        final String url = "jdbc:sqlite:" + ruta;

        try (Connection conn = java.sql.DriverManager.getConnection(url)) {
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                System.out.println("SQLite driver: " + meta.getDriverName());
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        try (Connection cn = java.sql.DriverManager.getConnection(url)) {
            cn.setAutoCommit(false);
            try (Statement st = cn.createStatement()) {
                st.setQueryTimeout(30);

                ensureTable(cn, st, "empleado",
                        "CREATE TABLE empleado(" +
                                "cc TEXT PRIMARY KEY NOT NULL," +
                                "nombre TEXT,cargo TEXT,empresa TEXT,sede TEXT,area TEXT," +
                                "h_ingreso TEXT,h_salida TEXT,ciudad TEXT,fijo TEXT,ultimo_cambio TEXT," +
                                "h1 TEXT,h2 TEXT,h3 TEXT,h4 TEXT,h5 TEXT,h6 TEXT,h7 TEXT,h8 TEXT,h9 TEXT,h10 TEXT," +
                                "is_dentro TEXT,activo TEXT,vacaciones TEXT,derecho_vale TEXT,id_persona TEXT," +
                                "cond1 TEXT,cond2 TEXT,cond3 TEXT,cond4 TEXT,nro_vale TEXT,fecha_castigo DATETIME,tarjeta TEXT)"
                );

                ensureTable(cn, st, "empleado_tmp",
                        "CREATE TABLE empleado_tmp(" +
                                "cc TEXT PRIMARY KEY NOT NULL," +
                                "nombre TEXT,cargo TEXT,empresa TEXT,sede TEXT,area TEXT," +
                                "h_ingreso TEXT,h_salida TEXT,ciudad TEXT,fijo TEXT,ultimo_cambio TEXT," +
                                "h1 TEXT,h2 TEXT,h3 TEXT,h4 TEXT,h5 TEXT,h6 TEXT,h7 TEXT,h8 TEXT,h9 TEXT,h10 TEXT," +
                                "is_dentro TEXT,activo TEXT,vacaciones TEXT,derecho_vale TEXT,id_persona TEXT," +
                                "cond1 TEXT,cond2 TEXT,cond3 TEXT,cond4 TEXT,nro_vale TEXT,fecha_castigo DATETIME,tarjeta TEXT)"
                );

                ensureTable(cn, st, "config",
                        "CREATE TABLE config(dato TEXT PRIMARY KEY NOT NULL UNIQUE, valor TEXT)"
                );

                ensureTable(cn, st, "horarios",
                        "CREATE TABLE horarios(CC TEXT, INICIO TEXT, FIN TEXT, FECHA TEXT, \"UPDATE\" TEXT)"
                );
                ensureTable(cn, st, "horarios_tmp",
                        "CREATE TABLE horarios_tmp(CC TEXT, INICIO TEXT, FIN TEXT, FECHA TEXT, \"UPDATE\" TEXT)"
                );

                ensureTable(cn, st, "permisos",
                        "CREATE TABLE permisos(CC TEXT, FECHA_INICIO TEXT, FECHA_FIN TEXT)"
                );
                ensureTable(cn, st, "permisos_tmp",
                        "CREATE TABLE permisos_tmp(CC TEXT, FECHA_INICIO TEXT, FECHA_FIN TEXT)"
                );

                ensureTable(cn, st, "sedes",
                    "CREATE TABLE sedes(" +
                        "id_sede TEXT, " +
                        "sede TEXT, " +
                        "backup TEXT, " +
                        "backup_ini DATE, " +
                        "backup_ter DATE, " +
                        "versionactualapp TEXT, " +
                        "mensaje1 TEXT, " +
                        "mensaje2 TEXT, " +
                        "imprimir_vales TEXT DEFAULT 'NO'" +
                    ")"
                );

                // sedes_tmp con create-if-not-exists y fila inicial
                if (!tableExists(cn, "sedes_tmp")) {
                   st.executeUpdate(
                        "CREATE TABLE sedes_tmp(" +
                            "id_sede TEXT," +
                            "sede TEXT," +
                            "backup TEXT," +
                            "backup_ini DATE," +
                            "backup_ter DATE," +
                            "versionactualapp TEXT," +
                            "mensaje1 TEXT," +
                            "mensaje2 TEXT," +
                            "imprimir_vales TEXT DEFAULT 'NO'" +
                        ")"
                    ); 
                    st.executeUpdate(
                            "INSERT INTO sedes_tmp(id_sede,sede,backup,versionactualapp,imprimir_vales) " +
                                    "VALUES ('1','Centro','N','21.0.0','NO')"
                    );
                }

                ensureTable(cn, st, "eventos",
                        "CREATE TABLE eventos(" +
                                "cc TEXT, fecha DATETIME, evento TEXT, " +
                                "impresion INTEGER DEFAULT 0, ctrl_impresion INTEGER DEFAULT 0, " +
                                "forma TEXT DEFAULT 'H', sync INTEGER DEFAULT 0)"
                );
                ensureTable(cn, st, "eventos_tmp",
                        "CREATE TABLE eventos_tmp(" +
                                "cc TEXT, fecha DATETIME, evento TEXT, " +
                                "impresion INTEGER DEFAULT 0, ctrl_impresion INTEGER DEFAULT 0, " +
                                "forma TEXT DEFAULT 'H', sync INTEGER DEFAULT 0)"
                );
                ensureTable(cn, st, "eventos_sitio",
                        "CREATE TABLE eventos_sitio(" +
                                "cc TEXT, fecha DATETIME, evento TEXT, " +
                                "impresion INTEGER DEFAULT 0, ctrl_impresion INTEGER DEFAULT 0, " +
                                "forma TEXT DEFAULT 'H', sync INTEGER DEFAULT 0)"
                );

                // Config inicial
                ensureConfigKV(st, "sede", "Centro");
                ensureConfigKV(st, "id_sede", "1");
                ensureConfigKV(st, "etapa", "1");
                ensureConfigKV(st, "ultima_s", "");
                ensureConfigKV(st, "hash", "");
                ensureConfigKV(st, "hash_horarios", "");
                ensureConfigKV(st, "hash_permisos", "");
                ensureConfigKV(st, "sinc_max_evento", "0");
                ensureConfigKV(st, "sinc_etapa", "1");
                ensureConfigKV(st, "versionactualapp", "21.0.0");
                ensureConfigKV(st, "versioninstalacion", "21.0.0");
                ensureConfigKV(st, "serieapp", wwclave);
                ensureConfigKV(st, "mensaje1", " ");
                ensureConfigKV(st, "imprimir_vales", "NO");
                

                st.executeUpdate(
                        "INSERT INTO sedes(id_sede,sede,backup,versionactualapp) " +
                                "SELECT '1','Centro','N','21.0.0' " +
                                "WHERE NOT EXISTS (SELECT 1 FROM sedes WHERE id_sede='1')"
                );

                cn.commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------------------------------
    // Sincronización
    // ------------------------------------------------------------------------------------
    public void sincroInicio(Statement st) throws SQLException {
        st.executeUpdate("DELETE FROM config WHERE dato='sinc_etapa'");
        HashMap<String, String> m = new HashMap<>();
        m.put("dato", "sinc_etapa");
        m.put("valor", "1");
        Db.insertHashMap(m, "config", st);
    }

    public void sincroSedes_app(Statement st) throws SQLException {
        HashMap<String, String> params = new HashMap<>();
        params.put("para1", "1");
        JSONTask t = new JSONTask("getSedes_app01", params);
        if (!t.enviar(false, this)) {
            wsError(t, "Sedes", st);
            return;
        }

        HashMap<String, String> m = t.respuesta;
        if (m == null) return;

        int size = parseIntSafe(m.get("size"), 0);
        st.executeUpdate("DELETE FROM sedes");
        st.executeUpdate("DELETE FROM sedes_tmp");

        String keys = m.get("keys");
        String[] keys_a = (keys != null ? keys.split(",")
                : new String[]{"id_sede","sede","backup","backup_ini","backup_ter",
                               "versionactualapp","mensaje1","mensaje2"});

        for (int i = 0; i < size; i++) {
            HashMap<String, String> row = new HashMap<>();
            for (String k : keys_a) {
                if ("hash".equals(k)) continue;
                row.put(k, m.get(i + "_" + k));
            }
            Db.insertHashMap(row, "sedes", st);
            Db.insertHashMap(row, "sedes_tmp", st);
        }
        st.executeUpdate("UPDATE config set valor=1 where dato='sinc_etapa' and valor>0");
    }

    public void ActualizarVersion(Statement st) throws SQLException {
        String idSede = readConfig(st, "id_sede", "1");
        System.out.println("DEBUG ID SEDE LOCAL: " + idSede);

        try (ResultSet rs = st.executeQuery(
                "SELECT versionactualapp FROM sedes_tmp WHERE id_sede='" + esc(idSede) + "' LIMIT 1")) {
            if (rs.next()) {
                String v = rs.getString(1);
                System.out.println("DEBUG VERSION EN sedes_tmp PARA LA SEDE " + idSede + ": " + v);

                if (v == null || v.trim().isEmpty()) v = wqueversion;
                upsertConfig(st, "versionactualapp", v);

                HashMap<String, String> dbgVer = Db.selectOne(
                    "SELECT valor FROM config WHERE dato='versionactualapp'", st
                );
                System.out.println("DEBUG versionactualapp GUARDADA EN config: " +
                    (dbgVer != null ? dbgVer.get("valor") : "NULL"));
            } else {
                System.out.println("DEBUG: no se encontró registro en sedes_tmp para id_sede=" + idSede);
            }
        }
    }

    // SIN WS
    public void ActualizarMensaje1(Statement st) throws SQLException {
        String idSede = readConfig(st, "id_sede", "1");
        String msg = null;

        try (ResultSet rs = st.executeQuery(
                "SELECT mensaje1 FROM sedes_tmp WHERE id_sede='" + esc(idSede) + "' LIMIT 1")) {
            if (rs.next()) msg = rs.getString(1);
        }
        if (msg == null || msg.trim().isEmpty()) {
            try (ResultSet rs = st.executeQuery(
                    "SELECT mensaje1 FROM sedes WHERE id_sede='" + esc(idSede) + "' LIMIT 1")) {
                if (rs.next()) msg = rs.getString(1);
            }
        }
        if (msg == null) msg = " ";
        upsertConfig(st, "mensaje1", msg);
    }
/*************************************************************************************************************************/
//SIN WS
    public void ActualizarImprimirVales(Statement st) throws SQLException {
        String idSede = readConfig(st, "id_sede", "1");
        String val = null;

        System.out.println("DEBUG ActualizarImprimirVales -> idSede = " + idSede);

        try (ResultSet rs = st.executeQuery(
                "SELECT imprimir_vales FROM sedes_tmp WHERE id_sede='" + esc(idSede) + "' LIMIT 1")) {

            if (rs.next()) {
                val = rs.getString(1);
                System.out.println("DEBUG sedes_tmp.imprimir_vales = [" + val + "]");
            } else {
                System.out.println("DEBUG No existe fila en sedes_tmp para id_sede=" + idSede);
            }
        }

        if (val == null || val.trim().isEmpty()) {
            val = "NO";
        } else {
            val = val.trim().toUpperCase();
        }

        System.out.println("DEBUG valor final a guardar en config.imprimir_vales = [" + val + "]");

        upsertConfig(st, "imprimir_vales", val);

        HashMap<String, String> dbg = Db.selectOne(
                "SELECT valor FROM config WHERE dato='imprimir_vales'", st);

        System.out.println("DEBUG config.imprimir_vales guardado = [" +
                (dbg != null ? dbg.get("valor") : "NULL") + "]");
    }
    
/***************************************************************************************************************************/    
    
    
    /*********************************************************************************************************************/
   /** Envía eventos locales pendientes en lote usando setEventos_jhcm_ht */
    /** Envía eventos locales pendientes en lote usando setEventos_jhcm_ht */
    public void sincroEnviarEventos(Statement statement) throws SQLException {

        if (!lockEnvioEventos.compareAndSet(false, true)) {
            System.out.println("sincroEnviarEventos: ya hay un envío de eventos en curso, se omite este ciclo.");
            return;
        }

        try {
            String selectSql = "SELECT * FROM eventos_tmp WHERE sync = 0 ORDER BY fecha";
            System.out.println("sincroEnviarEvento ..... Datos de eventos..... " + selectSql);

            java.util.List<java.util.HashMap<String, String>> eventos = Db.selectAll(selectSql, statement);

            if (eventos == null || eventos.isEmpty()) {
                statement.executeUpdate("UPDATE config SET valor = 2 WHERE dato = 'sinc_etapa' AND valor <> 0");
                return;
            }

            java.util.List<String> eventosFormateados = new java.util.ArrayList<>();

            for (java.util.HashMap<String, String> evento : eventos) {
                java.util.List<String> pares = new java.util.ArrayList<>();
                for (java.util.Map.Entry<String, String> entry : evento.entrySet()) {
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
                String eventoStr = "[" + String.join(":::::", pares) + "]";
                eventosFormateados.add(eventoStr);
            }

            String dataCompleta = "[[" + String.join(";;;;;", eventosFormateados) + "]]";

            java.util.HashMap<String, String> parametros3 = new java.util.HashMap<>();
            parametros3.put("info", dataCompleta);

            java.util.HashMap<String, String> idSedeObj =
                    Db.selectOne("SELECT valor FROM config WHERE dato = 'id_sede'", statement);
            String idSede = (idSedeObj != null && idSedeObj.containsKey("valor"))
                    ? idSedeObj.get("valor")
                    : "1";
            parametros3.put("id_sede", idSede);

            JSONTask t3 = new JSONTask("setEventos_jhcm_ht", parametros3);
            System.out.println(".....Enviando eventos al Servidor Principal.....");

            boolean envioExitoso = false;
            try {
                envioExitoso = t3.enviar(false, null);
            } catch (Exception e) {
                System.err.println("Error al enviar eventos: " + e.getMessage());
                envioExitoso = false;
            }

            if (envioExitoso) {
                int updated = statement.executeUpdate("UPDATE eventos_tmp SET sync = 1 WHERE sync = 0");
                System.out.println("Eventos marcados como sincronizados: " + updated);
                statement.executeUpdate("UPDATE config SET valor = 2 WHERE dato = 'sinc_etapa' AND valor <> 0");
            } else {
                wsinconexion = 1;
                statement.executeUpdate("UPDATE config SET valor = 0 WHERE dato = 'sinc_etapa'");
                wjson += " - Envio Eventos";
            }

        } finally {
            lockEnvioEventos.set(false);
        }
    }
   
 /***************************************************************************************************************/   
    
    public void sincroEmpleados_jhcm(Statement st) throws SQLException {
        HashMap<String, String> params = new HashMap<>();
        HashMap<String, String> res_has = Db.selectOne("SELECT valor FROM config WHERE dato='hash'", st);
        if (res_has != null) params.put("hash", res_has.get("valor"));
        HashMap<String, String> res_has_sede = Db.selectOne("SELECT valor FROM config WHERE dato='id_sede' ", st);
        if (res_has_sede != null) params.put("sede", res_has_sede.get("valor"));

        JSONTask t2 = new JSONTask("getEmpleadosAll_jhcm_ht", params);
        if (!t2.enviar(false, this)) {
            wsError(t2, "Empleados", st);
            return;
        }

        HashMap<String, String> m = t2.respuesta;
        if (m == null) return;

        int size = parseIntSafe(m.get("size"), 0);
        String keys = m.get("keys");
        String[] keys_a = (keys != null ? keys.split(",")
                : new String[]{"cc","nombre","cargo","empresa","sede","area","h_ingreso","h_salida","ciudad","fijo",
                               "ultimo_cambio","is_dentro","activo","vacaciones","derecho_vale","id_persona","tarjeta",
                               "h1","h2","h3","h4","h5","h6","h7","h8","h9","h10","cond1","cond2","cond3","cond4",
                               "nro_vale","fecha_castigo"});

        boolean continuar = true;
        String hashnew = null;
        boolean borrarTodo = true;

        for (int i = 0; i < size && continuar; i++) {
            if (m.get(i + "_igual") != null && i == 0) {
                st.executeUpdate("UPDATE config set valor=3 where dato='sinc_etapa' and valor<>0");
                continuar = false;
                continue;
            }
            if (i == 0) {
                String hv = m.get(i + "_hash");
                if (hv != null && !hv.isEmpty()) hashnew = hv;
            }
            if (borrarTodo) {
                borrarTodo = false;
                st.executeUpdate("DELETE FROM empleado");
            }

            HashMap<String, String> campos = new HashMap<>();
            for (String k : keys_a) {
                if ("hash".equals(k)) continue;
                campos.put(k, m.get(i + "_" + k));
            }
            Db.insertHashMap(campos, "empleado", st);
            st.executeUpdate("UPDATE config set valor=3 where dato='sinc_etapa' and valor<>0");
        }

        if (!borrarTodo && hashnew != null) {
            HashMap<String, String> campos = new HashMap<>();
            campos.put("dato", "hash");
            campos.put("valor", hashnew);
            st.executeUpdate("DELETE FROM config WHERE dato='hash'");
            Db.insertHashMap(campos, "config", st);
            st.executeUpdate("UPDATE config set valor=3 where dato='sinc_etapa' and valor<>0");
        }
    }

    public void sincroHorarios(Statement st) throws SQLException {
        HashMap<String, String> params = new HashMap<>();
        HashMap<String, String> res_has = Db.selectOne("SELECT valor FROM config WHERE dato='hash_horarios'", st);
        if (res_has != null) params.put("hash_horarios", res_has.get("valor"));

        // Nombre correcto del WS
        JSONTask t = new JSONTask("getHorariosAll", params);

        if (!t.enviar(false, this)) {
            wsError(t, "Horarios", st);
            return;
        }

        HashMap<String, String> m = t.respuesta;
        if (m == null) return;

        int size = parseIntSafe(m.get("size"), 0);
        String keys = m.get("keys");
        String[] keys_a = (keys != null ? keys.split(",")
                : new String[]{"CC","INICIO","FIN","FECHA","UPDATE"});

        boolean continuar = true;
        String hashnew = null;
        boolean borrarTodo = true;

        for (int i = 0; i < size && continuar; i++) {
            if (m.get(i + "_igual") != null && i == 0) {
                st.executeUpdate("UPDATE config set valor=4 where dato='sinc_etapa' and valor<>0");
                continuar = false;
                continue;
            }
            if (i == 0) {
                String hv = m.get(i + "_hash");
                if (hv != null && !hv.isEmpty()) hashnew = hv;
            }
            if (borrarTodo) {
                borrarTodo = false;
                st.executeUpdate("DELETE FROM horarios");
            }
            HashMap<String, String> row = new HashMap<>();
            for (String k : keys_a) {
                if ("hash".equals(k)) continue;
                row.put(k, m.get(i + "_" + k));
            }
            Db.insertHashMap(row, "horarios", st);
            st.executeUpdate("UPDATE config set valor=4 where dato='sinc_etapa' and valor<>0");
        }

        if (!borrarTodo && hashnew != null) {
            HashMap<String, String> campos = new HashMap<>();
            campos.put("dato", "hash_horarios");
            campos.put("valor", hashnew);
            st.executeUpdate("DELETE FROM config WHERE dato='hash_horarios'");
            Db.insertHashMap(campos, "config", st);
            st.executeUpdate("UPDATE config set valor=4 where dato='sinc_etapa' and valor<>0");
        }
    }
    
/***************************************************************************************************************************/
    
    public void sincroPermisos(Statement st) throws SQLException {
        // 1. Obtener hash actual (opcional, para la API)
        HashMap<String, String> res_has = Db.selectOne("SELECT valor FROM config WHERE dato='hash_permisos'", st);
        HashMap<String, String> params = new HashMap<>();
        if (res_has != null) {
            params.put("hash_permisos", res_has.get("valor"));
        }

        // 2. Llamar a la API
        JSONTask t2 = new JSONTask("getPermisos", params);
        if (!t2.enviar(false, this)) {
            wsError(t2, "Permisos", st);
            return;
        }

        HashMap<String, String> m = t2.respuesta;
        if (m == null) {
            System.err.println("[SINCRONIZACIÓN] Respuesta nula de getPermisos");
            return;
        }

        // 3. ¿Ya está sincronizado?
        if ("1".equals(m.get("0_igual"))) {
            st.executeUpdate("UPDATE config SET valor = 5 WHERE dato = 'sinc_etapa' AND valor <> 0");
            return;
        }

        // 4. Validar que venga 'size'
        int size = parseIntSafe(m.get("size"), -1);
        if (size < 0) {
            System.err.println("[SINCRONIZACIÓN] Falta o es inválido el campo 'size' en la respuesta");
            return;
        }

        // 5. Obtener nuevo hash (obligatorio)
        String hashnew = m.get("0_hash");
        if (hashnew == null) {
            System.err.println("[SINCRONIZACIÓN] No se recibió '0_hash' en la respuesta");
            return;
        }

        // 6. Definir columnas
        String keys = m.get("keys");
        String[] keys_a = (keys != null && !keys.trim().isEmpty())
                ? keys.split(",")
                : new String[]{"CC", "FECHA_INICIO", "FECHA_FIN"};

        // 7. Borrar tabla local (siempre que no esté ya sincronizada)
        st.executeUpdate("DELETE FROM permisos");

        // 8. Insertar registros (aunque size = 0)
        for (int i = 0; i < size; i++) {
            HashMap<String, String> row = new HashMap<>();
            for (String k : keys_a) {
                if ("hash".equalsIgnoreCase(k.trim())) continue;
                row.put(k.trim(), m.get(i + "_" + k.trim()));
            }
            Db.insertHashMap(row, "permisos", st);
        }

        // 9. Actualizar hash en config
        st.executeUpdate("DELETE FROM config WHERE dato = 'hash_permisos'");
        HashMap<String, String> campos = new HashMap<>();
        campos.put("dato", "hash_permisos");
        campos.put("valor", hashnew);
        Db.insertHashMap(campos, "config", st);

        // 10. Marcar etapa completada
        st.executeUpdate("UPDATE config SET valor = 9 WHERE dato = 'sinc_etapa' AND valor <> 0");
    }
    
/*********************************************************************************************************************/

public void sincroDescargaEventos(Statement statement) throws SQLException {

    // ===================== 1) Parámetros =====================
    HashMap<String, String> parametros = new HashMap<>();

    // max actual (compatibilidad hacia atrás)
    HashMap<String, String> res_max = Db.selectOne(
            "SELECT valor FROM config WHERE dato='sinc_max_evento'", statement);
    String maxActual = (res_max != null && res_max.get("valor") != null)
            ? res_max.get("valor").trim() : "0";
    parametros.put("max", maxActual);

    // sede
    HashMap<String, String> res_has_sede = Db.selectOne(
            "SELECT valor FROM config WHERE dato='id_sede' ", statement);
    String sede = (res_has_sede != null && res_has_sede.get("valor") != null)
            ? res_has_sede.get("valor").trim() : "1";
    parametros.put("sede", sede);

    // fecha HOY (el WS lo usa si llega en vez de 'max')
    String hoy = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
    parametros.put("fecha", hoy);

    System.out.println("getEventos_ht01....." + parametros);
    System.out.println("***************************************************************************************");

    JSONTask t = new JSONTask("getEventos_ht01", parametros);

    boolean ok = t.enviar(false, this);
    if (!ok) {
        wsinconexion = 1;
        statement.executeUpdate("UPDATE config set valor=0 where dato='sinc_etapa'");
        wjson += " - Desc.Eventos";
        return;
    }

    // ===================== 2) Respuesta =====================
    HashMap<String, String> resp = t.respuesta;
    if (resp == null || resp.isEmpty()) {
        System.err.println("[sincroDescargaEventos] Respuesta NULL o vacía desde JSONTask.");
        statement.executeUpdate("UPDATE config set valor=6 where dato='sinc_etapa' and valor<>0");
        wjson += " - No hay datos cuando traer Eventos)";
        return;
    }

    // Borrar eventos ya sincronizados
    statement.executeUpdate("DELETE FROM eventos WHERE sync = 1");
    System.out.println("Borrado los eventos que ya fueron sincronozados Bdatos ppal");

    String new_max = maxActual;
    long maxReportado = -1L;

    // ===================== 3) Intentar JSON directo =====================
    String jsonResponse = null;
    // Claves típicas
    String[] posiblesClaves = { "data", "json", "body", "respuesta", "response", "eventos", "Salida", "salida", "out" };
    for (String k : posiblesClaves) {
        String v = resp.get(k);
        if (v != null && !v.trim().isEmpty()) { jsonResponse = v.trim(); break; }
    }
    if (jsonResponse == null) {
        // Buscar cualquier valor que empiece con "[" o "{"
        for (String v : resp.values()) {
            if (v == null) continue;
            String s = v.trim();
            if (!s.isEmpty() && (s.startsWith("[") || s.startsWith("{"))) { jsonResponse = s; break; }
        }
    }
    if (jsonResponse == null && resp.size() == 1) {
        String only = resp.values().iterator().next();
        if (only != null && !only.trim().isEmpty()) jsonResponse = only.trim();
    }

    boolean insertoDesdeJson = false;

    if (jsonResponse != null && !jsonResponse.isEmpty()) {
        try {
            if (jsonResponse.trim().startsWith("[")) {
                // ===== formato: arreglo directo =====
                org.json.JSONArray eventos = new org.json.JSONArray(jsonResponse);
                for (int i = 0; i < eventos.length(); i++) {
                    org.json.JSONObject ev = eventos.getJSONObject(i);

                    // caso control: { "max": N } sin evento/cc/fecha
                    if (ev.has("max") && ( !ev.has("cc") && !ev.has("fecha") && !ev.has("evento") )) {
                        try {
                            long mv = Long.parseLong(String.valueOf(ev.get("max")));
                            if (mv > maxReportado) maxReportado = mv;
                        } catch (Exception ignore) {}
                        continue;
                    }

                    String vCC     = ev.optString("cc", "").trim();
                    String vFecha  = ev.optString("fecha", "").trim();
                    String vEvento = ev.optString("evento", ev.optString("motivo","")).trim();
                    String vForma  = ev.optString("forma", "").trim();
                    if (ev.has("max")) {
                        try {
                            long mv = Long.parseLong(String.valueOf(ev.get("max")));
                            if (mv > maxReportado) maxReportado = mv;
                        } catch (Exception ignore) {}
                    }

                    if (vCC.isEmpty() || vFecha.isEmpty() || vEvento.isEmpty()) {
                        System.err.println("[sincroDescargaEventos] JSON registro #" + i + " incompleto. cc=" + vCC +
                                ", fecha=" + vFecha + ", evento=" + vEvento + " (omitido)");
                        continue;
                    }

                    HashMap<String, String> campos = new HashMap<>();
                    campos.put("cc",     vCC);
                    campos.put("fecha",  vFecha);
                    campos.put("evento", vEvento);
                    campos.put("forma",  vForma);
                    campos.put("sync",   "1");
                    Db.insertHashMap(campos, "eventos", statement);
                    insertoDesdeJson = true;
                }

            } else if (jsonResponse.trim().startsWith("{")) {
                // ===== formato: objeto con data/eventos =====
                org.json.JSONObject obj = new org.json.JSONObject(jsonResponse);
                org.json.JSONArray eventos = null;
                if (obj.has("data") && obj.get("data") instanceof org.json.JSONArray) {
                    eventos = obj.getJSONArray("data");
                } else if (obj.has("eventos") && obj.get("eventos") instanceof org.json.JSONArray) {
                    eventos = obj.getJSONArray("eventos");
                } else if (obj.has("max")) {
                    // control sin eventos
                    try {
                        long mv = Long.parseLong(String.valueOf(obj.get("max")));
                        if (mv > maxReportado) maxReportado = mv;
                    } catch (Exception ignore) {}
                }

                if (eventos != null) {
                    for (int i = 0; i < eventos.length(); i++) {
                        org.json.JSONObject ev = eventos.getJSONObject(i);

                        String vCC     = ev.optString("cc", "").trim();
                        String vFecha  = ev.optString("fecha", "").trim();
                        String vEvento = ev.optString("evento", ev.optString("motivo","")).trim();
                        String vForma  = ev.optString("forma", "").trim();
                        if (ev.has("max")) {
                            try {
                                long mv = Long.parseLong(String.valueOf(ev.get("max")));
                                if (mv > maxReportado) maxReportado = mv;
                            } catch (Exception ignore) {}
                        }

                        if (vCC.isEmpty() || vFecha.isEmpty() || vEvento.isEmpty()) {
                            System.err.println("[sincroDescargaEventos] JSON registro #" + i + " incompleto. cc=" + vCC +
                                    ", fecha=" + vFecha + ", evento=" + vEvento + " (omitido)");
                            continue;
                        }

                        HashMap<String, String> campos = new HashMap<>();
                        campos.put("cc",     vCC);
                        campos.put("fecha",  vFecha);
                        campos.put("evento", vEvento);
                        campos.put("forma",  vForma);
                        campos.put("sync",   "1");
                        Db.insertHashMap(campos, "eventos", statement);
                        insertoDesdeJson = true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Respuesta JSON directa (DEBUG): " + jsonResponse);
            System.err.println("Fallo parseando JSON directo; intentaré mapa aplanado… " + e);
            // Caeremos al plan B (mapa aplanado)
        }
    }

    if (!insertoDesdeJson) {
        // ===================== 4) Plan B: mapa aplanado (size / i_campos) =====================
        int size = 0;
        String sizeStr = resp.get("size");
        if (sizeStr != null && !sizeStr.trim().isEmpty()) {
            try { size = Integer.parseInt(sizeStr.trim()); } catch (NumberFormatException ignore) {}
        }
        if (size <= 0) {
            int maxIndex = -1;
            for (String k : resp.keySet()) {
                int us = (k == null) ? -1 : k.indexOf('_');
                if (us > 0) {
                    String pref = k.substring(0, us);
                    try {
                        int idx = Integer.parseInt(pref);
                        if (idx > maxIndex) maxIndex = idx;
                    } catch (NumberFormatException ignore) {}
                }
            }
            if (maxIndex >= 0) size = maxIndex + 1;
        }

        for (int i = 0; i < size; i++) {
            String keyMax    = i + "_max";
            String keyCC     = i + "_cc";
            String keyFecha  = i + "_fecha";
            String keyEvento = i + "_evento";
            String keyMotivo = i + "_motivo";
            String keyForma  = i + "_forma";

            String vMax    = resp.getOrDefault(keyMax, "").trim();
            String vCC     = resp.getOrDefault(keyCC, "").trim();
            String vFecha  = resp.getOrDefault(keyFecha, "").trim();
            String vEvento = resp.getOrDefault(keyEvento, "").trim();
            if (vEvento.isEmpty()) vEvento = resp.getOrDefault(keyMotivo, "").trim();
            String vForma  = resp.getOrDefault(keyForma, "").trim();

            // Solo-control: viene max pero no cc/fecha/evento → no insertar
            if (vCC.isEmpty() && vFecha.isEmpty() && vEvento.isEmpty()) {
                if (!vMax.isEmpty()) {
                    try {
                        long mv = Long.parseLong(vMax);
                        if (mv > maxReportado) maxReportado = mv;
                    } catch (NumberFormatException ignore) {}
                }
                continue;
            }

            if (!vMax.isEmpty()) {
                try {
                    long mv = Long.parseLong(vMax);
                    if (mv > maxReportado) maxReportado = mv;
                } catch (NumberFormatException ignore) {}
            }

            if (vCC.isEmpty() || vFecha.isEmpty() || vEvento.isEmpty()) {
                System.err.println("[sincroDescargaEventos] Registro #" + i + " incompleto. cc=" + vCC +
                        ", fecha=" + vFecha + ", evento=" + vEvento + " (omitido)");
                continue;
            }

            HashMap<String, String> campos = new HashMap<>();
            campos.put("cc",     vCC);
            campos.put("fecha",  vFecha);
            campos.put("evento", vEvento);
            campos.put("forma",  vForma);
            campos.put("sync",   "1");
            Db.insertHashMap(campos, "eventos", statement);
        }
    }

    if (maxReportado >= 0) new_max = String.valueOf(maxReportado);

    // ===================== 5) Actualizar config y etapa =====================
    HashMap<String, String> camposCfg = new HashMap<>();
    camposCfg.put("dato", "sinc_max_evento");
    camposCfg.put("valor", new_max);

    statement.executeUpdate("DELETE FROM config WHERE dato='sinc_max_evento'");
    Db.insertHashMap(camposCfg, "config", statement);

    statement.executeUpdate("UPDATE config set valor=6 where dato='sinc_etapa' and valor<>0");
}


    
/*********************************************************************************************************************/    
    public void sincroEnviarEventos_backup(Statement st) throws SQLException {
        System.out.println(" .....LLego a sincroEnviarEventos_backup");

        // 1) Leer flags de backup de la sede actual
        //   Corrige la consulta: une sedes_tmp con config.id_sede
        HashMap<String, String> sedeCfg = Db.selectOne(
            "SELECT s.backup, s.backup_ini, s.backup_ter " +
            "FROM sedes_tmp s " +
            "JOIN config c ON c.dato='id_sede' AND s.id_sede=c.valor " +
            "LIMIT 1", st
        );

        if (sedeCfg == null) {
            System.out.println(" .....No se reciben datos de sedes_tmp / id_sede en config.......");
            // no hay datos -> damos por terminada la etapa de backup
            st.executeUpdate("UPDATE config SET valor=7 WHERE dato='sinc_etapa' AND valor<>0");
            return;
        }

        final String wvalidarbackup = safe(sedeCfg.get("backup"), "N").trim();
        final String wbackup_ini    = safe(sedeCfg.get("backup_ini"), "").trim();
        final String wbackup_ter    = safe(sedeCfg.get("backup_ter"), "").trim();

        System.out.println("backup.. " + wvalidarbackup);
        System.out.println("backup_ini.. " + wbackup_ini);
        System.out.println("backup_ter.. " + wbackup_ter);

        // 2) Si la sede NO tiene backup activo, cerramos etapa y salimos
        if (!"S".equalsIgnoreCase(wvalidarbackup)) {
            st.executeUpdate("UPDATE config SET valor=7 WHERE dato='sinc_etapa' AND valor<>0");
            System.out.println(" .....Backup desactivado para esta sede. Nada que enviar.");
            return;
        }

        // 3) Armar sentencia: por rango si hay fechas; si no, todo
        String wsentencia;
        if (!wbackup_ini.isEmpty() && !wbackup_ter.isEmpty()) {
            // Asumiendo formato 'YYYY-MM-DD HH:mm:ss' en fecha (o compatible con BETWEEN)
            wsentencia = "SELECT * FROM eventos_sitio " +
                         "WHERE fecha BETWEEN '" + esc(wbackup_ini) + "' AND '" + esc(wbackup_ter) + "' " +
                         "ORDER BY fecha";
        } else {
            wsentencia = "SELECT * FROM eventos_sitio ORDER BY fecha";
        }

        System.out.println(" .....Datos de eventos de backup......." + wsentencia);

        java.util.ArrayList<HashMap<String, String>> eventos = Db.selectAll(wsentencia, st);

        if (eventos == null || eventos.isEmpty()) {
            // No hay nada que enviar, cerramos etapa
            st.executeUpdate("UPDATE config SET valor=7 WHERE dato='sinc_etapa' AND valor<>0");
            System.out.println(" .....No hay eventos para backup en el rango.");
            return;
        }

        // 4) Serializar como lo haces (col---*---valor) unidos por ':::::' y filas por ';;;;;'
        java.util.ArrayList<String> filasSerializadas = new java.util.ArrayList<>();
        for (HashMap<String, String> evento : eventos) {
            java.util.ArrayList<String> cols = new java.util.ArrayList<>();
            // NO borres del mapa mientras iteras; recorre sus entries
            for (java.util.Map.Entry<String, String> e : evento.entrySet()) {
                String k = safe(e.getKey(), "");
                String v = safe(e.getValue(), "");
                cols.add(k + "---*---" + v);
            }
            // Unir columnas de la fila
            String fila = String.join(":::::", cols);
            filasSerializadas.add(fila);
        }
        // Unir todas las filas
        String data_aux = String.join(";;;;;", filasSerializadas);

        HashMap<String, String> parametros3 = new HashMap<>();
        parametros3.put("info", data_aux);

        // 5) Adjuntar id_sede
        HashMap<String, String> idSede = Db.selectOne("SELECT valor FROM config WHERE dato='id_sede'", st);
        parametros3.put("id_sede", idSede != null ? safe(idSede.get("valor"), "1") : "1");

        // 6) Enviar al WS correcto que indicaste
        JSONTask t3 = new JSONTask("setEventos_backup", parametros3);
        System.out.println(" .....Enviando eventos para Backup ....." + parametros3.size() + " filas");

        if (t3.enviar(false, this)) {
            System.out.println(" .....Backup OK en servidor principal.");
            st.executeUpdate("UPDATE config SET valor=7 WHERE dato='sinc_etapa' AND valor<>0");
        } else {
            // Manejo de error coherente con el resto del flujo
            wsinconexion = 1;
            wjson += " - Backup Envio Eventos";
            st.executeUpdate("UPDATE config SET valor=0 WHERE dato='sinc_etapa'");
            // wsError(t3, "EventosBackup", st); // Si quieres mostrar exactamente el error HTTP/404
        }
    }
/******************************************************************************************************************************/

    public void sincroActividad_version(Statement st, String accion) throws SQLException, UnknownHostException {
        InetAddress address = InetAddress.getLocalHost();

        HashMap<String, String> m = new HashMap<>();
        m.put("fecha", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        m.put("sede", readConfig(st, "id_sede", "1"));
        m.put("accion", (accion == null || accion.trim().isEmpty()) ? "SINCRONIZACION" : accion);
        m.put("versionapp", readConfig(st, "versionactualapp", wqueversion));
        m.put("ipws", process.JSONTask.wqueruta == null ? "" : process.JSONTask.wqueruta);
        m.put("ipcliente", address.getHostAddress());

        JSONTask t = new JSONTask("setActividad_version", m);
        if (!t.enviar(false, this)) {
            wsError(t, "Actividad", st);
            return;
        }

        registroaccion = "";
    }

    public void sincronizar_consulta(Statement st) throws SQLException {
        HashMap<String, String> res_has =
                Db.selectOne("SELECT valor FROM config WHERE dato='sinc_etapa' and valor='7' ", st);
        if (res_has != null) {
            st.executeUpdate("delete from eventos_tmp where sync=1");
            st.executeUpdate("insert into eventos_tmp select * from eventos");
            st.executeUpdate("delete from horarios_tmp");
            st.executeUpdate("insert into horarios_tmp select * from horarios");
            st.executeUpdate("delete from empleado_tmp");
            st.executeUpdate("insert into empleado_tmp select * from empleado");
            st.executeUpdate("delete from permisos_tmp");
            st.executeUpdate("insert into permisos_tmp select * from permisos");
            st.executeUpdate("delete from sedes_tmp");
            st.executeUpdate("insert into sedes_tmp select * from sedes");
        }
    }

    public void verificarVersionesBorrar(Statement st) throws SQLException {
        String idSede = readConfig(st, "id_sede", "1");
        String vLocal = readConfig(st, "versionactualapp", wqueversion);
        String vSede = vLocal;
        try (ResultSet rs = st.executeQuery(
                "SELECT versionactualapp FROM sedes_tmp WHERE id_sede='" + esc(idSede) + "' LIMIT 1")) {
            if (rs.next()) {
                String v = rs.getString(1);
                if (v != null && !v.trim().isEmpty()) vSede = v.trim();
            }
        }
        if (!Objects.equals(vLocal, vSede)) {
            upsertConfig(st, "versionactualapp", vSede);
        }
    }
   //********************************************************************************************
    public void verificarVersiones(Statement statement) throws SQLException {
        System.out.println("Entramos a validar la version");

        String wvinstala = "";
        String wvactual = "";
        String widsede = "";
        String wnamesede = "";

        // ==============================
        // 1. Leer versión actual requerida
        // ==============================
        HashMap<String, String> wvractual =
                Db.selectOne("SELECT valor FROM config WHERE dato='versionactualapp'", statement);
        if (wvractual != null && wvractual.get("valor") != null) {
            wvactual = wvractual.get("valor");
        }

        // ==============================
        // 2. Leer versión instalada localmente
        // ==============================
        HashMap<String, String> wvrinstala =
                Db.selectOne("SELECT valor FROM config WHERE dato='versioninstalacion'", statement);
        if (wvrinstala != null && wvrinstala.get("valor") != null) {
            wvinstala = wvrinstala.get("valor");
        }

        // ==============================
        // 3. Leer id de sede desde config
        // ==============================
        HashMap<String, String> wrsede =
                Db.selectOne("SELECT valor FROM config WHERE dato='id_sede'", statement);
        if (wrsede != null && wrsede.get("valor") != null) {
            widsede = wrsede.get("valor");
        }

        // ==============================
        // 4. Leer nombre de sede desde sedes_tmp
        // ==============================
        if (widsede != null && !widsede.trim().isEmpty()) {
            HashMap<String, String> wrnombresede =
                    Db.selectOne("SELECT sede FROM sedes_tmp WHERE id_sede='" + widsede + "'", statement);

            if (wrnombresede != null && wrnombresede.get("sede") != null) {
                wnamesede = wrnombresede.get("sede");
            }
        }

        // ==============================
        // 5. Validar diferencia de versiones
        // ==============================
        if (wvactual == null ? wvinstala != null : !wvactual.equals(wvinstala)) {
            System.out.println("Sede... " + wnamesede + " [" + widsede + "]");
            System.out.println("Version Requerida... " + wvactual);
            System.out.println("Version Instalada... " + wvinstala);
            System.out.println("Problemas con la version");

            JOptionPane.showMessageDialog(
                null,
                "Versión Control de Ingreso desactualizada.\n\n" +
                "Sede: " + wnamesede + " [" + widsede + "]\n" +
                "Versión requerida: " + wvactual + "\n" +
                "Versión instalada: " + wvinstala,
                "Actualización requerida",
                JOptionPane.WARNING_MESSAGE
            );

            System.exit(0);
        }
    }
   //******************************************************************************************** 
    public void verificarDatosEmpleados(Statement st) throws SQLException {
        int c = 0;
        try (ResultSet rs = st.executeQuery("SELECT COUNT(1) FROM empleado")) {
            if (rs.next()) c = rs.getInt(1);
        }
        if (inicio != null) {
            if (c == 0) ui(() -> inicio.inicioMensaje("ATENCIÓN: No hay empleados sincronizados"));
        }
    }

    // ------------------------------------------------------------------------------------
    // Manejo centralizado de errores de WS (incluye HTTP 404 y muestra en Inicio)
    // ------------------------------------------------------------------------------------
    private void wsError(JSONTask t, String modulo, Statement st) {
        wsinconexion = 1;
        try {
            st.executeUpdate("UPDATE config set valor=0 where dato='sinc_etapa'");
        } catch (Exception ignore) {}

        String msg;
        if (t != null) {
            // JSONTask ya compone mensaje_e; para 404 incluye:
            // "WebService no encontrado: <servicio> (HTTP 404 - <reason>) - <snippet>"
            String m = t.getErrorMessage();
            int sc = t.getHttpStatus();

            if (m != null && !m.isEmpty()) {
                msg = m; // usar exactamente el que compuso JSONTask
            } else if (sc > 0) {
                msg = "Error " + modulo + " - HTTP " + sc;
            } else {
                msg = "Error " + modulo;
            }
        } else {
            msg = "Error " + modulo;
        }

        // Guardar para logs existentes
        wjson = msg;

        // Mostrar en pantalla (Inicio -> versincronizacion) en el hilo de Swing
        if (inicio != null) ui(() -> inicio.inicioMensaje(msg));
    }

    // ------------------------------------------------------------------------------------
    // Utils DB
    // ------------------------------------------------------------------------------------
    private void ensureTable(Connection cn, Statement st, String table, String createSql) throws SQLException {
        if (tableExists(cn, table)) return;
        st.executeUpdate(createSql);
    }

    private boolean tableExists(Connection cn, String table) throws SQLException {
        DatabaseMetaData md = cn.getMetaData();
        try (ResultSet rs = md.getTables(null, null, table, null)) {
            if (rs.next()) return true;
        }
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT name FROM sqlite_master WHERE type='table' AND lower(name)=lower(?)")) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void ensureConfigKV(Statement st, String dato, String valor) throws SQLException {
        try (PreparedStatement ps = st.getConnection().prepareStatement(
                "INSERT INTO config(dato,valor) " +
                        "SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM config WHERE dato=?)")) {
            ps.setString(1, dato);
            ps.setString(2, valor);
            ps.setString(3, dato);
            ps.executeUpdate();
        }
    }

    private void upsertConfig(Statement st, String dato, String valor) throws SQLException {
        try (PreparedStatement ps = st.getConnection().prepareStatement(
                "UPDATE config SET valor=? WHERE dato=?")) {
            ps.setString(1, valor);
            ps.setString(2, dato);
            int n = ps.executeUpdate();
            if (n == 0) {
                try (PreparedStatement ins = st.getConnection().prepareStatement(
                        "INSERT INTO config(dato,valor) VALUES(?,?)")) {
                    ins.setString(1, dato);
                    ins.setString(2, valor);
                    ins.executeUpdate();
                }
            }
        }
    }

    private String readConfig(Statement st, String dato, String def) throws SQLException {
        try (PreparedStatement ps = st.getConnection().prepareStatement(
                "SELECT valor FROM config WHERE dato=? LIMIT 1")) {
            ps.setString(1, dato);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String v = rs.getString(1);
                    return (v != null ? v : def);
                }
            }
        }
        return def;
    }

    private void marcarUltimaSincronizacion(Statement st, Connection cn) throws SQLException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String now = df.format(new java.util.Date());
        st.executeUpdate("DELETE FROM config WHERE dato='ultima_s'");
        HashMap<String, String> campos = new HashMap<>();
        campos.put("dato", "ultima_s");
        campos.put("valor", now);
        Db.insertHashMap(campos, "config", st);
        try { cn.commit(); } catch (SQLException ignore) {}
        if (inicio != null) ui(() -> inicio.setUltimaSincronizacion(now));
    }

    private boolean ok() { return wsinconexion == 0; }
    private void sleepMinutes(int minutes) {
        try { Thread.sleep(minutes * 60L * 1000L); } catch (InterruptedException ignored) {}
    }
    private static String safe(String s, String def) { return (s == null || s.trim().isEmpty()) ? def : s.trim(); }
    private static String esc(String s) { return s == null ? "" : s.replace("'", "''"); }
    private static int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s == null ? "" : s); } catch (Exception e) { return def; }
    }
    private static String generateRandomPasswordApp(int len) {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }
}
