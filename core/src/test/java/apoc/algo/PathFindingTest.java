/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package apoc.algo;

import apoc.util.TestUtil;
import apoc.util.collection.Iterators;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.graphdb.Path;
import org.neo4j.test.rule.DbmsRule;
import org.neo4j.test.rule.ImpermanentDbmsRule;

import java.util.List;
import java.util.Map;

import static apoc.algo.AlgoUtil.SETUP_GEO;
import static apoc.algo.AlgoUtil.assertAStarResult;
import static apoc.util.TestUtil.testCall;
import static apoc.util.TestUtil.testResult;
import static apoc.util.Util.map;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class PathFindingTest {

    private static final String SETUP_MISSING_PROPERTY = "CREATE " +
            "(a:Loc{name:'A'}), " +
            "(b:Loc{name:'B'}), " +
            "(c:Loc{name:'C'}), " +
            "(d:Loc{name:'D'}), " +
            "(a)-[:ROAD {d:100}]->(d), " +
            "(a)-[:RAIL {d:5}]->(d), " +
            "(a)-[:ROAD {d:'10'}]->(b), " +
            "(b)-[:ROAD {d:20}]->(c), " +
            "(c)-[:ROAD]->(d), " +
            "(a)-[:ROAD {d:20}]->(c) ";
    private static final String SETUP_SIMPLE = "CREATE " +
            "(a:Loc{name:'A'}), " +
            "(b:Loc{name:'B'}), " +
            "(c:Loc{name:'C'}), " +
            "(d:Loc{name:'D'}), " +
            "(a)-[:ROAD {d:100}]->(d), " +
            "(a)-[:RAIL {d:5}]->(d), " +
            "(a)-[:ROAD {d:10}]->(b), " +
            "(b)-[:ROAD {d:20}]->(c), " +
            "(c)-[:ROAD {d:30}]->(d), " +
            "(a)-[:ROAD {d:20}]->(c) ";

    @Rule
    public DbmsRule db = new ImpermanentDbmsRule();


    @Before
   	public void setUp() {
   		TestUtil.registerProcedure(db, PathFinding.class);
   	}

    @After
    public void teardown() {
        db.shutdown();
    }

    @Test
    public void testAStar() {
        db.executeTransactionally(SETUP_GEO);
        testResult(db,
                "MATCH (from:City {name:'München'}), (to:City {name:'Hamburg'}) " +
                        "CALL apoc.algo.aStar(from, to, 'DIRECT', 'dist', 'lat', 'lon') yield path, weight " +
                        "RETURN path, weight" ,
                r -> assertAStarResult(r)
        );
    }

    @Test
    public void testAStarConfig() {
        db.executeTransactionally(SETUP_GEO);
        testResult(db,
                "MATCH (from:City {name:'München'}), (to:City {name:'Hamburg'}) " +
                        "CALL apoc.algo.aStarConfig(from, to, 'DIRECT', {weight:'dist',y:'lat', x:'lon',default:100}) yield path, weight " +
                        "RETURN path, weight" ,
                r -> assertAStarResult(r)
        );
    }

    @Test
    public void testAStarConfigWithPoint() {
        db.executeTransactionally(SETUP_GEO);
        testResult(db,
                "MATCH (from:City {name:'München'}), (to:City {name:'Hamburg'}) " +
                        "CALL apoc.algo.aStarConfig(from, to, 'DIRECT', {pointPropName:'coords', weight:'dist', default:100}) yield path, weight " +
                        "RETURN path, weight" ,
                AlgoUtil::assertAStarResult
        );
    }

    @Test
    public void testDijkstra() {
        db.executeTransactionally(SETUP_SIMPLE);
        testCall(db,
            "MATCH (from:Loc{name:'A'}), (to:Loc{name:'D'}) " +
            "CALL apoc.algo.dijkstra(from, to, 'ROAD>', 'd') yield path, weight " +
            "RETURN path, weight" ,
            row ->  {
                assertEquals(50.0, row.get("weight")) ;
                assertEquals(2, ((Path)(row.get("path"))).length()) ; // 3nodes, 2 rels
            }
        );
        testCall(db,
            "MATCH (from:Loc{name:'A'}), (to:Loc{name:'D'}) " +
            "CALL apoc.algo.dijkstra(from, to, '', 'd') yield path, weight " +
            "RETURN path, weight" ,
            row ->  {
                assertEquals(5.0, row.get("weight")) ;
                assertEquals(1, ((Path)(row.get("path"))).length()) ; // 2nodes, 1 rels
            }
        );
    }

    @Test
    public void testDijkstraMultipleShortest() {
        db.executeTransactionally(SETUP_SIMPLE);
        testResult(db,
                "MATCH (from:Loc{name:'A'}), (to:Loc{name:'D'}) " +
                        "CALL apoc.algo.dijkstra(from, to, 'ROAD>', 'd', 99999, 3) yield path, weight " +
                        "RETURN path, weight",
                result -> {
                    List<Map<String, Object>> records = Iterators.asList(result);
                    assertThat(
                            map(records, map -> map.get("weight")),
                            contains(50.0, 60.0, 100.0)
                    );

                    assertThat(
                            map(records, map -> ((Path) map.get("path")).length()),
                            contains(2, 3, 1)
                    );
                }
        );
    }

    @Test
    public void testAllSimplePaths() {
        db.executeTransactionally(SETUP_MISSING_PROPERTY);
        testResult(db,
                "MATCH (from:Loc{name:'A'}), (to:Loc{name:'D'}) " +
                        "CALL apoc.algo.allSimplePaths(from, to, 'ROAD>', 3) yield path " +
                        "RETURN path ORDER BY length(path)",
                res -> {
                    Path path;
                    path = (Path) res.next().get("path");
                    assertEquals(1, path.length());
                    path = (Path) res.next().get("path");
                    assertEquals(2, path.length());
                    path = (Path) res.next().get("path");
                    assertEquals(3, path.length());
                    assertEquals(false, res.hasNext());
                }
        );
    }
    @Test
    public void testAllSimplePathResults() {
        db.executeTransactionally(SETUP_MISSING_PROPERTY);
        testResult(db,
                "MATCH (from:Loc{name:'A'}), (to:Loc{name:'D'}) " +
                        "CALL apoc.algo.allSimplePaths(from, to, 'ROAD>', 3) yield path " +
                        "RETURN nodes(path) as nodes ORDER BY length(path)",
                res -> {
                    List nodes;
                    nodes = (List) res.next().get("nodes");
                    assertEquals(2, nodes.size());
                    nodes = (List) res.next().get("nodes");
                    assertEquals(3, nodes.size());
                    nodes = (List) res.next().get("nodes");
                    assertEquals(4, nodes.size());
                    assertEquals(false, res.hasNext());
                }
        );
    }
}
