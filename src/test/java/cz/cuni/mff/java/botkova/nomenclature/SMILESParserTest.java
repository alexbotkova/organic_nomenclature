package cz.cuni.mff.java.botkova.nomenclature;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class SMILESParserTest {

    @ParameterizedTest
    @CsvSource({
            // alkany
            "'C', 'CH4'",
            "'C(C)C', 'CH2(CH3)(CH3)'",
            "'C(CCCC(CC(C(C)C)CC)C)C', 'CH2(CH2(CH2(CH2(CH1(CH2(CH1(CH1(CH3)(CH3))(CH2(CH3))))(CH3)))))(CH3)'",
            // alkeny
            "'C=C', 'CH2(CH2)'",
            "'C=CC=CC=C', 'CH2(CH1(CH1(CH1(CH1(CH2)))))'",
            "'C=C(C(C)C)C=C', 'CH2(C(CH1(CH3)(CH3))(CH1(CH2)))'",
            // alkyny
            "'C#C', 'CH1(CH1)'",
            "'CC#CC=C', 'CH3(C(C(CH1(CH2))))'",
            "'C#CC(C=C(C)C)CC#C', 'CH1(C(CH1(CH1(C(CH3)(CH3)))(CH2(C(CH1)))))'",
            // derivaty
            "'C=O', 'CH2(O)'",
            "'CO', 'CH3(OH1)'",
            "'CN', 'CH3(NH2)'",
            "'C(CCCC(CC(C(O)C)CC)C)C', 'CH2(CH2(CH2(CH2(CH1(CH2(CH1(CH1(OH1)(CH3))(CH2(CH3))))(CH3)))))(CH3)'",
            "'C(=O)(O)', 'CH1(O)(OH1)'",
            "'CC(=O)(O)', 'CH3(C(O)(OH1))'",
            "'C(=O)(O)CC(=O)(O)', 'C(O)(OH1)(CH2(C(O)(OH1)))'",
    })
    void parseMolecule(String smiles, String expectedMolecularRepresentation) throws SMILESParser.InvalidSmilesException, Atom.LigancyExceededException {
        Molecule molecule = SMILESParser.parseMolecule(smiles);
        String moleculeRepresentation = molecule.DFS();
        assertEquals(expectedMolecularRepresentation, moleculeRepresentation);
    }
}