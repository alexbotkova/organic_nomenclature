package cz.cuni.mff.java.botkova.gui;

import cz.cuni.mff.java.botkova.nomenclature.Atom;

import java.awt.*;

/**
 * Trida reprezentujici uzivatelem nakresleny bod v GUI predstavujici dany atom.
 */
public class AtomPoint {
    private final Point point;
    private final AtomType type;

    final int id;
    Atom atom;
    public AtomPoint(Point point, AtomType type, int id) {
        this.point = point;
        this.type = type;
        this.id = id;
    }

    public AtomPoint(AtomPoint point) {
        this.point = point.point;
        this.type = point.type;
        this.id = point.id;
        this.atom = point.atom;
    }

    /**
     * Pro ziskani souradnic bodu v GUI.
     * @return Souradnice bodu.
     */
    public Point getPoint() {
        return point;
    }

    /**
     * Pro ziskani barvy atomu podle jeho typu.
     * @return Barva bodu.
     */
    public Color getColor() {
        switch (type) {
            case CARBON:
                return Color.BLACK;
            case OXYGEN:
                return Color.RED;
            case NITROGEN:
                return Color.BLUE;
            case SULFUR:
                return Color.YELLOW;
            default:
                return Color.BLACK;
        }
    }
}
