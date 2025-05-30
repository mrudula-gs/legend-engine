// Copyright 2022 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.persistence.components.ingestmode.unitemporal;

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.TestUtils;
import org.finos.legend.engine.persistence.components.common.DatasetFilter;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.FilterType;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FailOnDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchId;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.AllVersionsStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.DigestBasedResolver;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.VersionColumnBasedResolver;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.VersionComparator;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DerivedDataset;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.finos.legend.engine.persistence.components.relational.api.DataSplitRange;
import org.finos.legend.engine.persistence.components.versioning.TestDedupAndVersioning;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.finos.legend.engine.persistence.components.TestUtils.*;
import static org.finos.legend.engine.persistence.components.TestUtils.versionName;

class UnitemporalDeltaWithBatchIdTest extends BaseTest
{
    private final String basePathForInput = "src/test/resources/data/unitemporal-incremental-milestoning/input/batch_id_based/";
    private final String basePathForExpected = "src/test/resources/data/unitemporal-incremental-milestoning/expected/batch_id_based/";

    /*
    Scenario: Test milestoning Logic when staging table pre populated
    */
    @Test
    void testMilestoning() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTableWithoutPks(stagingTable);

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "without_delete_ind/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "without_delete_ind/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 3);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "without_delete_ind/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "without_delete_ind/expected_pass2.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(6, 0, 1, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);

        // ------------ Perform Pass3 empty batch (No Impact) -------------------------
        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "without_delete_ind/expected_pass3.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic when staging data comes from external Json
    */
    @Test
    void testMilestoningWithExternalJsonData() throws Exception
    {

        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        String dataPass1 = basePathForInput + "without_delete_ind/staging_data_pass1.json";
        Dataset stagingTable = TestUtils.getBasicJsonDatasetReferenceTable(dataPass1);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdInName, batchIdOutName};

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String expectedDataPass1 = basePathForExpected + "without_delete_ind/expected_pass1.csv";
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "without_delete_ind/staging_data_pass2.json";
        stagingTable = TestUtils.getBasicJsonDatasetReferenceTable(dataPass2);
        String expectedDataPass2 = basePathForExpected + "without_delete_ind/expected_pass2.csv";
        expectedStats = createExpectedStatsMap(3, 0, 1, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets.withStagingDataset(stagingTable), schema, expectedDataPass2, expectedStats);

        // ------------ Perform Pass3 empty batch (No Impact) -------------------------
        String dataPass3 = basePathForInput + "without_delete_ind/staging_data_pass3.json";
        stagingTable = TestUtils.getBasicJsonDatasetReferenceTable(dataPass3);
        String expectedDataPass3 = basePathForExpected + "without_delete_ind/expected_pass3.csv";
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets.withStagingDataset(stagingTable), schema, expectedDataPass3, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic when staging table pre populated and delete indicator is present
    */
    @Test
    void testMilestoningWithDeleteIndicator() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithDeleteIndicator();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTable(stagingTable);

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                .deleteField(deleteIndicatorName)
                .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "with_delete_ind/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_delete_ind/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithDeleteInd(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(4, 0, 4, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "with_delete_ind/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "with_delete_ind/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithDeleteInd(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(5, 0, 1, 1, 2);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);

        // ------------ Perform Pass3 empty batch (No Impact) -------------------------
        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "with_delete_ind/expected_pass3.csv";
        // 1. Load staging table
        loadStagingDataWithDeleteInd(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic when staging data comes from CSV and has less columns than main dataset
    */
    @Test
    void testMilestoningWithLessColumnsInStaging() throws Exception
    {

        DatasetDefinition mainTable = TestUtils.getUnitemporalIdBasedMainTable();
        String dataPass1 = basePathForInput + "less_columns_in_staging/staging_data_pass1.csv";
        Dataset stagingTable = TestUtils.getCsvDatasetRefWithLessColumnsThanMain(dataPass1);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdInName, batchIdOutName};

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String expectedDataPass1 = basePathForExpected + "less_columns_in_staging/expected_pass1.csv";
        // Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "less_columns_in_staging/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "less_columns_in_staging/expected_pass2.csv";
        stagingTable = TestUtils.getCsvDatasetRefWithLessColumnsThanMain(dataPass2);

        // Execute plans and verify results
        expectedStats = createExpectedStatsMap(3, 0, 1, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets.withStagingDataset(stagingTable), schema, expectedDataPass2, expectedStats);

        // ------------ Perform Pass3 empty batch (No Impact) -------------------------
        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "less_columns_in_staging/expected_pass3.csv";
        stagingTable = TestUtils.getCsvDatasetRefWithLessColumnsThanMain(dataPass3);

        // Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets.withStagingDataset(stagingTable), schema, expectedDataPass3, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic when staging table is pre populated and
    staging table is cleaned up in the end
    */
    @Test
    void testMilestoningWithDeleteIndicatorWithCleanStagingData() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getUnitemporalIdBasedMainTable();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithDeleteIndicator();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTable(stagingTable);

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                .deleteField(deleteIndicatorName)
                .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(true).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass With Clean Staging Table ------------------------
        String dataPass1 = basePathForInput + "with_delete_ind/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_delete_ind/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithDeleteInd(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(4, 0, 4, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);
        // 3. Assert that the staging table is truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 0);
    }

    @Test
    void testMilestoningAllVersionWithoutPerform() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        String dataPass1 = basePathForInput + "with_data_splits/staging_data_pass1.csv";
        Dataset stagingTable = TestUtils.getBasicCsvDatasetReferenceTableWithDataSplits(dataPass1);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdInName, batchIdOutName};

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestName)
                .versioningStrategy(AllVersionsStrategy.builder()
                        .dataSplitFieldName(dataSplitName)
                        .versioningField(expiryDateName)
                        .mergeDataVersionResolver(DigestBasedResolver.INSTANCE)
                        .performStageVersioning(false).build())
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .build())
                .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform milestoning Pass1 ------------------------
        String expectedDataPass1 = basePathForExpected + "with_data_splits/expected_pass1.csv";
        // Execute plans and verify results
        List<DataSplitRange> dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(1, 1));
        dataSplitRanges.add(DataSplitRange.of(2, 2));

        List<Map<String, Object>> expectedStatsList = new ArrayList<>();
        Map<String, Object> expectedStatsSplit1 = createExpectedStatsMap(4, 0, 4, 0, 0);
        Map<String, Object> expectedStatsSplit2 = createExpectedStatsMap(2, 0, 0, 2, 0);

        expectedStatsList.add(expectedStatsSplit1);
        expectedStatsList.add(expectedStatsSplit2);

        executePlansAndVerifyResultsWithSpecifiedDataSplits(ingestMode, options, datasets, schema, expectedDataPass1, expectedStatsList, dataSplitRanges);

        // ------------ Perform milestoning Pass2 ------------------------
        ingestMode = ingestMode.withDeduplicationStrategy(FilterDuplicates.builder().build());
        String dataPass2 = basePathForInput + "with_data_splits/staging_data_pass2.csv";
        stagingTable = TestUtils.getBasicCsvDatasetReferenceTableWithDataSplits(dataPass2);
        String expectedDataPass2 = basePathForExpected + "with_data_splits/expected_pass2.csv";
        // Execute plans and verify results
        dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(1, 1));
        dataSplitRanges.add(DataSplitRange.of(2, 2));
        dataSplitRanges.add(DataSplitRange.of(3, 3));

        expectedStatsList = new ArrayList<>();
        expectedStatsList.add(createExpectedStatsMap(4, 0, 1, 1, 0));
        expectedStatsList.add(createExpectedStatsMap(1, 0, 0, 1, 0));
        expectedStatsList.add(createExpectedStatsMap(1, 0, 0, 1, 0));

        executePlansAndVerifyResultsWithSpecifiedDataSplits(ingestMode, options, Datasets.of(mainTable, stagingTable), schema, expectedDataPass2, expectedStatsList, dataSplitRanges);

        // ------------ Perform milestoning Pass3 - Empty batch ------------------------
        String dataPass3 = "src/test/resources/data/empty_file.csv";
        stagingTable = TestUtils.getBasicCsvDatasetReferenceTableWithDataSplits(dataPass3);
        String expectedDataPass3 = basePathForExpected + "with_data_splits/expected_pass3.csv";
        // Execute plans and verify results
        dataSplitRanges = new ArrayList<>();
        expectedStatsList = new ArrayList<>();
        expectedStatsList.add(createExpectedStatsMap(0, 0, 0, 0, 0));
        executePlansAndVerifyResultsWithSpecifiedDataSplits(ingestMode, options, Datasets.of(mainTable, stagingTable), schema, expectedDataPass3, expectedStatsList, dataSplitRanges);
    }

    @Test
    void testUniTemporalDeltaWithAllVersionGreaterThanAndStagingFilters() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingDataset = DatasetDefinition.builder()
                .group(testSchemaName)
                .name(stagingTableName)
                .schema(TestDedupAndVersioning.baseSchemaWithVersionAndBatchWithoutDigest)
                .build();

        createStagingTableWithoutPks(stagingDataset);
        DerivedDataset stagingTable = DerivedDataset.builder()
                .group(testSchemaName)
                .name(stagingTableName)
                .schema(TestDedupAndVersioning.baseSchemaWithVersionWithoutDigest)
                .addDatasetFilters(DatasetFilter.of("batch", FilterType.EQUAL_TO, 1))
                .build();
        String path = basePathForInput + "with_all_version/data1_without_digest.csv";
        TestDedupAndVersioning.loadDataIntoStagingTableWithVersionAndBatchWithoutDigest(path);

        // Generate the milestoning object
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .versioningStrategy(AllVersionsStrategy.builder()
                        .versioningField(versionName)
                        .mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN))
                        .performStageVersioning(true)
                        .build())
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .build())
                .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, versionName, incomeName, expiryDateName};

        // ------------ Perform milestoning Pass1 ------------------------
        String expectedDataPass1 = basePathForExpected + "with_all_version/greater_than/expected_pass1.csv";
        // 2. Execute plans and verify results
        List<Map<String, Object>> expectedStatsList = new ArrayList<>();
        Map<String, Object> expectedStats1  = createExpectedStatsMap(3,0,3,0,0);
        Map<String, Object> expectedStats2  = createExpectedStatsMap(2,0,0,2,0);
        Map<String, Object> expectedStats3  = createExpectedStatsMap(1,0,0,1,0);
        expectedStatsList.add(expectedStats1);
        expectedStatsList.add(expectedStats2);
        expectedStatsList.add(expectedStats3);
        executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass1, expectedStatsList, fixedClock_2000_01_01);

        // ------------ Perform milestoning Pass2 Fail on Duplicates ------------------------
        ingestMode = ingestMode.withDeduplicationStrategy(FailOnDuplicates.builder().build());
        stagingTable = stagingTable.withDatasetFilters(DatasetFilter.of("batch", FilterType.EQUAL_TO, 2));
        datasets = Datasets.of(mainTable, stagingTable);
        try
        {
            executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass1, expectedStatsList, fixedClock_2000_01_01);
            Assertions.fail("Should not succeed");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Encountered Duplicates, Failing the batch as Fail on Duplicates is set as Deduplication strategy", e.getMessage());
        }

        // ------------ Perform milestoning Pass2 Filter Duplicates ------------------------
        String expectedDataPass2 = basePathForExpected + "with_all_version/greater_than/expected_pass2.csv";
        expectedStatsList = new ArrayList<>();
        Map<String, Object> expectedStats4  = createExpectedStatsMap(4,0,1,0,0);
        Map<String, Object> expectedStats5  = createExpectedStatsMap(2,0,0,2,0);
        expectedStatsList.add(expectedStats4);
        expectedStatsList.add(expectedStats5);

        ingestMode = ingestMode.withDeduplicationStrategy(FilterDuplicates.builder().build());
        stagingTable = stagingTable.withDatasetFilters(DatasetFilter.of("batch", FilterType.EQUAL_TO, 2));
        datasets = Datasets.of(mainTable, stagingTable);
        executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass2, expectedStatsList, fixedClock_2000_01_01);

        // ------------ Perform milestoning Pass3 Data Error ------------------------
        stagingTable = stagingTable.withDatasetFilters(DatasetFilter.of("batch", FilterType.EQUAL_TO, 3));
        datasets = Datasets.of(mainTable, stagingTable);

        try
        {
            executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass2, expectedStatsList, fixedClock_2000_01_01);
            Assertions.fail("Should not succeed");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Encountered Data errors (same PK, same version but different data), hence failing the batch", e.getMessage());
        }
    }

    @Test
    void testUniTemporalDeltaWithAllVersionDigestBasedAndStagingFilters() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingDataset = DatasetDefinition.builder()
                .group(testSchemaName)
                .name(stagingTableName)
                .schema(TestDedupAndVersioning.baseSchemaWithVersionAndBatch)
                .build();

        createStagingTableWithoutPks(stagingDataset);
        DerivedDataset stagingTable = DerivedDataset.builder()
                .group(testSchemaName)
                .name(stagingTableName)
                .schema(TestDedupAndVersioning.baseSchemaWithVersion)
                .addDatasetFilters(DatasetFilter.of("batch", FilterType.EQUAL_TO, 1))
                .build();
        String path = basePathForInput + "with_all_version/data1.csv";
        TestDedupAndVersioning.loadDataIntoStagingTableWithVersionAndBatch(path);

        // Generate the milestoning object
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestName)
                .versioningStrategy(AllVersionsStrategy.builder()
                        .versioningField(versionName)
                        .mergeDataVersionResolver(DigestBasedResolver.INSTANCE)
                        .performStageVersioning(true)
                        .build())
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .build())
                .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, versionName, incomeName, expiryDateName, digestName};

        // ------------ Perform milestoning Pass1 ------------------------
        String expectedDataPass1 = basePathForExpected + "with_all_version/digest_based/expected_pass1.csv";
        // 2. Execute plans and verify results
        List<Map<String, Object>> expectedStatsList = new ArrayList<>();
        Map<String, Object> expectedStats1  = createExpectedStatsMap(3,0,3,0,0);
        Map<String, Object> expectedStats2  = createExpectedStatsMap(2,0,0,2,0);
        Map<String, Object> expectedStats3  = createExpectedStatsMap(1,0,0,1,0);
        expectedStatsList.add(expectedStats1);
        expectedStatsList.add(expectedStats2);
        expectedStatsList.add(expectedStats3);
        executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass1, expectedStatsList, fixedClock_2000_01_01);

        // ------------ Perform milestoning Pass2 Fail on Duplicates ------------------------
        ingestMode = ingestMode.withDeduplicationStrategy(FailOnDuplicates.builder().build());
        stagingTable = stagingTable.withDatasetFilters(DatasetFilter.of("batch", FilterType.EQUAL_TO, 2));
        datasets = Datasets.of(mainTable, stagingTable);
        try
        {
            executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass1, expectedStatsList, fixedClock_2000_01_01);
            Assertions.fail("Should not succeed");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Encountered Duplicates, Failing the batch as Fail on Duplicates is set as Deduplication strategy", e.getMessage());
        }

        // ------------ Perform milestoning Pass2 Filter Duplicates ------------------------
        String expectedDataPass2 = basePathForExpected + "with_all_version/digest_based/expected_pass2.csv";
        expectedStatsList = new ArrayList<>();
        Map<String, Object> expectedStats4  = createExpectedStatsMap(4,0,1,1,0);
        Map<String, Object> expectedStats5  = createExpectedStatsMap(2,0,0,2,0);
        expectedStatsList.add(expectedStats4);
        expectedStatsList.add(expectedStats5);

        ingestMode = ingestMode.withDeduplicationStrategy(FilterDuplicates.builder().build());
        stagingTable = stagingTable.withDatasetFilters(DatasetFilter.of("batch", FilterType.EQUAL_TO, 2));
        datasets = Datasets.of(mainTable, stagingTable);
        executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass2, expectedStatsList, fixedClock_2000_01_01);

        // ------------ Perform milestoning Pass3 Data Error ------------------------
        stagingTable = stagingTable.withDatasetFilters(DatasetFilter.of("batch", FilterType.EQUAL_TO, 3));
        datasets = Datasets.of(mainTable, stagingTable);

        try
        {
            executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass2, expectedStatsList, fixedClock_2000_01_01);
            Assertions.fail("Should not succeed");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Encountered Data errors (same PK, same version but different data), hence failing the batch", e.getMessage());
        }
    }

}
