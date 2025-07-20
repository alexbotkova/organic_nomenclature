package cz.cuni.mff.java.botkova.nomenclature;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class ChainTest {

    @ParameterizedTest
    @CsvSource({
            "'C', '[C-0]'",
            "'C(C)C(=O)(O)', '[C-1, C-0, C-2]'",
            "'C(N)CCC(O)(=O)', '[C-0, C-2, C-3, C-4]'",
            "'C(=O)(O)CC(=O)(O)', '[C-0, C-3, C-4]'",
            "'CCC', '[C-0, C-1, C-2]'",
            "'CC(C(C)CC)CCCCCC', '[C-4, C-3, C-5, C-1, C-6, C-7, C-8, C-9, C-10, C-11]'",
            "'C1CC1', '[C-0, C-1, C-2]'",
            "'C1C(O)C1','[C-1, C-0, C-3]'",
            "'C1C(O)C1C','[C-1, C-0, C-3]'",
            "'CC1C(O)CCCC1C', '[C-2, C-1, C-7, C-6, C-5, C-4]'",
            "'CC1C(O)CC(CC(C)CC)CC1C', '[C-2, C-1, C-12, C-11, C-5, C-4]'",
            // 3
            "'CC(C(O)(C)C)C', '[C-0, C-1, C-5, C-4]'",
            // 1
            "'CC(O)CC1C(O)C1', '[C-5, C-4, C-7]'",
            // 2
            "'CC(O)CC1CC1', '[C-0, C-1, C-3]'",
            "'C=CC', '[C-0, C-1, C-2]'",
            "'C=C=C', '[C-0, C-1, C-2]'",
            // 4
            "'C=C(CCCCCCC)C=C', '[C-7, C-6, C-5, C-4, C-3, C-2, C-8, C-1, C-9, C-10]'",
            "'C=C(CC)C=C', '[C-2, C-3, C-1, C-4, C-5]'",
            "'C=C(CC)C=C', '[C-2, C-3, C-1, C-4, C-5]'",
            // 5
            "'CC(CC)C=C', '[C-2, C-3, C-1, C-4, C-5]'",
            // 6
            "'CCC(CC)C=C', '[C-0, C-1, C-2, C-5, C-6]'",
            // 7
            "'CCCC(CC)C=C', '[C-0, C-1, C-2, C-3, C-6, C-7]'",
            "'CC=O','[C-0, C-1]'"

    })
    void getPath(String smiles, String expected) throws SMILESParser.InvalidSmilesException, Atom.LigancyExceededException, Molecule.InvalidLigandConfigurationException {
        Molecule molecule = SMILESParser.parseMolecule(smiles);
        molecule.identifyStructures();
        Chain chain = new Chain(molecule);
        assertEquals(expected, chain.atoms.toString());
    }
}