/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sistemaarchivos.interfaz;

import javax.swing.table.AbstractTableModel;
import sistemaarchivos.EntradaSalida.GestorES;

/**
 *
 * @author eabdf
 */
public final class ModeloTablaColaES extends AbstractTableModel {
    private final String[] columnas = {"PID", "Operación", "Pista", "Espera (ms)"};
    private Object[][] datos = new Object[0][4];

    public void actualizarDesdeGestor(GestorES gestor) {
        int n = gestor.tamanoCola();
        Object[][] tmp = new Object[n][4];
        gestor.volcarTabla(tmp);
        this.datos = tmp;
        fireTableDataChanged();
    }

    @Override public int getRowCount() { return datos.length; }
    @Override public int getColumnCount() { return columnas.length; }
    @Override public String getColumnName(int c) { return columnas[c]; }
    @Override public Object getValueAt(int r, int c) { return datos[r][c]; }
}
