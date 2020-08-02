package com.e.fudgetbudget;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.media.Image;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.e.fudgetbudget.model.BudgetModel;
import com.e.fudgetbudget.model.ProjectedTransaction;
import com.e.fudgetbudget.model.RecordedTransaction;
import com.e.fudgetbudget.model.Transaction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;

class ProjectionViewModel {
    private final MainActivity context;
    private BudgetModel budgetModel;


    ProjectionViewModel(MainActivity context, BudgetModel budgetModel) {
        this.context = context;
        this.budgetModel = budgetModel;
    }


//    <T extends Transaction> void loadProjectionLineItemsRebuild(LinearLayout list_view){
//        LinkedHashMap<LocalDate, ArrayList<Double>> projectedBalances = budgetModel.getProjectedBalancesByPeriod();
//        LinkedHashMap<LocalDate, ArrayList<T>> projections = budgetModel.getProjectedTransactionsByPeriod();
//        Collection<LocalDate> keySet;
//
//        if(projectedBalances.keySet().containsAll( projections.keySet() )) keySet = projectedBalances.keySet();
//        else throw new Error("invalid data structure in ProjectionViewModel.loadProjectionLineItems");
//
//        //each loop handles a single period
//        for(LocalDate firstDateInPeriod : keySet ){
//            //add to list_view header line item for the period -> projectedBalances line item with date representing the period (ie: if monthly, just show month and year)
//            //clicking on the header lineitem should focus the view
//            //when lineitem is in focus, expand the view to display all projections for the period
//            //  projections line item will need to carry through the end of day balance and display in line
//            //  each projection lineitem needs to have a button to reconcile which will display a confirmation dialog with option to edit before recording
//            //  selecting the lineitem opens the projection in fulldisplay view
//
//            LinearLayout periodHeaderLineItem = getProjectionLineItemLayout( firstDateInPeriod, projectedBalances.get(firstDateInPeriod) );
//
//            periodHeaderLineItem.setOnClickListener( view -> {} );
//
//            list_view.addView( periodHeaderLineItem );
//        }
//
//    }

    <T extends Transaction> void loadProjectionLineItems(LinearLayout list_view) {
        LinkedHashMap<LocalDate, ArrayList<Double>> projectedBalances = budgetModel.getProjectedBalancesByPeriod();
        LinkedHashMap<LocalDate, ArrayList<T>> projections = budgetModel.getProjectedTransactionsByPeriod();


        if(!projectedBalances.keySet().containsAll( projections.keySet() )) throw new Error("projected key sets do not match");
        else for( LocalDate periodBeginDate: projections.keySet()){
            LinearLayout balanceProjectionLineItem = getProjectionLineItemLayout( periodBeginDate, projectedBalances.get(periodBeginDate) );

            balanceProjectionLineItem.setOnClickListener( view -> {
                LinearLayout expandView = view.findViewById( R.id.projectedBalance_lineitem_expanded );
                final String[] bod_balance = {String.valueOf( projectedBalances.get( periodBeginDate ).stream().findFirst().get() )};



                if(expandView.getChildCount() <= 0){
                    LinearLayout projectedTransactionsList = (LinearLayout) LinearLayout.inflate( this.context, R.layout.layout_projected_transactions_list, null );
                    LinearLayout listBody = projectedTransactionsList.findViewById( R.id.list_body );


                    for( T projection : projections.get(periodBeginDate)){
                        String projectionEodBalance, projectionAmount;

                        if(projection.getIncomeFlag()) projectionAmount = projection.getProperty( R.string.amount_tag ).toString();
                        else projectionAmount = String.valueOf( "-" + projection.getProperty( R.string.amount_tag ) );

                        projectionEodBalance = String.valueOf( Double.valueOf( bod_balance[0] ) + Double.valueOf( projectionAmount ) );


                        LinearLayout projectedTransactionLineItem = (LinearLayout) LinearLayout.inflate( this.context, R.layout.layout_projected_transactions_lineitem, null );

                        ((TextView)projectedTransactionLineItem.findViewById( R.id.date_value )).setText(((LocalDate)projection.getProperty(R.string.date_tag )).format( DateTimeFormatter.ofPattern( "d-E" ) ));
                        ((TextView)projectedTransactionLineItem.findViewById( R.id.label_value )).setText((String)projection.getProperty(R.string.label_tag ));

                        if((boolean)projection.getProperty( R.string.income_tag ))
                            ((TextView)projectedTransactionLineItem.findViewById( R.id.amount_value )).setText(String.valueOf(projection.getProperty(R.string.amount_tag )));
                        else ((TextView)projectedTransactionLineItem.findViewById( R.id.amount_value )).setText( projectionAmount );


                        ((TextView)projectedTransactionLineItem.findViewById( R.id.bod_balance_value )).setText(bod_balance[0]);
                        ((TextView)projectedTransactionLineItem.findViewById( R.id.eod_balance_value )).setText(String.valueOf(projectionEodBalance));

                        ImageButton recordButton = (ImageButton) projectedTransactionLineItem.findViewById( R.id.record_projection_button );
                        if(( (LocalDate) projection.getProperty( R.string.date_tag )).isAfter( LocalDate.now() )) recordButton.setVisibility( View.INVISIBLE );
                        recordButton.setImageIcon( Icon.createWithResource(this.context, androidx.appcompat.R.drawable.abc_ic_ab_back_material ));
                        recordButton.setOnClickListener( v -> showReconcileDialog( projection ) );

                        projectedTransactionLineItem.setOnClickListener( view1 -> {
                            if(projection instanceof ProjectedTransaction) showProjectionFullDisplayDialog( (ProjectedTransaction)projection );
                            else this.context.transactionViewModel.showFullDisplayDialog( (Transaction) projection );
                        });
                        listBody.addView( projectedTransactionLineItem );

                        bod_balance[0] = projectionEodBalance;
                        if(Double.parseDouble(bod_balance[0]) < budgetModel.getLowestProjectedBalance())
                            budgetModel.setLowestProjectedBalance(Double.parseDouble( bod_balance[0] ));
                    }


                    ((LinearLayout)view.findViewById( R.id.projectedBalance_lineitem_expanded )).addView( projectedTransactionsList );
//                    view.setBackgroundColor( Color.LTGRAY );
                    projectedTransactionsList.setBackgroundColor( Color.LTGRAY );
                    balanceProjectionLineItem.setBackgroundColor( Color.GRAY );

                }
                else {
                    expandView.removeAllViews();
                    balanceProjectionLineItem.setBackgroundColor( 0 );
                }
            } );

            list_view.addView( balanceProjectionLineItem );
        }
    }
    void loadBalanceView(LinearLayout balancesheet_page) {
        TextView balance_value = (TextView) balancesheet_page.findViewById( R.id.current_balance_value );
        balance_value.setText( String.valueOf( budgetModel.getCurrentBalance() ));
    }


