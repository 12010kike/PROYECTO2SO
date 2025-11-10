/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sistemaarchivos.sistema;

import sistemaarchivos.modelo.Permisos;
import sistemaarchivos.modelo.TipoNodo;

/**
 *
 * @author eabdf
 */
public final class Archivo extends NodoSistema{
  private int tamanioEnBloques;
    private int primerBloque; // -1 si vacío

    public Archivo(String nombre, String propietario, Permisos permisos, int tamanioEnBloques, int primerBloque) {
        super(nombre, propietario, permisos, TipoNodo.ARCHIVO);
        this.tamanioEnBloques = tamanioEnBloques;
        this.primerBloque = primerBloque;
    }

    public int getTamanioEnBloques() { return tamanioEnBloques; }
    public int getPrimerBloque() { return primerBloque; }

    public void setTamanioEnBloques(int t) { this.tamanioEnBloques = t; }
    public void setPrimerBloque(int pb) { this.primerBloque = pb; }  
}
