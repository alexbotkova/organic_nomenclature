package cz.cuni.mff.java.botkova.nomenclature;

import java.util.*;
import java.util.stream.Stream;

/**
 * Trida slouzici k vytvoreni nazvu molekuly.
 */
public class Nomenclature {
    public static final String[] SENIOR_GROUPS = {"-COOH", "-SO3H", "-CHO", "-CO", "-OH", "-SH", "-NH2", "-NO2"};
    private static final String[] SENIOR_GROUP_SUFFIXES = { "ova kyselina", "sulfonova kyselina", "al", "on", "ol", "thiol", "amin", "" };
    private static final String[] SENIOR_GROUP_PREFIXES = { "karboxy", "sulfo", "oxo", "oxo", "hydroxy", "sulfanyl", "amino", "nitro" };
    public static final  String[] MULTIPLICITY = {"", "di", "tri", "tetra", "penta"};
    private final Molecule molecule;
    private final Chain chain;
    private final int length;

    private final List<Integer> locantsOfMostSeniorGroup = new ArrayList<>();
    private final List<Integer> locantsOfDoubleBonds = new ArrayList<>();
    private final List<Integer> locantsOfTripleBonds = new ArrayList<>();
    private final Map<String, List<Integer>> locantsOfSeniorGroups = new HashMap<>();
    private final Map<Integer, List<Atom>> locantsOfSideChainStarts = new HashMap<>();
    private final Map<String, List<Integer>> locantsOfSideChainNames = new HashMap<>();
    public String name;

    String[] alkanePrefixes = {
            "meth", "eth", "prop", "but", "pent", "hex", "hept", "okt", "non", "dek",
            "undek", "dodek", "tridek", "tetradek", "pentadek", "hexadek", "heptadek",
            "oktadek", "nonadek", "eikos", "heneikos", "dokos", "trikos", "tetrakos",
            "pentakos", "hexakos", "heptakos", "oktakos", "nonakos", "triakonta",
            "hentriakonta", "dotriakonta", "tritriakonta", "tetratriakonta", "pentatriakonta",
            "hexatriakonta", "heptatriakonta", "oktatriakonta", "nonatriakonta", "tetraconta",
            "hentetraconta", "dotetraconta", "tritetraconta", "tetratetraconta", "pentatetraconta",
            "hexatetraconta", "heptatetraconta", "oktatetraconta", "nonatetraconta", "pentaconta",
            "henpentaconta", "dopentaconta", "tripentaconta", "tetrapentaconta", "pentapentaconta",
            "hexapentaconta", "heptapentaconta", "oktapentaconta", "nonapentaconta", "hexaconta",
            "henhexaconta", "dohexaconta", "trihexaconta", "tetrahexaconta", "pentahexaconta",
            "hexahexaconta", "heptahexaconta", "oktahexaconta", "nonahexaconta", "heptaconta",
            "henheptaconta", "doheptaconta", "triheptaconta", "tetraheptaconta", "pentaheptaconta",
            "hexaheptaconta", "heptaheptaconta", "oktaheptaconta", "nonaheptaconta", "oktaconta",
            "henoctaconta", "dooctaconta", "trioctaconta", "tetraoctaconta", "pentaoctaconta",
            "hexaoctaconta", "heptaoctaconta", "oktaoctaconta", "nonaoctaconta", "enneaconta",
            "henenneaconta", "donenneaconta", "trienneaconta", "tetraenneaconta", "pentaenneaconta",
            "hexaenneaconta", "heptaenneaconta", "oktaenneaconta", "nonaenneaconta", "hekt"
    };

    private Nomenclature(Molecule molecule) throws Molecule.InvalidLigandConfigurationException {
        this(molecule, false);
    }

    private Nomenclature(Molecule molecule, boolean isMain) throws Molecule.InvalidLigandConfigurationException {
        this.molecule = molecule;
        chain = new Chain(molecule);
        length = chain.atoms.size();
        if (isMain) {
            chain.isMain = true;
            chooseDirection();
        }
        parse();
        getSideChainNames();
        name();
    }

    /**
     * Vrati jmeno molekuly zadane v SMILES formatu.
     * @param smiles Retezec ve SMILES formatu.
     * @return Nazev zadane molekuly.
     * @throws SMILESParser.InvalidSmilesException
     * @throws Atom.LigancyExceededException
     * @throws Molecule.InvalidLigandConfigurationException
     */
    public static String getName(String smiles) throws SMILESParser.InvalidSmilesException, Atom.LigancyExceededException, Molecule.InvalidLigandConfigurationException {
        Molecule molecule = SMILESParser.parseMolecule(smiles);
        molecule.identifyStructures();
        Nomenclature nomenclature = new Nomenclature(molecule, true);
        return nomenclature.name;
    }

    public static String getName(Molecule molecule) throws Molecule.InvalidLigandConfigurationException {
        molecule.identifyStructures();
        Nomenclature nomenclature = new Nomenclature(molecule, true);
        return nomenclature.name;
    }

