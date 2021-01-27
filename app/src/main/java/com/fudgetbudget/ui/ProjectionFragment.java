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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class ProjectionFragment extends Fragment {

    private ListItemViewBuilder mListItemViewBuilder;
    private FudgetBudgetViewModel mViewModel;
    private String mProjectionKey;
    private MutableLiveData<ProjectedTransaction> mProjection;
    private MutableLiveData<Double> mProjectedBalance;
    private MutableLiveData<Double> mBalanceThreshold;
    private MutableLiveData<LocalDate> vLineDate;
    private MutableLiveData<String> vLineLabel;
    private MutableLiveData<String> vLineAmount;
    private MutableLiveData<String> vLineBalance;

    public static ProjectionFragment getInstance(String projectionKey){
        ProjectionFragment fragment = new ProjectionFragment();
        Bundle bundle = new Bundle();
        bundle.putString("PROJECTION_KEY", projectionKey);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mListItemViewBuilder = new ListItemViewBuilder(getContext());
        mProjectionKey = getArguments().getString("PROJECTION_KEY");

        if(savedInstanceState != null){
            vLineDate = new MutableLiveData<>(LocalDate.parse(savedInstanceState.getString("PROJECTION_LINE_DATE"), DateTimeFormatter.BASIC_ISO_DATE));
            vLineLabel = new MutableLiveData<>(savedInstanceState.getString("PROJECTION_LINE_LABEL"));
            vLineAmount = new MutableLiveData<>(savedInstanceState.getString("PROJECTION_LINE_AMOUNT"));
            vLineBalance = new MutableLiveData<>(savedInstanceState.getString("PROJECTION_LINE_BALANCE"));

            mProjection = new MutableLiveData<>();
            mProjectedBalance = new MutableLiveData<>();
            mBalanceThreshold = new MutableLiveData<>();
        }
        else {
            vLineDate = new MutableLiveData<>(LocalDate.MIN);
            vLineLabel = new MutableLiveData<>( "loading..." );
            vLineAmount = new MutableLiveData<>( "" );
            vLineBalance = new MutableLiveData<>( "" );

            mProjection = new MutableLiveData<>();
            mProjectedBalance = new MutableLiveData<>();
            mBalanceThreshold = new MutableLiveData<>();
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup projectionView = (ViewGroup) inflater.inflate(R.layout.layout_projection, container, false);

        View lineItem = projectionView.findViewById(R.id.line_item);
        lineItem.setOnClickListener( item -> mListItemViewBuilder.toggleCardVisibility(projectionView));

        vLineDate.observeForever( newDate -> {
            TextView dateView = projectionView.findViewById( R.id.line_item_date_value );
            if(dateView != null) dateView.setText( formatUtility.formatDate( "eee MM-dd", newDate ));

            if( newDate != null && newDate.isBefore( LocalDate.now().plusDays( 1 ) ))
                projectionView.findViewById( R.id.line_item ).setBackgroundResource(R.drawable.projection_overdue_background);
            else projectionView.findViewById( R.id.line_item ).setBackgroundResource(R.drawable.list_item_background);

        });
        vLineLabel.observeForever( newLabel -> {
            TextView labelView = projectionView.findViewById( R.id.line_item_label_value );
            if(labelView != null) labelView.setText( newLabel );
        });
        vLineAmount.observeForever( newAmount -> {
            TextView amountView = projectionView.findViewById( R.id.line_item_amount_value );
            if(amountView != null) {
                amountView.setText( "$" + formatUtility.formatCurrency( newAmount ));

                if(mProjection != null && mProjection.getValue() != null){
                    if(mProjection.getValue().getIncomeFlag()) amountView.setTextAppearance(R.style.income_text_style);
                    else amountView.setTextAppearance(R.style.expense_text_style);
                }

            }
        });
        vLineBalance.observeForever( newBalance -> {
            TextView balanceView = projectionView.findViewById( R.id.line_item_balance_value );
            if(balanceView != null){
                balanceView.setText( "$" + formatUtility.formatCurrency( newBalance ));

                if(mViewModel != null && mBalanceThreshold.getValue() != null){
                    Double threshold = mBalanceThreshold.getValue();
                    if(vLineDate != null && vLineDate.getValue().isBefore(LocalDate.now().plusDays(1)))
                        lineItem.setBackgroundResource(R.drawable.projection_overdue_background);
                    else if(Double.parseDouble(newBalance) < 0.0)
                        lineItem.setBackgroundResource(R.drawable.zero_balance_background);
                    else if(Double.parseDouble(newBalance) < threshold)
                        lineItem.setBackgroundResource(R.drawable.under_threshold_background);
                    else
                        lineItem.setBackgroundResource(R.drawable.list_item_background);
                }
            }
        });

        return projectionView;
    }


    @Override
    public void onResume() {
        super.onResume();

        Executors.newSingleThreadExecutor().execute(()->{
            FudgetBudgetViewModel model = new ViewModelProvider(requireActivity()).get(FudgetBudgetViewModel.class);


            HandlerCompat.createAsync(Looper.getMainLooper()).post(() -> {
                mViewModel = model;

                mViewModel.getProjection(mProjectionKey).observe( getViewLifecycleOwner(), updatedProjection -> {
                    if(mProjection.getValue() == null || !mProjection.getValue().equals(updatedProjection)) mProjection.postValue(updatedProjection);
                });

                mViewModel.getProjectedBalance(mProjectionKey).observe( getViewLifecycleOwner(), newBal -> {
                    if(mProjectedBalance.getValue() == null || !mProjectedBalance.getValue().equals(newBal)) mProjectedBalance.postValue(newBal);
                });

                mViewModel.getSettingsObject(R.string.setting_value_balance_threshold).observe(getViewLifecycleOwner(), threshold -> {
                    if(mBalanceThreshold.getValue() == null || !mBalanceThreshold.getValue().equals(threshold)) mBalanceThreshold.postValue((Double)threshold);
                });

                initializeCardButtons((ViewGroup) getView());
                mListItemViewBuilder.setCardLayoutPropertyViews((ViewGroup) getView(), model.getProjection(mProjectionKey).getValue(), false);

            });

        });


        ViewGroup projectionView = (ViewGroup) getView();


        mProjection.observe( getViewLifecycleOwner(), projection -> {
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

        mProjectedBalance.observe(getViewLifecycleOwner(), balance -> {
            vLineBalance.postValue(balance.toString());
            //also compare balance to threshold to determine if lineitem background needs to change
        });

        mBalanceThreshold.observe(getViewLifecycleOwner(), threshold -> {
            //compare to projectedbalance to determine if lineitem background needs to change
        });
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("PROJECTION_LINE_DATE", vLineDate.getValue().format(DateTimeFormatter.BASIC_ISO_DATE));
        outState.putString("PROJECTION_LINE_LABEL", vLineLabel.getValue());
        outState.putString("PROJECTION_LINE_AMOUNT", vLineAmount.getValue());
        outState.putString("PROJECTION_LINE_BALANCE", vLineBalance.getValue());

    }

    void initializeCardButtons(ViewGroup listItem){
            ProjectedTransaction transaction = mProjection.getValue();

            if( listItem != null && transaction != null){
                ViewGroup buttonsView = listItem.findViewById( R.id.card_buttons );
                ViewGroup cardDrawer = listItem.findViewById( R.id.card_view );

                Button modifyButton = buttonsView.findViewById( R.id.modify_transaction_button );
                Button resetProjectionButton = buttonsView.findViewById( R.id.reset_projections_button );
                Button updateButton = buttonsView.findViewById( R.id.editor_update_button );
                Button cancelButton = buttonsView.findViewById( R.id.editor_cancel_button );
                Button deleteButton = buttonsView.findViewById( R.id.editor_delete_button );
                Button recordTransactionButton = buttonsView.findViewById( R.id.record_transaction_button );
                if(modifyButton != null) modifyButton.setOnClickListener( v -> {
                    mListItemViewBuilder.setCardLayoutPropertyViews( cardDrawer, transaction, true );

                    cancelButton.setVisibility( VISIBLE );
                    modifyButton.setVisibility( GONE );
                    updateButton.setVisibility( VISIBLE );

                    if(transaction instanceof ProjectedTransaction) {
                        if(resetProjectionButton != null) resetProjectionButton.setVisibility( VISIBLE );
                        if(deleteButton != null) deleteButton.setVisibility( GONE );
                    }
                    else {
                        if(deleteButton != null) deleteButton.setVisibility( VISIBLE );
                        if(resetProjectionButton != null) resetProjectionButton.setVisibility( GONE );
                    }

                } );

                if(resetProjectionButton != null) {
                    if( !(transaction instanceof ProjectedTransaction)) resetProjectionButton.setVisibility( GONE );
                    else resetProjectionButton.setOnClickListener( v-> {
                        if(transaction instanceof ProjectedTransaction)
                            mViewModel.reset( (ProjectedTransaction)transaction);
                        mListItemViewBuilder.setCardLayoutPropertyViews( listItem, transaction, false );
                        mListItemViewBuilder.setLineItemPropertyViews( listItem, transaction );
                    } );
                }

                if(updateButton != null) updateButton.setOnClickListener( v -> {
                    mListItemViewBuilder.updateTransactionFromEditorView( listItem.findViewById( R.id.card_view ), transaction );
                    Toast toast;
                    if(mViewModel.update( transaction )) {
                        mListItemViewBuilder.setLineItemPropertyViews(listItem, transaction);
                        mListItemViewBuilder.setCardLayoutPropertyViews(listItem, transaction, false);
                        cancelButton.callOnClick();
                        toast = Toast.makeText(getContext(), "Update successful", Toast.LENGTH_SHORT);

                        //                View pageView = getActivity().findViewById( R.id.page_balanceSheet );
                        //                setPageHeader( pageView );
                        //                setPageLists( pageView );
                        //                viewModel.setLineItemPropertyViews( listItem, transaction );
                        //                viewModel.setCardLayoutPropertyViews( listItem, transaction, false );
                        //
                        //                listItem.findViewById( R.id.editor_cancel_button ).callOnClick();

                        //  getActivity().recreate();
                    } else toast = Toast.makeText( getContext(), "Error updating item", Toast.LENGTH_LONG );

                    toast.show();

                } );

                if(cancelButton != null) cancelButton.setOnClickListener( v-> {
                    mListItemViewBuilder.setCardLayoutPropertyViews( cardDrawer, transaction, false );

                    if(modifyButton != null)  modifyButton.setVisibility( VISIBLE );
                    if(updateButton != null)  updateButton.setVisibility( GONE );
                    if(resetProjectionButton != null)  resetProjectionButton.setVisibility( GONE );
                    if(deleteButton != null)  deleteButton.setVisibility( GONE );
                    if(cancelButton != null)  cancelButton.setVisibility( GONE );
                } );

//                if(deleteButton != null) deleteButton.setOnClickListener( v -> {
//                    mViewModel.delete( transaction );
//                } );

                if(recordTransactionButton != null) {
                    LocalDate transactionDate = (LocalDate) transaction.getProperty( R.string.date_tag );
                    if( transactionDate.isAfter( LocalDate.now() )) recordTransactionButton.setVisibility( GONE );
                    else recordTransactionButton.setOnClickListener( v -> {
                        mViewModel.reconcile(transaction);
                    } );
                }
            }



        }

}
