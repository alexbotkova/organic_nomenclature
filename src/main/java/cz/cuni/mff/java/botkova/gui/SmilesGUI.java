package cz.cuni.mff.java.botkova.gui;

import cz.cuni.mff.java.botkova.nomenclature.Nomenclature;

import javax.swing.*;
import java.awt.*;

/**
 * Trida pro zadani molekuly ve SMILES formatu.
 */
public class SmilesGUI {
    private final JTextField inputField;
    private final JTextArea resultArea;
    private final JPanel mainPanel;

    public SmilesGUI() {
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setPreferredSize(new Dimension(400, 150));

        inputField = new JTextField();
        JButton submitButton = new JButton("Pojmenovat");
        resultArea = new JTextArea();
        resultArea.setEditable(false);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        inputPanel.add(new JLabel("Zadejte molekulu ve SMILES formatu:"), BorderLayout.NORTH);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(submitButton, BorderLayout.EAST);

        inputPanel.add(new JPanel(), BorderLayout.SOUTH);
        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        submitButton.addActionListener(e -> {
            try {
                String smiles = inputField.getText();
                String moleculeName = Nomenclature.getName(smiles);
                resultArea.setText("Jmeno: " + moleculeName);
            } catch (Exception ex) {
                resultArea.setText("Chyba: " + ex.getMessage());
            }
        });
    }

    public JPanel getPanel() {
        return mainPanel;
    }
}