    /**
     * Vytvori nazev molekuly.
     */
    private void name() {
        // Koren nazvu.
        StringBuilder nameInProgress = new StringBuilder(alkanePrefixes[length-1]);
        if (chain.isMain) {
            if (locantsOfDoubleBonds.isEmpty() && locantsOfTripleBonds.isEmpty()) {
                nameInProgress.append("an");
            }
            dealWithMultipleBonds(nameInProgress);
        } else {
            dealWithMultipleBonds(nameInProgress);
            nameInProgress.append("yl");
        }
        if (chain.isCycle) {
            nameInProgress.insert(0, "cyklo");
        }
        // Pridame abecedne setridene predpony vedlejsich retezcu a funkncich skupin.
        appendPrefixes(locantsOfSideChainNames, nameInProgress);
        appendPrefixes(locantsOfSeniorGroups, nameInProgress);
        // Pokud ma funkcni skupinu vyssi nez nitroskupinu, tak pridame koncovku
        if (!locantsOfMostSeniorGroup.isEmpty()) {
            int indexOfMostSeniorGroup = getIndex(SENIOR_GROUPS, molecule.mostSeniorGroup);
            String suffix = SENIOR_GROUP_SUFFIXES[indexOfMostSeniorGroup];
            String locants = String.valueOf(printLocants(locantsOfMostSeniorGroup));
            nameInProgress.append("-").append(locants).append(suffix);
        }
        if (!chain.isMain) {
            nameInProgress.insert(0, "(");
            nameInProgress.append(")");
        }
        name = nameInProgress.toString();
    }

    /**
     * Vypise koncovky pro nasobne vazby.
     * @param nameInProgress Doposavad vytvorene jmeno.
     */
    private void dealWithMultipleBonds(StringBuilder nameInProgress) {
        if (!locantsOfDoubleBonds.isEmpty()) {
            nameInProgress.append("-");
            nameInProgress.append(printLocants(locantsOfDoubleBonds).append("en"));
        } if (!locantsOfTripleBonds.isEmpty()) {
            nameInProgress.append("-");
            nameInProgress.append(printLocants(locantsOfTripleBonds).append("yn"));
        }
    }

