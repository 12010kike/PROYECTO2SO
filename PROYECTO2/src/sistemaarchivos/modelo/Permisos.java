/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sistemaarchivos.modelo;

/**
 *
 * @author eabdf
 */
public final class Permisos {
    public boolean propietarioLectura = true;
    public boolean propietarioEscritura = true;
    public boolean propietarioEjecucion = false;

    public boolean otrosLectura = true;
    public boolean otrosEscritura = false;
    public boolean otrosEjecucion = false;

    public static Permisos porDefectoDirectorio() {
        Permisos p = new Permisos();
        p.propietarioEjecucion = true;
        p.otrosEjecucion = true;
        return p;
    }

    public static Permisos porDefectoArchivo() {
        return new Permisos();
    }
}
