package com.fisincorporated.soaringforecast.task.list;

import android.annotation.SuppressLint;
import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import com.fisincorporated.soaringforecast.common.ObservableViewModel;
import com.fisincorporated.soaringforecast.repository.AppRepository;
import com.fisincorporated.soaringforecast.repository.Task;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class TaskListViewModel extends ObservableViewModel {

    private AppRepository appRepository;
    private MutableLiveData<List<Task>> tasks = new MutableLiveData<>();

    public TaskListViewModel(@NonNull Application application) {
        super(application);
    }

    public TaskListViewModel setAppRepository(AppRepository appRepository) {
        this.appRepository = appRepository;
        tasks = new MutableLiveData<>();
        tasks.setValue(new ArrayList<>());
        return this;
    }

    @SuppressLint("CheckResult")
    public LiveData<List<Task>> listTasks() {
        appRepository.listAllTasks()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(taskList -> {
                            tasks.setValue(taskList);
                            notifyChange();
                        },
                        t -> {
                            Timber.e(t);
                        });
        return tasks;
    }


}
