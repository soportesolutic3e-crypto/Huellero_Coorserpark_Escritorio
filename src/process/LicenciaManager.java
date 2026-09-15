package process;

import database.ConexionDb;
import java.io.*;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Gestiona la licencia del equipo.
 *
 * Primera ejecución : genera el archivo .huapp_lic amarrado al hardware.
 * Ejecuciones siguientes: valida que el hardware coincida con el archivo.
 */
public class LicenciaManager {

    private static final String ARCHIVO  = ".huapp_lic";
    private static final String CARPETA  = System.getProperty("user.home") + File.separator + "huapp";
    private static final byte[] AES_KEY  = "Hu4pp$S3cur3K3y!"  .getBytes(StandardCharsets.UTF_8); // 16 bytes

    // ── Resultado de validación ───────────────────────────────────────────────
    public static class Resultado {
        public final boolean ok;
        public final String  archivoEstado;
        public final String  macEstado;
        public final String  discoEstado;
        public final String  cpuEstado;
        public final String  codigo;

        Resultado(boolean ok, String arch, String mac, String disco, String cpu, String cod) {
            this.ok            = ok;
            this.archivoEstado = arch;
            this.macEstado     = mac;
            this.discoEstado   = disco;
            this.cpuEstado     = cpu;
            this.codigo        = cod;
        }
    }

    // ── API pública ───────────────────────────────────────────────────────────

    /**
     * Llama esto al arrancar la app.
     * Si no existe el archivo lo crea (primera instalación).
     * Si existe lo valida contra el hardware actual.
     */
    public static Resultado verificar() {
        try {
            Path ruta = Paths.get(CARPETA, ARCHIVO);

            if (!Files.exists(ruta)) {
                activar(ruta);
                return new Resultado(true, "CREADO", "OK", "OK", "OK", "LIC-000");
            }

            return validar(ruta);

        } catch (Exception e) {
            return new Resultado(false, "ERROR", "?", "?", "?", "LIC-999");
        }
    }

    /**
     * Llama esto cuando el usuario intenta usar la app (Registro o Enrolamiento).
     * Requiere que id_sede ya esté configurado en SQLite.
     * Si no hay id_sede, deja pasar (sede aún no configurada).
     * Si no hay internet, deja pasar (permisivo).
     */
    public static Resultado verificarServidor() {
        try {
            String idSede = getIdSede();
            if (idSede.isEmpty()) return new Resultado(true, "OK", "OK", "OK", "OK", "LIC-OK");

            Path ruta = Paths.get(CARPETA, ARCHIVO);
            if (!Files.exists(ruta)) return new Resultado(true, "OK", "OK", "OK", "OK", "LIC-OK");

            // Leer encHex del archivo
            String encHex = null;
            String raw = new String(Files.readAllBytes(ruta), StandardCharsets.UTF_8);
            for (String line : raw.split("\n")) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) { encHex = line; break; }
            }
            if (encHex == null) return new Resultado(true, "OK", "OK", "OK", "OK", "LIC-OK");

            HashMap<String, String> p = new HashMap<>();
            p.put("id_sede",     idSede);
            p.put("lic_content", encHex);

            // Registrar (si ya existe en servidor, el WS lo ignora o actualiza)
            JSONTask reg = new JSONTask("registrarLicencia", p);
            reg.enviar(false, null);

            // Validar estado
            JSONTask val = new JSONTask("validarLicencia", p);
            val.enviar(false, null);

