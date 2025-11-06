/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sistemaarchivos.estructuras;

/**
 *
 * @author eabdf
 * @param <T>
 */
public final class NodoEnlazado<T> {
    T valor;
    NodoEnlazado<T> siguiente;

    public NodoEnlazado(T valor) {
        this.valor = valor;
    }
}
