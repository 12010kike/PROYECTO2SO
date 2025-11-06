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
public final class ListaEnlazada<T> {
  private NodoEnlazado<T> cabeza;
    private NodoEnlazado<T> cola;
    private int tamano;

    public void agregarAlFinal(T valor) {
        NodoEnlazado<T> n = new NodoEnlazado<>(valor);
        if (cabeza == null) {
            cabeza = cola = n;
        } else {
            cola.siguiente = n;
            cola = n;
        }
        tamano++;
    }

    public void agregarAlInicio(T valor) {
        NodoEnlazado<T> n = new NodoEnlazado<>(valor);
        if (cabeza == null) {
            cabeza = cola = n;
        } else {
            n.siguiente = cabeza;
            cabeza = n;
        }
        tamano++;
    }

    public boolean removerPrimeraOcurrencia(T valor) {
        NodoEnlazado<T> ant = null;
        NodoEnlazado<T> act = cabeza;
        while (act != null) {
            if ((act.valor == null && valor == null) || (act.valor != null && act.valor.equals(valor))) {
                if (ant == null) {
                    cabeza = act.siguiente;
                } else {
                    ant.siguiente = act.siguiente;
                }
                if (act == cola) cola = ant;
                tamano--;
                return true;
            }
            ant = act;
            act = act.siguiente;
        }
        return false;
    }

    public boolean contiene(T valor) {
        NodoEnlazado<T> act = cabeza;
        while (act != null) {
            if ((act.valor == null && valor == null) || (act.valor != null && act.valor.equals(valor))) {
                return true;
            }
            act = act.siguiente;
        }
        return false;
    }

    public int tamano() {
        return tamano;
    }

    public T obtener(int indice) {
        if (indice < 0 || indice >= tamano) throw new IndexOutOfBoundsException("Índice fuera de rango: " + indice);
        int i = 0;
        NodoEnlazado<T> act = cabeza;
        while (i < indice) {
            act = act.siguiente;
            i++;
        }
        return act.valor;
    }

    public boolean estaVacia() {
        return tamano == 0;
    }

    public void limpiar() {
        cabeza = cola = null;
        tamano = 0;
    }

    
    public interface Visitante<T> { void aceptar(T valor, int indice); }

    public void recorrer(Visitante<T> visitante) {
        int i = 0;
        NodoEnlazado<T> act = cabeza;
        while (act != null) {
            visitante.aceptar(act.valor, i++);
            act = act.siguiente;
        }
    }  
}
