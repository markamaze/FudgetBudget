package com.fudgetbudget.ui;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.TextView;

import com.fudgetbudget.R;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;

public class PeriodItemViewBuilder {
    private final Context context;

    public PeriodItemViewBuilder(Context ctx) {
        context = ctx;
    }

    void setPeriodItemHeader(ViewGroup periodHeader, LocalDate periodBeginDate){
        TextView periodDateView = periodHeader.findViewById( R.id.projection_period_header_date );
        periodDateView.setText( periodBeginDate.format( DateTimeFormatter.ofPattern( "MMMM yyyy" ) ) );

    }
    void setPeriodItemBalances(ViewGroup periodItem, List<Double> periodBalances){
        Double initialBal = periodBalances.get(0);
        Double endBal = periodBalances.get(3);
        Double income = periodBalances.get(1);
        Double expense = periodBalances.get(2);


        TextView startOfPeriodBalance = periodItem.findViewById( R.id.projection_period_header_initial_balance );
        //                if(initialBal < 0) startOfPeriodBalance.setTextAppearance( R.style.balance_amount_negative );
        //                else startOfPeriodBalance.setTextAppearance( R.style.balance_amount_not_negative );
        startOfPeriodBalance.setText( formatUtility.formatCurrency( initialBal ) );

        TextView endOfPeriodBalance = periodItem.findViewById( R.id.projection_period_header_ending_balance );
//        if(endBal < 0) endOfPeriodBalance.setTextColor( this.context.getColor( R.color.colorCurrencyNegative ));
//        else endOfPeriodBalance.setTextColor( this.context.getColor( R.color.colorCurrencyNotNegative ));
        endOfPeriodBalance.setText( formatUtility.formatCurrency( endBal ) );

        TextView periodIncome = periodItem.findViewById( R.id.projection_period_header_income);
//        periodIncome.setTextColor( this.context.getColor( R.color.colorCurrencyNotNegative ));
        periodIncome.setText( "+" + formatUtility.formatCurrency( income ));

        TextView periodExpenses = periodItem.findViewById( R.id.projection_period_header_expense );
//        periodExpenses.setTextColor( this.context.getColor( R.color.colorCurrencyNegative ));
        periodExpenses.setText( "-" + formatUtility.formatCurrency( expense ));

        TextView netGain = periodItem.findViewById( R.id.projection_period_header_net_gain );
        TextView netGainLabel = periodItem.findViewById( R.id.projection_period_header_net_gain_label );
        if(endBal < initialBal){
            netGainLabel.setText( "Loss" );
            netGain.setText("-" + formatUtility.formatCurrency( endBal - initialBal ));
//            netGain.setTextColor( this.context.getColor( R.color.colorCurrencyNegative ));
        } else {
            netGainLabel.setText( "Gain");
            netGain.setText("+" + formatUtility.formatCurrency( endBal - initialBal ));
//            netGain.setTextColor( this.context.getColor( R.color.colorCurrencyNotNegative ));
        }
    }
    void setPeriodItemBalances(ViewGroup periodItem, HashMap<String, Double> periodBalances){
        //use this method for setting the balances in the periodItem view
        //should be called by the balancesheet and records


        TextView currentBalanceView = periodItem.findViewById( R.id.current_recorded_balance );
        Double currentBalance = periodBalances.get( "current_recorded_balance" );
        if(currentBalanceView != null && currentBalance != null){
            currentBalanceView.setText( String.valueOf( currentBalance ) );
        }

        TextView periodIncomeView = periodItem.findViewById( R.id.period_income_balance );
        Double periodIncome = periodBalances.get( "period_income_balance" );
        if(periodIncomeView != null && periodIncome != null){
            periodIncomeView.setText( String.valueOf( periodIncome ) );

        }

        TextView periodExpenseView = periodItem.findViewById( R.id.period_expense_balance );
        Double periodExpense = periodBalances.get( "period_expense_balance" );
        if(periodExpenseView != null && periodExpense != null){
            periodExpenseView.setText( String.valueOf( periodExpense ) );
        }

        TextView periodNetChangeView = periodItem.findViewById( R.id.period_netchange_balance );
        Double periodNetChange = periodBalances.get( "period_netchange_balance" );
        if(periodNetChangeView != null && periodNetChange != null){
            periodNetChangeView.setText( String.valueOf( periodNetChange ) );
        }

//        TextView startingBalanceView = periodItem.findViewById( R.id.period_starting_balance );
//        Double startingBalance = periodBalances.get( "period_starting_balance" );
//        if(startingBalanceView != null && startingBalance != null){
//            startingBalanceView.setText( String.valueOf( startingBalance ) );
//        }
//
//        TextView endingBalanceView = periodItem.findViewById( R.id.period_ending_balance );
//        Double endingBalance = periodBalances.get( "period_ending_balance" );
//        if(endingBalanceView != null && endingBalance != null){
//            endingBalanceView.setText( String.valueOf( endingBalance ) );
//        }

    }


}
