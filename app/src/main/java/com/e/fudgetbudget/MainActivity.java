package com.e.fudgetbudget;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;

public class MainActivity extends AppCompatActivity {

    private TransactionViewModel transactionViewModel;
    private ProjectionViewModel projectionViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        projectionViewModel = new ProjectionViewModel( this );
        transactionViewModel = new TransactionViewModel( this );

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
        ConstraintLayout overviewPage = (ConstraintLayout) View.inflate( this, R.layout.page_overview, null);

        LinearLayout layout_balance_data = overviewPage.findViewById( R.id.balance_lineitem );

        projectionViewModel.loadBalanceView( layout_balance_data );

        return overviewPage;
    }
    private View transactionsPage(){
        LinearLayout pageLayoutTransactions = (LinearLayout) getLayoutInflater().inflate( R.layout.page_transactions, null );
        LinearLayout incomeTransactionList = pageLayoutTransactions.findViewById( R.id.transaction_income_list );
        LinearLayout expenseTransactionList = pageLayoutTransactions.findViewById( R.id.transaction_expense_list );

        transactionViewModel.loadIncomeLineItems(incomeTransactionList.findViewById( R.id.list_body ));
        transactionViewModel.loadExpenseLineItems(expenseTransactionList.findViewById( R.id.list_body ));

        pageLayoutTransactions.findViewById( R.id.button_create_income_transaction )
                .setOnClickListener( view -> transactionViewModel.showEditorDialog(R.string.tag_transaction_income));

        pageLayoutTransactions.findViewById( R.id.button_create_expense_transaction )
                .setOnClickListener( view -> transactionViewModel.showEditorDialog( R.string.tag_transaction_expense ) );

        return pageLayoutTransactions;
    }
    private View balanceSheetPage() {
        LinearLayout page_layout_balancesheet = (LinearLayout) getLayoutInflater().inflate( R.layout.page_balancesheet, null );
        LinearLayout projected_balance_list = page_layout_balancesheet.findViewById( R.id.projected_balance_list_body );
        projectionViewModel.loadProjectionLineItems( projected_balance_list );

        return page_layout_balancesheet;
    }
    private View recordsPage() {
        LinearLayout page_records = (LinearLayout) getLayoutInflater().inflate( R.layout.page_records, null );

        //add all records to view with id: R.id.record_list_body

        return page_records;
    }


    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.actionbar_menu_main, menu);
        return true;
    }

}
