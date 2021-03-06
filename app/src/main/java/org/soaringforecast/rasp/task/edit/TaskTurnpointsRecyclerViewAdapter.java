package org.soaringforecast.rasp.task.edit;

import android.databinding.DataBindingUtil;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.soaringforecast.rasp.R;
import org.soaringforecast.rasp.common.recycleradapter.GenericListClickListener;
import org.soaringforecast.rasp.common.recycleradapter.GenericRecyclerViewAdapter;
import org.soaringforecast.rasp.common.recycleradapter.GenericViewHolder;
import org.soaringforecast.rasp.databinding.TaskTurnpointView;
import org.soaringforecast.rasp.repository.TaskTurnpoint;
import org.soaringforecast.rasp.touchhelper.ItemTouchHelperAdapter;
import org.soaringforecast.rasp.touchhelper.ItemTouchHelperViewHolder;

import java.util.Collections;

public class TaskTurnpointsRecyclerViewAdapter
        extends GenericRecyclerViewAdapter<TaskTurnpoint, TaskTurnpointsRecyclerViewAdapter.TaskTurnpointViewHolder>
        implements ItemTouchHelperAdapter, DragOps {

    private GenericListClickListener<TaskTurnpoint> itemClickListener;
    private TaskAndTurnpointsViewModel taskAndTurnpointsViewModel;

    public TaskTurnpointsRecyclerViewAdapter(TaskAndTurnpointsViewModel taskAndTurnpointsViewModel) {
        super(taskAndTurnpointsViewModel.getTaskTurnpoints().getValue());
        this.taskAndTurnpointsViewModel = taskAndTurnpointsViewModel;
    }


    @Override
    public TaskTurnpointViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TaskTurnpointView binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()), R.layout.task_turnpoint_detail, parent, false);
        return new TaskTurnpointViewHolder(binding, this);
    }

    @Override
    public void onBindViewHolder(TaskTurnpointViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        //TODO get better way to do following
        holder.getViewDataBinding().setClickListener(itemClickListener);
    }


    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(getItems(), fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        taskAndTurnpointsViewModel.renumberTurnpoints();
        return true;
    }

    @Override
    public void dragCompleted() {
        notifyDataSetChanged();
    }


    @Override
    public void onItemDismiss(int position) {
        // Need to keep track of deleted turnpoints so can delete from db when(if) user saves changes
        taskAndTurnpointsViewModel.deleteTaskTurnpoint(position);
        notifyDataSetChanged();
    }

    public static class TaskTurnpointViewHolder extends GenericViewHolder<TaskTurnpoint, TaskTurnpointView>
            implements ItemTouchHelperViewHolder {

        private TaskTurnpointView viewDataBinding;
        private DragOps dragOps;

        TaskTurnpointViewHolder(TaskTurnpointView bindingView, DragOps dragOps) {
            super(bindingView);
            this.dragOps = dragOps;
            viewDataBinding = bindingView;
        }

        public void onBind(TaskTurnpoint item, int position) {
            viewDataBinding.setTaskTurnpoint(item);
            viewDataBinding.setPosition(position);
        }

        @Override
        public TaskTurnpointView getViewDataBinding() {
            return viewDataBinding;
        }

        @Override
        public void onItemSelected() {
            itemView.setBackgroundColor(itemView.getResources().getColor(R.color.drag_color));
        }

        @Override
        public void onItemClear() {
            itemView.setBackgroundColor(itemView.getResources().getColor(R.color.drag_drop));
            dragOps.dragCompleted();
        }

    }

}
