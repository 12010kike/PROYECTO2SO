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
    private final Directorio raiz;

    public GestorSistemaArchivos(Disco disco) {
        this.disco = disco;
        this.raiz = new Directorio("raiz", "admin", Permisos.porDefectoDirectorio());
        // Directorios base
        Directorio sistema = new Directorio("sistema", "admin", Permisos.porDefectoDirectorio());
        Directorio usuarios = new Directorio("usuarios", "admin", Permisos.porDefectoDirectorio());
        raiz.agregarHijo(sistema);
        raiz.agregarHijo(usuarios);
    }

    public Directorio getRaiz() {
        return raiz;
    }

    // ------- UTILIDAD PARA BUSCAR DIRECTORIO POR RUTA SIMPLE "raiz/usuarios" -------
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
        // no usar String.split con regex complejos; la forma básica es suficiente aquí
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

    // -------------------- CRUD DIRECTORIO --------------------
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
            // liberar archivos recursivamente
            liberarArchivosRecursivo(d);
            return padre.eliminarHijoPorNombre(nombre);
        }
        return false;
    }

    private void liberarArchivosRecursivo(Directorio dir) {
        // eliminar hijos: si archivo -> liberar cadena, si directorio -> bajar
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
        // borrar marcados por nombre
        aEliminar.recorrer((nombre, i) -> dir.eliminarHijoPorNombre(nombre));
    }

    // -------------------- CRUD ARCHIVO --------------------
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

    // -------------------- ASIGNACIÓN ENCANDENADA (FAT SIMPLE) --------------------
    /**
     * Asigna 'cantidad' bloques libres encadenados.
     * Retorna el índice del primer bloque o -1 si no hay suficientes.
     */
    private int asignarCadena(int cantidad) {
        // buscar bloques libres y encadenarlos
        int anterior = -1;
        int primero = -1;
        int encontrados = 0;

        for (int i = 0; i < disco.obtenerNumeroBloques() && encontrados < cantidad; i++) {
            if (disco.obtenerBloque(i).estaLibre()) {
                if (primero < 0) primero = i;
                if (anterior >= 0) {
                    disco.ocuparBloque(anterior, i); // enlazar anterior -> i
                }
                anterior = i;
                encontrados++;
                disco.ocuparBloque(i, Bloque.FIN_CADENA); // marcar ocupado provisionalmente
            }
        }
        if (encontrados < cantidad) {
            // rollback de los que se ocuparon
            int idx = primero;
            while (idx >= 0 && idx != Bloque.LIBRE) {
                int sig = disco.obtenerBloque(idx).getSiguiente();
                disco.liberarBloque(idx);
                if (sig == Bloque.FIN_CADENA || sig == Bloque.LIBRE) break;
                idx = sig;
            }
            return -1;
        }
        return primero;
    }

    /** Libera toda la cadena a partir del primer bloque. */
    private void liberarCadena(int primerBloque) {
        int idx = primerBloque;
        while (idx >= 0) {
            int sig = disco.obtenerBloque(idx).getSiguiente();
            disco.liberarBloque(idx);
            if (sig == Bloque.FIN_CADENA || sig == Bloque.LIBRE) {
                break;
            }
            idx = sig;
        }
    }
}
