package cz.cuni.mff.java.botkova.gui;

import cz.cuni.mff.java.botkova.nomenclature.Molecule;
import cz.cuni.mff.java.botkova.nomenclature.Nitrogen;

import javax.swing.*;

/**
 * Trida pro uvodni stranku GUI nechavajici uzivatele rozhodnout, zda chce molekulu zadat textove ve SMILES formatu, nebo ji nakreslit.
 */
public class MoleculeInputGUI extends JFrame{
    public MoleculeInputGUI() {
        setTitle("Zadani molekuly");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();
        JPanel smilesPanel = createSmilesPanel();
        JPanel drawPanel = createDrawPanel();

        tabbedPane.addTab("SMILES", smilesPanel);
        tabbedPane.addTab("Nakreslit molekulu", drawPanel);
        add(tabbedPane);
    }

    /**
     * Vytvori SMILES panel.
     * @return SMILES panel
     */
    private JPanel createSmilesPanel() {
        SmilesGUI smilesGui = new SmilesGUI();
        return smilesGui.getPanel();
    }

    /**
     * Vytvori panel pro kresleni.
     * @return panel pro kresleni
     */
    private JPanel createDrawPanel() {
        Molecule molecule = new Molecule(new Nitrogen());
        DrawGUI drawGui = new DrawGUI(molecule);
        return drawGui.getPanel();
    }

    /**
     * Zde se cela aplikace spousti.
     * @param args
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MoleculeInputGUI app = new MoleculeInputGUI();
            app.setVisible(true);
        });
    }
}