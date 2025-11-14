/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sistemaarchivos.interfaz;
import sistemaarchivos.disco.Disco;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JPanel;
/**
 *
 * @author eabdf
 */
public final class PanelDisco extends JPanel {
    private Disco disco;
    private final int columnas = 20; 
    private final int tamCelda = 20;
    private final int margen   = 6;

    public PanelDisco(Disco disco) {
        this.disco = disco;
        setBackground(Color.WHITE);
        actualizarTamanoPreferido();
    }

    public void setDisco(Disco disco) {
        this.disco = disco;
        actualizarTamanoPreferido();
        repaint();
    }
    
    private void actualizarTamanoPreferido() {
        int filas = (int) Math.ceil((double) disco.obtenerNumeroBloques() / columnas);
        int ancho = columnas * tamCelda + margen * 2;
        int alto  = filas * tamCelda + margen * 2;
        setPreferredSize(new Dimension(ancho, alto));
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (disco == null) return;

        boolean[] libres = disco.mapaLibres();

        int filas = (int) Math.ceil((double) libres.length / columnas);
        int x0 = margen;
        int y0 = margen;

        int idx = 0;
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                int x = x0 + c * tamCelda;
                int y = y0 + f * tamCelda;

                if (idx < libres.length) {
                    g.setColor(libres[idx] ? new Color(200, 255, 200) : new Color(255, 200, 200));
                    g.fillRect(x, y, tamCelda - 1, tamCelda - 1);

                    g.setColor(Color.GRAY);
                    g.drawRect(x, y, tamCelda - 1, tamCelda - 1);
                } else {
                    g.setColor(Color.LIGHT_GRAY);
                    g.drawRect(x, y, tamCelda - 1, tamCelda - 1);
                }
                idx++;
            }
        }
    } 
}
