package com.fudgetbudget;

import android.os.Bundle;

import com.fudgetbudget.model.Transaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

public class MainActivity<T extends Transaction> extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        ViewModelProvider.Factory factory = new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <V extends ViewModel> V create(@NonNull Class<V> modelClass) {
                return (V) new FudgetBudgetViewModel<T>( new FudgetBudgetRepo<>(getExternalFilesDir(null).getPath()) );
            }
        };
        new ViewModelProvider(this, factory).get(FudgetBudgetViewModel.class);

        BottomNavigationView navView = findViewById( R.id.nav_view );
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_budget, R.id.navigation_transactions, R.id.navigation_records, R.id.navigation_tools ).build();
        NavController navController = Navigation.findNavController( MainActivity.this, R.id.nav_host_fragment );
        NavigationUI.setupActionBarWithNavController( MainActivity.this, navController, appBarConfiguration );
        NavigationUI.setupWithNavController( navView, navController );

    }

}
