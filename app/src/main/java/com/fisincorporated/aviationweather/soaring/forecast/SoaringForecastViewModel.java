package com.fisincorporated.aviationweather.soaring.forecast;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.databinding.BaseObservable;
import android.databinding.DataBindingUtil;
import android.databinding.InverseBindingMethod;
import android.databinding.InverseBindingMethods;
import android.graphics.Bitmap;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.fisincorporated.aviationweather.R;
import com.fisincorporated.aviationweather.app.AppPreferences;
import com.fisincorporated.aviationweather.app.ViewModelLifeCycle;
import com.fisincorporated.aviationweather.common.Constants;
import com.fisincorporated.aviationweather.common.Constants.FORECASTACTION;
import com.fisincorporated.aviationweather.databinding.SoaringForecastImageBinding;
import com.fisincorporated.aviationweather.messages.DataLoadCompleteEvent;
import com.fisincorporated.aviationweather.messages.DataLoadingEvent;
import com.fisincorporated.aviationweather.messages.ReadyToSelectSoaringForecastEvent;
import com.fisincorporated.aviationweather.retrofit.SoaringForecastApi;
import com.fisincorporated.aviationweather.soaring.json.GpsLocationAndTimes;
import com.fisincorporated.aviationweather.soaring.json.ModelForecastDate;
import com.fisincorporated.aviationweather.soaring.json.ModelLocationAndTimes;
import com.fisincorporated.aviationweather.soaring.json.RegionForecastDate;
import com.fisincorporated.aviationweather.soaring.json.RegionForecastDates;
import com.fisincorporated.aviationweather.utils.ViewUtilities;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLngBounds;

import org.cache2k.Cache;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@InverseBindingMethods({
        @InverseBindingMethod(type = Spinner.class, attribute = "android:selectedItemPosition"),})

