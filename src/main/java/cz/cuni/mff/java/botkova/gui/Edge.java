package cz.cuni.mff.java.botkova.gui;

/**
 * Trida reprezentujici hranu mezi dvemy body. Predstavuje vazbu mezi dvemi atomy a typ predstabuje jeji násobnost.
 */
public class Edge {
    AtomPoint p1, p2;
    int type;
    public Edge(AtomPoint p1, AtomPoint p2, int type) {
        this.p1 = p1;
        this.p2 = p2;
        this.type = type;
    }

    public Edge(Edge edge) {
        this.p1 = edge.p1;
        this.p2 = edge.p2;
        this.type = edge.type;
    }
}
