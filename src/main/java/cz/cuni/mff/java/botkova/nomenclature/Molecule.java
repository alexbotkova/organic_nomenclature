package cz.cuni.mff.java.botkova.nomenclature;

import java.util.*;

/**
 * Molekula je reprezentovana jako neorientovany graf.
 */
public class Molecule {
    public int numberOfAtoms = 1;
    public Atom start;
    public String mostSeniorGroup;
    public HashMap<String, List<Atom>> atomsWithSeniorGroups = new HashMap<>();
    public List<Atom> atomsWithMultipleBonds = new ArrayList<>();
    public List<Atom[]> startsAndEndsOfCycles = new ArrayList<>();
    public int maxLengthOfCycle = 0;
    public List<Atom> startsOfTheLongestCycles = new ArrayList<>();
    // Zbytek molekuly ziskame pruchodem pres navazane atomy na start.
    public Molecule(Atom start) {
        this.start = start;
    }

    /**
     * Poznacime do mapy, na jakych atomech se nachazi dana funkcni skupina, a na jakych atomech zacina cyklus.
     * @throws InvalidLigandConfigurationException
     */
    public void identifyStructures() throws InvalidLigandConfigurationException {
        identifySeniorGroupsAndMultipleBonds(start, new HashMap<>());
        getMostSeniorGroup();
        findPathBetweenTwoAtoms();
    }

    /**
     * Ziska nejhlavnejsi funkcni skupinu.
     */
    private void getMostSeniorGroup() {
        for (String group : Nomenclature.SENIOR_GROUPS) {
            List<Atom> atoms = atomsWithSeniorGroups.get(group);
            if (atoms != null && !atoms.isEmpty()) {
                mostSeniorGroup = group;
                return;
            }
        }
        mostSeniorGroup = null;
    }

    /**
     * Do mapy atomu a funkncich skupin pridame atom s danou fs.
     * @param atom Pridavany atom.
     * @param group Pridavana funkcni skupina.
     */
    private void addToAtomsWithSeniorGroups(Atom atom, String group) {
        atomsWithSeniorGroups.computeIfAbsent(group, k -> new ArrayList<>()).add(atom);
    }

    /**
     * Odstranime atom ze seznamu atomu s danou fs.
     * @param atom Odstranovany atom.
     * @param group Funkcni skupina, ze ktere odstranujeme atom.
     */
    private void removeFromAtomsWithSeniorGroups(Atom atom, String group) {
        List<Atom> atomsList = atomsWithSeniorGroups.get(group);
        if (atomsList != null) {
            atomsList.remove(atom);
            if (atomsList.isEmpty()) {
                atomsWithSeniorGroups.remove(group);
            }
        }
    }

    /**
     * Projde molekulu a poznamena informace o funkncich skupinach a nasobnych vazbach.
     * @param atom Zrovna zkoumany atom.
     * @param visited Mapa, s informacemi, ktere atomy jsme jiz navstivili.
     * @throws InvalidLigandConfigurationException
     */
    private void identifySeniorGroupsAndMultipleBonds(Atom atom, Map<Integer, Boolean> visited) throws InvalidLigandConfigurationException {
        visited.put(atom.ID,true);

        int atomNumOfH = atom.countHydrogens();
        boolean hasOxoGroup = false;
        List<Integer> bondedCarbonsID = new ArrayList<>();
        int i = 0;
        while (i < atom.ligands.length) {
            Atom ligand = atom.ligands[i];
            // "null" znaci navazany vodik. Pokud narazime na vodik, tak zbytek seznamu ligandu za nim jsou take vodiky.
            if (ligand == null)
                break;

            int ligandNumOfH = ligand.countHydrogens();
            int ligandNumOfO = ligand.countAtoms('O');
            switch (ligand.symbol) {
                case 'C':
                    // Ukladame ID navazanych uhliku, a pokud narazime na jiz ulozene ID, tak se jedna o nasobnou vazbu.
                    if (bondedCarbonsID.contains(ligand.ID) && !atomsWithMultipleBonds.contains(atom))
                        atomsWithMultipleBonds.add(atom);
                    else
                        bondedCarbonsID.add(ligand.ID);
                    // Dale prochazime nenavstivene uhliky.
                    if (!visited.getOrDefault(ligand.ID, false))
                        identifySeniorGroupsAndMultipleBonds(ligand, visited);
                    break;
                case 'O':
                    if (ligandNumOfH == 1) {
                        addToAtomsWithSeniorGroups(atom, "-OH");
                        atom.incrementNumOfSeniorGroup("-OH", 1);
                    } else if (ligandNumOfH == 0) {
                        hasOxoGroup = true;
                        i++;
                    } else {
                        throw new InvalidLigandConfigurationException(atom, 'O');
                    }
                    break;
                case 'S':
                    if (ligandNumOfH > 0 && ligandNumOfO == 0) {
                        addToAtomsWithSeniorGroups(atom, "-SH");
                        atom.incrementNumOfSeniorGroup("-SH", 1);
                    } else if (ligandNumOfO == 3){
                        addToAtomsWithSeniorGroups(atom, "-SO3H");
                        atom.incrementNumOfSeniorGroup("-SO3H", 1);
                    } else {
                        throw new InvalidLigandConfigurationException(atom, 'S');
                    }
                    break;
                case 'N':
                    if (ligandNumOfH == 2) {
                        addToAtomsWithSeniorGroups(atom, "-NH2");
                        atom.incrementNumOfSeniorGroup("-NH2", 1);
                    } else if (ligandNumOfO == 2) {
                        addToAtomsWithSeniorGroups(atom, "-NO2");
                        atom.incrementNumOfSeniorGroup("-NO2", 1);
                    } else {
                        throw new InvalidLigandConfigurationException(atom, 'N');
                    }
                    break;
            }
            i++;
        }
        if (hasOxoGroup) {
            int numOfOH = atom.numOfSeniorGroupsMap.getOrDefault("-OH", 0);
            if (numOfOH > 0) {
                // -COOH se sklada z -OH a =O. Pokud je na uhliku =O a -OH, zvysime pocet -COOH o 1 a -OH snizime o 1.
                atom.incrementNumOfSeniorGroup("-COOH", 1);
                atom.incrementNumOfSeniorGroup("-OH", -1);
                addToAtomsWithSeniorGroups(atom, "-COOH");

                // Pokud jsme takto zuzitkovali vsechny -OH, odstranime polozku z mapy.
                if (atom.numOfSeniorGroupsMap.getOrDefault("-OH", 0) == 0)
                    removeFromAtomsWithSeniorGroups(atom, "-OH");
            } else if (atomNumOfH > 0) {
                // Pokud nema -OH, ale ma nejake vodiky, jedna se o aldehyd.
                atom.incrementNumOfSeniorGroup("-CHO", 1);
                addToAtomsWithSeniorGroups(atom, "-CHO");
            } else {
                // Jinak je to keton.
                atom.incrementNumOfSeniorGroup("-CO", 1);
                addToAtomsWithSeniorGroups(atom, "-CO");
            }
        }
    }

