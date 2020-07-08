package com.e.fudgetbudget;

import android.graphics.Color;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.e.fudgetbudget.model.BudgetModel;
import com.e.fudgetbudget.model.ProjectedTransaction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;

class ProjectionViewModel {
    private final MainActivity context;
    private BudgetModel budgetModel;


    ProjectionViewModel(MainActivity context) {
        this.context = context;
        this.budgetModel = new BudgetModel( context );
    }


    void loadProjectionLineItems(LinearLayout list_view) {
        LinkedHashMap<LocalDate, ArrayList<ProjectedTransaction>> projections = budgetModel.getProjectedTransactionsByPeriod(60);

        //TODO: I need to carry the running balance for the projections
        //  I did this before, but either it got removed or I'm not seeing where it went and it's not working
        projections.forEach( (date, projections_list) -> {
            list_view.addView( getProjectionLineItemLayout(date, projections_list) );
        });
    }
    void loadBalanceView(LinearLayout balancesheet_page) {
        TextView balance_value = (TextView) balancesheet_page.findViewById( R.id.current_balance_value );
        balance_value.setText( String.valueOf( budgetModel.getCurrentBalance() ));
    }


//    void showBalanceEditorDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder( this.context );
//        LinearLayout dialog_view = getBalanceEditorLayout();
//        dialog_view.setTag(R.string.object_type, R.string.balance_tag);
//        View finalDialog_view = dialog_view;
//        builder.setPositiveButton( "Update", ((dialog, which) -> {
//            EditText balance_editor = (EditText) finalDialog_view.findViewById( balancesheet_currentbalance_editor_id );
//            String new_balance = balance_editor.getText().toString();
//            budgetModel.update(Double.parseDouble( new_balance ));
//            dialog.dismiss();
//            this.context.recreate();
//        }) );
//        builder.setNegativeButton("Discard", (dialog, which) -> dialog.dismiss() );
//        builder.setView( finalDialog_view );
//        builder.show();
//    }


//    private LinearLayout getBalanceEditorLayout(){
//        LinearLayout balance_editor_view = (LinearLayout) LinearLayout.inflate( this.context, property_displays_balance_layout, null );
//
//        TextView reset_balance_header = (TextView) balance_editor_view.findViewById( reset_current_balance_header_id );
//        LinearLayout reset_balance_layout = (LinearLayout) balance_editor_view.findViewById( reset_currentbalance_layout_id );
//
//        balance_editor_view.removeAllViews();
//        balance_editor_view.addView( reset_balance_header );
//        balance_editor_view.addView( reset_balance_layout );
//
//        return balance_editor_view;
//    }


