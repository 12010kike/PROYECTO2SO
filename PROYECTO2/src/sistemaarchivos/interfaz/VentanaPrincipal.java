/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sistemaarchivos.interfaz;
import sistemaarchivos.constantes.constantes;
import sistemaarchivos.modelo.ModoUsuario;
import sistemaarchivos.Planificacion.PoliticaPlanificacion;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import sistemaarchivos.EntradaSalida.GestorES;
import sistemaarchivos.EntradaSalida.SolicitudES;
import sistemaarchivos.EntradaSalida.TipoOperacionesES;
import sistemaarchivos.disco.Bloque;
import sistemaarchivos.disco.Disco;
import sistemaarchivos.modelo.NodoArbolVista;
import sistemaarchivos.persistencia.PersistenciaJSON;
import sistemaarchivos.sistema.Archivo;
import sistemaarchivos.sistema.Directorio;
import sistemaarchivos.sistema.GestorSistemaArchivos;
import sistemaarchivos.sistema.NodoSistema;

/**
 *
 * @author eabdf
 */
public class VentanaPrincipal extends JFrame {
 // Árbol y tablas
    private final JTree arbolSistema;
    private final ModeloTablaAsignacion modeloAsignacion;
    private final JTable tablaAsignacion;
    private final ModeloTablaColaES modeloColaES;
    private final JTable tablaColaES;

    // Controles de modo y política
    private final JRadioButton radioAdmin;
    private final JRadioButton radioUsuario;
    private final JComboBox<PoliticaPlanificacion> comboPolitica;

    // Disco y FS
    private final PanelDisco panelDisco;
    private Disco disco;
    private final GestorSistemaArchivos gestorFS;

    // E/S y métricas
    private final GestorES gestorES = new GestorES();
    private final JLabel etiquetaMetricas =
            new JLabel("E/S: detenida | Recorrido: 0 | Espera prom.: 0 ms | Atendidas: 0");
    private final javax.swing.JTextField campoCabezal = new javax.swing.JTextField("0", 4);

    // Botones inferiores
    private final JButton btnVerificar = new JButton("Verificar asignación");
    private final JButton btnExportCSV = new JButton("Exportar CSV E/S");

    // Guardamos referencias de Iniciar/Detener para toggle
    private JButton btnIniciarESRef;
    private JButton btnDetenerESRef;

    public VentanaPrincipal() {
        super(constantes.TITULO_APLICACION);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(constantes.ANCHO_VENTANA, constantes.ALTO_VENTANA);
        setLocationRelativeTo(null);
        getContentPane().setLayout(new BorderLayout());

        // Modelo base
        this.disco = Disco.crearPorDefecto();
        this.gestorFS = new GestorSistemaArchivos(disco);

        // ================== BARRA SUPERIOR ==================
        JToolBar barra = new JToolBar();
        barra.setFloatable(false);

        JButton btnCrearDir   = new JButton("Crear directorio");
        JButton btnCrearArch  = new JButton("Crear archivo");
        JButton btnEliminar   = new JButton("Eliminar");
        JButton btnGuardar    = new JButton("Guardar JSON");
        JButton btnCargar     = new JButton("Cargar JSON");

        barra.add(btnCrearDir);
        barra.add(btnCrearArch);
        barra.add(btnEliminar);
        barra.addSeparator();
        barra.add(btnGuardar);
        barra.add(btnCargar);

        barra.addSeparator();
        radioAdmin  = new JRadioButton(constantes.TEXTO_MODO_ADMIN, true);
        radioUsuario= new JRadioButton(constantes.TEXTO_MODO_USUARIO, false);
        ButtonGroup grupoModo = new ButtonGroup();
        grupoModo.add(radioAdmin);
        grupoModo.add(radioUsuario);
        barra.add(radioAdmin);
        barra.add(radioUsuario);

        barra.addSeparator();
        barra.add(new JLabel(" Planificación: "));
        comboPolitica = new JComboBox<>(PoliticaPlanificacion.values());
        barra.add(comboPolitica);

        // ---- Controles E/S ----
        barra.addSeparator();
        JButton btnAgregarES = new JButton("Agregar E/S");
        btnIniciarESRef = new JButton("Iniciar E/S");
        btnDetenerESRef = new JButton("Detener E/S");
        btnDetenerESRef.setEnabled(false); // aún no iniciado

        barra.add(btnAgregarES);
        barra.add(btnIniciarESRef);
        barra.add(btnDetenerESRef);
        barra.add(new JLabel(" Cabezal: "));
        barra.add(campoCabezal);

        getContentPane().add(barra, BorderLayout.NORTH);

        // ================== ZONA CENTRAL ==================
        arbolSistema = new JTree();
        JScrollPane scrollArbol = new JScrollPane(arbolSistema);
        scrollArbol.setPreferredSize(new Dimension(300, 400));

        JTabbedPane pestañas = new JTabbedPane();

        panelDisco = new PanelDisco(disco);
        JScrollPane scrollDisco = new JScrollPane(panelDisco);
        pestañas.addTab("Disco", scrollDisco);

        modeloAsignacion = new ModeloTablaAsignacion();
        tablaAsignacion = new JTable(modeloAsignacion);
        pestañas.addTab("Tabla de asignación", new JScrollPane(tablaAsignacion));

        modeloColaES = new ModeloTablaColaES();
        tablaColaES = new JTable(modeloColaES);
        pestañas.addTab("Cola de E/S", new JScrollPane(tablaColaES));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollArbol, pestañas);
        split.setDividerLocation(320);
        getContentPane().add(split, BorderLayout.CENTER);

