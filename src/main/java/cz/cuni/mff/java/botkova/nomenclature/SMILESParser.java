package cz.cuni.mff.java.botkova.nomenclature;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Stack;

/**
 * Trida pro nacitani molekul z retezcu ve formatu SMILES
 */
public class SMILESParser {
    private static final String ATOM_SYMBOLS= "CONS"; // Pismena predstavujici atomy
    private static final String BOND_SYMBOLS= "-=#"; // Symboly predstavujici nasobnosti vazeb

    /**
     * Rozhodne, zda znak predstavuje atom.
     * @param ch Nacteny znak.
     * @return True, pokud znak predstavuje podporovany atom. False, pokud ne.
     */
    private static boolean isAtom(char ch) {
        return ATOM_SYMBOLS.indexOf(ch) != -1;
    }

    /**
     * Rozhodne, zda znak predstavuje nasobnost vazby.
     * @param ch Nacteny znak.
     * @return True, pokud znak predstavuje nasobnost vazby. False, pokud ne.
     */
    private static boolean isBond(char ch) {
        return BOND_SYMBOLS.indexOf(ch) != -1;
    }

    /**
     * Pokud znak predstavuje atom, funkce vrati instanci daneho atomu.
     * @param ch Nacteny znak.
     * @return Instanci daneho atomu.
     */
    private static Atom recognizeAtom(char ch) {
        return switch (ch) {
            case 'C' -> new Carbon();
            case 'O' -> new Oxygen();
            case 'N' -> new Nitrogen();
            case 'S' -> new Sulphur();
            default -> null;
        };
    }

    /**
     * Pokud znak predstavuje nasobnost vazby, funkce vrati nasobnost.
     * @param ch Nacteny znak
     * @return Nasobnost vazby.
     */
    private static int getBondMultiplicity(char ch) {
        return switch (ch) {
            case '-' -> 1;
            case '=' -> 2;
            case '#' -> 3;
            default -> -1;
        };
    }

    /**
     * Ze SMILES retezce vygeneruje instanci tridy Molekula.
     * @param smiles SMILES retezec.
     * @return Instanci molekuly.
     */
    public static Molecule parseMolecule(String smiles) throws InvalidSmilesException, Atom.LigancyExceededException {
        char[] smilesCharArray = smiles.toCharArray();
        char startChar = smilesCharArray[0];
        if (startChar != 'C') {
                throw new InvalidSmilesException.InvalidFormat();
        }
        // Prvni znak ve SMILES retezci musi byt pismeno znacici atom. Pokud ne, tak je retezec neplatne zadan a program skonci.
        Atom start = recognizeAtom(startChar);
        if (start == null)
            throw new InvalidSmilesException.InvalidCharacter(startChar);
        start.ID = 0;
        Molecule molecule = new Molecule(start);

        // Inicializujeme zasobnik pro uschovu atomu a informaci, v jake vetvi se nachazeji.
        Stack<Atom> stack = new Stack<>();
        stack.push(start);
        int bondMultiplicity = 1;
        HashMap<Integer, Atom> startsOfCycles = new HashMap<>();
        for (char ch : Arrays.copyOfRange(smilesCharArray, 1, smilesCharArray.length)) {
            // Pokud nacitany znak predstavuje atom a nahore v zasobniku je taky atom, tak je to jeho rodic a navazeme je na sebe s danou nasobnosti.
            if (isAtom(ch)) {
                Atom atom = recognizeAtom(ch);
                Atom parent = stack.peek();

                if (parent != null) {
                    parent.bindAtoms(atom, bondMultiplicity);
                    molecule.numberOfAtoms++;
                    atom.ID = molecule.numberOfAtoms-1;
                    bondMultiplicity = 1;
                }
                // Atom pridame do zasobniku.
                stack.push(atom);
            // Ziskame nasobnost vazby dalsiho atomu, ktery nacteme, s atomem na vrcholu zasobniku.
            } else if (isBond(ch)) {
                bondMultiplicity = getBondMultiplicity(ch);
            // '(' znaci zacatek nove vetve, ktera se bude vazat na atom pred '('.
            } else if (ch == '(') {
                // "null" bude znacit zacatek vetve
                stack.push(null);
            // ')' znaci konec vetve. "null" nam slouzil, jako placeholder zacatku nove vetve, takze odebirame atomy ze zasobniku dokud na nej nenarazime.
            // Atom, ktery byl za "null" navazeme na atom, ktery byl pred nim.
            } else if (ch == ')') {
                if (!stack.isEmpty()) {
                    Atom branchStart = null;
                    Atom atom = stack.pop();
                    while (atom != null) {
                        branchStart = atom;
                        atom = stack.pop();
                    }
                    Atom parent = stack.peek();
                    parent.bindAtoms(branchStart, bondMultiplicity);
                    bondMultiplicity = 1;
                    molecule.numberOfAtoms++;
                    if (branchStart != null) {
                        branchStart.ID = molecule.numberOfAtoms-1;
                    } else
                        throw new InvalidSmilesException.InvalidFormat();
                }
            // Zacatek cyklu je znacen cislici, konec take.
            } else if (Character.isDigit(ch)) {
                Integer key = Character.getNumericValue(ch);
                // Pokud mame danou cislici jiz ulozenou, tak se jedna o konec cyklu, kdy ho spojime se zacatkem, ktery je ulozen v hash mape.
                if (startsOfCycles.containsKey(key)) {
                    Atom startOfCycle = startsOfCycles.get(key);
                    Atom endOfCycle = stack.peek();
                    molecule.startsAndEndsOfCycles.add(new Atom[]{startOfCycle, endOfCycle});

                    startOfCycle.bindAtoms(endOfCycle, bondMultiplicity);
                    bondMultiplicity = 1;
                    startsOfCycles.remove(key);
                }
                else {
                    Atom  startOfCycle = stack.peek();
                    startsOfCycles.put(key, startOfCycle);
                }
            } else {
                throw new InvalidSmilesException.InvalidCharacter(ch);
            }
        }
        return molecule;
    }

    public static class InvalidSmilesException extends Exception {
        public InvalidSmilesException(String message) {
            super(message);
        }
        public static class InvalidCharacter extends InvalidSmilesException {
            public InvalidCharacter(char ch) {
                super("Neplatny znak ve SMILES retezci: " + ch);
            }
        }
        public static class InvalidFormat extends InvalidSmilesException {
            public InvalidFormat() {
                super("Neplatny format SMILES retezce.");
            }
        }
    }
}