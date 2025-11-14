/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sistemaarchivos.Planificacion;

import sistemaarchivos.EntradaSalida.SolicitudES;
import sistemaarchivos.estructuras.ColaEnlazada;

/**
 *
 * @author eabdf
 */
public final class PlanificadorSSTF implements PlanificadorDisco {

    private int posicion;
    private int min;
    private int max;

    @Override
    public void configurar(int posicionActual, int pistaMin, int pistaMax) {
        this.posicion = posicionActual;
        this.min = pistaMin;
        this.max = pistaMax;
    }

    @Override
    public SolicitudES seleccionarSiguiente(ColaEnlazada<SolicitudES> cola) {
        if (cola.estaVacia()) return null;

        final SolicitudES[] mejor = {null};
        final int[] mejorDelta = {Integer.MAX_VALUE};

        cola.recorrer((s, i) -> {
            int d = Math.abs(s.getPistaDestino() - posicion);
            if (d < mejorDelta[0]) { mejorDelta[0] = d; mejor[0] = s; }
        });

        if (mejor[0] != null) {
            cola.removerPrimeraOcurrencia(mejor[0]); // ← se REMUEVE de la cola aquí
        }
        return mejor[0];
    }
}
