/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sistemaarchivos.EntradaSalida;

import java.util.concurrent.Semaphore;
import sistemaarchivos.Planificacion.PlanificadorCScan;
import sistemaarchivos.Planificacion.PlanificadorDisco;
import sistemaarchivos.Planificacion.PlanificadorFIFO;
import sistemaarchivos.Planificacion.PlanificadorSCAN;
import sistemaarchivos.Planificacion.PlanificadorSSTF;
import sistemaarchivos.Planificacion.PoliticaPlanificacion;
import sistemaarchivos.constantes.constantes;
import sistemaarchivos.estructuras.ColaEnlazada;

/**
 *
 * @author eabdf
 */
public final class GestorES {
  private final ColaEnlazada<SolicitudES> cola = new ColaEnlazada<>();
    private final Semaphore semElementos = new Semaphore(0, true);
    private final Object candadoCola = new Object();

    private Thread hilo;
    private volatile boolean ejecutando = false;

    private int posicionCabezal = 0;
    private int maxPistas = constantes.PISTAS_DISCO - 1;

    // métricas
    private long solicitudesAtendidas = 0;
    private long recorridoTotal = 0;
    private long esperaAcumuladaMs = 0;

    private PoliticaPlanificacion politica = PoliticaPlanificacion.FIFO;
    private PlanificadorDisco planificador = new PlanificadorFIFO();

   
    public void configurarCabezal(int pos, int max) {
        if (pos < 0) pos = 0;
        this.posicionCabezal = Math.min(pos, max);
        this.maxPistas = max;
    }

    public void setPolitica(PoliticaPlanificacion p) {
        this.politica = p;
        switch (p) {
            case FIFO -> planificador = new PlanificadorFIFO();
            case SSTF -> planificador = new PlanificadorSSTF();
            case SCAN -> planificador = new PlanificadorSCAN();
            case C_SCAN -> planificador = new PlanificadorCScan();
        }
    }

   
    public void encolar(SolicitudES s) {
        s.marcarEncolado();
        synchronized (candadoCola) { cola.ofrecer(s); }
        semElementos.release();
    }

  
    public void iniciar() {
        if (ejecutando) return;
        ejecutando = true;
        hilo = new Thread(this::bucleAtencion, "Hilo-ES");
        hilo.setDaemon(true);
        hilo.start();
    }

    public void detener() {
        ejecutando = false;
        if (hilo != null) hilo.interrupt();
    }

  
    public long getSolicitudesAtendidas() { return solicitudesAtendidas; }
    public long getRecorridoTotal() { return recorridoTotal; }
    public long getEsperaPromedioMs() { return solicitudesAtendidas == 0 ? 0 : esperaAcumuladaMs / solicitudesAtendidas; }
    public int getPosicionCabezal() { return posicionCabezal; }
    public int getMaxPistas() { return maxPistas; }

  
    public int tamanoCola() { synchronized (candadoCola) { return cola.tamano(); } }
    public void volcarTabla(Object[][] destino) {
        synchronized (candadoCola) {
            final int[] k = {0};
            cola.recorrer((s, i) -> {
                if (k[0] < destino.length) {
                    destino[k[0]][0] = s.getIdProceso();
                    destino[k[0]][1] = s.getTipo().name();
                    destino[k[0]][2] = s.getPistaDestino();
                    destino[k[0]][3] = s.getEsperaMs();
                    k[0]++;
                }
            });
        }
    }

  
    private void bucleAtencion() {
        while (ejecutando) {
            try { semElementos.acquire(); } catch (InterruptedException ie) { if (!ejecutando) break; }
            if (!ejecutando) break;

            SolicitudES s;
            synchronized (candadoCola) {
                planificador.configurar(posicionCabezal, 0, maxPistas);
                s = planificador.seleccionarSiguiente(cola);
            }
            if (s == null) continue;

            s.marcarInicio();
            int destino = s.getPistaDestino();
            int movimiento = Math.abs(destino - posicionCabezal);
            recorridoTotal += movimiento;

            try { Thread.sleep(Math.max(0, movimiento) + 5L); } catch (InterruptedException ignored) {}
            posicionCabezal = destino;
            s.marcarFin();

            esperaAcumuladaMs += s.getEsperaMs();
            solicitudesAtendidas++;
        }
    }
}
