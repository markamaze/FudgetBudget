package com.fudgetbudget;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fudgetbudget.model.Transaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainActivity<T extends Transaction> extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        ViewModelProvider.Factory factory = new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <V extends ViewModel> V create(@NonNull Class<V> modelClass) {
                return (V) new FudgetBudgetViewModel( new FudgetBudgetRepo(getExternalFilesDir(null).getPath()) );
            }
        };
        new ViewModelProvider(this, factory).get(FudgetBudgetViewModel.class);

        BottomNavigationView navView = findViewById( R.id.nav_view );
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_transactions, R.id.navigation_budget, R.id.navigation_records ).build();
        NavController navController = Navigation.findNavController( MainActivity.this, R.id.nav_host_fragment );
        NavigationUI.setupActionBarWithNavController( MainActivity.this, navController, appBarConfiguration );
        NavigationUI.setupWithNavController( navView, navController );

    }

    @Override
    public void onBackPressed() {
        View toolbar = findViewById(R.id.toolbar);
        View infobar = findViewById(R.id.about);
        boolean closedView = false;
        if(toolbar.getVisibility() == VISIBLE) {
            toolbar.setVisibility(GONE);
            closedView = true;
        }
        if(infobar.getVisibility() == VISIBLE) {
            infobar.setVisibility(GONE);
            closedView = true;
        }
        if(!closedView) super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {

        switch (item.getItemId())
        {
            case R.id.about_page:
                View infobar = findViewById(R.id.about);
                if(infobar.getVisibility() != VISIBLE) {
                    infobar.setVisibility(VISIBLE);
                    findViewById(R.id.toolbar).setVisibility(GONE);
                    infobar.bringToFront();
                }
                else infobar.setVisibility(GONE);


                return true;

            case R.id.toolbar_toggle:
                View toolbar = findViewById(R.id.toolbar);
                if(toolbar.getVisibility() != VISIBLE) {
                    toolbar.setVisibility(VISIBLE);
                    findViewById(R.id.about).setVisibility(GONE);
                    toolbar.bringToFront();
                }
                else toolbar.setVisibility(GONE);

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
