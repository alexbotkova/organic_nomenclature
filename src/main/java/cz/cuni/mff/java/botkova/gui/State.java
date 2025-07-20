package cz.cuni.mff.java.botkova.gui;

import java.util.ArrayList;
import java.util.List;
import cz.cuni.mff.java.botkova.nomenclature.*;

/**
 * Trida pro ukladani dat DrawGUI v dany moment.
 * @param points Prave existujici body nakreslene v GUI.
 * @param edges Prave existujici hrany mezi body nakreslene v GUI.
 * @param atoms Atomuy reprezentované prave existujicimi body nakreslene v GUI.
 */
public record State(List<AtomPoint> points, List<Edge> edges, List<Atom> atoms) {
    public State(List<AtomPoint> points, List<Edge> edges, List<Atom> atoms) {
        // Vytvoreni deep kopii.
        this.points = new ArrayList<>(points.size());
        for (AtomPoint point : points) {
            this.points.add(new AtomPoint(point));
        }

        this.edges = new ArrayList<>(edges.size());
        for (Edge edge : edges) {
            this.edges.add(new Edge(edge));
        }

        List<List<Integer>> listOfLigands = new ArrayList<>();
        for (Atom value : atoms) {
            List<Integer> ligands = new ArrayList<>();
            for (Atom ligand : value.ligands) {
                if (ligand != null)
                    ligands.add(ligand.ID);
            }
            listOfLigands.add(ligands);
        }

        this.atoms = new ArrayList<>(atoms.size());
        for (Atom atom : atoms) {
            this.atoms.add(new Atom(atom));
        }

        for (int i = 0; i < this.atoms.size(); i++) {
            Atom atom = this.atoms.get(i);
            List<Integer> ligands = listOfLigands.get(i);
            for (int j = 0; j < ligands.size(); j++) {
                atom.ligands[j] = this.atoms.get(ligands.get(j));
            }
        }
    }
}
