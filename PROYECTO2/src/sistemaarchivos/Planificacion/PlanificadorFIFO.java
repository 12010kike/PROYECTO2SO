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
public final class PlanificadorFIFO implements PlanificadorDisco {
     @Override
     public void configurar(int pos, int min, int max) {}

    /**
     *
     * @param cola
     * @return
     */
    @Override
    public SolicitudES seleccionarSiguiente(ColaEnlazada<SolicitudES> cola) {
        return cola.retirar();
    }
}
