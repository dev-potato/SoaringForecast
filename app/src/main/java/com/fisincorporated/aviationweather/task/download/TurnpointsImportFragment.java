package com.fisincorporated.aviationweather.task.download;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fisincorporated.aviationweather.R;
import com.fisincorporated.aviationweather.common.Constants;
import com.fisincorporated.aviationweather.databinding.TurnpointsImportView;
import com.fisincorporated.aviationweather.messages.ImportFile;
import com.fisincorporated.aviationweather.messages.PopThisFragmentFromBackStack;
import com.fisincorporated.aviationweather.messages.SnackbarMessage;
import com.fisincorporated.aviationweather.task.TurnpointProcessor;
import com.fisincorporated.aviationweather.workmanager.TurnpointsImportWorker;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.State;
import androidx.work.WorkManager;
import dagger.android.support.DaggerFragment;

public class TurnpointsImportFragment extends DaggerFragment {

    @Inject
    TurnpointProcessor turnpointProcessor;

    TurnpointsImportView turnpointsImportView;

    TurnpointsImportViewModel turnpointsImportViewModel;

    private List<File> files = new ArrayList<>();

    private TurnpointsImportRecyclerViewAdapter recyclerViewAdapter;

    @Inject
    public TurnpointsImportFragment() {
    }

    public static TurnpointsImportFragment newInstance() {
        TurnpointsImportFragment turnpointsImportFragment = new TurnpointsImportFragment();
        return turnpointsImportFragment;
    }

    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        turnpointsImportView = DataBindingUtil.inflate(inflater,R.layout.turnpoint_import_files, container, false);

        turnpointsImportViewModel = ViewModelProviders.of(this)
                .get(TurnpointsImportViewModel.class)
                .setTurnpointProcessor(turnpointProcessor);


        RecyclerView recyclerView = turnpointsImportView.turnpointImportsRecyclerView;
        recyclerViewAdapter = new TurnpointsImportRecyclerViewAdapter(turnpointsImportViewModel.getCupFiles());

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext()
                , linearLayoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setAdapter(recyclerViewAdapter);

        return turnpointsImportView.getRoot();
    }


    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }


    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.import_turnpoints);
        //TODO refresh file list
        files = turnpointsImportViewModel.getCupFiles();
        if (files.size() == 0) {
            displayNoTurnpointFilesDialog();
        } else {
            recyclerViewAdapter.updateFileList(files);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    private void displayNoTurnpointFilesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.no_turnpoint_files_found)
                .setTitle(R.string.no_cup_files_found)
                .setPositiveButton(R.string.yes, (dialog, id) -> {
                    displayButFirstDialog();
                })
                .setNegativeButton(R.string.no, (dialog, which) -> {
                    exitThisFragment();
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }

    private void displayButFirstDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.need_to_download_cup_files)
                .setTitle(R.string.but_first)
                .setPositiveButton(R.string.start_browser, (dialog, id) -> {
                    dialog.dismiss();
                    startTurnpointBrowser();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                    exitThisFragment();
                });
        builder.create().show();

    }

    private void startTurnpointBrowser() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://soaringweb.org/TP/NA.html#US"));
        startActivity(browserIntent);
    }

    private void exitThisFragment() {
            EventBus.getDefault().post(new PopThisFragmentFromBackStack());
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ImportFile importFile) {
        turnpointsImportView.turnpointImportsProgressBar.setVisibility(View.VISIBLE);

        Data.Builder builder = new Data.Builder();
        builder.putString(Constants.TURNPOINT_FILE_NAME, importFile.getFile().getName());
        OneTimeWorkRequest importTurnpointFileWorker =
                new OneTimeWorkRequest.Builder(TurnpointsImportWorker.class)
                        .setInputData(builder.build())
                        .build();
        WorkManager.getInstance().enqueue(importTurnpointFileWorker);
        WorkManager.getInstance().getStatusById(importTurnpointFileWorker.getId())
                .observe(this, workStatus -> {
                    // Do something with the status
                    if (workStatus != null && workStatus.getState().isFinished()) {
                        turnpointsImportView.turnpointImportsProgressBar.setVisibility(View.GONE);
                        EventBus.getDefault().post(new SnackbarMessage(getString(R.string.import_successful, importFile.getFile().getName())));
                        EventBus.getDefault().post(new PopThisFragmentFromBackStack());
                    } else {
                        if (workStatus != null && workStatus.getState().equals(State.FAILED)){
                            EventBus.getDefault().post(new SnackbarMessage(getString(R.string.import_failed, importFile.getFile().getName())));
                        }
                    }
                });

    }
}
