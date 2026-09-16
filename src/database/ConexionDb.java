package database;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;

/**
 * Maneja la conexión SQLite local y utilidades simples.
 *
 * *Compatibilidad hacia atrás*:
 *  - Expone `public Statement statement` y `public Connection connection`
 *    (como esperaban tus modelos), para no tocar otras clases.
 *  - Incluye un método de instancia `insertHashMap(...)` que delega en Db.insertHashMap(...).
 *
 * *Mejoras*:
 *  - Soporta AutoCloseable (try-with-resources).
 *  - Cierra SIEMPRE statement y connection en close().
 *  - Implementa selectOne/selectAll con PreparedStatement (sin params).
 */
public class ConexionDb implements AutoCloseable {

    /** Compatibilidad: accesibles desde modelos existentes */
    public Connection connection;
    public Statement statement;

    private final String dbName = "datosv7.sqlite";
    private final Path dbPath;

    /** Crea con ruta por defecto: {user.home}/huapp/datosv7.sqlite */
    public ConexionDb() {
        this.dbPath = Paths.get(System.getProperty("user.home"), "huapp", dbName);
    }

    /** Permite crear con ruta de BD personalizada. */
    public ConexionDb(Path customPath) {
        this.dbPath = customPath;
    }

    /** Abre conexión y statement (timeout 30s). */
    /***METODO ANTERIOR **
    public void Conectar() throws SQLException {
        String url = "jdbc:sqlite:" + dbPath.toString();
        this.connection = DriverManager.getConnection(url);
        this.connection.setAutoCommit(true);
        this.statement = this.connection.createStatement();
        this.statement.setQueryTimeout(30);
    }
    */
    /*METODO CONECTAR VERSION V23-15-09-2026*/
        /** Abre conexión y statement (timeout 30s). */
    public void Conectar() throws SQLException {
        String url = "jdbc:sqlite:" + dbPath.toString();
        this.connection = DriverManager.getConnection(url);
        this.connection.setAutoCommit(true);
        this.statement = this.connection.createStatement();
        this.statement.setQueryTimeout(30);
        this.statement.execute("PRAGMA busy_timeout=5000");
    }

    /** Getter (por si en otro punto prefieres no acceder directo al campo). */
    public Connection getConnection() { return this.connection; }
    public Statement getStatement()   { return this.statement; }

    /* ===================== Utilidades de instancia (compat) ===================== */

    /**
     * Inserta usando columnas del mapa y delega a Db.insertHashMap(...).
     * Mantiene la firma que estás usando en modelos: n.insertHashMap(campos, "tabla")
     */
    public void insertHashMap(HashMap<String, String> data, String table) throws SQLException {
        Db.insertHashMap(data, table, this.statement);
    }

    /** SELECT * ... LIMIT 1 (sin parámetros). */
    public HashMap<String, String> selectOne(String sql) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) return null;
            return mapRow(rs);
        }
    }

    /** SELECT * ... (sin parámetros). */
    public ArrayList<HashMap<String, String>> selectAll(String sql) throws SQLException {
        ArrayList<HashMap<String, String>> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(mapRow(rs));
        }
        return out;
    }

    /** Mapeo genérico de fila: usa getString por simplicidad/compatibilidad con SQLite. */
    private static HashMap<String, String> mapRow(ResultSet rs) throws SQLException {
        HashMap<String, String> row = new HashMap<>();
        ResultSetMetaData md = rs.getMetaData();
        int n = md.getColumnCount();
        for (int i = 1; i <= n; i++) {
            String name = md.getColumnLabel(i);
            row.put(name, rs.getString(i));
        }
        return row;
    }

    /** Cierra statement y connection. Seguro de llamar múltiples veces. */
    @Override
    public void close() throws SQLException {
        SQLException first = null;
        if (statement != null) {
            try { statement.close(); } catch (SQLException e) { first = e; }
            statement = null;
        }
        if (connection != null) {
            try { connection.close(); } catch (SQLException e) { if (first == null) first = e; }
            connection = null;
        }
        if (first != null) throw first;
    }
}