    /**
     * Nalezenim cesty mezy zacatky a konci cyklu oznacime vsechny uhliky v danem cyklu.
     */
    private void findPathBetweenTwoAtoms() {
        List<Atom> path;
        for (Atom[] atoms : startsAndEndsOfCycles) {
            Atom start = atoms[0]; Atom end = atoms[1];
            path = new ArrayList<>();
            path.add(start);
            //findPathBetweenTwoAtoms(start, end, new HashMap<>(), path);
            Map<Integer, Boolean> visited = new HashMap<>();
            visited.put(end.ID, true);
            findPathBetweenTwoAtoms(start, start, end, visited, path);
            path.add(end);

            if (path.size() > maxLengthOfCycle) {
                maxLengthOfCycle = path.size();
                startsOfTheLongestCycles.clear();
                startsOfTheLongestCycles.add(start);
            } else if (path.size() == maxLengthOfCycle) {
                startsOfTheLongestCycles.add(start);
            }

            for (Atom atom : path) {
                atom.isPartOfCycle = true;
            }
        }
    }

    /**
     * Mezi zadanymi 2 atomy nalezne cestu. Pro oznaceny atomu v cyklu.
     * @param current Prave zkoumany atom.
     * @param end Konec cesty.
     * @param visited Mapa, s informacemi, ktere atomy jsme jiz navstivili.
     * @param path Tvorena cesta.
     * @return Cestu mezi 2 zadanymi atomy.
     */
    private boolean findPathBetweenTwoAtoms(Atom current, Atom start, Atom end, Map<Integer, Boolean> visited, List<Atom> path) {
        //if (Objects.equals(current, end))
          //  return true;

        if (!Objects.equals(current, start)) {
            for (Atom ligand : current.ligands) {
                if (Objects.equals(end, ligand))
                    return true;
            }
        }

        visited.put(current.ID, true);

        for (Atom ligand : current.ligands) {
            //if (ligand != null && ligand.symbol == 'C' && !visited.getOrDefault(ligand.ID, false) && ligand.ID > current.ID) {
            if (ligand != null && ligand.symbol == 'C' && !visited.getOrDefault(ligand.ID, false) &&!Objects.equals(end, ligand)) {
                path.add(ligand);

                if (findPathBetweenTwoAtoms(ligand, start, end, visited, path))
                    return true;
                path.removeLast();
            }
        }
        return false;
    }

    public static class InvalidLigandConfigurationException extends Exception {
        public InvalidLigandConfigurationException(Atom atom, char ligandSymbol) {
            super("Neplatna funkcni skupina na atomu " + atom.toString() + " zacinajici na " + ligandSymbol + ".");
        }
    }

    /**
     * Pro debuggovani.
     * @return Retezcova reprezentace molekuly.
     */
    public String DFS() {
        return DFS(start, new HashMap<>(), new StringBuilder()).toString();
    }
    private StringBuilder DFS(Atom atom, Map<Integer, Boolean> visited, StringBuilder output) {
        visited.put(atom.ID, true);
        output.append(atom.symbol);
        int numOfH = atom.countHydrogens();
        if (numOfH > 0)
            output.append("H").append(numOfH);

        for (Atom ligand : atom.ligands) {
            if (ligand != null) {
                if (!visited.getOrDefault(ligand.ID, false)) {
                    output.append("(");
                    output = new StringBuilder(DFS(ligand, visited, output));
                    output.append(")");
                }
            }
        }
        return output;
    }
    public void findStartsAndEndsOfCycles() {
        findStartsAndEndsOfCycles(new boolean[numberOfAtoms], start, null);
    }

    private void findStartsAndEndsOfCycles(boolean[] visited, Atom atom, Atom parent) {
        visited[atom.ID] = true;
        for (Atom ligand : new HashSet<>(Arrays.asList(atom.ligands))) {
            if (ligand != null) {
                if (!visited[ligand.ID]) {
                    findStartsAndEndsOfCycles(visited, ligand, atom);
                } else if (!Objects.equals(ligand, parent)) {
                    startsAndEndsOfCycles.add(new Atom[]{ligand, atom});
                }
            }
        }
    }
}




