/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sistemaarchivos.interfaz;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import sistemaarchivos.disco.Bloque;
import sistemaarchivos.disco.Disco;

/**
 *
 * @author eabdf
 */
public final class InspectorCadena extends JDialog {
   private final JTextArea area = new JTextArea();

    public InspectorCadena(JFrame padre, Disco disco, int primerBloque, int bloquesEsperados) {
        super(padre, "Inspector de cadena (asignación encadenada)", true);
        setLayout(new BorderLayout(6, 6));
        area.setEditable(false);
        add(new JScrollPane(area), BorderLayout.CENTER);

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());
        JPanel sur = new JPanel();
        sur.add(btnCerrar);
        add(sur, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(560, 360));
        pack();
        setLocationRelativeTo(padre);

        poblar(disco, primerBloque, bloquesEsperados);
    }

    private void poblar(Disco disco, int primer, int esperados) {
        StringBuilder sb = new StringBuilder();
        if (primer < 0) {
            sb.append("El archivo no tiene bloques asignados (primerBloque = -1).\n");
            area.setText(sb.toString());
            return;
        }
        boolean[] visitado = new boolean[disco.obtenerNumeroBloques()];
        int actual = primer;
        int cuenta = 0;
        boolean ciclo = false;

        sb.append("Primer bloque: ").append(primer).append("\n");
        sb.append("Cadena: [");

        while (actual >= 0 && actual < visitado.length) {
            if (visitado[actual]) { ciclo = true; break; }
            visitado[actual] = true;

            sb.append(actual);
            cuenta++;

            Bloque b = disco.obtenerBloque(actual);
            int sig = b.getSiguiente();
            if (sig >= 0) sb.append(" -> ");
            actual = sig;
        }
        sb.append("]\n\n");
        sb.append("Bloques recorridos: ").append(cuenta).append("\n");
        sb.append("Bloques esperados : ").append(Math.max(0, esperados)).append("\n");

        if (ciclo) {
            sb.append("\n⚠ Se detectó un ciclo en la cadena (bloque repetido).\n");
        } else if (esperados > 0 && cuenta != esperados) {
            sb.append("\n⚠ Tamaño inconsistente: recorridos != esperados.\n");
        } else {
            sb.append("\n✓ Cadena consistente.\n");
        }
        area.setText(sb.toString());
    } 
}
