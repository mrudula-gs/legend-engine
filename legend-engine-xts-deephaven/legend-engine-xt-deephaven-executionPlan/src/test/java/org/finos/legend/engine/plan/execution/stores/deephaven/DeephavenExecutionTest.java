package org.finos.legend.engine.plan.execution.stores.deephaven;

import org.eclipse.microprofile.openapi.models.Paths;

import org.junit.Test;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class DeephavenExecutionTest
{
    private final Path testGrammarPath = Paths.of("/engineGrammar._pure_");
    //private final String testDataPath;

    @BeforeClass
    public static void beforeClass() throws Exception
    {
        // set up the deephaven instance with testdata

    }

    @AfterClass
    public static void afterClass() throws Exception
    {
        // clean up connections here
    }

    @Test
    public void deephavenSelectWhere() throws IOException
    {
        String testGrammar = Files.lines(this.testGrammarPath).collect(Collectors.joining("\n"));
        //PureModelContextData pmcd = PureGrammarParser.newInstance().parseModel()
    }
}
