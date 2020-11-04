package com.fudgetbudget.ui;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.savedstate.SavedStateRegistry;
import androidx.savedstate.SavedStateRegistryController;

import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.fudgetbudget.FudgetBudgetViewModel;
import com.fudgetbudget.R;
import com.fudgetbudget.model.Transaction;

import java.util.List;

import static android.view.View.GONE;

public class StoredTransactionsFragment <T extends Transaction> extends Fragment {

    private FudgetBudgetViewModel<T> mViewModel;
    private ListItemViewBuilder<T> mListItemViewBuilder;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(FudgetBudgetViewModel.class);
        mListItemViewBuilder = new ListItemViewBuilder<T>(getContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View storedTransactionView = inflater.inflate( R.layout.fragment_stored_transactions, container, false );
        ViewGroup expenseList = storedTransactionView.findViewById( R.id.transaction_expense_list );
        ViewGroup incomeList = storedTransactionView.findViewById( R.id.transaction_income_list );


        mViewModel.getCachedTransactions().observe( getViewLifecycleOwner(), transactionsCache -> {
            List<Transaction> expenses = transactionsCache.get(R.string.transaction_type_expense);
            List<Transaction> income = transactionsCache.get(R.string.transaction_type_income);

            expenseList.removeAllViews();
            incomeList.removeAllViews();

            if(expenses != null) expenses.forEach( transaction -> expenseList.addView( getTransactionView(transaction) ));
            if(expenses != null) income.forEach( transaction -> incomeList.addView( getTransactionView( transaction )));

        });



        Button createExpenseButton = storedTransactionView.findViewById( R.id.button_create_expense_transaction );
        createExpenseButton.setOnClickListener( button -> mViewModel.update(Transaction.getInstance(R.string.transaction_type_expense, getContext().getExternalFilesDir(null).getPath())));

        Button createIncomeButton = storedTransactionView.findViewById( R.id.button_create_income_transaction );
        createIncomeButton.setOnClickListener( button -> mViewModel.update(Transaction.getInstance(R.string.transaction_type_income, getContext().getExternalFilesDir(null).getPath())));

        return storedTransactionView;
    }

    @Override
    public void onPause() {
        super.onPause();
        getView().setVisibility(View.INVISIBLE);
    }

    private View getTransactionView(Transaction transaction) {
        ViewGroup listItem = (ViewGroup) getLayoutInflater().inflate( R.layout.layout_transaction, null );

        listItem.findViewById( R.id.line_item_recurrence_short_value ).setVisibility( View.VISIBLE );
        listItem.findViewById( R.id.line_item_balance_value ).setVisibility( GONE );

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
