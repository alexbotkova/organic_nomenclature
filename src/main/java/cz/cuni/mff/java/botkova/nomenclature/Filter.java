package cz.cuni.mff.java.botkova.nomenclature;

import java.util.*;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * Trida pro vyfiltrovani potencialnich cest na jednu, ktera nejvice odpovida IUPAC kriteriim.
 */
public class Filter {
    private List<List<Atom>> cyclePaths;
    private List<List<Atom>> completePaths;
    private Map<Atom, List<List<Atom>>> partialPaths;
    private Chain chain;
    public Filter(List<List<Atom>> cyclePaths, List<List<Atom>> completePaths, Map<Atom, List<List<Atom>>> partialPaths, Chain chain) {
        this.cyclePaths = cyclePaths;
        this.completePaths = completePaths;
        this.partialPaths = partialPaths;
        this.chain = chain;
    }

    private List<List<Atom>> filteredPathsFromCompleteOrCycles = new ArrayList<>();
    private Map<Atom, List<List<Atom>>> filteredPathsFromPartial = new HashMap<>();

    // filtry
    private static final ToIntFunction<? super Atom> SENIOR_GROUPS_LAMBDA = atom -> atom.numOfSeniorGroups;
    private static final ToIntFunction<? super Atom> CARBONS_IN_CYCLE_LAMBDA = Filter::countCarbonsInCycle;
    private static final ToIntFunction<? super Atom> ATOMS_LAMBDA = atom -> 1;
    private static final ToIntFunction<? super Atom> MULTIPLE_BONDS_LAMBDA = atom -> atom.hasMultipleBond ? 1 : 0;

    /**
     * Spocita uhliky obsahujici mezi ligandy zacatky uhlikovym cyklu.
     * @param carbon Prochazene uhliky pochazeji z pole ligandu tohoto atomu.
     * @return Pocet takovych profiltrovanych uhliku.
     */
    private static int countCarbonsInCycle(Atom carbon) {
        return (int) Arrays.stream(carbon.ligands)
                .filter(ligand -> ligand != null && ligand.symbol == 'C' && ligand.isPartOfCycle)
                .count();
    }

    /**
     * Funkce bere mnoziny potencialnich cest rozdelenych na cyklicke, uplne a castecne, kdy pro uplnou cetu je potreba 2 spojit.
     * @return Cestu reprezentujici hlavni retezec molekuly podle IUPAC kriterii.
     */
    public List<Atom> filter() {
        int maxNumSeniorGroupsCycle = getMaxNumOf(cyclePaths, SENIOR_GROUPS_LAMBDA);
        int maxNumSeniorGroupsComplete = getMaxNumOf(completePaths, SENIOR_GROUPS_LAMBDA);
        int[] maxNumSeniorGroupsPartialArray = getMaxNumOf(partialPaths, SENIOR_GROUPS_LAMBDA);
        int maxNumSeniorGroupsPartial = Arrays.stream(maxNumSeniorGroupsPartialArray).sum();
        if (maxNumSeniorGroupsPartial != 0) maxNumSeniorGroupsPartial--;

        // Pokud aspon jedna mnozina obsahuje aspon jednu cestu s funkcni skupinou.
        if (!(maxNumSeniorGroupsCycle == 0 && maxNumSeniorGroupsComplete == 0 && maxNumSeniorGroupsPartial == 0)) {
            // V pripade shody s linearnimi cestami, cyklicke maji prednost.
            if (maxNumSeniorGroupsCycle >= Math.max(maxNumSeniorGroupsComplete, maxNumSeniorGroupsPartial)) {
                chain.isCycle = true;
                filteredPathsFromCompleteOrCycles = filterPathsWithMost(cyclePaths, maxNumSeniorGroupsCycle, SENIOR_GROUPS_LAMBDA);
            } else if (maxNumSeniorGroupsComplete > maxNumSeniorGroupsPartial) {
                filteredPathsFromCompleteOrCycles = filterPathsWithMost(completePaths, maxNumSeniorGroupsComplete, SENIOR_GROUPS_LAMBDA);
            } else if (maxNumSeniorGroupsComplete == maxNumSeniorGroupsPartial) {
                filteredPathsFromCompleteOrCycles = filterPathsWithMost(completePaths, maxNumSeniorGroupsComplete, SENIOR_GROUPS_LAMBDA);
                filteredPathsFromPartial = filterPathsWithMost(partialPaths, maxNumSeniorGroupsPartialArray, filteredPathsFromCompleteOrCycles, SENIOR_GROUPS_LAMBDA);
            } else {
                filteredPathsFromPartial = filterPathsWithMost(partialPaths, maxNumSeniorGroupsPartialArray, filteredPathsFromCompleteOrCycles, SENIOR_GROUPS_LAMBDA);
            }
        } else {
            // Dalsim kriteriem po funkcnich skupinach je cyklicnost.
            if (!cyclePaths.isEmpty()) {
                chain.isCycle = true;
                filteredPathsFromCompleteOrCycles = cyclePaths;
            } else {
                filteredPathsFromCompleteOrCycles = completePaths;
                filteredPathsFromPartial = partialPaths;
            }
        }

        // Z linearnich cest vybereme tu, ktera obsahuje vice atomu, z kterych vychazi cyklus.
        if (!chain.isCycle) selectPathsFromCompleteOrCycleOrFromPartial(CARBONS_IN_CYCLE_LAMBDA);
        // Pote ty nejdelsi.
        selectPathsFromCompleteOrCycleOrFromPartial(ATOMS_LAMBDA);
        // Pote ty, co obsahuji nejvice nasobnych vazeb.
        selectPathsFromCompleteOrCycleOrFromPartial(MULTIPLE_BONDS_LAMBDA);

        //Pokud zbyly rozlozene cesty, tak z nich pro kazdy atom vytvorime uplnou cestu, kterou presuneme do mnozin vyslednych cest.
        if (!filteredPathsFromPartial.isEmpty()) {
            if (!filteredPathsFromPartial.values().isEmpty()) {
                Atom key = filteredPathsFromPartial.keySet().iterator().next();
                List<List<Atom>> value = filteredPathsFromPartial.get(key);
                List<List<Atom>> pathToBeConnected = new ArrayList<>();
                pathToBeConnected.add(value.getFirst());
                pathToBeConnected.add(value.getLast());
                makePartialPathsComplete(filteredPathsFromPartial, filteredPathsFromCompleteOrCycles, pathToBeConnected, key);
            }
        }
        return filteredPathsFromCompleteOrCycles.getFirst();
    }

