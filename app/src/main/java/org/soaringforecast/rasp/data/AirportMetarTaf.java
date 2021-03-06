package org.soaringforecast.rasp.data;

import org.soaringforecast.rasp.data.metars.Metar;
import org.soaringforecast.rasp.data.taf.TAF;

public class AirportMetarTaf {

    private String airportName;

    private Metar metar;

    private TAF taf;

    private String icaoId;

    private Float elevationM;

    public Metar getMetar() {
        return metar;
    }

    public void setMetar(Metar metar) {
        this.metar = metar;
    }

    public TAF getTaf() {
        return taf;
    }

    public void setTaf(TAF taf) {
        this.taf = taf;
    }

    public String getIcaoId() {
        return icaoId;
    }

    public void setIcaoId(String icaoId) {
        this.icaoId = icaoId;
    }

    public Float getElevationM() {
        return elevationM;
    }

    public void setElevationM(Float elevationM) {
        this.elevationM = elevationM;
    }

    public String getAirportName() {
        return airportName;
    }

    public void setAirportName(String airportName) {
        this.airportName = airportName;
    }
}
