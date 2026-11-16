package com.lifelane.service;

import java.util.List;

/** Contract for querying OSRM candidate routes between two geographic points. */
public interface OsrmRoutingClient {

    List<OsrmRoute> route(Coordinate from, Coordinate to);
}