package com.fudgetbudget.ui;

import androidx.core.os.HandlerCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.fudgetbudget.FudgetBudgetViewModel;
import com.fudgetbudget.R;
import com.fudgetbudget.model.Transaction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class RecordsFragment<T extends Transaction> extends Fragment {

    private FudgetBudgetViewModel<T> mViewModel;
    private ListItemViewBuilder mListItemViewBuilder;
    private PeriodItemViewBuilder mPeriodItemViewBuilder;
    private MutableLiveData<List<LocalDate>> vRecordPeriods;
    private Map<LocalDate, MutableLiveData<List<String>>> vPeriodKeys;
    private Map<String, MutableLiveData<T>> vKeyRecord;
    private MutableLiveData<Double> vCurrentBalance;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mListItemViewBuilder = new ListItemViewBuilder( getContext() );
        mPeriodItemViewBuilder = new PeriodItemViewBuilder( getContext() );

        if(savedInstanceState != null){
            vRecordPeriods = new MutableLiveData<>(Arrays.asList(Arrays.asList(savedInstanceState.getStringArray("RECORD_PERIODS"))
                    .stream().map( period -> LocalDate.parse(period, DateTimeFormatter.BASIC_ISO_DATE)).toArray(LocalDate[]::new)));

            vCurrentBalance = new MutableLiveData<>( Double.parseDouble(savedInstanceState.getString("CURRENT_BALANCE") ));

        }
        else {
            vRecordPeriods = new MutableLiveData<>(new ArrayList<>());
            vCurrentBalance = new MutableLiveData<>(0.0);
        }

        Executors.newSingleThreadExecutor().execute(() -> {
//            ViewModelProvider.Factory factory = new ViewModelProvider.Factory() {
//                @NonNull
//                @Override
//                public <V extends ViewModel> V create(@NonNull Class<V> modelClass) {
//                    return (V) new RecordsViewModel<T>( getContext() );
//                }
//            };
//            RecordsViewModel<T> model = new ViewModelProvider(this, factory).get(RecordsViewModel.class);

            FudgetBudgetViewModel<T> model = new ViewModelProvider(requireActivity()).get(FudgetBudgetViewModel.class);
            List<LocalDate> recordPeriods = model.getProjectedPeriods().getValue();
            Double currentBalance = model.getCurrentBalance();
            HandlerCompat.createAsync(Looper.getMainLooper()).post( () -> {
                mViewModel = model;
                vRecordPeriods.postValue(recordPeriods);

//                mViewModel.getRecordPeriods().observe( getViewLifecycleOwner(), periods -> vRecordPeriods.postValue(periods));
            });
        });

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View page_records = inflater.inflate( R.layout.fragment_records, container, false );


        vRecordPeriods.observe( getViewLifecycleOwner(), periods -> buildBudgetRecords(periods));
        vCurrentBalance.observe( getViewLifecycleOwner(), newBalance -> {
            TextView currentBalanceView = page_records.findViewById( R.id.current_recorded_balance );
            currentBalanceView.setText( formatUtility.formatCurrency( newBalance ));
        });

        return page_records;
    }

    private void buildBudgetRecords(List<LocalDate> periods) {
        ViewGroup recordList = requireView().findViewById(R.id.record_list);

        FragmentManager fm = getChildFragmentManager();

        recordList.removeAllViews();
        periods.sort(LocalDate::compareTo);
        Collections.reverse(periods);
        periods.forEach( period -> fm.beginTransaction().add(R.id.record_list, PeriodFragment.getInstance(period)).commitNow() );

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        String[] savedRecordPeriods = vRecordPeriods.getValue().stream().map( period ->
                period.format(DateTimeFormatter.BASIC_ISO_DATE)).toArray(String[]::new);
        outState.putStringArray("RECORD_PERIODS", savedRecordPeriods);
        outState.putString("CURRENT_BALANCE", vCurrentBalance.getValue().toString());
    }

