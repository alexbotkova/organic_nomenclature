package cz.cuni.mff.java.botkova.nomenclature;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class NomenclatureTest {

    @ParameterizedTest
    @CsvSource({
            "'C', 'methan'",
            "'CCCCCC', 'hexan'",
            "'CCC(CC)CCC', '3-(ethyl)-hexan'",
            "'CCC(CC)(CC)CCC', '3,3-di(ethyl)-hexan'",
            "'CCC(CC)(C)CCC', '3-(ethyl)-3-(methyl)-hexan'",

            "'C=C', 'eth-1-en'",
            "'C#C', 'eth-1-yn'",
            "'C=C=C', 'prop-1,2-dien'",
            "'C=CC=C', 'but-1,3-dien'",
            "'C=CC#C', 'but-1-en-3-yn'",
            "'C=C=CC#CC#CC=C', 'non-1,2,8-trien-4,6-diyn'",
            "'CCC(CC=C)C', '4-(methyl)-hex-1-en'",
            "'CCC(CC=C)CCC', '4-(ethyl)-hept-1-en'",
            "'CCC(CC=C)(CC=C)CCC', '4-(ethyl)-4-(propyl)-hept-1,6-dien'",


            "'CS', 'methan-1-thiol'",
            "'CS(O)(O)(O)', 'methan-1-sulfonova kyselina'",
            "'CN(O)(O)', '1-nitro-methan'",
            "'CN', 'methan-1-amin'",
            "'C(N)N(O)(O)', '1-nitro-methan-1-amin'",
            "'C=O', 'methan-1-al'",
            "'CC(=O)C', 'propan-2-on'",
            "'C(=O)(O)', 'methan-1-ova kyselina'",
            "'C(O)C(=O)', '2-hydroxy-ethan-1-al'",
            "'C(O)C(O)', 'ethan-1,2-diol'",
            "'C(O)C(=O)(O)', '2-hydroxy-ethan-1-ova kyselina'",
            "'CCC(CC(O)(=O))(CC)CCC', '3,3-di(ethyl)-hexan-1-ova kyselina'",

            "'C1CC1', 'cyklopropan'",
            "'C1C(O)C(O)C1', 'cyklobutan-1,4-diol'",
            "'C1(O)C=CC(O)C1', 'cyklopent-3-en-2,5-diol'",
            "'C1(O)C=CC(CCCO)CC(O)C1', '4-(propyl-3-ol)-cyklohept-5-en-2,7-diol'",
    })
    void getNameTest(String smiles, String expected) throws SMILESParser.InvalidSmilesException, Molecule.InvalidLigandConfigurationException, Atom.LigancyExceededException {
        String actual = Nomenclature.getName(smiles);
        assertEquals(expected, actual);
    }
}