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
public final class PlanificadorCScan implements PlanificadorDisco {
  private int posicion;
    private int min;
    private int max;

    @Override
    public void configurar(int posicionActual, int min, int max) {
        this.posicion = posicionActual;
        this.min = min;
        this.max = max;
    }

    @Override
    public SolicitudES seleccionarSiguiente(ColaEnlazada<SolicitudES> cola) {
        if (cola.estaVacia()) return null;

        // 1) Buscar la solicitud con pista >= posicion más cercana (menor delta hacia arriba)
        final SolicitudES[] candidato = {null};
        final int[] mejorDelta = {Integer.MAX_VALUE};
        cola.recorrer((s, i) -> {
            int p = s.getPistaDestino();
            if (p >= posicion) {
                int d = p - posicion;
                if (d < mejorDelta[0]) {
                    mejorDelta[0] = d;
                    candidato[0] = s;
                }
            }
        });
        if (candidato[0] != null) {
            cola.removerPrimeraOcurrencia(candidato[0]);
            return candidato[0];
        }

        // 2) No hay por encima: elegir la solicitud con la pista mínima (wrap al inicio)
        final SolicitudES[] minPista = {null};
        final int[] minValor = {Integer.MAX_VALUE};
        cola.recorrer((s, i) -> {
            int p = s.getPistaDestino();
            if (p < minValor[0]) {
                minValor[0] = p;
                minPista[0] = s;
            }
        });
        if (minPista[0] != null) {
            cola.removerPrimeraOcurrencia(minPista[0]);
        }
        return minPista[0];
    }
}
