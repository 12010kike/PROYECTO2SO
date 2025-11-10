/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sistemaarchivos.disco;

/**
 *
 * @author eabdf
 */
public final class Bloque {
    public static final int FIN_CADENA = -1;
    public static final int LIBRE      = -2;

    private final int indice;
    private boolean libre;
    private int siguiente;

    public Bloque(int indice) {
        this.indice = indice;
        this.libre = true;
        this.siguiente = LIBRE;
    }

    public int getIndice() { return indice; }

    public boolean estaLibre() { return libre; }

    public void ocupar() {
        libre = false;
        if (siguiente == LIBRE) {
            siguiente = FIN_CADENA; 
        }
    }

    public void liberar() {
        libre = true;
        siguiente = LIBRE;
    }

    public int getSiguiente() { return siguiente; }

    public void setSiguiente(int siguiente) { this.siguiente = siguiente; }

}