    private LinearLayout getProjectionLineItemLayout(LocalDate date, ArrayList<?> projected_transaction_list) {
        LinearLayout line_item_view = (LinearLayout)LinearLayout.inflate( this.context, R.layout.layout_projected_balance_lineitem, null);

        TextView incomeValue = (TextView) line_item_view.findViewById( R.id.projection_lineitem_income_amount );
        TextView expenseValue = (TextView) line_item_view.findViewById( R.id.projection_lineitem_expense_amount );
        TextView eodBalanceValue = (TextView) line_item_view.findViewById( R.id.projection_lineitem_eod_balance_amount );
        TextView dateValue = (TextView) line_item_view.findViewById( R.id.date_value );


        double expense_sum = budgetModel.getExpenseSumFromList(projected_transaction_list);
        double income_sum = budgetModel.getIncomeSumFromList(projected_transaction_list);
        double end_of_day_balance = budgetModel.getCurrentBalance() + income_sum - expense_sum;

        dateValue.setText( date.format( DateTimeFormatter.ofPattern( "MMM dd, yy" ) ) );
        eodBalanceValue.setText( String.valueOf( end_of_day_balance ) );
        incomeValue.setText( String.valueOf( income_sum ));
        expenseValue.setText( String.valueOf( expense_sum ));

        line_item_view.setOnClickListener( view -> {
            LinearLayout expandView = line_item_view.findViewById( R.id.projectedBalance_lineitem_expanded );

            if(expandView.getChildCount() <= 0){
                LinearLayout projectedTransactionsList = (LinearLayout) LinearLayout.inflate( this.context, R.layout.layout_projected_transactions_list, null );
                LinearLayout listBody = projectedTransactionsList.findViewById( R.id.list_body );

                projected_transaction_list.forEach( projection -> {
                    ProjectedTransaction projectedTransaction = (ProjectedTransaction) projection;
                    //todo: add a button to each projected_transaction_lineitem that will open it in a reconcile dialog
                    LinearLayout projectedTransactionLineItem = (LinearLayout) LinearLayout.inflate( this.context, R.layout.layout_projected_transactions_lineitem, null );
                    ((TextView)projectedTransactionLineItem.findViewById( R.id.date_value )).setText(projectedTransaction.getScheduledDate().format( DateTimeFormatter.ofPattern( "MM/dd" ) ));
                    ((TextView)projectedTransactionLineItem.findViewById( R.id.label_value )).setText(projectedTransaction.getLabel());
                    if(projectedTransaction.getIncomeFlag())
                        ((TextView)projectedTransactionLineItem.findViewById( R.id.amount_value )).setText(String.valueOf(projectedTransaction.getAmount()));
                    else ((TextView)projectedTransactionLineItem.findViewById( R.id.amount_value )).setText("- " +String.valueOf(projectedTransaction.getAmount()));

                    //
                    projectedTransactionLineItem.setOnClickListener( view1 -> showProjectionFullDisplayDialog( projectedTransaction ));
                    listBody.addView( projectedTransactionLineItem );
                } );


                ((LinearLayout)line_item_view.findViewById( R.id.projectedBalance_lineitem_expanded )).addView( projectedTransactionsList );
                line_item_view.setBackgroundColor( Color.GRAY );
            }
            else {
                expandView.removeAllViews();
                line_item_view.setBackgroundColor( Color.WHITE );
            }

        } );

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

        label.setText(projectedTransaction.getLabel());
        editProjectedDate.setText(projectedTransaction.getProjectedDate().format(DateTimeFormatter.ofPattern( "MMMM dd, yyyy" )));
        scheduledDate.setText(projectedTransaction.getScheduledDate().format(DateTimeFormatter.ofPattern( "MMMM dd, yyyy" )));
        editAmount.setText(String.valueOf(projectedTransaction.getAmount()));
        //todo: add projectectedTransaction property for scheduledAmount
        scheduledValue.setText(String.valueOf( projectedTransaction.getAmount() ));
        editNote.setText(projectedTransaction.getNote());


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

        label.setText(projectedTransaction.getLabel());
        projectedDate.setText(projectedTransaction.getProjectedDate().format(DateTimeFormatter.ofPattern( "MMMM dd, yyyy" )));
        scheduledDate.setText(projectedTransaction.getScheduledDate().format(DateTimeFormatter.ofPattern( "MMMM dd, yyyy" )));
        amount.setText(String.valueOf(projectedTransaction.getAmount()));
        //todo: add projectectedTransaction property for scheduledAmount
        scheduledValue.setText(String.valueOf( projectedTransaction.getAmount() ));
        note.setText(projectedTransaction.getNote());

        return fullDisplayLayout;
    }
    private LinearLayout getReconcileProjectionLayout(ProjectedTransaction projectedTransaction) {
        LinearLayout reconcileLayout = (LinearLayout) LinearLayout.inflate( this.context, R.layout.layout_reconcile_projection, null );
        /*
         *
         * */


        return reconcileLayout;
    }

    public void showProjectionFullDisplayDialog(ProjectedTransaction projectedTransaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder( this.context );
        LinearLayout fullDisplayView = getProjectionFullDisplayLayout( projectedTransaction );

        TextView header = fullDisplayView.findViewById( R.id.fulldisplay_header );
        if(projectedTransaction.getIncomeFlag()) header.setText("Income Transaction");
        else header.setText("Expense Transaction");

        builder.setNeutralButton( "Edit", (dialog, which) -> {
            dialog.dismiss();
            showProjectionEditorDialog(projectedTransaction);
        });
        builder.setNegativeButton( "Close", (dialog, which) -> dialog.dismiss() );

        builder.setView( fullDisplayView );
        builder.show();
    }
    private void showReconcileDialog(ProjectedTransaction projectedTransaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder( this.context );
        LinearLayout reconcileView = getReconcileProjectionLayout( projectedTransaction );

        TextView header = reconcileView.findViewById( R.id.fulldisplay_header );
        header.setText("Reconcile Transaction");

        builder.setNeutralButton( "Cancel", (dialog, which) -> dialog.dismiss() );
        builder.setPositiveButton( "Submit", (dialog, which) -> {
            dialog.dismiss();
            //todo: add acknowledgment alert
        } );

        builder.setView( reconcileView );
        builder.show();
    }
    private void showProjectionEditorDialog(ProjectedTransaction projectedTransaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder( this.context );
        LinearLayout editorView = getProjectionEditorLayout( projectedTransaction );

        TextView header = editorView.findViewById( R.id.fulldisplay_header );
        if(projectedTransaction.getIncomeFlag()) header.setText("Modify Income");
        else header.setText("Modify Expense");

        builder.setPositiveButton( "Save", (dialog, which) -> {
            //todo: extract the new projectedTransaction and use budgetModel to update the values
            // (which are stored within the root transaction)
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
        LocalDate date = (LocalDate)( (Button) editorView.findViewById( R.id.date_editor_button )).getTag(R.string.date_tag );
        Double amount = Double.valueOf(( (EditText) editorView.findViewById( R.id.amount_editor )).getText().toString());
        String note = ( (EditText) editorView.findViewById( R.id.note_editor )).getText().toString();

        projectedTransaction.setPropertyById( R.id.projected_date_value, date );
        projectedTransaction.setPropertyById( R.id.amount_value, amount );
        projectedTransaction.setPropertyById( R.id.note_value, note );


        return projectedTransaction;
    }

}
