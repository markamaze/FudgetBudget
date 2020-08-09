package com.fudgetbudget;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fudgetbudget.model.RecordedTransaction;
import com.fudgetbudget.model.Transaction;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity<T extends Transaction> extends AppCompatActivity {

    private BudgetModel<T> budgetModel;
    private ViewModel viewModel;
    OnSwipeTouchListener pageViewTouchListener;
    private int activePageIndex;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        this.pageViewTouchListener = new OnSwipeTouchListener( this );

        budgetModel = new BudgetModel<T>( this );
        viewModel = new ViewModel(this, budgetModel);

//        ((ImageButton)findViewById( R.id.nav_tab_tools_icon )).setImageDrawable( getDrawable( android.R.drawable.ic_menu_compass ) );
//        ((ScrollView)findViewById( R.id.page_view )).addView( balanceSheetPage() );

        findViewById( R.id.page_view ).setOnTouchListener( this.pageViewTouchListener );


        View transactionTab = findViewById( R.id.nav_tab_transactions );
        View recordsTab = findViewById( R.id.nav_tab_records );
        View balanceSheetTab = findViewById( R.id.nav_tab_balancesheet );
        View toolsTab = findViewById( R.id.nav_tab_tools_icon );

        transactionTab.setOnClickListener( v -> {
            this.activePageIndex = 0;
            loadActivePage();
        } );
        recordsTab.setOnClickListener( v -> {
            this.activePageIndex = 2;
            loadActivePage();
        }  );
        balanceSheetTab.setOnClickListener( v -> {
            this.activePageIndex = 1;
            loadActivePage();
        }  );
        toolsTab.setOnClickListener( v -> {
            this.activePageIndex = 3;
            loadActivePage();
        }  );

        this.activePageIndex = 1;
        loadActivePage();

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev){
        pageViewTouchListener.getGestureDetector().onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    public void activePageSwiped(boolean swipedLeft){
        int pageIndex = this.activePageIndex;

        if(pageIndex == 0 && !swipedLeft) return;
        if(pageIndex == 3 && swipedLeft) return;

        if(swipedLeft) this.activePageIndex++;
        else this.activePageIndex--;

        loadActivePage();
    }
    private void loadActivePage() {
        ScrollView currentPageView = findViewById( R.id.page_view );
        int inctiveTabBackground = getResources().getColor( R.color.colorAccent, null );
        int inactiveTabTextColor = getResources().getColor( R.color.colorPrimaryDark,null );
        int activeTabBackground = getResources().getColor( R.color.colorPrimaryDark, null );
        int activeTabTextColor = getResources().getColor( R.color.colorAccent, null );

        TextView balanceSheetTab = findViewById( R.id.nav_tab_text_balancesheet );
        TextView transactionsTab = findViewById( R.id.nav_tab_text_transactions );
        TextView recordsTab = findViewById( R.id.nav_tab_text_records );
        TextView toolsTab = findViewById( R.id.nav_tab_tools_icon );

        balanceSheetTab.setBackgroundColor( inctiveTabBackground );
        balanceSheetTab.setTextColor( inactiveTabTextColor );
        transactionsTab.setBackgroundColor( inctiveTabBackground );
        transactionsTab.setTextColor( inactiveTabTextColor );
        recordsTab.setBackgroundColor( inctiveTabBackground );
        recordsTab.setTextColor( inactiveTabTextColor );
        toolsTab.setBackgroundColor( inctiveTabBackground );
        toolsTab.setTextColor( inactiveTabTextColor );


        if(activePageIndex == 1) {
            balanceSheetTab.setBackgroundColor( activeTabBackground );
            balanceSheetTab.setTextColor( activeTabTextColor );

            currentPageView.removeAllViews();
            currentPageView.addView( balanceSheetPage() );
        }
        else if(activePageIndex == 0) {
            transactionsTab.setBackgroundColor( activeTabBackground );
            transactionsTab.setTextColor( activeTabTextColor );

            currentPageView.removeAllViews();
            currentPageView.addView( transactionsPage() );
        }
        else if(activePageIndex == 2) {
            recordsTab.setBackgroundColor( activeTabBackground );
            recordsTab.setTextColor( activeTabTextColor );

            currentPageView.removeAllViews();
            currentPageView.addView( recordsPage() );
        }
        else if(activePageIndex == 3) {
            toolsTab.setBackgroundColor( activeTabBackground );
            toolsTab.setTextColor( activeTabTextColor );

            currentPageView.removeAllViews();
            currentPageView.addView( toolsPage() );
        }
    }


    public View transactionsPage(){
        LinearLayout pageLayoutTransactions = (LinearLayout) getLayoutInflater().inflate( R.layout.page_transactions, null );
        ArrayList<Transaction> incomeListData = budgetModel.getTransactionsByType( R.string.transaction_type_income );
        ArrayList<Transaction> expenseListData = budgetModel.getTransactionsByType( R.string.transaction_type_expense );

        //set page header values
        LinearLayout header = pageLayoutTransactions.findViewById( R.id.page_header );
        TextView averageIncomeView = header.findViewById( R.id.average_income );
        TextView averageExpenseView = header.findViewById( R.id.average_expenses );
        TextView avergeNetGainLossView = header.findViewById( R.id.average_gain_loss );
        Double averageIncome = budgetModel.getProjectedAverageValue( R.string.income_tag );
        Double averageExpense = budgetModel.getProjectedAverageValue( R.string.expense_tag );
        Double averageNet = averageIncome - averageExpense;

        averageIncomeView.setText( viewModel.formatCurrency( averageIncome ) + "/month");
        averageExpenseView.setText(viewModel.formatCurrency( averageExpense ) + "/month");
        avergeNetGainLossView.setText( viewModel.formatCurrency( averageNet ) + "/month");

        //build income transaction list
        int[] incomeListColumnTags = new int[]{R.string.date_tag, R.string.label_tag, R.string.amount_tag};
        ViewGroup incomeTransactionList = pageLayoutTransactions.findViewById( R.id.transaction_income_list );
        viewModel.buildListView(incomeTransactionList, incomeListData, incomeListColumnTags );


        //build expense transaction list
        int[] expenseListColumnTags = new int[]{R.string.date_tag, R.string.label_tag, R.string.amount_tag};
        ViewGroup expenseTransactionList = pageLayoutTransactions.findViewById( R.id.transaction_expense_list );
        viewModel.buildListView( expenseTransactionList, expenseListData, expenseListColumnTags );

        //set onclick handlers to create new transactions
        pageLayoutTransactions.findViewById( R.id.button_create_income_transaction )
                .setOnClickListener( v -> viewModel.createTransaction(R.string.transaction_type_income));

        pageLayoutTransactions.findViewById( R.id.button_create_expense_transaction )
                .setOnClickListener( v -> viewModel.createTransaction(R.string.transaction_type_expense));



        return pageLayoutTransactions;
    }
    public View balanceSheetPage() {
        LinearLayout page_layout_balancesheet = (LinearLayout) getLayoutInflater().inflate( R.layout.page_balancesheet, null );

        TextView currentBalanceView = page_layout_balancesheet.findViewById( R.id.current_recorded_balance );
        Double currentBalance = budgetModel.getCurrentBalance();
        currentBalanceView.setText( viewModel.formatCurrency(currentBalance));

        TextView expendableFundsView = page_layout_balancesheet.findViewById( R.id.current_expendable_funds );
        Double expendableFunds = budgetModel.getExpendableValue();
        expendableFundsView.setText( viewModel.formatCurrency(expendableFunds));

        ArrayList<Object[]> projectionListData = budgetModel.getProjectionsWithBalancesByPeriod();
        ViewGroup projectionList = page_layout_balancesheet.findViewById( R.id.projected_balance_list );
        viewModel.buildListView( projectionList, projectionListData );

        return page_layout_balancesheet;
    }
    public View recordsPage() {
        LinearLayout page_records = (LinearLayout) getLayoutInflater().inflate( R.layout.page_records, null );

        TextView currentBalanceView = page_records.findViewById( R.id.current_recorded_balance );
        Double currentBalance = budgetModel.getCurrentBalance();
        currentBalanceView.setText( viewModel.formatCurrency( currentBalance ));

        int[] recordsListColumns = new int[]{R.string.date_tag, R.string.label_tag, R.string.amount_tag};
        ArrayList<RecordedTransaction> recordsListData = budgetModel.getRecords( "all" );
        ViewGroup recordsList = page_records.findViewById( R.id.record_list );

        Collections.reverse(recordsListData);
        viewModel.buildListView( recordsList, recordsListData, recordsListColumns );

        return page_records;
    }

    public View toolsPage() {
        LinearLayout tools_page = (LinearLayout) View.inflate( this, R.layout.page_tools, null );


        return tools_page;
    }
}
