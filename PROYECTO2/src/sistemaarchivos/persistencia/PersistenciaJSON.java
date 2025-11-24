/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sistemaarchivos.persistencia;
import sistemaarchivos.constantes.constantes;
import sistemaarchivos.disco.Bloque;
import sistemaarchivos.disco.Disco;
import sistemaarchivos.EntradaSalida.SolicitudES;
import sistemaarchivos.EntradaSalida.TipoOperacionesES;
import sistemaarchivos.sistema.Archivo;
import sistemaarchivos.sistema.Directorio;
import sistemaarchivos.sistema.NodoSistema;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import sistemaarchivos.modelo.Permisos;
/**
 *
 * @author obelm
 */

public final class PersistenciaJSON {

    private PersistenciaJSON() {}

    // =========================================================
    // GUARDAR
    // =========================================================
    public static String exportarEstado(Disco disco, Directorio raiz, SolicitudES[] cola,
                                        String politica, int posCabezal) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        // ----- disco -----
        sb.append("  \"disco\": {\n");
        sb.append("    \"numBloques\": ").append(disco.obtenerNumeroBloques()).append(",\n");
        sb.append("    \"bloques\": [");
        for (int i = 0; i < disco.obtenerNumeroBloques(); i++) {
            Bloque b = disco.obtenerBloque(i);
            sb.append("{\"i\":").append(i)
              .append(",\"libre\":").append(b.estaLibre())
              .append(",\"sig\":").append(b.getSiguiente()).append("}");
            if (i < disco.obtenerNumeroBloques() - 1) sb.append(",");
        }
        sb.append("]\n  },\n");

        // ----- fs -----
        sb.append("  \"fs\": ");
        escribirNodoFS(sb, raiz);
        sb.append(",\n");

        // ----- colaES -----
        sb.append("  \"colaES\": [");
        if (cola != null) {
            for (int i = 0; i < cola.length; i++) {
                SolicitudES s = cola[i];
                sb.append("{\"pid\":\"").append(esc(s.getIdProceso())).append("\"")
                  .append(",\"tipo\":\"").append(s.getTipo().name()).append("\"")
                  .append(",\"pista\":").append(s.getPistaDestino()).append("}");
                if (i < cola.length - 1) sb.append(",");
            }
        }
        sb.append("],\n");

        // ----- planificacion -----
        sb.append("  \"planificacion\": {")
          .append("\"politica\":\"").append(esc(politica)).append("\",")
          .append("\"posCabezal\":").append(posCabezal).append(",")
          .append("\"pistas\":").append(constantes.PISTAS_DISCO)
          .append("}\n");

