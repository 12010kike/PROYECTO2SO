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
public class ColaEnlazada<T> {
  private final ListaEnlazada<T> lista = new ListaEnlazada<>();

    public void ofrecer(T valor) {
        lista.agregarAlFinal(valor);
    }

    public T retirar() {
        if (lista.estaVacia()) return null;
        T v = lista.obtener(0);
        lista.removerPrimeraOcurrencia(v);
        return v;
    }

    public boolean estaVacia() {
        return lista.estaVacia();
    }

    public int tamano() {
        return lista.tamano();
    }

    public boolean contiene(T v) {
        return lista.contiene(v);
    }  
}
