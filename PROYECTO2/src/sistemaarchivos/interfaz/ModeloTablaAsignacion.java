/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sistemaarchivos.interfaz;

import javax.swing.table.AbstractTableModel;
import sistemaarchivos.estructuras.ListaEnlazada;
import sistemaarchivos.sistema.Archivo;
import sistemaarchivos.sistema.Directorio;
import sistemaarchivos.sistema.NodoSistema;

/**
 *
 * @author eabdf
 */
public final class ModeloTablaAsignacion extends AbstractTableModel {
     private final String[] columnas = {"Archivo", "Bloques", "Primer bloque"};
    private Object[][] datos = new Object[0][3];

    public void actualizarDesdeRaiz(Directorio raiz) {
        // Contar archivos
        final int[] contador = {0};
        recorrer(raiz, nodo -> {
            if (nodo instanceof Archivo) contador[0]++;
        });

        Object[][] tmp = new Object[contador[0]][3];
        final int[] fila = {0};
        recorrer(raiz, nodo -> {
            if (nodo instanceof Archivo a) {
                tmp[fila[0]][0] = a.getNombre();
                tmp[fila[0]][1] = a.getTamanioEnBloques();
                tmp[fila[0]][2] = a.getPrimerBloque();
                fila[0]++;
            }
        });

        this.datos = tmp;
        fireTableDataChanged();
    }

    private interface Visitante { void ver(NodoSistema n); }

    private void recorrer(Directorio dir, Visitante v) {
        ListaEnlazada<NodoSistema> hijos = dir.getHijos();
        hijos.recorrer((n, i) -> {
            if (n instanceof Directorio d) {
                recorrer(d, v);
            } else {
                v.ver(n);
            }
        });
    }

    @Override public int getRowCount() { return datos.length; }
    @Override public int getColumnCount() { return columnas.length; }
    @Override public String getColumnName(int column) { return columnas[column]; }
    @Override public Object getValueAt(int rowIndex, int columnIndex) { return datos[rowIndex][columnIndex]; }
}

