import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.scylladb.ScyllaDBContainer;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;

import java.net.InetSocketAddress;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ScyllaDBExampleTest {

    private ScyllaDBContainer scylladb;
    private CqlSession session;

    @BeforeEach
    public void setUp() {
        scylladb = new ScyllaDBContainer("scylladb/scylla:2025.1")
            .withExposedPorts(9042, 19042);
        scylladb.start();

        session = CqlSession.builder()
            .addContactPoint(new InetSocketAddress(scylladb.getHost(), scylladb.getMappedPort(9042)))
            .withLocalDatacenter("datacenter1")
            .build();

        session.execute("CREATE KEYSPACE IF NOT EXISTS test_keyspace WITH replication = "
            + "{'class': 'NetworkTopologyStrategy', 'datacenter1': 1}");
        session.execute("USE test_keyspace");
        session.execute("CREATE TABLE IF NOT EXISTS users (id UUID PRIMARY KEY, name text, age int)");
    }

    @AfterEach
    public void tearDown() {
        if (session != null) {
            session.close();
        }
        if (scylladb != null) {
            scylladb.stop();
        }
    }

    @Test
    public void testScyllaDBOperations() {
        // Insert sample data
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();

        session.execute("INSERT INTO users (id, name, age) VALUES (?, ?, ?)", user1Id, "John Doe", 30);
        session.execute("INSERT INTO users (id, name, age) VALUES (?, ?, ?)", user2Id, "Jane Doe", 27);

        // Retrieve and verify the inserted data
        ResultSet results = session.execute("SELECT * FROM users");
        int count = 0;
        for (Row row : results) {
            assertNotNull(row.getString("name"));
            assertNotNull(row.getInt("age"));
            count++;
        }

        assertEquals(2, count); // Ensure two users are present
    }
} 
