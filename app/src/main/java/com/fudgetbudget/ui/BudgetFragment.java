package com.fudgetbudget.ui;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fudgetbudget.FudgetBudgetViewModel;
import com.fudgetbudget.R;
import com.fudgetbudget.model.ProjectedTransaction;
import com.fudgetbudget.model.RecordedTransaction;
import com.fudgetbudget.model.Transaction;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class BudgetFragment<T extends Transaction> extends Fragment {

    private FudgetBudgetViewModel<T> mViewModel;
//    private PeriodItemViewBuilder mPeriodItemViewBuilder;
    private ListItemViewBuilder mListItemViewBuilder;
    private LinearLayoutManager mPeriodLayoutManager;
    private PeriodLayoutAdapter mPeriodLayoutAdapter;
    private Map<LocalDate, ArrayList<T>> mProjectionData;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach( context );
        ViewModelProvider.Factory factory = new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new FudgetBudgetViewModel(context);
            }
        };
        mViewModel = new ViewModelProvider(this, factory).get( FudgetBudgetViewModel.class );
        mListItemViewBuilder = new ListItemViewBuilder( context );
//        mPeriodItemViewBuilder = new PeriodItemViewBuilder( context );

        mProjectionData = mViewModel.projectionsByPeriod().getValue();

        mPeriodLayoutManager = new LinearLayoutManager( getContext() );
        mPeriodLayoutAdapter = new PeriodLayoutAdapter( mProjectionData );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View budgetView = inflater.inflate( R.layout.fragment_budget, container, false );
        RecyclerView periodListView = budgetView.findViewById( R.id.period_projection_list );

        periodListView.setLayoutManager( mPeriodLayoutManager );
        periodListView.setAdapter( mPeriodLayoutAdapter );

        mViewModel.projectionsByPeriod().observe( getViewLifecycleOwner(), projectionData -> {
            mProjectionData = projectionData;
            mPeriodLayoutAdapter.notifyDataSetChanged();
        } );

//        TransitionInflater transitionInflater = TransitionInflater.from(requireContext());
//        setExitTransition(transitionInflater.inflateTransition( android.R.transition.slide_bottom ));

        return budgetView;
    }

//    private void setPageHeader(View pageLayout) {
//
//        TextView currentBalanceView = pageLayout.findViewById( R.id.current_recorded_balance );
//        Double currentBalance = mViewModel.getCurrentBalance();
//        currentBalanceView.setText( formatUtility.formatCurrency(currentBalance));
//
//        TextView expendableFundsView = pageLayout.findViewById( R.id.current_expendable_funds );
//        Double expendableFunds = mViewModel.getExpendableValue();
//        expendableFundsView.setText( formatUtility.formatCurrency(expendableFunds));
//
//    }
//
//    private void setPageLists(View pageLayout) {
//        ViewGroup projectionList = pageLayout.findViewById( R.id.period_projection_list );
//
//        projectionList.removeAllViews();
//        ArrayList<Object[]> data = mProjectionsWithBalancesByPeriod;
//        buildPeriodListView( projectionList, data );
//    }

//    private void buildPeriodListView(ViewGroup periodListView, ArrayList<Object[]> listData) {
//
//        for( Object[] item : listData){
//            ViewGroup periodItemView;
//            LocalDate periodStartDate;
//            List periodBalanceData;
//            Map periodTransactionsWithBalance;
//
//            if( !(item[0] instanceof LocalDate) || !(item[1] instanceof List) || !(item[2] instanceof Map)) throw new Error( "unknown data type" );
//            else {
//                periodStartDate = (LocalDate)item[0];
//                periodBalanceData = (List)item[1];
//                periodTransactionsWithBalance = (Map)item[2];
//            }
//
//            periodItemView = (ViewGroup) getLayoutInflater().inflate( R.layout.period_item_projected, null );
//
//            mPeriodItemViewBuilder.setPeriodItemHeader( periodItemView.findViewById( R.id.period_header ), periodStartDate );
//
//            ViewGroup periodListHeader = (ViewGroup) getLayoutInflater().inflate(R.layout.layout_transaction, null);
//            mListItemViewBuilder.setLineItemHeader( periodListHeader );
//
//            ViewGroup periodList = periodItemView.findViewById( R.id.period_projection_list );
//            periodList.addView( periodListHeader );
//            buildPeriodListView( periodList, periodTransactionsWithBalance);
//            mPeriodItemViewBuilder.setPeriodItemBalances( periodItemView, periodBalanceData );
//
//
//            periodItemView.findViewById( R.id.period_projection_list ).setVisibility( VISIBLE );
//            periodItemView.findViewById( R.id.period_list_item_balances ).setVisibility( GONE );
//            periodItemView.findViewById( R.id.period_header ).setOnClickListener( v -> {
//                View dataList = periodItemView.findViewById( R.id.period_projection_list );
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
//
//            periodListView.addView( periodItemView );
//        }
//    }