    /**
     * Prida k nazvu lokanty.
     * @param locants Seznam lokantu.
     * @return Doposavad vytvorene jmeno.
     */
    private StringBuilder printLocants(List<Integer> locants) {
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < locants.size(); i++) {
            prefix.append(locants.get(i)+1);
            if (i < locants.size() - 1)
                prefix.append(",");
            else
                prefix.append("-");
        }
        prefix.append(MULTIPLICITY[locants.size()-1]);
        return prefix;
    }

    /**
     * Paralelne ziskame nazvy vedlejsich retezcu pouzitim rekurze.
     * @throws Molecule.InvalidLigandConfigurationException
     */
    private void getSideChainNames() throws Molecule.InvalidLigandConfigurationException {
        locantsOfSideChainStarts.entrySet().parallelStream().forEach(entry -> {
            List<Atom> starts = entry.getValue();
            Integer locant = entry.getKey();

            for (Atom start : starts) {
                for (int i = 0; i < start.ligands.length; i++) {
                    Atom ligand = start.ligands[i];
                    if (ligand != null) {
                        if (ligand.ID == chain.atoms.get(locant).ID) {
                            start.ligands[i] = null;
                        }
                    }
                }
                Molecule newMolecule = new Molecule(start);
                try {
                    newMolecule.identifyStructures();
                } catch (Molecule.InvalidLigandConfigurationException e) {
                    throw new RuntimeException(e);
                }
                Nomenclature sideChainName = null;
                try {
                    sideChainName = new Nomenclature(newMolecule);
                } catch (Molecule.InvalidLigandConfigurationException e) {
                    throw new RuntimeException(e);
                }
                List<Integer> locants = locantsOfSideChainNames.getOrDefault(sideChainName.name, new ArrayList<>());
                locants.add(locant);
                locantsOfSideChainNames.put(sideChainName.name, locants);
            }
        });
    }

    /**
     * Prida predpony k nazvu.
     * @param locants Lokanty predpon.
     * @param nameInProgress Doposavad vytvorene jmeno.
     */
    private void appendPrefixes(Map<String, List<Integer>> locants, StringBuilder nameInProgress) {
        List<String> sortedKeys = new ArrayList<>(locants.keySet());
        Collections.sort(sortedKeys);
        sortedKeys = sortedKeys.reversed();
        for (String key : sortedKeys) {
            List<Integer> locantsValue = locants.get(key);
            StringBuilder  prefix = printLocants(locantsValue).append(key).append("-");
            nameInProgress.insert(0, prefix);
        }
    }

    /**
     * Vybereme ten smer retezce, kde je suma lokantu mensi.
     */
    private void chooseDirection() {
        int sumOfLocants = 0;
        int sumOfLocantsReversed = 0;

        for (int i = 0; i < length; i++) {
            int locant = i + 1;
            Atom atom = chain.atoms.get(i);
            sumOfLocants += atom.numOfSeniorGroups * (i + 1);
            sumOfLocants += atom.numOfSeniorGroups * (locantsOfSeniorGroups.size()-i+1);
            //sumOfLocantsReversed += atom.numOfSeniorGroups * (locantsOfSeniorGroups.size()-i+1);
            if (i < chain.atoms.size() - 1) {
                if (i < chain.atoms.size() - 1) {
                    int nextAtomInChainID = chain.atoms.get(i + 1).ID;
                    int bondMultiplicity = getBondMultiplicityBetweenTwoAtoms(atom, nextAtomInChainID);
                    if (bondMultiplicity > 1)
                        sumOfLocants += locant;
                }
                if (i > 0) {
                    int previousAtomInChainID = chain.atoms.get(i - 1).ID;
                    if (getBondMultiplicityBetweenTwoAtoms(atom, previousAtomInChainID) > 1)
                        sumOfLocantsReversed += length - locant;
                }
            }
        }

        if (sumOfLocantsReversed < sumOfLocants) {
            chain.atoms = chain.atoms.reversed();
        }
    }

    /**
     * Zjisti nasobnost vazby 2 atomu.
     * @param atom Atom ve vazbe.
     * @param ligandID ID druheho atomu ve vazbe.
     * @return Nasobnost vazby 2 atomu.
     */
    private int getBondMultiplicityBetweenTwoAtoms(Atom atom, int ligandID) {
        return (int) Stream.of(atom.ligands)
                .filter(Objects::nonNull)
                .filter(ligand -> ligand.ID == ligandID)
                .count();
    }

    /**
     * Zjisti index prvku v seznamu.
     * @param array Zkoumany seznam.
     * @param value Hlexany prvek.
     * @return Index prvku v seznamu.
     */
    public static int getIndex(String[] array, String value) {
        for (int i = 0; i < array.length; i++) {
            if (Objects.equals(array[i], value)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Projde retezec a poznaci, kde se nachazi funkcni skupiny a kde nasobne vazby.
     */
    private void parse() {
        for (int i = 0; i < chain.atoms.size(); i++) {
            Atom atom = chain.atoms.get(i);
            if (atom.hasSeniorGroup()) {
                for (Map.Entry<String, Integer> entry : atom.numOfSeniorGroupsMap.entrySet()) {
                    if (entry.getValue() > 0) {
                        String group = entry.getKey();
                        if (Objects.equals(group, molecule.mostSeniorGroup) && !Objects.equals(group, "-NO2")) {
                            locantsOfMostSeniorGroup.add(i);
                        } else {
                            int indexOfSeniorGroup = getIndex(SENIOR_GROUPS, group);
                            String seniorGroupPrefix = SENIOR_GROUP_PREFIXES[indexOfSeniorGroup];
                            List<Integer> locants = locantsOfSeniorGroups.getOrDefault(seniorGroupPrefix, new ArrayList<>());
                            locants.add(i);
                            locantsOfSeniorGroups.put(seniorGroupPrefix, locants);
                        }
                    }
                }
            } if (atom.hasMultipleBond && i < chain.atoms.size() - 1) {
                int nextAtomInChainID = chain.atoms.get(i + 1).ID;
                int bondMultiplicity = getBondMultiplicityBetweenTwoAtoms(atom, nextAtomInChainID);
                if (bondMultiplicity == 2) {
                    locantsOfDoubleBonds.add(i);
                } else if (bondMultiplicity == 3) {
                    locantsOfTripleBonds.add(i);
                }
            }
            List<Atom> sideChains = new ArrayList<>();
            for (Atom ligand : atom.ligands) {
                if (ligand!= null && ligand.symbol == 'C') {
                    if (!chain.isCycle) {
                        if (i > 0 && ligand.ID != chain.atoms.get(i - 1).ID && i < chain.atoms.size() - 1 && ligand.ID != chain.atoms.get(i + 1).ID) {
                            sideChains.add(ligand);
                        }
                    } else {
                        if (i == 0) {
                            if (ligand.ID != chain.atoms.get(i + 1).ID && ligand.ID != chain.atoms.getLast().ID) {
                                sideChains.add(ligand);

                            }
                        } else if (i == chain.atoms.size() - 1) {
                            if (ligand.ID != chain.atoms.get(i - 1).ID && ligand.ID != chain.atoms.getFirst().ID) {
                                sideChains.add(ligand);

                            }
                        } else {
                            if (ligand.ID != chain.atoms.get(i - 1).ID && ligand.ID != chain.atoms.get(i + 1).ID) {
                                sideChains.add(ligand);
                            }
                        }
                    }
                }
            }
            if (!sideChains.isEmpty())
                locantsOfSideChainStarts.put(i, sideChains);
        }
    }
}