    /**
     * Zredukuje tri puvodni mnoziny nejvice na dve, v pripade, ze cykly mene odpovidaji kriteriim nez linearni uplne cesty, ktere odpovidaji doposavad
     * stejnym parametrum jako linearni castecne. Pokud ne, tak zbyde pouze jedna mnozina obsahujici cyklicke cesty.
     * @param lambda Uvazovany filtr.
     */
    private void selectPathsFromCompleteOrCycleOrFromPartial(ToIntFunction<? super Atom> lambda) {
        int maxNumComplete = getMaxNumOf(filteredPathsFromCompleteOrCycles, lambda);
        int[] maxNumPartialArray = getMaxNumOf(filteredPathsFromPartial, lambda);
        int maxNumPartial = Arrays.stream(maxNumPartialArray).sum();
        if (maxNumComplete > maxNumPartial) {
            filteredPathsFromPartial.clear();
        } else if (maxNumPartial > maxNumComplete)
            filteredPathsFromCompleteOrCycles.clear();
        filteredPathsFromCompleteOrCycles = filterPathsWithMost(filteredPathsFromCompleteOrCycles, maxNumComplete, lambda);
        filteredPathsFromPartial = filterPathsWithMost(filteredPathsFromPartial, maxNumPartialArray, filteredPathsFromCompleteOrCycles, lambda);
    }

    /**
     * Z mnoziny uplnych cest (linearnich nebo cyklickych )ziska maximalni pocet profiltrovanych uhliku v nejake ceste.
     * @param paths Uvazovana mnozina.
     * @param lambda Uvazovany filtr.
     * @return Maximalni pocet profiltrovanych uhliku.
     */
    private int getMaxNumOf(List<List<Atom>> paths, ToIntFunction<? super Atom> lambda) {
        if (paths == null) return 0;

        return paths.parallelStream()
                .mapToInt(path -> path.parallelStream()
                        .mapToInt(lambda).sum())
                .max().orElse(0);
    }