    private LinearLayout getProjectionLineItemLayout(LocalDate date, ArrayList<?> projectedBalances) {
        LinearLayout line_item_view = (LinearLayout)LinearLayout.inflate( this.context, R.layout.layout_projected_balance_lineitem, null);

        TextView bodBalanceValue = (TextView) line_item_view.findViewById( R.id.projection_lineitem_bod_balance_amount );
        TextView incomeValue = (TextView) line_item_view.findViewById( R.id.projection_lineitem_income_amount );
        TextView expenseValue = (TextView) line_item_view.findViewById( R.id.projection_lineitem_expense_amount );
        TextView eodBalanceValue = (TextView) line_item_view.findViewById( R.id.projection_lineitem_eod_balance_amount );
        TextView dateValue = (TextView) line_item_view.findViewById( R.id.date_value );

        dateValue.setText( date.format( DateTimeFormatter.ofPattern( "MMM yyyy" ) ) );
        bodBalanceValue.setText( String.valueOf( projectedBalances.get( 0 ) ));
        incomeValue.setText( String.valueOf( projectedBalances.get( 1 ) ));
        expenseValue.setText( String.valueOf( "-" + projectedBalances.get( 2 ) ));
        eodBalanceValue.setText( String.valueOf( projectedBalances.get( 3 ) ) );



//        line_item_view.setOnClickListener( view -> {
//            LinearLayout expandView = line_item_view.findViewById( R.id.projectedBalance_lineitem_expanded );
//
//            if(expandView.getChildCount() <= 0){
//                LinearLayout projectedTransactionsList = (LinearLayout) LinearLayout.inflate( this.context, R.layout.layout_projected_transactions_list, null );
//                LinearLayout listBody = projectedTransactionsList.findViewById( R.id.list_body );
//
//                projected_transaction_list.forEach( projection -> {
//                    ProjectedTransaction projectedTransaction = (ProjectedTransaction) projection;
//                    //todo: add a button to each projected_transaction_lineitem that will open it in a reconcile dialog
//                    LinearLayout projectedTransactionLineItem = (LinearLayout) LinearLayout.inflate( this.context, R.layout.layout_projected_transactions_lineitem, null );
//                    ((TextView)projectedTransactionLineItem.findViewById( R.id.date_value )).setText(projectedTransaction.getScheduledDate().format( DateTimeFormatter.ofPattern( "MM/dd" ) ));
//                    ((TextView)projectedTransactionLineItem.findViewById( R.id.label_value )).setText(projectedTransaction.getLabel());
//                    if(projectedTransaction.getIncomeFlag())
//                        ((TextView)projectedTransactionLineItem.findViewById( R.id.amount_value )).setText(String.valueOf(projectedTransaction.getAmount()));
//                    else ((TextView)projectedTransactionLineItem.findViewById( R.id.amount_value )).setText("- " +String.valueOf(projectedTransaction.getAmount()));
//
//                    //
//                    projectedTransactionLineItem.setOnClickListener( view1 -> showProjectionFullDisplayDialog( projectedTransaction ));
//                    listBody.addView( projectedTransactionLineItem );
//                } );
//
//
//                ((LinearLayout)line_item_view.findViewById( R.id.projectedBalance_lineitem_expanded )).addView( projectedTransactionsList );
//                line_item_view.setBackgroundColor( Color.GRAY );
//            }
//            else {
//                expandView.removeAllViews();
//                line_item_view.setBackgroundColor( Color.WHITE );
//            }
//
//        } );

        return line_item_view;
    }
    private LinearLayout getProjectionEditorLayout(ProjectedTransaction projectedTransaction) {
        LinearLayout editorLayout = (LinearLayout) LinearLayout.inflate( this.context, R.layout.layout_projected_transactions_editor, null );

        TextView label = editorLayout.findViewById( R.id.label_value );
        Button editProjectedDate = editorLayout.findViewById( R.id.date_editor_button );
        TextView scheduledDate = editorLayout.findViewById( R.id.scheduled_date_value );
        EditText editAmount = editorLayout.findViewById( R.id.amount_editor );
        TextView scheduledValue = editorLayout.findViewById( R.id.scheduled_amount_value );
        EditText editNote = editorLayout.findViewById( R.id.note_editor );

        label.setText((String)projectedTransaction.getProperty(R.string.label_tag ));
        editProjectedDate.setText(((LocalDate)projectedTransaction.getProperty(R.string.date_tag )).format(DateTimeFormatter.ofPattern( "MMMM dd, yyyy" )));
        editProjectedDate.setTag(R.string.date_value_tag, String.valueOf(((LocalDate)projectedTransaction.getProperty( R.string.date_tag )).toEpochDay()));
        scheduledDate.setText(((LocalDate)projectedTransaction.getProperty( R.string.date_tag )).format(DateTimeFormatter.ofPattern( "MMMM dd, yyyy" )));
        editAmount.setText(String.valueOf(projectedTransaction.getProperty(R.string.amount_tag )));
        scheduledValue.setText(String.valueOf( projectedTransaction.getProperty( R.string.amount_tag ) ));
        editNote.setText((String)projectedTransaction.getProperty( R.string.note_tag ));


        editProjectedDate.setOnClickListener( view -> {
            final LocalDate[] newDate = {((LocalDate)projectedTransaction.getProperty(R.string.date_tag ))};
            DatePickerDialog pickerDialog = new DatePickerDialog( this.context );
            DatePicker picker = pickerDialog.getDatePicker();

            picker.init( newDate[0].getYear(), newDate[0].getMonthValue()-1, newDate[0].getDayOfMonth(), (button_view, year, monthOfYear, dayOfMonth) -> {
                newDate[0] = LocalDate.of( year, monthOfYear+1, dayOfMonth );
            } );
            pickerDialog.setButton( DatePickerDialog.BUTTON_POSITIVE, "OK", (dialog, which)->{
                editProjectedDate.setText( newDate[0].format( DateTimeFormatter.ofPattern( "MMMM dd, yyyy" ) ));
                editProjectedDate.setTag(R.string.date_value_tag, newDate[0].toEpochDay() );
                dialog.dismiss();
            } );

            pickerDialog.setContentView( picker );
            pickerDialog.show();
        } );

        return editorLayout;
    }
    private LinearLayout getProjectionFullDisplayLayout(ProjectedTransaction projectedTransaction) {
        LinearLayout fullDisplayLayout = (LinearLayout) LinearLayout.inflate( this.context, R.layout.layout_projected_transactions_fulldisplay, null );

        TextView label = fullDisplayLayout.findViewById( R.id.label_value );
        TextView projectedDate = fullDisplayLayout.findViewById( R.id.projected_date_value );
        TextView scheduledDate = fullDisplayLayout.findViewById( R.id.scheduled_date_value );
        TextView amount = fullDisplayLayout.findViewById( R.id.amount_value );
        TextView scheduledValue = fullDisplayLayout.findViewById( R.id.scheduled_amount_value );
        TextView note = fullDisplayLayout.findViewById( R.id.note_value );

        label.setText((String)projectedTransaction.getProperty(R.string.label_tag ));
        projectedDate.setText(((LocalDate)projectedTransaction.getProperty( R.string.date_tag )).format(DateTimeFormatter.ofPattern( "MMMM dd, yyyy" )));
        scheduledDate.setText(((LocalDate)projectedTransaction.getProperty(R.string.date_tag )).format(DateTimeFormatter.ofPattern( "MMMM dd, yyyy" )));
        amount.setText(String.valueOf(projectedTransaction.getProperty(R.string.amount_tag )));
        //todo: add projectectedTransaction property for scheduledAmount
        scheduledValue.setText(String.valueOf( projectedTransaction.getProperty(R.string.amount_tag )) );
        note.setText((String)projectedTransaction.getProperty(R.string.note_tag ));

        return fullDisplayLayout;
    }
    private <T extends Transaction> LinearLayout getReconcileProjectionLayout(T projectedTransaction) {
        LinearLayout reconcileLayout = (LinearLayout) LinearLayout.inflate( this.context, R.layout.layout_reconcile_projection, null );

        return reconcileLayout;
    }

