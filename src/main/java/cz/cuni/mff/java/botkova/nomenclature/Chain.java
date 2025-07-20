package cz.cuni.mff.java.botkova.nomenclature;

import java.util.*;
import java.util.stream.Stream;

/**
 * Trida predstavujici retezec uhliku v molekule.
 */
public class Chain {
    // V molekule najdeme jeden hlavni retezec tvorici koren nazvu, z nej vychazejici retezce jsou vedlejsi.
    boolean isMain = false;
    // Pokud je retezec tvoren uhliky tvorici cyklus.
    boolean isCycle = false;
    // Seznam atomu v retezci.
    List<Atom> atoms;

    public Chain(Molecule molecule) {
        atoms = getPath(molecule);
    }

    /**
     * Z molekuly ziska mnoziny cest potencialnich hlavnich retezcu.Heuristicky zacne prohledavanim z atomu obsahujici strukturu, ktera musi byt podle
     * hierarchickych kriterii obsazena v hlavnim retezci.
     * @param molecule Zpracovavana molekula.
     * @return Mnoziny cest potencialnich hlavnich retezcu.
     */
    private List<Atom> getPath(Molecule molecule) {
        // Pokud ma molekula funkcni skupiny.
        List<Atom> atomsWithSeniorGroups = molecule.atomsWithSeniorGroups.getOrDefault(molecule.mostSeniorGroup, new ArrayList<>());
        if (!atomsWithSeniorGroups.isEmpty())
            return getPath(molecule, atomsWithSeniorGroups);
        // Cykly
        List<Atom> startsOfTheLongestCycles = molecule.startsOfTheLongestCycles;
        if (!startsOfTheLongestCycles.isEmpty())
            return  getPath(molecule, startsOfTheLongestCycles);
        // Nasobne vazby
        List<Atom> atomsWithMultipleBonds = molecule.atomsWithMultipleBonds;
        if (!atomsWithMultipleBonds.isEmpty())
            return getPath(molecule, atomsWithMultipleBonds);
        // Pokud ani jedno z vyse uvedenych, bude hlavni retezec nejdelsi cesta v molekule - neorientovanem grafu.
        else {
            List<Atom> furthestAtomsFromStart = BFS(molecule.start, new HashMap<>(), new HashMap<>());
            // Pro pripad, ze molekula obsahuje pouze 1 uhlik.
            if (furthestAtomsFromStart.isEmpty()) {
                List<Atom> methan = new ArrayList<>();
                methan.add(molecule.start);
                return methan;
            }
            return getPath(molecule, furthestAtomsFromStart);
        }
    }

    /**
     * Z mnoziny atomu obsahujich danou strukturu zacne prohledavat molekulu a hledat nejdelsi cesty, ktere nasledne profiltruje.
     * @param molecule Prochazena molekula.
     * @param atoms Mnozina atomu s danou strukturou, z kterych zaciname vyhledavani.
     * @return profiltrovanou cestu.
     */
    private List<Atom> getPath(Molecule molecule, List<Atom> atoms) {
        List<List<Atom>> cyclePaths = new ArrayList<>();
        List<List<Atom>> completePaths = new ArrayList<>();
        Map<Atom, List<List<Atom>>> partialPaths = new HashMap<>();

        while (!atoms.isEmpty()) {
            Atom start = atoms.getFirst();
            Set<Integer> processedIds = new HashSet<>();

            // Pokud je atom v cyklu, retezec jsou atomy tvprici dany cyklus.
            if (start.isPartOfCycle) {
                List<Atom> path = getCyclicPaths(start, molecule, atoms);
                removeProcessedAtoms(path, atoms);
                cyclePaths.add(path);
            // Pokud je atom list, tak jako potencialni retezce vezmeme vsechny z nej vychazejici cesty.
            } else if ((int) Stream.of(start.ligands)
                    .filter(Objects::nonNull)
                    .filter(atom -> atom.symbol == 'C')
                    .filter(atom -> processedIds.add(atom.ID))
                    .count() <= 1) {
               getAcyclicPaths(molecule, start, completePaths, new HashMap<>(), new HashMap<>(), atoms);
           // Pro nelistovy atom take, ale s tim, ze plnou cestu vytvorime spojenim dvou cest.
            } else {
                partialPaths.put(start, getAcyclicPaths(molecule, start, new ArrayList<>(), new HashMap<>(), new HashMap<>(), atoms));
                addPartialPaths(partialPaths, molecule, atoms);
            }
        }
        // Mnoziny potencialnich cest profiltrujeme podle kriterii.
        Filter filter = new Filter(cyclePaths, completePaths, partialPaths, this);
        return filter.filter();
    }

