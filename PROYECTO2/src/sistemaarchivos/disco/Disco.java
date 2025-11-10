/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sistemaarchivos.disco;

import sistemaarchivos.constantes.constantes;

/**
 *
 * @author eabdf
 */
public final class Disco {
    private final Bloque[] bloques;

    public Disco(int numeroBloques) {
        this.bloques = new Bloque[numeroBloques];
        for (int i = 0; i < numeroBloques; i++) {
            bloques[i] = new Bloque(i);
        }
    }

    public static Disco crearPorDefecto() {
        return new Disco(constantes.NUMERO_BLOQUES);
    }

    public int obtenerNumeroBloques() {
        return bloques.length;
    }

    public Bloque obtenerBloque(int indice) {
        if (indice < 0 || indice >= bloques.length) throw new IndexOutOfBoundsException("Índice inválido de bloque");
        return bloques[indice];
    }

    public boolean[] mapaLibres() {
        boolean[] libres = new boolean[bloques.length];
        for (int i = 0; i < bloques.length; i++) {
            libres[i] = bloques[i].estaLibre();
        }
        return libres;
    }

    /**
     * Marca un bloque como ocupado y fija su siguiente.
     * Uso de bajo nivel (la lógica de encadenamiento se hará en el gestor del sistema de archivos).
     * @param indice
     * @param siguiente
     */
    public void ocuparBloque(int indice, int siguiente) {
        Bloque b = obtenerBloque(indice);
        b.ocupar();
        b.setSiguiente(siguiente);
    }

    /** Libera un bloque individual (no la cadena completa).
     * @param indice */
    public void liberarBloque(int indice) {
        obtenerBloque(indice).liberar();
    }

    /** Reset total del disco. */
    public void formatear() {
        for (Bloque bloque : bloques) {
            bloque.liberar();
        }
    }
}
