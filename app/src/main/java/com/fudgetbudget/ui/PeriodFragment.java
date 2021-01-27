package com.fudgetbudget.ui;

import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.HandlerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.fudgetbudget.FudgetBudgetViewModel;
import com.fudgetbudget.R;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.Executors;

public class PeriodFragment extends Fragment {

    private FudgetBudgetViewModel mViewModel;
    private MutableLiveData<LinkedList<String>> vListItemKeys;
    private LocalDate vPeriod;
    private int viewId;

    public static PeriodFragment getInstance(LocalDate periodDate, String type){
        PeriodFragment fragment = new PeriodFragment();
        Bundle bundle = new Bundle();
        bundle.putString("PERIOD_DATE", periodDate.toString());
        bundle.putString("KEY_TYPE", type);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vPeriod = LocalDate.parse(getArguments().getString("PERIOD_DATE"));
        viewId = View.generateViewId();

        if(savedInstanceState != null) vListItemKeys = new MutableLiveData<>(new LinkedList<>(savedInstanceState.getStringArrayList("PERIOD_KEYS")));
        else vListItemKeys = new MutableLiveData<>(new LinkedList<>());

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View periodView = inflater.inflate(R.layout.period_item_projected, null);
        periodView.setId(viewId);
        TextView periodText = periodView.findViewById(R.id.period_header_date);
        periodText.setText( vPeriod.format( DateTimeFormatter.ofPattern("MMM yyyy")) );

        if( mViewModel != null){
            mViewModel.getProjectionDataKeys(vPeriod).observe(getViewLifecycleOwner(), projectionKeys -> vListItemKeys.postValue(projectionKeys));
        }
        vListItemKeys.observe(getViewLifecycleOwner(), periodKeys -> {

            FragmentManager fm = getChildFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();

            if(getArguments().getString("KEY_TYPE").contentEquals("PROJECTIONS")){
                periodKeys.forEach( key -> {
                    if(fm.findFragmentByTag(key) == null){
                        ProjectionFragment newFragment = ProjectionFragment.getInstance(key);
                        ft.add(R.id.period_list, newFragment, key);
                    }
                });
            }
            else if(getArguments().getString("KEY_TYPE").contentEquals("RECORDS")) {
                periodKeys.forEach( key -> {
                    if(fm.findFragmentByTag(key) == null){
                        RecordFragment newFragment = RecordFragment.getInstance(key);
                        ft.add(R.id.period_list, newFragment, key);
                    }
                });
            }


            fm.getFragments().forEach( fragment -> {
                if(!periodKeys.contains(fragment.getTag())) ft.remove(fragment);
            });

            ft.commit();

        });


        return periodView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Executors.newSingleThreadExecutor().execute(() -> {
            FudgetBudgetViewModel model = new ViewModelProvider(requireActivity()).get(FudgetBudgetViewModel.class);

            HandlerCompat.createAsync(Looper.getMainLooper()).post(() -> {
                mViewModel = model;

                if(getArguments().getString("KEY_TYPE").contentEquals("PROJECTIONS"))
                    mViewModel.getProjectionDataKeys(vPeriod).observe(getViewLifecycleOwner(), projectionKeys -> vListItemKeys.setValue(projectionKeys));

                else if(getArguments().getString("KEY_TYPE").contentEquals("RECORDS"))
                    mViewModel.getRecordDataKeys(vPeriod).observe( getViewLifecycleOwner(), recordKeys -> vListItemKeys.setValue(recordKeys));

                getView().findViewById(viewId).findViewById(R.id.loading_message).setVisibility(View.GONE);

            });
        });

        view.findViewById(R.id.period_header_date).setOnClickListener( v -> {
            View balances = view.findViewById(R.id.period_list_item_balances);
            View list =  view.findViewById(R.id.period_list);

            if(list.getVisibility() != View.VISIBLE) list.setVisibility(View.VISIBLE);
            else list.setVisibility(View.GONE);

            if(balances.getVisibility() != View.VISIBLE) balances.setVisibility(View.VISIBLE);
            else balances.setVisibility(View.GONE);
        });

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putStringArrayList("PERIOD_KEYS", new ArrayList(vListItemKeys.getValue()));
    }
}
