/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sistemaarchivos.EntradaSalida;

import java.util.concurrent.Semaphore;
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

   
    private long solicitudesAtendidas = 0;
    private long recorridoTotal = 0; // en unidades de pista
    private long esperaAcumuladaMs = 0;

  
    public void configurarCabezal(int pos, int max) {
        if (pos < 0) pos = 0;
        this.posicionCabezal = Math.min(pos, max);
        this.maxPistas = max;
    }

    public int getPosicionCabezal() { return posicionCabezal; }
    public int getMaxPistas() { return maxPistas; }

    public void encolar(SolicitudES s) {
        s.marcarEncolado();
        synchronized (candadoCola) {
            cola.ofrecer(s);
        }
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
    public long getEsperaPromedioMs() {
        return solicitudesAtendidas == 0 ? 0 : esperaAcumuladaMs / solicitudesAtendidas;
    }

   
    public int tamanoCola() {
        synchronized (candadoCola) { return cola.tamano(); }
    }

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
            try {
                semElementos.acquire();
            } catch (InterruptedException ie) {
                if (!ejecutando) break;
            }
            if (!ejecutando) break;

            SolicitudES s;
            synchronized (candadoCola) {
                s = cola.retirar();
            }
            if (s == null) continue;

            s.marcarInicio();
            int destino = s.getPistaDestino();
            int movimiento = Math.abs(destino - posicionCabezal);
            recorridoTotal += movimiento;

            
            try {
                // 1 ms por pista movida + 5 ms de servicio base
                Thread.sleep(Math.max(0, movimiento) + 5L);
            } catch (InterruptedException ignored) {}

            posicionCabezal = destino;
            s.marcarFin();

            // métricas
            esperaAcumuladaMs += s.getEsperaMs();
            solicitudesAtendidas++;
        }
    } 
}
