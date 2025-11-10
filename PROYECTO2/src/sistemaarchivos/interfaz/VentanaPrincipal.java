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
import sistemaarchivos.disco.Disco;
import sistemaarchivos.modelo.NodoArbolVista;
import sistemaarchivos.sistema.GestorSistemaArchivos;

/**
 *
 * @author eabdf
 */
public class VentanaPrincipal extends JFrame {
   private final JTree arbolSistema;
    private final ModeloTablaAsignacion modeloAsignacion;
    private final JTable tablaAsignacion;
    private final JTable tablaColaES;
    private final JRadioButton radioAdmin;
    private final JRadioButton radioUsuario;
    private final JComboBox<PoliticaPlanificacion> comboPolitica;

    private final PanelDisco panelDisco;
    private final Disco disco;
    private final GestorSistemaArchivos gestor;

    public VentanaPrincipal() {
        super(constantes.TITULO_APLICACION);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(constantes.ANCHO_VENTANA, constantes.ALTO_VENTANA);
        setLocationRelativeTo(null);

        // Modelo
        this.disco = Disco.crearPorDefecto();
        this.gestor = new GestorSistemaArchivos(disco);

        // Barra
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

        getContentPane().add(barra, BorderLayout.NORTH);

        // Árbol
        arbolSistema = new JTree();
        JScrollPane scrollArbol = new JScrollPane(arbolSistema);

        // Pestañas derecha
        JTabbedPane pestañas = new JTabbedPane();

        panelDisco = new PanelDisco(disco);
        JScrollPane scrollDisco = new JScrollPane(panelDisco);
        pestañas.addTab("Disco", scrollDisco);

        modeloAsignacion = new ModeloTablaAsignacion();
        tablaAsignacion = new JTable(modeloAsignacion);
        pestañas.addTab("Tabla de asignación", new JScrollPane(tablaAsignacion));

        tablaColaES = new JTable(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{"PID", "Operación", "Destino", "Pista/Sector"}
        ));
        pestañas.addTab("Cola de E/S", new JScrollPane(tablaColaES));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollArbol, pestañas);
        split.setDividerLocation(320);
        getContentPane().add(split, BorderLayout.CENTER);

        JLabel etiquetaEstado = new JLabel("Paso 2: CRUD básico y asignación encadenada.");
        getContentPane().add(etiquetaEstado, BorderLayout.SOUTH);

        // Poblado inicial
        refrescarVista();

        // Acciones
        btnCrearDir.addActionListener(e -> accionCrearDirectorio());
        btnCrearArch.addActionListener(e -> accionCrearArchivo());
        btnEliminar.addActionListener(e -> accionEliminar());

        btnGuardar.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Guardar JSON (se implementará en paso de Persistencia)"));
        btnCargar.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Cargar JSON (se implementará en paso de Persistencia)"));
    }

    private void refrescarVista() {
        // Actualiza árbol
        DefaultMutableTreeNode raizUI = construirNodoUI(gestor.getRaiz());
        arbolSistema.setModel(new DefaultTreeModel(raizUI));
        // Actualiza disco
        panelDisco.repaint();
        // Actualiza tabla de asignación
        modeloAsignacion.actualizarDesdeRaiz(gestor.getRaiz());
    }

    private DefaultMutableTreeNode construirNodoUI(sistemaarchivos.sistema.NodoSistema n) {
        DefaultMutableTreeNode ui = new DefaultMutableTreeNode(n.getNombre());
        if (n instanceof sistemaarchivos.sistema.Directorio d) {
            d.getHijos().recorrer((h, i) -> ui.add(construirNodoUI(h)));
        }
        return ui;
    }

    private String rutaSeleccionada() {
        var path = arbolSistema.getSelectionPath();
        if (path == null) return "raiz";
        Object[] comps = path.getPath();
        // construir ruta tipo "raiz/subdir/otro"
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
        boolean ok = gestor.crearDirectorio(rutaSeleccionada(), nombre.trim(), "admin");
        if (!ok) {
            JOptionPane.showMessageDialog(this, "No se pudo crear el directorio (ruta inválida o ya existe).");
        }
        refrescarVista();
    }

    private void accionCrearArchivo() {
        if (obtenerModoActual() != ModoUsuario.ADMINISTRADOR) {
            JOptionPane.showMessageDialog(this, "Solo el Administrador puede crear archivos.");
            return;
        }
        JTextField campoNombre = new JTextField();
        JTextField campoBloques = new JTextField();
        Object[] msg = {
                "Nombre del archivo:", campoNombre,
                "Tamaño (en bloques):", campoBloques
        };
        int r = JOptionPane.showConfirmDialog(this, msg, "Crear archivo", JOptionPane.OK_CANCEL_OPTION);
        if (r != JOptionPane.OK_OPTION) return;

        String nombre = campoNombre.getText();
        int tam;
        try {
            tam = Integer.parseInt(campoBloques.getText());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ingrese un tamaño válido (entero > 0).");
            return;
        }
        boolean ok = gestor.crearArchivo(rutaSeleccionada(), nombre.trim(), "admin", tam);
        if (!ok) {
            JOptionPane.showMessageDialog(this, "No se pudo crear el archivo (espacio insuficiente o conflicto de nombre).");
        }
        refrescarVista();
    }

    private void accionEliminar() {
        if (obtenerModoActual() != ModoUsuario.ADMINISTRADOR) {
            JOptionPane.showMessageDialog(this, "Solo el Administrador puede eliminar.");
            return;
        }
        var path = arbolSistema.getSelectionPath();
        if (path == null || path.getPathCount() <= 1) {
            JOptionPane.showMessageDialog(this, "Seleccione un archivo o directorio (no la raíz).");
            return;
        }
        String nombre = path.getLastPathComponent().toString();
        String rutaPadre = construirRutaPadre(path.getPath());
        // intentar eliminar archivo; si no, directorio
        boolean ok = gestor.eliminarArchivo(rutaPadre, nombre);
        if (!ok) ok = gestor.eliminarDirectorioRecursivo(rutaPadre, nombre);
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

    public ModoUsuario obtenerModoActual() {
        return radioAdmin.isSelected() ? ModoUsuario.ADMINISTRADOR : ModoUsuario.USUARIO;
    }

    public PoliticaPlanificacion obtenerPoliticaSeleccionada() {
        return (PoliticaPlanificacion) comboPolitica.getSelectedItem();
    }
}