public class SoaringForecastViewModel extends BaseObservable implements ViewModelLifeCycle
        , OnMapReadyCallback {

    private static final String TAG = SoaringForecastViewModel.class.getSimpleName();

    private SoaringForecastImageBinding viewDataBinding;
    private ValueAnimator soaringForecastImageAnimation;
    private RegionForecastDates regionForecastDates = new RegionForecastDates();
    private SoaringForecastModel selectedSoaringForecastModel;
    private ModelForecastDate selectedModelForecastDate = null;
    private View bindingView;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private HashMap<String, SoaringForecastImageSet> imageMap = new HashMap<>();
    private List<String> forecastTimes;
    private int numberForecastTimes;
    private SoaringForecastImageSet soaringForecastImageSet;
    private int lastImageIndex = -1;
    private Fragment parentFragment;
    private GoogleMap googleMap;
    private LatLngBounds mapLatLngBounds;
    private GroundOverlay forecastOverlay;
    private List<ModelForecastDate> modelForecastDates;
    private RecyclerViewAdapterModelForecastDate modelForecastDaterecyclerViewAdapter;
    private ProgressBar mapProgressBar;

    @Inject
    public SoaringForecastDownloader soaringForecastDownloader;

    @Inject
    public Cache<String, SoaringForecastImage> soaringForecastImageCache;

    @Inject
    public List<SoaringForecastModel> soaringForecastModels;

    @Inject
    public AppPreferences appPreferences;

    @Inject
    public SoaringForecastApi soaringForecastApi;

    @Inject
    SoaringForecastViewModel() {
    }

    public SoaringForecastViewModel setView(Fragment fragment, View view) {
        parentFragment = fragment;
        fireLoadStarted();
        bindingView = view.findViewById(R.id.soaring_forecast_constraint_layout);
        bindViewModel();

        return this;
    }

    public void bindViewModel() {
        viewDataBinding = DataBindingUtil.bind(bindingView);
        if (viewDataBinding != null) {
            viewDataBinding.setViewModel(this);
            selectedSoaringForecastModel = appPreferences.getSoaringForecastType();
            setupSoaringForecastModelsRecyclerView(soaringForecastModels);
            mapProgressBar = viewDataBinding.soaringForecastMapProgressBar;
            setMapProgresBarVisibility(true);

        }
    }

    private void setMapProgresBarVisibility(boolean visible) {
        if (mapProgressBar != null) {
            mapProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Set up recycler view with forecast models - gfs, rap, ...
     *
     * @param soaringForecastModels
     */
    private void setupSoaringForecastModelsRecyclerView(List<SoaringForecastModel> soaringForecastModels) {
        viewDataBinding.soaringForecastTypeRecyclerView.setHasFixedSize(true);
        viewDataBinding.soaringForecastTypeRecyclerView.setLayoutManager(
                new LinearLayoutManager(viewDataBinding.getRoot().getContext(), LinearLayoutManager.HORIZONTAL, false));
        RecyclerViewAdapterSoaringForecastModel recyclerViewAdapter = new RecyclerViewAdapterSoaringForecastModel(soaringForecastModels);
        viewDataBinding.soaringForecastTypeRecyclerView.setAdapter(recyclerViewAdapter);
    }

    private void setupModelForecastDateRecyclerView(List<ModelForecastDate> modelForecastDateList) {
        viewDataBinding.regionForecastDateRecyclerView.setHasFixedSize(true);
        viewDataBinding.regionForecastDateRecyclerView.setLayoutManager(
                new LinearLayoutManager(viewDataBinding.getRoot().getContext(), LinearLayoutManager.HORIZONTAL, false));
        modelForecastDaterecyclerViewAdapter = new RecyclerViewAdapterModelForecastDate(modelForecastDateList);
        viewDataBinding.regionForecastDateRecyclerView.setAdapter(modelForecastDaterecyclerViewAdapter);
    }


    @Override
    public void onResume() {
        // Setting the initial satellite region and type will cause call update images so need
        //to bypass
        EventBus.getDefault().register(this);
        soaringForecastDownloader.loadForecastsForDay(appPreferences.getSoaringForecastRegion());
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        soaringForecastDownloader.cancelOutstandingLoads();
        stopImageAnimation();
    }

    @Override
    public void onDestroy() {
        soaringForecastDownloader.shutdown();
        compositeDisposable.dispose();
        stopImageAnimation();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(DataLoadingEvent event) {
        stopImageAnimation();
    }

    /**
     * Should be called after response from current.json call
     * You get a list of all dates for which some forecast model will be provided.
     *
     * @param regionForecastDates
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(RegionForecastDates regionForecastDates) {
        storeRegionForecastDates(regionForecastDates);
    }

    /**
     * Should be called once all models for each date in current.json response are received and processed
     *
     * @param readyToSelectSoaringForecastEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ReadyToSelectSoaringForecastEvent readyToSelectSoaringForecastEvent) {
        // At this point for each date RegionForecastDates contains for each date the type of forecast available
        // So pull out the dates available for the forecast type selected.
        createForecastDateListForSelectedModel();
        setupModelForecastDateRecyclerView(modelForecastDates);
        // Get whatever current date is to start
        if (modelForecastDates.size() > 0) {
            setMapProgresBarVisibility(false);
            selectedModelForecastDate = modelForecastDates.get(0);
            // TODO - load bitmaps for hcrit for first forecast type (e.g. gfs)
            Timber.d("Ready to load bitmaps");
            startLoadingSoaringForecastImages();
        } else {
            Snackbar.make(bindingView, R.string.model_forecast_for_date_not_available, Snackbar.LENGTH_LONG);
        }
    }

    private void createForecastDateListForSelectedModel() {
        GpsLocationAndTimes gpsLocationAndTimes;
        ModelLocationAndTimes modelLocationAndTimes;
        String model = selectedSoaringForecastModel.getName();
        modelForecastDates = new ArrayList<>();
        for (RegionForecastDate regionForecastDate : regionForecastDates.getRegionForecastDateList()) {
            modelLocationAndTimes = regionForecastDate.getModelLocationAndTimes();
            if (modelLocationAndTimes != null && modelLocationAndTimes.getGpsLocationAndTimesForModel(model) != null) {
                ModelForecastDate modelForecastDate = new ModelForecastDate(model);
                modelForecastDate.setBaseDate(regionForecastDate.getIndex(), regionForecastDate.getFormattedDate(), regionForecastDate.getYyyymmddDate());
                modelForecastDate.setGpsLocationAndTimes(modelLocationAndTimes.getGpsLocationAndTimesForModel(model));
                modelForecastDates.add(modelForecastDate);
            }
        }
    }

    private void startLoadingSoaringForecastImages() {
        loadSoaringForecastImages();
        setMapBounds();
        displayMap();

    }

    private void storeRegionForecastDates(RegionForecastDates downloadedRegionForecastDates) {
        regionForecastDates = downloadedRegionForecastDates;
        regionForecastDates.parseForecastDates();
        soaringForecastDownloader.loadTypeLocationAndTimes(appPreferences.getSoaringForecastRegion(), downloadedRegionForecastDates);
    }

    @SuppressLint("CheckResult")
    private void loadSoaringForecastImages() {
        stopImageAnimation();
        compositeDisposable.clear();
        imageMap.clear();
        setMapProgresBarVisibility(true);
        DisposableObserver disposableObserver = soaringForecastDownloader.getSoaringForecastForTypeAndDay(
                bindingView.getContext().getString(R.string.new_england_region)
                , selectedModelForecastDate.getYyyymmddDate(), selectedSoaringForecastModel.getName()
                , "wstar"
                , selectedModelForecastDate.getGpsLocationAndTimes().getTimes())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<SoaringForecastImage>() {
                    @Override
                    public void onStart() {
                        fireLoadStarted();
                    }

                    @Override
                    public void onNext(SoaringForecastImage soaringForecastImage) {
                        storeImage(soaringForecastImage);
                    }

                    @Override
                    public void onError(Throwable e) {
                        displayCallFailure(e);
                    }

                    @Override
                    public void onComplete() {
                        fireLoadComplete();
                        getForecastTimes();
                        setMapProgresBarVisibility(false);
                        startImageAnimation();

                    }
                });
        compositeDisposable.add(disposableObserver);
    }

    private void storeImage(SoaringForecastImage soaringForecastImage) {
        SoaringForecastImageSet imageSet = imageMap.get(soaringForecastImage.getForecastTime());
        if (imageSet == null) {
            imageSet = new SoaringForecastImageSet();
        }
        switch (soaringForecastImage.getBitmapType()) {
            case Constants.BODY:
                imageSet.setBodyImage(soaringForecastImage);
                break;
            case Constants.HEAD:
                imageSet.setHeaderImage(soaringForecastImage);
                break;
            case Constants.SIDE:
                imageSet.setSideImage(soaringForecastImage);
                break;
            case Constants.FOOT:
                imageSet.setFooterImage(soaringForecastImage);
                break;
            default:
                Timber.d("Unknown forecast image type: %s", soaringForecastImage.getBitmapType());
        }
        imageMap.put(soaringForecastImage.getForecastTime(), imageSet);
    }

    private void displayCallFailure(Throwable t) {
        ViewUtilities.displayErrorDialog(viewDataBinding.getRoot(), bindingView.getContext().getString(R.string.oops), t.toString());
    }

    private void fireLoadStarted() {
        EventBus.getDefault().post(new DataLoadingEvent());
    }

    private void fireLoadComplete() {
        EventBus.getDefault().post(new DataLoadCompleteEvent());
    }

    /**
     * Selected forecast type gfs, nam, ...
     *
     * @param soaringForecastModel
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(SoaringForecastModel soaringForecastModel) {
        if (!soaringForecastModel.equals(selectedSoaringForecastModel)) {
            selectedSoaringForecastModel = soaringForecastModel;
            appPreferences.setSoaringForecastType(selectedSoaringForecastModel);
            createForecastDateListForSelectedModel();
            if (modelForecastDaterecyclerViewAdapter != null) {
                modelForecastDaterecyclerViewAdapter.updateModelForecastDateList(modelForecastDates);
            }
            loadSoaringForecastImages();
        }
    }

    /**
     * Selected model date
     *
     * @param modelForecastDate
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ModelForecastDate modelForecastDate) {
        selectedModelForecastDate = modelForecastDate;
        loadSoaringForecastImages();
    }

    private void stopImageAnimation() {
        Timber.d("Stopping Animation");
        if (soaringForecastImageAnimation != null) {
            soaringForecastImageAnimation.cancel();
            return;
        }
        Timber.e("soaringForecastImageanimation is null so no animation to stop");
    }

    private void startImageAnimation() {
        stopImageAnimation();
        // need to 'overshoot' the animation to be able to get the last image value
        Thread thread = Thread.currentThread();
        Timber.d("Creating animation on %1$s ( %2$d )", thread.getName(), thread.getId());
        soaringForecastImageAnimation = ValueAnimator.ofInt(0, numberForecastTimes);
        soaringForecastImageAnimation.setInterpolator(new LinearInterpolator());
        soaringForecastImageAnimation.setDuration(15000);
        soaringForecastImageAnimation.addUpdateListener(updatedAnimation -> {
//                Timber.d("RunnableJob is being run by %1$s ( %2$d )",  thread.getName(), thread.getId() );
            int index = (int) updatedAnimation.getAnimatedValue();
            //Timber.d("animation index: %d  ", index);
            if (index > numberForecastTimes - 1) {
                index = numberForecastTimes - 1;
            }
            // Don't force redraw if still on same image as last time
            if (lastImageIndex != index) {
                // Timber.d("updating image to index: %1$d", index);
                displayForecastImage(index);
            }
            lastImageIndex = index;
        });
        soaringForecastImageAnimation.setRepeatCount(ValueAnimator.INFINITE);
        soaringForecastImageAnimation.start();
    }

    private void getForecastTimes() {
        forecastTimes = selectedModelForecastDate.getGpsLocationAndTimes().getTimes();
        numberForecastTimes = forecastTimes.size();
    }

    public void displayForecastImage(int index) {
        soaringForecastImageSet = imageMap.get(forecastTimes.get(index));
        viewDataBinding.soaringForecastImageLocalTime.setText(forecastTimes.get(index));
        if (soaringForecastImageSet != null) {
            if (soaringForecastImageSet.getHeaderImage() != null
                    && soaringForecastImageSet.getFooterImage() != null
                    && soaringForecastImageSet.getBodyImage() != null) {
                setGroundOverlay(soaringForecastImageSet.getBodyImage().getBitmap());
                viewDataBinding.soaringForecastScaleImage.setImageBitmap(soaringForecastImageSet.getFooterImage().getBitmap());
                viewDataBinding.soaringForecastHeaderImage.setImageBitmap(soaringForecastImageSet.getHeaderImage().getBitmap());
            }
        }
    }

    // Stepping thru forecast images

    public void onStepClick(FORECASTACTION forecastaction) {
        switch (forecastaction) {
            case BACKWARD:
                stopImageAnimation();
                displayForecastImage(stepImageBy(-1));
                break;
            case FORWARD:
                stopImageAnimation();
                displayForecastImage(stepImageBy(1));
                break;
            case LOOP:
                startImageAnimation();
                break;
        }
    }

    private int stepImageBy(int step) {
        lastImageIndex = lastImageIndex + step;
        if (lastImageIndex < 0) {
            lastImageIndex = numberForecastTimes - 1;
        } else if (lastImageIndex > (numberForecastTimes - 1)) {
            lastImageIndex = 0;
        }
        return lastImageIndex;
    }

    //------- map stuff ---------

    private void displayMap() {
        SupportMapFragment supportMapFragment = (SupportMapFragment) parentFragment.getChildFragmentManager().findFragmentById(R.id.soaring_forecast_map);
        supportMapFragment.getMapAsync(this);
    }

    private void setMapBounds() {
        GpsLocationAndTimes gpsLocationAndTimes = selectedModelForecastDate.getGpsLocationAndTimes();
        mapLatLngBounds = new LatLngBounds(
                gpsLocationAndTimes.getSouthWestLatLng(), gpsLocationAndTimes.getNorthEastLatLng());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        setupMap();// do your map stuff here
    }


    private void setupMap() {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mapLatLngBounds, 0));
        googleMap.setLatLngBoundsForCameraTarget(mapLatLngBounds);
    }

    private void setGroundOverlay(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        if (forecastOverlay == null) {
            GroundOverlayOptions forecastOverlayOptions = new GroundOverlayOptions()
                    .image(BitmapDescriptorFactory.fromBitmap(bitmap))
                    .positionFromBounds(mapLatLngBounds);
            forecastOverlay = googleMap.addGroundOverlay(forecastOverlayOptions);
        } else {
            forecastOverlay.setImage(BitmapDescriptorFactory.fromBitmap(bitmap));
        }
    }
}