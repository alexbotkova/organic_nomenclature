package cz.cuni.mff.java.botkova.nomenclature;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MoleculeTest {

    static Stream<Arguments> objectProvider() throws SMILESParser.InvalidSmilesException, Atom.LigancyExceededException, Molecule.InvalidLigandConfigurationException {
        Molecule m1 = SMILESParser.parseMolecule("CO");
        m1.identifyStructures();
        Molecule m2 = SMILESParser.parseMolecule("C=O");
        m2.identifyStructures();
        Molecule m3 = SMILESParser.parseMolecule("C(=O)(O)");
        m3.identifyStructures();
        Molecule m4 = SMILESParser.parseMolecule("C(O)(C)");
        m4.identifyStructures();
        Molecule m5 = SMILESParser.parseMolecule("C(=O)(C)");
        m5.identifyStructures();
        Molecule m6 = SMILESParser.parseMolecule("C(=O)(O)CC(=O)(O)");
        m6.identifyStructures();
        Molecule m7 = SMILESParser.parseMolecule("CCCC");
        m7.identifyStructures();
        Molecule m8 = SMILESParser.parseMolecule("C(N)CCC(O)(=O)");
        m8.identifyStructures();
        return Stream.of(
                Arguments.of("{-OH=[C-0]}", m1.atomsWithSeniorGroups.toString()),
                Arguments.of("{-CHO=[C-0]}", m2.atomsWithSeniorGroups.toString()),
                Arguments.of("{-COOH=[C-0]}", m3.atomsWithSeniorGroups.toString()),
                Arguments.of("{-OH=[C-0]}", m4.atomsWithSeniorGroups.toString()),
                Arguments.of("{-CHO=[C-0]}", m5.atomsWithSeniorGroups.toString()),
                Arguments.of("{-COOH=[C-4, C-0]}", m6.atomsWithSeniorGroups.toString()),
                Arguments.of("{}", m7.atomsWithSeniorGroups.toString()),
                Arguments.of("{-COOH=[C-4], -NH2=[C-0]}", m8.atomsWithSeniorGroups.toString())
        );
    }

    @ParameterizedTest
    @MethodSource("objectProvider")
    void identifySeniorGroups(String expected, String actual) {
        assertEquals(expected, actual);
    }

    static Stream<Arguments> objectProvider2() throws SMILESParser.InvalidSmilesException, Atom.LigancyExceededException, Molecule.InvalidLigandConfigurationException {
        Molecule m1 = SMILESParser.parseMolecule("CO");

        m1.identifyStructures();
        Molecule m2 = SMILESParser.parseMolecule("C=O");
        m2.identifyStructures();
        Molecule m3 = SMILESParser.parseMolecule("C(=O)(O)");
        m3.identifyStructures();
        Molecule m4 = SMILESParser.parseMolecule("C(O)(C)");
        m4.identifyStructures();
        Molecule m5 = SMILESParser.parseMolecule("C(=O)(C)");
        m5.identifyStructures();
        Molecule m6 = SMILESParser.parseMolecule("C(=O)(O)CC(=O)(O)");
        m6.identifyStructures();
        Molecule m7 = SMILESParser.parseMolecule("CCCC");
        m7.identifyStructures();
        Molecule m8 = SMILESParser.parseMolecule("C(N)CCC(O)(=O)");
        m8.identifyStructures();
        return Stream.of(
                Arguments.of("-OH", m1.mostSeniorGroup),
                Arguments.of("-CHO", m2.mostSeniorGroup),
                Arguments.of("-COOH", m3.mostSeniorGroup),
                Arguments.of("-OH", m4.mostSeniorGroup),
                Arguments.of("-CHO", m5.mostSeniorGroup),
                Arguments.of("-COOH", m6.mostSeniorGroup),
                Arguments.of(null, m7.mostSeniorGroup),
                Arguments.of("-COOH", m8.mostSeniorGroup)
        );
    }

    @ParameterizedTest
    @MethodSource("objectProvider2")
    void getMostSeniorGroup(String expected, String actual) {
        assertEquals(expected, actual);
    }

    static Stream<Arguments> objectProvider3() throws SMILESParser.InvalidSmilesException, Atom.LigancyExceededException, Molecule.InvalidLigandConfigurationException {
        Molecule m1 = SMILESParser.parseMolecule("C=CC=CC=C");

        m1.identifyStructures();
        Molecule m2 = SMILESParser.parseMolecule("C=C(C(C)C)C=C");
        m2.identifyStructures();
        Molecule m3 = SMILESParser.parseMolecule("C#C");
        m3.identifyStructures();

        return Stream.of(
                Arguments.of("[C-1, C-3, C-5, C-4, C-2, C-0]", m1.atomsWithMultipleBonds.toString()),
                Arguments.of("[C-1, C-6, C-5, C-0]", m2.atomsWithMultipleBonds.toString()),
                Arguments.of("[C-1, C-0]", m3.atomsWithMultipleBonds.toString())
        );
    }

    @ParameterizedTest
    @MethodSource("objectProvider3")
    void getAtomsWithMultipleBonds(String expected, String actual) {
        assertEquals(expected, actual);
    }
}

