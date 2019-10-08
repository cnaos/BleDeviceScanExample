package io.github.cnaos.example.bledevicescan

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.github.cnaos.example.bledevicescan.ui.main.MainFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }
    }

}