    void showProjectionFullDisplayDialog(ProjectedTransaction projectedTransaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder( this.context );
        LinearLayout fullDisplayView = getProjectionFullDisplayLayout( projectedTransaction );

        TextView header = fullDisplayView.findViewById( R.id.fulldisplay_header );
        if((boolean)projectedTransaction.getProperty(R.string.income_tag )) header.setText("Income Transaction");
        else header.setText("Expense Transaction");

        builder.setNeutralButton( "Edit", (dialog, which) -> {
            dialog.dismiss();
            showProjectionEditorDialog(projectedTransaction);
        });
        builder.setNegativeButton( "Close", (dialog, which) -> dialog.dismiss() );

        builder.setView( fullDisplayView );
        builder.show();
    }
    private <T extends Transaction> void showReconcileDialog(T transaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder( this.context );
        LinearLayout reconcileView = getReconcileProjectionLayout( transaction );

        if(transaction instanceof ProjectedTransaction)
            reconcileView.addView( getProjectionEditorLayout( (ProjectedTransaction)transaction ) );
        else reconcileView.addView( this.context.transactionViewModel.getEditorLayout( transaction ) );

        builder.setNeutralButton( "Cancel", (dialog, which) -> dialog.dismiss() );
        builder.setPositiveButton( "Create Record", (dialog, which) -> {
            budgetModel.reconcile(transaction);
            dialog.dismiss();
            this.context.recreate();
        } );

        builder.setView( reconcileView );
        builder.show();
    }
    private void showProjectionEditorDialog(ProjectedTransaction projectedTransaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder( this.context );
        LinearLayout editorView = getProjectionEditorLayout( projectedTransaction );

        TextView header = editorView.findViewById( R.id.fulldisplay_header );
        if((boolean)projectedTransaction.getProperty(R.string.income_tag )) header.setText("Modify Income");
        else header.setText("Modify Expense");

        builder.setPositiveButton( "Save", (dialog, which) -> {
            //todo:
            ProjectedTransaction updatedProjection = updateProjectedTransactionFromEditor(projectedTransaction, editorView);
            budgetModel.update( updatedProjection );
            dialog.dismiss();
            this.context.recreate();
        });
        builder.setNeutralButton( "Close", (dialog, which) -> dialog.dismiss() );


        builder.setView( editorView );
        builder.show();
    }

    private ProjectedTransaction updateProjectedTransactionFromEditor(ProjectedTransaction projectedTransaction, LinearLayout editorView){
        Button dateButton = (Button) editorView.findViewById( R.id.date_editor_button );
        long longDate = Long.parseLong( dateButton.getTag(R.string.date_value_tag).toString() );
        LocalDate date = LocalDate.ofEpochDay( longDate );
        Double amount = Double.valueOf(( (EditText) editorView.findViewById( R.id.amount_editor )).getText().toString());
        String note = ( (EditText) editorView.findViewById( R.id.note_editor )).getText().toString();

        projectedTransaction.setProperty( R.string.date_tag, date );
        projectedTransaction.setProperty( R.string.amount_tag, amount );
        projectedTransaction.setProperty( R.string.note_tag, note );


        return projectedTransaction;
    }
}
