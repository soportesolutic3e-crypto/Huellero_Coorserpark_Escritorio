package database;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utilidades estáticas para operar sobre una conexión ya abierta.
 * Mantiene compatibilidad con el proyecto:
 *  - selectOne(String, Statement)
 *  - selectAll(String, Statement)
 *  - insertHashMap(Map, table, Statement)
 *
 * Mejoras:
 *  - insertHashMap usa PreparedStatement con placeholders (?).
 *  - Variantes con parámetros para evitar concatenaciones:
 *      selectOne(String sql, List<Object> params, Statement st)
 *      selectAll(String sql, List<Object> params, Statement st)
 */
public final class Db {

    private Db() {}

    /* ========================= SELECTS SIN PARÁMETROS (compatibilidad) ========================= */

    public static HashMap<String, String> selectOne(String sql, Statement st) throws SQLException {
        Connection conn = st.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) return null;
            return mapRow(rs);
        }
    }

    public static ArrayList<HashMap<String, String>> selectAll(String sql, Statement st) throws SQLException {
        Connection conn = st.getConnection();
        ArrayList<HashMap<String, String>> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(mapRow(rs));
        }
        return out;
    }

    /* ========================= SELECTS CON PARÁMETROS (recomendado a futuro) ========================= */

    public static HashMap<String, String> selectOne(String sql, List<Object> params, Statement st) throws SQLException {
        Connection conn = st.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapRow(rs);
            }
        }
    }

    public static ArrayList<HashMap<String, String>> selectAll(String sql, List<Object> params, Statement st) throws SQLException {
        Connection conn = st.getConnection();
        ArrayList<HashMap<String, String>> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapRow(rs));
            }
        }
        return out;
    }

    /* ============================== INSERT DINÁMICO SEGURO =================================== */

    public static void insertHashMap(Map<String, String> data, String table, Statement st) throws SQLException {
        if (data == null || data.isEmpty()) return;

        Connection conn = st.getConnection();

        List<String> cols = new ArrayList<>(data.keySet());
        String colList = cols.stream().map(Db::quoteIdent).collect(Collectors.joining(","));
        String placeholders = cols.stream().map(c -> "?").collect(Collectors.joining(","));

        String sql = "INSERT INTO " + quoteIdent(table) + " (" + colList + ") VALUES (" + placeholders + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < cols.size(); i++) {
                String val = data.get(cols.get(i));
                ps.setString(i + 1, val); // null-safe
            }
            ps.executeUpdate();
        }
    }

    /* ============================== HELPERS PRIVADOS =================================== */

    private static void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        if (params == null || params.isEmpty()) return;
        for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
    }

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

    /** Escapa identificadores (tabla/columna) con comillas dobles para SQLite. */
    private static String quoteIdent(String ident) {
        if (ident == null) throw new IllegalArgumentException("Identifier null");
        String safe = ident.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }
}
