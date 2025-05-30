// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.ingestmode.mixed;

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.TestUtils;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.partitioning.NoPartitioning;
import org.finos.legend.engine.persistence.components.ingestmode.partitioning.Partitioning;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTime;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.*;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.api.optimizers.IngestModeOptimizer;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.Transformer;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.finos.legend.engine.persistence.components.TestUtils.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IngestModeOptimizerTest extends BaseTest
{

    Datasets setupStagingAndMainDatasets() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = DatasetDefinition.builder()
                .group(testSchemaName)
                .name(stagingTableName)
                .schema(SchemaDefinition.builder()
                        .addFields(date)
                        .addFields(entity)
                        .addFields(startTime)
                        .addFields(price)
                        .addFields(volume)
                        .addFields(digest)
                        .build()
                )
                .build();

        // Create staging table
        createStagingTable(stagingTable);
        String path = "src/test/resources/data/unitemporal-snapshot-milestoning/input/partition_optimization/staging_data_pass1.csv";
        loadStagingDataForWithPartition(path);
        Datasets datasets = Datasets.of(mainTable, stagingTable);
        return datasets;
    }

    Datasets setupStagingMainAndDeletePartitionsDatasets() throws Exception
    {
        Datasets datasets = setupStagingAndMainDatasets();

        DatasetDefinition deletePartitionDataset = DatasetDefinition.builder()
                .group(testSchemaName)
                .name(deletePartitionTableName)
                .schema(SchemaDefinition.builder()
                        .addFields(date)
                        .build())
                .build();

        // Create Delete partition table
        createStagingTable(deletePartitionDataset);
        String path = "src/test/resources/data/unitemporal-snapshot-milestoning/input/partition_optimization/delete_partitions1.csv";
        loadDeletePartitionData(path);
        return Datasets.builder()
                .mainDataset(datasets.mainDataset())
                .stagingDataset(datasets.stagingDataset())
                .deletePartitionDataset(deletePartitionDataset).build();
    }

    Datasets setupStagingMainAndDeletePartitionsDatasetsMultiplePartitionsKeys() throws Exception
    {
        return setupStagingMainAndDeletePartitionsDatasetsMultiplePartitionsKeys("src/test/resources/data/unitemporal-snapshot-milestoning/input/partition_optimization/delete_partitions2.csv");
    }

    Datasets setupStagingMainAndLargeDeletePartitionsDatasetsMultiplePartitionsKeys() throws Exception
    {
        return setupStagingMainAndDeletePartitionsDatasetsMultiplePartitionsKeys("src/test/resources/data/unitemporal-snapshot-milestoning/input/partition_optimization/delete_partitions3.csv");
    }

    Datasets setupStagingMainAndDeletePartitionsDatasetsMultiplePartitionsKeys(String deleteFile) throws Exception
    {
        Datasets datasets = setupStagingAndMainDatasets();

        Field date = Field.builder().name(dateName).type(FieldType.of(DataType.DATE, Optional.empty(), Optional.empty())).primaryKey(false).fieldAlias(dateName).build();
        Field entity = Field.builder().name(entityName).type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).primaryKey(false).fieldAlias(entityName).build();

        DatasetDefinition deletePartitionDataset = DatasetDefinition.builder()
                .group(testSchemaName)
                .name(deletePartitionTableName)
                .schema(SchemaDefinition.builder()
                        .addFields(date)
                        .addFields(entity)
                        .build())
                .build();

        // Create Delete partition table
        createStagingTable(deletePartitionDataset);
        loadDeletePartitionDataWithMultiPartitionKeys(deleteFile);
        return Datasets.builder()
                .mainDataset(datasets.mainDataset())
                .stagingDataset(datasets.stagingDataset())
                .deletePartitionDataset(deletePartitionDataset).build();
    }

    @Test
    public void testOptimizeUnitempSnapshotUnPartitioned() throws Exception
    {
        Datasets datasets = setupStagingAndMainDatasets();
        UnitemporalSnapshot unitemporalSnapshot = UnitemporalSnapshot.builder()
                .digestField(digestName)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .dateTimeInName(batchTimeInName)
                        .dateTimeOutName(batchTimeOutName)
                        .build())
                .build();

        Transformer<SqlGen, SqlPlan> transformer = new RelationalTransformer(H2Sink.get());

        UnitemporalSnapshot enrichedUnitempSnapshot = (UnitemporalSnapshot) unitemporalSnapshot.accept(new IngestModeOptimizer(datasets, executor, transformer));
        assertTrue(enrichedUnitempSnapshot.partitioningStrategy() instanceof NoPartitioning);
    }

    @Test
    public void testOptimizeUnitempSnapshotPartitionedWithDate() throws Exception
    {
        // Test with staging and main dataset
        Datasets datasets = setupStagingAndMainDatasets();
        UnitemporalSnapshot unitemporalSnapshot = UnitemporalSnapshot.builder()
                .digestField(digestName)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .dateTimeInName(batchTimeInName)
                        .dateTimeOutName(batchTimeOutName)
                        .build())
                .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Arrays.asList("date")).derivePartitionSpec(true).build())
                .build();

        Transformer<SqlGen, SqlPlan> transformer = new RelationalTransformer(H2Sink.get());

        UnitemporalSnapshot enrichedUnitempSnapshot = (UnitemporalSnapshot) unitemporalSnapshot.accept(new IngestModeOptimizer(datasets, executor, transformer));
        List<Map<String, Object>> expectedPartitionSpec = new ArrayList<>();
        addPartitionSpec(expectedPartitionSpec, "2021-12-01");
        addPartitionSpec(expectedPartitionSpec, "2021-12-02");

        assertEquals(expectedPartitionSpec, ((Partitioning)(enrichedUnitempSnapshot.partitioningStrategy())).partitionSpecList());

        // Test with staging, main and delete partition tables - delete partitions never come into partition spec list
        datasets = setupStagingMainAndDeletePartitionsDatasets();
        enrichedUnitempSnapshot = (UnitemporalSnapshot) unitemporalSnapshot.accept(new IngestModeOptimizer(datasets, executor, transformer));
        expectedPartitionSpec = new ArrayList<>();
        addPartitionSpec(expectedPartitionSpec, "2021-12-01");
        addPartitionSpec(expectedPartitionSpec, "2021-12-02");

        assertEquals(expectedPartitionSpec, ((Partitioning)(enrichedUnitempSnapshot.partitioningStrategy())).partitionSpecList());
    }

    @Test
    public void testOptimizeUnitempSnapshotPartitionedWithDateAndString() throws Exception
    {
        // Test with staging and main dataset
        Datasets datasets = setupStagingAndMainDatasets();
        UnitemporalSnapshot unitemporalSnapshot = UnitemporalSnapshot.builder()
                .digestField(digestName)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .dateTimeInName(batchTimeInName)
                        .dateTimeOutName(batchTimeOutName)
                        .build())
                .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Arrays.asList("date", "entity")).derivePartitionSpec(true).build())
                .build();

        Transformer<SqlGen, SqlPlan> transformer = new RelationalTransformer(H2Sink.get());

        UnitemporalSnapshot enrichedUnitempSnapshot = (UnitemporalSnapshot) unitemporalSnapshot.accept(new IngestModeOptimizer(datasets, executor, transformer));
        List<Map<String, Object>> expectedPartitionSpec = new ArrayList<>();
        addPartitionSpec(expectedPartitionSpec, "2021-12-01", "GS");
        addPartitionSpec(expectedPartitionSpec, "2021-12-01", "IBM");
        addPartitionSpec(expectedPartitionSpec, "2021-12-01", "JPM");
        addPartitionSpec(expectedPartitionSpec, "2021-12-02", "GS");
        addPartitionSpec(expectedPartitionSpec, "2021-12-02", "IBM");
        addPartitionSpec(expectedPartitionSpec, "2021-12-02", "JPMX");

        assertEquals(expectedPartitionSpec, ((Partitioning)(enrichedUnitempSnapshot.partitioningStrategy())).partitionSpecList());

        // Test with staging, main and delete partition tables
        datasets = setupStagingMainAndDeletePartitionsDatasetsMultiplePartitionsKeys();
        enrichedUnitempSnapshot = (UnitemporalSnapshot) unitemporalSnapshot.accept(new IngestModeOptimizer(datasets, executor, transformer));
        expectedPartitionSpec = new ArrayList<>();
        addPartitionSpec(expectedPartitionSpec, "2021-12-01", "GS");
        addPartitionSpec(expectedPartitionSpec, "2021-12-01", "IBM");
        addPartitionSpec(expectedPartitionSpec, "2021-12-01", "JPM");
        addPartitionSpec(expectedPartitionSpec, "2021-12-02", "GS");
        addPartitionSpec(expectedPartitionSpec, "2021-12-02", "IBM");
        addPartitionSpec(expectedPartitionSpec, "2021-12-02", "JPMX");

        assertEquals(expectedPartitionSpec, ((Partitioning)(enrichedUnitempSnapshot.partitioningStrategy())).partitionSpecList());
    }

    @Test
    public void testOptimizeUnitempSnapshotPartitionedWithDateAndStringAboveMaxPartitionSpecLimit() throws Exception
    {
        // Test with staging and main dataset
        Datasets datasets = setupStagingAndMainDatasets();
        UnitemporalSnapshot unitemporalSnapshot = UnitemporalSnapshot.builder()
                .digestField(digestName)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .dateTimeInName(batchTimeInName)
                        .dateTimeOutName(batchTimeOutName)
                        .build())
                .partitioningStrategy(Partitioning.builder()
                        .addAllPartitionFields(Arrays.asList("date", "entity"))
                        .derivePartitionSpec(true)
                        .maxPartitionSpecFilters(5L)
                        .build())
                .build();

        Transformer<SqlGen, SqlPlan> transformer = new RelationalTransformer(H2Sink.get());

        UnitemporalSnapshot enrichedUnitempSnapshot = (UnitemporalSnapshot) unitemporalSnapshot.accept(new IngestModeOptimizer(datasets, executor, transformer));
        assertTrue(((Partitioning)(enrichedUnitempSnapshot.partitioningStrategy())).partitionSpecList().isEmpty());

        // Test with staging, main and delete partition tables
        datasets = setupStagingMainAndDeletePartitionsDatasetsMultiplePartitionsKeys();
        enrichedUnitempSnapshot = (UnitemporalSnapshot) unitemporalSnapshot.accept(new IngestModeOptimizer(datasets, executor, transformer));
        assertTrue(((Partitioning)(enrichedUnitempSnapshot.partitioningStrategy())).partitionSpecList().isEmpty());
    }

    @Test
    public void testOptimizeUnitempSnapshotPartitionedWithLimitNotReachedForDeletePartitionsButForStagingDataset() throws Exception
    {
        // Test with staging and main dataset
        Datasets datasets = setupStagingAndMainDatasets();
        UnitemporalSnapshot unitemporalSnapshot = UnitemporalSnapshot.builder()
                .digestField(digestName)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .dateTimeInName(batchTimeInName)
                        .dateTimeOutName(batchTimeOutName)
                        .build())
                .partitioningStrategy(Partitioning.builder()
                        .addAllPartitionFields(Arrays.asList("date", "entity"))
                        .derivePartitionSpec(true)
                        .maxPartitionSpecFilters(6L)
                        .build())
                .build();

        Transformer<SqlGen, SqlPlan> transformer = new RelationalTransformer(H2Sink.get());

        UnitemporalSnapshot enrichedUnitempSnapshot = (UnitemporalSnapshot) unitemporalSnapshot.accept(new IngestModeOptimizer(datasets, executor, transformer));
        List<Map<String, Object>> expectedPartitionSpec = new ArrayList<>();
        addPartitionSpec(expectedPartitionSpec, "2021-12-01", "GS");
        addPartitionSpec(expectedPartitionSpec, "2021-12-01", "IBM");
        addPartitionSpec(expectedPartitionSpec, "2021-12-01", "JPM");
        addPartitionSpec(expectedPartitionSpec, "2021-12-02", "GS");
        addPartitionSpec(expectedPartitionSpec, "2021-12-02", "IBM");
        addPartitionSpec(expectedPartitionSpec, "2021-12-02", "JPMX");

        assertEquals(expectedPartitionSpec, ((Partitioning)(enrichedUnitempSnapshot.partitioningStrategy())).partitionSpecList());

        // Test with staging, main and delete partition tables
        datasets = setupStagingMainAndLargeDeletePartitionsDatasetsMultiplePartitionsKeys();
        enrichedUnitempSnapshot = (UnitemporalSnapshot) unitemporalSnapshot.accept(new IngestModeOptimizer(datasets, executor, transformer));
        assertEquals(expectedPartitionSpec, ((Partitioning)(enrichedUnitempSnapshot.partitioningStrategy())).partitionSpecList());
    }

    @Test
    public void testOptimizeUnitempSnapshotPartitionedWithMaxLimitReached() throws Exception
    {
        Datasets datasets = setupStagingAndMainDatasets();
        UnitemporalSnapshot unitemporalSnapshot = UnitemporalSnapshot.builder()
                .digestField(digestName)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .dateTimeInName(batchTimeInName)
                        .dateTimeOutName(batchTimeOutName)
                        .build())
                .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Arrays.asList("date", "entity")).derivePartitionSpec(true).maxPartitionSpecFilters(2L).build())
                .build();

        Transformer<SqlGen, SqlPlan> transformer = new RelationalTransformer(H2Sink.get());

        UnitemporalSnapshot enrichedUnitempSnapshot = (UnitemporalSnapshot) unitemporalSnapshot.accept(new IngestModeOptimizer(datasets, executor, transformer));
        assertTrue(((Partitioning)(enrichedUnitempSnapshot.partitioningStrategy())).partitionSpecList().isEmpty());
    }


    @Test
    public void testOptimizeUnitempSnapshotPartitionedWithTimestamp() throws Exception
    {
        Datasets datasets = setupStagingAndMainDatasets();
        UnitemporalSnapshot unitemporalSnapshot = UnitemporalSnapshot.builder()
                .digestField(digestName)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .dateTimeInName(batchTimeInName)
                        .dateTimeOutName(batchTimeOutName)
                        .build())
                .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Arrays.asList("start_time")).derivePartitionSpec(true).build())
                .build();

        Transformer<SqlGen, SqlPlan> transformer = new RelationalTransformer(H2Sink.get());

        UnitemporalSnapshot enrichedUnitempSnapshot = (UnitemporalSnapshot) unitemporalSnapshot.accept(new IngestModeOptimizer(datasets, executor, transformer));
        List<Map<String, Object>> expectedPartitionSpec = new ArrayList<>();
        addPartitionSpecForTimestamp(expectedPartitionSpec, "2021-12-01 00:00:00.0");
        addPartitionSpecForTimestamp(expectedPartitionSpec, "2021-12-01 00:00:01.0");
        addPartitionSpecForTimestamp(expectedPartitionSpec, "2021-12-01 00:00:02.0");
        addPartitionSpecForTimestamp(expectedPartitionSpec, "2021-12-01 00:00:03.0");
        addPartitionSpecForTimestamp(expectedPartitionSpec, "2021-12-01 00:00:04.0");

        assertEquals(expectedPartitionSpec, ((Partitioning)(enrichedUnitempSnapshot.partitioningStrategy())).partitionSpecList());
    }


    protected void loadStagingDataForWithPartition(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
                "INSERT INTO \"TEST\".\"staging\"(date, entity, start_time, price, volume, digest) " +
                "SELECT CONVERT( \"date\",DATE ), \"entity\", CONVERT( \"start_time\", DATETIME), CONVERT( \"price\", DECIMAL(20,2)), CONVERT( \"volume\", BIGINT), \"digest\"" +
                " FROM CSVREAD( '" + path + "', 'date, entity, start_time, price, volume, digest', NULL )";
        h2Sink.executeStatement(loadSql);
    }

    protected void loadDeletePartitionData(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"main_deleted_partitions\";" +
                "INSERT INTO \"TEST\".\"main_deleted_partitions\"(date) " +
                "SELECT CONVERT( \"date\",DATE )" +
                " FROM CSVREAD( '" + path + "', 'date', NULL )";
        h2Sink.executeStatement(loadSql);
    }

    protected void loadDeletePartitionDataWithMultiPartitionKeys(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"main_deleted_partitions\";" +
                "INSERT INTO \"TEST\".\"main_deleted_partitions\"(date, entity) " +
                "SELECT CONVERT( \"date\",DATE ), \"entity\"" +
                " FROM CSVREAD( '" + path + "', 'date, entity', NULL )";

        h2Sink.executeStatement(loadSql);
    }

    private static void addPartitionSpec(List<Map<String, Object>> partitionSpecList, String date)
    {
        partitionSpecList.add(new HashMap<String,Object>()
        {
            {
                put("date", date);
            }
        });
    }

    private static void addPartitionSpecForTimestamp(List<Map<String, Object>> partitionSpecList, String timestamp)
    {
        partitionSpecList.add(new HashMap<String,Object>()
        {
            {
                put("start_time", timestamp);
            }
        });
    }

    private static void addPartitionSpec(List<Map<String, Object>> partitionSpecList, String date, String entity)
    {
        partitionSpecList.add(new HashMap<String,Object>()
        {
            {
                put("date", date);
                put("entity", entity);
            }
        });
    }

}
