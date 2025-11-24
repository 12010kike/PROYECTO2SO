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
public abstract class NodoSistema {
    protected String nombre;
    protected String propietario;
    protected Permisos permisos;
    protected final TipoNodo tipo;

    protected NodoSistema(String nombre, String propietario, Permisos permisos, TipoNodo tipo) {
        this.nombre = nombre;
        this.propietario = propietario;
        this.permisos = permisos;
        this.tipo = tipo;
    }

    public String getNombre() { return nombre; }
    public String getPropietario() { return propietario; }
    public Permisos getPermisos() { return permisos; }
    public TipoNodo getTipo() { return tipo; }

    public void renombrar(String nuevoNombre) {
        this.nombre = nuevoNombre;
    }
     public void setNombre(String nuevo) {
        if (nuevo == null) return;
        String limpio = nuevo.trim();
        if (!limpio.isEmpty()) this.nombre = limpio;
    }
}

