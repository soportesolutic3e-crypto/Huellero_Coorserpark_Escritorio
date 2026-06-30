





package models;

import database.ConexionDb;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Diagnóstico de vales por cédula (cc).
 * NO modifica la BD; solo lee y explica el estado y las reglas aplicadas.
 */
public final class ValeDiagnostic {

    public static final String EVT_INGRESO = "Ingreso";
    public static final String EVT_SALIDA  = "Salida";
    public static final String EVT_VALE    = "Impresion-Vale";
    public static final String EVT_CAST8   = "Castigo-8";
    public static final String EVT_CAST30  = "Castigo-30";

    public static class Result {
        public String cc;
        public boolean existe;
        public boolean activo;
        public boolean fijo;         // true = fijo, false = rotativo
        public boolean isDentro;     // true = está dentro actualmente
        public String derechoVale;   // "1" = tiene derecho
        public String cond1;         // rotativo: validar turno (S/N)
        public String cond2;         // fijo: reglas de impresión (M/I/…)
        public String toleranciaMin; // cond3: minutos tolerancia
        public String hIngreso;      // hora aplicable hoy (HH:mm:ss)
        public String hSalida;       // hora aplicable hoy (HH:mm:ss)
        public boolean tieneTurnoHoy; // para rotativo
        public boolean valeYaImpresoHoy;
        public boolean tieneCastigoHoy;
        public boolean puedeImprimir;
        public java.util.ArrayList<String> razones = new java.util.ArrayList<String>();

        @Override public String toString() {
            StringBuilder sb = new StringBuilder();
            if (!existe) return "CC " + cc + " no existe en empleado_tmp";
            sb.append("CC: ").append(cc).append("\n");
            sb.append("Activo: ").append(activo ? "Sí" : "No").append("\n");
            sb.append("Tipo: ").append(fijo ? "Fijo" : "Rotativo").append("\n");
            sb.append("Dentro ahora: ").append(isDentro ? "Sí" : "No").append("\n");
            sb.append("Derecho a vale: ").append("1".equals(derechoVale) ? "Sí" : "No").append("\n");
            sb.append("cond1 (rotativo validar turno): ").append(nvl(cond1)).append("\n");
            sb.append("cond2 (fijo genera vale): ").append(nvl(cond2)).append("\n");
            sb.append("Tolerancia (min): ").append(nvl(toleranciaMin)).append("\n");
            sb.append("Horario de hoy: ").append(nvl(hIngreso)).append(" → ").append(nvl(hSalida)).append("\n");
            if (!fijo) sb.append("Tiene turno hoy: ").append(tieneTurnoHoy ? "Sí" : "No").append("\n");
            sb.append("Vale ya impreso hoy: ").append(valeYaImpresoHoy ? "Sí" : "No").append("\n");
            sb.append("Castigo hoy: ").append(tieneCastigoHoy ? "Sí" : "No").append("\n");
            sb.append("¿Puede imprimir ahora?: ").append(puedeImprimir ? "Sí" : "No").append("\n");
            if (!razones.isEmpty()) {
                sb.append("Razones / Bloqueos:\n");
                for (String r : razones) sb.append(" - ").append(r).append("\n");
            }
            return sb.toString();
        }
        private static String nvl(String s){ return s==null?"":s; }
    }

