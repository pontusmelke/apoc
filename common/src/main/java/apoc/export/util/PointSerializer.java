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
package apoc.export.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

import org.neo4j.graphdb.spatial.Point;

public class PointSerializer extends JsonSerializer<Point> {
    @Override
    public void serialize(Point value, JsonGenerator jsonGenerator, SerializerProvider serializers) throws IOException {

        String crsType = value.getCRS().getType();
        double[] coordinates = value.getCoordinate().getCoordinate();

        if (crsType.startsWith("cartesian")) {
            if (coordinates.length == 3) {
                jsonGenerator.writeObject( new PointCartesian( crsType, coordinates[0], coordinates[1], coordinates[2] ) );
            } else {
                jsonGenerator.writeObject( new PointCartesian( crsType, coordinates[0], coordinates[1] ) );
            }
        } else {
            if (coordinates.length == 3) {
                jsonGenerator.writeObject( new PointWgs( crsType, coordinates[0], coordinates[1], coordinates[2] ) );
            } else {
                jsonGenerator.writeObject( new PointWgs( crsType, coordinates[0], coordinates[1] ) );
            }
        }
    }


    static class PointCartesian {
        private String crs;
        private Double x;
        private Double y;
        private Double z;

        public PointCartesian(String crs, Double x, Double y, Double z) {
            this.crs = crs;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public PointCartesian(String crs, Double x, Double y) {
            this.crs = crs;
            this.x = x;
            this.y = y;
        }

        public String getCrs() {
            return crs;
        }

        public void setCrs(String crs) {
            this.crs = crs;
        }

        public Double getX() {
            return x;
        }

        public void setX(Double x) {
            this.x = x;
        }

        public Double getY() {
            return y;
        }

        public void setY(Double y) {
            this.y = y;
        }

        public Double getZ() {
            return z;
        }

        public void setZ(Double z) {
            this.z = z;
        }
    }


    static class PointWgs {
        private String crs;
        private Double latitude;
        private Double longitude;
        private Double height;

        public PointWgs(String crs, Double longitude, Double latitude, Double height) {
            this.crs = crs;
            this.latitude = latitude;
            this.longitude = longitude;
            this.height = height;
        }

        public PointWgs(String crs, Double longitude, Double latitude) {
            this.crs = crs;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getCrs() {
            return crs;
        }

        public void setCrs(String crs) {
            this.crs = crs;
        }

        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }

        public Double getHeight() {
            return height;
        }

        public void setHeight(Double height) {
            this.height = height;
        }
    }

}
