package process;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import database.ConexionDb;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Envía una petición JSON al servidor (por POST form-urlencoded) y devuelve
 * un HashMap<String,String> con el resultado “aplanado”.
 *
 * - Si la respuesta es un JSONArray: genera pares i_campo -> valor y añade:
 *     size -> cantidad de elementos
 *     keys -> lista de campos originales separada por coma
 * - Si la respuesta es un JSONObject: genera pares campo -> valor
 *
 * Todas las conversiones a String son tolerantes (String.valueOf).
 *
 * Manejo de errores:
 * - Para HTTP 404: mensaje incluye el nombre del servicio y un extracto del cuerpo.
 * - getHttpStatus()/getErrorMessage() permiten a la capa superior mostrar en UI.
 */
public class JSONTask {

    /** Placeholder que se reemplaza con el valor tomado de la tabla config (dato='ip'). */
    private static String ruta = "XXX/blank_ws_huellero";
    //private static String ruta = "XXX";
    //private static String ruta = "XXX/huellas/index.php/Servicios";
    /** Expuesto para diagnóstico: último host/ip leído desde config.ip */
    public static String wqueruta;

    /** Nombre o path del servicio (lo que se concatena a la ruta base). */
    private final String servicio;

    /** Parámetros a enviar (se serializan a JSON y se mandan como data=Base64(JSON)). */
    private final HashMap<String, String> parametros;

    /** Resultado ya parseado a mapa plano. */
    public HashMap<String, String> respuesta;

    /** Señal de error en transporte o parseo. */
    public boolean error = false;

    /** Mensaje de error amigable para UI. */
    public String mensaje_e = "";

    /** Datos crudos / estado HTTP para diagnóstico. */
    private int httpStatus = 0;
    private String httpReason = "";
    private String rawResponse = "";