//    private void buildPeriodListView(ViewGroup recordsList, ArrayList<Object[]> recordsWithBalancesByPeriod) {
//        if(recordsWithBalancesByPeriod == null) return;
//
//        for( Object[] item : recordsWithBalancesByPeriod) {
//            ViewGroup periodItemView;
//            LocalDate periodStartDate;
//            HashMap periodBalanceData;
//            ArrayList periodRecords;
//
//            if (!(item[0] instanceof LocalDate) || !(item[1] instanceof HashMap) || !(item[2] instanceof ArrayList))
//                throw new Error( "unknown data type" );
//            else {
//                periodStartDate = (LocalDate) item[0];
//                periodBalanceData = (HashMap) item[1];
//                periodRecords = (ArrayList) item[2];
//            }
//
//            periodItemView = (ViewGroup) getLayoutInflater().inflate( R.layout.period_item_recorded, null );
//            ViewGroup periodList = periodItemView.findViewById( R.id.period_record_list );
//            ViewGroup periodHeader = periodItemView.findViewById( R.id.period_header );
//
//            mPeriodItemViewBuilder.setPeriodItemHeader( periodHeader, periodStartDate );
//            mPeriodItemViewBuilder.setPeriodItemBalances( periodItemView, periodBalanceData );
//
//            buildTransactionListView( periodList, periodRecords );
//
//            periodHeader.setOnClickListener( v -> {
//                View dataList = periodItemView.findViewById( R.id.period_record_list );
//                View balanceList = periodItemView.findViewById( R.id.period_list_item_balances );
//                if(dataList.getVisibility() != VISIBLE){
//                    dataList.setVisibility( VISIBLE );
//                    balanceList.setVisibility( GONE );
//                } else {
//                    dataList.setVisibility( GONE );
//                    balanceList.setVisibility( VISIBLE );
//                }
//            } );
//
//            recordsList.addView( periodItemView );
//        }
//
//    }
//
//    private void buildTransactionListView(ViewGroup listView, ArrayList<T> listData) {
//        ViewGroup listHeader = (ViewGroup) getLayoutInflater().inflate(R.layout.layout_transaction, null);
//        ((ViewGroup)listHeader.findViewById( R.id.line_item )).removeView( listHeader.findViewById( R.id.line_item_balance_value ) );
//        mListItemViewBuilder.setLineItemHeader(listHeader );
//        listView.addView( listHeader );
//
//        for( T item : listData){
//            ViewGroup listLineItem = inflateListItem( listView, (Transaction) item );
//            listView.addView( listLineItem );
//        }
//    }
//
//    private ViewGroup inflateListItem(ViewGroup listView, Transaction transaction) {
//        ViewGroup listItem = (ViewGroup) getLayoutInflater().inflate( R.layout.layout_transaction, null );
//        ViewGroup lineItem = listItem.findViewById( R.id.line_item );
//        ViewGroup cardView = listItem.findViewById(R.id.card_view);
//
//        lineItem.removeView( lineItem.findViewById( R.id.line_item_balance_value ) );
//        cardView.removeView( cardView.findViewById( R.id.property_layout_recurrence_switch ) );
//        cardView.removeView( cardView.findViewById( R.id.property_layout_recurrence_editor ) );
//
//        mListItemViewBuilder.setLineItemPropertyViews( lineItem, transaction );
//        mListItemViewBuilder.setCardLayoutPropertyViews( listItem.findViewById( R.id.card_view ), transaction, false );
//        initializeCardButtons( listView, listItem, transaction );
//        lineItem.setOnClickListener( v -> mListItemViewBuilder.toggleCardVisibility( listItem ) );
//
//        return listItem;
//    }
//
//    private void initializeCardButtons(ViewGroup listView, ViewGroup lineItemView, Transaction transaction){
//        ViewGroup buttonsView = lineItemView.findViewById( R.id.card_buttons );
//        ViewGroup cardDrawer = lineItemView.findViewById( R.id.card_view );
//
//        Button modifyButton = buttonsView.findViewById( R.id.modify_transaction_button );
//        Button resetProjectionButton = buttonsView.findViewById( R.id.reset_projections_button );
//        Button updateButton = buttonsView.findViewById( R.id.editor_update_button );
//        Button cancelButton = buttonsView.findViewById( R.id.editor_cancel_button );
//        Button deleteButton = buttonsView.findViewById( R.id.editor_delete_button );
//        Button recordTransactionButton = buttonsView.findViewById( R.id.record_transaction_button );
//
//        if(recordTransactionButton != null) recordTransactionButton.setVisibility( GONE );
//        if(resetProjectionButton != null) resetProjectionButton.setVisibility( GONE );
//
//        if(modifyButton != null) modifyButton.setOnClickListener( v -> {
//            mListItemViewBuilder.setCardLayoutPropertyViews( cardDrawer, transaction, true );
//
//            cancelButton.setVisibility( VISIBLE );
//            modifyButton.setVisibility( GONE );
//            updateButton.setVisibility( VISIBLE );
//            deleteButton.setVisibility( VISIBLE );
//
//        } );
//
//        if(updateButton != null) updateButton.setOnClickListener( v -> {
//            mListItemViewBuilder.updateTransactionFromEditorView( lineItemView.findViewById( R.id.card_view ), transaction );
//            mViewModel.update( transaction );
//
//            mListItemViewBuilder.setLineItemPropertyViews( lineItemView.findViewById( R.id.line_item ), transaction );
//            mListItemViewBuilder.setCardLayoutPropertyViews( lineItemView.findViewById( R.id.card_view ), transaction, false );
//
//            modifyButton.setVisibility( VISIBLE );
//            updateButton.setVisibility( GONE );
//            deleteButton.setVisibility( GONE );
//            cancelButton.setVisibility( GONE );
//
//            //            View layout_date = cardDrawer.findViewById( R.id.property_layout_date ).findViewById( R.id.date_value );
//            //            if(layout_date != null) layout_date.setOnClickListener( null );
//        } );
//
//        if(cancelButton != null) cancelButton.setOnClickListener( v-> {
//            mListItemViewBuilder.setCardLayoutPropertyViews( cardDrawer, transaction, false );
//
//            modifyButton.setVisibility( VISIBLE );
//            updateButton.setVisibility( GONE );
//            deleteButton.setVisibility( GONE );
//            cancelButton.setVisibility( GONE );
//
//            //            View layout_date = cardDrawer.findViewById( R.id.property_layout_date ).findViewById( R.id.date_value );
//            //            if(layout_date != null) layout_date.setOnClickListener( null );
//        } );
//
//        if(deleteButton != null) deleteButton.setOnClickListener( v -> {
//            mViewModel.delete( transaction );
//            listView.removeView( lineItemView );
//        } );
//
//
//    }

}
