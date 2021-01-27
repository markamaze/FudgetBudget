package com.fudgetbudget.ui;

import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.HandlerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.fudgetbudget.FudgetBudgetViewModel;
import com.fudgetbudget.R;
import com.fudgetbudget.model.Transaction;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class StoredTransactionsFragment extends Fragment {

    private FudgetBudgetViewModel mViewModel;
    private ListItemViewBuilder mListItemViewBuilder;
    private MutableLiveData<List<Transaction>> vTransactions;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mListItemViewBuilder = new ListItemViewBuilder(getContext());

        if(savedInstanceState != null) vTransactions = new MutableLiveData<>(new ArrayList<>());
        else vTransactions = new MutableLiveData<>(new ArrayList<>());

        Executors.newSingleThreadExecutor().execute( () -> {
            FudgetBudgetViewModel model = new ViewModelProvider(requireActivity()).get(FudgetBudgetViewModel.class);

            HandlerCompat.createAsync(Looper.getMainLooper()).post( () -> {
                mViewModel = model;
                mViewModel.getTransactions().observeForever( transactions -> vTransactions.postValue(transactions));
            } );
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View storedTransactionFragView = inflater.inflate( R.layout.fragment_stored_transactions, container, false );

        View createExpenseButton = storedTransactionFragView.findViewById( R.id.button_create_expense_transaction );
        createExpenseButton.setOnClickListener( button -> mViewModel.update(Transaction.getInstance(R.string.transaction_type_expense, getContext().getExternalFilesDir(null).getPath())));

        View createIncomeButton = storedTransactionFragView.findViewById( R.id.button_create_income_transaction );
        createIncomeButton.setOnClickListener( button -> mViewModel.update(Transaction.getInstance(R.string.transaction_type_income, getContext().getExternalFilesDir(null).getPath())));

        Button deleteTransactions = storedTransactionFragView.findViewById( R.id.tool_delete_transactions );
        deleteTransactions.setOnClickListener( v -> {
            mViewModel.delete(R.string.transaction_type_all);
        } );

        vTransactions.observeForever( transactions -> {
            ViewGroup expenseList = storedTransactionFragView.findViewById( R.id.transaction_expense_list );
            ViewGroup incomeList = storedTransactionFragView.findViewById( R.id.transaction_income_list );

            expenseList.removeAllViews();
            incomeList.removeAllViews();

            Collections.sort(transactions);
            transactions.forEach( transaction -> {
                if(transaction.getIncomeFlag()) incomeList.addView( getTransactionView(transaction, inflater) );
                else expenseList.addView( getTransactionView(transaction, inflater) );
            });
        });
        return storedTransactionFragView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);



    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        getView().setVisibility(View.INVISIBLE);
    }




    private View getTransactionView(Transaction transaction, LayoutInflater inflater) {
        ViewGroup listItem = (ViewGroup) inflater.inflate( R.layout.layout_transaction, null );

        mListItemViewBuilder.setLineItemPropertyViews( listItem, transaction );
        mListItemViewBuilder.setCardLayoutPropertyViews( listItem, transaction, false );
        listItem.setOnClickListener( v -> mListItemViewBuilder.toggleCardVisibility( listItem ) );
        initializeCardButtons( listItem, transaction );

        return listItem;
    }
    private <T extends Transaction> void initializeCardButtons(ViewGroup lineItemView, Transaction transaction){
        ViewGroup buttonsView = lineItemView.findViewById( R.id.card_buttons );
        ViewGroup cardDrawer = lineItemView.findViewById( R.id.card_view );

        Button modifyButton = buttonsView.findViewById( R.id.modify_transaction_button );
        Button resetProjectionButton = buttonsView.findViewById( R.id.reset_projections_button );
        Button updateButton = buttonsView.findViewById( R.id.editor_update_button );
        Button cancelButton = buttonsView.findViewById( R.id.editor_cancel_button );
        Button deleteButton = buttonsView.findViewById( R.id.editor_delete_button );

        Button recordButton = buttonsView.findViewById( R.id.record_transaction_button );
        if(recordButton != null) recordButton.setVisibility( GONE );

        if(modifyButton != null) modifyButton.setOnClickListener( v -> {
            mListItemViewBuilder.setCardLayoutPropertyViews( cardDrawer, transaction, true );

            cancelButton.setVisibility( View.VISIBLE );
            modifyButton.setVisibility( GONE );
            updateButton.setVisibility( View.VISIBLE );
            resetProjectionButton.setVisibility( View.VISIBLE );
            deleteButton.setVisibility( View.VISIBLE );
        } );

        if(resetProjectionButton != null) resetProjectionButton.setOnClickListener( v-> {
            mViewModel.reset( transaction );
            mListItemViewBuilder.setLineItemPropertyViews( lineItemView, transaction );
            mListItemViewBuilder.setCardLayoutPropertyViews( lineItemView, transaction, false );
        } );

        if(updateButton != null) updateButton.setOnClickListener( v -> {
            mListItemViewBuilder.updateTransactionFromEditorView( lineItemView.findViewById( R.id.card_view ), transaction );
            mViewModel.update( transaction );

            mListItemViewBuilder.setLineItemPropertyViews( lineItemView, transaction );
            lineItemView.findViewById( R.id.line_item ).callOnClick();
        } );

        if(cancelButton != null) cancelButton.setOnClickListener( v-> {
            mListItemViewBuilder.setCardLayoutPropertyViews( cardDrawer, transaction, false );

            modifyButton.setVisibility( View.VISIBLE );
            updateButton.setVisibility( GONE );
            resetProjectionButton.setVisibility( GONE );
            deleteButton.setVisibility( GONE );
            cancelButton.setVisibility( GONE );

//            View layout_date = cardDrawer.findViewById( R.id.property_layout_date ).findViewById( R.id.date_value );
//            if(layout_date != null) layout_date.setOnClickListener( null );
        } );

        if(deleteButton != null) deleteButton.setOnClickListener( v -> {
            mViewModel.delete( transaction );

//            listView.removeView( lineItemView );
        } );

    }
}