//    private void buildPeriodListView(ViewGroup listView, Map<?,?> periodTransactionsWithBalance) {
//        for(Map.Entry item : periodTransactionsWithBalance.entrySet() ){
//            if( !(item.getKey() instanceof Transaction)
//                    || !(item.getValue() instanceof Double)) throw new Error("unknown data type");
//            Transaction transaction = (Transaction) item.getKey();
//            Double balance = (Double) item.getValue();
//
//            listView.addView( inflateNewPeriodListItem(listView, transaction, balance) );
//        }
//
//    }

//    private ViewGroup inflateNewPeriodListItem(ViewGroup listView, Transaction transaction, Double balance){
//        ViewGroup listItem = (ViewGroup) getLayoutInflater().inflate( R.layout.layout_transaction, null );
//        ViewGroup cardView = listItem.findViewById(R.id.card_view);
//        ViewGroup lineView = listItem.findViewById( R.id.line_item );
//
//        cardView.removeView( cardView.findViewById( R.id.property_layout_recurrence_switch ) );
//        cardView.removeView( cardView.findViewById( R.id.property_layout_recurrence_editor ) );
//
//        mListItemViewBuilder.setLineItemPropertyViews( listItem.findViewById( R.id.line_item ), transaction );
//        mListItemViewBuilder.setLineItemAlert( (ViewGroup) listItem.findViewById( R.id.line_item ), transaction, balance, mViewModel.getBalanceThreshold() );
//        mListItemViewBuilder.setCardLayoutPropertyViews( listItem.findViewById( R.id.card_view ), transaction, false );
//
//        initializeCardButtons( listView, (ViewGroup) listItem, transaction );
//
//        TextView balanceView = listItem.findViewById( R.id.line_item_balance_value );
//        if(balanceView != null) balanceView.setText( formatUtility.formatCurrency( balance ));
//
//        lineView.setOnClickListener( v -> mListItemViewBuilder.toggleCardVisibility( listItem ) );
//        return listItem;
//    }

    private class PeriodLayoutAdapter extends RecyclerView.Adapter<PeriodLayoutAdapter.ViewHolder> {

        private final List<LocalDate> mPeriodData;
        private final ArrayList<ArrayList<T>> mPeriodProjections;

        PeriodLayoutAdapter(Map<LocalDate, ArrayList<T>> periodData){
            mPeriodData = new ArrayList<>( periodData.keySet() );
            mPeriodProjections = new ArrayList<>( periodData.values() );
        }


        class ViewHolder extends RecyclerView.ViewHolder {
            TextView periodTitle;
            RecyclerView projectionList;


            ViewHolder(View itemView) {
                super(itemView);
                periodTitle = itemView.findViewById( R.id.projection_period_header_date );
                projectionList = itemView.findViewById( R.id.period_projection_list );
            }
        }

        @NonNull
        @Override
        public PeriodLayoutAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = getLayoutInflater().inflate(R.layout.period_item_projected, parent, false);
            return new PeriodLayoutAdapter.ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull PeriodLayoutAdapter.ViewHolder holder, int position) {
            LocalDate period = mPeriodData.get( position );
            ArrayList<T> projections = mPeriodProjections.get( position );

            holder.periodTitle.setText(period.format( DateTimeFormatter.ofPattern( "MMMM yyyy" ) ));

            RecyclerView projectionListView = holder.projectionList;
            projectionListView.setLayoutManager( new LinearLayoutManager(getContext()) );
            projectionListView.setAdapter( new ProjectionListAdapter( projections ) );

        }

        @Override
        public int getItemCount() {
            return mPeriodData.size();
        }


        class ProjectionListAdapter extends RecyclerView.Adapter<ProjectionListAdapter.ViewHolder>{
            ArrayList<T> mListData;

            public ProjectionListAdapter(ArrayList<T> projections) {
                mListData = projections;
            }

            class ViewHolder extends RecyclerView.ViewHolder {
                TextView lineItemLabel;
                TextView lineItemDate;
                TextView lineItemAmount;
                TextView lineItemBalance;
                EditText labelValue;
                EditText noteValue;
                EditText amountValue;
                Button dateValue;
                Button recurrenceSwitch;



                ViewHolder(View itemView) {
                    super(itemView);
                    lineItemLabel = itemView.findViewById( R.id.line_item_label_value );
                    lineItemDate = itemView.findViewById( R.id.line_item_date_value );
                    lineItemAmount = itemView.findViewById( R.id.line_item_amount_value );
                }
            }

            @NonNull
            @Override
            public ProjectionListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View itemView = getLayoutInflater().inflate( R.layout.layout_transaction, parent, false );
                itemView.setBackgroundResource(R.drawable.list_item_background);
                itemView.setOnClickListener( view -> {
                    View cardView = view.findViewById( R.id.card_view );
                    View cardButtons = view.findViewById( R.id.card_buttons );
                    if(cardView.getVisibility() != VISIBLE) {
                        cardView.setVisibility( VISIBLE );
                        cardButtons.setVisibility( VISIBLE );
                        view.setBackgroundResource(R.drawable.list_item_background_selected);

                    }
                    else {
                        cardView.setVisibility( GONE );
                        cardButtons.setVisibility( GONE );
                        view.setBackgroundResource(R.drawable.list_item_background);
                    }
                } );
                return new ProjectionListAdapter.ViewHolder( itemView );
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                T listItem = mListData.get( position );
                holder.lineItemLabel.setText(listItem.getProperty( R.string.label_tag ).toString());
                holder.lineItemDate.setText(listItem.getProperty( R.string.date_tag ).toString());
                holder.lineItemAmount.setText(listItem.getProperty( R.string.amount_tag ).toString());

                initializeCardButtons( (ViewGroup) holder.itemView, listItem );
            }

            @Override
            public int getItemCount() {
                return mListData.size();
            }


            void initializeCardButtons(ViewGroup listItem, T transaction){
                ViewGroup buttonsView = listItem.findViewById( R.id.card_buttons );
                ViewGroup cardDrawer = listItem.findViewById( R.id.card_view );

                Button modifyButton = buttonsView.findViewById( R.id.modify_transaction_button );
                Button resetProjectionButton = buttonsView.findViewById( R.id.reset_projections_button );
                Button updateButton = buttonsView.findViewById( R.id.editor_update_button );
                Button cancelButton = buttonsView.findViewById( R.id.editor_cancel_button );
                Button deleteButton = buttonsView.findViewById( R.id.editor_delete_button );
                Button recordTransactionButton = buttonsView.findViewById( R.id.record_transaction_button );

                if(modifyButton != null) modifyButton.setOnClickListener( v -> {
                    mListItemViewBuilder.setCardLayoutPropertyViews( cardDrawer, transaction, true );

                    cancelButton.setVisibility( VISIBLE );
                    modifyButton.setVisibility( GONE );
                    updateButton.setVisibility( VISIBLE );

                    if(transaction instanceof ProjectedTransaction) {
                        if(resetProjectionButton != null) resetProjectionButton.setVisibility( VISIBLE );
                        if(deleteButton != null) deleteButton.setVisibility( GONE );
                    }
                    else {
                        if(deleteButton != null) deleteButton.setVisibility( VISIBLE );
                        if(resetProjectionButton != null) resetProjectionButton.setVisibility( GONE );
                    }

                } );

                if(resetProjectionButton != null) {
                    if( !(transaction instanceof ProjectedTransaction)) resetProjectionButton.setVisibility( GONE );
                    else resetProjectionButton.setOnClickListener( v-> {
                        if(transaction instanceof ProjectedTransaction)
                            mViewModel.reset( (ProjectedTransaction)transaction);
                        mListItemViewBuilder.setCardLayoutPropertyViews( listItem, transaction, false );
                        mListItemViewBuilder.setLineItemPropertyViews( listItem, transaction );
                    } );
                }

                if(updateButton != null) updateButton.setOnClickListener( v -> {
                    mListItemViewBuilder.updateTransactionFromEditorView( listItem.findViewById( R.id.card_view ), transaction );
                    Toast toast;
                    if(mViewModel.update( transaction )) {
                        toast = Toast.makeText(getContext(), "Update successful", Toast.LENGTH_SHORT);

                        //                View pageView = getActivity().findViewById( R.id.page_balanceSheet );
                        //                setPageHeader( pageView );
                        //                setPageLists( pageView );
                        //                viewModel.setLineItemPropertyViews( listItem, transaction );
                        //                viewModel.setCardLayoutPropertyViews( listItem, transaction, false );
                        //
                        //                listItem.findViewById( R.id.editor_cancel_button ).callOnClick();

                        //  getActivity().recreate();
                    } else toast = Toast.makeText( getContext(), "Error updating item", Toast.LENGTH_LONG );

                    toast.show();

                } );

                if(cancelButton != null) cancelButton.setOnClickListener( v-> {
                    mListItemViewBuilder.setCardLayoutPropertyViews( cardDrawer, transaction, false );

                    if(modifyButton != null)  modifyButton.setVisibility( VISIBLE );
                    if(updateButton != null)  updateButton.setVisibility( GONE );
                    if(resetProjectionButton != null)  resetProjectionButton.setVisibility( GONE );
                    if(deleteButton != null)  deleteButton.setVisibility( GONE );
                    if(cancelButton != null)  cancelButton.setVisibility( GONE );
                } );

                if(deleteButton != null) deleteButton.setOnClickListener( v -> {
                    mViewModel.delete( transaction );
                } );

                if(recordTransactionButton != null) {
                    LocalDate transactionDate = (LocalDate) transaction.getProperty( R.string.date_tag );
                    if( transaction instanceof RecordedTransaction || transactionDate.isAfter( LocalDate.now() ))
                        recordTransactionButton.setVisibility( GONE );
                    else recordTransactionButton.setOnClickListener( v -> {
                        mViewModel.reconcile(transaction);
                    } );
                }
            }
        }

    }
}
