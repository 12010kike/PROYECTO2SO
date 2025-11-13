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
public class PlanificadorSCAN implements PlanificadorDisco {
     private int posicion;
    private int min;
    private int max;
    private boolean haciaArriba = true;

    @Override
    public void configurar(int posicionActual, int min, int max) {
        this.posicion = posicionActual;
        this.min = min;
        this.max = max;
        if (posicion >= (min + max) / 2) { haciaArriba = false; }
    }

    @Override
    public SolicitudES seleccionarSiguiente(ColaEnlazada<SolicitudES> cola) {
        if (cola.estaVacia()) return null;

        // 1) Buscar la solicitud más cercana en la dirección actual
        final SolicitudES[] candidato = {null};
        final int[] mejorDeltaAbs = {Integer.MAX_VALUE};

        cola.recorrer((s, i) -> {
            int delta = s.getPistaDestino() - posicion;
            boolean valida = haciaArriba ? (delta >= 0) : (delta <= 0);
            int adelta = Math.abs(delta);
            if (valida && adelta < mejorDeltaAbs[0]) {
                mejorDeltaAbs[0] = adelta;
                candidato[0] = s;
            }
        });

        if (candidato[0] != null) {
            cola.removerPrimeraOcurrencia(candidato[0]);
            return candidato[0];
        }

        // 2) Si no hay en la dirección actual, invertimos y tomamos el más cercano (en cualquier dirección)
        haciaArriba = !haciaArriba;

        final SolicitudES[] cand2 = {null};
        final int[] mejor2 = {Integer.MAX_VALUE};
        cola.recorrer((s, i) -> {
            int ad = Math.abs(s.getPistaDestino() - posicion);
            if (ad < mejor2[0]) { mejor2[0] = ad; cand2[0] = s; }
        });

        if (cand2[0] != null) {
            cola.removerPrimeraOcurrencia(cand2[0]);
        }
        return cand2[0];
    }
}
