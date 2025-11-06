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
/**
 *
 * @author eabdf
 */
public class VentanaPrincipal extends JFrame {
   private final JTree arbolSistema;
    private final JTable tablaAsignacion;
    private final JTable tablaColaES;
    private final JRadioButton radioAdmin;
    private final JRadioButton radioUsuario;
    private final JComboBox<PoliticaPlanificacion> comboPolitica;

    public VentanaPrincipal() {
        super(constantes.TITULO_APLICACION);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(constantes.ANCHO_VENTANA, constantes.ALTO_VENTANA);
        setLocationRelativeTo(null);

      
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

        
        arbolSistema = new JTree(); 
        JScrollPane scrollArbol = new JScrollPane(arbolSistema);

  
        JTabbedPane pestañas = new JTabbedPane();

       
        JPanel panelDiscoPlaceholder = new JPanel(new GridBagLayout());
        panelDiscoPlaceholder.add(new JLabel("Panel de Disco (se activa en PASO 1)"));
        pestañas.addTab("Disco", panelDiscoPlaceholder);

       
        tablaAsignacion = new JTable(new DefaultTableModel(
                new Object[][]{},
                new String[]{"Archivo", "Bloques", "Primer bloque"}
        ));
        pestañas.addTab("Tabla de asignación", new JScrollPane(tablaAsignacion));

       
        tablaColaES = new JTable(new DefaultTableModel(
                new Object[][]{},
                new String[]{"PID", "Operación", "Destino", "Pista/Sector"}
        ));
        pestañas.addTab("Cola de E/S", new JScrollPane(tablaColaES));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollArbol, pestañas);
        split.setDividerLocation(320);
        getContentPane().add(split, BorderLayout.CENTER);

        
        JLabel etiquetaEstado = new JLabel("Listo. Paso 0 completado.");
        getContentPane().add(etiquetaEstado, BorderLayout.SOUTH);

        btnCrearDir.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Crear directorio (disponible en PASO 2)"));
        btnCrearArch.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Crear archivo (disponible en PASO 2)"));
        btnEliminar.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Eliminar (disponible en PASO 2)"));
        btnGuardar.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Guardar JSON (disponible en pasos posteriores)"));
        btnCargar.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Cargar JSON (disponible en pasos posteriores)"));
    }

    public ModoUsuario obtenerModoActual() {
        return radioAdmin.isSelected() ? ModoUsuario.ADMINISTRADOR : ModoUsuario.USUARIO;
    }

    public PoliticaPlanificacion obtenerPoliticaSeleccionada() {
        return (PoliticaPlanificacion) comboPolitica.getSelectedItem();
    } 
}
