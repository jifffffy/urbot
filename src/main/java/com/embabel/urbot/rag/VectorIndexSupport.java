package com.embabel.urbot.rag;

import org.drivine.manager.PersistenceManager;
import org.drivine.query.QuerySpecification;
import org.drivine.schema.SchemaItemKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Keeps persisted vector indexes aligned with the configured embedding model.
 * <p>
 * Neo4j vector indexes are created with fixed dimensions. Changing the embedding
 * model (for example from OpenAI's 1536-dimension model to Ollama's 768-dimension
 * nomic-embed-text) leaves an incompatible index behind because provisioning uses
 * {@code CREATE VECTOR INDEX IF NOT EXISTS}. Drop only the affected index so it can
 * be recreated with the current dimensions; documents may need re-ingestion.
 */
public final class VectorIndexSupport {

    private static final Logger logger = LoggerFactory.getLogger(VectorIndexSupport.class);

    private VectorIndexSupport() {
    }

    public static void dropIfDimensionsDiffer(
            PersistenceManager persistenceManager,
            int expectedDimensions,
            String... indexNames) {
        var managedNames = Set.of(indexNames);
        try {
            for (var index : persistenceManager.getIndexes().list()) {
                if (index.getKind() != SchemaItemKind.VECTOR_INDEX
                        || !managedNames.contains(index.getName())
                        || index.getDimensions() == null
                        || index.getDimensions() == expectedDimensions) {
                    continue;
                }
                logger.warn(
                        "Dropping vector index '{}' with {} dimensions (embedding model now uses {}); "
                                + "re-ingest documents if searches return no results",
                        index.getName(), index.getDimensions(), expectedDimensions);
                persistenceManager.execute(QuerySpecification.withStatement(
                        "DROP INDEX `%s` IF EXISTS".formatted(index.getName())));
            }
        } catch (Exception e) {
            logger.warn("Could not verify vector index dimensions: {}", e.getMessage());
        }
    }
}