    /**
     * Atomy z mnoziny zacatku prohledavani jsou postupne odebirany, jak se vyskytuji v jiz objevenych cestach.
     * @param path Cesta, ve ktere hledame taove atomy.
     * @param atomsToProcess Mnozina tomu, ze ktere odebirame.
     */
    private void removeProcessedAtoms(List<Atom> path, List<Atom> atomsToProcess) {
        for (Atom atom : path) {
            atomsToProcess.remove(atom);
        }
    }

    /**
     * Pokud je u nejakeho atomu pouze jedna castecna cesta, najde dalsi s druhym nejlepsim poctem atomu s danou strukturou.
     * @param partialPaths Prochazene castecne cesty.
     * @param molecule Molekula, ve ktere jsou dane cesty.
     * @param atoms Atomy, z kterych spoustime vyhledavani.
     */
    private void addPartialPaths(Map<Atom, List<List<Atom>>> partialPaths, Molecule molecule, List<Atom> atoms) {
        for (Map.Entry<Atom, List<List<Atom>>> entry : partialPaths.entrySet()) {
            List<List<Atom>> paths = entry.getValue();
            if (paths.size() == 1) {
                Map<Integer, Boolean> visitedForBFS = new HashMap<>();
                Map<Integer, Boolean> visitedForGetPath = new HashMap<>();
                List<Atom> path = paths.getFirst();
                path.forEach(atom -> {
                    visitedForBFS.put(atom.ID, true);
                    visitedForGetPath.put(atom.ID, true);
                });
                Atom start = entry.getKey();
                visitedForBFS.put(start.ID, false); visitedForGetPath.put(start.ID,false);
                List<List<Atom>> pathsToAdd = getAcyclicPaths(molecule, start, new ArrayList<>(), visitedForBFS, visitedForGetPath, atoms);
                paths.addAll(pathsToAdd);
            }
        }
    }

    /**
     * Funkce pro nalezeni cesty, pokud atom neni soucasti cyklu.
     * @param molecule Prochazena molekula.
     * @param start Atom, ze ktereho spoustime vyhledavani.
     * @param paths Mnozina, do ktere pridame nalezenou cestu.
     * @param visitedForBFS Mapa s navstivenymi atomy pro funkci BFS.
     * @param visitedForGetPath Mapa s navstivenymi atomy pro funkci getPath.
     * @param atoms Atomy, ze kterych spoustime vyhledavani.
     * @return Mnozinu cest.
     */
    private List<List<Atom>> getAcyclicPaths(Molecule molecule, Atom start, List<List<Atom>> paths, Map<Integer, Boolean> visitedForBFS, Map<Integer, Boolean> visitedForGetPath, List<Atom> atoms) {
        Map<Integer, Integer> distances = new HashMap<>();
        List<Atom> furthestAtoms = BFS(start, distances, visitedForBFS);

        for (Atom end : furthestAtoms) {
            visitedForGetPath.put(start.ID, false);
            visitedForGetPath.put(end.ID, true);
            List<Atom> path = getPath(end, start, distances, visitedForGetPath, molecule);

            if (path != null) {
                removeProcessedAtoms(path, atoms);
                paths.add(path);
            }
        }
        if (furthestAtoms.isEmpty()) {
            List<Atom> path = new ArrayList<>();
            path.add(start);
            removeProcessedAtoms(path, atoms);
            paths.add(path);
        }
        return paths;
    }

    /**
     * Odstranime atomy s funkcni skupinou z atomu v cyklicke ceste.
     * @param ligand Atom, o kterem odstranime informace  z molekuly.
     * @param molecule Molekula, z ktere odstranujeme informace.
     */
    private void removeUsedAtomsWithMostSeniorGroup(Atom ligand, Molecule molecule) {
        if (ligand.numOfSeniorGroupsMap.getOrDefault(molecule.mostSeniorGroup, 0) > 0) {
            molecule.atomsWithSeniorGroups.get(molecule.mostSeniorGroup).remove(ligand);
        }
    }

