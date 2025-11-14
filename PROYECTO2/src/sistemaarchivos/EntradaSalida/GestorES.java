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
    private Thread hilo;

    // ----------------- Estado del disco / métricas -----------------
    private int posicionCabezal = 0;
    private int maxPistas = constantes.PISTAS_DISCO - 1; // índice máximo de pista

    private long recorridoTotal = 0;
    private long solicitudesAtendidas = 0;
    private long esperaAcumuladaMs = 0;

    // ==============================================================
    // API pública consultada por la GUI
    // ==============================================================

    /** Encola una solicitud y despierta al hilo si estaba esperando. */
    public void encolar(SolicitudES s) {
        if (s == null) return;
        synchronized (candadoCola) {
            cola.ofrecer(s);
        }
        semElementos.release();
    }

    /** Tamaño actual de la cola. */
    public int tamanoCola() {
        synchronized (candadoCola) { return cola.tamano(); }
    }

    /** Vuelca un snapshot de la cola a una tabla para la GUI (PID, Tipo, Pista, Espera). */
    public void volcarTabla(Object[][] tabla) {
        synchronized (candadoCola) {
            final int[] fila = {0};
            cola.recorrer((s, i) -> {
                if (tabla != null && fila[0] < tabla.length) {
                    tabla[fila[0]][0] = s.getIdProceso();
                    tabla[fila[0]][1] = s.getTipo().name();
                    tabla[fila[0]][2] = s.getPistaDestino();
                    tabla[fila[0]][3] = s.getEsperaMs(); // la solicitud calcula su espera
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

    /** Cambia la política y deja el planificador listo con el estado actual. */
    public void setPolitica(PoliticaPlanificacion p) {
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

    /** Posición inicial del cabezal y cota superior de pistas. */
    public void configurarCabezal(int pos, int pistaMax) {
        this.maxPistas = Math.max(0, pistaMax);
        this.posicionCabezal = Math.max(0, Math.min(pos, this.maxPistas));
        if (this.planificador != null) {
            this.planificador.configurar(posicionCabezal, 0, maxPistas);
        }
    }

    /** Inicia el hilo de atención. */
    public void iniciar() {
        if (hilo != null && hilo.isAlive()) return;
        ejecutando = true;
        hilo = new Thread(this::bucleAtencion, "Hilo-ES");
        hilo.setDaemon(true);
        hilo.start();
    }

    /** Detiene el hilo de atención de forma segura. */
    public void detener() {
        ejecutando = false;
        // libera al hilo si está en acquire()
        semElementos.release();
        try { if (hilo != null) hilo.join(200); } catch (InterruptedException ignored) {}
    }

    // ==============================================================
    // BUCLE DEL HILO: selección por planificador + simulación de movimiento
    // ==============================================================
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
                if (cola.estaVacia()) continue;         // carrera rara: nada que hacer
                // ¡Clave! Configurar con la posición actual ANTES de seleccionar
                planificador.configurar(posicionCabezal, 0, maxPistas);
                s = planificador.seleccionarSiguiente(cola); // el planificador REMUEVE la elegida
            }
            if (s == null) continue;

            s.marcarInicio();

            int destino = s.getPistaDestino();
            int movimiento = Math.abs(destino - posicionCabezal);
            recorridoTotal += movimiento;

            // Simula tiempo de búsqueda: >=10ms y proporcional al movimiento
            try { Thread.sleep(Math.max(10, movimiento)); } catch (InterruptedException ignored) {}

            posicionCabezal = destino;

            s.marcarFin();
            esperaAcumuladaMs += s.getEsperaMs();
            solicitudesAtendidas++;
        }
    }
}