    /** Diagnostica por CC. No lanza excepciones; retorna razones si algo falla. */
    public static Result diagnosticarPorCC(String cc) {
        Result r = new Result();
        r.cc = cc;

        String hoy = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String ini = hoy + " 00:00:00";
        String fin = hoy + " 23:59:59";

        try (ConexionDb db = new ConexionDb()) {
            db.Conectar();

            // 1) Empleado
            Map<String,String> emp = db.selectOne("SELECT * FROM empleado_tmp WHERE cc = '" + escape(cc) + "'");
            if (emp == null) {
                r.existe = false;
                return r;
            }
            r.existe = true;
            r.activo = !"0".equals(get(emp, "activo"));
            r.fijo   = !"0".equals(get(emp, "fijo")); // "1"=fijo, "0"=rotativo
            r.isDentro = "1".equals(get(emp, "is_dentro"));
            r.derechoVale = get(emp, "derecho_vale");
            r.cond1 = get(emp, "cond1"); // rotativo: validar turno del día (S/N)
            r.cond2 = get(emp, "cond2"); // fijo: M (medio día), I (solo ingreso), etc.
            r.toleranciaMin = get(emp, "cond3"); // minutos de tolerancia

            // 2) Horario que aplica hoy
            if (r.fijo) {
                r.hIngreso = cortarHora(get(emp, "h_ingreso"));
                r.hSalida  = cortarHora(get(emp, "h_salida"));
            } else {
                List<HashMap<String,String>> turnos = db.selectAll(
                    "SELECT * FROM horarios_tmp WHERE cc = '" + escape(cc) + "' AND fecha = '" + hoy + "' LIMIT 1"
                );
                r.tieneTurnoHoy = (turnos != null && !turnos.isEmpty());
                if (r.tieneTurnoHoy) {
                    Map<String,String> t = turnos.get(0);
                    r.hIngreso = get(t, "INICIO");
                    r.hSalida  = get(t, "FIN");
                } else {
                    r.hIngreso = "";
                    r.hSalida  = "";
                }
            }

            // 3) Ya imprimió vale hoy
            r.valeYaImpresoHoy = count(db,
                "SELECT COUNT(1) AS c FROM eventos_tmp WHERE cc=? AND fecha>=? AND fecha<=? AND evento=?",
                cc, ini, fin, EVT_VALE) > 0;
            if (r.valeYaImpresoHoy) r.razones.add("El vale de hoy ya fue impreso.");

            // 4) Castigos hoy
            boolean cast8  = count(db, "SELECT COUNT(1) AS c FROM eventos_tmp WHERE cc=? AND fecha>=? AND fecha<=? AND evento=?", cc, ini, fin, EVT_CAST8) > 0;
            boolean cast30 = count(db, "SELECT COUNT(1) AS c FROM eventos_tmp WHERE cc=? AND fecha>=? AND fecha<=? AND evento=?", cc, ini, fin, EVT_CAST30) > 0;
            r.tieneCastigoHoy = cast8 || cast30;
            if (r.tieneCastigoHoy) r.razones.add("Llegada tarde con castigo registrado hoy.");

            // 5) Reglas de derecho + ventanas horarias
            if (!"1".equals(r.derechoVale)) r.razones.add("El empleado no tiene derecho a vale (derecho_vale != 1).");

            Calendar cal = Calendar.getInstance();
            int horaActual = cal.get(Calendar.HOUR_OF_DAY);

            if (r.fijo) {
                if ("M".equalsIgnoreCase(r.cond2)) {
                    if (!(horaActual >= 11 && horaActual < 14)) {
                        r.razones.add("Fijo: vales solo entre 11:00 y 13:59 (cond2=M).");
                    }
                }
                if ("I".equalsIgnoreCase(r.cond2)) {
                    if (r.isDentro) {
                        r.razones.add("Fijo: vale solo al ingreso (cond2=I) y ya está dentro.");
                    }
                }
            } else {
                // Rotativo
                if ("S".equalsIgnoreCase(r.cond1)) {
                    if (!r.tieneTurnoHoy) {
                        r.razones.add("Rotativo: cond1=S exige turno del día en horarios_tmp y no existe.");
                    }
                }
                if (r.isDentro) {
                    r.razones.add("Rotativo: vale solo al ingreso (ya está dentro).");
                }
            }

            // 6) Decisión final (no imprime si cualquiera de estos bloquea)
            r.puedeImprimir = r.activo
                    && "1".equals(r.derechoVale)
                    && !r.valeYaImpresoHoy
                    && !r.tieneCastigoHoy
                    && (r.fijo ? pasaFijoVentanas(r, horaActual) : pasaRotativoVentanas(r));

            return r;

        } catch (SQLException e) {
            Result err = new Result();
            err.cc = cc;
            err.existe = true; // la existencia pudo fallar por SQL, pero devolvemos el error
            err.razones.add("Error SQL: " + e.getMessage());
            return err;
        }
    }

    // ---------- reglas auxiliares ----------
    private static boolean pasaFijoVentanas(Result r, int horaActual) {
        if ("M".equalsIgnoreCase(r.cond2)) {
            if (!(horaActual >= 11 && horaActual < 14)) return false;
        }
        if ("I".equalsIgnoreCase(r.cond2)) {
            if (r.isDentro) return false;
        }
        return true;
    }

    private static boolean pasaRotativoVentanas(Result r) {
        if ("S".equalsIgnoreCase(r.cond1) && !r.tieneTurnoHoy) return false;
        if (r.isDentro) return false; // solo al ingreso
        return true;
    }

    // ---------- helpers DB ----------
    private static int count(ConexionDb db, String sql, String cc, String ini, String fin, String evento) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = db.getConnection().prepareStatement(sql);
            ps.setString(1, cc);
            ps.setString(2, ini);
            ps.setString(3, fin);
            ps.setString(4, evento);
            rs = ps.executeQuery();
            return rs.next() ? rs.getInt("c") : 0;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
        }
    }

    private static String get(Map<String,String> m, String k) {
        String v = (m == null) ? null : m.get(k);
        return v == null ? "" : v;
    }

    private static String cortarHora(String ts) {
        if (ts == null) return "";
        ts = ts.trim();
        return ts.length() >= 19 ? ts.substring(11, 19) : (ts.length() >= 5 ? ts.substring(0, 5) : ts);
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("'", "''");
    }

    private ValeDiagnostic() {}
}