    /**
     * @param servicio   Nombre del servicio (ej: "getEmpleadosAll_jhcm_ht", "setEventos_jhcm_ht")
     * @param parametros HashMap de parámetros que se codifican como JSON -> Base64 -> form-urlencoded
     */
    public JSONTask(String servicio, HashMap<String, String> parametros) {

        // Leer IP/host del webservice desde la BD local (config.dato='ip')
        ConexionDb d = new ConexionDb();
        try {
            d.Conectar();

            HashMap<String, String> res_has_ip = d.selectOne("SELECT valor FROM config WHERE dato='ip'");
            String res_has_ip_str = "000.000.000.000:00";

            if (res_has_ip != null) {
                res_has_ip_str = res_has_ip.get("valor");
            } else {
                System.out.println("++++++++++++++++++++++++++++++++++ NO IP DATA");
            }
            wqueruta = res_has_ip_str;

            // Reemplaza placeholder por IP/host configurado solo si sigue en "XXX"
            ruta = "XXX".equals(ruta) ? res_has_ip_str : ruta.replace("XXX", res_has_ip_str);

            System.out.println("Ruta de sincronizacion " + ruta);
        } catch (SQLException ex) {
            Logger.getLogger(JSONTask.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try { d.close(); } catch (SQLException ignore) {}
        }

        this.parametros = parametros;
        this.servicio = servicio;
    }

    // ------------------------------------------------------------------------------------
    // API pública
    // ------------------------------------------------------------------------------------

    /**
     * Ejecuta la petición y muestra (opcionalmente) un mensaje de error en UI.
     * @return true si todo OK (HTTP y JSON); false si hubo error
     */
    public boolean enviar(boolean mostrar_men, Object frame) {
        respuesta = exe();

        if (error) {
            if (mostrar_men && (frame instanceof Component)) {
                JOptionPane.showMessageDialog((Component) frame, this.mensaje_e);
            }
            return false;
        }
        return true;
    }

    /** Estado HTTP de la última ejecución (0 si no se alcanzó el servidor). */
    public int getHttpStatus() { return httpStatus; }

    /** Motivo HTTP (reason phrase) si el servidor lo suministra. */
    public String getHttpReason() { return httpReason == null ? "" : httpReason; }

    /** Cuerpo crudo devuelto por el servidor (útil para diagnóstico). */
    public String getRawResponse() { return rawResponse == null ? "" : rawResponse; }

    /** Mensaje de error preparado para UI (incluye servicio y snippet en 404). */
    public String getErrorMessage() { return mensaje_e == null ? "" : mensaje_e; }

    /** Nombre del servicio invocado (para logs). */
    public String getServicio() { return servicio; }

    /** Mapa de respuesta “aplanada”. */
    public HashMap<String, String> getRespuesta() { return respuesta; }

    // ------------------------------------------------------------------------------------
    // Núcleo
    // ------------------------------------------------------------------------------------

    /**
     * Hace el POST y parsea la salida a HashMap<String,String>.
     */
    protected HashMap<String, String> exe() {

        HttpURLConnection connection = null;
        String salida = null;
        HashMap<String, String> salida2 = null;

        try {
            System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");

            // 1) Serializar parámetros a JSON
            JSONObject ob = new JSONObject(parametros);
            String json = ob.toString();
            System.out.println("Enviar JSON: " + json);
            System.out.println("Enviar MAP:  " + parametros);

            // 2) Codificar el JSON
            Charset bodyCharset = StandardCharsets.UTF_8;
            byte[] ptext = json.getBytes(bodyCharset);

            // 3) Base64 del JSON y URL-encode para enviar como form-urlencoded
            String codificado = Base64.getEncoder().encodeToString(ptext);
            //String data = "data=" + URLEncoder.encode(codificado, StandardCharsets.UTF_8);
            String data = "data=" + URLEncoder.encode(codificado, "UTF-8");
            System.out.println("Data POST: " + (data.length() > 120 ? data.substring(0, 120) + "..." : data));

            // 4) Construcción robusta de la URL
            //    Nota: Se asume que 'ruta' ya viene con esquema/puerto si aplica, y que 'servicio' es el path final.
            String base = ruta+"/?fn="; 
            // Si tu servicio requiere esquema forzado, descomenta:
            /*
            if (!base.startsWith("http://") && !base.startsWith("https://")) {
                base = "http://" + base;  // cambia a https si aplica
            }
            */
            URI uri;
            try {
                // No agregamos "/" automáticamente para respetar tu backend tal cual lo usabas
                uri = new URI(base + servicio);
            } catch (URISyntaxException e) {
                //mensaje_e = "URI inválida: " + (base + servicio) + " -> " + e.getMessage();
                mensaje_e = "URI inválida, falta Configuración ";
                error = true;
                return null;
            }
            URL url = uri.toURL();

            System.out.println("Enviando a: " + base + servicio);
            System.out.println("URL Host:  " + url.getHost());
            System.out.println("URL File:  " + url.getFile());

            // 5) Abrir conexión y configurar
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3500);
            connection.setReadTimeout(10000); // evita bloqueos indefinidos
            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-Agent", "JSONTask/1.0");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setRequestProperty("Content-Length",
                    Integer.toString(data.getBytes(StandardCharsets.UTF_8).length));
            connection.setRequestProperty("Content-Language", "es-CO");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            // 6) Enviar cuerpo
            try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                wr.write(data.getBytes(StandardCharsets.UTF_8));
                wr.flush();
            }

            // 7) Conectar y leer
            long t0 = System.currentTimeMillis();
            connection.connect();
            httpStatus = connection.getResponseCode();
            httpReason = safeStr(connection.getResponseMessage());
            long t1 = System.currentTimeMillis();
            System.out.println("HTTP=" + httpStatus + " (" + (t1 - t0) + " ms) Reason=" + httpReason);

