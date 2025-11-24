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

 // ----------------- Cola y sincronización -----------------
    private final ColaEnlazada<SolicitudES> cola = new ColaEnlazada<>();
    private final Object candadoCola = new Object();
    private final Semaphore semElementos = new Semaphore(0, true);

    // ----------------- Planificación -----------------
    private PoliticaPlanificacion politica = PoliticaPlanificacion.FIFO;
    private PlanificadorDisco planificador = new PlanificadorFIFO();

    // ----------------- Hilo de trabajo -----------------
    private volatile boolean ejecutando = false;
    private volatile Thread hilo;

    // ----------------- Estado del disco / métricas -----------------
    private int posicionCabezal = 0;
    private int maxPistas = constantes.PISTAS_DISCO - 1; // índice máximo de pista

    private long recorridoTotal = 0;
    private long solicitudesAtendidas = 0;
    private long esperaAcumuladaMs = 0;

    // ============================================================== //
    // API pública consultada por la GUI
    // ============================================================== //

    /** Encola una solicitud y despierta al hilo si estaba esperando.
     * @param s */
    public void encolar(SolicitudES s) {
        if (s == null) return;
        synchronized (candadoCola) {
            cola.ofrecer(s);
        }
        semElementos.release();
    }

    /** Tamaño actual de la cola.
     * @return  */
    public int tamanoCola() {
        synchronized (candadoCola) { return cola.tamano(); }
    }

    /** Vuelca un snapshot de la cola a una tabla para la GUI (PID, Tipo, Pista, Espera).
     * @param tabla */
    public void volcarTabla(Object[][] tabla) {
        synchronized (candadoCola) {
            final int[] fila = {0};
            cola.recorrer((s, i) -> {
                if (tabla != null && fila[0] < tabla.length) {
                    tabla[fila[0]][0] = s.getIdProceso();
                    tabla[fila[0]][1] = s.getTipo().name();
                    tabla[fila[0]][2] = s.getPistaDestino();
                    tabla[fila[0]][3] = s.getEsperaMs(); // la solicitud lleva su propio tiempo de espera
                }
                fila[0]++;
            });
        }
    }

    // --------- Métricas / estado ---------
    public long getRecorridoTotal()       { return recorridoTotal; }
    public long getSolicitudesAtendidas() { return solicitudesAtendidas; }
    public long getEsperaPromedioMs()     { return (solicitudesAtendidas == 0) ? 0 : (esperaAcumuladaMs / solicitudesAtendidas); }
    public int  getPosicionCabezal()      { return posicionCabezal; }
    public PoliticaPlanificacion getPolitica() { return politica; }
    /** Reinicia métricas y (opcional) limpia la cola.
     * @param limpiarCola */
    public void resetEstadisticas(boolean limpiarCola) {
    synchronized (candadoCola) {
        if (limpiarCola) {
            while (!cola.estaVacia()) { cola.retirar(); }
        }
    }
    recorridoTotal = 0L;
    solicitudesAtendidas = 0L;
    esperaAcumuladaMs = 0L;
}


    /** ¿El hilo está corriendo? Úsalo para la UI.
     * @return  */
    public boolean estaEjecutando() {
        Thread t = this.hilo;
        return ejecutando && t != null && t.isAlive();
    }

    /** Cambia la política y deja el planificador listo con el estado actual.
     * @param p */
    public void setPolitica(PoliticaPlanificacion p) {
        if (p == null) return;
        this.politica = p;
        switch (p) {
            case FIFO   -> this.planificador = new PlanificadorFIFO();
            case SSTF   -> this.planificador = new PlanificadorSSTF();
            case SCAN   -> this.planificador = new PlanificadorSCAN();
            case C_SCAN -> this.planificador = new PlanificadorCScan();
        }
        if (this.planificador != null) {
            this.planificador.configurar(posicionCabezal, 0, maxPistas);
        }
    }

    /** Posición inicial del cabezal y cota superior de pistas.
     * @param pos
     * @param pistaMax */
    public void configurarCabezal(int pos, int pistaMax) {
        this.maxPistas = Math.max(0, pistaMax);
        this.posicionCabezal = Math.max(0, Math.min(pos, this.maxPistas));
        if (this.planificador != null) {
            this.planificador.configurar(posicionCabezal, 0, maxPistas);
        }
    }

    /** Inicia el hilo de atención. */
    public void iniciar() {
        if (estaEjecutando()) return;
        ejecutando = true;
        hilo = new Thread(this::bucleAtencion, "Hilo-ES");
        hilo.setDaemon(true);
        hilo.start();
    }

    /**
     * Detiene el hilo de atención de forma segura.
     * No borra la cola ni las métricas (eso queda para el botón de "Reiniciar" si lo tienes).
     * Es compatible con persistencia (Paso 5).
     */
    public void detener() {
        ejecutando = false;
        // Desbloquear acquire() y, si estuviera durmiendo, interrumpir el sleep
        semElementos.release();
        Thread t = hilo;
        if (t != null) {
            t.interrupt();
            try { t.join(300); } catch (InterruptedException ignored) {}
        }
        hilo = null; // deja el estado limpio para un próximo iniciar()
    }

    /** Limpia SOLO métricas (opcional para un botón de "Reiniciar métricas"). */
    public void resetearMetricas() {
        recorridoTotal = 0;
        solicitudesAtendidas = 0;
        esperaAcumuladaMs = 0;
    }

    // ============================================================== //
    // Bucle del hilo: selección por planificador + simulación de movimiento
    // ============================================================== //
    private void bucleAtencion() {
        while (ejecutando) {
            try {
                semElementos.acquire();                 // espera a que haya al menos 1 solicitud
            } catch (InterruptedException ie) {
                if (!ejecutando) break;
            }
            if (!ejecutando) break;

            SolicitudES s;
            synchronized (candadoCola) {
                if (cola.estaVacia()) {
                    // Nada para atender: volver a esperar
                    continue;
                }
                // Configurar con la posición actual ANTES de seleccionar
                planificador.configurar(posicionCabezal, 0, maxPistas);
                s = planificador.seleccionarSiguiente(cola); // el planificador REMUEVE la elegida
            }
            if (s == null) continue;

            s.marcarInicio();

            int destino = s.getPistaDestino();
            int movimiento = Math.abs(destino - posicionCabezal);
            recorridoTotal += movimiento;

            // Simula tiempo de búsqueda: >=10 ms y proporcional al movimiento
            try { Thread.sleep(Math.max(10, movimiento)); }
            catch (InterruptedException ie) { if (!ejecutando) break; }

            posicionCabezal = destino;

            s.marcarFin();
            esperaAcumuladaMs += s.getEsperaMs();
            solicitudesAtendidas++;
        }
    }
}