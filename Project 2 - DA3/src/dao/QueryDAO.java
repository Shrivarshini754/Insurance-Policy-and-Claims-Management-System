package dao;

import db.Neo4jConnection;
import model.QueryResult;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.exceptions.Neo4jException;

import java.util.ArrayList;
import java.util.List;

/**
 * QueryDAO - Data Access Object for executing arbitrary interactive Cypher queries.
 * Migrated from JDBC SQL to Neo4j Cypher using Neo4jConnection.
 * Preserves the executeQuery(String) method signature and QueryResult return structure.
 */
public class QueryDAO {

    /**
     * Executes an arbitrary Cypher query against the Neo4j database.
     *
     * @param cypher Cypher query string
     * @return QueryResult populated with column names and row string arrays,
     *         or error details if execution fails
     */
    public QueryResult executeQuery(String cypher) {
        QueryResult resultObj = new QueryResult();

        if (cypher == null || cypher.trim().isEmpty()) {
            resultObj.setSuccess(false);
            resultObj.setErrorMessage("Cypher query cannot be null or empty.");
            return resultObj;
        }

        try (Session session = Neo4jConnection.getSession()) {
            Result result = session.run(cypher);

            List<String> columnNames = new ArrayList<>(result.keys());
            List<String[]> rows = new ArrayList<>();

            if (columnNames.isEmpty()) {
                // Statements without a RETURN clause (e.g. pure CREATE/SET/DELETE)
                var summary = result.consume();
                var counters = summary.counters();
                columnNames.add("Summary");

                String summaryText = String.format(
                    "Nodes created: %d, deleted: %d | Relationships created: %d, deleted: %d | Properties set: %d",
                    counters.nodesCreated(),
                    counters.nodesDeleted(),
                    counters.relationshipsCreated(),
                    counters.relationshipsDeleted(),
                    counters.propertiesSet()
                );
                rows.add(new String[] { summaryText });
            } else {
                while (result.hasNext()) {
                    Record record = result.next();
                    String[] row = new String[columnNames.size()];
                    for (int i = 0; i < columnNames.size(); i++) {
                        row[i] = formatValue(record.get(i));
                    }
                    rows.add(row);
                }
            }

            resultObj.setColumnNames(columnNames);
            resultObj.setRows(rows);
            resultObj.setSuccess(true);

        } catch (Neo4jException e) {
            resultObj.setSuccess(false);
            resultObj.setErrorMessage(e.code() + ": " + e.getMessage());
        } catch (Exception e) {
            resultObj.setSuccess(false);
            resultObj.setErrorMessage(e.getMessage() != null ? e.getMessage() : "Unknown query execution error.");
        }

        return resultObj;
    }

    /**
     * Helper to format Neo4j Values (nodes, relationships, maps, lists, primitives)
     * into readable strings for tabular Swing display.
     */
    private String formatValue(Value val) {
        if (val == null || val.isNull()) {
            return "";
        }
        try {
            String typeName = val.type().name();
            if ("NODE".equalsIgnoreCase(typeName)) {
                org.neo4j.driver.types.Node node = val.asNode();
                return ":" + String.join(":", node.labels()) + " " + node.asMap();
            } else if ("RELATIONSHIP".equalsIgnoreCase(typeName)) {
                org.neo4j.driver.types.Relationship rel = val.asRelationship();
                return "[:" + rel.type() + " " + rel.asMap() + "]";
            }
        } catch (Exception ignored) {
        }

        Object obj = val.asObject();
        return (obj != null) ? obj.toString() : "";
    }
}
