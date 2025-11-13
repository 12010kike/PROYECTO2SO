/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package sistemaarchivos.Planificacion;

import sistemaarchivos.EntradaSalida.SolicitudES;
import sistemaarchivos.estructuras.ColaEnlazada;

/**
 *
 * @author eabdf
 */
public interface PlanificadorDisco {
 void configurar(int posicionActual, int pistaMin, int pistaMax);
    SolicitudES seleccionarSiguiente(ColaEnlazada<SolicitudES> cola);   
}
