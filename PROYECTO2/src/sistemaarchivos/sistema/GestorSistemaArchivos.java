/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sistemaarchivos.sistema;

import sistemaarchivos.disco.Bloque;
import sistemaarchivos.disco.Disco;
import sistemaarchivos.estructuras.ListaEnlazada;
import sistemaarchivos.modelo.Permisos;

/**
 *
 * @author eabdf
 */
public final class GestorSistemaArchivos {
   private final Disco disco;
    private Directorio raiz;

    public GestorSistemaArchivos(Disco disco) {
        this.disco = disco;
        this.raiz = new Directorio("raiz", "admin", Permisos.porDefectoDirectorio());
        Directorio sistema  = new Directorio("sistema",  "admin", Permisos.porDefectoDirectorio());
        Directorio usuarios = new Directorio("usuarios", "admin", Permisos.porDefectoDirectorio());
        raiz.agregarHijo(sistema);
        raiz.agregarHijo(usuarios);
    }

    public Directorio getRaiz() { return raiz; }

    /** Reemplaza la raíz (se usa al cargar JSON).
     * @param nuevaRaiz */
    public void reemplazarRaiz(Directorio nuevaRaiz) {
        if (nuevaRaiz != null) this.raiz = nuevaRaiz;
    }

    // =========================================================
    //   BÚSQUEDA DE DIRECTORIO POR RUTA "raiz/usuarios/..."
    // =========================================================
    public Directorio obtenerDirectorioPorRuta(String ruta) {
        if (ruta == null || ruta.isEmpty() || "raiz".equals(ruta)) return raiz;
        String[] partes = dividirRuta(ruta);
        Directorio actual = raiz;
        for (int i = 1; i < partes.length; i++) { // saltar "raiz"
            NodoSistema hijo = actual.obtenerHijoPorNombre(partes[i]);
            if (hijo instanceof Directorio d) {
                actual = d;
            } else {
                return null;
            }
        }
        return actual;
    }

    private String[] dividirRuta(String ruta) {
        // Sin java.util. Construimos a mano.
        ListaEnlazada<String> partes = new ListaEnlazada<>();
        String acum = "";
        for (int i = 0; i < ruta.length(); i++) {
            char c = ruta.charAt(i);
            if (c == '/') {
                if (!acum.isEmpty()) {
                    partes.agregarAlFinal(acum);
                    acum = "";
                }
            } else {
                acum += c;
            }
        }
        if (!acum.isEmpty()) partes.agregarAlFinal(acum);

        String[] arr = new String[partes.tamano()];
        final int[] k = {0};
        partes.recorrer((s, i) -> arr[k[0]++] = s);
        return arr;
    }

    // =========================================================
    //                       CRUD DIRECTORIO
    // =========================================================
    public boolean crearDirectorio(String rutaPadre, String nombre, String propietario) {
        Directorio padre = obtenerDirectorioPorRuta(rutaPadre);
        if (padre == null) return false;
        if (padre.obtenerHijoPorNombre(nombre) != null) return false; // ya existe

        Directorio nuevo = new Directorio(nombre, propietario, Permisos.porDefectoDirectorio());
        padre.agregarHijo(nuevo);
        return true;
    }

    public boolean eliminarDirectorioRecursivo(String rutaPadre, String nombre) {
        Directorio padre = obtenerDirectorioPorRuta(rutaPadre);
        if (padre == null) return false;
        NodoSistema candidato = padre.obtenerHijoPorNombre(nombre);
        if (candidato instanceof Directorio d) {
            // liberar archivos recursivamente antes de eliminar
            liberarArchivosRecursivo(d);
            return padre.eliminarHijoPorNombre(nombre);
        }
        return false;
    }

