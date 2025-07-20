package cz.cuni.mff.java.botkova.gui;

import cz.cuni.mff.java.botkova.nomenclature.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * Trida pro nakresleni moelkuly.
 */
public class DrawGUI {
    private List<AtomPoint> points = new ArrayList<>();
    private List<Edge> edges = new ArrayList<>();
    private AtomPoint selectedPoint = null;
    private Edge selectedEdge = null;
    private AtomType currentAtomType = AtomType.CARBON; // Default je uhlik.
    private int edgeType = 1; // Default je jednoducha vazba.
    private List<Atom> atoms = new ArrayList<>();
    private Molecule molecule;
    private final JPanel mainPanel;
    private final JTextArea nameDisplayArea;
    private final Stack<State> historyStack = new Stack<>(); // Zasobnik na ulozeni stavu.

    public DrawGUI(Molecule molecule) {
        this.molecule = molecule;

        mainPanel = new JPanel(new BorderLayout());
        DrawingPanel drawingPanel = new DrawingPanel();
        mainPanel.add(drawingPanel, BorderLayout.CENTER);

        JPanel atomControlPanel = new JPanel();
        atomControlPanel.setLayout(new BoxLayout(atomControlPanel, BoxLayout.Y_AXIS));

        JButton carbonButton = new JButton("Uhlik");
        JButton oxygenButton = new JButton("Kyslik");
        JButton nitrogenButton = new JButton("Dusik");
        JButton sulfurButton = new JButton("Sira");

        JButton singleEdgeButton = new JButton("Jednoducha vazba");
        JButton doubleEdgeButton = new JButton("Dvojna vazba");
        JButton tripleEdgeButton = new JButton("Trojna vazba");

        carbonButton.addActionListener(e -> currentAtomType = AtomType.CARBON);
        oxygenButton.addActionListener(e -> currentAtomType = AtomType.OXYGEN);
        nitrogenButton.addActionListener(e -> currentAtomType = AtomType.NITROGEN);
        sulfurButton.addActionListener(e -> currentAtomType = AtomType.SULFUR);

        singleEdgeButton.addActionListener(e -> edgeType = 1);
        doubleEdgeButton.addActionListener(e -> edgeType = 2);
        tripleEdgeButton.addActionListener(e -> edgeType = 3);

        atomControlPanel.add(carbonButton);
        atomControlPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Odmezereni
        atomControlPanel.add(oxygenButton);
        atomControlPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Odmezereni
        atomControlPanel.add(nitrogenButton);
        atomControlPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Odmezereni
        atomControlPanel.add(sulfurButton);
        atomControlPanel.add(Box.createRigidArea(new Dimension(0, 30))); // Odmezereni
        atomControlPanel.add(singleEdgeButton);
        atomControlPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Odmezereni
        atomControlPanel.add(doubleEdgeButton);
        atomControlPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Odmezereni
        atomControlPanel.add(tripleEdgeButton);


        mainPanel.add(atomControlPanel, BorderLayout.WEST);

        JPanel edgeControlPanel = new JPanel();
        edgeControlPanel.setLayout(new BoxLayout(edgeControlPanel, BoxLayout.Y_AXIS));

        JButton deleteButton = new JButton("Smazat");
        JButton nameButton = new JButton("Pojmenovat");
        JButton eraseButton = new JButton("Smazat molekulu");
        JButton undoButton = new JButton("Zpet");

        deleteButton.addActionListener(e -> {
            saveState(); // Ulozeni stavu pred odstranenim
            deleteSelected();
        });

        nameButton.addActionListener(e -> nameMolecule());

        eraseButton.addActionListener(e -> {
            saveState(); // Ulozeni stavu pred odstranenim
            eraseMolecule();
        });

        undoButton.addActionListener(e -> undoAction());

        edgeControlPanel.add(deleteButton);
        edgeControlPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Odmezereni
        edgeControlPanel.add(nameButton);
        edgeControlPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Odmezereni
        edgeControlPanel.add(eraseButton);
        edgeControlPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Odmezereni
        edgeControlPanel.add(undoButton);

        mainPanel.add(edgeControlPanel, BorderLayout.EAST);

        nameDisplayArea = new JTextArea();
        nameDisplayArea.setEditable(false);
        nameDisplayArea.setLineWrap(true);
        nameDisplayArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(nameDisplayArea);
        scrollPane.setPreferredSize(new Dimension(800, 50));
        mainPanel.add(scrollPane, BorderLayout.SOUTH);

        drawingPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point p = e.getPoint();
                selectedEdge = null; // Reset

                if (e.getButton() == MouseEvent.BUTTON1) { // Leve tlacitko to pridat/ vybrat bod/ hranu
                    saveState(); // Ulozit stav pred pridanim
                    AtomPoint foundPoint = findPoint(p);
                    if (foundPoint != null) {
                        selectedPoint = foundPoint;
                        selectedEdge = null; // Zajistime, ze zadna hrana neni vybrana, kdyz jsme vybrali bod
                    } else {
                        selectedPoint = null; // Zajistime, ze zadny bod neni vybrana, kdyz jsme vybrali hranu
                        Edge foundEdge = findEdge(p);
                        if (foundEdge != null) {
                            selectedEdge = foundEdge;
                        } else { // Pokud jsme nevybrali nic, tak vytvorime novy bod
                            AtomPoint newPoint = new AtomPoint(p, currentAtomType, points.size());
                            points.add(newPoint);
                            selectedPoint = newPoint;

                            Atom newAtom = null;
                            switch (currentAtomType) {
                                case CARBON -> {
                                    newAtom = new Carbon();
                                    molecule.start = newAtom;
                                }
                                case OXYGEN -> newAtom = new Oxygen();
                                case NITROGEN -> newAtom = new Nitrogen();
                                case SULFUR -> newAtom = new Sulphur();
                            }
                            newAtom.ID = newPoint.id;
                            atoms.add(newAtom);
                        }
                    }
                } else if (e.getButton() == MouseEvent.BUTTON3) { // Prave tlacitko na vytvoreni nove hrany
                    if (selectedPoint != null) {
                        saveState(); // Ulozime stav pred vytvorenim nove hrany
                        Atom atom1 = atoms.get(selectedPoint.id);

                        AtomPoint endPoint = findPoint(p);
                        if (endPoint != null && !endPoint.equals(selectedPoint)) {
                            boolean exc = false;
                            Atom atom2 = atoms.get(endPoint.id);
                            try {
                                atom1.bindAtoms(atom2, edgeType);
                            } catch (Atom.LigancyExceededException ex) {
                                undoAction();
                                exc = true;
                                JOptionPane.showMessageDialog(
                                        null,
                                        ex.getMessage(),
                                        "Error",
                                        JOptionPane.ERROR_MESSAGE
                                );
                            }
                            if (!exc) edges.add(new Edge(selectedPoint, endPoint, edgeType));
                            selectedPoint = null;
                        }
                    }
                } else if (e.getClickCount() == 2) { // Dvojklik na smazani hrany nebo bodu
                    saveState(); // Ulozime stav pred smazanim
                    p = e.getPoint();
                    AtomPoint foundPoint = findPoint(p);
                    if (foundPoint != null) {
                        deletePoint(foundPoint);
                    } else {
                        Edge foundEdge = findEdge(p);
                        if (foundEdge != null) {
                            edges.remove(foundEdge);
                        }
                    }
                }
                drawingPanel.repaint();
            }
        });
    }

    private void saveState() {
        List<AtomPoint> previousPoints = new ArrayList<>(points);
        List<Edge> previousEdges = new ArrayList<>(edges);
        List<Atom> previousAtoms = new ArrayList<>(atoms);
        historyStack.push(new State(previousPoints, previousEdges, previousAtoms));
    }

    private void undoAction() {
        if (!historyStack.isEmpty()) {
            State previousState = historyStack.pop();
            this.points = previousState.points();
            this.edges = previousState.edges();
            this.atoms = previousState.atoms();
            mainPanel.repaint(); // Prekreslime

            // Start molekuly bude uhlik
            if (!atoms.isEmpty()) {
                for (Atom atom : atoms) {
                    if (atom.symbol == 'C') {
                        molecule.start = atom;
                        break;
                    }
                }
            } else {
                // TODO
                molecule.start = null;
            }
        } else {
            JOptionPane.showMessageDialog(
                    null,
                    "Neni co vratit zpet.",
                    "Zpet",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    /**
     * Pred pojmenovanim musi body a hrany tvorit souvisly graf.
     * @return true pokud jsou vsechny body spojene
     */
    private boolean checkConnectivity() {
        boolean[] visited = new boolean[atoms.size()];
        DFS(atoms.getFirst(), visited);
        for (boolean isConnected : visited) {
            if (!isConnected)
                return false;
        }
        return true;
    }

    /**
     * Pomocna funkce pro checkConnectivity().
     * @param atom
     * @param visited
     */
    private void DFS(Atom atom, boolean[] visited) {
        visited[atom.ID] = true;
        for (Atom ligand : atom.ligands) {
            if (ligand != null && !visited[ligand.ID])
                DFS(ligand, visited);
        }
    }

    /**
     * Metoda pro pojmenovani molekuly.
     */
    private void nameMolecule() {
        molecule = new Molecule(new Nitrogen());
        for (Atom atom : atoms) {
            atom.reset();
            if (atom.symbol == 'C') {
                molecule.start = atom;
            }
        }
        try {
            if (molecule.start.symbol == 'N')
                throw new EmptyMoleculeException();

            boolean isConnected = checkConnectivity();
            if (!isConnected)
                throw new DisconnectedAtomsException();

            molecule.numberOfAtoms = atoms.size();
            molecule.findStartsAndEndsOfCycles();
            String moleculeName = Nomenclature.getName(molecule);

            nameDisplayArea.setText("Nazev molekuly: " + moleculeName);

            mainPanel.repaint();
        } catch (Molecule.InvalidLigandConfigurationException | EmptyMoleculeException | DisconnectedAtomsException e) {
            JOptionPane.showMessageDialog(
                    null,
                    e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Metoda pro vymazani aktualni molekuly a kreslici plochy
     */
    private void eraseMolecule() {
        points.clear();
        edges.clear();
        atoms.clear();
        molecule = new Molecule(new Nitrogen());
        nameDisplayArea.setText("");
        mainPanel.repaint();
    }

    /**
     * Metoda pro nalezeni bodu s danymi souradnicemi.
     * @param p Souradnice
     * @return Bod
     */
    private AtomPoint findPoint(Point p) {
        for (AtomPoint point : points) {
            if (point.getPoint().distance(p) < 10) {
                return point;
            }
        }
        return null;
    }

    /**
     * Najde hranu, na ktere lezi bod ziskany stisknutim mysi.
     * @param p Souradnice bodu, kde byla stisknuta mys.
     * @return Hranu nebo null.
     */
    private Edge findEdge(Point p) {
        for (Edge edge : edges) {
            if (edge.p1 == null || edge.p2 == null) continue;
            if (isPointOnLine(p, edge.p1.getPoint(), edge.p2.getPoint())) {
                return edge;
            }
        }
        return null;
    }

    /**
     * Pomocna funkce pro findEdge(). Zjisti zda bod lezi na dane hrane.
     * @param p bod
     * @param p1 jeden konec hrany
     * @param p2 druhy konec hrany
     * @return true/ false podle toho zda je ci neni bod na dane hrane
     */
    private boolean isPointOnLine(Point p, Point p1, Point p2) {
        int threshold = 3;
        double distance = Math.abs((p2.y - p1.y) * p.x - (p2.x - p1.x) * p.y + p2.x * p1.y - p2.y * p1.x) / p1.distance(p2);
        return distance < threshold;
    }

    /**
     * Smaze bod a vsechny hrany, ktere z nej vedou.
     * @param point Bod, jez ma byt smazan.
     */
    private void deletePoint(AtomPoint point) {
        Atom atom = atoms.get(point.id);
        for (Atom ligand : atom.ligands) {
            if (ligand != null) {
                for (int i = 0; i < ligand.ligands.length; i++) {
                    if (Objects.equals(ligand.ligands[i], atom))
                        ligand.ligands[i] = null;
                }
            }
        }
        atoms.remove(atom);
        points.remove(point);
        edges.removeIf(edge -> Objects.equals(edge.p1,point) || Objects.equals(edge.p2,point));
    }

    /**
     * Smaze vazbu mezi dvema atomy.
     * @param atom1
     * @param atom2
     */
    private void deleteBond(Atom atom1, Atom atom2) {
        int bondMultiplicity = 0;
        for (int i = 0; i < atom1.ligands.length; i++) {
            Atom ligand = atom1.ligands[i];
            if (Objects.equals(ligand, atom2)) {
                atom1.ligands[i] = null;
                bondMultiplicity++;
            }
        }
        if (bondMultiplicity > 0) {
            if (bondMultiplicity == 2)
                atom1.numOfDoubleBonds--;
            else if (bondMultiplicity == 3)
                atom1.numOfTripleBonds--;

            if (atom1.numOfDoubleBonds == 0 && atom1.numOfTripleBonds == 0)
                atom1.hasMultipleBond = false;
        }
    }

    /**
     * Smaze bud vybrany bod nebo hranu.
     */
    private void deleteSelected() {
        if (selectedPoint != null) {
            deletePoint(selectedPoint);
            selectedPoint = null; // Reset
        } else if (selectedEdge != null) {
            Atom atom1 = atoms.get(selectedEdge.p1.id);
            Atom atom2 = atoms.get(selectedEdge.p2.id);
            deleteBond(atom1, atom2);
            deleteBond(atom2, atom1);

            edges.remove(selectedEdge);
            selectedEdge = null; // Reset
        }
        mainPanel.repaint();
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    private class DrawingPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Namaluje hrany
            for (Edge edge : edges) {
                if (edge == null || edge.p1 == null || edge.p2 == null) continue;

                if (edge.equals(selectedEdge)) {
                    g.setColor(Color.GREEN); // Zvyrazni zelene vybranou hranu.
                } else {
                    g.setColor(Color.BLACK);
                }

                if (edge.type == 1) {
                    g.drawLine(edge.p1.getPoint().x, edge.p1.getPoint().y, edge.p2.getPoint().x, edge.p2.getPoint().y);
                } else if (edge.type == 2) {
                    drawDoubleEdge(g, edge.p1.getPoint(), edge.p2.getPoint());
                } else if (edge.type == 3) {
                    drawTripleEdge(g, edge.p1.getPoint(), edge.p2.getPoint());
                }
            }

            // Namaluje body
            for (AtomPoint atomPoint : points) {
                if (atomPoint.equals(selectedPoint)) {
                    g.setColor(Color.GREEN); // Zvyrazni zelene vybrany bod.
                } else {
                    g.setColor(atomPoint.getColor());
                }

                Point point = atomPoint.getPoint();
                g.fillOval(point.x - 5, point.y - 5, 10, 10);
            }
        }

        private void drawDoubleEdge(Graphics g, Point p1, Point p2) {
            int offset = 3;
            g.drawLine(p1.x - offset, p1.y - offset, p2.x - offset, p2.y - offset);
            g.drawLine(p1.x + offset, p1.y + offset, p2.x + offset, p2.y + offset);
        }

        private void drawTripleEdge(Graphics g, Point p1, Point p2) {
            int offset = 5;
            g.drawLine(p1.x - offset, p1.y - offset, p2.x - offset, p2.y - offset);
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
            g.drawLine(p1.x + offset, p1.y + offset, p2.x + offset, p2.y + offset);
        }
    }

    public static class EmptyMoleculeException extends Exception {
        public EmptyMoleculeException() {
            super("Prazdna molekula.");
        }
    }

    public static class DisconnectedAtomsException extends Exception {
        public DisconnectedAtomsException() {
            super("Nenavazane atomy.");
        }
    }
}