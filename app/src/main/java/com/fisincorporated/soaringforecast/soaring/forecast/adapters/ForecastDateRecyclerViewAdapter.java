package com.fisincorporated.soaringforecast.soaring.forecast.adapters;

import android.databinding.DataBindingUtil;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.fisincorporated.soaringforecast.R;
import com.fisincorporated.soaringforecast.common.recycleradapter.GenericRecyclerViewAdapter;
import com.fisincorporated.soaringforecast.databinding.ModelForecastDateView;
import com.fisincorporated.soaringforecast.soaring.json.ModelForecastDate;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

public class ForecastDateRecyclerViewAdapter extends GenericRecyclerViewAdapter<ModelForecastDate, ForecastDateViewHolder> {

    public ForecastDateRecyclerViewAdapter(List<ModelForecastDate> items) {
        super(items);
    }

    @Override
    public ForecastDateViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ModelForecastDateView binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()), R.layout.soaring_forecast_date, parent, false);
       return new ForecastDateViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ForecastDateViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        //TODO get better way to do following
        holder.getViewDataBinding().modelForecastDateLabel.setSelected(getItems().get(position).equals(getSelectedItem()));
        holder.getViewDataBinding().setDateClickListener(this);
    }

    public void updateModelForecastDateList(List<ModelForecastDate> modelForecastDates) {
        int position = -1;
        getItems().clear();
        if (modelForecastDates != null) {
            getItems().addAll(modelForecastDates);
            //if selected date  has been previously set, see if that date is
            // in the new list and if so set selected to same date as previously set
            // if not in list set to first date in list.
            if (getSelectedItem() != null) {
                position = checkForSelectedDateInNewList(getSelectedItem(), modelForecastDates);
            }
            if (position > -1) {
                setSelectedItem(modelForecastDates.get(position));
            } else {
                if (modelForecastDates.size() > 0) {
                    setSelectedItem(modelForecastDates.get(0));
                }
            }
        }
        notifyDataSetChanged();
    }

    private int checkForSelectedDateInNewList(ModelForecastDate modelForecastDate, List<ModelForecastDate> newForecastDates) {
        if (newForecastDates == null || newForecastDates.size() == 0) {
            return -1;
        }
        for (int i = 0; i < newForecastDates.size(); ++i) {
            if (newForecastDates.get(i).getYyyymmddDate().equals(modelForecastDate.getYyyymmddDate())) {
                return i;
            }
        }
        return -1;
    }

    public void onDateClick(ModelForecastDate modelForecastDate, Integer position) {
        if (modelForecastDate != getSelectedItem()) {
            setSelectedItem(modelForecastDate);
            notifyDataSetChanged();
            smoothScrollToPosition(position);
            EventBus.getDefault().post(modelForecastDate);
        }
    }

}