    /**
     * Ziska cyklickou cestu
     * @param start Zacatek cyklu.
     * @param molecule Molekula, ve ktere se cyklus nachazi
     * @param atoms Mnozina zacatku startu.
     * @return Cyklickou cestu.
     */
    private List<Atom> getCyclicPaths(Atom start, Molecule molecule, List<Atom> atoms) {
        Map<Integer, Boolean> inPath = new HashMap<>();
        inPath.put(start.ID,true);
        List<Atom> path = new ArrayList<>();
        path.add(start);

        Atom current = start;
        while (true) {
            boolean foundNext = false;
            for (Atom ligand : current.ligands) {
                if (ligand != null && ligand.isPartOfCycle && !inPath.getOrDefault(ligand.ID, false)) {
                    inPath.put(ligand.ID, true);
                    path.add(ligand);
                    removeUsedAtomsWithMostSeniorGroup(ligand, molecule);
                    current = ligand;
                    foundNext = true;
                    break; // Prerusime for cyklus a pokracujeme v prozkoumavani cesty.
                }
            }
            if (!foundNext) {
                removeProcessedAtoms(path, atoms);
                return path;
            }
        }
    }

    /**
     * Prohleda molekulu do sirky a od zadaneho zacatku najde nejvzdalenejsi atomy.
     * @param start Zacatek prohledavani.
     * @param distances Mapa vzdalenosti atomu od startu.
     * @param visited Mapa, kam ukladame, zda atom byl ci nebyl navstiven.
     * @return Mnozinu nejvzdalenejsi atomy od startu.
     */
    private List<Atom> BFS(Atom start, Map<Integer, Integer> distances, Map<Integer, Boolean> visited) {
        Queue<Atom> queue = new LinkedList<>();
        queue.add(start);
        visited.put(start.ID , true);
        distances.put(start.ID, 0);

        int maxDistance = -1;
        List<Atom> furthestAtoms = new ArrayList<>();

        while (!queue.isEmpty()) {
            Atom current = queue.poll();
            for (Atom ligand : current.ligands) {
                if (ligand != null && ligand.symbol == 'C' && !visited.getOrDefault(ligand.ID, false) && !ligand.isPartOfCycle) {
                    visited.put(ligand.ID, true);

                    int distance = distances.get(current.ID) + 1;
                    if (distance > maxDistance) {
                        maxDistance = distance;
                        furthestAtoms.clear();
                        furthestAtoms.add(ligand);
                    } else if (distance == maxDistance) {
                        furthestAtoms.add(ligand);
                    }

                    distances.put(ligand.ID, distance);
                    queue.add(ligand);
                }
            }
        }
        return furthestAtoms;
    }

    /**
     * Ziska cestu mezi dvema atomy.
     * @param end Konec cesty.
     * @param start Zacatek cesty.
     * @param distances Mapa vzdalenosti atomu od startu.
     * @param visited Mapa, kam ukladame, zda atom byl ci nebyl navstiven.
     * @param molecule Prochazena molekula.
     * @return Cestu mezi startem a zacatkem.
     */
    private List<Atom> getPath(Atom end, Atom start, Map<Integer, Integer> distances, Map<Integer, Boolean> visited, Molecule molecule) {
        List<Atom> path = new ArrayList<>();
        path.add(end);

        Atom current = end;
        while (current != start) {
            boolean foundNext = false;
            for (Atom ligand : current.ligands) {
                if (ligand != null && ligand.symbol == 'C' && !visited.getOrDefault(ligand.ID, false) && distances.getOrDefault(ligand.ID, -7) == distances.get(current.ID) - 1) {
                    visited.put(ligand.ID, true);
                    path.add(ligand);
                    removeUsedAtomsWithMostSeniorGroup(ligand, molecule);
                    current = ligand;
                    foundNext = true;
                    break;
                }
            }
            if (!foundNext) {
                return null;
            }
        }
        return path;
    }
}
