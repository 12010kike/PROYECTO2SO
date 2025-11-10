/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sistemaarchivos.sistema;

import sistemaarchivos.estructuras.ListaEnlazada;
import sistemaarchivos.modelo.Permisos;
import sistemaarchivos.modelo.TipoNodo;

/**
 *
 * @author eabdf
 */
public final class Directorio extends NodoSistema {
    private final ListaEnlazada<NodoSistema> hijos = new ListaEnlazada<>();

    public Directorio(String nombre, String propietario, Permisos permisos) {
        super(nombre, propietario, permisos, TipoNodo.DIRECTORIO);
    }

    public void agregarHijo(NodoSistema nodo) {
        hijos.agregarAlFinal(nodo);
    }

    public boolean eliminarHijoPorNombre(String nombre) {
        final NodoSistema[] encontrado = {null};
        hijos.recorrer((n, i) -> {
            if (encontrado[0] == null && n.getNombre().equals(nombre)) {
                encontrado[0] = n;
            }
        });
        if (encontrado[0] != null) {
            return hijos.removerPrimeraOcurrencia(encontrado[0]);
        }
        return false;
    }

    public NodoSistema obtenerHijoPorNombre(String nombre) {
        final NodoSistema[] encontrado = {null};
        hijos.recorrer((n, i) -> {
            if (encontrado[0] == null && n.getNombre().equals(nombre)) {
                encontrado[0] = n;
            }
        });
        return encontrado[0];
    }

    public ListaEnlazada<NodoSistema> getHijos() {
        return hijos;
    }
}
