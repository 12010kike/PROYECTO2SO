/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sistemaarchivos.EntradaSalida;

/**
 *
 * @author eabdf
 */
public final class SolicitudES {
    private final String idProceso;
    private final TipoOperacionesES tipo;
    private final int pistaDestino;

    private final long creadoNanos;
    private long encoladoNanos;
    private long inicioAtencionNanos;
    private long finAtencionNanos;

    public SolicitudES(String idProceso, TipoOperacionesES tipo, int pistaDestino) {
        this.idProceso = idProceso;
        this.tipo = tipo;
        this.pistaDestino = pistaDestino;
        this.creadoNanos = System.nanoTime();
    }

    public String getIdProceso() { return idProceso; }
    public TipoOperacionesES getTipo() { return tipo; }
    public int getPistaDestino() { return pistaDestino; }

   
    public void marcarEncolado() { this.encoladoNanos = System.nanoTime(); }
    public void marcarInicio() { this.inicioAtencionNanos = System.nanoTime(); }
    public void marcarFin() { this.finAtencionNanos = System.nanoTime(); }

    public long getEsperaMs() {
        long base = (inicioAtencionNanos == 0 ? System.nanoTime() : inicioAtencionNanos);
        return (base - (encoladoNanos == 0 ? creadoNanos : encoladoNanos)) / 1_000_000L;
    }

    public long getServicioMs() {
        if (finAtencionNanos == 0 || inicioAtencionNanos == 0) return 0;
        return (finAtencionNanos - inicioAtencionNanos) / 1_000_000L;
    }
}
