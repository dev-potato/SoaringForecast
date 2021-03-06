package org.soaringforecast.rasp.retrofit;

import io.reactivex.Single;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Streaming;


public interface AirportListDownloadApi {
    @Streaming
    @GET("airports.csv")
    Single<ResponseBody> downloadAirportList();
}