    /**
     * Z mnoziny castecnych cest ziska maximalni pocet profiltrovanych uhliku v nejake ceste.
     * @param partialPaths Mnozina castecnych cest.
     * @param lambda Uvazovany filtr.
     * @return Maximalni pocet profiltrovanych uhliku.
     */
    private int[] getMaxNumOf(Map<Atom, List<List<Atom>>> partialPaths, ToIntFunction<? super Atom> lambda) {
        if (partialPaths == null) return new int[]{0, 0};

        // Pokud pouze jedna cesta obsahuje max pocet, jako druhou vybereme tu s druhym maximalnim poctem.
        int max1 = Integer.MIN_VALUE;
        int max2 = Integer.MIN_VALUE;

        for (Map.Entry<Atom, List<List<Atom>>> entry : partialPaths.entrySet()) {
            List<List<Atom>> paths = entry.getValue();
            for (List<Atom> path : paths) {
                int num = path.parallelStream().mapToInt(lambda).sum();

                if (num >= max1) {
                    max2 = max1;
                    max1 = num;
                } else if (num > max2) {
                    max2 = num;
                }
            }
        }
        return new int[]{max1, max2};
    }

    /**
     * Z mnoziny uplnych cest (linearnich nebo cyklickych) vyfiltruje ty, ktere obsahuji dany pocet uhliku s danou strukturou.
     * @param paths Uvazovana mnozina.
     * @param max Maximalni pocet uhliku s danou strukturou.
     * @param lambda Uvazovany filtr.
     * @return Vyfiltrovane cesty.
     */
    private List<List<Atom>> filterPathsWithMost(List<List<Atom>> paths, int max, ToIntFunction<? super Atom> lambda) {
        if (paths == null) return null;

        return paths.parallelStream()
                .filter(path -> path.parallelStream()
                        .mapToInt(lambda).sum() == max)
                .collect(Collectors.toList());
    }

    /**
     * Pokud v prubehu filtrovani jsou u daneho atomu pouze
     * dve cesty, spojime je a presuneme do mnoziny uplnych cest.
     * @param partialPaths Mnozina castecnych cest.
     * @param completePaths  Mnozina uplnych cest.
     * @param filteredPaths Mnozina vyfiltrovanych cest.
     * @param atom Atom, jehoz castecne cesty prochazime.
     */
    private  void makePartialPathsComplete(Map<Atom, List<List<Atom>>> partialPaths, List<List<Atom>> completePaths, List<List<Atom>> filteredPaths, Atom atom) {
        if (filteredPaths.size() == 2) {
            List<Atom> startPath = filteredPaths.getFirst();
            List<Atom> endPath = filteredPaths.getLast();
            endPath = endPath.reversed();
            endPath.removeFirst();

            startPath.addAll(endPath);
            completePaths.add(startPath);

            partialPaths.remove(atom);
        }
    }

    /**
     * Z mnoziny castecnych cest vyfiltruje ty, ktere obsahuji dany pocet uhliku s danou strukturou.
     * @param partialPaths Mnozina castecnych cest.
     * @param maxSeniorGroupsArray Seznam atomu obsahuji zkoumanou strukturu.
     * @param completePaths Mnozina uplnych cest.
     * @param lambda Uvazovany filtr.
     * @return
     */
    private Map<Atom, List<List<Atom>>> filterPathsWithMost(Map<Atom, List<List<Atom>>> partialPaths, int[] maxSeniorGroupsArray, List<List<Atom>> completePaths, ToIntFunction<? super Atom> lambda) {
        if (partialPaths == null) return null;

        for (Map.Entry<Atom, List<List<Atom>>> entry : partialPaths.entrySet()) {
            List<List<Atom>> pathsWithMostSeniorGroups = filterPathsWithMost(entry.getValue(), maxSeniorGroupsArray[0], lambda);
            List<List<Atom>> pathsWithSecondMostSeniorGroups = new ArrayList<>();

            if (pathsWithMostSeniorGroups.size() == 1) {
                pathsWithSecondMostSeniorGroups = entry.getValue();
                pathsWithSecondMostSeniorGroups.removeAll(pathsWithMostSeniorGroups);
                pathsWithSecondMostSeniorGroups = filterPathsWithMost(pathsWithSecondMostSeniorGroups, maxSeniorGroupsArray[1], lambda);
            }
            pathsWithMostSeniorGroups.addAll(pathsWithSecondMostSeniorGroups);
            partialPaths.put(entry.getKey(), pathsWithMostSeniorGroups);

            makePartialPathsComplete(partialPaths, completePaths, pathsWithMostSeniorGroups, entry.getKey());
        }
        return partialPaths;
    }
}