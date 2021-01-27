package com.fudgetbudget.ui;

import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.HandlerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.fudgetbudget.FudgetBudgetViewModel;
import com.fudgetbudget.R;
import com.fudgetbudget.model.ProjectedTransaction;
import com.fudgetbudget.model.RecordedTransaction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class RecordFragment extends Fragment {

    private ListItemViewBuilder mListItemViewBuilder;
    private FudgetBudgetViewModel mViewModel;
    private String mRecordKey;
    private MutableLiveData<RecordedTransaction> mRecord;
    private MutableLiveData<LocalDate> vLineDate;
    private MutableLiveData<String> vLineLabel;
    private MutableLiveData<String> vLineAmount;

    public static RecordFragment getInstance(String recordKey){
        RecordFragment fragment = new RecordFragment();
        Bundle bundle = new Bundle();
        bundle.putString("RECORD_KEY", recordKey);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mListItemViewBuilder = new ListItemViewBuilder(getContext());
        mRecordKey = getArguments().getString("RECORD_KEY");

        if(savedInstanceState != null){
            vLineDate = new MutableLiveData<>(LocalDate.parse(savedInstanceState.getString("RECORD_LINE_DATE"), DateTimeFormatter.BASIC_ISO_DATE));
            vLineLabel = new MutableLiveData<>(savedInstanceState.getString("RECORD_LINE_LABEL"));
            vLineAmount = new MutableLiveData<>(savedInstanceState.getString("RECORD_LINE_AMOUNT"));
        }
        else {
            vLineDate = new MutableLiveData<>(LocalDate.MIN);
            vLineLabel = new MutableLiveData<>( "loading..." );
            vLineAmount = new MutableLiveData<>( "" );
        }

        Executors.newSingleThreadExecutor().execute(()->{
            FudgetBudgetViewModel model = new ViewModelProvider(requireActivity()).get(FudgetBudgetViewModel.class);
            HandlerCompat.createAsync(Looper.getMainLooper()).post(() -> {
                mViewModel = model;
                mRecord = new MutableLiveData<>(model.getRecord(mRecordKey).getValue());

                observeModelData((ViewGroup) getView());
                initializeCardButtons((ViewGroup) getView());
                mListItemViewBuilder.setCardLayoutPropertyViews((ViewGroup) getView(), mRecord.getValue(), false);
            });

        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup projectionView = (ViewGroup) inflater.inflate(R.layout.layout_record, container, false);
        projectionView.findViewById(R.id.line_item).setOnClickListener( item -> mListItemViewBuilder.toggleCardVisibility(projectionView));

        if(mRecord != null){
            observeModelData(projectionView);
            initializeCardButtons(projectionView);
            mListItemViewBuilder.setCardLayoutPropertyViews(projectionView, mRecord.getValue(), false);
        }

        vLineDate.observeForever( newDate -> {
            TextView dateView = projectionView.findViewById( R.id.line_item_date_value );
            if(dateView != null) dateView.setText( formatUtility.formatDate( "eee MM-dd", newDate ));
        });
        vLineLabel.observeForever( newLabel -> {
            TextView labelView = projectionView.findViewById( R.id.line_item_label_value );
            if(labelView != null) labelView.setText( newLabel );
        });
        vLineAmount.observeForever( newAmount -> {
            TextView amountView = projectionView.findViewById( R.id.line_item_amount_value );
            if(amountView != null) {
                amountView.setText( "$" + formatUtility.formatCurrency( newAmount ));

                if(mRecord != null && mRecord.getValue() != null){
                    if(mRecord.getValue().getIncomeFlag()) amountView.setTextAppearance(R.style.income_text_style);
                    else amountView.setTextAppearance(R.style.expense_text_style);
                }
            }


        });


        return projectionView;
    }

    private void observeModelData(ViewGroup projectionView) {
        mViewModel.getRecord(mRecordKey).observeForever(record -> {
            if(!mRecord.getValue().equals(record)) mRecord.postValue(record);
        });



        mRecord.observeForever(projection -> {
            //postValue for any of the line item values that are changed
            LocalDate projectionDate = (LocalDate)projection.getProperty(R.string.date_tag);
            if( !projectionDate.isEqual(vLineDate.getValue()) ) vLineDate.postValue(projectionDate);

            String projectionLabel = (String)projection.getProperty(R.string.label_tag);
            if( !projectionLabel.contentEquals(vLineLabel.getValue()) ) vLineLabel.postValue(projectionLabel);

            String projectionAmount = String.valueOf( projection.getProperty(R.string.amount_tag));
            if( !projectionAmount.contentEquals(vLineAmount.getValue()) ) vLineAmount.postValue(projectionAmount);

            initializeCardButtons(projectionView);
            mListItemViewBuilder.setCardLayoutPropertyViews(projectionView, projection, false);
        });

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("RECORD_LINE_DATE", vLineDate.getValue().format(DateTimeFormatter.BASIC_ISO_DATE));
        outState.putString("RECORD_LINE_LABEL", vLineLabel.getValue());
        outState.putString("RECORD_LINE_AMOUNT", vLineAmount.getValue());

    }

    void initializeCardButtons(ViewGroup listItem){
        RecordedTransaction record = mRecord.getValue();

        if( listItem != null && record != null){
            ViewGroup buttonsView = listItem.findViewById( R.id.card_buttons );
            ViewGroup cardDrawer = listItem.findViewById( R.id.card_view );

            Button modifyButton = buttonsView.findViewById( R.id.modify_transaction_button );
            Button resetProjectionButton = buttonsView.findViewById( R.id.reset_projections_button );
            Button updateButton = buttonsView.findViewById( R.id.editor_update_button );
            Button cancelButton = buttonsView.findViewById( R.id.editor_cancel_button );
            Button deleteButton = buttonsView.findViewById( R.id.editor_delete_button );
            Button recordTransactionButton = buttonsView.findViewById( R.id.record_transaction_button );
            if(modifyButton != null) modifyButton.setOnClickListener( v -> {
                mListItemViewBuilder.setCardLayoutPropertyViews( cardDrawer, record, true );

                cancelButton.setVisibility( VISIBLE );
                modifyButton.setVisibility( GONE );
                updateButton.setVisibility( VISIBLE );


                    if(deleteButton != null) deleteButton.setVisibility( VISIBLE );
                    if(resetProjectionButton != null) resetProjectionButton.setVisibility( GONE );

            } );

            if(resetProjectionButton != null) {
                resetProjectionButton.setVisibility( GONE );
            }

            if(updateButton != null) updateButton.setOnClickListener( v -> {
                mListItemViewBuilder.updateTransactionFromEditorView( listItem.findViewById( R.id.card_view ), record );
                Toast toast;
                if(mViewModel.update( record )) {
                    mListItemViewBuilder.setLineItemPropertyViews(listItem, record);
                    mListItemViewBuilder.setCardLayoutPropertyViews(listItem, record, false);
                    cancelButton.callOnClick();
                    toast = Toast.makeText(getContext(), "Update successful", Toast.LENGTH_SHORT);

                } else toast = Toast.makeText( getContext(), "Error updating item", Toast.LENGTH_LONG );

                toast.show();

            } );

            if(cancelButton != null) cancelButton.setOnClickListener( v-> {
                mListItemViewBuilder.setCardLayoutPropertyViews( cardDrawer, record, false );

                if(modifyButton != null)  modifyButton.setVisibility( VISIBLE );
                if(updateButton != null)  updateButton.setVisibility( GONE );
                if(resetProjectionButton != null)  resetProjectionButton.setVisibility( GONE );
                if(deleteButton != null)  deleteButton.setVisibility( GONE );
                if(cancelButton != null)  cancelButton.setVisibility( GONE );
            } );

            if(deleteButton != null) deleteButton.setOnClickListener( v -> {
                mViewModel.delete( record );
            } );

            if(recordTransactionButton != null) {
                recordTransactionButton.setVisibility( GONE );
            }
        }



    }

}
