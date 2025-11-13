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
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import sistemaarchivos.EntradaSalida.GestorES;
import sistemaarchivos.EntradaSalida.SolicitudES;
import sistemaarchivos.EntradaSalida.TipoOperacionesES;
import sistemaarchivos.disco.Disco;
import sistemaarchivos.modelo.NodoArbolVista;
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
    private final JTextField campoCabezal = new JTextField("0", 4);

    public VentanaPrincipal() {
        super(constantes.TITULO_APLICACION);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(constantes.ANCHO_VENTANA, constantes.ALTO_VENTANA);
        setLocationRelativeTo(null);

        // Modelo base
        this.disco = Disco.crearPorDefecto();
        this.gestorFS = new GestorSistemaArchivos(disco);

        // ---- Barra superior ----
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
        radioAdmin = new JRadioButton(constantes.TEXTO_MODO_ADMIN, true);
        radioUsuario = new JRadioButton(constantes.TEXTO_MODO_USUARIO, false);
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
        JButton btnIniciarES = new JButton("Iniciar E/S");
        JButton btnDetenerES = new JButton("Detener E/S");
        barra.add(btnAgregarES);
        barra.add(btnIniciarES);
        barra.add(btnDetenerES);
        barra.add(new JLabel(" Cabezal: "));
        barra.add(campoCabezal);

        getContentPane().add(barra, BorderLayout.NORTH);

        // ---- Izquierda: Árbol ----
        arbolSistema = new JTree();
        JScrollPane scrollArbol = new JScrollPane(arbolSistema);

        // ---- Derecha: pestañas ----
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

        getContentPane().add(etiquetaMetricas, BorderLayout.SOUTH);

        // ---- Poblado inicial ----
        refrescarVista();

        // ---- Acciones FS ----
        btnCrearDir.addActionListener(e -> accionCrearDirectorio());
        btnCrearArch.addActionListener(e -> accionCrearArchivo());
        btnEliminar.addActionListener(e -> accionEliminar());

        // Persistencia (PASO 5; aquí solo aviso)
        btnGuardar.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "Guardar JSON estará disponible en el PASO 5."));
        btnCargar.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "Cargar JSON estará disponible en el PASO 5."));

        // ---- Acciones E/S ----
        btnAgregarES.addActionListener(e -> accionAgregarES());
        btnIniciarES.addActionListener(e -> accionIniciarES());
        btnDetenerES.addActionListener(e -> accionDetenerES());

        // Política seleccionable (aplica al GestorES)
        comboPolitica.addActionListener(e -> {
            PoliticaPlanificacion p = (PoliticaPlanificacion) comboPolitica.getSelectedItem();
            gestorES.setPolitica(p);
        });

        // Timer UI para refrescar cola y métricas sin bloquear EDT
        new javax.swing.Timer(400, e -> {
            modeloColaES.actualizarDesdeGestor(gestorES);
            etiquetaMetricas.setText("E/S: " +
                    (gestorES.getSolicitudesAtendidas() >= 0 ? "activa" : "detenida")
                    + " | Recorrido: " + gestorES.getRecorridoTotal()
                    + " | Espera prom.: " + gestorES.getEsperaPromedioMs() + " ms"
                    + " | Atendidas: " + gestorES.getSolicitudesAtendidas());
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
        if (n instanceof Directorio) {
            Directorio d = (Directorio) n;
            d.getHijos().recorrer((h, i) -> ui.add(construirNodoUI(h)));
        }
        return ui;
    }

    private String rutaSeleccionada() {
        javax.swing.tree.TreePath path = arbolSistema.getSelectionPath();
        if (path == null) return "raiz";
        Object[] comps = path.getPath();
        String ruta = "";
        for (int i = 0; i < comps.length; i++) {
            ruta += comps[i].toString();
            if (i < comps.length - 1) ruta += "/";
        }
        return ruta;
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
        JTextField campoNombre = new JTextField();
        JTextField campoBloques = new JTextField();
        Object[] msg = {"Nombre del archivo:", campoNombre, "Tamaño (en bloques):", campoBloques};
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
        String ruta = "";
        for (int i = 0; i < comps.length - 1; i++) {
            ruta += comps[i].toString();
            if (i < comps.length - 2) ruta += "/";
        }
        return ruta.isEmpty() ? "raiz" : ruta;
    }

    // -------------------- E/S --------------------
    private void accionAgregarES() {
        JTextField campoPid = new JTextField();
        JComboBox<TipoOperacionesES> comboTipo = new JComboBox<>(TipoOperacionesES.values());
        JTextField campoPista = new JTextField();
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
        try { pos = Integer.parseInt(campoCabezal.getText().trim()); } catch (NumberFormatException ex) { pos = 0; }
        gestorES.configurarCabezal(pos, constantes.PISTAS_DISCO - 1);
        // aplica política actual del combo
        PoliticaPlanificacion p = (PoliticaPlanificacion) comboPolitica.getSelectedItem();
        gestorES.setPolitica(p);
        gestorES.iniciar();
    }

    private void accionDetenerES() {
        gestorES.detener();
    }

    // -------------------- Utilidades --------------------
    public ModoUsuario obtenerModoActual() {
        return radioAdmin.isSelected() ? ModoUsuario.ADMINISTRADOR : ModoUsuario.USUARIO;
    }

    public PoliticaPlanificacion obtenerPoliticaSeleccionada() {
        return (PoliticaPlanificacion) comboPolitica.getSelectedItem();
    }
}
