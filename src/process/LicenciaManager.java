package process;

import java.io.*;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Enumeration;
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

    // ── Activación (primera vez) ──────────────────────────────────────────────

    private static void activar(Path ruta) throws Exception {
        Files.createDirectories(ruta.getParent());

        String mac   = getMAC();
        String disco = getDiskSerial();
        String cpu   = getProcessorID();
        String salt  = UUID.randomUUID().toString();
        String hash  = sha256(mac + disco + cpu + salt);

        String datos    = hash + "|" + salt + "|" + mac + "|" + disco + "|" + cpu;
        String encHex   = encriptar(datos);

        String archivo  = "# Licencia creada por Solutic 3E"        + "\n"
                        + encHex                                      + "\n"
                        + "# Licencia autorizada - Version 21.0.0"  + "\n";

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
            String[] partes = contenido.split("\\|", 5);
            if (partes.length != 5) {
                return new Resultado(false, "CORRUPTO", "?", "?", "?", "LIC-001");
            }

            String hashGuardado = partes[0];
            String salt         = partes[1];
            String macGuardada  = partes[2];
            String discoGuardado= partes[3];
            String cpuGuardado  = partes[4];

            String macActual   = getMAC();
            String discoActual = getDiskSerial();
            String cpuActual   = getProcessorID();

            if (!macActual.equals(macGuardada))    { macEst   = "NO COINCIDE"; ok = false; }
            if (!discoActual.equals(discoGuardado)){ discoEst = "NO COINCIDE"; ok = false; }
            if (!cpuActual.equals(cpuGuardado))    { cpuEst   = "NO COINCIDE"; ok = false; }

            if (ok) {
                String hashActual = sha256(macActual + discoActual + cpuActual + salt);
                if (!hashActual.equals(hashGuardado)) {
                    ok = false;
                    archEst = "HASH INVÁLIDO";
                }
            }

        } catch (Exception e) {
            return new Resultado(false, "ILEGIBLE", "?", "?", "?", "LIC-002");
        }

        String codigo = ok ? "LIC-OK" : "LIC-003";
        return new Resultado(ok, archEst, macEst, discoEst, cpuEst, codigo);
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