        // ================== BARRA INFERIOR ==================
        JToolBar barraInferior = new JToolBar();
        barraInferior.setFloatable(false);
        barraInferior.setLayout(new BoxLayout(barraInferior, BoxLayout.X_AXIS));
        barraInferior.add(etiquetaMetricas);
        barraInferior.add(Box.createHorizontalGlue());
        barraInferior.add(btnVerificar);
        barraInferior.add(Box.createHorizontalStrut(8));
        barraInferior.add(btnExportCSV);
        getContentPane().add(barraInferior, BorderLayout.SOUTH);

        // ---- Poblado inicial ----
        refrescarVista();

        // ================== ACCIONES ==================
        // FS
        btnCrearDir.addActionListener(e -> accionCrearDirectorio());
        btnCrearArch.addActionListener(e -> accionCrearArchivo());
        btnEliminar.addActionListener(e -> accionEliminar());

        // Persistencia (Paso 5)
        btnGuardar.addActionListener(e -> accionGuardarJSON());
        btnCargar.addActionListener(e -> accionCargarJSON());

        // E/S
        btnAgregarES.addActionListener(e -> accionAgregarES());
        btnIniciarESRef.addActionListener(e -> accionIniciarES());
        btnDetenerESRef.addActionListener(e -> accionDetenerES());

        // Política
        comboPolitica.addActionListener(e -> {
            PoliticaPlanificacion p = (PoliticaPlanificacion) comboPolitica.getSelectedItem();
            gestorES.setPolitica(p);
        });

        // Botones nuevos
        btnVerificar.addActionListener(this::accionVerificarAsignacion);
        btnExportCSV.addActionListener(this::accionExportarCSV);

