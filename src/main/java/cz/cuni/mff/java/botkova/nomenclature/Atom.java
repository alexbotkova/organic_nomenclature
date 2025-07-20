package cz.cuni.mff.java.botkova.nomenclature;

import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Trida slouzici jako sablona pro konkretni implementace atomu, ktere tvori molekulu.
 */
public class Atom {
    public int ID;
    public char symbol;
    public Atom[] ligands;
    public HashMap<String, Integer> numOfSeniorGroupsMap = new HashMap<>(); // Mapa pro uchovani poctu senior skupin pripojenych k tomuto atomu
    public int numOfSeniorGroups = 0; // Celkovy pocet senior skupin pripojenych k tomuto atomu
    public boolean isPartOfCycle = false;
    public boolean hasMultipleBond = false;
    public int numOfDoubleBonds = 0;
    public int numOfTripleBonds = 0;

    public Atom(int ligancy, char symbol) { // ligance = pocet potencialnich vazeb
        ligands = new Atom[ligancy];
        this.symbol = symbol;
    }

    public Atom(Atom atom) {
        this.ID = atom.ID;
        this.symbol = atom.symbol;
        this.ligands = new Atom[atom.ligands.length];
        for (int i = 0; i < atom.ligands.length; i++) {
            ligands[i] = null;
        }
        for (String seniorGroup : atom.numOfSeniorGroupsMap.keySet())
            this.numOfSeniorGroupsMap.put(seniorGroup, atom.numOfSeniorGroupsMap.get(seniorGroup));
        this.numOfSeniorGroups = atom.numOfSeniorGroups;
        this.isPartOfCycle = atom.isPartOfCycle;
        this.hasMultipleBond = atom.hasMultipleBond;
        this.numOfDoubleBonds = atom.numOfDoubleBonds;
        this.numOfTripleBonds = atom.numOfTripleBonds;
    }

    public void reset() {
        numOfSeniorGroupsMap = new HashMap<>();
        numOfSeniorGroups = 0;
        isPartOfCycle = false;
    }

    /**
     * Vazani tohoto atomu s jinym atomem pomoci urcite nasobnosti vazby
     * @param atom Atom, ktery vazeme.
     * @param bondMultiplicity Vaznost vazby.
     * @throws LigancyExceededException
     */
    public void bindAtoms(Atom atom, int bondMultiplicity) throws LigancyExceededException {
        if (bondMultiplicity > 1) {
            hasMultipleBond = true;
            atom.hasMultipleBond = true;
            if (bondMultiplicity == 2) {
                numOfDoubleBonds++;
                atom.numOfDoubleBonds++;
            } else {
                numOfTripleBonds++;
                atom.numOfTripleBonds++;
            }
        }
        for (int i = 0; i < bondMultiplicity; i++) {
            bindAtom(atom);
            atom.bindAtom(this);
        }
    }

    /**
     * Pomocna funkce pro navazani atomu.
     * @param atom Vazany atom.
     * @throws LigancyExceededException
     */
    private void bindAtom(Atom atom) throws LigancyExceededException {
        boolean exceededLigancy = true;
        // Pokud potrebujeme vytvorit vazbu, ale atom ma vsechny sva vazebna mista obsazena jinymi atomy, vyhodime vyjimku.
        for (int i = 0; i < ligands.length; i++) {
            if (ligands[i] == null) {
                ligands[i] = atom;
                exceededLigancy = false;
                break;
            }
        }
        if (exceededLigancy)
            throw new LigancyExceededException();
    }

    /**
     * Spocita navazane vodiky. "null" hodnoty znaci navazane vodiky.
     * @return Pocet navazanych vodiku.
     */
    public int countHydrogens() {
        return (int) Stream.of(ligands).filter(Objects::isNull).count();
    }

    /**
     * Spocita navazane atomy daneho prvku.
     * @param atomSymbol Symbol daneho prvku.
     * @return Pocet navazanych atomu.
     */
    public int countAtoms(char atomSymbol) {
        return (int) Stream.of(ligands)
                .filter(ligand -> ligand != null && ligand.symbol == atomSymbol)
                .count();
    }

    /**
     * Zvysi pocet dane fs.
     * @param key Dana fs.
     * @param incrementBy O kolik zvetsime pocet fs.
     */
    public void incrementNumOfSeniorGroup(String key, int incrementBy) {
        numOfSeniorGroupsMap.merge(key, incrementBy, Integer::sum);
        numOfSeniorGroups += incrementBy;
    }

    @Override
    public String toString() {
        return symbol + "-" + ID;
    }

    /**
     * Zjisti, zda ma atom nejakou fs.
     * @return True, pokud ma fs, false, pokud ne.
     */
    public boolean hasSeniorGroup() {
        return !(numOfSeniorGroupsMap.isEmpty());
    }

    public static class LigancyExceededException extends Exception {
        public LigancyExceededException() {
            super("Ligance prekrocena.");
        }
    }
}
