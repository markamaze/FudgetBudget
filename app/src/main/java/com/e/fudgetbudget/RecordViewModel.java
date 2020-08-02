package com.e.fudgetbudget;

import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.e.fudgetbudget.model.BudgetModel;
import com.e.fudgetbudget.model.RecordedTransaction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;

class RecordViewModel {
    private final MainActivity context;
    private BudgetModel budgetModel;

    RecordViewModel(MainActivity context, BudgetModel budgetModel) {
        this.context = context;
        this.budgetModel = budgetModel;
    }

    void loadRecordLineItems(LinearLayout list_view) {
        ArrayList<RecordedTransaction> records = budgetModel.getRecords( "all" );
        Collections.reverse( records );
        if(records.size() <= 0) list_view.addView( TextView.inflate( this.context, R.layout.view_empty_list_message, null ) );

        else records.iterator().forEachRemaining( recordedTransaction -> {
            LinearLayout lineItem = getRecordLineItemLayout( recordedTransaction );
            lineItem.setOnClickListener( view -> showRecordFullDisplayDialog(recordedTransaction) );
            list_view.addView( lineItem );
        });

    }

    void showRecordFullDisplayDialog(RecordedTransaction recordedTransaction){
        AlertDialog.Builder builder = new AlertDialog.Builder( this.context );
        LinearLayout fullDisplayView = getRecordFullDisplayLayout( recordedTransaction );

        TextView header = fullDisplayView.findViewById( R.id.fulldisplay_header );
        if((boolean)recordedTransaction.getProperty( R.string.income_tag )) header.setText("Income Record");
        else header.setText("Expense Record");

        builder.setView( fullDisplayView );

        //TODO: get reconciling and displaying records all working first, then figure out how to implement modifying records
//        builder.setPositiveButton( "Edit", (dialog, which) -> {
//            dialog.dismiss();
//            showRecordEditorDialog(recordedTransaction);
//        });
//        builder.setNeutralButton( "Delete", (dialog, which) -> {
//            budgetModel.delete( recordedTransaction );
//            dialog.dismiss();
//            this.context.recreate();
//        } );
        builder.setNegativeButton( "Close", (dialog, which) -> dialog.dismiss() );
        builder.show();
    }
//    void showRecordEditorDialog(RecordedTransaction recordedTransaction){}

    private LinearLayout getRecordLineItemLayout(RecordedTransaction recordedTransaction){
        LinearLayout recordLineItem = (LinearLayout) LinearLayout.inflate( this.context, R.layout.layout_record_lineitem, null );

        TextView date = recordLineItem.findViewById( R.id.date_value );
        TextView label = recordLineItem.findViewById( R.id.label_value );
        TextView amount = recordLineItem.findViewById( R.id.amount_value );

        date.setText( ((LocalDate)recordedTransaction.getProperty( R.string.date_tag )).format( DateTimeFormatter.ofPattern( "MM/dd" ) ));
        label.setText( (String) recordedTransaction.getProperty( R.string.label_tag ) );
        amount.setText( String.valueOf( recordedTransaction.getProperty( R.string.amount_tag ) ));

        return recordLineItem;
    }
//    private LinearLayout getRecordEditorLayout(RecordedTransaction recordedTransaction){
//        LinearLayout recordEditor = (LinearLayout) this.context.getLayoutInflater().inflate( R.layout.layout_record_editor, null );
//
//        return recordEditor;
//    }
    private LinearLayout getRecordFullDisplayLayout(RecordedTransaction recordedTransaction){
        LinearLayout recordFullDisplay = (LinearLayout) this.context.getLayoutInflater().inflate( R.layout.layout_record_fulldisplay, null );

        //set values for all record properties

        return recordFullDisplay;
    }

//    private RecordedTransaction updateRecordedTransactionFromEditor(RecordedTransaction recordedTransaction, LinearLayout editorView){
//
//        //TODO: adjust these properties, just copied this from projections
//        String label = ( (EditText) editorView.findViewById( R.id.label_editor )).getText().toString();
//        LocalDate date = (LocalDate) editorView.findViewById( R.id.date_editor_button ).getTag(R.string.date_tag );
//        Double amount = Double.valueOf(( (EditText) editorView.findViewById( R.id.amount_editor )).getText().toString());
//        String note = ( (EditText) editorView.findViewById( R.id.note_editor )).getText().toString();
//
//        recordedTransaction.setProperty( R.string.label_tag, label );
//        recordedTransaction.setProperty( R.string.date_tag, date );
//        recordedTransaction.setProperty( R.string.amount_tag, amount );
//        recordedTransaction.setProperty( R.string.note_tag, note );
//
//        return recordedTransaction;
//    }
}
