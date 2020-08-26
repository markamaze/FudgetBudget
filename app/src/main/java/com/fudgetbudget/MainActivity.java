package com.fudgetbudget;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fudgetbudget.model.RecordedTransaction;
import com.fudgetbudget.model.Transaction;

import java.time.LocalDate;
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

        setSupportActionBar( findViewById( R.id.action_bar ) );
        getSupportActionBar().setTitle( getTitle() );

        budgetModel = new BudgetModel<T>( this );
        viewModel = new ViewModel(this, budgetModel);

        findViewById( R.id.page_view ).setOnTouchListener( this.pageViewTouchListener );

        View transactionTab = findViewById( R.id.nav_tab_transactions );
        View recordsTab = findViewById( R.id.nav_tab_records );
        View balanceSheetTab = findViewById( R.id.nav_tab_balancesheet );
        View toolsTab = findViewById( R.id.nav_tab_tools );

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
        ViewGroup currentPageView = findViewById( R.id.page_view );

        Button transactionsTab = findViewById( R.id.nav_tab_transactions );
        Button balanceSheetTab = findViewById( R.id.nav_tab_balancesheet );
        Button recordsTab = findViewById( R.id.nav_tab_records );
        Button toolsTab = findViewById( R.id.nav_tab_tools );



        transactionsTab.setTextAppearance( R.style.NavButton_Inactive );
        balanceSheetTab.setTextAppearance( R.style.NavButton_Inactive );
        recordsTab.setTextAppearance( R.style.NavButton_Inactive );
        toolsTab.setTextAppearance( R.style.NavButton_Inactive );

        currentPageView.removeAllViews();

        if(activePageIndex == 1) {
            balanceSheetTab.setTextAppearance( R.style.NavButton_Active );
            currentPageView.addView( balanceSheetPage() );
        }
        else if(activePageIndex == 0) {
            transactionsTab.setTextAppearance( R.style.NavButton_Active );
            currentPageView.addView( transactionsPage() );
        }
        else if(activePageIndex == 2) {
            recordsTab.setTextAppearance( R.style.NavButton_Active );
            currentPageView.addView( recordsPage() );
        }
        else if(activePageIndex == 3) {
            toolsTab.setTextAppearance( R.style.NavButton_Active );
            currentPageView.addView( toolsPage() );
        }



    }


    public View transactionsPage(){

        getSupportActionBar().setTitle( "Fudget Budget: Budget" );


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
        viewModel.buildTransactionListView(incomeTransactionList, incomeListData, incomeListColumnTags );


        //build expense transaction list
        int[] expenseListColumnTags = new int[]{R.string.date_tag, R.string.label_tag, R.string.amount_tag};
        ViewGroup expenseTransactionList = pageLayoutTransactions.findViewById( R.id.transaction_expense_list );
        viewModel.buildTransactionListView( expenseTransactionList, expenseListData, expenseListColumnTags );

        //set onclick handlers to create new transactions
        pageLayoutTransactions.findViewById( R.id.button_create_income_transaction )
                .setOnClickListener( v -> {
                    ViewParent parent = v.getParent().getParent();
                    if(parent instanceof LinearLayout){
                        LinearLayout incomeList = (LinearLayout) parent;

                        T newTransaction = (T) Transaction.getInstance( R.string.transaction_type_income, this.getExternalFilesDir( null ).getAbsolutePath() );
                        newTransaction.setProperty( R.string.label_tag, "new income" );
                        budgetModel.update( newTransaction );
                        ViewGroup newListItem = (ViewGroup) View.inflate( this, R.layout.transaction_list_item, null );
                        ((TextView)newListItem.findViewById( R.id.date_value )).setText( viewModel.formatDate( R.string.date_string_dayofweekanddate, (LocalDate) newTransaction.getProperty( R.id.date_value ) ) );
//                        ((TextView) newListItem.findViewById( R.id.label_value )).setText( newTransaction.getProperty( R.string.date ))

                        incomeList.addView( newListItem, 2 );
                    }



                });

        pageLayoutTransactions.findViewById( R.id.button_create_expense_transaction )
                .setOnClickListener( v -> {
                    T newTransaction = (T) Transaction.getInstance( R.string.transaction_type_expense, this.getExternalFilesDir( null ).getAbsolutePath() );
                    newTransaction.setProperty( R.string.label_tag, "new expense" );
                    budgetModel.update( newTransaction );
                    ViewGroup newListItem = (ViewGroup) View.inflate( this, R.layout.transaction_list_item, null );
                    viewModel.setTransactionPropertyViews(newListItem, newTransaction, true);
                    ((ViewGroup)pageLayoutTransactions.findViewById( R.id.transaction_income_list )).addView( newListItem, 2 );
                });



        return pageLayoutTransactions;
    }
    public View balanceSheetPage() {
        getSupportActionBar().setTitle( "Fudget Budget: Balance Sheet" );

        LinearLayout page_layout_balancesheet = (LinearLayout) getLayoutInflater().inflate( R.layout.page_balancesheet, null );

        TextView currentBalanceView = page_layout_balancesheet.findViewById( R.id.current_recorded_balance );
        Double currentBalance = budgetModel.getCurrentBalance();
        currentBalanceView.setText( viewModel.formatCurrency(currentBalance));

        TextView expendableFundsView = page_layout_balancesheet.findViewById( R.id.current_expendable_funds );
        Double expendableFunds = budgetModel.getExpendableValue();
        expendableFundsView.setText( viewModel.formatCurrency(expendableFunds));

        ArrayList<Object[]> projectionListData = budgetModel.getProjectionsWithBalancesByPeriod();
        ViewGroup projectionList = page_layout_balancesheet.findViewById( R.id.projected_balance_list );
        viewModel.buildPeriodProjectionListView( projectionList, projectionListData );

        return page_layout_balancesheet;
    }
    public View recordsPage() {
        getSupportActionBar().setTitle( "Fudget Budget: Records" );

        LinearLayout page_records = (LinearLayout) getLayoutInflater().inflate( R.layout.page_records, null );

        TextView currentBalanceView = page_records.findViewById( R.id.current_recorded_balance );
        Double currentBalance = budgetModel.getCurrentBalance();
        currentBalanceView.setText( viewModel.formatCurrency( currentBalance ));

        int[] recordsListColumns = new int[]{R.string.date_tag, R.string.label_tag, R.string.amount_tag};
        ArrayList<RecordedTransaction> recordsListData = budgetModel.getRecords( "all" );
        ViewGroup recordsList = page_records.findViewById( R.id.record_list );

        Collections.reverse(recordsListData);
        viewModel.buildTransactionListView( recordsList, recordsListData, recordsListColumns );

        return page_records;
    }

    public View toolsPage() {
        getSupportActionBar().setTitle( "Fudget Budget: Tools" );

        LinearLayout tools_page = (LinearLayout) View.inflate( this, R.layout.page_tools, null );


        return tools_page;
    }
}