    private void liberarArchivosRecursivo(Directorio dir) {
        // Borrado postorden: si archivo -> liberar cadena; si directorio -> bajar
        ListaEnlazada<NodoSistema> hijos = dir.getHijos();
        final ListaEnlazada<String> aEliminar = new ListaEnlazada<>();
        hijos.recorrer((n, i) -> {
            if (n instanceof Archivo a) {
                liberarCadena(a.getPrimerBloque());
                aEliminar.agregarAlFinal(a.getNombre());
            } else if (n instanceof Directorio sub) {
                liberarArchivosRecursivo(sub);
                aEliminar.agregarAlFinal(sub.getNombre());
            }
        });
        // Eliminar marcados
        aEliminar.recorrer((nombre, i) -> dir.eliminarHijoPorNombre(nombre));
    }

    // =========================================================
    //                        CRUD ARCHIVO
    // =========================================================
    public boolean crearArchivo(String rutaPadre, String nombre, String propietario, int bloquesSolicitados) {
        if (bloquesSolicitados <= 0) return false;
        Directorio padre = obtenerDirectorioPorRuta(rutaPadre);
        if (padre == null) return false;
        if (padre.obtenerHijoPorNombre(nombre) != null) return false; // ya existe

        int primer = asignarCadena(bloquesSolicitados);
        if (primer < 0) return false; // no hay espacio

        Archivo nuevo = new Archivo(nombre, propietario, Permisos.porDefectoArchivo(), bloquesSolicitados, primer);
        padre.agregarHijo(nuevo);
        return true;
    }

    public boolean eliminarArchivo(String rutaPadre, String nombre) {
        Directorio padre = obtenerDirectorioPorRuta(rutaPadre);
        if (padre == null) return false;
        NodoSistema n = padre.obtenerHijoPorNombre(nombre);
        if (n instanceof Archivo a) {
            liberarCadena(a.getPrimerBloque());
            return padre.eliminarHijoPorNombre(nombre);
        }
        return false;
    }

    // =========================================================
    //                          RENOMBRAR
    // =========================================================
    /**
     * Renombra un hijo de {@code rutaPadre} si no hay colisión de nombres.
     * @param rutaPadre
     * @param nombreActual
     * @param nuevoNombre
     * @return true si se renombró; false si ruta inválida, no existe el hijo, o hay colisión.
     */
    public boolean renombrar(String rutaPadre, String nombreActual, String nuevoNombre) {
        Directorio padre = obtenerDirectorioPorRuta(rutaPadre);
        if (padre == null) return false;
        if (nuevoNombre == null || nuevoNombre.trim().isEmpty()) return false;

        // Evitar colisión
        if (padre.obtenerHijoPorNombre(nuevoNombre.trim()) != null) return false;

        NodoSistema objetivo = padre.obtenerHijoPorNombre(nombreActual);
        if (objetivo == null) return false;

        // Requiere que NodoSistema tenga setNombre(String) (ya lo agregamos).
        objetivo.setNombre(nuevoNombre.trim());
        return true;
    }

    // =========================================================
    //             ASIGNACIÓN ENCANDENADA (FAT SIMPLE)
    // =========================================================
    /**
     * Asigna 'cantidad' bloques libres encadenados y retorna el índice del primer bloque,
     * o -1 si no hay suficientes.
     */
    private int asignarCadena(int cantidad) {
        int anterior = -1;
        int primero  = -1;
        int usados   = 0;

        for (int i = 0; i < disco.obtenerNumeroBloques() && usados < cantidad; i++) {
            if (disco.obtenerBloque(i).estaLibre()) {
                if (primero < 0) primero = i;
                if (anterior >= 0) {
                    // enlazar anterior -> i
                    disco.ocuparBloque(anterior, i);
                }
                // ocupar i (de momento como fin)
                disco.ocuparBloque(i, Bloque.FIN_CADENA);
                anterior = i;
                usados++;
            }
        }

        if (usados < cantidad) {
            // rollback de lo que se alcanzó a ocupar
            liberarCadena(primero);
            return -1;
        }
        return primero;
    }

  
    private void liberarCadena(int primerBloque) {
        int idx = primerBloque;
        while (idx >= 0) {
            int sig = disco.obtenerBloque(idx).getSiguiente();
            disco.liberarBloque(idx);
            if (sig == Bloque.FIN_CADENA || sig == Bloque.LIBRE) break;
            idx = sig;
        }
    }
}