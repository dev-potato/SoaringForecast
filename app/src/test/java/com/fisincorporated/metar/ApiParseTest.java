package com.fisincorporated.metar;


import com.fisincorporated.aviationweather.data.taf.TafResponse;
import com.fisincorporated.aviationweather.retrofit.AppRetrofit;
import com.fisincorporated.aviationweather.retrofit.AviationWeatherApis;

import org.junit.Test;

import java.io.IOException;

import retrofit2.Call;

public class ApiParseTest {

    AviationWeatherApis client = AppRetrofit.get().create(AviationWeatherApis.class);
    String airportList = new String("KORH KBOS");

    //Set up this test to help make sure POJO simplexml annotations correct
    // >>>Synchronous call<<<
    @Test
    public void testMetarApiCall() throws IOException {
        Call<TafResponse> call = client.mostRecentTafForEachAirport(airportList, 2);
        TafResponse tafResponse = call.execute().body();

    }

}