        sb.append("}\n");
        return sb.toString();
    }

    private static void escribirNodoFS(StringBuilder sb, NodoSistema n) {
        switch (n) {
            case Directorio d -> {
                sb.append("{\"tipo\":\"directorio\",\"nombre\":\"").append(esc(d.getNombre())).append("\",")
                        .append("\"prop\":\"").append(esc(d.getPropietario())).append("\",")
                        .append("\"hijos\":[");
                final int total = d.getHijos().tamano();
                d.getHijos().recorrer((h, i) -> {
                    escribirNodoFS(sb, h);
                    if (i < total - 1) sb.append(",");
                });
                sb.append("]}");
            }
            case Archivo a -> sb.append("{\"tipo\":\"archivo\",\"nombre\":\"").append(esc(a.getNombre())).append("\",")
                    .append("\"prop\":\"").append(esc(a.getPropietario())).append("\",")
                    .append("\"bloques\":").append(a.getTamanioEnBloques()).append(",")
                    .append("\"primer\":").append(a.getPrimerBloque()).append("}");
            default -> {
            }
        }
    }

    private static String esc(String s) {
        if (s == null) return "";
        StringBuilder r = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') r.append('\\').append(c);
            else r.append(c);
        }
        return r.toString();
    }

    public static void guardarEnArchivo(Path destino, String json) throws IOException {
        Files.write(destino, json.getBytes(StandardCharsets.UTF_8));
    }

    // =========================================================
    // CARGAR
    // =========================================================
    public static final class ResultadoCarga {
        public Disco disco;
        public Directorio raiz;
        public SolicitudES[] cola;
        public String politica;
        public int posCabezal;
    }

    public static ResultadoCarga cargarDesdeArchivo(Path origen) throws IOException {
        String t = Files.readString(origen, StandardCharsets.UTF_8);
        if (t == null || t.trim().length() < 2 || t.indexOf('{') < 0) {
            throw new IOException("Archivo JSON vacío o inválido.");
        }
        return parsear(t);
    }

    private static ResultadoCarga parsear(String t) {
        ResultadoCarga r = new ResultadoCarga();

        // ---------- disco ----------
        String objDisco = seccionObjeto(t, "\"disco\"");
        int numBloques = entero(objDisco, "\"numBloques\"", -1);
        if (numBloques <= 0) throw new IllegalArgumentException("numBloques inválido en 'disco'.");
        r.disco = new Disco(numBloques);

        String arrBloques = seccionArreglo(objDisco, "\"bloques\"");
        int p = 0;
        while (true) {
            int o = arrBloques.indexOf("{", p);
            if (o < 0) break;
            int c = cierreDe(arrBloques, o, '{', '}');
            if (c < 0) throw new IllegalArgumentException("Bloque mal formado en 'bloques'.");
            String item = arrBloques.substring(o, c + 1);

            int i = entero(item, "\"i\"", -1);
            boolean libre = bool(item, "\"libre\"");
            int sig = entero(item, "\"sig\"", -1);
            if (i >= 0 && i < numBloques) {
                if (!libre) r.disco.ocuparBloque(i, sig);
                else r.disco.liberarBloque(i);
            }
            p = c + 1;
        }

        // ---------- fs ----------
        String objFS = seccionObjeto(t, "\"fs\"");
        r.raiz = (Directorio) parseNodoFS(objFS);

        // ---------- colaES ----------
        String arrCola = seccionArreglo(t, "\"colaES\"");
        // contar
        int cnt = 0; p = 0;
        while (true) { int o2 = arrCola.indexOf("{", p); if (o2 < 0) break; cnt++; p = cierreDe(arrCola, o2, '{', '}') + 1; }
        r.cola = new SolicitudES[cnt];
        p = 0; int k = 0;
        while (true) {
            int o2 = arrCola.indexOf("{", p); if (o2 < 0) break;
            int c2 = cierreDe(arrCola, o2, '{', '}');
            String it = arrCola.substring(o2, c2 + 1);
            String pid = texto(it, "\"pid\"");
            String tipo = texto(it, "\"tipo\"");
            int pista = entero(it, "\"pista\"", 0);
            r.cola[k++] = new SolicitudES(pid, TipoOperacionesES.valueOf(tipo), pista);
            p = c2 + 1;
        }

        // ---------- planificacion ----------
        String objPlan = seccionObjeto(t, "\"planificacion\"");
        r.politica = texto(objPlan, "\"politica\"");
        r.posCabezal = entero(objPlan, "\"posCabezal\"", 0);

        return r;
    }

    private static NodoSistema parseNodoFS(String obj) {
        String tipo = texto(obj, "\"tipo\"");
        if ("directorio".equals(tipo)) {
            String nombre = texto(obj, "\"nombre\"");
            String prop = texto(obj, "\"prop\"");
            Directorio d = new Directorio(nombre, prop, Permisos.porDefectoDirectorio());
            String arrHijos = seccionArreglo(obj, "\"hijos\"");
            int p = 0;
            while (true) {
                int o = arrHijos.indexOf("{", p);
                if (o < 0) break;
                int c = cierreDe(arrHijos, o, '{', '}');
                d.agregarHijo(parseNodoFS(arrHijos.substring(o, c + 1)));
                p = c + 1;
            }
            return d;
        } else if ("archivo".equals(tipo)) {
            String nombre = texto(obj, "\"nombre\"");
            String prop = texto(obj, "\"prop\"");
            int bloques = entero(obj, "\"bloques\"", 0);
            int primer = entero(obj, "\"primer\"", -1);
            return new Archivo(nombre, prop, Permisos.porDefectoArchivo(), bloques, primer);
        }
        throw new IllegalArgumentException("Nodo FS con 'tipo' desconocido: " + tipo);
    }

    // =========================================================
    // utilidades de búsqueda/parseo (sin java.util.*)
    // =========================================================
    private static int cierreDe(String t, int apertura, char open, char close) {
        if (apertura < 0 || apertura >= t.length()) return -1;
        int nivel = 0;
        for (int i = apertura; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == open) nivel++;
            else if (c == close) {
                nivel--;
                if (nivel == 0) return i;
            }
        }
        return -1;
    }

    private static String seccionObjeto(String t, String clave) {
        int i = t.indexOf(clave);
        if (i < 0) throw new IllegalArgumentException("Falta sección " + clave + ".");
        int o = t.indexOf("{", i);
        int c = cierreDe(t, o, '{', '}');
        if (o < 0 || c < 0) throw new IllegalArgumentException("Objeto mal formado en " + clave + ".");
        return t.substring(o, c + 1);
    }

    private static String seccionArreglo(String t, String clave) {
        int i = t.indexOf(clave);
        if (i < 0) throw new IllegalArgumentException("Falta sección " + clave + ".");
        int o = t.indexOf("[", i);
        int c = cierreDe(t, o, '[', ']');
        if (o < 0 || c < 0) throw new IllegalArgumentException("Arreglo mal formado en " + clave + ".");
        return t.substring(o + 1, c); // sin corchetes
    }

    private static int entero(String t, String clave, int def) {
        int i = t.indexOf(clave);
        if (i < 0) return def;
        int c = t.indexOf(":", i) + 1;
        int e = c;
        while (e < t.length() && " -0123456789".indexOf(t.charAt(e)) >= 0) e++;
        try { return Integer.parseInt(t.substring(c, e).trim()); }
        catch (NumberFormatException ex) { return def; }
    }

    private static boolean bool(String t, String clave) {
        int i = t.indexOf(clave);
        if (i < 0) return false;
        int c = t.indexOf(":", i) + 1;
        String s = t.substring(c, Math.min(t.length(), c + 5)).trim();
        return s.startsWith("true");
    }

    private static String texto(String t, String clave) {
        int i = t.indexOf(clave);
        if (i < 0) return "";
        int q1 = t.indexOf("\"", t.indexOf(":", i) + 1);
        int q2 = (q1 >= 0) ? t.indexOf("\"", q1 + 1) : -1;
        if (q1 < 0 || q2 < 0) return "";
        return desesc(t.substring(q1 + 1, q2));
    }

    private static String desesc(String s) {
        StringBuilder r = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) { r.append(s.charAt(i + 1)); i++; }
            else r.append(c);
        }
        return r.toString();
    }
}