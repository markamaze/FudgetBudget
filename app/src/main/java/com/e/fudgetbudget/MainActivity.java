package com.e.fudgetbudget;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;

import com.e.fudgetbudget.model.RecordedTransaction;
import com.e.fudgetbudget.model.Transaction;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private BudgetModel budgetModel;
    private ViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        budgetModel = new BudgetModel( this );
        viewModel = new ViewModel(this, budgetModel);

        Spinner page_switcher_view = findViewById( R.id.page_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.pages, android.R.layout.simple_list_item_1 );
        adapter.setDropDownViewResource( android.R.layout.simple_list_item_1 );
        page_switcher_view.setAdapter(adapter);
        page_switcher_view.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                ScrollView page_view = (ScrollView) MainActivity.this.findViewById( R.id.page_view );
                page_view.removeAllViews();

                if(pos == 0) page_view.addView( overviewPage() );
                else if(pos == 1) page_view.addView( balanceSheetPage() );
                else if(pos == 2) page_view.addView( transactionsPage() );
                else if(pos == 3) page_view.addView( recordsPage() );
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        } );
    }
    private View overviewPage() {
        LinearLayout overviewPage = (LinearLayout) View.inflate( this, R.layout.page_overview, null);

//        Double currentBalance = budgetModel.getCurrentBalance();
//        Double lowestBalance = budgetModel.getLowestProjectedBalance();
//        Double thresholdValue = budgetModel.getThresholdValue();
//        Double availableFunds;
//        if(lowestBalance < 0) availableFunds = 0.0;
//        else if((lowestBalance-thresholdValue) < 0) availableFunds = 0.0;
//        else availableFunds = lowestBalance - thresholdValue;
//
//        ((TextView)overviewPage.findViewById( R.id.current_balance_value )).setText( String.valueOf( currentBalance ));
//        ((TextView)overviewPage.findViewById( R.id.lowest_projected_balance_value )).setText( String.valueOf( lowestBalance ));
//        ((TextView) overviewPage.findViewById( R.id.avaliable_funds_value )).setText( String.valueOf( availableFunds ));
//        ((TextView) overviewPage.findViewById( R.id.balance_threshold_value )).setText( String.valueOf( thresholdValue ));

        return overviewPage;
    }
    private View transactionsPage(){
        LinearLayout pageLayoutTransactions = (LinearLayout) getLayoutInflater().inflate( R.layout.page_transactions, null );

        //build income transaction list
        int[] incomeListColumnTags = new int[]{R.string.date_tag, R.string.label_tag, R.string.amount_tag};
        ArrayList<Transaction> incomeListData = budgetModel.getTransactionsByType( R.string.transaction_type_income );
        ViewGroup incomeTransactionList = pageLayoutTransactions.findViewById( R.id.transaction_income_list );
        viewModel.getListView(incomeTransactionList, incomeListData, incomeListColumnTags );
//        pageLayoutTransactions.addView( incomeTransactionList );


        //build expense transaction list
        int[] expenseListColumnTags = new int[]{R.string.date_tag, R.string.label_tag, R.string.amount_tag};
        ArrayList<Transaction> expenseListData = budgetModel.getTransactionsByType( R.string.transaction_type_expense );
        ViewGroup expenseTransactionList = pageLayoutTransactions.findViewById( R.id.transaction_expense_list );
        viewModel.getListView( expenseTransactionList, expenseListData, expenseListColumnTags );
//        pageLayoutTransactions.addView( expenseTransactionList );


        //build unscheduled transaction list
        int[] unscheduledListColumnTags = new int[]{R.string.label_tag, R.string.amount_tag};
        ArrayList<Transaction> unscheduledListData = budgetModel.getTransactionsByType( R.string.transaction_type_unscheduled );
        ViewGroup unscheduledTransactionList = pageLayoutTransactions.findViewById( R.id.transaction_unscheduled_list );
        viewModel.getListView( unscheduledTransactionList, unscheduledListData, unscheduledListColumnTags );
//        pageLayoutTransactions.addView( unscheduledTransactionList );


        //set onclick handlers to create new transactions
        pageLayoutTransactions.findViewById( R.id.button_create_income_transaction )
                .setOnClickListener( view -> viewModel.createTransaction(R.string.transaction_type_income));

        pageLayoutTransactions.findViewById( R.id.button_create_expense_transaction )
                .setOnClickListener( view -> viewModel.createTransaction(R.string.transaction_type_expense));

        return pageLayoutTransactions;
    }
    private View balanceSheetPage() {
        LinearLayout page_layout_balancesheet = (LinearLayout) getLayoutInflater().inflate( R.layout.page_balancesheet, null );

        ArrayList<Object[]> projectionListData = budgetModel.getProjectionsWithBalances();
        ViewGroup projectionList = page_layout_balancesheet.findViewById( R.id.projected_balance_list );
        viewModel.getListView( projectionList, projectionListData );
//        page_layout_balancesheet.addView( projectionList );

        return page_layout_balancesheet;
    }
    private View recordsPage() {
        LinearLayout page_records = (LinearLayout) getLayoutInflater().inflate( R.layout.page_records, null );

        int[] recordsListColumns = new int[]{R.string.date_tag, R.string.label_tag, R.string.amount_tag};
        ArrayList<RecordedTransaction> recordsListData = budgetModel.getRecords( "all" );
        ViewGroup recordsList = page_records.findViewById( R.id.record_list );
        viewModel.getListView( recordsList, recordsListData, recordsListColumns );
//        page_records.addView( recordsList );

        return page_records;
    }


    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.actionbar_menu_main, menu);
        return true;
    }

}