        // Timer UI (ahora usa el estado real del hilo)
        new Timer(400, e -> {
            modeloColaES.actualizarDesdeGestor(gestorES);
            String estado = gestorES.estaEjecutando() ? "activa" : "detenida";
            etiquetaMetricas.setText(
                "E/S: " + estado
                + " | Recorrido: " + gestorES.getRecorridoTotal()
                + " | Espera prom.: " + gestorES.getEsperaPromedioMs() + " ms"
                + " | Atendidas: " + gestorES.getSolicitudesAtendidas()
            );
        }).start();
    }

    // -------------------- FS --------------------
    private void refrescarVista() {
        DefaultMutableTreeNode raizUI = construirNodoUI(gestorFS.getRaiz());
        arbolSistema.setModel(new DefaultTreeModel(raizUI));
        panelDisco.repaint();
        modeloAsignacion.actualizarDesdeRaiz(gestorFS.getRaiz());
    }

    private DefaultMutableTreeNode construirNodoUI(NodoSistema n) {
        DefaultMutableTreeNode ui = new DefaultMutableTreeNode(n.getNombre());
        if (n instanceof Directorio d) {
            d.getHijos().recorrer((h, i) -> ui.add(construirNodoUI(h)));
        }
        return ui;
    }

    private String rutaSeleccionada() {
        javax.swing.tree.TreePath path = arbolSistema.getSelectionPath();
        if (path == null) return "raiz";
        Object[] comps = path.getPath();
        StringBuilder ruta = new StringBuilder();
        for (int i = 0; i < comps.length; i++) {
            ruta.append(comps[i]);
            if (i < comps.length - 1) ruta.append("/");
        }
        return ruta.toString();
    }

    private void accionCrearDirectorio() {
        if (obtenerModoActual() != ModoUsuario.ADMINISTRADOR) {
            JOptionPane.showMessageDialog(this, "Solo el Administrador puede crear directorios.");
            return;
        }
        String nombre = JOptionPane.showInputDialog(this, "Nombre del directorio:");
        if (nombre == null || nombre.isBlank()) return;
        boolean ok = gestorFS.crearDirectorio(rutaSeleccionada(), nombre.trim(), "admin");
        if (!ok) JOptionPane.showMessageDialog(this, "No se pudo crear el directorio.");
        refrescarVista();
    }

    private void accionCrearArchivo() {
        if (obtenerModoActual() != ModoUsuario.ADMINISTRADOR) {
            JOptionPane.showMessageDialog(this, "Solo el Administrador puede crear archivos.");
            return;
        }
        javax.swing.JTextField campoNombre  = new javax.swing.JTextField();
        javax.swing.JTextField campoBloques = new javax.swing.JTextField();
        Object[] msg = { "Nombre del archivo:", campoNombre, "Tamaño (en bloques):", campoBloques };
        int r = JOptionPane.showConfirmDialog(this, msg, "Crear archivo", JOptionPane.OK_CANCEL_OPTION);
        if (r != JOptionPane.OK_OPTION) return;

        String nombre = campoNombre.getText();
        int tam;
        try { tam = Integer.parseInt(campoBloques.getText()); }
        catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Ingrese un tamaño válido (>0)."); return; }

        boolean ok = gestorFS.crearArchivo(rutaSeleccionada(), nombre.trim(), "admin", tam);
        if (!ok) JOptionPane.showMessageDialog(this, "No se pudo crear el archivo.");
        refrescarVista();
    }

    private void accionEliminar() {
        if (obtenerModoActual() != ModoUsuario.ADMINISTRADOR) {
            JOptionPane.showMessageDialog(this, "Solo el Administrador puede eliminar.");
            return;
        }
        javax.swing.tree.TreePath path = arbolSistema.getSelectionPath();
        if (path == null || path.getPathCount() <= 1) {
            JOptionPane.showMessageDialog(this, "Seleccione un archivo o directorio (no la raíz).");
            return;
        }
        String nombre = path.getLastPathComponent().toString();
        String rutaPadre = construirRutaPadre(path.getPath());
        boolean ok = gestorFS.eliminarArchivo(rutaPadre, nombre);
        if (!ok) ok = gestorFS.eliminarDirectorioRecursivo(rutaPadre, nombre);
        if (!ok) JOptionPane.showMessageDialog(this, "No se pudo eliminar.");
        refrescarVista();
    }

    private String construirRutaPadre(Object[] comps) {
        StringBuilder ruta = new StringBuilder();
        for (int i = 0; i < comps.length - 1; i++) {
            ruta.append(comps[i]);
            if (i < comps.length - 2) ruta.append("/");
        }
        return ruta.isEmpty() ? "raiz" : ruta.toString();
    }

    // -------------------- E/S --------------------
    private void accionAgregarES() {
        javax.swing.JTextField campoPid  = new javax.swing.JTextField();
        JComboBox<TipoOperacionesES> comboTipo = new JComboBox<>(TipoOperacionesES.values());
        javax.swing.JTextField campoPista= new javax.swing.JTextField();
        Object[] msg = {
                "PID:", campoPid,
                "Tipo:", comboTipo,
                "Pista destino (0-" + (constantes.PISTAS_DISCO - 1) + "):", campoPista
        };
        int r = JOptionPane.showConfirmDialog(this, msg, "Agregar solicitud E/S", JOptionPane.OK_CANCEL_OPTION);
        if (r != JOptionPane.OK_OPTION) return;

        int pista;
        try { pista = Integer.parseInt(campoPista.getText()); }
        catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Pista inválida."); return; }
        if (pista < 0 || pista >= constantes.PISTAS_DISCO) {
            JOptionPane.showMessageDialog(this, "Pista fuera de rango."); return;
        }

        SolicitudES s = new SolicitudES(campoPid.getText().trim(),
                (TipoOperacionesES) comboTipo.getSelectedItem(), pista);
        gestorES.encolar(s);
        modeloColaES.actualizarDesdeGestor(gestorES);
    }

    private void accionIniciarES() {
        int pos;
        try { pos = Integer.parseInt(campoCabezal.getText().trim()); }
        catch (NumberFormatException ex) { pos = 0; }
        gestorES.configurarCabezal(pos, constantes.PISTAS_DISCO - 1);
        gestorES.setPolitica((PoliticaPlanificacion) comboPolitica.getSelectedItem());
        gestorES.iniciar();

        btnIniciarESRef.setEnabled(false);
        btnDetenerESRef.setEnabled(true);
    }

    private void accionDetenerES() {
        gestorES.detener();

        // Refrescar estado visible de inmediato
        String estado = gestorES.estaEjecutando() ? "activa" : "detenida";
        etiquetaMetricas.setText(
            "E/S: " + estado
            + " | Recorrido: " + gestorES.getRecorridoTotal()
            + " | Espera prom.: " + gestorES.getEsperaPromedioMs() + " ms"
            + " | Atendidas: " + gestorES.getSolicitudesAtendidas()
        );

        btnIniciarESRef.setEnabled(true);
        btnDetenerESRef.setEnabled(false);
    }

    // -------------------- Utilidades --------------------
    public ModoUsuario obtenerModoActual() {
        return radioAdmin.isSelected() ? ModoUsuario.ADMINISTRADOR : ModoUsuario.USUARIO;
    }

    public PoliticaPlanificacion obtenerPoliticaSeleccionada() {
        return (PoliticaPlanificacion) comboPolitica.getSelectedItem();
    }

    // ================== JSON (PASO 5) ==================
    private void accionGuardarJSON() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar estado en JSON");
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                int n = gestorES.tamanoCola();
                Object[][] tabla = new Object[n][4];
                gestorES.volcarTabla(tabla);
                SolicitudES[] arr = new SolicitudES[n];
                for (int i = 0; i < n; i++) {
                    String pid = String.valueOf(tabla[i][0]);
                    TipoOperacionesES tipo = TipoOperacionesES.valueOf(String.valueOf(tabla[i][1]));
                    int pista = Integer.parseInt(String.valueOf(tabla[i][2]));
                    arr[i] = new SolicitudES(pid, tipo, pista);
                }
                String json = PersistenciaJSON.exportarEstado(
                        disco,
                        gestorFS.getRaiz(),
                        arr,
                        obtenerPoliticaSeleccionada().name(),
                        gestorES.getPosicionCabezal()
                );
                PersistenciaJSON.guardarEnArchivo(fc.getSelectedFile().toPath(), json);
                JOptionPane.showMessageDialog(this, "Estado guardado.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al guardar: " + ex.getMessage());
            }
        }
    }

    private void accionCargarJSON() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Cargar estado desde JSON");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                var res = PersistenciaJSON.cargarDesdeArchivo(fc.getSelectedFile().toPath());

                this.disco = res.disco;
                this.panelDisco.setDisco(this.disco);

                this.gestorFS.reemplazarRaiz(res.raiz);

                for (int i = 0; i < res.cola.length; i++) {
                    this.gestorES.encolar(res.cola[i]);
                }

                this.comboPolitica.setSelectedItem(PoliticaPlanificacion.valueOf(res.politica));
                this.gestorES.setPolitica(PoliticaPlanificacion.valueOf(res.politica));
                this.campoCabezal.setText(String.valueOf(res.posCabezal));
                this.gestorES.configurarCabezal(res.posCabezal, constantes.PISTAS_DISCO - 1);

                refrescarVista();
                this.modeloColaES.actualizarDesdeGestor(gestorES);
                JOptionPane.showMessageDialog(this, "Estado cargado desde: " + fc.getSelectedFile().getName());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar: " + ex.getMessage());
            }
        }
    }

    // ================== Botones INFERIORES ==================
    private void accionVerificarAsignacion(ActionEvent e) {
        StringBuilder rep = new StringBuilder();
        rep.append("VERIFICACIÓN DE ASIGNACIÓN ENCADENADA\n\n");

        final int[] archivos = {0};
        final int[] ok = {0};
        final int[] conProblemas = {0};

        verificarDirectorio(gestorFS.getRaiz(), rep, archivos, ok, conProblemas);

        rep.append("\nResumen:\n")
           .append("Archivos: ").append(archivos[0]).append("\n")
           .append("Correctos: ").append(ok[0]).append("\n")
           .append("Con problemas: ").append(conProblemas[0]).append("\n");

        JOptionPane.showMessageDialog(this, rep.toString(), "Verificar asignación", JOptionPane.INFORMATION_MESSAGE);
    }

    private void verificarDirectorio(Directorio d, StringBuilder rep, int[] archivos, int[] ok, int[] conProblemas) {
        d.getHijos().recorrer((n, i) -> {
            if (n instanceof Directorio sub) {
                verificarDirectorio(sub, rep, archivos, ok, conProblemas);
            } else if (n instanceof Archivo a) {
                archivos[0]++;
                int primer = a.getPrimerBloque();
                int esperados = a.getTamanioEnBloques();
                boolean[] visitado = new boolean[disco.obtenerNumeroBloques()];
                int count = 0;
                boolean ciclo = false;

                int actual = primer;
                while (actual >= 0 && actual < disco.obtenerNumeroBloques()) {
                    if (visitado[actual]) { ciclo = true; break; }
                    visitado[actual] = true;
                    count++;
                    Bloque b = disco.obtenerBloque(actual);
                    int sig = b.getSiguiente();
                    if (sig == -1) break;
                    actual = sig;
                }

                if (!ciclo && count == esperados) {
                    ok[0]++;
                } else {
                    conProblemas[0]++;
                    rep.append("⚠ Archivo ").append(a.getNombre())
                       .append(" — primer=").append(primer)
                       .append(" esperados=").append(esperados)
                       .append(" recorridos=").append(count)
                       .append(ciclo ? " (CICLO detectado)" : " (longitud distinta)")
                       .append("\n");
                }
            }
        });
    }

    private void accionExportarCSV(ActionEvent e) {
        try {
            int n = gestorES.tamanoCola();
            Object[][] tabla = new Object[n][4];
            gestorES.volcarTabla(tabla);

            StringBuilder sb = new StringBuilder();
            sb.append("PID,Operacion,Pista,Espera_ms\n");
            for (int i = 0; i < n; i++) {
                sb.append(tabla[i][0]).append(",")
                  .append(tabla[i][1]).append(",")
                  .append(tabla[i][2]).append(",")
                  .append(tabla[i][3]).append("\n");
            }
            sb.append("\n# Politica,PosCabezal,Recorrido,Atendidas,EsperaProm_ms\n");
            sb.append(obtenerPoliticaSeleccionada().name()).append(",")
              .append(gestorES.getPosicionCabezal()).append(",")
              .append(gestorES.getRecorridoTotal()).append(",")
              .append(gestorES.getSolicitudesAtendidas()).append(",")
              .append(gestorES.getEsperaPromedioMs()).append("\n");

            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Exportar CSV de la cola de E/S");
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                Path destino = fc.getSelectedFile().toPath();
                Files.write(destino, sb.toString().getBytes(StandardCharsets.UTF_8));
                JOptionPane.showMessageDialog(this, "CSV exportado a: " + destino.getFileName());
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al exportar CSV: " + ex.getMessage());
        }
    }

    // MAIN para prueba rápida (opcional)
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal v = new VentanaPrincipal();
            v.setVisible(true);
        });
    }
}