            try (InputStream stream = (httpStatus >= 200 && httpStatus < 300)
                        ? connection.getInputStream()
                        : connection.getErrorStream();
                 BufferedReader reader = new BufferedReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8))) {

                StringBuilder buffer = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) buffer.append(line);
                salida = buffer.toString();
            }

            rawResponse = salida == null ? "" : salida;
            System.out.println("Salida: " + (rawResponse.length() > 200 ? rawResponse.substring(0, 200) + "..." : rawResponse));

            // Manejo 404 con mensaje que sube a UI (Sincronizar.wsError lo usa tal cual)
            if (httpStatus == 404) {
                String snippet = snippet(rawResponse, 400);
                String reason = httpReason.isEmpty() ? "Not Found" : httpReason;
               // mensaje_e = "WebService no encontrado: " + servicio + " (HTTP 404 - " + reason + ")" + (snippet.isEmpty() ? "" : " - " + snippet);
                mensaje_e = "WebService no encontrado: " + servicio;
                error = true;
                return null;
            }

            // HTTP fuera de 2xx
            if (httpStatus < 200 || httpStatus >= 300) {
                String snippet = snippet(rawResponse, 400);
                String reason = httpReason.isEmpty() ? ("HTTP " + httpStatus) : httpReason;
                mensaje_e = "Error HTTP " + httpStatus + " (" + reason + ") en servicio: " + servicio
                          + (snippet.isEmpty() ? "" : " - " + snippet);
                error = true;
                return null;
            }

            if (salida == null) {
                mensaje_e = "Respuesta nula del servidor.";
                error = true;
                return null;
            }

            // 8) Parseo tolerante
            String trimmed = salida.trim();
            HashMap<String, String> map = new HashMap<>();

            if (trimmed.startsWith("[")) {
                // ---- Respuesta: Array de objetos ----
                JSONArray jsonarray = new JSONArray(trimmed);

                ArrayList<String> keys_originales = new ArrayList<>();
                boolean uno = false;

                for (int i = 0; i < jsonarray.length(); i++) {
                    JSONObject jObject = jsonarray.getJSONObject(i);

                    Iterator<?> keys = jObject.keys();
                    while (keys.hasNext()) {
                        String key_aux = (String) keys.next();
                        if (!uno) keys_originales.add(key_aux);

                        String key = i + "_" + key_aux;
                        Object v = jObject.opt(key_aux);
                        String value = (v == null) ? "" : String.valueOf(v);

                        map.put(key, value);
                    }
                    uno = true;
                }

                map.put("size", String.valueOf(jsonarray.length()));
                String aToString = java.util.Arrays.toString(keys_originales.toArray());
                aToString = aToString.substring(1, aToString.length() - 1).replace(" ", "");
                map.put("keys", aToString);

                salida2 = map;

            } else if (trimmed.startsWith("{")) {
                // ---- Respuesta: Objeto único ----
                JSONObject jObject = new JSONObject(trimmed);

                Iterator<?> keys = jObject.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    Object v = jObject.opt(key);
                    String value = (v == null) ? "" : String.valueOf(v);
                    map.put(key, value);
                }

                salida2 = map;

            } else {
                // ---- Ni JSON array ni JSON object (probablemente HTML de error) ----
                mensaje_e = "Respuesta no-JSON: " + (trimmed.length() > 200 ? trimmed.substring(0, 200) + "..." : trimmed);
                error = true;
                return null;
            }

        } catch (MalformedURLException e) {
            mensaje_e = "Error de conectividad: URL mal formada: " + e.getMessage();
            error = true;

        } catch (SocketException e) {
            mensaje_e = "En este momento no hay conexión al servidor, intente más tarde";
            error = true;

        } catch (IOException e) {
            mensaje_e = "Error de conectividad (IO): " + e.getMessage();
            error = true;

        } catch (JSONException e) {
            mensaje_e = "Error JSON: " + e.getMessage();
            error = true;

        } finally {
            if (connection != null) connection.disconnect();
        }

        return salida2;
    }

    // ------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------

    private static String safeStr(String s) { return s == null ? "" : s; }

    /**
     * Devuelve un extracto (limpio) del cuerpo para mostrar en errores.
     * - Elimina saltos repetidos.
     * - Trunca a maxLen chars.
     */
    private static String snippet(String body, int maxLen) {
        if (body == null) return "";
        String s = body.replaceAll("[\\r\\n]+", " ").trim();
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "...";
    }
}
