package com.fudgetbudget;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;

import com.fudgetbudget.FudgetBudgetRepo;
import com.fudgetbudget.R;
import com.fudgetbudget.model.ProjectedTransaction;
import com.fudgetbudget.model.RecordedTransaction;
import com.fudgetbudget.model.Transaction;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FudgetBudgetViewModel<T extends Transaction> extends ViewModel {
    private final FudgetBudgetRepo mModelRepo;
    private final String mDirectory;
    private MutableLiveData<ArrayList<Transaction>> cachedExpense;
    private MutableLiveData<ArrayList<Transaction>> cachedIncome;
    private Double mCalculatedAverageIncome;
    private Double mCalculatedAverageExpense;
    private Double mCalculatedAverageExpendable;

    private MutableLiveData<ArrayList<Object[]>> projectionsWithBalancesByPeriod;
    private LinkedHashMap<LocalDate, ArrayList<Double>> projectedBalancesByPeriod;
    private LinkedHashMap<LocalDate, ArrayList<T>> projectedTransactionsByPeriod;
    private Double lowestProjectedBalance;
    private LocalDate lowestProjectedBalanceDate;


    private MutableLiveData<Map<LocalDate, ArrayList<T>>> mProjectionsByPeriod;


    public FudgetBudgetViewModel(Context context){
        mDirectory = context.getExternalFilesDir( null ).getPath();
        mModelRepo = new FudgetBudgetRepo(mDirectory);


        AsyncTaskLoader asyncTaskLoader = new AsyncTaskLoader(context) {
            @Nullable
            @Override
            public Object loadInBackground() {
                return null;

            }
        };
    }

    public LiveData<ArrayList<Transaction>> getCachedExpense() {
        if (cachedExpense == null) {
            Stream<Transaction> expensesStream = mModelRepo.getActiveTransactions().stream().filter( transaction -> !((Transaction)transaction).getIncomeFlag() );
            ArrayList<Transaction> expenses = expensesStream.collect( Collectors.toCollection( ArrayList::new ) );
            Collections.sort( expenses );
            cachedExpense = new MutableLiveData<>(  );
            cachedExpense.setValue( expenses );
        }
        return cachedExpense;
    }
    public LiveData<ArrayList<Transaction>> getCachedIncome() {
        if (cachedIncome == null) {
            Stream<Transaction> incomeStream = mModelRepo.getActiveTransactions().stream().filter( transaction -> ((Transaction)transaction).getIncomeFlag() );
            ArrayList<Transaction> income = incomeStream.collect( Collectors.toCollection( ArrayList::new ) );
            Collections.sort( income );
            cachedIncome = new MutableLiveData<>(  );
            cachedIncome.setValue( income );
        }
        return cachedIncome;
    }



    public void createTransaction(int transaction_type) {
        Transaction newTransaction = Transaction.getInstance( transaction_type, mDirectory );
        ArrayList<Transaction> updatedCache;

        if(update( newTransaction )){
            if(newTransaction.getIncomeFlag()){
                updatedCache = cachedIncome.getValue();
                updatedCache.add( newTransaction );
                Collections.sort( updatedCache );

                cachedIncome.setValue( updatedCache );
            }else {
                updatedCache = cachedExpense.getValue();
                updatedCache.add( newTransaction );
                Collections.sort( updatedCache );

                cachedExpense.setValue( updatedCache );
            }

            //toast success
        } else {
            //toast failure
        }

    }
    public boolean update(Object object) { return mModelRepo.update( object ); }
    public boolean delete(Object object) { return mModelRepo.delete( object ); }
    public boolean reset(Object object) { return mModelRepo.reset( object ); }
    public boolean reconcile(Object object){
        return mModelRepo.reconcile( object );
    }

    public Double calculateAverage(int calculationType){
        switch (calculationType){
            case R.string.calculated_average_income:{
                if(mCalculatedAverageIncome == null){
                    mCalculatedAverageIncome = 0.0;
                }
                return mCalculatedAverageIncome;
            }
            case R.string.calculated_average_expense:{
                if(mCalculatedAverageExpense == null){
                    mCalculatedAverageExpense = 0.0;
                }
                return mCalculatedAverageExpense;
            }
            case R.string.calculated_average_expendable:{
                if(mCalculatedAverageExpendable == null){
                    Double averageIncome = calculateAverage( R.string.calculated_average_income );
                    Double averageExpense = calculateAverage( R.string.calculated_average_expense );
                    mCalculatedAverageExpendable = averageIncome - averageExpense;
                }
                return mCalculatedAverageExpendable;
            }
            default: return null;
        }
    }

    public Double getBalanceThreshold() { return 0.0; }

    public Double getCurrentBalance() { return 0.0; }

    public Double getExpendableValue() { return 0.0; }

    public Object getSettingsValue(int settingValueResource){
        return mModelRepo.getSettingsValue( settingValueResource );
    }



    public MutableLiveData<ArrayList<Object[]>> getProjectionsWithBalancesByPeriod(){
        if(projectionsWithBalancesByPeriod == null) setProjectionsWithBalancesByPeriod();
        return this.projectionsWithBalancesByPeriod;
    }
    private void setProjectionsWithBalancesByPeriod(){
        ArrayList<Object[]> result = new ArrayList<>(  );
        LinkedHashMap<LocalDate, ArrayList<Double>> balances = getProjectedBalancesByPeriod();
        LinkedHashMap<LocalDate, ArrayList<T>> projectedTransactions = getProjectedTransactionsByPeriod();
        final Double[] indexBal = {getCurrentBalance()};
        balances.forEach( (balancePeriodDate, balanceList) -> {
            ArrayList<T> periodTransactions = projectedTransactions.get( balancePeriodDate );
            LinkedHashMap<Transaction, Double> periodProjections = new LinkedHashMap<>(  );

            for( Transaction transaction : periodTransactions) {
                Double endBal;
                if(transaction.getIncomeFlag()) endBal = indexBal[0] + (Double)transaction.getProperty( R.string.amount_tag );
                else endBal = indexBal[0] - (Double)transaction.getProperty( R.string.amount_tag );
                periodProjections.put( transaction, endBal );
                indexBal[0] = endBal;

                setProjectedBalanceFlags(indexBal[0], (LocalDate)transaction.getProperty( R.string.date_tag ));
            }

            Object[] projectionItemData = new Object[]{balancePeriodDate, balanceList, periodProjections};
            result.add( projectionItemData );
        } );
        MutableLiveData<ArrayList<Object[]>> liveDataResult = new MutableLiveData<>(  );
        liveDataResult.setValue( result );
        projectionsWithBalancesByPeriod = liveDataResult;
    }


    LinkedHashMap<LocalDate, ArrayList<Double>> getProjectedBalancesByPeriod(){
        if(projectedBalancesByPeriod == null) setProjectedBalancesByPeriod();
        return projectedBalancesByPeriod;
    }
    private void setProjectedBalancesByPeriod(){
        LinkedHashMap<LocalDate, ArrayList<Double>> result = new LinkedHashMap<>();
        double tempCurrentBalance = this.getCurrentBalance();

        LinkedHashMap<LocalDate, ArrayList<T>> projectedTransactionByPeriod = getProjectedTransactionsByPeriod();

        for(  Map.Entry<LocalDate, ArrayList<T>> periodEntry: projectedTransactionByPeriod.entrySet()){
            LocalDate period = periodEntry.getKey();
            ArrayList<T> projectionList = periodEntry.getValue();
            double beginningBalance, endingBalance, incomeSum, expenseSum;

            beginningBalance = tempCurrentBalance;
            incomeSum = 0;
            expenseSum = 0;

            for(T projection : projectionList){
                if( projection.getIncomeFlag() ) incomeSum += (Double)projection.getProperty(R.string.amount_tag );
                else expenseSum += (Double)projection.getProperty( R.string.amount_tag );
            }

            endingBalance = beginningBalance + incomeSum - expenseSum;

            ArrayList<Double> balanceArray = new ArrayList<>(  );
            balanceArray.add(beginningBalance);
            balanceArray.add( incomeSum );
            balanceArray.add( expenseSum );
            balanceArray.add( endingBalance );

            result.put( period, balanceArray );
            tempCurrentBalance = endingBalance;
        }


        projectedBalancesByPeriod = result;
    }


    LinkedHashMap<LocalDate, ArrayList<T>> getProjectedTransactionsByPeriod(){
        if(this.projectedTransactionsByPeriod == null) setProjectedTransactionsByPeriod();
        return this.projectedTransactionsByPeriod;
    }
    private void setProjectedTransactionsByPeriod() {
        LinkedHashMap<LocalDate, ArrayList<T>> result = new LinkedHashMap<>();
        ArrayList<T> allProjections = new ArrayList<>(  );
        LocalDate cutoffDate = mModelRepo.getCutoffDate();
        LocalDate currentDate = LocalDate.now();
        LocalDate firstProjectedDate;


        mModelRepo.getActiveTransactions().forEach( transaction -> {
            ArrayList<T> projectedTransactions = getProjections( (Transaction)transaction );

            allProjections.addAll( projectedTransactions );
        } );

        firstProjectedDate = currentDate.with( MonthDay.of( currentDate.getMonth(), 1 ) );

        int periodsToProject = (int) mModelRepo.getSettingsValue( R.string.setting_value_periods_to_project );
        for( int index = 0; index <= periodsToProject; index ++){
            LocalDate indexDate = firstProjectedDate.plusMonths( index );
            result.put( indexDate, new ArrayList<>(  ));
        }

        while(!allProjections.isEmpty()){
            T projection = allProjections.remove( 0 );
            LocalDate projectedDate = (LocalDate) projection.getProperty( R.string.date_tag );
            if (projectedDate.isAfter( cutoffDate )) continue;

            if(projectedDate.isBefore( firstProjectedDate )){
                int index = 0;
                while(projectedDate.isBefore( firstProjectedDate )){
                    LinkedHashMap<LocalDate, ArrayList<T>> result_copy = new LinkedHashMap<>(  );
                    index++;
                    firstProjectedDate = firstProjectedDate.minusMonths( index );
                    result_copy.put( firstProjectedDate, new ArrayList<>(  ));
                    result.forEach( (date, projectionList) -> result_copy.put( date, projectionList ) );
                    result = result_copy;
                }

            }


            LocalDate resultInsertionDate = null;
            int keyIndex = 0;
            Object[] keySet = result.keySet().toArray();

            while(resultInsertionDate == null && keyIndex < keySet.length){
                LocalDate firstDayOfPeriod = ((LocalDate)keySet[keyIndex]);
                LocalDate firstDayOfNextPeriod;
                if(keyIndex == keySet.length -1) firstDayOfNextPeriod = cutoffDate;
                else firstDayOfNextPeriod = ((LocalDate)keySet[keyIndex + 1]);

                if( projectedDate.isAfter( firstDayOfPeriod.minusDays( 1 ) ) && projectedDate.isBefore( firstDayOfNextPeriod ) )
                    resultInsertionDate = (LocalDate) keySet[keyIndex];
                keyIndex++;
            }

            if(resultInsertionDate != null && !resultInsertionDate.isAfter( cutoffDate )){
                ArrayList<T> resultSet = result.get( resultInsertionDate );

                resultSet.add( projection );
                Collections.sort( resultSet );
                result.replace( resultInsertionDate, resultSet );
            }


        }

        projectedTransactionsByPeriod = result;
    }


    void setProjectedBalanceFlags(Double balance, LocalDate balanceDate){
        ArrayList<RecordedTransaction> records = getRecords( "all" );
        if(this.lowestProjectedBalance == null) {
            this.lowestProjectedBalance = getCurrentBalance();
            if(records.isEmpty()) this.lowestProjectedBalanceDate = LocalDate.now();
            else this.lowestProjectedBalanceDate = (LocalDate)(records.get( records.size()-1 ).getProperty( R.string.date_tag ));
        }
        if(balance < this.lowestProjectedBalance) {
            this.lowestProjectedBalance = balance;
            this.lowestProjectedBalanceDate = balanceDate;
        }

        //add setting of flags for notification of going below threshold or zero balances
    }

    public ArrayList<T> getProjections(Transaction transaction) {
        //this will take the transaction and return a list of all projected transactions to the cutoff date
        //list will generate projection when one is not found in storage, will load stored projection if found

        ArrayList<T> projections = new ArrayList<>(  );
        String recurrance = (String) transaction.getProperty( R.string.recurrence_tag );

        if(recurrance == null || recurrance.contentEquals( "0-0-0-0-0-0-0" )) projections.add( (T)transaction );
        else {
            LocalDate indexDate = (LocalDate) transaction.getProperty( R.string.date_tag );
            LinkedHashMap<LocalDate, ProjectedTransaction> storedProjections = mModelRepo.getTransactionProjections( transaction );


            if(storedProjections.size() > 0){
                LocalDate firstStoredProjectionDate = (LocalDate) storedProjections.keySet().toArray()[0];
                if(firstStoredProjectionDate.isBefore( indexDate )) indexDate = firstStoredProjectionDate;
            }

            while(!indexDate.isAfter( mModelRepo.getCutoffDate() )){
                ProjectedTransaction projection = storedProjections.get( indexDate );

                if(projection == null) projection = ProjectedTransaction.getInstance( transaction, indexDate );
                projections.add( (T) projection );
                indexDate = mModelRepo.getNextProjectedDate(transaction.getProperty( R.string.recurrence_tag ).toString(), indexDate);
            }

        }

        return projections;
    }

    public ArrayList<RecordedTransaction> getRecords(Object type){
        return mModelRepo.getRecordedTransactions();
    }

    public ArrayList<T> getOverdueTransactions(boolean includeCurrentDue) {
        ArrayList<T> result = new ArrayList<>(  );
        return result;
    }

    public ArrayList<Transaction> getActiveTransactions() {
        ArrayList<Transaction> result = new ArrayList<>(  );
        ArrayList<Transaction> income = new ArrayList<>( cachedIncome.getValue() );
        ArrayList<Transaction> expense = new ArrayList<>( cachedExpense.getValue() );
        result.addAll( income );
        result.addAll( expense );

        return result;
    }

    public ArrayList<Object[]> getRecordsWithBalancesByPeriod() {


        return new ArrayList<Object[]>();
    }




    public LiveData<Map<LocalDate, ArrayList<T>>> projectionsByPeriod(){
        if(mProjectionsByPeriod == null){
            LinkedHashMap<LocalDate, ArrayList<T>> result = new LinkedHashMap<>(  );
            ArrayList<Transaction> expenses = getCachedExpense().getValue();
            ArrayList<Transaction> income = getCachedIncome().getValue();
            ArrayList<T> allProjections = new ArrayList<>(  );

            expenses.forEach( transaction -> allProjections.addAll( getProjections( transaction ) ));
            income.forEach( transaction -> allProjections.addAll( getProjections( transaction ) ) );

            Collections.sort( allProjections );

            for(T projection : allProjections){
                LocalDate projectionPeriod = ((LocalDate) projection.getProperty( R.string.date_tag )).withDayOfMonth( 1 );

                if(!result.containsKey( projectionPeriod )) {
                    result.put( projectionPeriod, new ArrayList<>(  ) );
                }

                ArrayList<T> periodProjections = result.get( projectionPeriod );
                periodProjections.add( projection );
                Collections.sort( periodProjections );
                result.replace( projectionPeriod, periodProjections );
            }

            mProjectionsByPeriod = new MutableLiveData<>( result );
        }
        return mProjectionsByPeriod;
    }




    private void buildProjection(){
        HashMap<LocalDate, HashMap<String, LiveData<Object[]>>> projectedBudget; //String = "{transaction uuid}{+scheduled date if a projection}" //Object[] = {Transaction,Double}
        ArrayList<Transaction> transactions = getActiveTransactions();
        Double balanceIndex = getCurrentBalance();

        LinkedHashMap<LocalDate, ArrayList<T>> projectionsByPeriod = new LinkedHashMap<>(  );
        transactions.stream()
                .map( transaction -> getProjections( transaction ) )
                .reduce( (projectionArray, projections) -> {
                    if(projections == null) projections = new ArrayList<T>();
                    projections.addAll( projectionArray );
                    return projections;
                } ).get().stream()
                .forEach( projection -> {
                    LocalDate periodDate = getPeriodDate( projection );
                    if(!(projectionsByPeriod.containsKey( periodDate ))) projectionsByPeriod.put( periodDate, new ArrayList<T>(  ) );
                    ArrayList periodProjections = projectionsByPeriod.get( periodDate );

                    periodProjections.add( projection );
                } );

        projectionsByPeriod.entrySet().stream();
//                .map( (periodDate, periodTransactions) -> {
//
//                } );
    }

    private LocalDate getPeriodDate(T transaction){
        return ((LocalDate)transaction.getProperty( R.string.date_tag )).withDayOfMonth( 1 );
    }







    /*
        okay, so it is working, but how I'm loading and accessing data is not at all sufficient

        the problem is that I'm creating large complex object arrays which change with any update to a transaction
        I need to define a more functional approach. the goals of this approach are:
            -   replace calls for these massive objects with a series of functions
                -   a single function that takes a list of transactions
                    and returns them grouped by period
                -   a single function that takes a list of transactions and a starting balance
                    and returns a list with the resulting balances following each transaction
                -   a single function that takes a single Transaction and returns a list of Projections/Transactions
            -   when changes to transactions occur a function should only recalculate it's result if needed
                -   if I can somehow identify what specifically needs to change when an observed object
                    changes, I can use id's or tags or a flag to determine which functions need to be re-called
                -   I could also have separate functions for setting specific view elements
                    -   if only the calculated transaction balances need to be changed,
                        should have a function which takes the map of views and balances to modify the
                        value shown in the view
                    -   instead of running every item through the alert function, just set the balance view style
                        when the amount is loaded by checking against threshold and zero
                -   If an item is added or removed, insert or remove the view and reload the balance values
                -

        handling changes to transactions without rebuilding evertying, is what I'm most unsure how to handle.
        some thoughts I have are:
        -   all the views effected shouldn't be recreated, only update the values that are effected
        -   I would need to observe a more nuanced set of LiveData objects and I'm not familiar with that class enough
        -   I think a solution may lie in the area of making each list item a Fragment with a
            viewModel providing data being observed from the main activity viewmodel
            -   I'm having trouble though with dynamically managing child fragments
            -   Another class that may hold answers is Room. Not sure about what it's
                function and use cases are though but what I've gathered is that it's connected to
                ViewModel and LiveData
                    -
     */




}

