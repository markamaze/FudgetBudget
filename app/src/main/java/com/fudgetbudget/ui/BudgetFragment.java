package com.fudgetbudget.ui;

import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.HandlerCompat;
import androidx.fragment.app.Fragment;

import com.fudgetbudget.FudgetBudgetViewModel;
import com.fudgetbudget.R;
import com.fudgetbudget.model.Transaction;

import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;

public class BudgetFragment<T extends Transaction> extends Fragment {

    private FudgetBudgetViewModel<T> mViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Executors.newSingleThreadExecutor().execute( () -> {
            FudgetBudgetViewModel<T> model = new ViewModelProvider(requireActivity()).get(FudgetBudgetViewModel.class);
            HandlerCompat.createAsync(Looper.getMainLooper()).post( () -> {
                mViewModel = model;
                mViewModel.getProjectedPeriods().observe(getViewLifecycleOwner(), periods -> buildBudgetProjection(periods));
            } );
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(mViewModel != null) mViewModel.getProjectedPeriods().observe(getViewLifecycleOwner(), periods -> buildBudgetProjection(periods));
        return inflater.inflate( R.layout.fragment_budget, container, false );
    }
    @Override
    public void onPause() {
        super.onPause();
        getView().setVisibility(View.INVISIBLE);
//        getParentFragmentManager().beginTransaction().remove(this).add(new BudgetFragmentLoader(), null).commit();
    }


    private void buildBudgetProjection(List<LocalDate> periods) {
        ViewGroup projectionList = requireView().findViewById(R.id.period_projection_list);
        View loadingView = requireView().findViewById(R.id.loading_budget_view);

        FragmentManager fm = getChildFragmentManager();

        projectionList.removeAllViews();
        loadingView.setVisibility(View.VISIBLE);
        periods.sort(LocalDate::compareTo);
        periods.forEach( period -> fm.beginTransaction().add(R.id.period_projection_list, PeriodFragment.getInstance(period)).commitNow() );
        loadingView.setVisibility(View.GONE);

    }

}
