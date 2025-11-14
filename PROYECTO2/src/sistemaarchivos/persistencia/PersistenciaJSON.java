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
/**
 *
 * @author obelm
 */
/** Serialización / deserialización a JSON sin librerías externas. */
public final class PersistenciaJSON {

    private PersistenciaJSON() {}

    // --------------------------- GUARDAR ---------------------------
    public static String exportarEstado(Disco disco, Directorio raiz, SolicitudES[] cola,
                                        String politica, int posCabezal) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        // disco
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

        // fs
        sb.append("  \"fs\": ");
        escribirNodoFS(sb, raiz);
        sb.append(",\n");

        // cola
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

        // planificacion
        sb.append("  \"planificacion\": {\"politica\":\"").append(esc(politica))
          .append("\",\"posCabezal\":").append(posCabezal)
          .append(",\"pistas\":").append(constantes.PISTAS_DISCO).append("}\n");

        sb.append("}\n");
        return sb.toString();
    }

    private static void escribirNodoFS(StringBuilder sb, NodoSistema n) {
        if (n instanceof Directorio d) {
            sb.append("{\"tipo\":\"directorio\",\"nombre\":\"").append(esc(d.getNombre())).append("\",")
              .append("\"prop\":\"").append(esc(d.getPropietario())).append("\",")
              .append("\"hijos\":[");
            final int total = d.getHijos().tamano();
            d.getHijos().recorrer((h, i) -> {
                escribirNodoFS(sb, h);
                if (i < total - 1) sb.append(",");
            });
            sb.append("]}");
        } else if (n instanceof Archivo a) {
            sb.append("{\"tipo\":\"archivo\",\"nombre\":\"").append(esc(a.getNombre())).append("\",")
              .append("\"prop\":\"").append(esc(a.getPropietario())).append("\",")
              .append("\"bloques\":").append(a.getTamanioEnBloques()).append(",")
              .append("\"primer\":").append(a.getPrimerBloque()).append("}");
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

    // --------------------------- CARGAR ---------------------------
    public static final class ResultadoCarga {
        public Disco disco;
        public Directorio raiz;
        public SolicitudES[] cola;
        public String politica;
        public int posCabezal;
    }

    public static ResultadoCarga cargarDesdeArchivo(Path origen) throws IOException {
        String t = Files.readString(origen, StandardCharsets.UTF_8);
        return parsear(t);
    }

    // Parser específico (simple pero suficiente para nuestro formato)
    private static ResultadoCarga parsear(String t) {
        ResultadoCarga r = new ResultadoCarga();

        // disco
        int numBloques = entero(t, "\"numBloques\"", 0);
        r.disco = new Disco(numBloques);

        int iBloques = t.indexOf("\"bloques\"");
        int ab = t.indexOf("[", iBloques), cb = t.indexOf("]", ab);
        String arr = t.substring(ab + 1, cb);
        int p = 0;
        while (true) {
            int ii = arr.indexOf("{\"i\":", p);
            if (ii < 0) break;
            int ci = arr.indexOf("}", ii);
            String item = arr.substring(ii, ci + 1);
            int i = entero(item, "\"i\"", 0);
            boolean libre = bool(item, "\"libre\"");
            int sig = entero(item, "\"sig\"", -1);
            if (!libre) r.disco.ocuparBloque(i, sig);
            else r.disco.liberarBloque(i);
            p = ci + 1;
        }

        // fs (raiz)
        int ifs = t.indexOf("\"fs\"");
        int o = t.indexOf("{", ifs);
        int c = finObjeto(t, o);
        r.raiz = (Directorio) parseNodoFS(t.substring(o, c + 1));

        // cola
        int ic = t.indexOf("\"colaES\"");
        int ac = t.indexOf("[", ic), cc = t.indexOf("]", ac);
        String colaTxt = t.substring(ac + 1, cc);
        int cnt = 0; p = 0;
        while (true) { int j = colaTxt.indexOf("{", p); if (j < 0) break; cnt++; p = colaTxt.indexOf("}", j) + 1; }
        r.cola = new SolicitudES[cnt];
        p = 0; int k = 0;
        while (true) {
            int j = colaTxt.indexOf("{", p); if (j < 0) break;
            int q = colaTxt.indexOf("}", j);
            String it = colaTxt.substring(j, q + 1);
            String pid = texto(it, "\"pid\"");
            String tipo = texto(it, "\"tipo\"");
            int pista = entero(it, "\"pista\"", 0);
            r.cola[k++] = new SolicitudES(pid, TipoOperacionesES.valueOf(tipo), pista);
            p = q + 1;
        }

        // planificacion
        int ip = t.indexOf("\"planificacion\"");
        int op = t.indexOf("{", ip), cp = t.indexOf("}", op);
        String plan = t.substring(op, cp + 1);
        r.politica = texto(plan, "\"politica\"");
        r.posCabezal = entero(plan, "\"posCabezal\"", 0);
        return r;
    }

    private static NodoSistema parseNodoFS(String obj) {
        String tipo = texto(obj, "\"tipo\"");
        if ("directorio".equals(tipo)) {
            String nombre = texto(obj, "\"nombre\"");
            String prop = texto(obj, "\"prop\"");
            Directorio d = new Directorio(nombre, prop, sistemaarchivos.modelo.Permisos.porDefectoDirectorio());
            int ih = obj.indexOf("\"hijos\""); int a = obj.indexOf("[", ih), b = obj.indexOf("]", a);
            String hs = obj.substring(a + 1, b);
            int p = 0;
            while (true) {
                int j = hs.indexOf("{", p); if (j < 0) break;
                int q = finObjeto(hs, j);
                d.agregarHijo(parseNodoFS(hs.substring(j, q + 1)));
                p = q + 1;
            }
            return d;
        } else {
            String nombre = texto(obj, "\"nombre\"");
            String prop = texto(obj, "\"prop\"");
            int bloques = entero(obj, "\"bloques\"", 0);
            int primer = entero(obj, "\"primer\"", -1);
            return new Archivo(nombre, prop, sistemaarchivos.modelo.Permisos.porDefectoArchivo(), bloques, primer);
        }
    }

    // ------------ utilidades de parseo (simples) ------------
    private static int entero(String t, String clave, int def) {
        int i = t.indexOf(clave); if (i < 0) return def;
        int c = t.indexOf(":", i) + 1; int e = c;
        while (e < t.length() && " -0123456789".indexOf(t.charAt(e)) >= 0) e++;
        try { return Integer.parseInt(t.substring(c, e).trim()); } catch (Exception ex) { return def; }
    }
    private static boolean bool(String t, String clave) {
        int i = t.indexOf(clave); int c = t.indexOf(":", i) + 1;
        String s = t.substring(c, Math.min(t.length(), c + 5)).trim(); return s.startsWith("true");
    }
    private static String texto(String t, String clave) {
        int i = t.indexOf(clave); int q1 = t.indexOf("\"", t.indexOf(":", i) + 1); int q2 = t.indexOf("\"", q1 + 1);
        return desesc(t.substring(q1 + 1, q2));
    }
    private static int finObjeto(String t, int ini) {
        int nivel = 0; for (int i = ini; i < t.length(); i++) {
            char c = t.charAt(i); if (c == '{') nivel++; if (c == '}') { nivel--; if (nivel == 0) return i; } }
        return ini;
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