            if (!val.error && val.respuesta != null) {
                String estado = val.respuesta.get("estado");
                if ("BLOQUEADA".equals(estado)) {
                    return new Resultado(false, "BLOQUEADA", "OK", "OK", "OK", "LIC-004");
                }
                if ("INACTIVA".equals(estado)) {
                    return new Resultado(false, "INACTIVA", "OK", "OK", "OK", "LIC-005");
                }
                if ("NO_COINCIDE".equals(estado)) {
                    return new Resultado(false, "NO_COINCIDE", "OK", "OK", "OK", "LIC-006");
                }
            }
        } catch (Exception ignored) { }

        return new Resultado(true, "OK", "OK", "OK", "OK", "LIC-OK");
    }

    // ── Activación (primera vez) ──────────────────────────────────────────────

    private static void activar(Path ruta) throws Exception {
        Files.createDirectories(ruta.getParent());

        String disco = getDiskSerial();
        String cpu   = getProcessorID();
        String salt  = UUID.randomUUID().toString();
        String hash  = sha256(disco + cpu + salt);

        String datos   = hash + "|" + salt + "|" + disco + "|" + cpu;
        String encHex  = encriptar(datos);

        String archivo = "# Licencia creada por Solutic 3E"        + "\n"
                       + encHex                                      + "\n"
                       + "# Licencia autorizada - Version 22.0.0"  + "\n";

        Files.write(ruta, archivo.getBytes(StandardCharsets.UTF_8));

        // Ocultar archivo en Windows
        try {
            Runtime.getRuntime().exec("attrib +H \"" + ruta.toAbsolutePath() + "\"");
        } catch (Exception ignored) { }

    }

    // ── Validación ────────────────────────────────────────────────────────────

    private static Resultado validar(Path ruta) {
        String archEst = "OK", macEst = "OK", discoEst = "OK", cpuEst = "OK";
        boolean ok = true;

        try {
            String raw = new String(Files.readAllBytes(ruta), StandardCharsets.UTF_8);
            String encHex = null;
            for (String line : raw.split("\n")) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) { encHex = line; break; }
            }
            if (encHex == null) return new Resultado(false, "CORRUPTO", "?", "?", "?", "LIC-001");

            String contenido = desencriptar(encHex);
            String[] partes  = contenido.split("\\|", -1);

            String hashGuardado, salt, discoGuardado, cpuGuardado;
            boolean formatoViejo;

            if (partes.length == 4) {
                // Formato nuevo: hash|salt|disco|cpu
                formatoViejo  = false;
                hashGuardado  = partes[0];
                salt          = partes[1];
                discoGuardado = partes[2];
                cpuGuardado   = partes[3];
            } else if (partes.length == 5) {
                // Formato viejo: hash|salt|mac|disco|cpu
                formatoViejo  = true;
                hashGuardado  = partes[0];
                salt          = partes[1];
                // partes[2] = mac (ignorada en comparación)
                discoGuardado = partes[3];
                cpuGuardado   = partes[4];
            } else {
                return new Resultado(false, "CORRUPTO", "?", "?", "?", "LIC-001");
            }

            String discoActual = getDiskSerial();
            String cpuActual   = getProcessorID();

            if (!discoActual.equals(discoGuardado)) { discoEst = "NO COINCIDE"; ok = false; }
            if (!cpuActual.equals(cpuGuardado))     { cpuEst   = "NO COINCIDE"; ok = false; }

            if (ok) {
                String hashActual = formatoViejo
                    ? sha256(partes[2] + discoActual + cpuActual + salt)  // viejo: usa MAC guardada
                    : sha256(discoActual + cpuActual + salt);              // nuevo: sin MAC
                if (!hashActual.equals(hashGuardado)) {
                    ok = false;
                    archEst = "HASH INVÁLIDO";
                }
            }

        } catch (Exception e) {
            return new Resultado(false, "ILEGIBLE", "?", "?", "?", "LIC-002");
        }

        if (!ok) {
            return new Resultado(false, archEst, macEst, discoEst, cpuEst, "LIC-003");
        }

        return new Resultado(true, archEst, macEst, discoEst, cpuEst, "LIC-OK");
    }

    // ── Sede ──────────────────────────────────────────────────────────────────

    private static String getIdSede() {
        ConexionDb d = new ConexionDb();
        try {
            d.Conectar();
            HashMap<String, String> res = d.selectOne("SELECT valor FROM config WHERE dato='id_sede'");
            if (res != null && res.get("valor") != null) return res.get("valor").trim();
        } catch (SQLException ignored) {
        } finally {
            try { d.close(); } catch (Exception ignored) { }
        }
        return "";
    }

    // ── Hardware ──────────────────────────────────────────────────────────────

    private static String getMAC() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();
                if (ni.isLoopback() || ni.isVirtual() || !ni.isUp()) continue;
                byte[] mac = ni.getHardwareAddress();
                if (mac == null) continue;
                StringBuilder sb = new StringBuilder();
                for (byte b : mac) sb.append(String.format("%02X", b));
                return sb.toString();
            }
        } catch (Exception ignored) { }
        return "NO-MAC";
    }

    private static String getDiskSerial() {
        return wmicQuery("wmic diskdrive get serialnumber");
    }

    private static String getProcessorID() {
        return wmicQuery("wmic cpu get processorid");
    }

    private static String wmicQuery(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader br = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.toLowerCase().contains("serial")
                        && !line.toLowerCase().contains("processor")) {
                    return line.replaceAll("\\s+", "");
                }
            }
        } catch (Exception ignored) { }
        return "NO-DATA";
    }

    // ── Criptografía ──────────────────────────────────────────────────────────

    private static String encriptar(String texto) throws Exception {
        SecretKeySpec key = new SecretKeySpec(AES_KEY, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] enc = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(enc);
    }

    private static String desencriptar(String hex) throws Exception {
        SecretKeySpec key = new SecretKeySpec(AES_KEY, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] dec = cipher.doFinal(hexToBytes(hex));
        return new String(dec, StandardCharsets.UTF_8);
    }

    private static String sha256(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return bytesToHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
            out[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        return out;
    }
}
